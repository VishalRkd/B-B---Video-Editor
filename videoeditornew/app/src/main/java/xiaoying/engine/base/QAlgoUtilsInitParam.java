package xiaoying.engine.base;

import xiaoying.utils.QBitmap;

/* JADX INFO: loaded from: classes18.dex */
public class QAlgoUtilsInitParam {
    public static final int ALGO_TYPE_AUDIOEBUR = 1003;
    public static final int ALGO_TYPE_AUDIOVAD = 1002;
    public static final int ALGO_TYPE_AUDIO_CHORUS = 1000;
    public static final int ALGO_TYPE_AUTOLUT = 20;
    public static final int ALGO_TYPE_BEAT_DETECT = 1001;
    public static final int ALGO_TYPE_CARTOONLITE = 18;
    public static final int ALGO_TYPE_COLORCORRECTION = 5;
    public static final int ALGO_TYPE_COLORMATCH = 6;
    public static final int ALGO_TYPE_ESSENTIA_BPM = 1005;
    public static final int ALGO_TYPE_ESSENTIA_MODE = 1004;
    public static final int ALGO_TYPE_FACECARTOON = 4;
    public static final int ALGO_TYPE_FACEMORPHING = 14;
    public static final int ALGO_TYPE_FACETRACK = 17;
    public static final int ALGO_TYPE_FACEWRAP = 11;
    public static final int ALGO_TYPE_FACE_DETECT = 1;
    public static final int ALGO_TYPE_IMAGERESTORE = 19;
    public static final int ALGO_TYPE_NONE = 0;
    public static final int ALGO_TYPE_PERSONINST = 10;
    public static final int ALGO_TYPE_PITCH_DETECT = 1006;
    public static final int ALGO_TYPE_SEGMENT = 2;
    public static final int ALGO_TYPE_SEGMENTCLOTH = 3;
    public static final int ALGO_TYPE_SEGMENTPEG = 15;
    public static final int ALGO_TYPE_SINGLETRACK = 13;
    public static final int ALGO_TYPE_SKELETON = 7;
    public static final int ALGO_TYPE_SMARTCROP = 8;
    public static final int ALGO_TYPE_SPLITERHEAD = 9;
    public static final int ALGO_TYPE_VFI = 12;
    public static final int ALGO_TYPE_VIDEOMATTING = 26;
    public static final int ALGO_TYPE_VOS = 16;
    public String mediaPath = null;
    public QRange range = new QRange(0, -1);
    public int startTime = 0;
    public QAlgoUtilsParamBase[] params = null;
    public boolean bReverse = false;

    /* JADX INFO: loaded from: classes19.dex */
    public static class QAlgoUtilsParamAudioChorus extends QAlgoUtilsParamBase {
        public QAlgoUtilsParamAudioChorus() {
            this.algoType = 1000;
        }
    }

    public static class QAlgoUtilsParamBase {
        public int algoType = 0;
    }

    /* JADX INFO: loaded from: classes19.dex */
    public static class QAlgoUtilsParamFaceTrack extends QAlgoUtilsParamBase {
        public QAlgoUtilsParamFaceTrack() {
            this.algoType = 17;
        }
    }

    /* JADX INFO: loaded from: classes19.dex */
    public static class QAlgoUtilsParamPersonInst extends QAlgoUtilsParamBase {
        public QAlgoUtilsParamPersonInst() {
            this.algoType = 10;
        }
    }

    /* JADX INFO: loaded from: classes19.dex */
    public static class QAlgoUtilsParamPitchDT extends QAlgoUtilsParamBase {
        public float fMinLowFreq = 0.0f;
        public float fMinMidFreq = 0.0f;
        public float fMinHighFreq = 0.0f;
        public float fMaxHighFreq = 0.0f;

        public QAlgoUtilsParamPitchDT() {
            this.algoType = 1006;
        }
    }

    /* JADX INFO: loaded from: classes19.dex */
    public static class QAlgoUtilsParamVOS extends QAlgoUtilsParamBase {
        public QBitmap bitMask = null;
        public String ext = null;

        public QAlgoUtilsParamVOS() {
            this.algoType = 16;
        }
    }
}
