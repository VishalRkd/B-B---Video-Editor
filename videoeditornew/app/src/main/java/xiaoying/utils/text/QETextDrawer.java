package xiaoying.utils.text;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;

import java.io.File;
import java.text.Bidi;
import java.text.BreakIterator;
import java.util.ArrayList;

/* JADX INFO: loaded from: classes19.dex */
public class QETextDrawer {
    boolean DEBUG;
    protected int mAutoScale;
    protected float mAutoScaleFactor;
    protected Bitmap mBitmap;
    protected Canvas mCanvas;
    protected QERange[] mCodeIdxs;
    protected float mFontScaleRatio;
    protected float mFontUpscaleFactor;
    protected Paint mGlowPaint;
    protected int mGlowSize;
    protected int mHasThumbnailLayer;
    protected boolean mNeedArrangeTextInLayout;
    protected QETextRect mOriginLayoutTextRect;
    protected TextPaint mPaint;
    protected float mShadowOffsetX;
    protected float mShadowOffsetY;
    protected Paint mShadowPaint;
    protected float mShadowWidth;
    protected int mSingleLine;
    protected int mSizeCx;
    protected int mSizeCy;
    protected int mSplitShadow;
    protected int mStringRight2Left;
    protected Paint mStrokePaint;
    protected int mTargetSizeCx;
    protected int mTargetSizeCy;
    protected float mTextAscent;
    protected float mTextDescent;
    protected QERect mTextLayoutRegion;
    protected float mTextLeading;
    protected QETextRect mTextRect;
    protected int mTextSize;
    protected String mTextStr;
    protected QETextWordDesc mTextWordDesc;
    protected int mUnitType;
    protected int mVerticalText;
    protected int templateType;
    final int NO_SCALE_NO_LINEFEED = 0;
    final int AUTO_SCALE_NO_LINEFEED = 1;
    final int NO_SCALE_AUTO_LINEFEED = 2;
    final int AUTO_SCALE_AUTO_LINEFEED = 3;
    final int MAX_TEXTURE_WIDTH = 1024;
    final int TEMPLATE_TYPE_THUMBNAIL = 2;
    final int TEMPLATE_TYPE_SLIDEPLUS = 1;
    final int TEMPLATE_TYPE_VIVAVIDEO = 0;

    public static class QEAutoLinefeedResult {
        public int fontSize = 0;
        public int width = 0;
        public int height = 0;
        public String textStr = "";
    }

    public static class QEGlyphDesc {
        public int glowColor;
        public int index = 0;
        public int textColor = 0;
        public Matrix matrix = new Matrix();
        public int shadowColor = 0;
        public int strokeColor = 0;
    }

    public static class QELayoutDesc {
        public int fontSize = 0;
        public int layoutWidth = 0;
        public int layoutHeight = 0;
    }

    public static class QERange {

                public int s = 0;

                public int e = 0;
    }

    public static class QERect {

                public float l = 0.0f;

                public float t = 0.0f;

                public float r = 0.0f;

                public float b = 0.0f;
    }

    public static class QETextRect {
        public QERect mRect = new QERect();
        public int lineCount = 0;
        public int[] lineChars = null;
        public QERect[] lineRects = null;
        public int charCount = 0;
        public QERect[] charRects = null;
    }

    public static class QETextWordDesc {
        public int[] wordCharIndex = null;
        public int[] charWordIndex = null;
        public int wordNum = 0;
    }

    public QETextDrawer() {
        this.DEBUG = false;
        TextPaint textPaint = new TextPaint(129);
        this.mPaint = textPaint;
        this.mTextSize = 20;
        textPaint.setTextSize(20);
        this.mPaint.setTypeface(Typeface.DEFAULT);
        this.mTextAscent = this.mPaint.ascent();
        this.mTextAscent = this.mPaint.getFontMetrics().top;
        this.mTextDescent = this.mPaint.descent();
        this.mTextRect = new QETextRect();
        this.mOriginLayoutTextRect = new QETextRect();
        this.mSizeCx = 0;
        this.mSizeCy = 0;
        this.mTargetSizeCx = 0;
        this.mTargetSizeCy = 0;
        this.mBitmap = null;
        this.mCanvas = null;
        this.mTextStr = null;
        this.mShadowPaint = null;
        this.mStrokePaint = null;
        this.mCodeIdxs = null;
        this.mUnitType = 0;
        this.mShadowWidth = 0.0f;
        this.mSplitShadow = 0;
        this.mShadowOffsetX = 0.0f;
        this.mShadowOffsetY = 0.0f;
        this.mAutoScale = 0;
        this.mSingleLine = 1;
        this.mVerticalText = 0;
        this.mFontScaleRatio = 1.0f;
        this.mFontUpscaleFactor = 1.0f;
        QERect qERect = new QERect();
        this.mTextLayoutRegion = qERect;
        qERect.l = 0.0f;
        qERect.t = 0.0f;
        qERect.r = 1.0f;
        qERect.b = 1.0f;
        this.mNeedArrangeTextInLayout = false;
        this.templateType = 0;
        this.mTextWordDesc = new QETextWordDesc();
        this.mStringRight2Left = 0;
        this.DEBUG = false;
        this.mHasThumbnailLayer = 0;
        this.mAutoScaleFactor = 1.0f;
        this.mTextLeading = 0.0f;
    }

    public static int dumpStringChar(String textStr) {
        TextPaint textPaint = new TextPaint(129);
        textPaint.setTextSize(20.0f);
        int iCodePointCount = textStr.codePointCount(0, textStr.length());
        int i10 = 0;
        int i11 = 0;
        while (i10 < iCodePointCount) {
            int iCharCount = Character.charCount(textStr.codePointAt(i11)) + i11;
            textPaint.measureText(textStr, i11, iCharCount);
            i10++;
            i11 = iCharCount;
        }
        return 0;
    }

