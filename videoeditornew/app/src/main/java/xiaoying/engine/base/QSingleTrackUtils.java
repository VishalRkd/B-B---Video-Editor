package xiaoying.engine.base;

import xiaoying.engine.QEngine;
import xiaoying.utils.QRect;

/* JADX INFO: loaded from: classes19.dex */
public class QSingleTrackUtils {
    public static final int MOTION_TRACK = 0;
    public static final int SINGLE_TRACK = 1;
    private QCallbackWrapper wrapper;
    private long handle = 0;
    private long globalRef = 0;

    public static class QSingleTrackParam {
        public String videoFilePath = null;
        public String jsonFilePath = null;
        public String videoCropJsonPath = null;
        public QRect cropRect = null;
        public QTransformInfo clipTransform = null;
        public QRect rect = null;
        public int flipState = 0;
        public QRange range = new QRange(0, -1);
        public int trackMode = 0;
        public int fps = 0;
    }

    public QSingleTrackUtils() {
        this.wrapper = null;
        this.wrapper = new QCallbackWrapper();
    }

    private native void nativeSingleTrackCancel(long handle);

    private native int nativeSingleTrackPause(long handle);

    private native void nativeSingleTrackRelease(long handle);

    private native int nativeSingleTrackResume(long handle);

    private native int nativeSingleTrackUtilsCreate(QEngine engine, QCallbackWrapper wrapper);

    private native void nativeSingleTrackUtilsDestroy(long handle);

    private native int nativeStartTrack(long handle, QSingleTrackParam param);

    public void Cancel() {
        nativeSingleTrackCancel(this.handle);
    }

    public int Create(QEngine engine, IQSessionStateListener listener) {
        QCallbackWrapper qCallbackWrapper = this.wrapper;
        qCallbackWrapper.listener = listener;
        return nativeSingleTrackUtilsCreate(engine, qCallbackWrapper);
    }

    public void Destroy() {
        nativeSingleTrackUtilsDestroy(this.handle);
        this.handle = 0L;
    }

    public int Pause() {
        return nativeSingleTrackPause(this.handle);
    }

    public void Release() {
        nativeSingleTrackRelease(this.handle);
    }

    public int Resume() {
        return nativeSingleTrackResume(this.handle);
    }

    public int StartTrack(QSingleTrackParam param) {
        return nativeStartTrack(this.handle, param);
    }
}
