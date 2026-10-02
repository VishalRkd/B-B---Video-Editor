package xiaoying.engine.base;

import xiaoying.utils.QRect;

/* JADX INFO: loaded from: classes19.dex */
public class QTextMulInfo {
    public QMultiBTInfo[] mMultiBTInfo;
    public int mPreviewPos;
    public QRect mRegionRect;
    public long mTemplateID;
    public int mTextCount;

    public static class QMultiBTInfo {
        public QBubbleTemplateInfo mBTInfo;
        public int mParamID;
        public int mParentParamID;
        public QRect mTextRegion;
    }
}
