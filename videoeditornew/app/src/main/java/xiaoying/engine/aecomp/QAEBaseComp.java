package xiaoying.engine.aecomp;

import xiaoying.engine.QEngine;
import xiaoying.engine.base.IQSessionStateListener;
import xiaoying.engine.base.QSessionState;
import xiaoying.engine.base.QTransformInfo;
import xiaoying.engine.base.QVEError;
import xiaoying.engine.clip.QKeyFrameUniformData;
import xiaoying.engine.storyboard.IQThemeOperationListener;
import xiaoying.engine.storyboard.QThemeOperation;
import xiaoying.utils.QBitmap;

/* JADX INFO: loaded from: classes19.dex */
public abstract class QAEBaseComp extends QAEBaseItem {
    protected long sphandle = 0;
    protected long nativeThumbnailManager = 0;
    private long jniglobalobjectref = 0;
    protected IQSessionStateListener listener = null;
    protected IQThemeOperationListener themeOPListener = null;

    public static int getProjectEngineVersion(QEngine engine, String projectPath) {
        return QAEBaseItem.nativeGetProjectEngineVersion(engine, projectPath);
    }

    public static int getProjectType(QEngine engine, String projectPath) {
        return QAEBaseItem.nativeGetProjectType(engine, projectPath);
    }

    public static int getProjectVersion(QEngine engine, String projectPath) {
        return QAEBaseItem.nativeGetProjectVersion(engine, projectPath);
    }

    private int onSessionStatus(QSessionState state) {
        IQSessionStateListener iQSessionStateListener = this.listener;
        if (iQSessionStateListener == null) {
            return 0;
        }
        return iQSessionStateListener.onSessionStatus(state);
    }

    private int onThemeOperation(QThemeOperation operation) {
        IQThemeOperationListener iQThemeOperationListener = this.themeOPListener;
        if (iQThemeOperationListener == null) {
            return 0;
        }
        return iQThemeOperationListener.onThemeOperation(operation);
    }

    public int UpdateKeyFrameDataOffsetValue(String keyValue, float offset) {
        long j10 = this.wphandle;
        return 0 == j10 ? QVEError.QERR_COMMON_JAVA_NOT_INIT : nativeUpdateKeyFrameDataOffsetValue(j10, keyValue, offset);
    }

    public int cancelProject() {
        long j10 = this.wphandle;
        return 0 == j10 ? QVEError.QERR_COMMON_JAVA_NOT_INIT : nativeCancelProject(j10);
    }

    public int createThumbnailManager(int width, int height, int resampleMode, int fps) {
        return createThumbnailManager(width, height, resampleMode, fps, false, false);
    }

    public int destroyThumbnailManager() {
        return nativeDestroyThumbnailManager(this.nativeThumbnailManager);
    }

    public QAEBaseComp duplicate() {
        long j10 = this.wphandle;
        if (0 == j10) {
            return null;
        }
        return nativeDuplicate(j10);
    }

    public QAEProjectData fetchProjectData() {
        long j10 = this.wphandle;
        if (0 == j10) {
            return null;
        }
        return nativeFetchProjectData(j10);
    }

    public QKeyFrameUniformData findKeyFrameData(String keyValue) {
        long j10 = this.wphandle;
        if (0 == j10) {
            return null;
        }
        return nativeFindKeyFrameData(j10, keyValue);
    }

    public QKeyFrameUniformData.Value findKeyFrameDataValue(String keyValue, int timestamp) {
        long j10 = this.wphandle;
        if (0 == j10) {
            return null;
        }
        return nativeFindKeyFrameDataValue(j10, keyValue, timestamp);
    }

    public QTransformInfo get3DTransformWithKeyFrame(int timestamp) {
        long j10 = this.wphandle;
        if (0 == j10) {
            return null;
        }
        return nativeGet3DTransformWithKeyFrame(j10, timestamp);
    }

    public QAEConstants.QAECompBlendMode getBlendMode() {
        Object property = getProperty(QAEConstants.PROP_AEBASE_ITEM_BLEND_MODE);
        if (property == null) {
            return QAEConstants.QAECompBlendMode.AECompBlendModeNone;
        }
        int iIntValue = ((Integer) property).intValue();
        return iIntValue < QAEConstants.QAECompBlendMode.values().length ? QAEConstants.QAECompBlendMode.values()[iIntValue] : QAEConstants.QAECompBlendMode.AECompBlendModeNone;
    }

    public QAEBaseComp[] getComArrayByTime(int time) {
        long j10 = this.wphandle;
        if (0 == j10) {
            return null;
        }
        return nativeGetCompArrayByTime(j10, time);
    }

    public QAEBaseComp getCompByGroup(int groupId, int index) {
        long j10 = this.wphandle;
        if (0 == j10) {
            return null;
        }
        return nativeGetCompByGroup(j10, groupId, index);
    }

    public QAEBaseComp getCompByIndex(int index) {
        long j10 = this.wphandle;
        if (0 == j10) {
            return null;
        }
        return nativeGetCompByIndex(j10, index);
    }

    public QAEBaseComp getCompByTime(int time) {
        long j10 = this.wphandle;
        if (0 == j10) {
            return null;
        }
        return nativeGetCompByTime(j10, time);
    }