    public QEAutoLinefeedResult autoLinefeedForVerticalText(String textStr, TextPaint paint, QSize constraints) {
        float f10;
        String[] strArr;
        QEAutoLinefeedResult qEAutoLinefeedResult = new QEAutoLinefeedResult();
        qEAutoLinefeedResult.fontSize = 0;
        float f11 = constraints.x;
        qEAutoLinefeedResult.width = (int) constraints.y;
        qEAutoLinefeedResult.textStr = new String(textStr);
        String[] strArrSplit = textStr.split(System.getProperty("line.separator"));
        int length = strArrSplit.length;
        if (length < 1) {
            return qEAutoLinefeedResult;
        }
        String str = new String(textStr);
        Paint.FontMetrics fontMetrics = paint.getFontMetrics();
        float fAbs = Math.abs(fontMetrics.bottom) + Math.abs(fontMetrics.top) + Math.abs(fontMetrics.leading);
        float f12 = 0.0f;
        int i10 = 0;
        int i11 = 0;
        int i12 = 0;
        float f13 = 0.0f;
        float f14 = 0.0f;
        while (i10 < length) {
            int length2 = strArrSplit[i10].length();
            String str2 = strArrSplit[i10];
            if (str2 == null || length2 <= 0) {
                f10 = fAbs;
                strArr = strArrSplit;
            } else {
                float[] fArr = new float[length2];
                paint.getTextWidths(str2, fArr);
                int i13 = 0;
                while (i13 < length2) {
                    float f15 = fArr[i13];
                    if (f15 > f13) {
                        f13 = f15;
                    }
                    f14 += fAbs;
                    float f16 = fAbs;
                    if (f14 > constraints.y) {
                        int i14 = i11 + i12;
                        i12++;
                        f12 += f13;
                        f14 = f16;
                        str = str.substring(0, i14) + "\n" + str.substring(i14, str.length());
                    }
                    i13++;
                    i11++;
                    fAbs = f16;
                    strArrSplit = strArrSplit;
                }
                f10 = fAbs;
                strArr = strArrSplit;
                f12 += f13;
            }
            i10++;
            fAbs = f10;
            strArrSplit = strArr;
        }
        qEAutoLinefeedResult.width = (int) f12;
        qEAutoLinefeedResult.height = (int) constraints.y;
        qEAutoLinefeedResult.textStr = new String(str);
        return qEAutoLinefeedResult;
    }

    public QEAutoLinefeedResult autoScaleAutoLinefeedVerticalText(int inSize, String textStr, TextPaint paint, QSize constraints) {
        new QEAutoLinefeedResult();
        QEAutoLinefeedResult qEAutoLinefeedResultAutoLinefeedForVerticalText = autoLinefeedForVerticalText(textStr, paint, constraints);
        int i10 = qEAutoLinefeedResultAutoLinefeedForVerticalText.height;
        float f10 = i10;
        float f11 = constraints.y;
        if (f10 > f11 || qEAutoLinefeedResultAutoLinefeedForVerticalText.width > constraints.x) {
            int iMax = (int) (inSize / Math.max((qEAutoLinefeedResultAutoLinefeedForVerticalText.width * 1.0f) / constraints.x, (i10 * 1.0f) / f11));
            while (Math.abs(iMax - inSize) > 1) {
                int i11 = (int) (((double) (iMax + inSize)) * 0.5d);
                paint.setTextSize(i11);
                QEAutoLinefeedResult qEAutoLinefeedResultAutoLinefeedForVerticalText2 = autoLinefeedForVerticalText(textStr, paint, constraints);
                float f12 = qEAutoLinefeedResultAutoLinefeedForVerticalText2.width;
                if (Math.abs((int) (f12 - constraints.x)) < 5) {
                    iMax = i11;
                    qEAutoLinefeedResultAutoLinefeedForVerticalText = qEAutoLinefeedResultAutoLinefeedForVerticalText2;
                    break;
                }
                if (f12 < constraints.x) {
                    iMax = i11;
                } else {
                    inSize = i11;
                }
                qEAutoLinefeedResultAutoLinefeedForVerticalText = qEAutoLinefeedResultAutoLinefeedForVerticalText2;
            }
            qEAutoLinefeedResultAutoLinefeedForVerticalText.fontSize = iMax;
        } else {
            qEAutoLinefeedResultAutoLinefeedForVerticalText.fontSize = inSize;
        }
        return qEAutoLinefeedResultAutoLinefeedForVerticalText;
    }

    public QELayoutDesc calcFontSize(TextPaint textPaint, String textStr, int inSize) {
        float f10;
        char c10;
        int autoScaleFontSize = inSize;
        int i10;
        float f11 = 99999.0f;
        int i11 = this.mTargetSizeCx;
        float length = i11;
        int i12 = this.mTargetSizeCy;
        float f12 = i12;
        if (this.mNeedArrangeTextInLayout) {
            QERect qERect = this.mTextLayoutRegion;
            float f13 = i11 * (qERect.r - qERect.l);
            f12 = (qERect.b - qERect.t) * i12;
            length = f13;
        }
        float[] textRegion = getTextRegion(textPaint, textStr, this.mVerticalText);
        int i13 = (int) textRegion[0];
        int i14 = (int) textRegion[1];
        if (this.mTargetSizeCx == 0 && this.mTargetSizeCy == 0) {
            double d10 = i13;
            length = (float) (d10 + ((d10 * 1.0d) / ((double) textStr.length())));
            f12 = i14;
        }
        if (this.mHasThumbnailLayer != 1) {
            f10 = length;
        } else if (this.mVerticalText == 0) {
            f10 = length;
            f12 = (float) (((double) f12) / (((double) this.mTextLeading) + 1.0d));
        } else {
            length = (float) (((double) length) / (((double) this.mTextLeading) + 1.0d));
            f10 = length;
        }
        float f14 = (i13 * 1.0f) / f10;
        float f15 = (i14 * 1.0f) / f12;
        int i15 = this.mAutoScale;
        if (i15 == 1 && this.mSingleLine == 1) {
            c10 = 1;
        } else if (i15 == 1 && this.mSingleLine == 0) {
            c10 = 3;
        } else {
            c10 = (i15 == 0 && this.mSingleLine == 0) ? (char) 2 : (char) 0;
        }
        if (c10 == 1) {
            if (f14 > 1.0d || f15 > 1.0d) {
                autoScaleFontSize = (int) ((f14 >= f15 ? this.mTextSize / f14 : this.mTextSize / f15) - 1.0d);
            }
            f10 = f11;
            QELayoutDesc qELayoutDesc = new QELayoutDesc();
            qELayoutDesc.fontSize = (int) (autoScaleFontSize * this.mFontScaleRatio);
            qELayoutDesc.layoutWidth = (int) f10;
            qELayoutDesc.layoutHeight = (int) f11;
            return qELayoutDesc;
        }
        if (c10 == 2) {
            if (this.mVerticalText == 1) {
                QSize qSize = new QSize(f10, f12);
                new QSize();
                QEAutoLinefeedResult qEAutoLinefeedResultAutoLinefeedForVerticalText = autoLinefeedForVerticalText(textStr, textPaint, qSize);
                this.mTextStr = new String(qEAutoLinefeedResultAutoLinefeedForVerticalText.textStr);
                i10 = qEAutoLinefeedResultAutoLinefeedForVerticalText.width;
            } else {
                i10 = this.mTargetSizeCx;
            }
            f10 = i10;
            autoScaleFontSize = inSize;
        } else if (c10 == 3) {
            if (this.mVerticalText != 1) {
                autoScaleFontSize = getAutoScaleFontSize(textPaint, textStr, this.mTextSize, f10, f12);
            } else {
                QEAutoLinefeedResult qEAutoLinefeedResultAutoScaleAutoLinefeedVerticalText = autoScaleAutoLinefeedVerticalText(this.mTextSize, this.mTextStr, textPaint, new QSize(f10, f12));
                this.mTextStr = new String(qEAutoLinefeedResultAutoScaleAutoLinefeedVerticalText.textStr);
                f10 = qEAutoLinefeedResultAutoScaleAutoLinefeedVerticalText.width;
                autoScaleFontSize = qEAutoLinefeedResultAutoScaleAutoLinefeedVerticalText.fontSize;
            }
        }
        QELayoutDesc qELayoutDesc2 = new QELayoutDesc();
        qELayoutDesc2.fontSize = (int) (autoScaleFontSize * this.mFontScaleRatio);
        qELayoutDesc2.layoutWidth = (int) f10;
        qELayoutDesc2.layoutHeight = (int) f11;
        return qELayoutDesc2;
    }

