package xiaoying.engine.storyboard;

import xiaoying.engine.QEngine;
import xiaoying.engine.audioanalyze.QAudioAnalyzeParam;
import xiaoying.engine.base.IQSessionStateListener;
import xiaoying.engine.base.QRange;
import xiaoying.engine.base.QSession;
import xiaoying.engine.base.QThemeClipList;
import xiaoying.engine.base.QVEError;
import xiaoying.engine.clip.QClip;
import xiaoying.engine.clip.QTransition;

/* JADX INFO: loaded from: classes19.dex */
public class QStoryboard extends QSession {
    public static final int APPLY_SMART_THEME_STATUS_APPLY_THEME = 2;
    public static final int APPLY_SMART_THEME_STATUS_CREATE_START = 0;
    public static final int APPLY_SMART_THEME_STATUS_OVER = 4;
    public static final int APPLY_SMART_THEME_STATUS_PREPARE_PIPE = 3;
    public static final int APPLY_SMART_THEME_STATUS_REFRESH_STORBOARD = 1;
    public static final int LTCST_MULTI_PIC = 1;
    public static final int LTCST_SINGLE_VIDEO = 2;
    public static final int LYRIC_THEME_CLIP_SRC_TYPE = 0;
    public static final int NEW_FRAMEWORK_PRJ_VERSION = 262144;
    public static final int PROP_AUDIO_APPLY_SCALE = 16406;
    public static final int PROP_AUDIO_PITCH_DELTA = 16403;
    public static final int PROP_AUTO_APPLY_THEME = 16387;
    private static final int PROP_BASE = 16384;
    public static final int PROP_FIT_TRACK = 16385;
    public static final int PROP_FORCE_STATIC_TRANS = 16398;
    public static final int PROP_IS_TIME_SCALE_USE_AUDIO_PITCH = 16408;
    public static final int PROP_IS_USE_STUFF_CLIP = 16405;
    public static final int PROP_ORIGINAL_DURATION = 16396;
    public static final int PROP_OUTPUT_RESOLUTION = 16395;
    public static final int PROP_RATIO_SETTED = 16400;
    public static final int PROP_RESET_THEME_ELEM = 16401;
    public static final int PROP_SINGLE_HW_INSTANCE = 16399;
    public static final int PROP_STORYBOARD_CLIP_IS_VISABLE = 16411;
    public static final int PROP_STORYBOARD_IS_WATERMARK_SKIP_LAST_CLIP = 16413;
    public static final int PROP_STPRYBBOARD_PROJECT_ID = 16412;
    public static final int PROP_STUFF_CLIP_FPS = 16409;
    public static final int PROP_THEME_BACK_COVER = 16393;
    public static final int PROP_THEME_COVER = 16392;
    public static final int PROP_THEME_FILTER_MODE = 16407;
    public static final int PROP_THEME_ID = 16394;
    public static final int PROP_THEME_TEMPLATE = 16391;
    public static final int PROP_TIME_SCALE = 16402;
    public static final int THEME_FILTER_MODE_OVERLAY = 2;
    public static final int THEME_FILTER_MODE_REPLACE = 0;
    public static final int THEME_FILTER_MODE_RETAIN = 1;
    public static final int THEME_RESET_CODE_MUSIC = 1;
    private IQThemeOperationListener themeOPListener = null;
    private QThemeOperation themeOPData = null;
    private boolean isRefData = false;
    private long autoProducerHandle = 0;

    public static int getProjectVersion(QEngine engine, String strProjectFile) {
        if (engine == null) {
            return 0;
        }
        return ((Integer) engine.getProperty(112)).intValue() == 393216 ? nativeGetProjectVersionAEWrapper(engine, strProjectFile) : nativeGetProjectVersion(engine, strProjectFile);
    }

    private native int nativeApplySmartTheme(long handle, long templateID, QThemeClipList clipList);

    private native int nativeApplySmartThemeAEWrapper(long handle, long templateID, QThemeClipList clipList);

    private native int nativeApplyTheme(QStoryboard storyboard, String themeTemplate);

    private native int nativeApplyThemeAEWrapper(QStoryboard storyboard, String themeTemplate);

    private native int nativeApplyTrim(long handle);

    private native int nativeApplyTrimAEWrapper(long handle);

    private native QRange nativeConvertRangeOriginalToDst(long handle, QRange oriRange);

