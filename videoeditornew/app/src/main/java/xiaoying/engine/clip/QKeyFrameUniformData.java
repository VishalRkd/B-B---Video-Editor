package xiaoying.engine.clip;

/* JADX INFO: loaded from: classes19.dex */
public class QKeyFrameUniformData {
    public static final int KEYFRAME_TRANSFORM_COMMON_OFFSET_TYPE_MUL = 1;
    public static final int KEYFRAME_TRANSFORM_COMMON_OFFSET_TYPE_PLUS = 0;
    public Value[] values = null;
    public String name = null;

    public static class Value {
        public int offsetOpcodeType;

                public int ts = 0;
        public float offsetValue = 0.0f;
        public double floatValue = 0.0d;
        public int method = 0;
        public long templateID = 0;
        public QKeyFrameTransformData.ExtInfo extInfo = null;
        public QKeyFrameTransformData.EasingInfo easingInfo = null;
    }
}
