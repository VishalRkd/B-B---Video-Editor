package xiaoying.engine.audioanalyze;

import java.util.ArrayList;
import xiaoying.engine.base.QRange;
import xiaoying.engine.base.QVEError;

/* JADX INFO: loaded from: classes18.dex */
public class QAudioAnalyze {
    protected IAudioAnalyzeListener listener = null;
    protected long handle = 0;
    protected long globalref = 0;

    public static QAudioBeatDetectionResult getBeatDetectResult(String strResFile, QRange audioRange) {
        return nativeGetBeatDetectResult(strResFile, audioRange);
    }

    public static float getLoudnessDetectResult(String strResFile) {
        return nativeGetLoudnessDetectResult(strResFile);
    }

    public static float[] getOnsetDetectResult(String strResFile, QRange audioRange) {
        return nativeGetOnsetDetectResult(strResFile, audioRange);
    }

    public static float[] getTempoDetectResult(String strResFile, QRange audioRange) {
        return nativeGetTempoDetectResult(strResFile, audioRange);
    }

    private native int nativeGetAnalysisResult(long handle, long lTimeStamp, int nTargetIndex, QAAResult resOut);

    private static native QAudioBeatDetectionResult nativeGetBeatDetectResult(String strResFile, QRange audioRange);

    private static native float nativeGetLoudnessDetectResult(String strResFile);

    private static native float[] nativeGetOnsetDetectResult(String strResFile, QRange audioRange);

    private static native float[] nativeGetTempoDetectResult(String strResFile, QRange audioRange);

    private native int nativeGetTimeWindowWidth(long handle);

    private native int nativeInit(QAudioAnalyzeParam param, ArrayList targetTypeOut);

    private native int nativeUninit(long handle);

    public void OnAnalyzingProcess(QAudioAnalyzeCallBackData state) {
        IAudioAnalyzeListener iAudioAnalyzeListener = this.listener;
        if (iAudioAnalyzeListener == null) {
            return;
        }
        iAudioAnalyzeListener.OnAnalyzingProcess(state);
    }

    public int getAnalysisResult(long lTimeStamp, int nTargetIndex, QAAResult resOut) {
        long j10 = this.handle;
        return 0 == j10 ? QVEError.QERR_COMMON_JAVA_NOT_INIT : nativeGetAnalysisResult(j10, lTimeStamp, nTargetIndex, resOut);
    }

    public int getTimeWindowWidth() {
        long j10 = this.handle;
        if (0 == j10) {
            return -1;
        }
        return nativeGetTimeWindowWidth(j10);
    }

    public int init(QAudioAnalyzeParam param, IAudioAnalyzeListener listener, ArrayList targetTypeOut) {
        this.listener = listener;
        return nativeInit(param, targetTypeOut);
    }

    public int uninit() {
        long j10 = this.handle;
        if (0 == j10) {
            return QVEError.QERR_COMMON_JAVA_NOT_INIT;
        }
        nativeUninit(j10);
        this.handle = 0L;
        return 0;
    }

    public static class QAudioBeatDetectionResult {
        public float[] beatPos;
        public float[] downBeatPos;

        public QAudioBeatDetectionResult() {
            this.beatPos = null;
            this.downBeatPos = null;
        }

        public QAudioBeatDetectionResult(int beatCnt, int downBeatCnt) {
            this.beatPos = new float[beatCnt];
            this.downBeatPos = new float[downBeatCnt];
        }
    }
}
