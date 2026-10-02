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
}
