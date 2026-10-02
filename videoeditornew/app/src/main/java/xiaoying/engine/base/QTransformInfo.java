package xiaoying.engine.base;

/* JADX INFO: loaded from: classes19.dex */
public class QTransformInfo {
    public float mScaleX = 1.0f;
    public float mScaleY = 1.0f;
    public float mScaleZ = 1.0f;
    public float mShiftX = 0.5f;
    public float mShiftY = 0.5f;
    public float mShiftZ = 0.0f;
    public float mAngleX = 0.0f;
    public float mAngleY = 0.0f;
    public float mAngleZ = 0.0f;
    public float mAnchorX = 0.5f;
    public float mAnchorY = 0.5f;
    public float mAnchorZ = 0.5f;

    public boolean equals(Object o10) {
        if (!(o10 instanceof QTransformInfo)) {
            return false;
        }
        QTransformInfo qTransformInfo = (QTransformInfo) o10;
        return Math.abs(qTransformInfo.mScaleX - this.mScaleX) < 1.0E-6f && Math.abs(qTransformInfo.mScaleY - this.mScaleY) < 1.0E-6f && Math.abs(qTransformInfo.mScaleZ - this.mScaleZ) < 1.0E-6f && Math.abs(qTransformInfo.mShiftX - this.mShiftX) < 1.0E-6f && Math.abs(qTransformInfo.mShiftY - this.mShiftY) < 1.0E-6f && Math.abs(qTransformInfo.mShiftZ - this.mShiftZ) < 1.0E-6f && Math.abs(qTransformInfo.mAngleX - this.mAngleX) < 1.0E-6f && Math.abs(qTransformInfo.mAngleY - this.mAngleY) < 1.0E-6f && Math.abs(qTransformInfo.mAngleZ - this.mAngleZ) < 1.0E-6f && Math.abs(qTransformInfo.mAnchorX - this.mAnchorX) < 1.0E-6f && Math.abs(qTransformInfo.mAnchorY - this.mAnchorY) < 1.0E-6f && Math.abs(qTransformInfo.mAnchorZ - this.mAnchorZ) < 1.0E-6f;
    }
}
