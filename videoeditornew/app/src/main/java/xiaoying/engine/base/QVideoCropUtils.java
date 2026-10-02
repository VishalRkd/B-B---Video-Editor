package xiaoying.engine.base;

import xiaoying.engine.QEngine;
import xiaoying.utils.QRect;

/* JADX INFO: loaded from: classes19.dex */
public class QVideoCropUtils {
    public static final int CROP_MODE_AUTO = 1;
    public static final int CROP_MODE_FAST = 0;
    public static final int PICTURE_CROP_MODE_APT_HEAD_MODE = 5;
    public static final int PICTURE_CROP_MODE_BODY_MODE = 4;
    public static final int PICTURE_CROP_MODE_CHEST_MODE = 6;
    public static final int PICTURE_CROP_MODE_FACE_MODE = 2;
    public static final int PICTURE_CROP_MODE_MAX_IMAGE_CONTENT = 0;
    public static final int PICTURE_CROP_MODE_MAX_TARGET_REGION = 1;
    public static final int PICTURE_CROP_MODE_PET_BODY_MODE = 8;
    public static final int PICTURE_CROP_MODE_PET_HEAD_MODE = 7;
    public static final int PICTURE_CROP_MODE_UPPER_BODY_MODE = 3;
    public static final int SHOT_CROP_MODE_FIX = 0;
    public static final int SHOT_CROP_MODE_MIX = 3;
    public static final int SHOT_CROP_MODE_SCAN = 2;
    public static final int SHOT_CROP_MODE_TRACK = 1;
    private QCallbackWrapper wrapper;
    private long handle = 0;
    private long globalRef = 0;

    public QVideoCropUtils() {
        this.wrapper = null;
        this.wrapper = new QCallbackWrapper();
    }

    private native int nativeSetVideoCropParams(long handle, int cropMode, boolean usePad, int expectWidth, int expectHeight, int picCropMode);

    private native int nativeVideoCropAdjustBox(long handle, String strJsonPath, QRect rect, int shotCropMode, int frameNumber, int timestamp);

    private native void nativeVideoCropCancel(long handle);

    private native QRect nativeVideoCropCropImage(long handle, String strImagePath, QRange range);

    private native int nativeVideoCropCropVideo(long handle, String strVideoPath, QRange range);

    private native int nativeVideoCropGetResult(long handle, String strJsonPath);

    private native int nativeVideoCropGetTaskName(long handle);

    private native int nativeVideoCropPause(long handle);

    private native void nativeVideoCropRelease(long handle);

    private native int nativeVideoCropResume(long handle);

    private native int nativeVideoCropSetCropFps(long handle, int fps);

    private native int nativeVideoCropUtilsCreate(QEngine engine, String strVideoPath, QCallbackWrapper wrapper);

    private native void nativeVideoCropUtilsDestroy(long handle);

    public int AdjustBox(String strJsonPath, QRect rect, int shotCropMode, int frameNumber, int timestamp) {
        return nativeVideoCropAdjustBox(this.handle, strJsonPath, rect, shotCropMode, frameNumber, timestamp);
    }

    public void Cancel() {
        nativeVideoCropCancel(this.handle);
    }

    public int Create(QEngine engine, String strVideoPath, IQSessionStateListener listener) {
        QCallbackWrapper qCallbackWrapper = this.wrapper;
        qCallbackWrapper.listener = listener;
        return nativeVideoCropUtilsCreate(engine, strVideoPath, qCallbackWrapper);
    }

    public QRect CropImage(String strImagePath, QRange range) {
        return nativeVideoCropCropImage(this.handle, strImagePath, range);
    }

    public int CropVideo(String strVideoPath, QRange range) {
        return nativeVideoCropCropVideo(this.handle, strVideoPath, range);
    }

    public void Destroy() {
        nativeVideoCropUtilsDestroy(this.handle);
        this.handle = 0L;
    }

    public int GetResult(String strJsonPath) {
        return nativeVideoCropGetResult(this.handle, strJsonPath);
    }

    public int GetTaskName() {
        return nativeVideoCropGetTaskName(this.handle);
    }

    public int Pause() {
        return nativeVideoCropPause(this.handle);
    }

    public void Release() {
        nativeVideoCropRelease(this.handle);
    }

    public int Resume() {
        return nativeVideoCropResume(this.handle);
    }

    public int SetCropFps(int fps) {
        return nativeVideoCropSetCropFps(this.handle, fps);
    }

    public int SetVideoCropParams(int cropMode, boolean usePad, int expectWidth, int expectHeight) {
        return nativeSetVideoCropParams(this.handle, cropMode, usePad, expectWidth, expectHeight, 0);
    }

    public int SetVideoCropParams(int cropMode, boolean usePad, int expectWidth, int expectHeight, int picCropMode) {
        return nativeSetVideoCropParams(this.handle, cropMode, usePad, expectWidth, expectHeight, picCropMode);
    }
}
