package xiaoying.utils;

import java.util.ArrayDeque;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/* JADX INFO: loaded from: classes19.dex */
class EvtQueue {
    private final Condition cond;
    private ArrayDeque<Evt> evt_list;
    private final Lock lock;
    private final Condition ntf;
    private final Lock ntfLock;
    private ArrayDeque<Evt> ntf_list;

    public class Evt {
        public int code;

        public Evt(int c10) {
            this.code = c10;
        }
    }

    public EvtQueue() {
        ReentrantLock reentrantLock = new ReentrantLock();
        this.lock = reentrantLock;
        this.cond = reentrantLock.newCondition();
        ReentrantLock reentrantLock2 = new ReentrantLock();
        this.ntfLock = reentrantLock2;
        this.ntf = reentrantLock2.newCondition();
        this.evt_list = new ArrayDeque<>();
        this.ntf_list = new ArrayDeque<>();
    }

    public void notify(int code) {
        this.ntfLock.lock();
        this.ntf_list.add(new Evt(code));
        this.ntf.signalAll();
        this.ntfLock.unlock();
    }

    public void sendAction(int ac2) {
        this.lock.lock();
        this.evt_list.add(new Evt(ac2));
        this.cond.signalAll();
        this.lock.unlock();
    }

    public int waitAction() {
        this.lock.lock();
        while (this.evt_list.isEmpty()) {
            try {
                this.cond.await();
            } catch (Exception unused) {
            }
        }
        Evt evtRemove = this.evt_list.remove();
        this.lock.unlock();
        return evtRemove.code;
    }

    public int waitNotify() {
        this.ntfLock.lock();
        while (this.ntf_list.isEmpty()) {
            try {
                this.ntf.await();
            } catch (Exception unused) {
            }
        }
        Evt evtRemove = this.ntf_list.remove();
        this.ntfLock.unlock();
        return evtRemove.code;
    }
}
