package xiaoying.basedef;

/* JADX INFO: loaded from: classes18.dex */
public class QRange {
    public int length;
    public int start;

    public QRange() {
        this.start = 0;
        this.length = 0;
    }

    public void copy(QRange src) {
        this.length = src.length;
        this.start = src.start;
    }

    public boolean equals(QRange range) {
        return this.start == range.start && this.length == range.length;
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
