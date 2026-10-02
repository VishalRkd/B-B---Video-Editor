package xiaoying.utils;

import android.media.MediaCodec;
import android.media.MediaCrypto;
import android.media.MediaFormat;
import android.util.Pair;
import android.view.Surface;
import java.nio.ByteBuffer;

/* JADX INFO: loaded from: classes19.dex */
class VEncoder {
    static final String TAG = "Encoder";
    private Codec.Type codec_;
    private int dataSize_;
    private byte[] data_;
    private MediaCodec encoder_;
    private MediaFormat format_;
    private ByteBuffer[] inputBuffers_;
    private ByteBuffer[] outputBuffers_;
    private CodecInspector.Resolution res;
    private int timestamp_ = 0;
    private int framerate_ = 20;

    public VEncoder(Codec.Type codec, CodecInspector.Resolution res) {
        Codec.Type type = Codec.Type.kNone;
        this.res = res;
        this.codec_ = codec;
    }

    public int Init() {
        try {
            this.encoder_ = MediaCodec.createEncoderByType(Codec.toMediaFormatType(this.codec_));
            Pair<Integer, Integer> pairSizeForResolution = Utils.sizeForResolution(this.res);
            try {
                int iIntValue = ((((Integer) pairSizeForResolution.first).intValue() * ((Integer) pairSizeForResolution.second).intValue()) * 3) / 2;
                this.dataSize_ = iIntValue;
                this.data_ = new byte[iIntValue];
                int iBitrateForResolution = Utils.bitrateForResolution(this.res);
                MediaFormat mediaFormatCreateVideoFormat = MediaFormat.createVideoFormat("video/avc", ((Integer) pairSizeForResolution.first).intValue(), ((Integer) pairSizeForResolution.second).intValue());
                this.format_ = mediaFormatCreateVideoFormat;
                mediaFormatCreateVideoFormat.setInteger("bitrate", iBitrateForResolution);
                this.format_.setInteger("frame-rate", this.framerate_);
                this.format_.setInteger("color-format", 21);
                this.format_.setInteger("i-frame-interval", this.framerate_);
                try {
                    this.encoder_.configure(this.format_, (Surface) null, (MediaCrypto) null, 1);
                    this.encoder_.start();
                    this.inputBuffers_ = this.encoder_.getInputBuffers();
                    this.outputBuffers_ = this.encoder_.getOutputBuffers();
                    MessageCtx.getInstance().Log(TAG, "Init: success");
                    return 0;
                } catch (Exception e10) {
                    MessageCtx.getInstance().Log(TAG, "Init: configure/start : " + e10.getMessage());
                    return -3;
                }
            } catch (Exception e11) {
                MessageCtx.getInstance().Log(TAG, "Init: setup format : " + e11.getMessage());
                return -2;
            }
        } catch (Exception e12) {
            MessageCtx.getInstance().Log(TAG, "Init: createEncoderByType(AVC) : " + e12.getMessage());
            return -1;
        }
    }

    public void Uninit() {
        try {
            MediaCodec mediaCodec = this.encoder_;
            if (mediaCodec != null) {
                mediaCodec.stop();
            }
        } catch (Exception e10) {
            MessageCtx.getInstance().Log(TAG, "Uninit: stop : " + e10.getMessage());
        }
        this.format_ = null;
        this.inputBuffers_ = null;
        this.outputBuffers_ = null;
        this.dataSize_ = 0;
        this.data_ = null;
    }

    public int encodeNext() {
        int i10 = 0;
        do {
            try {
                int iEncodeNextFrame = encodeNextFrame();
                if (iEncodeNextFrame != 0) {
                    return iEncodeNextFrame;
                }
                i10++;
            } catch (Exception unused) {
                return -800;
            }
        } while (i10 <= 20);
        return -4096;
    }

    public int encodeNextFrame() {
        try {
            int iDequeueInputBuffer = this.encoder_.dequeueInputBuffer(-1L);
            this.inputBuffers_[iDequeueInputBuffer].rewind();
            this.inputBuffers_[iDequeueInputBuffer].put(this.data_, 0, this.dataSize_);
            this.encoder_.queueInputBuffer(iDequeueInputBuffer, 0, this.dataSize_, this.timestamp_, 0);
            this.timestamp_ = (int) (((double) this.timestamp_) + (1000000.0d / ((double) this.framerate_)));
            int iDequeueOutputBuffer = this.encoder_.dequeueOutputBuffer(new MediaCodec.BufferInfo(), 10000L);
            if (iDequeueOutputBuffer >= 0) {
                int iLimit = this.outputBuffers_[iDequeueOutputBuffer].limit();
                this.encoder_.releaseOutputBuffer(iDequeueOutputBuffer, false);
                return iLimit;
            }
            if (iDequeueOutputBuffer == -3) {
                this.outputBuffers_ = this.encoder_.getOutputBuffers();
                MessageCtx.getInstance().Log(TAG, "encodeNextFrame : INFO_OUTPUT_BUFFERS_CHANGED");
                return 0;
            }
            if (iDequeueOutputBuffer == -2) {
                this.format_ = this.encoder_.getOutputFormat();
                MessageCtx.getInstance().Log(TAG, "encodeNextFrame : INFO_OUTPUT_FORMAT_CHANGED");
                return 0;
            }
            if (iDequeueOutputBuffer == -1) {
                MessageCtx.getInstance().Log(TAG, "encodeNextFrame : INFO_TRY_AGAIN_LATER");
                return 0;
            }
            MessageCtx.getInstance().Log(TAG, "encodeNextFrame : failure : " + iDequeueOutputBuffer);
            return iDequeueOutputBuffer;
        } catch (Exception e10) {
            MessageCtx.getInstance().Log(TAG, "encodeNextFrame : queue/dequeue : " + e10.getMessage());
            return -99;
        }
    }
}
