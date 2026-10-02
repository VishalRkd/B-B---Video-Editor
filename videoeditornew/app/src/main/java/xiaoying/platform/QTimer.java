package xiaoying.platform;

import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicInteger;

/* JADX INFO: loaded from: classes19.dex */
public final class QTimer {
    private static AtomicInteger mTimerRefCount = new AtomicInteger();
    private static Timer timer;
    private QTimerTask timerTask = null;

    public class QTimerTask extends TimerTask {
        long timerProc;
        long userData;

        public QTimerTask(long timerProc, long userData) {
            this.timerProc = timerProc;
            this.userData = userData;
        }

        @Override // java.util.TimerTask, java.lang.Runnable
        public void run() {
            try {
                QTimer.this.nativeTimerCallback(this.timerProc, this.userData);
            } catch (Exception unused) {
            }
        }
    }

    public static int create() {
        synchronized (mTimerRefCount) {
            mTimerRefCount.getAndIncrement();
        }
        return 0;
    }

    public static int destroy() {
        Timer timer2;
        synchronized (mTimerRefCount) {
            try {
                if ((mTimerRefCount.get() > 0 ? mTimerRefCount.decrementAndGet() : 0) == 0 && (timer2 = timer) != null) {
                    timer2.cancel();
                    timer = null;
                }
            } catch (Throwable th2) {
                throw th2;
            }
        }
        return 0;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public native void nativeTimerCallback(long timerProc, long userData);

    public synchronized int cancel() {
        QTimerTask qTimerTask = this.timerTask;
        if (qTimerTask != null) {
            qTimerTask.cancel();
            this.timerTask = null;
        }
        return 0;
    }

    public int set(int timeElapsed, long timerProc, long userData) {
        return setEx(timeElapsed, false, timerProc, userData);
    }

    public int setEx(int timeElapsed, boolean repeat, long timerProc, long userData) {
        synchronized (this) {
            try {
                QTimerTask qTimerTask = this.timerTask;
                if (qTimerTask != null) {
                    qTimerTask.cancel();
                    this.timerTask = null;
                }
                this.timerTask = new QTimerTask(timerProc, userData);
            } catch (Throwable th2) {
                throw th2;
            }
        }
        synchronized (mTimerRefCount) {
            try {
                if (mTimerRefCount.get() <= 0) {
                    return 0;
                }
                if (timer == null) {
                    timer = new Timer();
                }
                try {
                    if (repeat) {
                        long j10 = timeElapsed;
                        timer.schedule(this.timerTask, j10, j10);
                    } else {
                        timer.schedule(this.timerTask, timeElapsed);
                    }
                } catch (Exception unused) {
                }
                return 0;
            } catch (Throwable th3) {
                throw th3;
            }
        }
    }
}
