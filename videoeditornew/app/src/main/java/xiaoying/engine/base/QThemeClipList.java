package xiaoying.engine.base;

/* JADX INFO: loaded from: classes19.dex */
public class QThemeClipList {
    public static final int QVET_OLD_THEME_UPLOAD_BASE = 8388608;
    public static final int THEME_TYPE_CARD_POINT = 1;
    public static final int THEME_TYPE_DEFAULT = -1;
    public static final int THEME_TYPE_NORMAL_SHORT_CUT = 0;
    public QThemeClipInfo[] clipList;
    public QThemeClipOutInfo[] clipOutList;
    public int nMaxDuration;
    public int nThemeHeight;
    public int nThemeType;
    public int nThemeWidth;

    public static class QThemeClipInfo {
        public static final long CLIP_NEED_CHECK_BODY = 1;
        public static final long CLIP_NEED_CHECK_FACE = 2;
        public static final long CLIP_NEED_CHECK_VOICE2TEXT = 4;
        public static final int MEDIA_TYPE_NONE = 0;
        public static final int MEDIA_TYPE_PICTURE = 1;
        public static final int MEDIA_TYPE_VIDEO = 2;
        public long nCheckKind;
        public int nClipCoverType;
        public int nCropType;
        public int nDuration;
        public int nMediaType;
    }

    public static class QThemeClipOutInfo {
        public long nCheckKind;
        public int nLen;
        public int nPos;
        public String strFilename;
    }
}
