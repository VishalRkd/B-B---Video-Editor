package xiaoying.engine.clip;

import xiaoying.engine.QEngine;
import xiaoying.engine.base.QStyle;
import xiaoying.engine.base.QTransformInfo;
import xiaoying.engine.base.QVEError;
import xiaoying.engine.storyboard.QStoryboard;
import xiaoying.utils.QBitmap;
import xiaoying.utils.QPoint;
import xiaoying.utils.QRect;
import xiaoying.utils.QSize;

/* JADX INFO: loaded from: classes19.dex */
public class QSceneClip extends QClip {
    public static final int ALIGNMENT_BOTTOM = 8;
    public static final int ALIGNMENT_DEFAULT = 0;
    public static final int ALIGNMENT_HOR_CENTER = 32;
    public static final int ALIGNMENT_LEFT = 1;
    public static final int ALIGNMENT_MIDDLE = 16;
    public static final int ALIGNMENT_RIGHT = 2;
    public static final int ALIGNMENT_TOP = 4;
    public static final int ALIGNMENT_VER_CENTER = 64;

    public static class QSceneSegMask {
        public int index = 0;
        public QBitmap bitMap = null;
    }

    public static class QSceneSourceTransform {
        public int paramID = 0;
        public QTransformInfo tranform = null;
    }

    public static class QSceneTextDisable {
        public int paramID = 0;
        public boolean isDisable = false;
    }

    private native int nativeCreate(QEngine engine, long llTemplateID, QSize resolution);

    private native int nativeGetElementCount(long handle);

    private native int nativeGetElementFocusImageID(long handle, int elementIndex);

    private native int nativeGetElementIndexByPoint(long handle, int x10, int y10);

    private native QRect nativeGetElementRegion(long handle, int elementIndex);

    private native int nativeGetElementSource(long handle, int elementIndex, QStoryboard source);

    private native int nativeGetElementSourceAlignment(long handle, int elementIndex);

    private native QPoint nativeGetElementTipsLocation(long handle, int elementIndex);

    private native int nativeGetExternalSource(long handle, int index, QEffect.QEffectExternalSource externalSource);

    private native Object nativeGetProp(long handle, int propertyId);

    private native QStyle.QEffectPropertyData nativeGetPropData(long handle, int propID);

    private native long nativeGetSceneTemplate(long handle);

    private native int nativeGetSegMask(QSceneSegMask segMask);

    private native QSceneSourceTransform nativeGetSourceTransform(long handle, int paramID);

    private native QSceneSourceTransform[] nativeGetSourceTransformList(long handle);

    private native int nativeSetElementSource(long handle, int elementIndex, QStoryboard source);

    private native int nativeSetExternalSource(long handle, int index, QEffect.QEffectExternalSource externalSource);

    private native int nativeSetProp(long handle, int propertyId, Object data);

    private native int nativeSetSceneTemplate(long handle, long llTemplateID, QSize resolution);

    private native int nativeSetSegMask(QSceneSegMask segMask);

    private native int nativeSwapElementSource(long handle, int elementIndex1, int elementIndex2);

    public int getElementCount() {
        return nativeGetElementCount(this.handle);
    }

    public int getElementFocusImageID(int elementIndex) {
        return nativeGetElementFocusImageID(this.handle, elementIndex);
    }

    public int getElementIndexByPoint(int x10, int y10) {
        return nativeGetElementIndexByPoint(this.handle, x10, y10);
    }

    public QRect getElementRegion(int elementIndex) {
        return nativeGetElementRegion(this.handle, elementIndex);
    }

    public int getElementSource(int elementIndex, QStoryboard source) {
        return nativeGetElementSource(this.handle, elementIndex, source);
    }

    public int getElementSourceAlignment(int elementIndex) {
        return nativeGetElementSourceAlignment(this.handle, elementIndex);
    }

    public QPoint getElementTipsLocation(int elementIndex) {
        return nativeGetElementTipsLocation(this.handle, elementIndex);
    }

    public int getExternalSource(int index, QEffect.QEffectExternalSource externalSource) {
        return nativeGetExternalSource(this.handle, index, externalSource);
    }

    public QStyle.QEffectPropertyData getPropData(int propID) {
        return nativeGetPropData(this.handle, propID);
    }

    @Override // xiaoying.engine.clip.QClip, xiaoying.engine.base.QSession
    public Object getProperty(int propertyID) {
        long j10 = this.handle;
        if (0 == j10) {
            return null;
        }
        return nativeGetProp(j10, propertyID);
    }

    public int getSceneSegMask(QSceneSegMask segMask) {
        return nativeGetSegMask(segMask);
    }

    public long getSceneTemplate() {
        return nativeGetSceneTemplate(this.handle);
    }

    public QSceneSourceTransform getSourceTransform(int paramID) {
        return nativeGetSourceTransform(this.handle, paramID);
    }

    public QSceneSourceTransform[] getSourceTransformList() {
        return nativeGetSourceTransformList(this.handle);
    }

    public int init(QEngine engine, long llTemplateID, QSize resolution) {
        return nativeCreate(engine, llTemplateID, resolution);
    }

    public int setElementSource(int elementIndex, QStoryboard source) {
        return nativeSetElementSource(this.handle, elementIndex, source);
    }

    public int setExternalSource(int index, QEffect.QEffectExternalSource externalSource) {
        return nativeSetExternalSource(this.handle, index, externalSource);
    }

    @Override // xiaoying.engine.clip.QClip, xiaoying.engine.base.QSession
    public int setProperty(int propertyID, Object data) {
        long j10 = this.handle;
        return 0 == j10 ? QVEError.QERR_COMMON_JAVA_NOT_INIT : nativeSetProp(j10, propertyID, data);
    }

    public int setSceneSegMask(QSceneSegMask segMask) {
        return nativeSetSegMask(segMask);
    }

    public int setSceneTemplate(long llTemplateID, QSize resolution) {
        return nativeSetSceneTemplate(this.handle, llTemplateID, resolution);
    }

    public int swapElementSource(int elementIndex1, int elementIndex2) {
        return nativeSwapElementSource(this.handle, elementIndex1, elementIndex2);
    }
}
