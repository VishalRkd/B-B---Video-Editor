package xiaoying.engine.storyboard;

import xiaoying.engine.clip.QMediaSource;

/* JADX INFO: loaded from: classes19.dex */
public class QThemeText {
    private long templateID = 0;
    private QMediaSource textSource = null;

    private QThemeText() {
    }

    public long getTemplateID() {
        return this.templateID;
    }

    public QMediaSource getTextSource() {
        return this.textSource;
    }

    public int setTextSource(QMediaSource textSource) {
        this.textSource = textSource;
        return 0;
    }
}