    public int doMeasure() {
        int i10;
        int i11;
        ArrayList arrayList;
        int i12;
        TextPaint textPaint;
        ArrayList arrayList2;
        int i13;
        float f10;
        TextPaint textPaint2;
        int i14;
        int i15;
        Rect rect;
        TextPaint textPaint3;
        int i16;
        int i17;
        if (this.DEBUG) {
            if (strstr(this.mTextStr, "ver")) {
                this.mVerticalText = 1;
            }
            if (strstr(this.mTextStr, "ASAL")) {
                this.mAutoScale = 1;
                this.mSingleLine = 0;
            } else if (strstr(this.mTextStr, "NSAL")) {
                this.mAutoScale = 0;
                this.mSingleLine = 0;
            } else if (strstr(this.mTextStr, "NSNL")) {
                this.mAutoScale = 0;
                this.mSingleLine = 1;
            } else {
                this.mAutoScale = 1;
                this.mSingleLine = 1;
            }
        }
        this.mStringRight2Left = isR2LArabic();
        TextPaint textPaint4 = new TextPaint(this.mPaint);
        if (this.mSizeCx <= 0) {
            this.mSizeCx = 1280;
        }
        int i18 = this.mTextSize;
        QELayoutDesc qELayoutDesc = new QELayoutDesc();
        qELayoutDesc.fontSize = i18;
        qELayoutDesc.layoutWidth = this.mSizeCx;
        qELayoutDesc.layoutHeight = this.mSizeCy;
        QELayoutDesc qELayoutDescCalcFontSize = calcFontSize(textPaint4, this.mTextStr, this.mTextSize);
        int i19 = qELayoutDescCalcFontSize.fontSize;
        float f11 = i19;
        this.mAutoScaleFactor = (f11 * 1.0f) / this.mTextSize;
        this.mTextSize = i19;
        if (this.templateType == 0) {
            i19 = (int) (this.mFontUpscaleFactor * f11);
        }
        float f12 = i19;
        this.mPaint.setTextSize(f12);
        Paint paint = this.mShadowPaint;
        if (paint != null) {
            paint.setTextSize(f12);
        }
        Paint paint2 = this.mStrokePaint;
        if (paint2 != null) {
            paint2.setTextSize(f12);
        }
        Paint paint3 = this.mGlowPaint;
        if (paint3 != null) {
            paint3.setTextSize(f12);
        }
        textPaint4.setTextSize(f12);
        Paint.FontMetrics fontMetrics = textPaint4.getFontMetrics();
        float fAbs = Math.abs(fontMetrics.bottom) + Math.abs(fontMetrics.top) + Math.abs(fontMetrics.leading);
        StaticLayout staticLayout = new StaticLayout(this.mTextStr, textPaint4, this.mStringRight2Left == 1 ? 99999 : 1024, Layout.Alignment.ALIGN_NORMAL, 1.0f, 2.0f, false);
        int lineCount = staticLayout.getLineCount();
        if (this.mTextStr.length() >= 2) {
            String str = this.mTextStr;
            if (str.substring(str.length() - 1).equals("\n")) {
                lineCount--;
            }
        }
        QETextRect qETextRect = this.mTextRect;
        qETextRect.lineCount = lineCount;
        qETextRect.lineChars = new int[lineCount];
        qETextRect.lineRects = new QERect[lineCount];
        for (int i20 = 0; i20 < lineCount; i20++) {
            this.mTextRect.lineRects[i20] = new QERect();
        }
        Rect rect2 = new Rect();
        ArrayList arrayList3 = new ArrayList();
        ArrayList arrayList4 = new ArrayList();
        int i21 = 0;
        int i22 = 0;
        float f13 = 0.0f;
        while (i21 < lineCount) {
            StaticLayout staticLayout3 = staticLayout;
            int lineStart = staticLayout3.getLineStart(i21);
            int lineEnd = staticLayout3.getLineEnd(i21);
            if (lineStart > lineEnd) {
                lineStart = lineEnd;
                lineEnd = lineStart;
            }
            ArrayList<QERange> lineRange = getLineRange(this.mTextStr.substring(lineStart, lineEnd), this.mUnitType, lineStart);
            if (-1 == staticLayout3.getParagraphDirection(i21)) {
                int size = lineRange.size() - 1;
                int i23 = 0;
                while (size > i23) {
                    QERange qERange = lineRange.get(i23);
                    QELayoutDesc qELayoutDesc2 = qELayoutDescCalcFontSize;
                    QERange qERange2 = lineRange.get(size);
                    float f14 = f11;
                    int i24 = qERange.s;
                    int i25 = lineCount;
                    int i26 = qERange.e;
                    qERange.s = qERange2.s;
                    qERange.e = qERange2.e;
                    qERange2.s = i24;
                    qERange2.e = i26;
                    i23++;
                    size--;
                    f11 = f14;
                    qELayoutDescCalcFontSize = qELayoutDesc2;
                    lineCount = i25;
                    textPaint4 = textPaint4;
                }
            }
            TextPaint textPaint5 = textPaint4;
            int i27 = lineCount;
            QELayoutDesc qELayoutDesc3 = qELayoutDescCalcFontSize;
            float f15 = f11;
            arrayList4.addAll(lineRange);
            staticLayout3.getLineBounds(i21, rect2);
            QERect qERect = this.mTextRect.lineRects[i21];
            float f16 = rect2.left;
            qERect.l = f16;
            qERect.r = rect2.right;
            qERect.t = f13;
            float f17 = f13 + fAbs;
            qERect.b = f17;
            float f18 = f13 + fAbs;
            int size2 = lineRange.size();
            this.mTextRect.lineChars[i21] = size2;
            int i28 = i22;
            int i29 = 0;
            while (i29 < size2) {
                StaticLayout staticLayout4 = staticLayout3;
                QERange qERange3 = lineRange.get(i29);
                ArrayList<QERange> arrayList5 = lineRange;
                QERect qERect2 = new QERect();
                arrayList3.add(qERect2);
                qERect2.t = f13;
                qERect2.b = f17;
                qERect2.l = f16;
                qERect2.r = f16;
                float f19 = f13;
                textPaint3 = textPaint5;
                if (qERange3.s >= 0) {
                    rect = rect2;
                    if (qERange3.e <= this.mTextStr.length() && (i16 = qERange3.s) < (i17 = qERange3.e)) {
                        textPaint3 = textPaint5;
                        float fMeasureText = textPaint3.measureText(this.mTextStr, i16, i17);
                        if (i21 != i27 - 1 && i29 == size2 - 1 && this.mTextStr.charAt(i28) == '\n') {
                            f16 += 1.0f;
                            qERect2.b = qERect2.t + 1.0f;
                            qERect.r -= fMeasureText;
                        } else {
                            f16 += fMeasureText;
                        }
                        qERect2.r = f16;
                    }
                    i29++;
                    i28++;
                    textPaint5 = textPaint3;
                    lineRange = arrayList5;
                    staticLayout3 = staticLayout4;
                    f13 = f19;
                    rect2 = rect;
                    f18 = f18;
                } else {
                    rect = rect2;
                }
                textPaint3 = textPaint5;
                i29++;
                i28++;
                textPaint5 = textPaint3;
                lineRange = arrayList5;
                staticLayout3 = staticLayout4;
                f13 = f19;
                rect2 = rect;
                f18 = f18;
            }
            StaticLayout staticLayout5 = staticLayout3;
            Rect rect3 = rect2;
            float f20 = f18;
            TextPaint textPaint6 = textPaint5;
            qERect.r = f16;
            QERect qERect3 = this.mTextRect.mRect;
            float f21 = qERect3.l;
            float f22 = qERect.l;
            if (f21 > f22) {
                qERect3.l = f22;
            }
            if (qERect3.r < f16) {
                qERect3.r = f16;
            }
            float f23 = qERect3.t;
            float f24 = qERect.t;
            if (f23 > f24) {
                qERect3.t = f24;
            }
            float f25 = qERect3.b;
            float f26 = qERect.b;
            if (f25 < f26) {
                qERect3.b = f26;
            }
            if (qERect3.r <= qERect3.l || qERect3.b <= qERect3.t) {
                Rect rect4 = new Rect();
                String str2 = this.mTextStr;
                textPaint6.getTextBounds(str2, 0, str2.length(), rect4);
                float f27 = rect4.top;
                qERect3.t = f27;
                qERect3.l = rect4.left;
                qERect3.r = rect4.right;
                lineCount = i27;
                qERect3.b = f27 + (lineCount * fAbs);
            } else {
                lineCount = i27;
            }
            i21++;
            textPaint4 = textPaint6;
            i22 = i28;
            f11 = f15;
            qELayoutDescCalcFontSize = qELayoutDesc3;
            staticLayout = staticLayout5;
            rect2 = rect3;
            f13 = f20;
        }
        TextPaint textPaint7 = textPaint4;
        Rect rect5 = rect2;
        int size3 = arrayList4.size();
        QERange[] qERangeArr = new QERange[size3];
        this.mCodeIdxs = qERangeArr;
        arrayList4.toArray(qERangeArr);
        QETextRect qETextRect2 = this.mTextRect;
        qETextRect2.charCount = size3;
        QERect[] qERectArr = new QERect[size3];
        qETextRect2.charRects = qERectArr;
        arrayList3.toArray(qERectArr);
        textPaint7.setTextSize(f11);
        this.mTextAscent = textPaint7.ascent();
        this.mTextDescent = textPaint7.descent();
        Paint.FontMetrics fontMetrics2 = textPaint7.getFontMetrics();
        float f28 = fontMetrics2.leading;
        float f29 = fontMetrics2.top;
        float f30 = fontMetrics2.bottom;
        this.mTextAscent = f29;
        float fAbs2 = Math.abs(f30) + Math.abs(f29) + Math.abs(f28);
        int i30 = qELayoutDescCalcFontSize.layoutWidth;
        if (this.mHasThumbnailLayer == 0) {
            i10 = 2;
            if (this.templateType != 2) {
                i30 = 9999;
            }
        } else {
            i10 = 2;
        }
        ArrayList arrayList6 = arrayList4;
        Rect rect6 = rect5;
        TextPaint textPaint8 = textPaint7;
        StaticLayout staticLayout6 = new StaticLayout(this.mTextStr, textPaint7, i30, Layout.Alignment.ALIGN_NORMAL, 1.0f, 2.0f, false);
        int lineCount2 = staticLayout6.getLineCount();
        if (this.mTextStr.length() >= i10) {
            String str3 = this.mTextStr;
            if (str3.substring(str3.length() - 1).equals("\n")) {
                lineCount2--;
            }
        }
        QETextRect qETextRect3 = this.mOriginLayoutTextRect;
        qETextRect3.lineCount = lineCount2;
        qETextRect3.lineChars = new int[lineCount2];
        qETextRect3.lineRects = new QERect[lineCount2];
        for (int i31 = 0; i31 < lineCount2; i31++) {
            this.mOriginLayoutTextRect.lineRects[i31] = new QERect();
        }
        Rect rect7 = new Rect();
        ArrayList arrayList7 = new ArrayList();
        ArrayList arrayList8 = new ArrayList();
        float f31 = 0.0f;
        int i32 = 0;
        int i33 = 0;
        while (i32 < lineCount2) {
            int lineStart2 = staticLayout6.getLineStart(i32);
            int lineEnd2 = staticLayout6.getLineEnd(i32);
            if (lineStart2 > lineEnd2) {
                lineEnd2 = lineStart2;
                lineStart2 = lineEnd2;
            }
            ArrayList<QERange> lineRange2 = getLineRange(this.mTextStr.substring(lineStart2, lineEnd2), this.mUnitType, lineStart2);
            if (-1 == staticLayout6.getParagraphDirection(i32)) {
                int size4 = lineRange2.size() - 1;
                int i34 = 0;
                while (size4 > i34) {
                    int i35 = i33;
                    QERange qERange4 = lineRange2.get(i34);
                    ArrayList arrayList9 = arrayList6;
                    QERange qERange5 = lineRange2.get(size4);
                    int i36 = lineCount2;
                    int i37 = qERange4.s;
                    TextPaint textPaint9 = textPaint8;
                    int i38 = qERange4.e;
                    qERange4.s = qERange5.s;
                    qERange4.e = qERange5.e;
                    qERange5.s = i37;
                    qERange5.e = i38;
                    i34++;
                    size4--;
                    lineCount2 = i36;
                    i33 = i35;
                    arrayList6 = arrayList9;
                    arrayList7 = arrayList7;
                    textPaint8 = textPaint9;
                }
                i11 = lineCount2;
                arrayList = arrayList7;
                i12 = i33;
                textPaint = textPaint8;
                arrayList2 = arrayList6;
            } else {
                i11 = lineCount2;
                arrayList = arrayList7;
                i12 = i33;
                textPaint = textPaint8;
                arrayList2 = arrayList6;
            }
            arrayList8.addAll(lineRange2);
            staticLayout6.getLineBounds(i32, rect7);
            QERect qERect4 = this.mOriginLayoutTextRect.lineRects[i32];
            float f32 = rect6.left;
            qERect4.l = f32;
            qERect4.r = rect6.right;
            qERect4.t = f31;
            float f33 = f31 + fAbs2;
            qERect4.b = f33;
            float f34 = f31 + fAbs2;
            int size5 = lineRange2.size();
            this.mOriginLayoutTextRect.lineChars[i32] = size5;
            int i39 = i12;
            int i40 = 0;
            while (i40 < size5) {
                StaticLayout staticLayout7 = staticLayout6;
                QERange qERange6 = lineRange2.get(i40);
                Rect rect8 = rect6;
                QERect qERect5 = new QERect();
                Rect rect9 = rect7;
                ArrayList arrayList10 = arrayList;
                arrayList10.add(qERect5);
                qERect5.t = f31;
                qERect5.b = f33;
                qERect5.l = f32;
                float f35 = f31;
                textPaint2 = textPaint;
                if (qERange6.s >= 0) {
                    f10 = f33;
                    if (qERange6.e <= this.mTextStr.length() && (i14 = qERange6.s) < (i15 = qERange6.e)) {
                        textPaint2 = textPaint;
                        float fMeasureText2 = textPaint2.measureText(this.mTextStr, i14, i15);
                        if (i32 != i11 - 1 && i40 == size5 - 1 && this.mTextStr.charAt(i39) == '\n') {
                            f32 += 1.0f;
                            qERect5.b = qERect5.t + 1.0f;
                            qERect4.r -= fMeasureText2;
                        } else {
                            f32 += fMeasureText2;
                        }
                        qERect5.r = f32;
                    }
                    i40++;
                    i39++;
                    textPaint = textPaint2;
                    f31 = f35;
                    staticLayout6 = staticLayout7;
                    f33 = f10;
                    lineRange2 = lineRange2;
                    rect6 = rect8;
                    arrayList = arrayList10;
                    rect7 = rect9;
                } else {
                    f10 = f33;
                }
                textPaint2 = textPaint;
                i40++;
                i39++;
                textPaint = textPaint2;
                f31 = f35;
                staticLayout6 = staticLayout7;
                f33 = f10;
                lineRange2 = lineRange2;
                rect6 = rect8;
                arrayList = arrayList10;
                rect7 = rect9;
            }
            StaticLayout staticLayout8 = staticLayout6;
            Rect rect10 = rect6;
            Rect rect11 = rect7;
            ArrayList arrayList11 = arrayList;
            TextPaint textPaint10 = textPaint;
            qERect4.r = f32;
            QERect qERect6 = this.mOriginLayoutTextRect.mRect;
            float f36 = qERect6.l;
            float f37 = qERect4.l;
            if (f36 > f37) {
                qERect6.l = f37;
            }
            if (qERect6.r < f32) {
                qERect6.r = f32;
            }
            float f38 = qERect6.t;
            float f39 = qERect4.t;
            if (f38 > f39) {
                qERect6.t = f39;
            }
            float f40 = qERect6.b;
            float f41 = qERect4.b;
            if (f40 < f41) {
                qERect6.b = f41;
            }
            if (qERect6.r <= qERect6.l || qERect6.b <= qERect6.t) {
                Rect rect12 = new Rect();
                String str4 = this.mTextStr;
                textPaint10.getTextBounds(str4, 0, str4.length(), rect12);
                float f42 = rect12.top;
                qERect6.t = f42;
                qERect6.l = rect12.left;
                qERect6.r = rect12.right;
                i13 = i11;
                qERect6.b = f42 + (i13 * fAbs2);
            } else {
                i13 = i11;
            }
            i32++;
            lineCount2 = i13;
            arrayList7 = arrayList11;
            textPaint8 = textPaint10;
            f31 = f34;
            i33 = i39;
            rect7 = rect11;
            arrayList6 = arrayList2;
            staticLayout6 = staticLayout8;
            rect6 = rect10;
        }
        int size6 = arrayList8.size();
        QERange[] qERangeArr2 = new QERange[size6];
        this.mCodeIdxs = qERangeArr2;
        arrayList6.toArray(qERangeArr2);
        QETextRect qETextRect4 = this.mOriginLayoutTextRect;
        qETextRect4.charCount = size6;
        QERect[] qERectArr2 = new QERect[size6];
        qETextRect4.charRects = qERectArr2;
        arrayList7.toArray(qERectArr2);
        splitWord(this.mTextStr);
        return 0;
    }

