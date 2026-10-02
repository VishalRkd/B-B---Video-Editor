package xiaoying.engine.clip;

import xiaoying.utils.QPoint;

/* JADX INFO: loaded from: classes19.dex */
public class QKeyFrameColorCurveData {
    public Value[] values = null;

    public static class OutValue {
        public int[] red = new int[256];
        public int[] green = new int[256];
        public int[] blue = new int[256];
    }

    public static class Value {
        public QPoint[] blue;
        public QPoint[] green;
        public QPoint[] red;
        public QPoint[] rgb;

                public int ts;
    }
}
