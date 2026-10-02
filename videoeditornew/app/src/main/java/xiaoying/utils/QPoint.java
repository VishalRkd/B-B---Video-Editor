package xiaoying.utils;

/* JADX INFO: loaded from: classes19.dex */
public final class QPoint {

        public int x;

        public int y;

    public QPoint() {
        this.x = 0;
        this.y = 0;
    }

    public boolean equals(int x10, int y10) {
        return this.x == x10 && this.y == y10;
    }

    public void negate() {
        this.x = -this.x;
        this.y = -this.y;
    }

    public void offset(int dx, int dy) {
        this.x += dx;
        this.y += dy;
    }

    public void set(int x10, int y10) {
        this.x = x10;
        this.y = y10;
    }

    public boolean equals(Object o10) {
        if (!(o10 instanceof QPoint)) {
            return false;
        }
        QPoint qPoint = (QPoint) o10;
        return this.x == qPoint.x && this.y == qPoint.y;
    }

    public QPoint(int x10, int y10) {
        this.x = x10;
        this.y = y10;
    }

    public QPoint(QPoint src) {
        this.x = src.x;
        this.y = src.y;
    }
}
