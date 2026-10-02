package xiaoying.engine.base;

/* JADX INFO: loaded from: classes18.dex */
public class QAlgoUtilsResult {
    public static final int ALGO_RESULT_TYPE_AUDIO_PITCH_DT = 7;
    public static final int ALGO_RESULT_TYPE_ESSENTIA_MODE = 5;
    public static final int ALGO_RESULT_TYPE_FLOAT = 6;
    public static final int ALGO_RESULT_TYPE_JSON_PATH = 3;
    public static final int ALGO_RESULT_TYPE_RANGE = 1;
    public static final int ALGO_RESULT_TYPE_SMART_CROP = 2;
    public static final int ALGO_RESULT_TYPE_VECTOR_BYTE = 4;
    public QAlgoUtilsResultData[] resultData = null;

    public static class QAlgoUtilsResultBase {
        public int resultType = 0;
    }

    public static class QAlgoUtilsResultData {
        public int algoType = 0;
        public QAlgoUtilsResultBase[] result = null;
    }

    /* JADX INFO: loaded from: classes19.dex */
    public static class QAlgoUtilsResultFloat extends QAlgoUtilsResultBase {
        public float fValue = 0.0f;

        public QAlgoUtilsResultFloat() {
            this.resultType = 6;
        }
    }

    /* JADX INFO: loaded from: classes19.dex */
    public static class QAlgoUtilsResultMode extends QAlgoUtilsResultBase {
        public String strKey = null;
        public String strScale = null;

        public QAlgoUtilsResultMode() {
            this.resultType = 5;
        }
    }

    /* JADX INFO: loaded from: classes19.dex */
    public static class QAlgoUtilsResultPitchDT extends QAlgoUtilsResultBase {
        public int totalTime = 0;
        public int lowFreqTime = 0;
        public int midFreqTime = 0;
        public int highFreqTime = 0;

        public QAlgoUtilsResultPitchDT() {
            this.resultType = 7;
        }
    }

    /* JADX INFO: loaded from: classes19.dex */
    public static class QAlgoUtilsResultRange extends QAlgoUtilsResultBase {
        public QRange range = null;

        public QAlgoUtilsResultRange() {
            this.resultType = 1;
        }
    }
}
