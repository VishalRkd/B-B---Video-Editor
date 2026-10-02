package xiaoying.engine.base.pcm;

/* JADX INFO: loaded from: classes19.dex */
public class QPCMEDataFloat {
    public float[] left;
    public float maxAbsLeft;
    public float maxAbsRight;
    public float[] right;

    public QPCMEDataFloat() {
        this.left = null;
        this.right = null;
        this.maxAbsLeft = 0.0f;
        this.maxAbsRight = 0.0f;
    }

    public QPCMEDataFloat(boolean needLeft, boolean needRight, int smpCnt) {
        if (needLeft) {
            this.left = new float[smpCnt];
        } else {
            this.left = null;
        }
        if (needRight) {
            this.right = new float[smpCnt];
        } else {
            this.right = null;
        }
        this.maxAbsLeft = 0.0f;
        this.maxAbsRight = 0.0f;
    }
}
