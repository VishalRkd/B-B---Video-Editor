package xiaoying.engine.base;

/* JADX INFO: loaded from: classes18.dex */
public class QBenchLogger {
    private static QBenchLogger logger = new QBenchLogger();

    private QBenchLogger() {
    }

    public static QBenchLogger getInstance() {
        return logger;
    }

    private static native void nativeSetActive(boolean bval);

    public void setActive(boolean active) {
        nativeSetActive(active);
    }
}
