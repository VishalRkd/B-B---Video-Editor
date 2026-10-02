package xiaoying.engine.base.monitor;

/* JADX INFO: loaded from: classes19.dex */
public class QMonitor {
    public static final int PROP_APPEND_MODULE = 5;
    public static final int PROP_EXTERNAL_LOGGER = 2;
    public static final int PROP_LOG_LEVEL = 1;
    public static final int PROP_NONE = 0;
    public static final int PROP_REMOVE_MODULE = 6;
    public static final int PROP_SET_MODULE = 4;
    public static final int PROP_USE_EXTERNAL_LOGGGER = 3;
    private static long mNative;
    private static int mRefCnt;
    private static QMonitor mSingleton;

    private QMonitor() {
    }

    public static synchronized QMonitor createInstance() throws Exception {
        QMonitor qMonitor;
        try {
            qMonitor = mSingleton;
            if (qMonitor != null) {
                mRefCnt++;
            } else {
                if (0 != mNative) {
                    throw new Exception("--==QMonitor==-- When new instance: mSingleton = null, but mNative != null");
                }
                QMonitor qMonitor2 = new QMonitor();
                long jNativeCreateInstance = nativeCreateInstance();
                mNative = jNativeCreateInstance;
                if (0 == jNativeCreateInstance) {
                    throw new Exception("--==QMonitor==-- When new instance: nativeCreateInstance return null pointer");
                }
                mRefCnt++;
                qMonitor = qMonitor2;
            }
        } catch (Throwable th2) {
            throw th2;
        }
        return qMonitor;
    }

    public static synchronized void destoryIntance() {
        int i10 = mRefCnt;
        if (i10 <= 0) {
            return;
        }
        int i11 = i10 - 1;
        mRefCnt = i11;
        if (i11 == 0) {
            nativeDestoryInstance(mNative);
            mNative = 0L;
            mSingleton = null;
        }
    }

    private static native long nativeCreateInstance();

    private static native void nativeDestoryInstance(long hNative);

    private native int nativeSetProp(long hNative, int propID, Object propData);

    public synchronized int setProp(int propID, Object propData) {
        return nativeSetProp(mNative, propID, propData);
    }
}
