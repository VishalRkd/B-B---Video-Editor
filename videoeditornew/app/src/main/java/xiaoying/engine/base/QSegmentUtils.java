package xiaoying.engine.base;

import android.content.Context;
import xiaoying.engine.QEngine;
import xiaoying.utils.QBitmap;
import xiaoying.utils.QPoint;
import xiaoying.utils.QRect;

/* JADX INFO: loaded from: classes19.dex */
public class QSegmentUtils {
    private long handle = 0;

    public static QPoint[] GetPointFromMask(QBitmap mask) {
        return nativeGetPointFromMask(mask);
    }

    private native int nativeCheckSegmentNum(QBitmap bitMap, float fThreshold);

    private native QBitmap nativeGetMaskByBMP(long handle, QBitmap bitMap, int dwRotation);

    private native QBitmap nativeGetMaskByBMPByImgPath(long handle, String strImgFile, int dwRotation);

    private native QRect nativeGetMaskRectByImgPath(long handle, String srcPath, String maskPath, int rotation);

    private static native QPoint[] nativeGetPointFromMask(QBitmap mask);

    private native void nativeQSegmentUtilsDestroy(long handle);

    private native int nativeSegmentUtilsCreate(QEngine engine, Context appContext, String strModelFilePath);

    public int CheckSegmentNum(QBitmap bitMap, float fThreshold) {
        return nativeCheckSegmentNum(bitMap, fThreshold);
    }

    public int Create(QEngine engine, Context appContext, String strModelFilePath) {
        return nativeSegmentUtilsCreate(engine, appContext, strModelFilePath);
    }

    public void Destroy() {
        nativeQSegmentUtilsDestroy(this.handle);
        this.handle = 0L;
    }

    public QBitmap GetMaskByBMP(QBitmap bitMap, int dwRotation) {
        return nativeGetMaskByBMP(this.handle, bitMap, dwRotation);
    }

    public QBitmap GetMaskByBMPByImgPath(String strImgFile, int dwRotation) {
        return nativeGetMaskByBMPByImgPath(this.handle, strImgFile, dwRotation);
    }

    public QRect GetMaskRectByImgPath(String srcPath, String maskPath, int rotation) {
        return nativeGetMaskRectByImgPath(this.handle, srcPath, maskPath, rotation);
    }
}
