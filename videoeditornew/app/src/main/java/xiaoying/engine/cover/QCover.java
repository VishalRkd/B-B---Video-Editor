package xiaoying.engine.cover;

import xiaoying.engine.base.QBubbleTextSource;
import xiaoying.engine.base.QVEError;
import xiaoying.engine.clip.QClip;
import xiaoying.engine.clip.QEffect;
import xiaoying.engine.clip.QUserData;

/* JADX INFO: loaded from: classes19.dex */
public class QCover extends QClip {
    public static final int TITLE_EFFECT_GROUP = -1;

    private native int nativeGetTitle(long handle, int index, QBubbleTextSource textsource);

    private native int nativeGetTitleAEWrapper(long handle, int index, QBubbleTextSource textsource);

    private native int nativeGetTitleCount(long handle);

    private native int nativeGetTitleCountAEWrapper(long handle);

    private native QTitleInfo nativeGetTitleDefaultInfo(long handle, int index, int languageID);

    private native QTitleInfo nativeGetTitleDefaultInfoAEWrapper(long handle, int index, int languageID);

    private native int nativeGetTitleEffect(long handle, int index, QEffect effect);

    private native int nativeGetTitleEffectAEWrapper(long handle, int index, QEffect effect);

    private native QUserData nativeGetTitleUserData(long handle, int index);

    private native QUserData nativeGetTitleUserDataAEWrapper(long handle, int index);

    private native int nativeSetTitle(long handle, int index, QBubbleTextSource textsource);

    private native int nativeSetTitleAEWrapper(long handle, int index, QBubbleTextSource textsource);

    private native int nativeSetTitleUserData(long handle, int index, QUserData data);

    private native int nativeSetTitleUserDataAEWrapper(long handle, int index, QUserData data);

    public QBubbleTextSource getTitle(int index) {
        if (0 == this.handle && 0 == this.spweakaehandle) {
            return null;
        }
        QBubbleTextSource qBubbleTextSource = new QBubbleTextSource();
        if ((this.frameworkVersion == 393216 ? nativeGetTitleAEWrapper(this.spweakaehandle, index, qBubbleTextSource) : nativeGetTitle(this.handle, index, qBubbleTextSource)) != 0) {
            return null;
        }
        return qBubbleTextSource;
    }

    public int getTitleCount() {
        long j10 = this.handle;
        if (0 == j10 && 0 == this.spweakaehandle) {
            return 0;
        }
        return this.frameworkVersion == 393216 ? nativeGetTitleCountAEWrapper(this.spweakaehandle) : nativeGetTitleCount(j10);
    }

    public QTitleInfo getTitleDefaultInfo(int index, int languageID) {
        long j10 = this.handle;
        if (0 == j10 && 0 == this.spweakaehandle) {
            return null;
        }
        return this.frameworkVersion == 393216 ? nativeGetTitleDefaultInfoAEWrapper(this.spweakaehandle, index, languageID) : nativeGetTitleDefaultInfo(j10, index, languageID);
    }

    public QEffect getTitleEffect(int index) {
        if (0 == this.handle && 0 == this.spweakaehandle) {
            return null;
        }
        QEffect qEffect = new QEffect();
        if ((this.frameworkVersion == 393216 ? nativeGetTitleEffectAEWrapper(this.spweakaehandle, index, qEffect) : nativeGetTitleEffect(this.handle, index, qEffect)) != 0) {
            return null;
        }
        return qEffect;
    }

    public QUserData getTitleUserData(int index) {
        long j10 = this.handle;
        if (0 == j10 && 0 == this.spweakaehandle) {
            return null;
        }
        return this.frameworkVersion == 393216 ? nativeGetTitleUserDataAEWrapper(this.spweakaehandle, index) : nativeGetTitleUserData(j10, index);
    }

    public int setTitle(int index, QBubbleTextSource source) {
        long j10 = this.handle;
        if (0 == j10 && 0 == this.spweakaehandle) {
            return QVEError.QERR_COMMON_JAVA_NOT_INIT;
        }
        return this.frameworkVersion == 393216 ? nativeSetTitleAEWrapper(this.spweakaehandle, index, source) : nativeSetTitle(j10, index, source);
    }

    public int setTitleUserData(int index, QUserData data) {
        long j10 = this.handle;
        if (0 == j10 && 0 == this.spweakaehandle) {
            return QVEError.QERR_COMMON_JAVA_NOT_INIT;
        }
        return this.frameworkVersion == 393216 ? nativeSetTitleUserDataAEWrapper(this.spweakaehandle, index, data) : nativeSetTitleUserData(j10, index, data);
    }
}
