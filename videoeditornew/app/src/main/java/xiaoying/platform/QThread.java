package xiaoying.platform;

import android.os.Process;

/* JADX INFO: loaded from: classes19.dex */
public final class QThread {
    public static final int QTHREAD_PRIORITY_ABOVE_NORMAL = 1;
    public static final int QTHREAD_PRIORITY_BELOW_LOWEST = -2;
    public static final int QTHREAD_PRIORITY_BELOW_NORMAL = -1;
    public static final int QTHREAD_PRIORITY_HIGHEST = 2;
    public static final int QTHREAD_PRIORITY_NORMAL = 0;
    Thread hThread;
    private String mName;
    private int mPriority;
    private boolean mbNeedSetPriority;

    public QThread() {
        this.hThread = null;
        this.mName = null;
        this.mPriority = 0;
        this.mbNeedSetPriority = true;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public native int nativeThreadProc(long threadProc, long userData);

    public int create(final long threadProc, final long userData) {
        Runnable runnable = new Runnable() { // from class: xiaoying.platform.QThread.1
            @Override // java.lang.Runnable
            public void run() {
                if (QThread.this.mbNeedSetPriority) {
                    try {
                        Process.setThreadPriority(QThread.this.mPriority);
                    } catch (Throwable th2) {
                        th2.printStackTrace();
                    }
                    QThread.this.mbNeedSetPriority = false;
                }
                try {
                    QThread.this.nativeThreadProc(threadProc, userData);
                } catch (Throwable th3) {
                    th3.printStackTrace();
                }
            }
        };
        if (this.mName != null) {
            this.hThread = new Thread(runnable, this.mName);
        } else {
            this.hThread = new Thread(runnable);
        }
        this.hThread.start();
        return 0;
    }

    public int destroy() {
        return 0;
    }

    public int exit() {
        Thread thread = this.hThread;
        if (thread == null) {
            return 0;
        }
        if (thread.isAlive()) {
            try {
                this.hThread.join();
            } catch (Exception unused) {
            }
        }
        this.hThread = null;
        return 0;
    }

    public synchronized int resume() {
        return 0;
    }

    public int setPriority(int nPriority) {
        int i10;
        if (nPriority != -2) {
            i10 = -1;
            if (nPriority == -1) {
                i10 = 1;
            } else if (nPriority != 1) {
                i10 = nPriority != 2 ? 0 : -8;
            }
        } else {
            i10 = 10;
        }
        this.mPriority = i10;
        this.mbNeedSetPriority = true;
        if (this.hThread == Thread.currentThread()) {
            try {
                Process.setThreadPriority(this.mPriority);
            } catch (Exception unused) {
            }
        }
        return 0;
    }

    public int sleep(int sleepTime) {
        try {
            Thread.sleep(sleepTime);
            return 0;
        } catch (Exception unused) {
            return 0;
        }
    }

    public synchronized int suspend() {
        return 0;
    }

    public QThread(String name) {
        this.hThread = null;
        this.mPriority = 0;
        this.mbNeedSetPriority = true;
        this.mName = name;
    }
}
