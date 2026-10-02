package xiaoying.utils;

/* JADX INFO: loaded from: classes19.dex */
public final class QRect {
    public int bottom;
    public int left;
    public int right;
    public int top;

    public QRect() {
        this.left = 0;
        this.top = 0;
        this.right = 0;
        this.bottom = 0;
    }

    public boolean equals(int left, int top, int right, int bottom) {
        return this.left == left && this.top == top && this.right == right && this.bottom == bottom;
    }

    public void negate() {
        this.left = -this.left;
        this.top = -this.top;
        this.right = -this.right;
        this.bottom = -this.bottom;
    }

    public void offset(int dx, int dy) {
        this.left += dx;
        this.right += dx;
        this.top += dy;
        this.bottom += dy;
    }

    public void set(int left, int top, int right, int bottom) {
        this.left = left;
        this.top = top;
        this.right = right;
        this.bottom = bottom;
    }

    public boolean equals(Object o10) {
        if (!(o10 instanceof QRect)) {
            return false;
        }
        QRect qRect = (QRect) o10;
        return this.left == qRect.left && this.top == qRect.top && this.right == qRect.right && this.bottom == qRect.bottom;
    }

    public QRect(int left, int top, int right, int bottom) {
        this.left = left;
        this.top = top;
        this.right = right;
        this.bottom = bottom;
    }

    public QRect(QRect r10) {
        this.left = r10.left;
        this.top = r10.top;
        this.right = r10.right;
        this.bottom = r10.bottom;
    }
}
