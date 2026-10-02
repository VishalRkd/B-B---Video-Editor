package xiaoying.engine.aecomp;

import xiaoying.engine.QEngine;
import xiaoying.engine.audioanalyze.QAudioAnalyzeParam;
import xiaoying.engine.base.QStyle;
import xiaoying.engine.base.QTransformInfo;
import xiaoying.engine.base.QVEError;
import xiaoying.engine.clip.QClip;
import xiaoying.engine.clip.QEffect;
import xiaoying.engine.clip.QKeyFrameUniformData;
import xiaoying.engine.clip.QSceneClip;
import xiaoying.utils.QBitmap;
import xiaoying.utils.QPoint;
import xiaoying.utils.QRect;
import xiaoying.utils.QSize;

/* JADX INFO: loaded from: classes18.dex */
public abstract class QAEBaseItem {
    protected long wphandle = 0;

    public static native int nativeGetProjectEngineVersion(QEngine engine, String projectPath);

    public static native int nativeGetProjectType(QEngine engine, String projectPath);

    public static native int nativeGetProjectVersion(QEngine engine, String projectPath);

    public int destroy() {
        return 0 == this.wphandle ? QVEError.QERR_COMMON_JAVA_NOT_INIT : nativeDestroy();
    }

    public void finalize() throws Throwable {
        try {
            destroy();
        } catch (Throwable unused) {
        }
        super.finalize();
    }

    public int getContraryScaledValue(int scaledTime) {
        long j10 = this.wphandle;
        if (0 == j10) {
            return 0;
        }
        return nativeGetContraryScaledValue(j10, scaledTime);
    }

    public <T> T getProperty(int i10) {
        long j10 = this.wphandle;
        if (0 == j10) {
            return null;
        }
        return (T) nativeGetProp(j10, i10, null);
    }

    public int getScaledValue(int time) {
        long j10 = this.wphandle;
        if (0 == j10) {
            return 0;
        }
        return nativeGetScaledValue(j10, time);
    }

    public native int nativeAdjustCropBoxByJson(long handle, String strJsonPath, int frameNumber);

    public native int nativeAdjustCropBoxByRect(long handle, QRect rect, int frameNumber);

    public native int nativeApplyTheme(long handle, String themeTemplate);

    public native int nativeCancelProject(long handle);

    public native int nativeCreate(QEngine engine, int groupId, float layerId, int type);

    public native int nativeCreate(QEngine engine, long llTemplateID, QSize resolution);

    public native int nativeCreateThumbnailManager(long handle, int width, int height, int resampleMode, int fps, boolean bPrimal, boolean bOnlyOriginal);

    public native int nativeDestroy();

    public native int nativeDestroyThumbnailManager(long nativeThumbnailManager);

    public native QAEBaseComp nativeDuplicate(long handle);

    public native QAEProjectData nativeFetchProjectData(long handle);

    public native QKeyFrameUniformData nativeFindKeyFrameData(long handle, String keyValue);

    public native QKeyFrameUniformData.Value nativeFindKeyFrameDataValue(long handle, String keyValue, int timestamp);

    public native QTransformInfo nativeGet3DTransformWithKeyFrame(long handle, int timestamp);

    public native QAEBaseComp[] nativeGetCompArrayByTime(long handle, int time);

    public native QAEBaseComp nativeGetCompByGroup(long handle, int groupId, int index);

    public native QAEBaseComp nativeGetCompByIndex(long handle, int index);

    public native QAEBaseComp nativeGetCompByTime(long handle, int time);

    public native QAEBaseComp nativeGetCompByUuid(long handle, String uuid);

    public native int nativeGetCompCount(long handle);

    public native int nativeGetCompCountByGroup(long handle, int groupId);

    public native int nativeGetCompIndex(long handle, QAEBaseComp comp);

    public native int nativeGetCompIndexInGroup(long handle, QAEBaseComp comp);

    public native int nativeGetContraryScaledValue(long handle, int scaledTime);

    public native QRect nativeGetCropBoxByFrameNumber(long handle, int frame);