    private native QRange nativeConvertRangeOriginalToDstAEWrapper(long handle, QRange oriRange);

    private native int nativeCreate(QEngine engine, QStoryboard storyboard);

    private native int nativeCreateAEWrapper(QEngine engine, QStoryboard storyboard);

    private native int nativeDestroy(QStoryboard storyboard);

    private native int nativeDestroyAEWrapper(QStoryboard storyboard);

    private native int nativeDuplicate(QStoryboard src, QStoryboard dst);

    private native int nativeDuplicateAEWrapper(QStoryboard src, QStoryboard dst);

    private native QProjectData nativeFetchProjectData(QStoryboard storyboard);

    private native QProjectData nativeFetchProjectDataAEWrapper(QStoryboard storyboard);

    private native QClip nativeGetClip(long handle, int index);

    private native QClip nativeGetClipAEWrapper(long handle, int index);

    private native QClip nativeGetClipByUuid(long handle, String strUuid);

    private native QClip nativeGetClipByUuidAEWrapper(long handle, String strUuid);

    private native int nativeGetClipCount(long handle);

    private native int nativeGetClipCountAEWrapper(long handle);

    private native QClipPosition[] nativeGetClipPositionArrayByTime(long handle, int time);

    private native QClipPosition[] nativeGetClipPositionArrayByTimeAEWrapper(long handle, int time);

    private native QClipPosition nativeGetClipPositionByIndex(long handle, int index);

    private native QClipPosition nativeGetClipPositionByIndexAEWrapper(long handle, int index);

    private native QClipPosition nativeGetClipPositionByTime(long handle, int time);

    private native QClipPosition nativeGetClipPositionByTimeAEWrapper(long handle, int time);

    private native QRange nativeGetClipTimeRange(long handle, int clipIndex);

    private native QRange nativeGetClipTimeRangeAEWrapper(long handle, int clipIndex);

    private native QClip nativeGetDataClip(long handle);

    private native QClip nativeGetDataClipAEWrapper(long handle);

    private native int nativeGetDuration(long handle);

    private native int nativeGetDurationAEWrapper(long handle);

    private native int nativeGetIndexByClipPosition(long handle, QClipPosition position);

    private native int nativeGetIndexByClipPositionAEWrapper(long handle, QClipPosition position);

    private native int nativeGetProjectEngineVersion(QStoryboard storyboard, String strProjectFile);

    private native int nativeGetProjectEngineVersionAEWrapper(QEngine engine, QStoryboard storyboard, String strProjectFile);

    private static native int nativeGetProjectVersion(QEngine engine, String strProjectFile);

    private static native int nativeGetProjectVersionAEWrapper(QEngine engine, String strProjectFile);

    private native Object nativeGetPropAEWrapper(long sessionHandle, int propertyID);

    private native QClip nativeGetStuffClip(long handle);

    private native QClip nativeGetStuffClipAEWrapper(long handle);

    private native QThemePackData nativeGetThemePackData(QStoryboard storyboard);

    private native int nativeGetTimeByClipPosition(long handle, QClipPosition position);

    private native int nativeGetTimeByClipPositionAEWrapper(long handle, QClipPosition position);

    private native QTransition nativeGetTransitionInfo(long handle, int clipIndex);

    private native QTransition nativeGetTransitionInfoAEWrapper(long handle, int clipIndex);

    private native QRange nativeGetTransitionTimeRange(long handle, int clipIndex);

    private native QRange nativeGetTransitionTimeRangeAEWrapper(long handle, int clipIndex);

    private native int nativeInsertClip(long handle, QClip clip, int index);

    private native int nativeInsertClipAEWrapper(long handle, QClip clip, int index);

    private native int nativeLoadProject(QStoryboard storyboard, String strProjectFile);

    private native int nativeLoadProjectAEWrapper(QStoryboard storyboard, String strProjectFile);

    private native int nativeLoadProjectData(QStoryboard storyboard, String strProjectFile);

    private native int nativeLoadProjectDataAEWrapper(QStoryboard storyboard, String strProjectFile);

    private native int nativeMoveClip(long handle, QClip clip, int index);

    private native int nativeMoveClipAEWrapper(long handle, QClip clip, int index);

    private native int nativeRemoveAllClip(long handle);

    private native int nativeRemoveAllClipAEWrapper(long handle);

    private native int nativeRemoveClip(long handle, QClip clip);

    private native int nativeRemoveClipAEWrapper(long handle, QClip clip);

