package xiaoying.engine.base;

import android.content.Context;
import xiaoying.engine.QEngine;
import xiaoying.utils.QBitmap;

/* JADX INFO: loaded from: classes19.dex */
public class QMultiDetecUtils {
    private long handle = 0;

    private native boolean nativeCheckPersonByBMP(long handle, QBitmap bitMap);

    private native int nativeMultiDetecCreate(QEngine engine, Context appContext, String strModelFilePath);

    private native void nativeMultiDetecDestroy(long handle);

    public boolean CheckPersonByBMP(QBitmap bitMap) {
        return nativeCheckPersonByBMP(this.handle, bitMap);
    }

    public int Create(QEngine engine, Context appContext, String strModelFilePath) {
        return nativeMultiDetecCreate(engine, appContext, strModelFilePath);
    }

    public void Destroy() {
        nativeMultiDetecDestroy(this.handle);
        this.handle = 0L;
    }
}
