package xiaoying.utils.text;

import xiaoying.utils.QSize;

/* JADX INFO: loaded from: classes19.dex */
public class QParagraphMeasureInfo {
    public int alignMode;
    public float fKernPercent;
    public int maxGCnt;
    public int maxLines;
    public int mrMode;
    public QSize oriTextSize;
    public int processMode;
    public QGlyphStyle[] styleList;
    public String txt;

    public QParagraphMeasureInfo() {
        this.txt = null;
        this.processMode = 1;
        this.mrMode = 0;
        this.fKernPercent = 0.0f;
        this.alignMode = 0;
        this.oriTextSize = new QSize();
        this.styleList = null;
        this.maxGCnt = 0;
    }

    public QParagraphMeasureInfo(int styleCnt) {
        this.txt = null;
        this.processMode = 1;
        this.mrMode = 0;
        this.fKernPercent = 0.0f;
        this.alignMode = 0;
        this.oriTextSize = new QSize();
        this.maxLines = 0;
        this.maxGCnt = 0;
        if (styleCnt > 0) {
            this.styleList = new QGlyphStyle[styleCnt];
        }
    }
}
