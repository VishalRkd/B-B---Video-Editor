package xiaoying.engine.clip;

import xiaoying.utils.QBezierCurve;

/* JADX INFO: loaded from: classes19.dex */
public class QKeyFrameTransformData {
    public Value[] values = null;
    public int baseX = 0;
    public int baseY = 0;
    public float baseRotation = 0.0f;
    public float baseWidthRatio = 1.0f;
    public float baseHeightRatio = 1.0f;

    public static class EasingInfo {
        public QBezierCurve[] curves;

                public long id;
    }

    public static class ExtInfo {
        public int frontX = 0;
        public int frontY = 0;
        public int backX = 0;
        public int backY = 0;
    }

    public static class Value {

                public int ts = 0;

                public int x = 0;

                public int y = 0;
        public float widthRatio = 0.0f;
        public float heightRatio = 0.0f;
        public float rotation = 0.0f;
        public int method = 0;
        public long templateID = 0;
        public int lineMode = 0;
        public ExtInfo extInfo = null;
    }
}
