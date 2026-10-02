package xiaoying.utils;

import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaCodecList;
import android.media.MediaCrypto;
import android.media.MediaFormat;
import android.os.Build;
import android.os.Bundle;
import android.view.Surface;
import java.nio.ByteBuffer;

/* JADX INFO: loaded from: classes19.dex */
public class QMediaCodecUtils {
    private static final int QMediaCodecUtils_ERR1 = 805306369;
    private static final int QMediaCodecUtils_ERR2 = 805306370;
    private static final int QMediaCodecUtils_ERR3 = 805306371;
    private static final int QMediaCodecUtils_ERR4 = 805306372;
    private static final int QMediaCodecUtils_ERR5 = 805306373;
    private static final int QMediaCodecUtils_ERR6 = 805306374;
    private static final int QMediaCodecUtils_NOERR = 0;
    public static final String TAG = "QMediaCodecUtils";
    private MediaCodec mCodec;
    private ByteBuffer[] mInputBuffers;
    private ByteBuffer[] mOutputBuffers;
    private int mSDKVersion;
    private boolean mbDec;
    private volatile boolean mbException = false;
    private volatile int mExceptionCode = 0;
    private int mInputBufIndex = -1;
    private Surface mSurface = null;

    public static String getComponentName(String mime, boolean bEncoder) {
        int codecCount = MediaCodecList.getCodecCount();
        for (int i10 = 0; i10 < codecCount; i10++) {
            MediaCodecInfo codecInfoAt = MediaCodecList.getCodecInfoAt(i10);
            if (codecInfoAt.isEncoder() == bEncoder) {
                for (String str : codecInfoAt.getSupportedTypes()) {
                    if (mime.equals(str)) {
                        return codecInfoAt.getName();
                    }
                }
            }
        }
        return null;
    }

    public static boolean isDecoderSupportColorSpace(int nColor) {
        int codecCount = MediaCodecList.getCodecCount();
        for (int i10 = 0; i10 < codecCount; i10++) {
            MediaCodecInfo codecInfoAt = MediaCodecList.getCodecInfoAt(i10);
            if (!codecInfoAt.isEncoder()) {
                for (String str : codecInfoAt.getSupportedTypes()) {
                    int[] iArr = codecInfoAt.getCapabilitiesForType(str).colorFormats;
                    if (iArr != null) {
                        for (int i11 : iArr) {
                            if (i11 == nColor) {
                                return true;
                            }
                        }
                    }
                }
            }
        }
        return false;
    }

    public int create(MediaFormat mediaFormat, Surface surface, boolean bDecoder, boolean bSW) {
        String string = mediaFormat.getString("mime");
        this.mSDKVersion = Build.VERSION.SDK_INT;
        if (bDecoder) {
            StringBuilder sb2 = new StringBuilder();
            sb2.append("Decoder Mime : ");
            sb2.append(string);
            if (this.mSDKVersion < 16) {
                return QMediaCodecUtils_ERR1;
            }
            try {
                if (!bSW) {
                    this.mCodec = MediaCodec.createDecoderByType(string);
                } else if (string.equals("video/avc")) {
                    this.mCodec = MediaCodec.createByCodecName("OMX.google.h264.decoder");
                } else if (string.equals("video/mp4v-es")) {
                    this.mCodec = MediaCodec.createByCodecName("OMX.google.mpeg4.decoder");
                } else if (string.equals("video/hevc")) {
                    this.mCodec = MediaCodec.createByCodecName("OMX.google.hevc.decoder");
                } else {
                    this.mCodec = MediaCodec.createDecoderByType(string);
                }
            } catch (Exception e10) {
                this.mbException = true;
                this.mExceptionCode = 1;
                StringBuilder sb3 = new StringBuilder();
                sb3.append("create createDecoderByType error ");
                sb3.append(e10.getMessage());
            }
        } else {
            try {
                this.mCodec = MediaCodec.createEncoderByType(string);
            } catch (Exception e11) {
                this.mbException = true;
                this.mExceptionCode = 2;
                StringBuilder sb4 = new StringBuilder();
                sb4.append("create createEncoderByType error ");
                sb4.append(e11.getMessage());
            }
        }
        this.mbDec = bDecoder;
        MediaCodec mediaCodec = this.mCodec;
        if (mediaCodec == null) {
            return QMediaCodecUtils_ERR3;
        }
        try {
            if (bDecoder) {
                mediaCodec.configure(mediaFormat, surface, (MediaCrypto) null, 0);
            } else {
                mediaCodec.configure(mediaFormat, surface, (MediaCrypto) null, 1);
                this.mSurface = this.mCodec.createInputSurface();
            }
            this.mCodec.start();
            if (bDecoder) {
                if (this.mSDKVersion < 21) {
                    ByteBuffer[] inputBuffers = this.mCodec.getInputBuffers();
                    this.mInputBuffers = inputBuffers;
                    if (inputBuffers == null) {
                        return QMediaCodecUtils_ERR4;
                    }
                }
            } else if (this.mSDKVersion < 21) {
                ByteBuffer[] outputBuffers = this.mCodec.getOutputBuffers();
                this.mOutputBuffers = outputBuffers;
                if (outputBuffers == null) {
                    return QMediaCodecUtils_ERR5;
                }
            }
        } catch (Exception e12) {
            this.mbException = true;
            this.mExceptionCode = 3;
            StringBuilder sb5 = new StringBuilder();
            sb5.append("init exception ");
            sb5.append(e12.getMessage());
        }
        return 0;
    }