    public int doProcess(QEGlyphDesc desc) {
        if (this.mCanvas == null || this.mCodeIdxs == null) {
            return -1;
        }
        RectF rectF = new RectF();
        rectF.right = this.mTargetSizeCx;
        rectF.bottom = this.mTargetSizeCy;
        float f10 = 0.0f;
        rectF.top = 0.0f;
        rectF.left = 0.0f;
        Paint paint = new Paint(this.mPaint);
        paint.setStyle(Paint.Style.FILL);
        paint.setARGB(0, 255, 255, 255);
        if (desc.index == 0) {
            this.mCanvas.drawRect(rectF, paint);
        }
        if (this.DEBUG) {
            Paint paint2 = new Paint(this.mPaint);
            paint2.setStyle(Paint.Style.STROKE);
            paint2.setARGB(255, 255, 0, 0);
            paint2.setStrokeWidth(2.0f);
            RectF rectF2 = new RectF();
            QERect qERect = this.mTextRect.charRects[desc.index];
            rectF2.left = qERect.l;
            rectF2.top = qERect.t;
            rectF2.right = qERect.r;
            rectF2.bottom = qERect.b;
            this.mCanvas.setMatrix(new Matrix());
            this.mCanvas.drawRect(rectF2, paint2);
        }
        QERect qERect2 = this.mTextRect.charRects[desc.index];
        this.mCanvas.setMatrix(desc.matrix);
        Paint.FontMetrics fontMetrics = this.mPaint.getFontMetrics();
        float f11 = fontMetrics.leading;
        float f12 = fontMetrics.ascent;
        float f13 = fontMetrics.descent;
        float f14 = fontMetrics.bottom;
        QERect qERect3 = this.mTextRect.charRects[desc.index];
        this.mCanvas.translate(0.0f, (qERect3.b - qERect3.t) - Math.abs(f14));
        QERange qERange = this.mCodeIdxs[desc.index];
        int i10 = qERange.s;
        int i11 = qERange.e;
        float f15 = this.mShadowOffsetX;
        int i12 = this.mTextSize;
        float f16 = f15 * i12;
        float f17 = this.mShadowOffsetY * i12;
        float f18 = this.mShadowWidth;
        if (i10 >= 0 && i11 <= this.mTextStr.length() && qERange.s < qERange.e) {
            this.mPaint.clearShadowLayer();
            if (this.mSplitShadow != 1 || this.templateType == 2) {
                this.mPaint.setShadowLayer(f18, f16, f17, desc.shadowColor);
            } else {
                QERect qERect4 = this.mTextRect.mRect;
                float f19 = qERect4.b - qERect4.t;
                this.mPaint.setShadowLayer(f18, 0.0f, -f19, desc.shadowColor);
                f10 = f19;
            }
            Paint paint3 = this.mGlowPaint;
            if (paint3 != null && this.mGlowSize > 0) {
                this.mCanvas.drawText(this.mTextStr, i10, i11, 0.0f, f10, paint3);
            }
            Paint paint4 = this.mStrokePaint;
            if (paint4 != null) {
                paint4.setColor(desc.strokeColor);
                this.mCanvas.drawText(this.mTextStr, i10, i11, 0.0f, f10, this.mStrokePaint);
            }
            this.mPaint.setColor(desc.textColor);
            this.mCanvas.drawText(this.mTextStr, i10, i11, 0.0f, f10, (Paint) this.mPaint);
            if (this.DEBUG) {
                this.mCanvas.drawLine(0.0f, f10, 3000.0f, f10, this.mPaint);
                Paint paint5 = new Paint(this.mPaint);
                paint5.setStyle(Paint.Style.STROKE);
                paint5.setARGB(255, 0, 255, 0);
                float f20 = f10 + f13;
                this.mCanvas.drawLine(0.0f, f20, 3000.0f, f20, paint5);
                paint5.setARGB(255, 255, 255, 0);
                float f21 = f10 + f12;
                this.mCanvas.drawLine(0.0f, f21, 3000.0f, f21, paint5);
            }
        }
        return 0;
    }

