package xiaoying.engine.clip;

/* JADX INFO: loaded from: classes19.dex */
public class QBoxEffect extends QEffect {
    public static final int AMVE_PROP_EFFECT_GROUP_BASE = 61440;
    public static final int AMVE_PROP_EFFECT_GROUP_SIZE = 61441;

    private native int nativeClearExternSource();

    private native int nativeDeleteEffect(QEffect qEffect);

    private native QEffect nativeGetEffectByIndex(int index);

    private native QEffect nativeGetEffectByUuid(String uuid);

    private native boolean nativeGetEffectStatus();

    private native Object nativeGetExternSource();

    private native int nativeInsertEffect(QEffect qEffect, int index);

    private native int nativeMoveEffect(QEffect qEffect, int index);

    private native int nativeSetExternSource(Object source);

    private native int nativeSwitchEffectStatus(boolean bStatus);

    public int clearExternSource() {
        return nativeClearExternSource();
    }

    @Override // xiaoying.engine.clip.QEffect
    public int deleteEffect(QEffect qEffect) {
        return nativeDeleteEffect(qEffect);
    }

    @Override // xiaoying.engine.clip.QEffect
    public QEffect getEffectByIndex(int index) {
        return nativeGetEffectByIndex(index);
    }

    public QEffect getEffectByUuid(String uuid) {
        return nativeGetEffectByUuid(uuid);
    }

    @Override // xiaoying.engine.clip.QEffect
    public int getEffectCount() {
        Object property = getProperty(AMVE_PROP_EFFECT_GROUP_SIZE);
        if (property == null) {
            return 0;
        }
        return ((Integer) property).intValue();
    }

    public boolean getEffectStatus() {
        return nativeGetEffectStatus();
    }

    public Object getExternSource() {
        return nativeGetExternSource();
    }

    public int insertEffect(QEffect qEffect, int index) {
        return nativeInsertEffect(qEffect, index);
    }

    public int moveEffect(QEffect qEffect, int index) {
        return nativeMoveEffect(qEffect, index);
    }

    public int setExternSource(Object source) {
        return nativeSetExternSource(source);
    }

    public int switchEffectStatus(boolean bStatus) {
        return nativeSwitchEffectStatus(bStatus);
    }
}
