package xiaoying.engine.storyboard;

/* JADX INFO: loaded from: classes19.dex */
public class QThemeOperation {
    public static int TYPE_ADD_COVER = 6;
    public static int TYPE_ADD_EFFECT = 1;
    public static int TYPE_ADD_FREEZE_FRAME = 15;
    public static int TYPE_ADD_FX = 11;
    public static int TYPE_ADD_MUSIC = 9;
    public static int TYPE_ADD_PASTER = 13;
    public static int TYPE_ADD_TEXT = 7;
    public static int TYPE_ADD_TRANSITION = 3;
    public static int TYPE_REMOVE_COVER = 5;
    public static int TYPE_REMOVE_EFFECT = 2;
    public static int TYPE_REMOVE_FREEZE_FRAME = 16;
    public static int TYPE_REMOVE_FX = 12;
    public static int TYPE_REMOVE_MUSIC = 10;
    public static int TYPE_REMOVE_PASTER = 14;
    public static int TYPE_REMOVE_TEXT = 8;
    public static int TYPE_REMOVE_TRANSITION = 4;
    private int errorCode = 0;
    private boolean opFinish = false;
    private Object opData = null;
    private float[] effectLayerIdArray = null;
    private int operationType = 0;
    private boolean onStoryboard = false;
    private int clipIndex = 0;
    private int effectTrackType = 0;
    private int effectGroupID = 0;

    private QThemeOperation() {
    }

    public int getClipIndex() {
        return this.clipIndex;
    }

    public int getEffectGroupID() {
        return this.effectGroupID;
    }

    public float[] getEffectLayerIdArray() {
        return this.effectLayerIdArray;
    }

    public int getEffectTrackType() {
        return this.effectTrackType;
    }

    public int getErrorCode() {
        return this.errorCode;
    }

    public Object getOperatorData() {
        return this.opData;
    }

    public int getType() {
        return this.operationType;
    }

    public boolean operateOnStoryboard() {
        return this.onStoryboard;
    }

    public boolean operatorFinish() {
        return this.opFinish;
    }

    public void setEffectGroupID(int groupId) {
        this.effectGroupID = groupId;
    }

    public int setEffectLayerIdArray(float[] layerIDArray) {
        float[] fArr;
        if (layerIDArray == null || (fArr = this.effectLayerIdArray) == null || layerIDArray.length != fArr.length) {
            return -1;
        }
        this.effectLayerIdArray = layerIDArray;
        return 0;
    }

    public int setEffectLayerIdByIndex(int index, float fLayerID) {
        float[] fArr = this.effectLayerIdArray;
        if (fArr == null || index >= fArr.length || index < 0) {
            return -1;
        }
        fArr[index] = fLayerID;
        return 0;
    }

    public void setEffectTrackType(int trackType) {
        this.effectTrackType = trackType;
    }
}