    public ByteBuffer dequeueInputBuffer() {
        try {
            if (this.mInputBufIndex < 0) {
                this.mInputBufIndex = this.mCodec.dequeueInputBuffer(10000L);
            }
            int i10 = this.mInputBufIndex;
            if (i10 < 0) {
                return null;
            }
            ByteBuffer inputBuffer = this.mSDKVersion < 21 ? this.mInputBuffers[i10] : this.mCodec.getInputBuffer(i10);
            inputBuffer.clear();
            return inputBuffer;
        } catch (Exception e10) {
            synchronized (this) {
                this.mbException = true;
                this.mExceptionCode = 5;
                StringBuilder sb2 = new StringBuilder();
                sb2.append("dequeueInputBuffer exception ");
                sb2.append(e10.getMessage());
                return null;
            }
        }
    }

    public int dequeueOutputBuffer(MediaCodec.BufferInfo bufferInfo, long timeout) {
        try {
            return this.mCodec.dequeueOutputBuffer(bufferInfo, timeout);
        } catch (Exception e10) {
            synchronized (this) {
                this.mbException = true;
                this.mExceptionCode = 8;
                StringBuilder sb2 = new StringBuilder();
                sb2.append("dequeueOutputBuffer exception ");
                sb2.append(e10.getMessage());
                return -1;
            }
        }
    }

    public void flush() {
        try {
            int i10 = this.mInputBufIndex;
            if (i10 != -1) {
                this.mCodec.queueInputBuffer(i10, 0, 0, 0L, 4);
                this.mInputBufIndex = -1;
            }
            this.mCodec.flush();
            this.mInputBufIndex = -1;
        } catch (Exception e10) {
            synchronized (this) {
                this.mbException = true;
                this.mExceptionCode = 7;
                StringBuilder sb2 = new StringBuilder();
                sb2.append("flush exception ");
                sb2.append(e10.getMessage());
            }
        }
    }

    public int getExceptionCode() {
        return this.mExceptionCode;
    }

    public Surface getInputSurface() {
        return this.mSurface;
    }

    public ByteBuffer getOutputBufferByIndex(int index) {
        try {
            return this.mSDKVersion < 21 ? this.mOutputBuffers[index] : this.mCodec.getOutputBuffer(index);
        } catch (Exception e10) {
            synchronized (this) {
                this.mbException = true;
                this.mExceptionCode = 22;
                StringBuilder sb2 = new StringBuilder();
                sb2.append("getOutputBufferByIndex exception ");
                sb2.append(e10.getMessage());
                return null;
            }
        }
    }