    private native int nativeSaveProject(QStoryboard storyboard, String strProjectFile);

    private native int nativeSaveProjectAEWrapper(QStoryboard storyboard, String strProjectFile);

    private native int nativeSaveTheme(QStoryboard storyboard, String strThemePath, long llId);

    private native int nativeSetLyricThemeAVParam(String strLyricFile, QAudioAnalyzeParam stParam, boolean bSyncClipTimeByLyric, int nSyncType);

    private native int nativeSetLyricThemeAVParamAEWrapper(String strLyricFile, QAudioAnalyzeParam stParam, boolean bSyncClipTimeByLyric, int nSyncType);

    private native int nativeSetLyricThemeClipTransLation(long lThemeID);

    private native int nativeSetLyricThemeClipTransLationAEWrapper(long lThemeID);

    private native int nativeSetPropAEWrapper(long sessionHandle, int propertyID, Object data);

    private native int nativeStopApplySmartTheme(long autoProducerHandle);

    private native int nativeStopApplySmartThemeAEWrapper(long autoProducerHandle);

    private native int nativeWaitApplySmartThemeComplete(long autoProducerHandle);

    private native int nativeWaitApplySmartThemeCompleteAEWrapper(long autoProducerHandle);

    private int onThemeOperation(QThemeOperation operation) {
        IQThemeOperationListener iQThemeOperationListener = this.themeOPListener;
        if (iQThemeOperationListener == null) {
            return 0;
        }
        return iQThemeOperationListener.onThemeOperation(this.themeOPData);
    }

    public QClipPosition[] GetClipPositionArrayByTime(int time) {
        long j10 = this.handle;
        if (0 == j10 && 0 == this.aehandle) {
            return null;
        }
        return this.frameworkVersion == 393216 ? nativeGetClipPositionArrayByTimeAEWrapper(this.aehandle, time) : nativeGetClipPositionArrayByTime(j10, time);
    }

    public QClipPosition GetClipPositionByIndex(int index) {
        long j10 = this.handle;
        if (0 == j10 && 0 == this.aehandle) {
            return null;
        }
        return this.frameworkVersion == 393216 ? nativeGetClipPositionByIndexAEWrapper(this.aehandle, index) : nativeGetClipPositionByIndex(j10, index);
    }

    public QClipPosition GetClipPositionByTime(int time) {
        long j10 = this.handle;
        if (0 == j10 && 0 == this.aehandle) {
            return null;
        }
        return this.frameworkVersion == 393216 ? nativeGetClipPositionByTimeAEWrapper(this.aehandle, time) : nativeGetClipPositionByTime(j10, time);
    }

    public int GetIndexByClipPosition(QClipPosition position) {
        long j10 = this.handle;
        if (0 == j10 && 0 == this.aehandle) {
            return -1;
        }
        return this.frameworkVersion == 393216 ? nativeGetIndexByClipPositionAEWrapper(this.aehandle, position) : nativeGetIndexByClipPosition(j10, position);
    }

    public QThemePackData GetThemePackData() {
        if (0 == this.handle) {
            return null;
        }
        return nativeGetThemePackData(this);
    }

    public int GetTimeByClipPosition(QClipPosition position) {
        long j10 = this.handle;
        if (0 == j10 && 0 == this.aehandle) {
            return -1;
        }
        return this.frameworkVersion == 393216 ? nativeGetTimeByClipPositionAEWrapper(this.aehandle, position) : nativeGetTimeByClipPosition(j10, position);
    }

    public int SetLyricThemeAVParam(String strLyricFile, QAudioAnalyzeParam stParam, boolean bSyncClipTimeByLyric, int nSyncType) {
        if (0 == this.handle && 0 == this.aehandle) {
            return QVEError.QERR_COMMON_JAVA_NOT_INIT;
        }
        return this.frameworkVersion == 393216 ? nativeSetLyricThemeAVParamAEWrapper(strLyricFile, stParam, bSyncClipTimeByLyric, nSyncType) : nativeSetLyricThemeAVParam(strLyricFile, stParam, bSyncClipTimeByLyric, nSyncType);
    }

    public int SetLyricThemeClipTransLation(long lThemeID) {
        if (0 == this.handle && 0 == this.aehandle) {
            return QVEError.QERR_COMMON_JAVA_NOT_INIT;
        }
        return this.frameworkVersion == 393216 ? nativeSetLyricThemeClipTransLationAEWrapper(lThemeID) : nativeSetLyricThemeClipTransLation(lThemeID);
    }

