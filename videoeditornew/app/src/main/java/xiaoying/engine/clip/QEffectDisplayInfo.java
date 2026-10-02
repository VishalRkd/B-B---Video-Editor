package xiaoying.engine.clip;

import xiaoying.utils.QRect;

/* JADX INFO: loaded from: classes19.dex */
public class QEffectDisplayInfo {
    public QRect regionRatio;
    public float rotation = 0.0f;
    public int transparency = 100;

    public QEffectDisplayInfo() {
        this.regionRatio = null;
        QRect qRect = new QRect();
        this.regionRatio = qRect;
        qRect.left = 0;
        qRect.top = 0;
        qRect.right = 10000;
        qRect.bottom = 10000;
    }
}