    public ByteBuffer[] getOutputBuffers() {
        return this.mOutputBuffers;
    }

    public boolean isException() {
        return this.mbException;
    }

    public int queueInputBuffer(int offset, int size, long presentationTimeUs, int flags) {
        try {
            this.mCodec.queueInputBuffer(this.mInputBufIndex, offset, size, presentationTimeUs, flags);
            this.mInputBufIndex = -1;
            return 0;
        } catch (Exception e10) {
            synchronized (this) {
                this.mbException = true;
                this.mExceptionCode = 6;
                StringBuilder sb2 = new StringBuilder();
                sb2.append("queueInputBuffer exception ");
                sb2.append(e10.getMessage());
                return 0;
            }
        }
    }

    public int regetOutputBuffers() {
        try {
            if (this.mSDKVersion >= 21) {
                return 0;
            }
            ByteBuffer[] outputBuffers = this.mCodec.getOutputBuffers();
            this.mOutputBuffers = outputBuffers;
            if (outputBuffers == null) {
                return QMediaCodecUtils_ERR6;
            }
            return 0;
        } catch (Exception e10) {
            synchronized (this) {
                this.mbException = true;
                this.mExceptionCode = 10;
                StringBuilder sb2 = new StringBuilder();
                sb2.append("regetOutputBuffers exception ");
                sb2.append(e10.getMessage());
                return 0;
            }
        }
    }

    public void release() {
        try {
            MediaCodec mediaCodec = this.mCodec;
            if (mediaCodec != null) {
                mediaCodec.stop();
                this.mCodec.release();
            }
            Surface surface = this.mSurface;
            if (surface != null) {
                surface.release();
            }
        } catch (Exception e10) {
            this.mbException = true;
            this.mExceptionCode = 4;
            StringBuilder sb2 = new StringBuilder();
            sb2.append("release exception ");
            sb2.append(e10.getMessage());
        }
    }

    public void releaseOutputBuffer(int bufferIndex, boolean bRender) {
        try {
            this.mCodec.releaseOutputBuffer(bufferIndex, bRender);
        } catch (Exception e10) {
            synchronized (this) {
                this.mbException = true;
                this.mExceptionCode = 9;
                StringBuilder sb2 = new StringBuilder();
                sb2.append("releaseOutputBuffer exception ");
                sb2.append(e10.getMessage());
            }
        }
    }

    public boolean requestKeyFrame() {
        if (!this.mbDec && this.mSDKVersion >= 19) {
            Bundle bundle = new Bundle();
            bundle.putInt("request-sync", 0);
            try {
                this.mCodec.setParameters(bundle);
            } catch (IllegalStateException unused) {
                return false;
            }
        }
        return true;
    }

    public int reset(MediaFormat mediaFormat, Surface surface) {
        try {
            this.mbException = false;
            this.mExceptionCode = 0;
            this.mCodec.reset();
            if (surface != null) {
                this.mCodec.configure(mediaFormat, surface, (MediaCrypto) null, 0);
                this.mCodec.start();
            }
            return 0;
        } catch (Exception e10) {
            this.mbException = true;
            this.mExceptionCode = 12;
            StringBuilder sb2 = new StringBuilder();
            sb2.append("mediacodec reset exception ");
            sb2.append(e10.getMessage());
            return 1;
        }
    }

    public void setBitrate(long bitrate) {
        Bundle bundle = new Bundle();
        bundle.putInt(MediaCodec.PARAMETER_KEY_VIDEO_BITRATE, (int) bitrate);
        this.mCodec.setParameters(bundle);
    }

    public void signalEndOfInputStream() {
        try {
            MediaCodec mediaCodec = this.mCodec;
            if (mediaCodec != null) {
                mediaCodec.signalEndOfInputStream();
            }
        } catch (Exception e10) {
            synchronized (this) {
                this.mbException = true;
                this.mExceptionCode = 11;
                StringBuilder sb2 = new StringBuilder();
                sb2.append("signalEndOfInputStream exception ");
                sb2.append(e10.getMessage());
            }
        }
    }
}
