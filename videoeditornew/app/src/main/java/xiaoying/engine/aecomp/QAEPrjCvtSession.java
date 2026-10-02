package xiaoying.engine.aecomp;

import xiaoying.engine.QEngine;
import xiaoying.engine.base.IQSessionStateListener;
import xiaoying.engine.base.QSession;
import xiaoying.engine.base.QVEError;

/* JADX INFO: loaded from: classes19.dex */
public class QAEPrjCvtSession extends QSession {
    private native int nativeConvertNewToOld(String prjNewFile, String prjOldFile);

    private native int nativeConvertOldToNew(String prjOldFile, String prjNewFile);

    private native int nativeConvertSlideShowNewToOld(String prjNewFile, String prjOldFile);

    private native int nativeConvertSlideShowOldToNew(String prjOldFile, String prjNewFile);

    private native int nativeCreate(QEngine engine);

    private native int nativeDestroy();

    public int convertNewToOld(String prjNewFile, String prjOldFile) {
        return 0 == this.handle ? QVEError.QERR_COMMON_JAVA_NOT_INIT : nativeConvertNewToOld(prjNewFile, prjOldFile);
    }

    public int convertOldToNew(String prjOldFile, String prjNewFile) {
        return 0 == this.handle ? QVEError.QERR_COMMON_JAVA_NOT_INIT : nativeConvertOldToNew(prjOldFile, prjNewFile);
    }

    public int convertSlideShowNewToOld(String prjNewFile, String prjOldFile) {
        return 0 == this.handle ? QVEError.QERR_COMMON_JAVA_NOT_INIT : nativeConvertSlideShowNewToOld(prjNewFile, prjOldFile);
    }

    public int convertSlideShowOldToNew(String prjOldFile, String prjNewFile) {
        return 0 == this.handle ? QVEError.QERR_COMMON_JAVA_NOT_INIT : nativeConvertSlideShowOldToNew(prjOldFile, prjNewFile);
    }

    @Override // xiaoying.engine.base.QSession
    public int init(QEngine engine, IQSessionStateListener listener) {
        super.init(engine, listener);
        return nativeCreate(engine);
    }

    @Override // xiaoying.engine.base.QSession
    public int unInit() {
        return 0 == this.handle ? QVEError.QERR_COMMON_JAVA_NOT_INIT : nativeDestroy();
    }
}