    public int drawColor(int c10) {
        this.mCanvas.drawColor(c10);
        return 0;
    }

    public int getAutoScaleFontSize(TextPaint textPaint, String textStr, int inTextSize, float layoutWidth, float layoutHeight) {
        int i10 = inTextSize;
        int i11 = (int) layoutWidth;
        StaticLayout staticLayout = new StaticLayout(this.mTextStr, textPaint, i11, Layout.Alignment.ALIGN_NORMAL, 1.0f, 0.0f, false);
        Paint.FontMetrics fontMetrics = textPaint.getFontMetrics();
        float lineCount = staticLayout.getLineCount() * (Math.abs(fontMetrics.bottom) + Math.abs(fontMetrics.top) + Math.abs(fontMetrics.leading));
        if (lineCount < layoutHeight) {
            return i10;
        }
        int i12 = (int) (((((double) layoutHeight) * 1.0d) / ((double) lineCount)) * ((double) i10));
        int i13 = (int) (((double) (i12 + i10)) * 0.5d);
        while (Math.abs(i12 - i10) > 1) {
            i13 = (int) (((double) (i12 + i10)) * 0.5d);
            textPaint.setTextSize(i13);
            i11 = i11;
            float height = new StaticLayout(this.mTextStr, textPaint, i11, Layout.Alignment.ALIGN_NORMAL, 1.0f, 0.0f, false).getHeight();
            if (Math.abs(height - layoutHeight) < 3.0f) {
                break;
            }
            if (height < layoutHeight) {
                i12 = i13;
            } else {
                i10 = i13;
                i13 = i12;
            }
        }
        return i13;
    }

