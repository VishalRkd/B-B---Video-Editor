package xiaoying.utils.text;

import xiaoying.basedef.QRectFloat;
import xiaoying.basedef.QSizeFloat;

/* JADX INFO: loaded from: classes19.dex */
public class QParagraphMeasureResult {
    public QRectFloat[] gRectList;
    public QLineInfo[] lineInfoList;
    public QSizeFloat pgSize;

    public QParagraphMeasureResult() {
        this.pgSize = new QSizeFloat();
        this.gRectList = null;
        this.lineInfoList = null;
    }

    public QParagraphMeasureResult(int gCnt, int lineCnt) {
        this.pgSize = new QSizeFloat();
        if (gCnt > 0) {
            this.gRectList = new QRectFloat[gCnt];
        }
        if (lineCnt > 0) {
            this.lineInfoList = new QLineInfo[lineCnt];
        }
    }
}
