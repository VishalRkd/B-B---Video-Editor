package xiaoying.engine.base.pcm;

/* JADX INFO: loaded from: classes19.dex */
public class QPCMECallbackData {
    public static final int STATUS_NONE = 0;
    public static final int STATUS_PAUSED = 3;
    public static final int STATUS_RUNNING = 2;
    public static final int STATUS_STOPPED = 4;
    public int status = 0;
    public int dataType = 0;
    public QPCMEDataFloat floatData = null;
    public int processedlen = 0;
    public int totalDuration = 0;
    public int errCode = 0;
}