    public int applySmartTheme(long templateID, IQSessionStateListener listener, QThemeClipList clipList) {
        long j10 = this.handle;
        if (0 == j10 && 0 == this.aehandle) {
            return QVEError.QERR_COMMON_JAVA_NOT_INIT;
        }
        this.listener = listener;
        return this.frameworkVersion == 393216 ? nativeApplySmartThemeAEWrapper(this.aehandle, templateID, clipList) : nativeApplySmartTheme(j10, templateID, clipList);
    }

    public int applyTheme(String themeTemplate, IQSessionStateListener listener) {
        if (0 == this.handle && 0 == this.aehandle) {
            return QVEError.QERR_COMMON_JAVA_NOT_INIT;
        }
        this.listener = listener;
        return this.frameworkVersion == 393216 ? nativeApplyThemeAEWrapper(this, themeTemplate) : nativeApplyTheme(this, themeTemplate);
    }

    public int applyTrim() {
        long j10 = this.handle;
        if (0 == j10 && 0 == this.aehandle) {
            return -1;
        }
        return this.frameworkVersion == 393216 ? nativeApplyTrimAEWrapper(this.aehandle) : nativeApplyTrim(j10);
    }

    public QRange convertRangeOriginalToDst(QRange oriRange) {
        long j10 = this.handle;
        if (0 == j10 && 0 == this.aehandle) {
            return null;
        }
        return this.frameworkVersion == 393216 ? nativeConvertRangeOriginalToDstAEWrapper(this.aehandle, oriRange) : nativeConvertRangeOriginalToDst(j10, oriRange);
    }

    public int duplicate(QStoryboard storyboardDst) {
        storyboardDst.frameworkVersion = this.frameworkVersion;
        return this.frameworkVersion == 393216 ? nativeDuplicateAEWrapper(this, storyboardDst) : nativeDuplicate(this, storyboardDst);
    }

    public QProjectData fetchProjectData() {
        if (0 == this.handle && 0 == this.aehandle) {
            return null;
        }
        return this.frameworkVersion == 393216 ? nativeFetchProjectDataAEWrapper(this) : nativeFetchProjectData(this);
    }

    public QClip getClip(int index) {
        long j10 = this.handle;
        if (0 == j10 && 0 == this.aehandle) {
            return null;
        }
        return this.frameworkVersion == 393216 ? nativeGetClipAEWrapper(this.aehandle, index) : nativeGetClip(j10, index);
    }

    public QClip getClipByUuid(String strUuid) {
        long j10 = this.handle;
        if (0 == j10 && 0 == this.aehandle) {
            return null;
        }
        return this.frameworkVersion == 393216 ? nativeGetClipByUuidAEWrapper(this.aehandle, strUuid) : nativeGetClipByUuid(j10, strUuid);
    }

    public int getClipCount() {
        long j10 = this.handle;
        if (0 == j10 && 0 == this.aehandle) {
            return QVEError.QERR_COMMON_JAVA_NOT_INIT;
        }
        return this.frameworkVersion == 393216 ? nativeGetClipCountAEWrapper(this.aehandle) : nativeGetClipCount(j10);
    }

    public QRange getClipTimeRange(int clipIndex) {
        long j10 = this.handle;
        if (0 == j10 && 0 == this.aehandle) {
            return null;
        }
        return this.frameworkVersion == 393216 ? nativeGetClipTimeRangeAEWrapper(this.aehandle, clipIndex) : nativeGetClipTimeRange(j10, clipIndex);
    }

    public QClip getDataClip() {
        long j10 = this.handle;
        if (0 == j10 && 0 == this.aehandle) {
            return null;
        }
        return this.frameworkVersion == 393216 ? nativeGetDataClipAEWrapper(this.aehandle) : nativeGetDataClip(j10);
    }

    public int getDuration() {
        long j10 = this.handle;
        if (0 == j10 && 0 == this.aehandle) {
            return 0;
        }
        return this.frameworkVersion == 393216 ? nativeGetDurationAEWrapper(this.aehandle) : nativeGetDuration(j10);
    }

    public QEngine getEngine() {
        return this.engine;
    }

