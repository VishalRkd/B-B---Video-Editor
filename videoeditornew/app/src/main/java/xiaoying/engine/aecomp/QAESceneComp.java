package xiaoying.engine.aecomp;

import xiaoying.engine.QEngine;
import xiaoying.engine.base.QStyle;
import xiaoying.engine.base.QVEError;
import xiaoying.engine.clip.QEffect;
import xiaoying.engine.clip.QSceneClip;
import xiaoying.utils.QPoint;
import xiaoying.utils.QRect;
import xiaoying.utils.QSize;

/* JADX INFO: loaded from: classes19.dex */
public class QAESceneComp extends QAEBaseComp {
    public static final int ALIGNMENT_BOTTOM = 8;
    public static final int ALIGNMENT_DEFAULT = 0;
    public static final int ALIGNMENT_HOR_CENTER = 32;
    public static final int ALIGNMENT_LEFT = 1;
    public static final int ALIGNMENT_MIDDLE = 16;
    public static final int ALIGNMENT_RIGHT = 2;
    public static final int ALIGNMENT_TOP = 4;
    public static final int ALIGNMENT_VER_CENTER = 64;

    public int create(QEngine engine, int groupId, float layerId, long llTemplateID, QSize resolution) {
        int iNativeCreate = nativeCreate(engine, groupId, layerId, 8);
        if (iNativeCreate != 0) {
            return iNativeCreate;
        }
        int sceneTemplate = setSceneTemplate(llTemplateID, resolution);
        if (sceneTemplate != 0) {
            destroy();
        }
        return sceneTemplate;
    }

    public int getElementCount() {
        long j10 = this.wphandle;
        if (0 == j10) {
            return 0;
        }
        return nativeGetElementCount(j10);
    }

    public int getElementFocusImageID(int elementIndex) {
        long j10 = this.wphandle;
        if (0 == j10) {
            return 0;
        }
        return nativeGetElementFocusImageID(j10, elementIndex);
    }

    public int getElementIndexByPoint(int x10, int y10) {
        long j10 = this.wphandle;
        if (0 == j10) {
            return -1;
        }
        return nativeGetElementIndexByPoint(j10, x10, y10);
    }

    public QRect getElementRegion(int elementIndex) {
        long j10 = this.wphandle;
        if (0 == j10) {
            return null;
        }
        return nativeGetElementRegion(j10, elementIndex);
    }

    public int getElementSourceAlignment(int elementIndex) {
        long j10 = this.wphandle;
        if (0 == j10) {
            return 0;
        }
        return nativeGetElementSourceAlignment(j10, elementIndex);
    }

    public QPoint getElementTipsLocation(int elementIndex) {
        long j10 = this.wphandle;
        if (0 == j10) {
            return null;
        }
        return nativeGetElementTipsLocation(j10, elementIndex);
    }

    public int getExternalSource(int index, QEffect.QEffectExternalSource externalSource) {
        long j10 = this.wphandle;
        return 0 == j10 ? QVEError.QERR_COMMON_JAVA_NOT_INIT : nativeGetExternalSource(j10, index, externalSource);
    }

    public QStyle.QEffectPropertyData getPropData(int propID) {
        if (0 == this.wphandle) {
            return null;
        }
        QStyle.QEffectPropertyData qEffectPropertyData = new QStyle.QEffectPropertyData();
        qEffectPropertyData.mID = propID;
        return (QStyle.QEffectPropertyData) getProperty(QAEConstants.PROP_AEBASE_ITEM_PROPDATA, qEffectPropertyData);
    }

    public int getSceneSegMask(QSceneClip.QSceneSegMask segMask) {
        long j10 = this.wphandle;
        return 0 == j10 ? QVEError.QERR_COMMON_JAVA_NOT_INIT : nativeGetSegMask(j10, segMask);
    }

    public long getSceneTemplate() {
        long j10 = this.wphandle;
        if (0 == j10) {
            return -1L;
        }
        return nativeGetSceneTemplate(j10);
    }

    public QSceneClip.QSceneSourceTransform getSourceTransform(int paramID) {
        long j10 = this.wphandle;
        if (0 == j10) {
            return null;
        }
        return nativeGetSourceTransform(j10, paramID);
    }

    public QSceneClip.QSceneSourceTransform[] getSourceTransformList() {
        long j10 = this.wphandle;
        if (0 == j10) {
            return null;
        }
        return nativeGetSourceTransformList(j10);
    }

    public int setExternalSource(int index, QEffect.QEffectExternalSource externalSource) {
        long j10 = this.wphandle;
        return 0 == j10 ? QVEError.QERR_COMMON_JAVA_NOT_INIT : nativeSetExternalSource(j10, index, externalSource);
    }

    public int setSceneSegMask(QSceneClip.QSceneSegMask segMask) {
        long j10 = this.wphandle;
        return 0 == j10 ? QVEError.QERR_COMMON_JAVA_NOT_INIT : nativeSetSegMask(j10, segMask);
    }

    public int setSceneTemplate(long llTemplateID, QSize resolution) {
        long j10 = this.wphandle;
        return 0 == j10 ? QVEError.QERR_COMMON_JAVA_NOT_INIT : nativeSetSceneTemplate(j10, llTemplateID, resolution);
    }

    public int swapElementSource(int elementIndex1, int elementIndex2) {
        long j10 = this.wphandle;
        return 0 == j10 ? QVEError.QERR_COMMON_JAVA_NOT_INIT : nativeSwapElementSource(j10, elementIndex1, elementIndex2);
    }
}
