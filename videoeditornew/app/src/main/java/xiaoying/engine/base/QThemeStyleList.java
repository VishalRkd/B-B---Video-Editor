package xiaoying.engine.base;

/* JADX INFO: loaded from: classes19.dex */
public class QThemeStyleList {
    public static final int QTHEME_TEXT_TYPE_ANIMATE = 3;
    public static final int QTHEME_TEXT_TYPE_COVER = 1;
    public static final int QTHEME_TEXT_TYPE_NONE = 0;
    public static final int QTHEME_TEXT_TYPE_NORMAL = 2;
    public QThemeEffectInfo[] clipFilterLists;
    public QThemeEffectInfo[] clipTransitionLists;
    public QThemeEffectInfo[] sbFilterLists;
    public QThemeEffectInfo[] sbPasterLists;
    public QThemeEffectInfo[] themeTextLists;

    public static class QThemeEffectInfo {
        public String defaultText;
        public long llTemplateID;
        public int nAlignment;
        public int nFilterDuration;
        public int nTextType;
        public QRange range;
    }
}
