package xiaoying.engine.base.wmd;

/* JADX INFO: loaded from: classes19.dex */
public class QWMDCallbackData {
    public static final int STATUS_NONE = 0;
    public static final int STATUS_PAUSED = 3;
    public static final int STATUS_RUNNING = 2;
    public static final int STATUS_STOPPED = 4;
    public int status = 0;
    public int startTimePos = 0;
    public int curTimePos = 0;
    public int timeLength = 0;
    public int detectActionCnt = 0;
    public String wmCode = null;
    public int dbgRunErr = 0;
    public int dbgWMErr = 0;
}
