package xiaoying.engine.producer;

import xiaoying.engine.QEngine;
import xiaoying.engine.aecomp.QAECompStream;
import xiaoying.engine.base.IQSessionStateListener;
import xiaoying.engine.base.QSession;
import xiaoying.engine.base.QSessionStream;
import xiaoying.engine.base.QVEError;

/* JADX INFO: loaded from: classes19.dex */
public class QProducer extends QSession {
    public static final int CPU_OVERLOAD_LEVEL_HIGH = 3;
    public static final int CPU_OVERLOAD_LEVEL_LOW = 1;
    public static final int CPU_OVERLOAD_LEVEL_MEDIUM = 2;
    private static final int PROP_BASE = 24576;
    public static final int PROP_CALLBACK_INTERVAL = 24583;
    public static final int PROP_ERR_INFO = 24584;
    public static final int PROP_EXP_LEN = 24579;
    public static final int PROP_META_DATA = 24594;
    public static final int PROP_PARAM = 24577;
    public static final int PROP_REVERSE = 24578;
    public static final int PROP_REVERSE_AUDIO_DISABLE = 24586;
    public static final int PROP_USE_GIF_ENCODER = 24580;
    public static final int PROP_USE_INPUT_FILE_NAME = 24582;
    public static final int PROP_USE_WEBP_ENCODER = 24585;
    private QProducerCreateParam producerCreateParam = new QProducerCreateParam();

    public static class QProducerCreateParam {
        public boolean bReverseMode = false;
        public boolean bGifEncoder = false;
        public boolean bWebpEncoder = false;
    }

    public static class QProducerErrInfo {
        public int mErrTime = 0;
        public int mAPrcErr = 0;
        public int mVDecErr = 0;
        public int mVPrcErr = 0;
        public boolean mbTransition = false;
        public int mLeftClipIndex = 0;
        public int mRightClipIndex = 0;
        public int mClipIndex = 0;
        public boolean mHWException = false;
    }

    private native int nativeActiveStream(long handle, QAECompStream stream);

    private native int nativeActiveStream(long handle, QSessionStream stream);

    private native int nativeCancel(long handle);

    private native int nativeCreate(QEngine engine, QProducer producer, QProducerCreateParam producerCreate);

    private native int nativeDeActiveStream(long handle);

    private native int nativeDestroy(QProducer producer);

    private native int nativePause(long handle);

    private native int nativeResume(long handle);

    private native int nativeSetCpuOverloadLevel(long handle, int cpuOverloadLevel);

    private native int nativeSetThreadPriority(long handle, int iPriority);

    private native int nativeStart(long handle);

    private native int nativeStop(long handle);

    public int activeStream(QSessionStream stream) {
        long j10 = this.handle;
        return 0 == j10 ? QVEError.QERR_COMMON_JAVA_NOT_INIT : nativeActiveStream(j10, stream);
    }

    public int cancel() {
        long j10 = this.handle;
        return 0 == j10 ? QVEError.QERR_COMMON_JAVA_NOT_INIT : nativeCancel(j10);
    }

    public int deactiveStream() {
        long j10 = this.handle;
        return 0 == j10 ? QVEError.QERR_COMMON_JAVA_NOT_INIT : nativeDeActiveStream(j10);
    }

    @Override // xiaoying.engine.base.QSession
    public int init(QEngine engine, IQSessionStateListener listener) {
        super.init(engine, listener);
        return nativeCreate(engine, this, this.producerCreateParam);
    }

    public int pause() {
        long j10 = this.handle;
        return 0 == j10 ? QVEError.QERR_COMMON_JAVA_NOT_INIT : nativePause(j10);
    }

    public int resume() {
        long j10 = this.handle;
        return 0 == j10 ? QVEError.QERR_COMMON_JAVA_NOT_INIT : nativeResume(j10);
    }

    public int setCPUOverloadLevel(int cpuOverloadLevel) {
        if (cpuOverloadLevel < 1 || cpuOverloadLevel > 3) {
            return QVEError.QERR_COMMON_JAVA_INVALID_PARAM;
        }
        long j10 = this.handle;
        return 0 == j10 ? QVEError.QERR_COMMON_JAVA_INVALID_PARAM : nativeSetCpuOverloadLevel(j10, cpuOverloadLevel);
    }

    @Override // xiaoying.engine.base.QSession
    public int setProperty(int propertyID, Object data) {
        if (propertyID == 24578) {
            this.producerCreateParam.bReverseMode = ((Boolean) data).booleanValue();
        } else if (propertyID == 24580) {
            this.producerCreateParam.bGifEncoder = ((Boolean) data).booleanValue();
        } else if (propertyID == 24585) {
            this.producerCreateParam.bWebpEncoder = ((Boolean) data).booleanValue();
        }
        return super.setProperty(propertyID, data);
    }

    public int start() {
        long j10 = this.handle;
        return 0 == j10 ? QVEError.QERR_COMMON_JAVA_NOT_INIT : nativeStart(j10);
    }

    public int stop() {
        long j10 = this.handle;
        return 0 == j10 ? QVEError.QERR_COMMON_JAVA_NOT_INIT : nativeStop(j10);
    }

    @Override // xiaoying.engine.base.QSession
    public int unInit() {
        return 0 == this.handle ? QVEError.QERR_COMMON_JAVA_NOT_INIT : nativeDestroy(this);
    }

    public int activeStream(QAECompStream stream) {
        long j10 = this.handle;
        return 0 == j10 ? QVEError.QERR_COMMON_JAVA_NOT_INIT : nativeActiveStream(j10, stream);
    }
}
