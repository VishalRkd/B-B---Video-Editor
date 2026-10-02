package xiaoying.utils;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.os.Process;
import java.util.Random;
import java.util.concurrent.LinkedBlockingQueue;

/* JADX INFO: loaded from: classes19.dex */
public class WorkThread extends HandlerThread {
    public static final int TASK_EVENT_TASK_CANCEL = -1;
    public static final int TASK_EVENT_TASK_DONE = -4;
    public static final int TASK_EVENT_TASK_EXIST = -3;
    public static final int TASK_EVENT_TASK_FAILED = -5;
    public static final int TASK_EVENT_TASK_REMAIN = -2;
    public static final int WORKTHREAD_IDLE_MODE_SLEEPING = 0;
    public static final int WORKTHREAD_IDLE_MODE_WAITING = 1;
    private static final String WORKTHREAD_QUIT_TAG = "WorkThreadTag@Quit";
    protected final WorkThreadCB mEventCB;
    private Handler mHandler;
    private int mOSThreadPriority;
    private LinkedBlockingQueue<WorkThreadTaskItem> mWorkItemQueue;
    private boolean mbReqThreadExit;

    public interface WorkThreadCB {
        void onClearTask(WorkThreadTaskItem objTask) throws Exception;

        void onEvent(int nEventID, int wParam, int lParam, WorkThreadTaskItem obj) throws Exception;
    }

    public WorkThread(WorkThreadCB cb2, String name) {
        super("WT@" + name + new Random().nextInt());
        this.mbReqThreadExit = false;
        this.mOSThreadPriority = 0;
        this.mHandler = null;
        this.mWorkItemQueue = null;
        this.mEventCB = cb2;
        this.mWorkItemQueue = new LinkedBlockingQueue<>();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void clear() {
        WorkThreadTaskItem workThreadTaskItemRemoveHeadTask;
        while (this.mWorkItemQueue.size() > 0 && (workThreadTaskItemRemoveHeadTask = removeHeadTask()) != null) {
            try {
                this.mEventCB.onEvent(-1, 0, 0, workThreadTaskItemRemoveHeadTask);
                workThreadTaskItemRemoveHeadTask.done();
            } catch (Throwable unused) {
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void processOneTask(WorkThreadTaskItem taskOnGoing) {
        try {
            try {
                this.mEventCB.onClearTask(taskOnGoing);
                this.mEventCB.onEvent(-4, 0, 0, taskOnGoing);
            } catch (Throwable th2) {
                try {
                    taskOnGoing.taskResultObj = th2;
                    this.mEventCB.onEvent(-5, 0, 0, taskOnGoing);
                } catch (Throwable th3) {
                    try {
                        this.mEventCB.onEvent(-4, 0, 0, taskOnGoing);
                        taskOnGoing.done();
                    } catch (Throwable unused) {
                    }
                    throw th3;
                }
            }
            taskOnGoing.done();
        } catch (Throwable unused2) {
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public WorkThreadTaskItem removeHeadTask() {
        try {
            return this.mWorkItemQueue.take();
        } catch (InterruptedException e10) {
            e10.printStackTrace();
            return null;
        }
    }

    public boolean addTask(String strTag, WorkThreadTaskItem obj) {
        if (obj == null) {
            return false;
        }
        try {
            obj.strTag = strTag;
            this.mWorkItemQueue.put(obj);
            Handler handler = this.mHandler;
            if (handler == null) {
                return true;
            }
            handler.sendEmptyMessage(0);
            return true;
        } catch (InterruptedException e10) {
            LogUtils.m173089e("WorkThread", e10.getMessage());
            e10.printStackTrace();
            return false;
        }
    }

    public int getTaskCount() {
        return this.mWorkItemQueue.size();
    }

    @Override // java.lang.Thread
    public void interrupt() {
        addTask(WORKTHREAD_QUIT_TAG, new WorkThreadTaskItem());
        this.mbReqThreadExit = true;
        try {
            join();
            super.interrupt();
        } catch (InterruptedException e10) {
            e10.printStackTrace();
        }
    }

    public void onPause() {
    }

    public void onResume() {
    }

    public void setIdleMode(int nMode) {
    }

    public void setOSThreadPriority(int nPriority) {
        this.mOSThreadPriority = nPriority;
    }

    @Override // java.lang.Thread
    public synchronized void start() {
        this.mbReqThreadExit = false;
        super.start();
        this.mHandler = new Handler(getLooper()) { // from class: xiaoying.utils.WorkThread.1
            @Override // android.os.Handler
            public void handleMessage(Message msg) {
                try {
                    Process.setThreadPriority(WorkThread.this.mOSThreadPriority);
                } catch (Exception unused) {
                }
                int i10 = msg.what;
                if (i10 != 0) {
                    if (i10 != 1) {
                        return;
                    }
                    WorkThread.this.clear();
                    WorkThread.this.quit();
                    return;
                }
                removeMessages(0);
                WorkThreadTaskItem workThreadTaskItemRemoveHeadTask = WorkThread.this.removeHeadTask();
                if (WorkThread.WORKTHREAD_QUIT_TAG.equals(workThreadTaskItemRemoveHeadTask.strTag)) {
                    sendEmptyMessage(1);
                    return;
                }
                WorkThread.this.processOneTask(workThreadTaskItemRemoveHeadTask);
                if (WorkThread.this.getTaskCount() > 0) {
                    sendEmptyMessageDelayed(0, 20L);
                }
            }
        };
    }
}
