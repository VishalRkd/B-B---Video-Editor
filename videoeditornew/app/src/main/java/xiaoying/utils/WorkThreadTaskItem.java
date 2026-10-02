package xiaoying.utils;

import java.util.concurrent.LinkedBlockingQueue;

/* JADX INFO: loaded from: classes19.dex */
public class WorkThreadTaskItem {
    public boolean bSync;
    public long lStartTimeStamp;
    private final LinkedBlockingQueue<Object> mMessageQueue;
    public int nTaskResultCode;
    public int nTaskType;
    public String strTag;
    public Object taskParamObj;
    public Object taskResultObj;

    public WorkThreadTaskItem() {
        this(false);
        this.bSync = false;
    }

    public void done() {
        LinkedBlockingQueue<Object> linkedBlockingQueue = this.mMessageQueue;
        if (linkedBlockingQueue != null) {
            try {
                linkedBlockingQueue.put(this);
            } catch (InterruptedException e10) {
                e10.printStackTrace();
            }
        }
    }

    public boolean isSyncTask() {
        return this.mMessageQueue != null;
    }

    public void waitDone() {
        LinkedBlockingQueue<Object> linkedBlockingQueue = this.mMessageQueue;
        if (linkedBlockingQueue != null) {
            try {
                linkedBlockingQueue.take();
            } catch (InterruptedException e10) {
                e10.printStackTrace();
            }
        }
    }

    public WorkThreadTaskItem(boolean bSyncTask) {
        this.nTaskType = -1;
        this.lStartTimeStamp = 0L;
        this.strTag = null;
        this.taskResultObj = null;
        this.nTaskResultCode = -1;
        this.taskParamObj = null;
        this.bSync = bSyncTask;
        if (bSyncTask) {
            this.mMessageQueue = new LinkedBlockingQueue<>();
        } else {
            this.mMessageQueue = null;
        }
    }
}
