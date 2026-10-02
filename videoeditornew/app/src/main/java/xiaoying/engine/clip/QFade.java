package xiaoying.engine.clip;

/* JADX INFO: loaded from: classes19.dex */
public class QFade {
    public static final int DURATION = 0;
    public static final int END_PERCENT = 2;
    public static final int START_PERCENT = 1;
    private int duration;
    private int endPercent;
    private int startPercent;

    public QFade() {
        this.duration = 0;
        this.startPercent = 0;
        this.endPercent = 0;
    }

    public int get(int field) {
        if (field == 0) {
            return this.duration;
        }
        if (field == 1) {
            return this.startPercent;
        }
        if (field != 2) {
            return -1;
        }
        return this.endPercent;
    }

    public void set(int field, int value) {
        if (field == 0) {
            this.duration = value;
        } else if (field == 1) {
            this.startPercent = value;
        } else {
            if (field != 2) {
                return;
            }
            this.endPercent = value;
        }
    }

    public QFade(QFade fade) {
        this.duration = 0;
        this.startPercent = 0;
        this.endPercent = 0;
        this.duration = fade.duration;
        this.startPercent = fade.startPercent;
        this.endPercent = fade.endPercent;
    }

    public QFade(int duration, int startPercent, int endPercent) {
        this.duration = duration;
        this.startPercent = startPercent;
        this.endPercent = endPercent;
    }
}
