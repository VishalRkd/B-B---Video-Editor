package xiaoying.engine.producer;

import xiaoying.engine.base.QVideoInfo;

/* JADX INFO: loaded from: classes19.dex */
public class QProducerState {
    private QVideoInfo srcVideoInfo = null;
    private QVideoInfo dstVideoInfo = null;
    private int state = 0;
    private int currentTime = 0;

    private QProducerState() {
    }

    public int getCurrentTime() {
        return this.currentTime;
    }

    public QVideoInfo getDstVideoInfo() {
        return this.dstVideoInfo;
    }

    public QVideoInfo getSrcVideoInfo() {
        return this.srcVideoInfo;
    }

    public int getState() {
        return this.state;
    }
}
