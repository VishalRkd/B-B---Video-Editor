package xiaoying.utils;

import android.media.MediaCodec;
import android.media.MediaCodecList;
import android.media.MediaCrypto;
import android.media.MediaFormat;
import android.os.Process;
import android.view.Surface;
import java.nio.ByteBuffer;

/* JADX INFO: loaded from: classes19.dex */
public class QMediaCodecDecoder {
    static final String TAG = "MCDECODER";
    static final byte[] start_code = {0, 0, 0, 1};
    private MediaCodec decoder_;
    boolean eos_sent_ = false;
    private MediaFormat format_;
    ByteBuffer[] inputBuffers_;
    ByteBuffer[] outputBuffers_;

    public QMediaCodecDecoder() {
        StringBuilder sb2 = new StringBuilder();
        sb2.append("QMediaCodecDecoder() Thread: ");
        sb2.append(Process.myTid());
        int codecCount = MediaCodecList.getCodecCount();
        for (int i10 = 0; i10 < codecCount; i10++) {
            for (String str : MediaCodecList.getCodecInfoAt(i10).getSupportedTypes()) {
                StringBuilder sb3 = new StringBuilder();
                sb3.append(" ");
                sb3.append(str);
            }
        }
    }

    private int indexOf(byte[] outerArray, byte[] smallerArray, int start) {
        while (start < (outerArray.length - smallerArray.length) + 1) {
            for (int i10 = 0; i10 < smallerArray.length; i10++) {
                if (outerArray[start + i10] != smallerArray[i10]) {
                    start++;
                }
            }
            return start;
        }
        return -1;
    }

    public boolean InitAACDecoder(int sampleRate, int channels, byte[] hd2, int hdsize) {
        StringBuilder sb2 = new StringBuilder();
        sb2.append("InitAACDecoder() Thread: ");
        sb2.append(Process.myTid());
        try {
            this.decoder_ = MediaCodec.createDecoderByType("audio/mp4a-latm");
        } catch (Exception e10) {
            e10.printStackTrace();
        }
        if (this.decoder_ == null) {
            return false;
        }
        try {
            MediaFormat mediaFormatCreateAudioFormat = MediaFormat.createAudioFormat("audio/mp4a-latm", sampleRate, channels);
            this.format_ = mediaFormatCreateAudioFormat;
            mediaFormatCreateAudioFormat.setInteger("aac-profile", 2);
            this.format_.setByteBuffer("csd-0", ByteBuffer.wrap(hd2, 0, hdsize));
            this.decoder_.configure(this.format_, (Surface) null, (MediaCrypto) null, 0);
            this.decoder_.start();
            this.inputBuffers_ = this.decoder_.getInputBuffers();
            this.outputBuffers_ = this.decoder_.getOutputBuffers();
            return true;
        } catch (Exception unused) {
            return false;
        }
    }

    public boolean InitAVCDecoder(int width, int height, byte[] hd2, int hdsize) {
        return InitAVCDecoder(width, height, hd2, hdsize, false);
    }

    public boolean InitAudioDecoder(MediaFormat format) {
        try {
            String string = format.getString("mime");
            StringBuilder sb2 = new StringBuilder();
            sb2.append("format:: ");
            sb2.append(string);
            this.decoder_ = MediaCodec.createDecoderByType(string);
        } catch (Exception e10) {
            e10.printStackTrace();
        }
        MediaCodec mediaCodec = this.decoder_;
        if (mediaCodec == null) {
            return false;
        }
        try {
            this.format_ = format;
            mediaCodec.configure(format, (Surface) null, (MediaCrypto) null, 0);
            this.decoder_.start();
            this.inputBuffers_ = this.decoder_.getInputBuffers();
            this.outputBuffers_ = this.decoder_.getOutputBuffers();
            return true;
        } catch (Exception unused) {
            return false;
        }
    }

    public boolean InitMP3Decoder(int sampleRate, int channels) {
        StringBuilder sb2 = new StringBuilder();
        sb2.append("InitMP3Decoder() Thread: ");
        sb2.append(Process.myTid());
        try {
            this.decoder_ = MediaCodec.createDecoderByType("audio/mpeg");
        } catch (Exception e10) {
            e10.printStackTrace();
        }
        if (this.decoder_ == null) {
            return false;
        }
        try {
            MediaFormat mediaFormatCreateAudioFormat = MediaFormat.createAudioFormat("audio/mpeg", sampleRate, channels);
            this.format_ = mediaFormatCreateAudioFormat;
            this.decoder_.configure(mediaFormatCreateAudioFormat, (Surface) null, (MediaCrypto) null, 0);
            this.decoder_.start();
            this.inputBuffers_ = this.decoder_.getInputBuffers();
            this.outputBuffers_ = this.decoder_.getOutputBuffers();
            return true;
        } catch (Exception unused) {
            return false;
        }
    }

