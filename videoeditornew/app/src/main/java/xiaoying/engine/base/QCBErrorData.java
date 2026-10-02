package xiaoying.engine.base;

/* JADX INFO: loaded from: classes19.dex */
public class QCBErrorData {
    public static final int ERROR_TYPE_CLIP = 1;
    public static final int ERROR_TYPE_EFFECT = 3;
    public static final int ERROR_TYPE_NONE = 0;
    public static final int ERROR_TYPE_SUB_EFFECT = 4;
    public static final int ERROR_TYPE_TRANSITION = 2;
    public int nErrorType = 0;
    public int nErrorCode = 0;
    public int nCurTime = 0;
    public int nClipIndex = -1;
    public int nEffectIndex = -1;
    public int nEffectTrackType = 0;
    public int nEffectGroupID = 0;
    public String clipUnique = null;
    public String effectUnique = null;
    public String filePath = null;
}
