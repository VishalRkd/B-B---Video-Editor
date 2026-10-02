package xiaoying.engine.aecomp;

import xiaoying.engine.QEngine;
import xiaoying.engine.audioanalyze.QAudioAnalyzeParam;
import xiaoying.engine.base.IQSessionStateListener;
import xiaoying.engine.base.QVEError;
import xiaoying.engine.storyboard.IQThemeOperationListener;

/* JADX INFO: loaded from: classes19.dex */
public class QAEComp extends QAEBaseComp {
    public int applyTheme(String themeTemplate, IQSessionStateListener listener) {
        long j10 = this.wphandle;
        if (0 == j10) {
            return QVEError.QERR_COMMON_JAVA_NOT_INIT;
        }
        this.listener = listener;
        return nativeApplyTheme(j10, themeTemplate);
    }

    public int create(QEngine engine, int groupId, float layerId) {
        return (this.sphandle == 0 && this.wphandle == 0) ? nativeCreate(engine, groupId, layerId, 1) : QVEError.QERR_COMMON_JAVA_FAIL;
    }

    public int setLyricThemeAVParam(String strLyricFile, QAudioAnalyzeParam stParam, boolean bSyncClipTimeByLyric, int nSyncType) {
        long j10 = this.wphandle;
        return 0 == j10 ? QVEError.QERR_COMMON_JAVA_NOT_INIT : nativeSetLyricThemeAVParam(j10, strLyricFile, stParam, bSyncClipTimeByLyric, nSyncType);
    }

    public int setLyricThemeClipTransLation(long lThemeID) {
        long j10 = this.wphandle;
        return 0 == j10 ? QVEError.QERR_COMMON_JAVA_NOT_INIT : nativeSetLyricThemeClipTransLation(j10, lThemeID);
    }

    public int setThemeOperationListener(IQThemeOperationListener themeOPListener) {
        this.themeOPListener = themeOPListener;
        return 0;
    }
}
