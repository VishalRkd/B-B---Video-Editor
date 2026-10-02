package xiaoying.engine.clip;

import xiaoying.engine.base.QRange;

/* JADX INFO: loaded from: classes19.dex */
public class QEffectTextTokenizeType {
    public int dwIndex = 0;
    public QTextTokenizeInfo[] vecInfo = null;

    public static class QTextTokenizeExtraEffect {
        public boolean enableEffect = false;
        public int shadowColor = 0;
        public float shadowBlurRadius = 0.0f;
        public float shadowXShift = 0.0f;
        public float shadowYShift = 0.0f;
        public int strokeColor = 0;
        public float strokeWPercent = 0.0f;
        public float fWordSpace = 0.0f;
        public float fLineSpace = 0.0f;
    }

    public static class QTextTokenizeInfo {
        public QTextTokenizePlayInfo[] vecPlayInfo = null;
        public QTextTokenizeSourceInfo sourceType = null;
    }

    public static class QTextTokenizePlayInfo {
        public int nStartPos = 0;
        public int nEndPos = 0;
        public QRange playRange = null;
    }

    public static class QTextTokenizeSource {
        public int transparency = 0;
        public int textColor = 0;
        public long templateId = 0;
        public String auxiliaryFont = null;
        public int auxiliaryFontFromType = 0;
        public boolean bold = false;
        public float fontSize = 0.0f;
        public QTextTokenizeExtraEffect extraEffect = null;
        public int changeFlag = 0;
    }

    public static class QTextTokenizeSourceInfo {
        public boolean bUseAdvStyle = false;
        public QEffectTextAdvStyle textAdvStyle = null;
        public QEffectTextAdvStyle.TextBoardConfig boardConfig = null;
        public QTextTokenizeSource textSourceType = null;
    }

    public static class QTextTokenizeStrInfo {
        public QTextTokenizeInfo[] vecInfo = null;
        public String tokenizeText = null;
    }

    public static class QTextTokenizeStrType {
        public QTextTokenizeStrInfo[] vecStrInfo = null;
        public int dwIndex = 0;
    }
}
