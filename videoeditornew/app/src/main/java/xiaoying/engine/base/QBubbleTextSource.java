package xiaoying.engine.base;

import xiaoying.utils.QPoint;
import xiaoying.utils.QRect;

/* JADX INFO: loaded from: classes19.dex */
public class QBubbleTextSource {
    public String asrJson;
    public String auxiliaryFont;
    public boolean bBold;
    public boolean bItalic;
    public int backgroundColor;
    public long bubbleTemplateID;
    public float fontSize;
    public boolean horizontalReversal;
    public int nChangeFlag;
    public int paramId;
    public int parentParamID;
    public QRect regionRatio;
    public float rotateAngle;
    public QPoint rotateCenter;
    public QTextExtraEffect tee;
    public String text;
    public int textAlignment;
    public int textColor;
    public int transparency;
    public boolean underLine;
    public boolean verticalReversal;

    /* JADX INFO: loaded from: classes18.dex */
    public class QTextExtraEffect {
        public boolean enableEffect = false;
        public int shadowColor = 0;
        public float shadowBlurRadius = 0.0f;
        public float shadowXShift = 0.0f;
        public float shadowYShift = 0.0f;
        public int strokeColor = 0;
        public float strokeWPercent = 0.0f;
        public float fWordSpace = 0.0f;
        public float fLineSpace = 0.0f;

        public QTextExtraEffect() {
        }
    }

    public QBubbleTextSource(QBubbleTextSource BubbleTextSource) {
        this.backgroundColor = 0;
        this.verticalReversal = false;
        this.horizontalReversal = false;
        this.rotateAngle = 0.0f;
        this.rotateCenter = null;
        this.regionRatio = null;
        this.transparency = 0;
        this.textColor = -1;
        this.text = null;
        this.textAlignment = 0;
        this.bubbleTemplateID = 0L;
        this.auxiliaryFont = null;
        this.paramId = 0;
        this.parentParamID = 0;
        this.tee = null;
        this.bBold = false;
        this.bItalic = false;
        this.underLine = false;
        this.nChangeFlag = 0;
        this.fontSize = 0.0f;
        this.asrJson = null;
        this.backgroundColor = BubbleTextSource.backgroundColor;
        this.verticalReversal = BubbleTextSource.verticalReversal;
        this.horizontalReversal = BubbleTextSource.horizontalReversal;
        this.rotateAngle = BubbleTextSource.rotateAngle;
        this.rotateCenter = BubbleTextSource.rotateCenter;
        this.regionRatio = BubbleTextSource.regionRatio;
        this.transparency = BubbleTextSource.transparency;
        this.textColor = BubbleTextSource.textColor;
        this.text = BubbleTextSource.text;
        this.textAlignment = BubbleTextSource.textAlignment;
        this.bubbleTemplateID = BubbleTextSource.bubbleTemplateID;
        this.auxiliaryFont = BubbleTextSource.auxiliaryFont;
        this.bBold = BubbleTextSource.bBold;
        this.bItalic = BubbleTextSource.bItalic;
        this.underLine = BubbleTextSource.underLine;
        this.nChangeFlag = BubbleTextSource.nChangeFlag;
        this.fontSize = BubbleTextSource.fontSize;
        if (BubbleTextSource.tee != null) {
            QTextExtraEffect qTextExtraEffect = new QTextExtraEffect();
            this.tee = qTextExtraEffect;
            QTextExtraEffect qTextExtraEffect2 = BubbleTextSource.tee;
            qTextExtraEffect.enableEffect = qTextExtraEffect2.enableEffect;
            qTextExtraEffect.shadowBlurRadius = qTextExtraEffect2.shadowBlurRadius;
            qTextExtraEffect.shadowColor = qTextExtraEffect2.shadowColor;
            qTextExtraEffect.shadowXShift = qTextExtraEffect2.shadowXShift;
            qTextExtraEffect.shadowYShift = qTextExtraEffect2.shadowYShift;
            qTextExtraEffect.strokeColor = qTextExtraEffect2.strokeColor;
            qTextExtraEffect.strokeWPercent = qTextExtraEffect2.strokeWPercent;
            qTextExtraEffect.fWordSpace = qTextExtraEffect2.fWordSpace;
            qTextExtraEffect.fLineSpace = qTextExtraEffect2.fLineSpace;
        }
    }

    public int getBackgroundColor() {
        return this.backgroundColor;
    }

    public long getBubbleTemplateID() {
        return this.bubbleTemplateID;
    }

    public boolean getHorizontalReversal() {
        return this.horizontalReversal;
    }

    public QRect getRegionRatio() {
        return this.regionRatio;
    }

    public float getRotateAngle() {
        return this.rotateAngle;
    }

    public QPoint getRotateCenter() {
        return this.rotateCenter;
    }

    public String getText() {
        return this.text;
    }

    public int getTextColor() {
        return this.textColor;
    }

    public int getTransparency() {
        return this.transparency;
    }

    public boolean getVerticalReversal() {
        return this.verticalReversal;
    }

    public QBubbleTextSource(int backgroundColor, boolean verticalReversal, boolean horizontalReversal, float rotateAngle, QPoint rotateCenter, QRect regionRatio, int transparency, int textColor, String text, long bubbleTemplateID, String auxiliaryFont) {
        this.textAlignment = 0;
        this.paramId = 0;
        this.parentParamID = 0;
        this.tee = null;
        this.bBold = false;
        this.bItalic = false;
        this.underLine = false;
        this.nChangeFlag = 0;
        this.fontSize = 0.0f;
        this.asrJson = null;
        this.backgroundColor = backgroundColor;
        this.verticalReversal = verticalReversal;
        this.horizontalReversal = horizontalReversal;
        this.rotateAngle = rotateAngle;
        this.rotateCenter = rotateCenter;
        this.regionRatio = regionRatio;
        this.transparency = transparency;
        this.textColor = textColor;
        this.text = text;
        this.bubbleTemplateID = bubbleTemplateID;
        this.auxiliaryFont = auxiliaryFont;
        this.tee = new QTextExtraEffect();
    }

    public QBubbleTextSource() {
        this.backgroundColor = 0;
        this.verticalReversal = false;
        this.horizontalReversal = false;
        this.rotateAngle = 0.0f;
        this.rotateCenter = null;
        this.regionRatio = null;
        this.transparency = 0;
        this.textColor = -1;
        this.text = null;
        this.textAlignment = 0;
        this.bubbleTemplateID = 0L;
        this.auxiliaryFont = null;
        this.paramId = 0;
        this.parentParamID = 0;
        this.tee = null;
        this.bBold = false;
        this.bItalic = false;
        this.underLine = false;
        this.nChangeFlag = 0;
        this.fontSize = 0.0f;
        this.asrJson = null;
    }
}
