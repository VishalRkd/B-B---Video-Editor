// macOS host fixture generator, independent of the Android editor/compiler.
// swift tools/generate_temporal_fixture.swift /tmp/foundation-4k30.mp4 3840 2160 30 3 h264
import AVFoundation
import CoreVideo
import Foundation

guard CommandLine.arguments.count == 8 else {
    fatalError("Expected: output.mp4 width height fps seconds h264|hevc sourceId(0...3)")
}
let args = CommandLine.arguments
let path = args[1]
let width = Int(args[2])!, height = Int(args[3])!, fps = Int(args[4])!, seconds = Int(args[5])!
let codec: AVVideoCodecType = args[6] == "hevc" ? .hevc : .h264
let source = Int(args[7])!
precondition(width % 16 == 0 && height % 10 == 0 && fps > 0 && seconds > 0 && (0...3).contains(source))
precondition(!FileManager.default.fileExists(atPath: path), "Choose a fresh output filename")
let writer = try AVAssetWriter(outputURL: URL(fileURLWithPath: path), fileType: .mp4)
let input = AVAssetWriterInput(mediaType: .video, outputSettings: [
    AVVideoCodecKey: codec, AVVideoWidthKey: width, AVVideoHeightKey: height,
    AVVideoCompressionPropertiesKey: [AVVideoAverageBitRateKey: 12_000_000,
        AVVideoExpectedSourceFrameRateKey: fps, AVVideoMaxKeyFrameIntervalKey: fps,
        AVVideoAllowFrameReorderingKey: false]])
let adaptor = AVAssetWriterInputPixelBufferAdaptor(assetWriterInput: input, sourcePixelBufferAttributes: [
    kCVPixelBufferPixelFormatTypeKey as String: kCVPixelFormatType_32BGRA,
    kCVPixelBufferWidthKey as String: width, kCVPixelBufferHeightKey as String: height,
    kCVPixelBufferIOSurfacePropertiesKey as String: [:]])
writer.add(input)
guard writer.startWriting() else { fatalError("start: \(String(describing: writer.error))") }
writer.startSession(atSourceTime: .zero)
for frame in 0..<(fps * seconds) {
    let deadline = Date().addingTimeInterval(30)
    while !input.isReadyForMoreMediaData && Date() < deadline { usleep(1000) }
    guard input.isReadyForMoreMediaData else { fatalError("Encoder stalled: \(String(describing: writer.error))") }
    autoreleasepool {
        var pixel: CVPixelBuffer?
        precondition(CVPixelBufferPoolCreatePixelBuffer(nil, adaptor.pixelBufferPool!, &pixel) == kCVReturnSuccess)
        let buffer = pixel!
        CVPixelBufferLockBaseAddress(buffer, [])
        let stride = CVPixelBufferGetBytesPerRow(buffer) / 4
        let base = CVPixelBufferGetBaseAddress(buffer)!.assumingMemoryBound(to: UInt32.self)
        base.initialize(repeating: 0xff18202e, count: stride * height)
        let code = (frame % 1024) | (source << 10) | (0b1010 << 12)
        for bit in 0..<16 {
            for row in 0..<2 {
                let white = ((code >> bit) & 1) == (row == 0 ? 1 : 0)
                let y = row == 0 ? height / 10 : height * 6 / 10
                for line in y..<(y + height * 3 / 10) {
                    base.advanced(by: line * stride + bit * width / 16)
                        .initialize(repeating: white ? 0xffffffff : 0xff000000, count: width / 16)
                }
            }
        }
        let marker = (frame * width / 45) % (width * 15 / 16)
        for y in (height * 46 / 100)..<(height * 54 / 100) {
            base.advanced(by: y * stride + marker).initialize(repeating: 0xffff0000, count: width / 16)
        }
        CVPixelBufferUnlockBaseAddress(buffer, [])
        precondition(adaptor.append(buffer, withPresentationTime: CMTime(value: Int64(frame), timescale: Int32(fps))))
    }
}
writer.endSession(atSourceTime: CMTime(value: Int64(fps * seconds), timescale: Int32(fps)))
input.markAsFinished()
let done = DispatchSemaphore(value: 0)
writer.finishWriting { done.signal() }
precondition(done.wait(timeout: .now() + 60) == .success && writer.status == .completed,
    "Finish failed: \(String(describing: writer.error))")
print("Created \(width)x\(height) \(fps)fps \(seconds)s \(codec.rawValue): \(path)")

// Independently decode every encoded sample and check its visible barcode against muxed PTS.
// A successfully written container alone is not a temporal fixture validation.
let asset = AVURLAsset(url: URL(fileURLWithPath: path))
let track = asset.tracks(withMediaType: .video).first!
let reader = try AVAssetReader(asset: asset)
let decoded = AVAssetReaderTrackOutput(track: track, outputSettings: [
    kCVPixelBufferPixelFormatTypeKey as String: kCVPixelFormatType_32BGRA])
decoded.alwaysCopiesSampleData = false
reader.add(decoded)
precondition(reader.startReading())
var verified = 0
while let sample = decoded.copyNextSampleBuffer() {
    let buffer = CMSampleBufferGetImageBuffer(sample)!
    CVPixelBufferLockBaseAddress(buffer, .readOnly)
    let row = CVPixelBufferGetBytesPerRow(buffer) / 4
    let pixels = CVPixelBufferGetBaseAddress(buffer)!.assumingMemoryBound(to: UInt32.self)
    var bits = 0
    for bit in 0..<16 {
        let pixel = pixels[height / 4 * row + (bit * 2 + 1) * width / 32]
        let brightness = Int(pixel & 255) + Int((pixel >> 8) & 255) + Int((pixel >> 16) & 255)
        if brightness > 384 { bits |= 1 << bit }
    }
    CVPixelBufferUnlockBaseAddress(buffer, .readOnly)
    let time = CMTimeGetSeconds(CMSampleBufferGetPresentationTimeStamp(sample))
    let expected = Int((time * Double(fps)).rounded()) % 1024
    precondition(bits & 1023 == expected && (bits >> 10) & 3 == source && (bits >> 12) & 15 == 10,
        "Encoded fixture identity/PTS mismatch at \(time): bits=\(bits), expected frame=\(expected)")
    verified += 1
}
precondition(reader.status == .completed && verified == fps * seconds,
    "Readback failed or lost frames: \(verified), \(String(describing: reader.error))")
print("Verified all \(verified) encoded frame identities against presentation timestamps")
