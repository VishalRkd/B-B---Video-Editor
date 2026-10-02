package xiaoying.basedef;

/* JADX INFO: loaded from: classes18.dex */
public class QSizeFloat {

        public float h;

        public float w;

    public QSizeFloat() {
        this.w = 0.0f;
        this.h = 0.0f;
    }

    public QSizeFloat(float w10, float h10) {
        this.w = w10;
        this.h = h10;
    }

    public QSizeFloat(QSizeFloat size) {
        this.w = size.w;
        this.h = size.h;
    }
}
