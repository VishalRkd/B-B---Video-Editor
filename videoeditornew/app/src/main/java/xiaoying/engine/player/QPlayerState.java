package xiaoying.engine.player;

import xiaoying.engine.base.QVideoInfo;

/* JADX INFO: loaded from: classes19.dex */
public class QPlayerState {
    public static final int CURRENT_TIME = 1;
    public static final int PLAYERBACK_MODE = 2;
    public static final int STATUS = 0;
    public static final int VOLUME = 3;
    public int currentTime;
    public int mLastDrawnVFTS = 0;
    public int mLastDrawnVFTSP = 0;
    public boolean muted;
    public int playbackMode;
    public int status;
    public QVideoInfo videoInfo;
    public int volume;

    private QPlayerState() {
    }

    public int get(int field) {
        if (field == 0) {
            return this.status;
        }
        if (field == 1) {
            return this.currentTime;
        }
        if (field == 2) {
            return this.playbackMode;
        }
        if (field != 3) {
            return -1;
        }
        return this.volume;
    }

    public boolean getMuteFlag() {
        return this.muted;
    }

    public QVideoInfo getVideoInfo() {
        return this.videoInfo;
    }
}
