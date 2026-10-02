package xiaoying.utils;

import android.media.MediaCodec;
import android.media.MediaCrypto;
import android.media.MediaFormat;
import android.os.Process;
import android.view.Surface;
import java.nio.ByteBuffer;

/* JADX INFO: loaded from: classes19.dex */
public class QMediaCodecAACEncoder {
    static final int ADTS_HEADER_SIZE = 7;
    static final String TAG = "AACENCODER";
    private MediaCodec encoder_;
    private MediaFormat format_;
    private ByteBuffer[] inputBuffers_;
    private ByteBuffer[] outputBuffers_;
    private int channels_ = 1;
    private int sampleRate_ = 44100;
    private boolean eos_sent_ = false;
    private boolean is_adts_ = false;

    public QMediaCodecAACEncoder() {
        StringBuilder sb2 = new StringBuilder();
        sb2.append("QMediaCodecAACDecoder() Thread: ");
        sb2.append(Process.myTid());
    }

    private void addADTStoPacket(byte[] packet, int packetLen, int sampleRate, int channels) {
        int i10 = (sampleRate == 44100 || sampleRate != 44800) ? 4 : 3;
        int i11 = packetLen + 7;
        packet[0] = -1;
        packet[1] = -7;
        packet[2] = (byte) (64 + (i10 << 2) + (channels >> 2));
        packet[3] = (byte) (((channels & 3) << 6) + (i11 >> 11));
        packet[4] = (byte) ((i11 & 2047) >> 3);
        packet[5] = (byte) (((i11 & 7) << 5) + 31);
        packet[6] = -4;
    }

    public boolean InitAACEncoder(int sampleRate, int channels) {
        return InitAACEncoder(sampleRate, channels, 128000);
    }

    public void UninitEncoder() {
        StringBuilder sb2 = new StringBuilder();
        sb2.append("UninitEncoder() Thread: ");
        sb2.append(Process.myTid());
        MediaCodec mediaCodec = this.encoder_;
        if (mediaCodec == null) {
            return;
        }
        try {
            mediaCodec.stop();
            this.encoder_.release();
        } catch (Exception unused) {
        }
    }

    public int encodeFrame(byte[] inbuffer, int insize, int ts, byte[] outbuffer) {
        MediaCodec mediaCodec = this.encoder_;
        if (mediaCodec == null) {
            return -1;
        }
        if (!this.eos_sent_) {
            int iDequeueInputBuffer = mediaCodec.dequeueInputBuffer(-1L);
            if (insize > 0) {
                this.inputBuffers_[iDequeueInputBuffer].rewind();
                this.inputBuffers_[iDequeueInputBuffer].put(inbuffer, 0, insize);
                this.encoder_.queueInputBuffer(iDequeueInputBuffer, 0, insize, ts, 0);
            } else {
                this.encoder_.queueInputBuffer(iDequeueInputBuffer, 0, 0, ts, 4);
                this.eos_sent_ = true;
            }
        }
        MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
        int iDequeueOutputBuffer = this.encoder_.dequeueOutputBuffer(bufferInfo, 10000L);
        if (iDequeueOutputBuffer >= 0) {
            ByteBuffer byteBuffer = this.outputBuffers_[iDequeueOutputBuffer];
            int i10 = bufferInfo.size;
            byteBuffer.get(outbuffer, bufferInfo.offset, i10);
            byteBuffer.clear();
            this.encoder_.releaseOutputBuffer(iDequeueOutputBuffer, false);
            return i10;
        }
        if (iDequeueOutputBuffer == -3) {
            this.outputBuffers_ = this.encoder_.getOutputBuffers();
            return 0;
        }
        if (iDequeueOutputBuffer != -2) {
            return 0;
        }
        MediaFormat outputFormat = this.encoder_.getOutputFormat();
        this.format_ = outputFormat;
        this.sampleRate_ = outputFormat.getInteger("sample-rate");
        this.channels_ = this.format_.getInteger("channel-count");
        MediaCodec.BufferInfo bufferInfo2 = new MediaCodec.BufferInfo();
        int iDequeueOutputBuffer2 = this.encoder_.dequeueOutputBuffer(bufferInfo2, 10000L);
        if (iDequeueOutputBuffer2 < 0) {
            return 0;
        }
        ByteBuffer byteBuffer2 = this.outputBuffers_[iDequeueOutputBuffer2];
        this.encoder_.releaseOutputBuffer(iDequeueOutputBuffer2, false);
        int iDequeueOutputBuffer3 = this.encoder_.dequeueOutputBuffer(bufferInfo2, 10000L);
        if (iDequeueOutputBuffer3 < 0) {
            return 0;
        }
        ByteBuffer byteBuffer3 = this.outputBuffers_[iDequeueOutputBuffer3];
        int i11 = bufferInfo2.size;
        byteBuffer3.get(outbuffer, bufferInfo2.offset, i11);
        byteBuffer3.clear();
        this.encoder_.releaseOutputBuffer(iDequeueOutputBuffer3, false);
        return i11;
    }

    public int getAudioSpecData(byte[] outbuffer) {
        ByteBuffer byteBuffer = this.format_.getByteBuffer("csd-0");
        int iLimit = byteBuffer.limit();
        if (iLimit > outbuffer.length) {
            return 0;
        }
        byteBuffer.get(outbuffer, 0, iLimit);
        return iLimit;
    }

    public boolean InitAACEncoder(int sampleRate, int channels, int bitrate) {
        StringBuilder sb2 = new StringBuilder();
        sb2.append("InitAACEncoder() Thread: ");
        sb2.append(Process.myTid());
        try {
            this.encoder_ = MediaCodec.createEncoderByType("audio/mp4a-latm");
        } catch (Exception e10) {
            e10.printStackTrace();
        }
        if (this.encoder_ == null) {
            return false;
        }
        try {
            MediaFormat mediaFormatCreateAudioFormat = MediaFormat.createAudioFormat("audio/mp4a-latm", sampleRate, channels);
            this.format_ = mediaFormatCreateAudioFormat;
            if (bitrate <= 0) {
                bitrate = 128000;
            }
            mediaFormatCreateAudioFormat.setInteger("bitrate", bitrate);
            this.encoder_.configure(this.format_, (Surface) null, (MediaCrypto) null, 1);
            this.encoder_.start();
            this.inputBuffers_ = this.encoder_.getInputBuffers();
            this.outputBuffers_ = this.encoder_.getOutputBuffers();
            return true;
        } catch (Exception e11) {
            e11.printStackTrace();
            return false;
        }
    }
}
