package xiaoying.engine.base.pcm;

import xiaoying.engine.QEngine;

/* JADX INFO: loaded from: classes19.dex */
public class QPCMExtractor {
    private long mHandle = 0;

    private native long nativePCMECreate(QEngine e10, QPCMEParam param);

    private native void nativePCMEDestroy(long h10);

    private native int nativePCMEPause(long h10);

    private native int nativePCMEResume(long h10);

    private native int nativePCMEStart(long h10);

    private native int nativePCMEStop(long h10);

    public int create(QEngine engine, QPCMEParam param) {
        long jNativePCMECreate = nativePCMECreate(engine, param);
        this.mHandle = jNativePCMECreate;
        return 0 == jNativePCMECreate ? -1 : 0;
    }

    public void destroy() {
        nativePCMEDestroy(this.mHandle);
        this.mHandle = 0L;
    }

    public int pause() {
        return nativePCMEPause(this.mHandle);
    }

    public int resume() {
        return nativePCMEResume(this.mHandle);
    }

    public int start() {
        return nativePCMEStart(this.mHandle);
    }

    public int stop() {
        return nativePCMEStop(this.mHandle);
    }
}
