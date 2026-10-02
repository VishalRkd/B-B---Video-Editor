package xiaoying.engine.storyboard;

import xiaoying.engine.base.QVEError;
import xiaoying.engine.clip.QMediaSource;

/* JADX INFO: loaded from: classes19.dex */
public class QThemeAddCoverData {
    private boolean cover = true;
    private String templateFile = null;
    private int coverWidth = 1920;
    private int coverHeight = 1080;
    private int sourceCount = 0;
    private QMediaSource[] source = null;
    private int textCount = 0;
    private QThemeText[] text = null;

    private QThemeAddCoverData() {
    }

    public int getCoverHeight() {
        return this.coverHeight;
    }

    public int getCoverWidth() {
        return this.coverWidth;
    }

    public QMediaSource[] getSource() {
        return this.source;
    }

    public int getSourceCount() {
        return this.sourceCount;
    }

    public String getTemplateFile() {
        return this.templateFile;
    }

    public int getTextCount() {
        return this.textCount;
    }

    public QThemeText[] getTextInfo() {
        return this.text;
    }

    public boolean isCover() {
        return this.cover;
    }

    public void setCoverHeight(int height) {
        this.coverHeight = height;
    }

    public void setCoverWidth(int width) {
        this.coverWidth = width;
    }

    public int setSource(QMediaSource[] source) {
        if (source.length < this.sourceCount) {
            return QVEError.QERR_COMMON_JAVA_INVALID_PARAM;
        }
        this.source = source;
        return 0;
    }
}
