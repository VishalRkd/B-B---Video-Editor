package xiaoying.engine.base;

import xiaoying.engine.QEngine;

/* JADX INFO: loaded from: classes19.dex */
public abstract class QSession {
    public static final int STATUS_INITIALIZING = 5;
    public static final int STATUS_NONE = 0;
    public static final int STATUS_PAUSED = 3;
    public static final int STATUS_READY = 1;
    public static final int STATUS_RUNNING = 2;
    public static final int STATUS_STOPPED = 4;
    private long jniglobalobjectref = 0;
    private QSessionState state = null;
    protected IQSessionStateListener listener = null;
    protected QEngine engine = null;
    protected long handle = 0;
    protected long aehandle = 0;
    protected int frameworkVersion = 327680;

    private native Object nativeGetProp(long sessionHandle, int propertyID);

    private native Object nativeGetState(long sessionHandle);

    private native int nativeSetProp(long sessionHandle, int propertyID, Object data);

    private int onSessionStatus(QSessionState state) {
        IQSessionStateListener iQSessionStateListener = this.listener;
        if (iQSessionStateListener == null) {
            return 0;
        }
        return iQSessionStateListener.onSessionStatus(state);
    }

    public int getFrameWorkVersion() {
        return this.frameworkVersion;
    }

    public Object getProperty(int propertyID) {
        return nativeGetProp(this.handle, propertyID);
    }

    public Object getState() {
        return nativeGetState(this.handle);
    }

    public int init(QEngine engine, IQSessionStateListener listener) {
        if (engine == null) {
            return QVEError.QERR_COMMON_JAVA_INVALID_PARAM;
        }
        this.engine = engine;
        this.listener = listener;
        Object prop112 = engine.getProperty(112);
        this.frameworkVersion = (prop112 instanceof Integer) ? ((Integer) prop112).intValue() : 0;
        return 0;
    }

    public int setProperty(int propertyID, Object data) {
        return nativeSetProp(this.handle, propertyID, data);
    }

    public void setSessionStateListener(IQSessionStateListener listener) {
        this.listener = listener;
    }

    public abstract int unInit();
}
