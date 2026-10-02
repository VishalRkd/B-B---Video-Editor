package xiaoying.engine.base;

import xiaoying.engine.QEngine;

/* JADX INFO: loaded from: classes19.dex */
public class QTransitionDetectUtils {
    public static final int MUSIC_BEATS = 0;
    public static final int VIDEO_SEG = 1;
    private QCallbackWrapper wrapper;
    private long handle = 0;
    private long globalRef = 0;

    public QTransitionDetectUtils() {
        this.wrapper = null;
        this.wrapper = new QCallbackWrapper();
    }

    private native int nativeDetectTransition(long handle, String input, int pos, int len);

    private native void nativeDetectTransitionCancel(long handle);

    private native int[] nativeGetResult(long handle);

    private native void nativeSetCallbak(long handle, long wrapper);

    private native int nativeTransitionDetectUtilsCreate(QEngine engine, int type, QCallbackWrapper wrapper);

    private native void nativeTransitionDetectUtilsRelease(long handle);

    public void Cancel() {
        nativeDetectTransitionCancel(this.handle);
    }

    public int Create(QEngine engine, int type) {
        return nativeTransitionDetectUtilsCreate(engine, type, this.wrapper);
    }

    public int DetectTransition(String input, int pos, int len) {
        return nativeDetectTransition(this.handle, input, pos, len);
    }

    public int[] GetResult() {
        return nativeGetResult(this.handle);
    }

    public void Release() {
        nativeTransitionDetectUtilsRelease(this.handle);
    }

    public void SetCallback(IQSessionStateListener listener) {
        this.wrapper.listener = listener;
        nativeSetCallbak(this.handle, this.globalRef);
    }
}