    public native QRect nativeGetCropBoxByTimestamp(long handle, int timestamp);

    public native int nativeGetCropBoxTimestampByFrameNumber(long handle, int frame);

    public native int nativeGetDuration(long handle);

    public native int nativeGetElementCount(long handle);

    public native int nativeGetElementFocusImageID(long handle, int elementIndex);

    public native int nativeGetElementIndexByPoint(long handle, int x10, int y10);

    public native QRect nativeGetElementRegion(long handle, int elementIndex);

    public native int nativeGetElementSourceAlignment(long handle, int elementIndex);

    public native QPoint nativeGetElementTipsLocation(long handle, int elementIndex);

    public native int nativeGetExternalSource(long handle, int index, QEffect.QEffectExternalSource extsource);

    public native QAEBaseLayer nativeGetLayer(long handle, int layerType);

    public native QAEBaseComp nativeGetParent(long handle);

    public native Object nativeGetProp(long handle, int id2, Object data);

    public native QStyle.QEffectPropertyData nativeGetPropData(long handle, int propID);

    public native int nativeGetScaledValue(long handle, int time);

    public native long nativeGetSceneTemplate(long handle);

    public native int nativeGetSegMask(long handle, QSceneClip.QSceneSegMask segMask);

    public native QSceneClip.QSceneSourceTransform nativeGetSourceTransform(long handle, int paramID);

    public native QSceneClip.QSceneSourceTransform[] nativeGetSourceTransformList(long handle);

    public native int nativeGetThumbnail(long handle, QBitmap bitmap, int position, boolean skipBlackFrame);

    public native QClip.QVideoShotInfo[] nativeGetVideoShotArray(long handle);

    public native int[] nativeGetVideoShotTimestampArray(long handle);

    public native int nativeInsertComp(long handle, QAEBaseComp comp);

    public native int nativeInsertKeyFrameData(long handle, String keyValue, QKeyFrameUniformData data);

    public native int nativeInsertKeyFrameDataValue(long handle, String keyValue, int timestamp, QKeyFrameUniformData.Value value);

    public native int nativeLoadProject(long handle, String prjFile);

    public native int nativeLoadProjectData(long handle, String prjFile);

    public native int nativeMoveCompByGroup(long handle, QAEBaseComp comp, int index);

    public native int nativeRemoveAll(long handle);

    public native int nativeRemoveComp(long handle, QAEBaseComp comp);

    public native int nativeRemoveKeyFrameData(long handle, String keyValue);

    public native int nativeRemoveKeyFrameDataValue(long handle, String keyValue, int timestamp);

    public native int nativeSaveProject(long handle, String prjFile);

    public native int nativeSetExternalSource(long handle, int index, QEffect.QEffectExternalSource extsource);

    public native int nativeSetLyricThemeAVParam(long handle, String strLyricFile, QAudioAnalyzeParam stParam, boolean bSyncClipTimeByLyric, int nSyncType);

    public native int nativeSetLyricThemeClipTransLation(long handle, long lThemeID);

    public native int nativeSetProp(long handle, int propertyId, Object data);

    public native int nativeSetSceneTemplate(long handle, long llTemplateID, QSize resolution);

    public native int nativeSetSegMask(long handle, QSceneClip.QSceneSegMask segMask);

    public native int nativeSetSource(long handle, QAECompSource source);

    public native int nativeSwapElementSource(long handle, int elementIndex1, int elementIndex2);

    public native int nativeUpdateKeyFrameDataOffsetValue(long handle, String keyValue, float offsetValue);

    public int setProperty(int id2, Object data) {
        long j10 = this.wphandle;
        return 0 == j10 ? QVEError.QERR_COMMON_JAVA_NOT_INIT : nativeSetProp(j10, id2, data);
    }

    public <T> T getProperty(int i10, T t10) {
        long j10 = this.wphandle;
        if (0 == j10) {
            return null;
        }
        return (T) nativeGetProp(j10, i10, t10);
    }
}