    public int getCharsLineIndex(int charIndex) {
        int i10 = 0;
        int i11 = 0;
        while (true) {
            QETextRect qETextRect = this.mTextRect;
            if (i10 >= qETextRect.lineCount || charIndex < (i11 = i11 + qETextRect.lineChars[i10])) {
                break;
            }
            i10++;
        }
        return i10;
    }

    public int getGlyphRange() {
        int length = this.mTextStr.length();
        int i10 = 0;
        int i11 = 0;
        while (i10 < length) {
            char cCharAt = this.mTextStr.charAt(i10);
            if (cCharAt >= 55296 && cCharAt <= 56319) {
                i10++;
            }
            i10++;
            i11++;
        }
        this.mCodeIdxs = new QERange[i11];
        for (int i12 = 0; i12 < i11; i12++) {
            this.mCodeIdxs[i12] = new QERange();
        }
        int i13 = 0;
        int i14 = 0;
        while (i13 < length) {
            char cCharAt2 = this.mTextStr.charAt(i13);
            QERange qERange = this.mCodeIdxs[i14];
            qERange.s = i13;
            int i15 = i13 + 1;
            qERange.e = i15;
            if (cCharAt2 >= 55296 && cCharAt2 <= 56319) {
                qERange.e = i13 + 2;
                i13 = i15;
            }
            i13++;
            i14++;
        }
        return 0;
    }

