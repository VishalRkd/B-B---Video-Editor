package xiaoying.utils;

import android.content.res.AssetFileDescriptor;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaCodecList;
import android.media.MediaCrypto;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.os.Process;
import android.view.Surface;
import java.nio.ByteBuffer;


/* JADX INFO: loaded from: classes19.dex */
class VDecoder {
    static final String TAG = "Decoder";
    private MediaCodec decoder_;
    private MediaExtractor extractor_;
    private AssetFileDescriptor fd_;
    private String file_;
    private MediaFormat format_;
    ByteBuffer[] inputBuffers_;
    ByteBuffer[] outputBuffers_;
    private int track_ = -1;
    private byte[] pkgData_ = null;
    private byte[] frameData_ = null;
    private boolean eos_sent_ = false;
    private boolean eos_ = false;

    public VDecoder(String file) {
        this.file_ = file;
    }

    private String[] getSupportedEncodeCodecs() {
        StringBuilder sb2 = new StringBuilder();
        sb2.append("QMediaCodecDecoder() Thread: ");
        sb2.append(Process.myTid());
        int codecCount = MediaCodecList.getCodecCount();
        for (int i10 = 0; i10 < codecCount; i10++) {
            MediaCodecInfo codecInfoAt = MediaCodecList.getCodecInfoAt(i10);
            String[] supportedTypes = codecInfoAt.getSupportedTypes();
            if (codecInfoAt.isEncoder()) {
                for (String str : supportedTypes) {
                    codecInfoAt.getCapabilitiesForType(str);
                    StringBuilder sb3 = new StringBuilder();
                    sb3.append("=");
                    sb3.append(str);
                }
            }
        }
        return null;
    }

    public int Init() {
        try {
            this.pkgData_ = new byte[2097152];
            try {
                MediaExtractor mediaExtractor = new MediaExtractor();
                this.extractor_ = mediaExtractor;
                AssetFileDescriptor assetFileDescriptor = this.fd_;
                if (assetFileDescriptor != null) {
                    assetFileDescriptor.toString();
                    this.extractor_.setDataSource(this.fd_.getFileDescriptor());
                } else {
                    mediaExtractor.setDataSource(this.file_);
                }
                this.format_ = null;
                int trackCount = this.extractor_.getTrackCount();
                for (int i10 = 0; i10 < trackCount; i10++) {
                    MediaFormat trackFormat = this.extractor_.getTrackFormat(i10);
                    String string = trackFormat.getString("mime");
                    if (string != null && string.contains("video")) {
                        this.format_ = trackFormat;
                        this.track_ = i10;
                        this.extractor_.selectTrack(i10);
                        break;
                    }
                }
                MediaFormat mediaFormat = this.format_;
                if (mediaFormat == null) {
                    MessageCtx.getInstance().Log(TAG, "Init : not video file");
                    return -3;
                }
                try {
                    MediaCodec mediaCodecCreateDecoderByType = MediaCodec.createDecoderByType(mediaFormat.getString("mime"));
                    this.decoder_ = mediaCodecCreateDecoderByType;
                    mediaCodecCreateDecoderByType.configure(this.format_, (Surface) null, (MediaCrypto) null, 0);
                    this.decoder_.start();
                    this.inputBuffers_ = this.decoder_.getInputBuffers();
                    this.outputBuffers_ = this.decoder_.getOutputBuffers();
                    this.eos_ = false;
                    this.eos_sent_ = false;
                    return 0;
                } catch (Exception e10) {
                    MessageCtx.getInstance().Log(TAG, "Init : config/start : " + e10.getMessage());
                    return -4;
                }
            } catch (Exception e11) {
                MessageCtx.getInstance().Log(TAG, "Init : SetData : " + this.file_ + " ; setDataSource : " + e11.getMessage());
                return -2;
            }
        } catch (Exception unused) {
            MessageCtx.getInstance().Log(TAG, "Init : alloc");
            return -1;
        }
    }

    public void Uninit() {
        try {
            MediaCodec mediaCodec = this.decoder_;
            if (mediaCodec != null) {
                mediaCodec.stop();
            }
        } catch (Exception e10) {
            MessageCtx.getInstance().Log(TAG, "Uninit: stop : " + e10.getMessage());
        }
        this.format_ = null;
        this.inputBuffers_ = null;
        this.outputBuffers_ = null;
        this.pkgData_ = null;
        this.eos_ = false;
        this.eos_sent_ = false;
    }

