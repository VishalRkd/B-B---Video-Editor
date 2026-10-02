package xiaoying.engine.base;

import xiaoying.utils.QSize;

/* JADX INFO: loaded from: classes19.dex */
public class QSessionStreamOpenParam {
    public String mStrFaceDTDataFile;
    public QWatermark mWatermark;
    public QWatermarkHideData mWMHideData = null;
    public QSize mFrameSize = new QSize();
    public QSize mRenderTargetSize = new QSize();
    public int mRotation = 0;
    public int mResampleMode = 0;
    public int mDecoderUsageType = 0;
    public int mFps = 0;

    public static class QWatermarkHideData {
        public String mWMUserCode = null;
        public int mWMHideInterval = 0;
    }

    public QSessionStreamOpenParam() {
        this.mWatermark = null;
        this.mWatermark = null;
    }

    public QWatermark getWatermark() {
        return this.mWatermark;
    }

    public void setWatermark(QWatermark waterMark) {
        this.mWatermark = waterMark;
    }
}
