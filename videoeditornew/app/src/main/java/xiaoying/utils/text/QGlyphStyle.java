package xiaoying.utils.text;

/* JADX INFO: loaded from: classes19.dex */
public class QGlyphStyle {
    public float DFontSize;
    public float DShadowBlurRadius;
    public float DShadowXShift;
    public float DShadowYShift;
    public String auxiliaryFont;
    public int gColor;
    public int gCount;
    public int gStartIdx;
    public int shadowColor;
    public float sizeFactor;
    public int strokeColor;
    public float strokeWPercent;

    public QGlyphStyle() {
        this.gStartIdx = 0;
        this.gCount = 0;
        this.auxiliaryFont = null;
        this.gColor = 0;
        this.sizeFactor = 0.0f;
        this.shadowColor = 0;
        this.DFontSize = 0.0f;
        this.DShadowBlurRadius = 0.0f;
        this.DShadowXShift = 0.0f;
        this.DShadowYShift = 0.0f;
        this.strokeColor = 0;
        this.strokeWPercent = 0.0f;
    }

    public QGlyphStyle(QGlyphStyle newGS) {
        this.gStartIdx = newGS.gStartIdx;
        this.gCount = newGS.gCount;
        this.auxiliaryFont = newGS.auxiliaryFont;
        this.gColor = newGS.gColor;
        this.sizeFactor = newGS.sizeFactor;
        this.shadowColor = newGS.shadowColor;
        this.DFontSize = newGS.DFontSize;
        this.DShadowBlurRadius = newGS.DShadowBlurRadius;
        this.DShadowXShift = newGS.DShadowXShift;
        this.DShadowYShift = newGS.DShadowYShift;
        this.strokeColor = newGS.strokeColor;
        this.strokeWPercent = newGS.strokeWPercent;
    }

    public QGlyphStyle(int gStartIdx, int gCount, String auxiliaryFont, int gColor, float sizeFactor, int shadowColor, float DFontSize, float DShadowBlurRadius, float DShadowXShift, float DShadowYShift, int strokeColor, float strokeWPercent) {
        this.gStartIdx = gStartIdx;
        this.gCount = gCount;
        this.auxiliaryFont = auxiliaryFont;
        this.gColor = gColor;
        this.sizeFactor = sizeFactor;
        this.shadowColor = shadowColor;
        this.DFontSize = DFontSize;
        this.DShadowBlurRadius = DShadowBlurRadius;
        this.DShadowXShift = DShadowXShift;
        this.DShadowYShift = DShadowYShift;
        this.strokeColor = strokeColor;
        this.strokeWPercent = strokeWPercent;
    }
}
