package xiaoying.engine.base;

import xiaoying.utils.QRect;

/* JADX INFO: loaded from: classes19.dex */
public class QTextAnimationInfo {
    public static final int ANIMA_TEXT_CHANGE_FLAG_ALIGN = 1;
    public static final int ANIMA_TEXT_CHANGE_FLAG_BOLD = 16;
    public static final int ANIMA_TEXT_CHANGE_FLAG_FONT_SIZE = 512;
    public static final int ANIMA_TEXT_CHANGE_FLAG_FRONT_FILE = 2;
    public static final int ANIMA_TEXT_CHANGE_FLAG_ITALIC = 32;
    public static final int ANIMA_TEXT_CHANGE_FLAG_LINE_SPACE = 64;
    public static final int ANIMA_TEXT_CHANGE_FLAG_MASK = -1;
    public static final int ANIMA_TEXT_CHANGE_FLAG_SHADOW = 256;
    public static final int ANIMA_TEXT_CHANGE_FLAG_STROKE = 8;
    public static final int ANIMA_TEXT_CHANGE_FLAG_TEXT_COLOR = 4;
    public static final int ANIMA_TEXT_CHANGE_FLAG_WORD_SPACE = 128;
    protected int mPosition = 0;
    private String mStrText = null;
    protected String mDefStrText = null;
    protected int mParamID = 0;
    protected int mTextEditable = 0;
    protected int mAlignment = 0;
    protected String mFont = null;
    protected float mFontSize = 50.0f;
    protected int mFontColor = 0;
    protected boolean mbStoryboardTA = false;
    protected int mIndex = 0;
    public long mllTemplateID = 0;
    public boolean mbVerReversal = false;
    public boolean mbHorReversal = false;
    public QRect mrcRegionRatio = null;
    public float mfRotation = 0.0f;
    public int mShadowColor = 0;
    public float mShadowBlurRadius = 0.0f;
    public float mShadowXShift = 0.0f;
    public float mShadowYShift = 0.0f;
    public int mStrokeColor = 0;
    public float mStrokeWPercent = 0.0f;
    public boolean mbBold = false;
    public boolean mbItalic = false;
    public boolean mbUnderLine = false;
    public float mWordSpace = 0.0f;
    public float mLineSpace = 0.0f;
    public int mChangeFlag = 0;

    public int getAlignment() {
        return this.mAlignment;
    }

    public String getDefText() {
        return this.mDefStrText;
    }

    public int getEditProp() {
        return this.mTextEditable;
    }

    public String getFont() {
        return this.mFont;
    }

    public int getFontColor() {
        return this.mFontColor;
    }

    public float getFontSize() {
        return this.mFontSize;
    }

    public int getPosition() {
        return this.mPosition;
    }

    public String getText() {
        return this.mStrText;
    }

    public int setAlignment(int alignment) {
        if ((this.mTextEditable & 8) == 0) {
            return QVEError.QERR_COMMON_FONT_NOTEDITABLE;
        }
        this.mAlignment = alignment;
        return 0;
    }

    public int setFont(String strFont) {
        this.mFont = strFont;
        return 0;
    }

    public int setFontColor(int fontcolor) {
        this.mFontColor = fontcolor;
        return 0;
    }

    public int setFontSize(float fontsize) {
        if ((this.mTextEditable & 2) == 0) {
            return QVEError.QERR_COMMON_FONT_SIZE_NOTEDITALBE;
        }
        this.mFontSize = fontsize;
        return 0;
    }

    public void setText(String strText) {
        this.mStrText = strText;
    }
}
