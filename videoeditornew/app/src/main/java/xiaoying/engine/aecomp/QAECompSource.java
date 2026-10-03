package xiaoying.engine.aecomp;

import xiaoying.engine.clip.QMediaMulSource;
import xiaoying.engine.clip.QMediaSource;

/* JADX INFO: loaded from: classes18.dex */
public class QAECompSource {
    private int effectMode;
    private QMediaMulSource multiSource;
    private boolean reverse;
    private QMediaSource source;
    private int sourceType;
    private boolean use2Replace;

    private QAECompSource() {
    }

    public static QAECompSource createAVSource(QMediaSource mediaSource, boolean reverse, boolean use2Replace) {
        QAECompSource qAECompSource = new QAECompSource();
        qAECompSource.source = mediaSource;
        qAECompSource.sourceType = 0;
        qAECompSource.reverse = reverse;
        qAECompSource.use2Replace = use2Replace;
        return qAECompSource;
    }

    public static QAECompSource createAdjustSource(QMediaSource mediaSource, int effectMode) {
        QAECompSource qAECompSource = new QAECompSource();
        qAECompSource.source = mediaSource;
        qAECompSource.sourceType = 0;
        qAECompSource.effectMode = effectMode;
        return qAECompSource;
    }

    public static QAECompSource createPresetSource(QMediaSource mediaSource) {
        QAECompSource qAECompSource = new QAECompSource();
        qAECompSource.source = mediaSource;
        qAECompSource.sourceType = 0;
        return qAECompSource;
    }

    public static QAECompSource createPresetSource(QMediaMulSource mediaMulSource) {
        QAECompSource qAECompSource = new QAECompSource();
        qAECompSource.multiSource = mediaMulSource;
        qAECompSource.sourceType = 1;
        return qAECompSource;
    }

    public static QAECompSource createAVSource(String filePath, boolean reverse) {
        return createAVSource(new QMediaSource(QMediaSource.TYPE_FILE, false, filePath), reverse, false);
    }

    public QMediaSource getSource() {
        return this.source;
    }

    public void setSource(QMediaSource source) {
        this.source = source;
    }

    public int getSourceType() {
        return this.sourceType;
    }

    public void setSourceType(int sourceType) {
        this.sourceType = sourceType;
    }

    public boolean isReverse() {
        return this.reverse;
    }

    public void setReverse(boolean reverse) {
        this.reverse = reverse;
    }

    public boolean isUse2Replace() {
        return this.use2Replace;
    }

    public void setUse2Replace(boolean use2Replace) {
        this.use2Replace = use2Replace;
    }

    public int getEffectMode() {
        return this.effectMode;
    }

    public void setEffectMode(int effectMode) {
        this.effectMode = effectMode;
    }

    public QMediaMulSource getMultiSource() {
        return this.multiSource;
    }

    public void setMultiSource(QMediaMulSource multiSource) {
        this.multiSource = multiSource;
    }
}
