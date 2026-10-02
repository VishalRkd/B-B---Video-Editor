package xiaoying.engine.clip;

import xiaoying.engine.base.QRange;
import xiaoying.engine.base.QTextAnimationInfo;
import xiaoying.utils.QRect;

/* JADX INFO: loaded from: classes19.dex */
public class QAudioLyricTextInfo {
    public int mLyricTextInfoCount;
    public QLyricTextInfo[] mlyricTextInfoData;

    public static class QLyricTextInfo {
        public int index;
        public QRect rcRegionRation;
        public QTextAnimationInfo textSource;
        public QRange timeRange;
    }
}
