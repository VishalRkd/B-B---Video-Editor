package xiaoying.engine.clip;

/* JADX INFO: loaded from: classes19.dex */
public class QKeyFrameCommonData {
    public static final int KEYFRAME_TRANSFORM_COMMON_OFFSET_TYPE_MUL = 1;
    public static final int KEYFRAME_TRANSFORM_COMMON_OFFSET_TYPE_PLUS = 0;
    public static final int KEYFRAME_TYPE_LINE_CURVE_MODE = 0;
    public static final int KEYFRAME_TYPE_LINE_LINE_MODE = 1;
    public static final int KEY_FRMAE_3D_TYPE_ANCHOR_X = 9;
    public static final int KEY_FRMAE_3D_TYPE_ANCHOR_Y = 10;
    public static final int KEY_FRMAE_3D_TYPE_ANCHOR_Z = 11;
    public static final int KEY_FRMAE_3D_TYPE_ANGLE_X = 6;
    public static final int KEY_FRMAE_3D_TYPE_ANGLE_Y = 7;
    public static final int KEY_FRMAE_3D_TYPE_ANGLE_Z = 8;
    public static final int KEY_FRMAE_3D_TYPE_SCALE_X = 0;
    public static final int KEY_FRMAE_3D_TYPE_SCALE_Y = 1;
    public static final int KEY_FRMAE_3D_TYPE_SCALE_Z = 2;
    public static final int KEY_FRMAE_3D_TYPE_SHIFT_X = 3;
    public static final int KEY_FRMAE_3D_TYPE_SHIFT_Y = 4;
    public static final int KEY_FRMAE_3D_TYPE_SHIFT_Z = 5;
    public Value[] values = null;
    public int name = 0;

    public static class ExtInfo {
        public float frontX = 0.0f;
        public float frontY = 0.0f;
        public float backX = 0.0f;
        public float backY = 0.0f;
    }

    public static class Value {
        public int offsetOpcodeType;

                public int ts = 0;
        public float offsetValue = 0.0f;
        public float floatValue = 0.0f;
        public int method = 0;
        public long templateID = 0;
        public int lineMode = 0;
        public ExtInfo extInfo = null;
        public QKeyFrameTransformData.EasingInfo easingInfo = null;
    }
}
