package xiaoying.engine.base;

/* JADX INFO: loaded from: classes19.dex */
public final class QThreadPool {
    public static int GetThreadPoolIdleThreadCount() {
        return nativeGetThreadPoolIdleThreadCount();
    }

    public static Object[] GetThreadPoolProcessingTaskName() {
        return nativeGetThreadPoolProcessingTaskName();
    }

    public static int GetThreadPoolTotalThreadCount() {
        return nativeGetThreadPoolTotalThreadCount();
    }

    public static Object[] GetThreadPoolWaitingTaskName() {
        return nativeGetThreadPoolWaitingTaskName();
    }

    private static native int nativeGetThreadPoolIdleThreadCount();

    private static native Object[] nativeGetThreadPoolProcessingTaskName();

    private static native int nativeGetThreadPoolTotalThreadCount();

    private static native Object[] nativeGetThreadPoolWaitingTaskName();
}