    public int getProjectEngineVersion(String projectPath) {
        if (0 == this.handle && 0 == this.aehandle) {
            return 0;
        }
        return this.frameworkVersion == 393216 ? nativeGetProjectEngineVersionAEWrapper(this.engine, this, projectPath) : nativeGetProjectEngineVersion(this, projectPath);
    }

    @Override // xiaoying.engine.base.QSession
    public Object getProperty(int propertyID) {
        if (this.handle == 0 && this.aehandle == 0) {
            return null;
        }
        return this.frameworkVersion == 393216 ? nativeGetPropAEWrapper(this.aehandle, propertyID) : super.getProperty(propertyID);
    }

    public QClip getStuffClip() {
        long j10 = this.handle;
        if (0 == j10 && 0 == this.aehandle) {
            return null;
        }
        return this.frameworkVersion == 393216 ? nativeGetStuffClipAEWrapper(this.aehandle) : nativeGetStuffClip(j10);
    }

    public QTransition getTransitionInfo(int clipIndex) {
        long j10 = this.handle;
        if (0 == j10 && 0 == this.aehandle) {
            return null;
        }
        return this.frameworkVersion == 393216 ? nativeGetTransitionInfoAEWrapper(this.aehandle, clipIndex) : nativeGetTransitionInfo(j10, clipIndex);
    }

    public QRange getTransitionTimeRange(int clipIndex) {
        long j10 = this.handle;
        if (0 == j10 && 0 == this.aehandle) {
            return null;
        }
        return this.frameworkVersion == 393216 ? nativeGetTransitionTimeRangeAEWrapper(this.aehandle, clipIndex) : nativeGetTransitionTimeRange(j10, clipIndex);
    }

    @Override // xiaoying.engine.base.QSession
    public int init(QEngine engine, IQSessionStateListener listener) {
        if (engine == null) {
            return QVEError.QERR_COMMON_JAVA_INVALID_PARAM;
        }
        super.init(engine, listener);
        int iIntValue = ((Integer) engine.getProperty(112)).intValue();
        this.frameworkVersion = iIntValue;
        return iIntValue == 393216 ? nativeCreateAEWrapper(engine, this) : nativeCreate(engine, this);
    }

    public int insertClip(QClip clip, int index) {
        long j10 = this.handle;
        if (0 == j10 && 0 == this.aehandle) {
            return QVEError.QERR_COMMON_JAVA_NOT_INIT;
        }
        return this.frameworkVersion == 393216 ? nativeInsertClipAEWrapper(this.aehandle, clip, index) : nativeInsertClip(j10, clip, index);
    }

    public int loadProject(String strProjectFile, IQSessionStateListener listener) {
        if (0 == this.handle && 0 == this.aehandle) {
            return QVEError.QERR_COMMON_JAVA_NOT_INIT;
        }
        this.listener = listener;
        return this.frameworkVersion == 393216 ? nativeLoadProjectAEWrapper(this, strProjectFile) : nativeLoadProject(this, strProjectFile);
    }

    public int loadProjectData(String strProjectFile, IQSessionStateListener listener) {
        if (0 == this.handle && 0 == this.aehandle) {
            return QVEError.QERR_COMMON_JAVA_NOT_INIT;
        }
        this.listener = listener;
        return this.frameworkVersion == 393216 ? nativeLoadProjectDataAEWrapper(this, strProjectFile) : nativeLoadProjectData(this, strProjectFile);
    }

    public int moveClip(QClip clip, int index) {
        long j10 = this.handle;
        if (0 == j10 && 0 == this.aehandle) {
            return QVEError.QERR_COMMON_JAVA_NOT_INIT;
        }
        return this.frameworkVersion == 393216 ? nativeMoveClipAEWrapper(this.aehandle, clip, index) : nativeMoveClip(j10, clip, index);
    }

    public int removeAllClip() {
        long j10 = this.handle;
        if (0 == j10 && 0 == this.aehandle) {
            return QVEError.QERR_COMMON_JAVA_NOT_INIT;
        }
        return this.frameworkVersion == 393216 ? nativeRemoveAllClipAEWrapper(this.aehandle) : nativeRemoveAllClip(j10);
    }

    public int removeClip(QClip clip) {
        long j10 = this.handle;
        if (0 == j10 && 0 == this.aehandle) {
            return QVEError.QERR_COMMON_JAVA_NOT_INIT;
        }
        return this.frameworkVersion == 393216 ? nativeRemoveClipAEWrapper(this.aehandle, clip) : nativeRemoveClip(j10, clip);
    }

