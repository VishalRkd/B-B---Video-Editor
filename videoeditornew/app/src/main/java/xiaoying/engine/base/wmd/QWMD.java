package xiaoying.engine.base.wmd;

import xiaoying.engine.QEngine;

/* JADX INFO: loaded from: classes19.dex */
public class QWMD {
    private long hNative = 0;

    private native long nativeWMDInit(QEngine engine, QWMDParameter param);

    private native int nativeWMDPause(long hNative);

    private native int nativeWMDResume(long hNative);

    private native int nativeWMDStart(long hNative);

    private native int nativeWMDStop(long hNative);

    private native void nativeWMDUninit(long hNative);

    public int init(QEngine engine, QWMDParameter param) {
        long jNativeWMDInit = nativeWMDInit(engine, param);
        this.hNative = jNativeWMDInit;
        return 0 == jNativeWMDInit ? -1 : 0;
    }

    public int pause() {
        return nativeWMDPause(this.hNative);
    }

    public int resume() {
        return nativeWMDResume(this.hNative);
    }

    public int start() {
        return nativeWMDStart(this.hNative);
    }

    public int stop() {
        return nativeWMDStop(this.hNative);
    }

    public void unint() {
        nativeWMDUninit(this.hNative);
        this.hNative = 0L;
    }
}