    public QAEBaseComp getCompByUuid(String uuid) {
        long j10 = this.wphandle;
        if (0 == j10) {
            return null;
        }
        return nativeGetCompByUuid(j10, uuid);
    }

    public int getCompCount() {
        long j10 = this.wphandle;
        if (0 == j10) {
            return 0;
        }
        return nativeGetCompCount(j10);
    }

    public int getCompCountByGroup(int groupId) {
        long j10 = this.wphandle;
        if (0 == j10) {
            return 0;
        }
        return nativeGetCompCountByGroup(j10, groupId);
    }

    public int getCompIndex(QAEBaseComp comp) {
        long j10 = this.wphandle;
        if (0 == j10) {
            return -1;
        }
        return nativeGetCompIndex(j10, comp);
    }

    public int getCompIndexInGroup(QAEBaseComp comp) {
        long j10 = this.wphandle;
        if (0 == j10) {
            return -1;
        }
        return nativeGetCompIndexInGroup(j10, comp);
    }

    public int getDuration() {
        long j10 = this.wphandle;
        if (0 == j10) {
            return 0;
        }
        return nativeGetDuration(j10);
    }

    public QAEBaseComp getParent() {
        long j10 = this.wphandle;
        if (0 == j10) {
            return null;
        }
        return nativeGetParent(j10);
    }

    public int getThumbnail(QBitmap thumbnail, int position, boolean skipBlackFrame) {
        long j10 = this.wphandle;
        return 0 == j10 ? QVEError.QERR_COMMON_JAVA_NOT_INIT : nativeGetThumbnail(j10, thumbnail, position, skipBlackFrame);
    }

    public int insertComp(QAEBaseComp comp) {
        long j10 = this.wphandle;
        return 0 == j10 ? QVEError.QERR_COMMON_JAVA_NOT_INIT : nativeInsertComp(j10, comp);
    }

    public int insertKeyFrameData(String keyValue, QKeyFrameUniformData data) {
        long j10 = this.wphandle;
        return 0 == j10 ? QVEError.QERR_COMMON_JAVA_NOT_INIT : nativeInsertKeyFrameData(j10, keyValue, data);
    }

    public int insertKeyFrameDataValue(String keyValue, int timestamp, QKeyFrameUniformData.Value value) {
        long j10 = this.wphandle;
        return 0 == j10 ? QVEError.QERR_COMMON_JAVA_NOT_INIT : nativeInsertKeyFrameDataValue(j10, keyValue, timestamp, value);
    }

    public int loadProject(String prjFile, IQSessionStateListener listener) {
        long j10 = this.wphandle;
        if (0 == j10) {
            return QVEError.QERR_COMMON_JAVA_NOT_INIT;
        }
        this.listener = listener;
        return nativeLoadProject(j10, prjFile);
    }

    public int loadProjectData(String prjFile, IQSessionStateListener listener) {
        long j10 = this.wphandle;
        if (0 == j10) {
            return QVEError.QERR_COMMON_JAVA_NOT_INIT;
        }
        this.listener = listener;
        return nativeLoadProjectData(j10, prjFile);
    }

    public int moveCompByGroup(QAEBaseComp comp, int index) {
        long j10 = this.wphandle;
        return 0 == j10 ? QVEError.QERR_COMMON_JAVA_NOT_INIT : nativeMoveCompByGroup(j10, comp, index);
    }

    public int removeAll() {
        long j10 = this.wphandle;
        return 0 == j10 ? QVEError.QERR_COMMON_JAVA_NOT_INIT : nativeRemoveAll(j10);
    }

    public int removeComp(QAEBaseComp comp) {
        long j10 = this.wphandle;
        return 0 == j10 ? QVEError.QERR_COMMON_JAVA_NOT_INIT : nativeRemoveComp(j10, comp);
    }

    public int removeKeyFrameData(String keyValue) {
        long j10 = this.wphandle;
        return 0 == j10 ? QVEError.QERR_COMMON_JAVA_NOT_INIT : nativeRemoveKeyFrameData(j10, keyValue);
    }

    public int removeKeyFrameDataValue(String keyValue, int timestamp) {
        long j10 = this.wphandle;
        return 0 == j10 ? QVEError.QERR_COMMON_JAVA_NOT_INIT : nativeRemoveKeyFrameDataValue(j10, keyValue, timestamp);
    }

    public int saveProject(String prjFile, IQSessionStateListener listener) {
        long j10 = this.wphandle;
        if (0 == j10) {
            return QVEError.QERR_COMMON_JAVA_NOT_INIT;
        }
        this.listener = listener;
        return nativeSaveProject(j10, prjFile);
    }

    public int setBlendMode(QAEConstants.QAECompBlendMode blendMode) {
        return setProperty(QAEConstants.PROP_AEBASE_ITEM_BLEND_MODE, Integer.valueOf(blendMode.ordinal()));
    }

    public int createThumbnailManager(int width, int height, int resampleMode, int fps, boolean bPrimal, boolean bOnlyOriginal) {
        long j10 = this.wphandle;
        return 0 == j10 ? QVEError.QERR_COMMON_JAVA_NOT_INIT : nativeCreateThumbnailManager(j10, width, height, resampleMode, fps, bPrimal, bOnlyOriginal);
    }
}
