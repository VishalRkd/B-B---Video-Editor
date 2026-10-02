package xiaoying.basedef;

/* JADX INFO: loaded from: classes18.dex */
public class QRectFloat {
    public float bottom;
    public float left;
    public float right;
    public float top;

    public QRectFloat() {
        this.left = 0.0f;
        this.top = 0.0f;
        this.right = 0.0f;
        this.bottom = 0.0f;
    }

    public boolean equals(float left, float top, float right, float bottom) {
        return this.left == left && this.top == top && this.right == right && this.bottom == bottom;
    }

    public void negate() {
        this.left = -this.left;
        this.top = -this.top;
        this.right = -this.right;
        this.bottom = -this.bottom;
    }

    public void offset(float dx, float dy) {
        this.left += dx;
        this.right += dx;
        this.top += dy;
        this.bottom += dy;
    }

    public void set(float left, float top, float right, float bottom) {
        this.left = left;
        this.top = top;
        this.right = right;
        this.bottom = bottom;
    }

    public boolean equals(Object o10) {
        if (!(o10 instanceof QRectFloat)) {
            return false;
        }
        QRectFloat qRectFloat = (QRectFloat) o10;
        return this.left == qRectFloat.left && this.top == qRectFloat.top && this.right == qRectFloat.right && this.bottom == qRectFloat.bottom;
    }

    public QRectFloat(float left, float top, float right, float bottom) {
        this.left = left;
        this.top = top;
        this.right = right;
        this.bottom = bottom;
    }

    public QRectFloat(QRectFloat r10) {
        this.left = r10.left;
        this.top = r10.top;
        this.right = r10.right;
        this.bottom = r10.bottom;
    }
}