    public ArrayList<QERange> getLineRange(String textStr, int wordUnit, int offset) {
        ArrayList<QERange> arrayList = new ArrayList<>();
        if (Bidi.requiresBidi(textStr.toCharArray(), 0, textStr.length())) {
            Bidi bidi = new Bidi(textStr, -2);
            int runCount = bidi.getRunCount();
            for (int i10 = 0; i10 < runCount; i10++) {
                int runStart = bidi.getRunStart(i10);
                int runLimit = bidi.getRunLimit(i10);
                arrayList.addAll(getUnitRange(textStr.substring(runStart, runLimit), bidi.getRunLevel(i10), offset + runStart));
            }
        } else {
            arrayList.addAll(getUnitRange(textStr, wordUnit, offset));
        }
        return arrayList;
    }

    public int getPointRange() {
        int iCodePointCount = this.mTextStr.codePointCount(0, this.mTextStr.length());
        this.mCodeIdxs = new QERange[iCodePointCount];
        int iCharCount = 0;
        for (int i10 = 0; i10 < iCodePointCount; i10++) {
            QERange qERange = new QERange();
            this.mCodeIdxs[i10] = qERange;
            qERange.s = iCharCount;
            iCharCount += Character.charCount(this.mTextStr.codePointAt(iCharCount));
            qERange.e = iCharCount;
        }
        return 0;
    }

    public float[] getTextRegion(TextPaint paint, String multiLineStr) {
        String[] strArrSplit = multiLineStr.split(System.getProperty("line.separator"));
        float f10 = 0.0f;
        for (String str : strArrSplit) {
            Rect rect = new Rect();
            paint.getTextBounds(str, 0, str.length(), rect);
            float fWidth = rect.left + rect.width();
            if (f10 < fWidth) {
                f10 = fWidth;
            }
        }
        return new float[]{f10, strArrSplit.length * paint.getFontSpacing()};
    }

    public ArrayList<QERange> getUnitRange(String textStr, int wordUnit, int offset) {
        BreakIterator characterInstance = wordUnit == 0 ? BreakIterator.getCharacterInstance() : BreakIterator.getWordInstance();
        ArrayList<QERange> arrayList = new ArrayList<>();
        characterInstance.setText(textStr);
        int iFirst = characterInstance.first();
        int next = characterInstance.next();
        while (true) {
            int i10 = next;
            int i11 = iFirst;
            iFirst = i10;
            if (-1 == iFirst) {
                return arrayList;
            }
            QERange qERange = new QERange();
            arrayList.add(qERange);
            qERange.s = i11 + offset;
            qERange.e = iFirst + offset;
            next = characterInstance.next();
        }
    }

    public int getWordsRange() {
        if (isR2LArabic() == 0) {
            return getPointRange();
        }
        String[] strArrSplit = this.mTextStr.split(" ");
        int length = strArrSplit.length;
        this.mCodeIdxs = new QERange[length];
        int i10 = 0;
        for (int i11 = 0; i11 < length; i11++) {
            QERange qERange = new QERange();
            this.mCodeIdxs[i11] = qERange;
            qERange.s = i10;
            int length2 = i10 + strArrSplit[i11].length();
            qERange.e = length2;
            i10 = length2 + 1;
        }
        return 0;
    }

    public int isR2LArabic() {
        int iCodePointCount = this.mTextStr.codePointCount(0, this.mTextStr.length());
        int iCharCount = 0;
        for (int i10 = 0; i10 < iCodePointCount; i10++) {
            int iCodePointAt = this.mTextStr.codePointAt(iCharCount);
            if (2 == Character.getDirectionality(iCodePointAt)) {
                return 1;
            }
            iCharCount += Character.charCount(iCodePointAt);
        }
        return 0;
    }

    public int loadState() {
        this.mCanvas.restore();
        return 0;
    }

    public int saveState() {
        this.mCanvas.save();
        return 0;
    }

    public int setAutoScale(int autoScale) {
        this.mAutoScale = autoScale;
        return 0;
    }

    public int setFont(String path) {
        Typeface typefaceGetTypeFace;
        try {
            if (!new File(path).exists()) {
                return 0;
            }
            typefaceGetTypeFace = QFontCache.GetTypeFace(path);
        } catch (RuntimeException unused) {
            typefaceGetTypeFace = null;
        }
        if (typefaceGetTypeFace != null) {
            this.mPaint.setTypeface(typefaceGetTypeFace);
            this.mTextAscent = this.mPaint.ascent();
            this.mTextDescent = this.mPaint.descent();
            this.mTextAscent = this.mPaint.getFontMetrics().top;
        }
        return 0;
    }

    public int setFontScaleRatio(float r10) {
        this.mFontScaleRatio = r10;
        return 0;
    }

    public int setFontUpscaleFactor(float r10) {
        this.mFontUpscaleFactor = r10;
        return 0;
    }

    public int setGlow(float r10, int c10) {
        if (r10 < 0.0f) {
            this.mGlowPaint = null;
            return 0;
        }
        if (this.mGlowPaint == null) {
            this.mGlowPaint = new Paint(this.mPaint);
        }
        this.mGlowPaint.setColor(c10);
        this.mGlowPaint.clearShadowLayer();
        this.mGlowPaint.setShadowLayer(r10, 0.0f, 0.0f, c10);
        this.mGlowSize = (int) r10;
        return 0;
    }