    public int decodeNext() {
        int i10 = 0;
        do {
            try {
                int iDecodeNextFrame = decodeNextFrame();
                if (iDecodeNextFrame != 0) {
                    return iDecodeNextFrame;
                }
                i10++;
            } catch (Exception unused) {
                return -800;
            }
        } while (i10 <= 15);
        return -4096;
    }

    public int decodeNextFrame() {
        String str;
        boolean z10;
        if (this.eos_) {
            return 0;
        }
        boolean z11 = false;
        int sampleData = 0;
        long sampleTime = 0;
        while (!z11) {
            sampleTime = this.extractor_.getSampleTime();
            if (sampleTime < 0) {
                sampleData = 0;
                z11 = true;
            }
            if (this.extractor_.getSampleTrackIndex() == this.track_) {
                try {
                    sampleData = this.extractor_.readSampleData(ByteBuffer.wrap(this.pkgData_), 0);
                    z11 = true;
                } catch (Exception e10) {
                    MessageCtx.getInstance().Log(TAG, "decodeNextFrame : readSampleData : " + e10.getMessage());
                    return -100;
                }
            }
            this.extractor_.advance();
        }
        try {
            int iDequeueInputBuffer = this.decoder_.dequeueInputBuffer(-1L);
            if (this.eos_sent_) {
                str = TAG;
                z10 = true;
            } else if (sampleData > 0) {
                this.inputBuffers_[iDequeueInputBuffer].rewind();
                this.inputBuffers_[iDequeueInputBuffer].put(this.pkgData_, 0, sampleData);
                this.decoder_.queueInputBuffer(iDequeueInputBuffer, 0, sampleData, sampleTime, 0);
                str = TAG;
                z10 = true;
            } else {
                MediaCodec mediaCodec = this.decoder_;
                str = TAG;
                z10 = true;
                try {
                    mediaCodec.queueInputBuffer(iDequeueInputBuffer, 0, 0, sampleTime, 4);
                    this.eos_sent_ = true;
                } catch (Exception e11) {
                    MessageCtx.getInstance().Log(str, "decodeNextFrame : queue/deque : " + e11.getMessage());
                    return -99;
                }
            }
            MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
            int iDequeueOutputBuffer = this.decoder_.dequeueOutputBuffer(bufferInfo, 10000L);
            if (iDequeueOutputBuffer >= 0) {
                ByteBuffer byteBuffer = this.outputBuffers_[iDequeueOutputBuffer];
                int iRemaining = byteBuffer.remaining();
                byteBuffer.clear();
                this.decoder_.releaseOutputBuffer(iDequeueOutputBuffer, false);
                if ((bufferInfo.flags & 4) != 0) {
                    MessageCtx.getInstance().Log(str, "decodeNextFrame : EOS.");
                    this.eos_ = z10;
                }
                return iRemaining;
            }
            if (iDequeueOutputBuffer == -3) {
                this.outputBuffers_ = this.decoder_.getOutputBuffers();
                MessageCtx.getInstance().Log(str, "decodeNextFrame : INFO_OUTPUT_BUFFERS_CHANGED");
                return 0;
            }
            if (iDequeueOutputBuffer == -2) {
                this.format_ = this.decoder_.getOutputFormat();
                MessageCtx.getInstance().Log(str, "decodeNextFrame : INFO_OUTPUT_FORMAT_CHANGED");
                return 0;
            }
            if (iDequeueOutputBuffer == -1) {
                MessageCtx.getInstance().Log(str, "decodeNextFrame : INFO_TRY_AGAIN_LATER");
                return 0;
            }
            MessageCtx.getInstance().Log(str, "decodeNextFrame : decode failure : " + iDequeueOutputBuffer);
            return iDequeueOutputBuffer;
        } catch (Exception e12) {
            e12.printStackTrace();
            return -99;
        }
    }

    public VDecoder(AssetFileDescriptor fd2) {
        this.fd_ = fd2;
    }
}
