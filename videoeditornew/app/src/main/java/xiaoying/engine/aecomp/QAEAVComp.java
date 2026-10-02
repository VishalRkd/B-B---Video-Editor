package xiaoying.engine.aecomp;

import xiaoying.engine.QEngine;
import xiaoying.engine.base.QVEError;
import xiaoying.engine.clip.QClip;
import xiaoying.engine.clip.QMediaSource;
import xiaoying.utils.QRect;

/* JADX INFO: loaded from: classes19.dex */
public class QAEAVComp extends QAEBaseComp {
    public int adjustCropBoxByJson(String strJsonPath, int frameNumber) {
        long j10 = this.wphandle;
        return 0 == j10 ? QVEError.QERR_COMMON_JAVA_NOT_INIT : nativeAdjustCropBoxByJson(j10, strJsonPath, frameNumber);
    }

    public int adjustCropBoxByRect(QRect rect, int frameNumber) {
        long j10 = this.wphandle;
        return 0 == j10 ? QVEError.QERR_COMMON_JAVA_NOT_INIT : nativeAdjustCropBoxByRect(j10, rect, frameNumber);
    }

    public int create(QEngine engine, int groupId, float layerId, QMediaSource mediaSource) {
        return create(engine, groupId, layerId, mediaSource, false);
    }

    public QRect getCropBoxByFrameNumber(int frame) {
        long j10 = this.wphandle;
        if (0 == j10) {
            return null;
        }
        return nativeGetCropBoxByFrameNumber(j10, frame);
    }

    public QRect getCropBoxByTimestamp(int timestamp) {
        long j10 = this.wphandle;
        if (0 == j10) {
            return null;
        }
        return nativeGetCropBoxByTimestamp(j10, timestamp);
    }

    public int getCropBoxTimestampByFrameNumber(int frame) {
        long j10 = this.wphandle;
        return 0 == j10 ? QVEError.QERR_COMMON_JAVA_NOT_INIT : nativeGetCropBoxTimestampByFrameNumber(j10, frame);
    }

    public QClip.QVideoShotInfo[] getVideoShotArray() {
        long j10 = this.wphandle;
        if (0 == j10) {
            return null;
        }
        return nativeGetVideoShotArray(j10);
    }

    public int[] getVideoShotTimestampArray() {
        long j10 = this.wphandle;
        if (0 == j10) {
            return null;
        }
        return nativeGetVideoShotTimestampArray(j10);
    }

    @Override // xiaoying.engine.aecomp.QAEBaseComp
    public int insertComp(QAEBaseComp comp) {
        if ((comp instanceof QAEAdjustComp) || (comp instanceof QAEPresetComp)) {
            return super.insertComp(comp);
        }
        return 2;
    }

    public int setSource(QMediaSource mediaSource) {
        return setSource(mediaSource, false, true);
    }

    public int create(QEngine engine, int groupId, float layerId, QMediaSource mediaSource, boolean isReverse) {
        int iNativeCreate = nativeCreate(engine, groupId, layerId, 2);
        if (iNativeCreate != 0) {
            return iNativeCreate;
        }
        int iNativeSetSource = nativeSetSource(this.wphandle, QAECompSource.createAVSource(mediaSource, isReverse, true));
        if (iNativeSetSource != 0) {
            destroy();
        }
        return iNativeSetSource;
    }

    public int setSource(QMediaSource mediaSource, boolean isReverse, boolean use2Replace) {
        long j10 = this.wphandle;
        return 0 == j10 ? QVEError.QERR_COMMON_JAVA_NOT_INIT : nativeSetSource(j10, QAECompSource.createAVSource(mediaSource, isReverse, use2Replace));
    }
}
