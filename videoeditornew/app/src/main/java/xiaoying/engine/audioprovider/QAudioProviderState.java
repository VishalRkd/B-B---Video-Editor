package xiaoying.engine.audioprovider;

/* JADX INFO: loaded from: classes18.dex */
public class QAudioProviderState {
    private int state = 0;
    private int currentTime = 0;

    private QAudioProviderState() {
    }

    public int getCurrentTime() {
        return this.currentTime;
    }

    public int getState() {
        return this.state;
    }
}