    public int saveProject(String strProjectFile, IQSessionStateListener listener) {
        if (0 == this.handle && 0 == this.aehandle) {
            return QVEError.QERR_COMMON_JAVA_NOT_INIT;
        }
        this.listener = listener;
        return this.frameworkVersion == 393216 ? nativeSaveProjectAEWrapper(this, strProjectFile) : nativeSaveProject(this, strProjectFile);
    }

    public int saveTheme(String strThemePath, long llId, IQSessionStateListener listener) {
        if (0 == this.handle) {
            return QVEError.QERR_COMMON_JAVA_NOT_INIT;
        }
        this.listener = listener;
        return nativeSaveTheme(this, strThemePath, llId);
    }

    @Override // xiaoying.engine.base.QSession
    public int setProperty(int propertyID, Object data) {
        return this.frameworkVersion == 393216 ? nativeSetPropAEWrapper(this.aehandle, propertyID, data) : super.setProperty(propertyID, data);
    }

    public int setThemeOperationListener(IQThemeOperationListener themeOPListener) {
        this.themeOPListener = themeOPListener;
        return 0;
    }

    public int stopApplySmartTheme() {
        if (0 == this.handle && 0 == this.aehandle) {
            return QVEError.QERR_COMMON_JAVA_NOT_INIT;
        }
        long j10 = this.autoProducerHandle;
        if (0 == j10) {
            return QVEError.QERR_COMMON_JAVA_NOT_INIT;
        }
        return this.frameworkVersion == 393216 ? nativeStopApplySmartThemeAEWrapper(j10) : nativeStopApplySmartTheme(j10);
    }

    @Override // xiaoying.engine.base.QSession
    public int unInit() {
        if (0 == this.handle && 0 == this.aehandle) {
            return QVEError.QERR_COMMON_JAVA_NOT_INIT;
        }
        return this.frameworkVersion == 393216 ? nativeDestroyAEWrapper(this) : nativeDestroy(this);
    }

    public int waitApplySmartThemeComplete() {
        if (0 == this.handle && 0 == this.aehandle) {
            return QVEError.QERR_COMMON_JAVA_NOT_INIT;
        }
        long j10 = this.autoProducerHandle;
        if (0 == j10) {
            return QVEError.QERR_COMMON_JAVA_NOT_INIT;
        }
        return this.frameworkVersion == 393216 ? nativeWaitApplySmartThemeCompleteAEWrapper(j10) : nativeWaitApplySmartThemeComplete(j10);
    }

    // --- Advanced Engine Ergonomic APIs ---

    public int addClip(QClip clip) {
        return insertClip(clip, getClipCount());
    }

    public java.util.List<QClip> getAllClips() {
        int count = getClipCount();
        java.util.List<QClip> list = new java.util.ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            QClip c = getClip(i);
            if (c != null) {
                list.add(c);
            }
        }
        return list;
    }

    public int setClipTrim(int clipIndex, int startMs, int lengthMs) {
        QClip clip = getClip(clipIndex);
        if (clip == null) return QVEError.QERR_COMMON_JAVA_INVALID_PARAM;
        int ret = clip.setTrimRange(startMs, lengthMs);
        if (ret == 0) {
            applyTrim();
        }
        return ret;
    }

    public int setClipSpeed(int clipIndex, float speed) {
        QClip clip = getClip(clipIndex);
        if (clip == null) return QVEError.QERR_COMMON_JAVA_INVALID_PARAM;
        int ret = clip.setTimeScale(speed);
        if (ret == 0) {
            applyTrim();
        }
        return ret;
    }

    public int setClipRotation(int clipIndex, int degrees) {
        QClip clip = getClip(clipIndex);
        if (clip == null) return QVEError.QERR_COMMON_JAVA_INVALID_PARAM;
        return clip.setRotation(degrees);
    }

    public int setClipVolume(int clipIndex, int volumePercent, boolean isMuted) {
        QClip clip = getClip(clipIndex);
        if (clip == null) return QVEError.QERR_COMMON_JAVA_INVALID_PARAM;
        clip.setAudioDisabled(isMuted);
        return clip.setAudioVolume(volumePercent);
    }

    public int applyTransition(int clipIndex, QTransition transition) {
        QClip clip = getClip(clipIndex);
        if (clip == null) return QVEError.QERR_COMMON_JAVA_INVALID_PARAM;
        return clip.setTransition(transition);
    }
}
