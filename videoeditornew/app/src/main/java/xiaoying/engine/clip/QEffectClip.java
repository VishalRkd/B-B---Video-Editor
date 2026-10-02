package xiaoying.engine.clip;

import xiaoying.engine.QEngine;
import xiaoying.engine.base.QVEError;

/* JADX INFO: loaded from: classes19.dex */
public class QEffectClip extends QClip {
    private native int nativeCreate(QEngine engine, QEffect effect, QClip clip);

    private native int nativeCreateAEWrapper(QEngine engine, QEffect effect, QClip clip);

    private native int nativeDestroy(QClip clip);

    private native int nativeDestroyAEWrapper(QClip clip);

    private native int nativeGetEffect(QEffect effect);

    private native int nativeGetEffectAEWrappter(QEffect effect);

    public QEffect getEffect() {
        QEffect qEffect = new QEffect();
        if ((this.frameworkVersion == 393216 ? nativeGetEffectAEWrappter(qEffect) : nativeGetEffect(qEffect)) != 0) {
            return null;
        }
        return qEffect;
    }

    public int init(QEngine engine, QEffect effect) {
        if (engine == null) {
            return QVEError.QERR_COMMON_JAVA_INVALID_PARAM;
        }
        int iIntValue = ((Integer) engine.getProperty(112)).intValue();
        this.frameworkVersion = iIntValue;
        return iIntValue == 393216 ? nativeCreateAEWrapper(engine, effect, this) : nativeCreate(engine, effect, this);
    }

    @Override // xiaoying.engine.clip.QClip, xiaoying.engine.base.QSession
    public int unInit() {
        if (0 == this.handle) {
            return QVEError.QERR_COMMON_JAVA_NOT_INIT;
        }
        return this.frameworkVersion == 393216 ? nativeDestroyAEWrapper(this) : nativeDestroy(this);
    }
}
