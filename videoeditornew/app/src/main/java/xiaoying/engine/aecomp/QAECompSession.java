package xiaoying.engine.aecomp;

import xiaoying.engine.QEngine;
import xiaoying.engine.base.IQSessionStateListener;
import xiaoying.engine.base.QSession;
import xiaoying.engine.base.QVEError;

/* JADX INFO: loaded from: classes19.dex */
public class QAECompSession extends QSession {
    private native int nativeCancelProject();

    private native int nativeCreate(QEngine engine);

    private native int nativeDestroy();

    private native int nativeDisplayRefresh(long handle);

    private native int nativeDuplicate(QAECompSession dst);

    private native QAEProjectData nativeFetchProjectData();

    private native QAEComp nativeGetCompData(long handle);

    private native int nativeGetProjectEngineVersion(String projectPath);

    private native int nativeLoadProject(String prjFile);

    private native int nativeLoadProjectData(String prjFile);

    private native int nativeSaveProject(String prjFile);

    public int cancelProject() {
        return 0 == this.handle ? QVEError.QERR_COMMON_JAVA_NOT_INIT : nativeCancelProject();
    }

    public int displayRefresh() {
        long j10 = this.handle;
        return 0 == j10 ? QVEError.QERR_COMMON_JAVA_NOT_INIT : nativeDisplayRefresh(j10);
    }

    public int duplicate(QAECompSession dstComp) {
        return 0 == this.handle ? QVEError.QERR_COMMON_JAVA_NOT_INIT : nativeDuplicate(dstComp);
    }

    public QAEProjectData fetchProjectData() {
        if (0 == this.handle) {
            return null;
        }
        return nativeFetchProjectData();
    }

    public QAEComp getCompData() {
        long j10 = this.handle;
        if (0 == j10) {
            return null;
        }
        return nativeGetCompData(j10);
    }

    public QEngine getEngine() {
        return this.engine;
    }

    public int getProjectEngineVersion(String projectPath) {
        if (0 == this.handle) {
            return 0;
        }
        return nativeGetProjectEngineVersion(projectPath);
    }

    @Override // xiaoying.engine.base.QSession
    public int init(QEngine engine, IQSessionStateListener listener) {
        super.init(engine, listener);
        nativeCreate(engine);
        return 0;
    }

    public int loadProject(String prjFile, IQSessionStateListener listener) {
        if (0 == this.handle) {
            return QVEError.QERR_COMMON_JAVA_NOT_INIT;
        }
        this.listener = listener;
        return nativeLoadProject(prjFile);
    }

    public int loadProjectData(String prjFile, IQSessionStateListener listener) {
        if (0 == this.handle) {
            return QVEError.QERR_COMMON_JAVA_NOT_INIT;
        }
        this.listener = listener;
        return nativeLoadProjectData(prjFile);
    }

    public int saveProject(String prjFile, IQSessionStateListener listener) {
        if (0 == this.handle) {
            return QVEError.QERR_COMMON_JAVA_NOT_INIT;
        }
        this.listener = listener;
        return nativeSaveProject(prjFile);
    }

    @Override // xiaoying.engine.base.QSession
    public int unInit() {
        return 0 == this.handle ? QVEError.QERR_COMMON_JAVA_NOT_INIT : nativeDestroy();
    }
}