    public void UninitDecoder() {
        StringBuilder sb2 = new StringBuilder();
        sb2.append("UninitDecoder() Thread: ");
        sb2.append(Process.myTid());
        MediaCodec mediaCodec = this.decoder_;
        if (mediaCodec == null) {
            return;
        }
        try {
            mediaCodec.stop();
            this.decoder_.release();
        } catch (Exception unused) {
        }
    }

    public int decodeFrame(byte[] inbuffer, int insize, int ts, byte[] outbuffer) {
        MediaCodec mediaCodec = this.decoder_;
        if (mediaCodec == null) {
            return -1;
        }
        if (!this.eos_sent_) {
            int iDequeueInputBuffer = mediaCodec.dequeueInputBuffer(-1L);
            if (insize > 0) {
                this.inputBuffers_[iDequeueInputBuffer].rewind();
                this.inputBuffers_[iDequeueInputBuffer].put(inbuffer, 0, insize);
                this.decoder_.queueInputBuffer(iDequeueInputBuffer, 0, insize, ts, 0);
            } else {
                this.decoder_.queueInputBuffer(iDequeueInputBuffer, 0, 0, ts, 4);
                this.eos_sent_ = true;
            }
        }
        MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
        int iDequeueOutputBuffer = this.decoder_.dequeueOutputBuffer(bufferInfo, 10000L);
        if (iDequeueOutputBuffer >= 0) {
            ByteBuffer byteBuffer = this.outputBuffers_[iDequeueOutputBuffer];
            int i10 = bufferInfo.size;
            byteBuffer.get(outbuffer, bufferInfo.offset, i10);
            byteBuffer.clear();
            this.decoder_.releaseOutputBuffer(iDequeueOutputBuffer, false);
            return i10;
        }
        if (iDequeueOutputBuffer == -3) {
            this.outputBuffers_ = this.decoder_.getOutputBuffers();
            return 0;
        }
        if (iDequeueOutputBuffer == -2) {
            this.format_ = this.decoder_.getOutputFormat();
            return 0;
        }
        if (iDequeueOutputBuffer == -1) {
            return 0;
        }
        StringBuilder sb2 = new StringBuilder();
        sb2.append("Decode ERROR : ");
        sb2.append(iDequeueOutputBuffer);
        return iDequeueOutputBuffer;
    }

    public int getBitsPersample() {
        MediaFormat mediaFormat = this.format_;
        if (mediaFormat == null) {
            return -1;
        }
        if (mediaFormat.containsKey("bit-width")) {
            return this.format_.getInteger("bit-width");
        }
        return 16;
    }

    public int getChannels() {
        MediaFormat mediaFormat = this.format_;
        if (mediaFormat == null) {
            return -1;
        }
        return mediaFormat.getInteger("channel-count");
    }

    public int getSampleRate() {
        MediaFormat mediaFormat = this.format_;
        if (mediaFormat == null) {
            return -1;
        }
        return mediaFormat.getInteger("sample-rate");
    }

    public void reset() {
        try {
            this.decoder_.flush();
            this.eos_sent_ = false;
        } catch (Exception unused) {
        }
    }

    /* JADX WARN: Code duplicated, block: B:9:0x0024 A[Catch: Exception -> 0x001e, TRY_LEAVE, TryCatch #0 {Exception -> 0x001e, blocks: (B:4:0x0015, B:7:0x0020, B:9:0x0024), top: B:19:0x0015 }] */
    public boolean InitAVCDecoder(int width, int height, byte[] hd2, int hdsize, boolean forceSW) {
        StringBuilder sb2 = new StringBuilder();
        sb2.append("InitAVCDecoder() Thread: ");
        sb2.append(Process.myTid());
        if (forceSW) {
            try {
                this.decoder_ = MediaCodec.createByCodecName("OMX.google.h264.decoder");
                if (this.decoder_ == null) {
                    this.decoder_ = MediaCodec.createDecoderByType("video/avc");
                }
            } catch (Exception e10) {
                e10.printStackTrace();
            }
        } else if (this.decoder_ == null) {
            try {
                this.decoder_ = MediaCodec.createDecoderByType("video/avc");
            } catch (Exception e10) {
                e10.printStackTrace();
            }
        }
        if (this.decoder_ == null) {
            return false;
        }
        try {
            this.format_ = MediaFormat.createVideoFormat("video/avc", width, height);
            byte[] bArr = start_code;
            int iIndexOf = indexOf(hd2, bArr, bArr.length);
            ByteBuffer byteBufferWrap = ByteBuffer.wrap(hd2, 0, iIndexOf);
            ByteBuffer byteBufferWrap2 = ByteBuffer.wrap(hd2, iIndexOf, hd2.length - iIndexOf);
            this.format_.setByteBuffer("csd-0", byteBufferWrap);
            this.format_.setByteBuffer("csd-1", byteBufferWrap2);
            this.decoder_.configure(this.format_, (Surface) null, (MediaCrypto) null, 0);
            this.decoder_.start();
            this.inputBuffers_ = this.decoder_.getInputBuffers();
            this.outputBuffers_ = this.decoder_.getOutputBuffers();
            return true;
        } catch (Exception unused) {
            return false;
        }
    }
}
