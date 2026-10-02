package xiaoying.engine.base;

import xiaoying.engine.QEngine;
import xiaoying.engine.clip.QClip;
import xiaoying.engine.clip.QEffect;
import xiaoying.utils.QBitmap;
import xiaoying.utils.QRect;

/* JADX INFO: loaded from: classes18.dex */
public class QAlgoUtils {
    private QCallbackWrapper wrapper;
    private long handle = 0;
    private long globalRef = 0;
    private int[] algoTypeArray = null;

    public static class QAlgoCacheParam {
        public int timestamp = 0;
        public int index = -1;
        public int algoType = 0;
        public String file = null;
        public String ext = null;
    }

    public static class QAlgoFaceRangeInfo {
        public int faceId = -1;
        public QRange[] rangeList = null;
    }

    public static class QAlgoFaceResult {
        public int faceCount = 0;
        public int firstFaceTime = -1;
        public QAlgoFaceRangeInfo[] faceInfo = null;
    }

    public static class QAlgoMaskData {
        public QRect rect = null;
        public QBitmap bitMask = null;
    }

    public static class QAlgoPersonInstData {
        public int personCount = 0;
        public QAlgoPersonInstInfo[] personInfo = null;
    }

    public static class QAlgoPersonInstInfo {
        public int personId = -1;
        public QRect rect = null;
        public QBitmap bitMask = null;
    }

    public QAlgoUtils() {
        this.wrapper = null;
        this.wrapper = new QCallbackWrapper();
    }

    public static boolean CheckCacheData(QEngine engine, QAlgoCacheParam cacheParam, QRange range) {
        return nativeCheckCacheData(engine, cacheParam, range);
    }

    public static int GetAlgoCacheData(QEngine engine, QAlgoCacheParam cacheParam, Object cacheData) {
        return nativeGetAlgoCacheData(engine, cacheParam, cacheData);
    }

    public static String GetAlgoCachePath(QEngine engine, QAlgoCacheParam cacheParam) {
        return nativeGetAlgoCachePath(engine, cacheParam);
    }

    public static int GetAlgoCacheResult(QEngine engine, QAlgoCacheParam cacheParam, Object resultData) {
        return nativeGetAlgoCacheResult(engine, cacheParam, resultData);
    }

    private native int nativeAlgoUtilsCreate(QEngine engine, QAlgoUtilsInitParam param, QCallbackWrapper wrapper);

    private native int nativeAlgoUtilsCreateByClip(QEngine engine, QClip clip, QCallbackWrapper wrapper);

    private native int nativeAlgoUtilsCreateByEffect(QEngine engine, QEffect effect, QCallbackWrapper wrapper);

    private native void nativeAlgoUtilsDestroy(long handle);

    private native int nativeAlgoUtilsGetResult(long handle, QClip clip);

    private native QAlgoUtilsResult nativeAlgoUtilsGetResult(long handle);

    private native int nativeAlgoUtilsPause(long handle);

    private native int nativeAlgoUtilsResume(long handle);

    private native int nativeAlgoUtilsStart(long handle);

    private native void nativeAlgoUtilsStop(long handle);

    private static native boolean nativeCheckCacheData(QEngine engine, QAlgoCacheParam cacheParam, QRange range);

    private static native int nativeGetAlgoCacheData(QEngine engine, QAlgoCacheParam cacheParam, Object cacheData);

    private static native String nativeGetAlgoCachePath(QEngine engine, QAlgoCacheParam cacheParam);

    private static native int nativeGetAlgoCacheResult(QEngine engine, QAlgoCacheParam cacheParam, Object resultData);

    public int Create(QEngine engine, QAlgoUtilsInitParam param, IQSessionStateListener listener) {
        if (0 != this.handle) {
            return QVEError.QERR_COMMON_JAVA_INVALID_PARAM;
        }
        QCallbackWrapper qCallbackWrapper = this.wrapper;
        qCallbackWrapper.listener = listener;
        return nativeAlgoUtilsCreate(engine, param, qCallbackWrapper);
    }

    public void Destroy() {
        long j10 = this.handle;
        if (0 != j10) {
            nativeAlgoUtilsDestroy(j10);
            this.handle = 0L;
        }
    }

    public QAlgoUtilsResult GetResult() {
        long j10 = this.handle;
        if (0 == j10) {
            return null;
        }
        return nativeAlgoUtilsGetResult(j10);
    }

    public int GetResultToClip(QClip clip) {
        long j10 = this.handle;
        if (0 == j10) {
            return 0;
        }
        return nativeAlgoUtilsGetResult(j10, clip);
    }

    public int Pause() {
        long j10 = this.handle;
        if (0 == j10) {
            return 0;
        }
        return nativeAlgoUtilsPause(j10);
    }

    public int Resume() {
        long j10 = this.handle;
        if (0 == j10) {
            return 0;
        }
        return nativeAlgoUtilsResume(j10);
    }

    public int Start() {
        long j10 = this.handle;
        if (0 == j10) {
            return 0;
        }
        return nativeAlgoUtilsStart(j10);
    }

    public void Stop() {
        long j10 = this.handle;
        if (0 != j10) {
            nativeAlgoUtilsStop(j10);
        }
    }

    public int Create(QEngine engine, QClip clip, IQSessionStateListener listener) {
        if (0 != this.handle) {
            return QVEError.QERR_COMMON_JAVA_INVALID_PARAM;
        }
        QCallbackWrapper qCallbackWrapper = this.wrapper;
        qCallbackWrapper.listener = listener;
        return nativeAlgoUtilsCreateByClip(engine, clip, qCallbackWrapper);
    }

    public int Create(QEngine engine, QEffect effect, IQSessionStateListener listener) {
        if (0 != this.handle) {
            return QVEError.QERR_COMMON_JAVA_INVALID_PARAM;
        }
        QCallbackWrapper qCallbackWrapper = this.wrapper;
        qCallbackWrapper.listener = listener;
        return nativeAlgoUtilsCreateByEffect(engine, effect, qCallbackWrapper);
    }
}