    public int setHasThumbnailLayer(int hasTL) {
        this.mHasThumbnailLayer = hasTL;
        StringBuilder sb2 = new StringBuilder();
        sb2.append("set mHasThumbnailLayer: ");
        sb2.append(Float.toString(this.mHasThumbnailLayer));
        return 0;
    }

    public int setShadow(float r10, int c10, float x10, float y10) {
        if (r10 < 0.0f) {
            this.mShadowPaint = null;
            return 0;
        }
        if (this.mShadowPaint == null) {
            this.mShadowPaint = new TextPaint(this.mPaint);
        }
        this.mShadowPaint.clearShadowLayer();
        this.mShadowPaint.setShadowLayer(r10, x10, y10, c10);
        this.mShadowWidth = r10;
        this.mShadowOffsetX = x10;
        this.mShadowOffsetY = y10;
        return 0;
    }

    public int setSingleLine(int singleLine) {
        this.mSingleLine = singleLine;
        return 0;
    }

    public int setSize(int w10, int h10) {
        this.mSizeCx = w10;
        this.mSizeCy = h10;
        this.mTargetSizeCx = w10;
        this.mTargetSizeCy = h10;
        this.mBitmap = Bitmap.createBitmap(w10, h10, Bitmap.Config.ARGB_8888);
        this.mCanvas = new Canvas(this.mBitmap);
        return this.mBitmap != null ? 0 : -1;
    }

    public int setSplitShadow(int split) {
        this.mSplitShadow = split;
        return 0;
    }

    public int setStroke(float w10, int c10) {
        if (this.mStrokePaint == null) {
            TextPaint textPaint = new TextPaint(this.mPaint);
            this.mStrokePaint = textPaint;
            textPaint.setStyle(Paint.Style.STROKE);
        }
        this.mStrokePaint.setStrokeWidth(w10);
        this.mStrokePaint.setColor(c10);
        return 0;
    }

    public int setTargetSize(int w10, int h10) {
        this.mTargetSizeCx = w10;
        this.mTargetSizeCy = h10;
        return 0;
    }

    public int setTemplateType(int type) {
        this.templateType = type;
        return 0;
    }

    public int setText(String text, float s10, int c10) {
        this.mTextStr = text;
        this.mPaint.setColor(c10);
        this.mPaint.setTextSize(s10);
        this.mTextSize = (int) s10;
        this.mTextAscent = this.mPaint.ascent();
        this.mTextDescent = this.mPaint.descent();
        this.mTextAscent = this.mPaint.getFontMetrics().top;
        Paint paint = this.mShadowPaint;
        if (paint != null) {
            paint.setTextSize(s10);
        }
        Paint paint2 = this.mStrokePaint;
        if (paint2 == null) {
            return 0;
        }
        paint2.setTextSize(s10);
        return 0;
    }

    public int setTextLayoutRegion(float l10, float r10, float t10, float b10) {
        QERect qERect = this.mTextLayoutRegion;
        qERect.l = l10;
        qERect.r = r10;
        qERect.t = t10;
        qERect.b = b10;
        this.mNeedArrangeTextInLayout = true;
        return 0;
    }

    public int setTextLeading(float textLeading) {
        this.mTextLeading = textLeading;
        return 0;
    }

    public int setVerticalText(int vertical) {
        this.mVerticalText = vertical;
        return 0;
    }

    public int splitWord(String str) {
        ArrayList<QERange> unitRange = getUnitRange(str, 1, 0);
        int length = str.length();
        QETextWordDesc qETextWordDesc = this.mTextWordDesc;
        qETextWordDesc.wordCharIndex = new int[length];
        qETextWordDesc.charWordIndex = new int[length];
        int size = unitRange.size();
        for (int i10 = 0; i10 < size; i10++) {
            int i11 = unitRange.get(i10).s;
            int i12 = unitRange.get(i10).e;
            this.mTextWordDesc.wordCharIndex[i10] = i11;
            while (i11 < i12) {
                this.mTextWordDesc.charWordIndex[i11] = i10;
                i11++;
            }
        }
        this.mTextWordDesc.wordNum = size;
        return 0;
    }

    public boolean strstr(String str1, String str2) {
        return str1.contains(str2);
    }

    public static class QSize {

                public float x;

                public float y;

        public QSize() {
            this.x = 0.0f;
            this.y = 0.0f;
        }

        public QSize(float a10, float b10) {
            this.x = a10;
            this.y = b10;
        }
    }

    public float[] getTextRegion(TextPaint paint, String multiLineStr, int bVerticalText) {
        float length;
        float f10;
        String[] strArrSplit = multiLineStr.split(System.getProperty("line.separator"));
        Paint.FontMetrics fontMetrics = paint.getFontMetrics();
        float fAbs = Math.abs(fontMetrics.bottom) + Math.abs(fontMetrics.top) + Math.abs(fontMetrics.leading);
        float f11 = 0.0f;
        if (bVerticalText != 1) {
            for (String str : strArrSplit) {
                Rect rect = new Rect();
                paint.getTextBounds(str, 0, str.length(), rect);
                float fWidth = rect.left + rect.width();
                if (f11 < fWidth) {
                    f11 = fWidth;
                }
            }
            length = strArrSplit.length * fAbs;
        } else {
            float f12 = 0.0f;
            float f13 = 0.0f;
            for (int i10 = 0; i10 < strArrSplit.length; i10++) {
                new Rect();
                int length2 = strArrSplit[i10].length();
                float f14 = length2 * fAbs;
                if (f12 < f14) {
                    f12 = f14;
                }
                String str2 = strArrSplit[i10];
                if (str2 == null || length2 <= 0) {
                    f10 = 0.0f;
                } else {
                    float[] fArr = new float[length2];
                    paint.getTextWidths(str2, fArr);
                    f10 = 0.0f;
                    for (int i11 = 0; i11 < length2; i11++) {
                        float f15 = fArr[i11];
                        if (f15 > f10) {
                            f10 = f15;
                        }
                    }
                }
                f13 += f10;
            }
            length = f12;
            f11 = f13;
        }
        return new float[]{f11, length};
    }
}
