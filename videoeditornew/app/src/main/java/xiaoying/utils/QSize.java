package xiaoying.utils;

/* JADX INFO: loaded from: classes19.dex */
public class QSize {
    public int mHeight;
    public int mWidth;

    public QSize() {
        this.mWidth = 0;
        this.mHeight = 0;
    }

    public void copy(QSize size) {
        this.mWidth = size.mWidth;
        this.mHeight = size.mHeight;
    }

    public QSize(int w10, int h10) {
        this.mWidth = w10;
        this.mHeight = h10;
    }
}
