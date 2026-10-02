package xiaoying.engine.base;

/* JADX INFO: loaded from: classes19.dex */
public class QRange {
    public static final int LENGTH = 1;
    public static final int START = 0;
    private int length;
    private int start;

    public QRange() {
        this.start = 0;
        this.length = 0;
    }

    public boolean equals(QRange range) {
        return this.start == range.start && this.length == range.length;
    }

    public int get(int field) {
        if (field == 0) {
            return this.start;
        }
        if (field != 1) {
            return -1;
        }
        return this.length;
    }

    public void set(int field, int value) {
        if (field == 0) {
            this.start = value;
        } else {
            if (field != 1) {
                return;
            }
            this.length = value;
        }
    }

    public boolean equals(int start, int length) {
        return this.start == start && this.length == length;
    }

    public QRange(int start, int length) {
        this.start = start;
        this.length = length;
    }

    public QRange(QRange range) {
        this.start = 0;
        this.length = 0;
        this.start = range.start;
        this.length = range.length;
    }
}
