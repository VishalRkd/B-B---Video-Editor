package xiaoying.engine.base.sd;

/* JADX INFO: loaded from: classes19.dex */
public class QSingDetectorCallbackData {
    public static final int STATUS_NONE = 0;
    public static final int STATUS_PAUSED = 3;
    public static final int STATUS_RUNNING = 2;
    public static final int STATUS_STOPPED = 4;
    public int total = 0;
    public int curPos = 0;
    public int status = 0;
    public int err = 0;
    public int offset = 0;
    public int resultCnt = 0;
    public int[] startList = null;
    public int[] endList = null;
}
