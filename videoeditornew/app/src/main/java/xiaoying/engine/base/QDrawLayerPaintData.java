package xiaoying.engine.base;

import xiaoying.basedef.QPointFloat;

/* JADX INFO: loaded from: classes19.dex */
public class QDrawLayerPaintData {
    public static final int DRAE_LAYER_SHAPE_TYPE_LINE = 1;
    public static final int DRAE_LAYER_SHAPE_TYPE_TEXT = 5;
    public boolean bPaintStatus = false;
    public QDrawLayerShapeBaseType[] data = null;
    public float fAlpha = 0.0f;
    public boolean bDrawEnd = false;

    public static class QDrawLayerShapeBaseType {
        protected int nType = 0;
        public int nGroupId = 0;
        public int nShapeId = 0;
    }

    public static class QDrawLayerShapeLineType extends QDrawLayerShapeBaseType {
        public QDrawLayerPaintPen penType;
        public QPointFloat[] vecPoint;

        public QDrawLayerShapeLineType() {
            this.nType = 1;
            this.vecPoint = null;
            this.penType = null;
        }
    }

    public static class QDrawLayerShapeTextType extends QDrawLayerShapeBaseType {
        public QDrawLayerPaintText textType;
        public QPointFloat[] vecPoint;

        public QDrawLayerShapeTextType() {
            this.nType = 5;
            this.vecPoint = null;
            this.textType = null;
        }
    }
}
