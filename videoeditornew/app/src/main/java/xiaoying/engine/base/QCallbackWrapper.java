package xiaoying.engine.base;

/* JADX INFO: loaded from: classes19.dex */
public class QCallbackWrapper {
    public static final int STATUS_CANCEL = 6;
    public static final int STATUS_INITIALIZING = 5;
    public static final int STATUS_NONE = 0;
    public static final int STATUS_PAUSED = 3;
    public static final int STATUS_READY = 1;
    public static final int STATUS_RUNNING = 2;
    public static final int STATUS_STOPPED = 4;
    public IQSessionStateListener listener = null;

    public int callback(QSessionState state) {
        IQSessionStateListener iQSessionStateListener = this.listener;
        if (iQSessionStateListener != null) {
            return iQSessionStateListener.onSessionStatus(state);
        }
        return -1;
    }
}
