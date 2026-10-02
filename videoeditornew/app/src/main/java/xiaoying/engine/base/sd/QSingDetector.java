package xiaoying.engine.base.sd;

/* JADX INFO: loaded from: classes19.dex */
public class QSingDetector {
    private long hNativeCtx = 0;

    private native long nativeSingDetectorCreate(QSingDetectorParameter param);

    private native void nativeSingDetectorDestroy(long h10);

    private native int nativeSingDetectorPause(long h10);

    private native int nativeSingDetectorResume(long h10);

    private native int nativeSingDetectorStart(long h10);

    private native int nativeSingDetectorStop(long h10);

    public int create(QSingDetectorParameter param) {
        long jNativeSingDetectorCreate = nativeSingDetectorCreate(param);
        this.hNativeCtx = jNativeSingDetectorCreate;
        return 0 == jNativeSingDetectorCreate ? -1 : 0;
    }

    public void destroy() {
        nativeSingDetectorDestroy(this.hNativeCtx);
        this.hNativeCtx = 0L;
    }

    public int pause() {
        return nativeSingDetectorPause(this.hNativeCtx);
    }

    public int resume() {
        return nativeSingDetectorResume(this.hNativeCtx);
    }

    public int start() {
        return nativeSingDetectorStart(this.hNativeCtx);
    }

    public int stop() {
        return nativeSingDetectorStop(this.hNativeCtx);
    }
}
