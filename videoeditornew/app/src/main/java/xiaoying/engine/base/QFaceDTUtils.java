package xiaoying.engine.base;

import android.content.Context;
import xiaoying.basedef.QPointFloat;
import xiaoying.engine.QEngine;
import xiaoying.utils.QRect;

/* JADX INFO: loaded from: classes19.dex */
public class QFaceDTUtils {
    private long handle = 0;

    public static class QFaceDTResult {
        public int faceCount = 0;
        public int maxFaceID = 0;
        public QFaceInfo[] faceinfo = null;
    }

    public static class QFaceExpressionInfo {
        public float fLEyeOpenRatio = 0.0f;
        public float fREyeOpenRatio = 0.0f;
        public float fLEyebrowRaiseRatio = 0.0f;
        public float fREyebrowRaiseRatio = 0.0f;
        public float fMouthOpenRatio = 0.0f;
    }

    public static class QFaceInfo {
        public QPointFloat[] featurePoint = null;
        public QRect faceRect = null;
        public float[] rotation = null;
        public QFaceExpressionInfo expressionInfo = null;
        public int faceId = 0;
    }

    public static class QFaceParam {
        public boolean bOnlyDetectFace = false;
    }

    public static int checkFaceDTLibLicenseFile(String strFilePath) {
        return nativecheckFaceDTLibLicenseFile(strFilePath);
    }

    public static int checkFaceDTlibLicenseData(byte[] filedata) {
        return nativecheckFaceDTLibLicenseData(filedata);
    }

    private native int nativeFDUtilsCreate(QEngine engine, Context appContext, String strtrackData);

    private native int nativeFDUtilsCreate(QEngine engine, Context appContext, QFaceParam faceParam);

    private native void nativeFDUtilsDestroy(long handle);

    private native int nativeFDUtilsDetectFaceByImg(long handle, String strImgFile, QFaceDTResult dtResult);

    private static native int nativecheckFaceDTLibLicenseData(byte[] filedata);

    private static native int nativecheckFaceDTLibLicenseFile(String strFilePath);

    public int Create(QEngine engine, Context appContext, String strtrackData) {
        return nativeFDUtilsCreate(engine, appContext, strtrackData);
    }

    public void Destroy() {
        nativeFDUtilsDestroy(this.handle);
        this.handle = 0L;
    }

    public int DetectFaceByImage(String strImgFile, QFaceDTResult dtResult) {
        return nativeFDUtilsDetectFaceByImg(this.handle, strImgFile, dtResult);
    }

    public int Create(QEngine engine, Context appContext, QFaceParam faceParam) {
        return nativeFDUtilsCreate(engine, appContext, faceParam);
    }
}
