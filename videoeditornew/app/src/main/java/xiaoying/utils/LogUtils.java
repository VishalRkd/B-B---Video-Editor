package xiaoying.utils;

import java.util.Date;
import java.util.HashMap;

/* JADX INFO: loaded from: classes19.dex */
public class LogUtils {
    public static final String APP_TAG = "AndroidCE";
    public static final int ENTER = 0;
    public static final int EXIT = 1;
    public static final int LOG_LEVEL_ALL = 31;
    public static final int LOG_LEVEL_DBG = 8;
    public static final int LOG_LEVEL_ERR = 1;
    public static final int LOG_LEVEL_INFO = 4;
    public static final int LOG_LEVEL_VERBOSE = 16;
    public static final int LOG_LEVEL_WARN = 2;
    public static final int NONE = 0;
    private static final boolean PERFORMANCE_LOG_OPEN = true;
    public static HashMap<String, Long> mTimeStamp = new HashMap<>();
    private static int mlogLevel = 15;

    public static void LOGP(String tag, String msg, int param) {
        HashMap<String, Long> map = mTimeStamp;
        if (map == null) {
            return;
        }
        if (param == 0) {
            StringBuilder sb2 = new StringBuilder();
            sb2.append("Performance log:");
            sb2.append(msg);
            sb2.append("<---");
            sb2.append(String.format(String.format("%1$tY-%1$tm-%1$td %1$tH:%1$tM:%1$tS %1$tL", new Date()), new Object[0]));
            mTimeStamp.put(msg, Long.valueOf(System.currentTimeMillis()));
            return;
        }
        if (map.get(msg) != null) {
            StringBuilder sb3 = new StringBuilder();
            sb3.append("Performance log:");
            sb3.append(msg);
            sb3.append("--->");
            sb3.append(String.format(String.format("%1$tY-%1$tm-%1$td %1$tH:%1$tM:%1$tS %1$tL", new Date()), new Object[0]));
            sb3.append(" cost:");
            sb3.append(System.currentTimeMillis() - mTimeStamp.get(msg).longValue());
            sb3.append("ms");
            mTimeStamp.remove(msg);
        }
    }

        public static void m173088d(String tag, String msg) {
        if ((mlogLevel & 8) != 0) {
            StringBuilder sb2 = new StringBuilder();
            sb2.append(tag);
            sb2.append(":");
            sb2.append(msg);
        }
    }

        public static void m173089e(String tag, String msg) {
        if ((mlogLevel & 1) != 0) {
            StringBuilder sb2 = new StringBuilder();
            sb2.append(tag);
            sb2.append(":");
            sb2.append(msg);
        }
    }

    public static void enalbeLog(int nLogLevel) {
        mlogLevel = nLogLevel;
    }

        public static void m173090i(String tag, String msg) {
        if ((mlogLevel & 4) != 0) {
            StringBuilder sb2 = new StringBuilder();
            sb2.append(tag);
            sb2.append(":");
            sb2.append(msg);
        }
    }

        public static void m173091v(String tag, String msg) {
        if ((mlogLevel & 16) != 0) {
            StringBuilder sb2 = new StringBuilder();
            sb2.append(tag);
            sb2.append(":");
            sb2.append(msg);
        }
    }
}
