package xiaoying.utils.text;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.text.TextPaint;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.LinkedList;
import xiaoying.basedef.QRange;
import xiaoying.basedef.QRectFloat;
import xiaoying.basedef.QSizeFloat;
import xiaoying.utils.QBitmap;
import xiaoying.utils.QBitmapFactory;
import xiaoying.utils.QColorSpace;
import xiaoying.utils.QSize;

/* JADX INFO: loaded from: classes19.dex */
public class QTextRender {
    private static final int INIT_FONTSIZE_TYPE_HIGHEST_LINE = 1;
    private static final int INIT_FONTSIZE_TYPE_UNIFORMED = 2;
    private static float INIT_FONT_SIZE = 20.0f;
    private static final String LOG_TAG = "QTextRender";
    private static final int STYLE_IDX_FOR_CHAR_ENTER = -1;

    public static class TU_GlyphSequencePickingData {
        QRange[] gRangeList;
        Integer[] sIdxArray;

        private TU_GlyphSequencePickingData() {
        }
    }

    public static class TU_SplittedLinesData {
        String[] lines;
        QRange[] linesGRange;

        private TU_SplittedLinesData() {
        }
    }

    private static boolean IsHighSurrogateChar(char ch2) {
        return ch2 >= 55296 && ch2 <= 56319;
    }

    private static void appendGlyph2DstString(TU_GlyphSequenceData gsd, int gIdx, StringBuilder sb2) {
        if (gsd == null || sb2 == null || gIdx < 0) {
            return;
        }
        QRange[] qRangeArr = gsd.gChRangelist;
        if (gIdx >= qRangeArr.length) {
            return;
        }
        QRange qRange = qRangeArr[gIdx];
        String str = gsd.str;
        int i10 = qRange.start;
        sb2.append(str.substring(i10, qRange.length + i10));
    }

    private static TextPaint createBasicTextPaint(String fontFile, float fontSize) {
        if (fontSize <= 0.0f) {
            return null;
        }
        TextPaint textPaint = new TextPaint(129);
        Typeface typefaceGetTypeFace = QFontCache.GetTypeFace(fontFile);
        if (typefaceGetTypeFace != null) {
            textPaint.setTypeface(typefaceGetTypeFace);
        }
        textPaint.setTextSize(fontSize);
        return textPaint;
    }

    private static TU_ParagraphRenderInfo createRenderInfoWithBasicCutLineInfo(TU_ParagraphInputInfo inputInfo, String cutTxt, int[] gSIdxArray) {
        TU_SplittedLinesData tU_SplittedLinesDataSplit2Lines = split2Lines(cutTxt);
        int length = tU_SplittedLinesDataSplit2Lines.lines.length;
        TU_ParagraphRenderInfo tU_ParagraphRenderInfo = new TU_ParagraphRenderInfo();
        TU_LineInfo[] tU_LineInfoArr = (TU_LineInfo[]) newObjectArray(TU_LineInfo.class, length);
        tU_ParagraphRenderInfo.cutlines = tU_LineInfoArr;
        for (int i10 = 0; i10 < length; i10++) {
            TU_LineInfo tU_LineInfo = tU_LineInfoArr[i10];
            String str = tU_SplittedLinesDataSplit2Lines.lines[i10];
            tU_LineInfo.str = str;
            tU_LineInfo.gChRangeList = getGlyphUnicharRange_mtbl(str);
            tU_LineInfoArr[i10].styleList = reconstructionStyleList_for_cutLine(inputInfo, gSIdxArray, tU_SplittedLinesDataSplit2Lines.linesGRange[i10]);
        }
        return tU_ParagraphRenderInfo;
    }

    private static TextPaint createTextPaint(float baseFontSize, QGlyphStyle style, boolean isWithEffect) {
        if (baseFontSize <= 0.0f || style == null) {
            return null;
        }
        float f10 = style.sizeFactor;
        if (f10 <= 0.0f) {
            return null;
        }
        float f11 = baseFontSize * f10;
        TextPaint textPaintCreateBasicTextPaint = createBasicTextPaint(style.auxiliaryFont, f11);
        textPaintCreateBasicTextPaint.setColor(style.gColor);
        if (isWithEffect) {
            float f12 = style.DShadowBlurRadius;
            if (f12 > 0.0f && style.DFontSize > 0.0f) {
                textPaintCreateBasicTextPaint.setShadowLayer(f12 * f11, style.DShadowXShift * f11, -(f11 * style.DShadowYShift), style.shadowColor);
            }
            if (style.strokeWPercent > 0.0f) {
                Paint.FontMetrics fontMetrics = textPaintCreateBasicTextPaint.getFontMetrics();
                float f13 = (fontMetrics.descent - fontMetrics.ascent) * style.strokeWPercent;
                textPaintCreateBasicTextPaint.setStyle(Paint.Style.STROKE);
                textPaintCreateBasicTextPaint.setStrokeCap(Paint.Cap.ROUND);
                textPaintCreateBasicTextPaint.setStrokeJoin(Paint.Join.ROUND);
                textPaintCreateBasicTextPaint.setStrokeWidth(f13);
                textPaintCreateBasicTextPaint.setColor(style.strokeColor);
            }
        }
        return textPaintCreateBasicTextPaint;
    }

    private static TU_ParagraphRenderInfo cutLine_AutoMultiLineAutoScale(TU_ParagraphInputInfo inputInfo) {
        float f10;
        int i10;
        boolean z10;
        int i11;
        int i12;
        QSizeFloat glyphsSequenceSize = null;
        if (inputInfo == null || 1 != inputInfo.styleList.length) {
            return null;
        }
        float initBaseFontSize = getInitBaseFontSize(inputInfo, 1);
        float fontHeight = getFontHeight(inputInfo.styleList[0].auxiliaryFont, getInitBaseFontSize(inputInfo, 2));
        int length = (inputInfo.txt.length() * 3) + 1;
        int[] iArr = new int[length];
        StringBuilder sb2 = new StringBuilder(length);
        QSize qSize = inputInfo.oriTextSize;
        float f11 = qSize.mWidth;
        float f12 = qSize.mHeight;
        float f13 = 1;
        float f14 = initBaseFontSize / f13;
        float f15 = fontHeight / f13;
        TU_GlyphSequenceData tU_GlyphSequenceData = new TU_GlyphSequenceData();
        tU_GlyphSequenceData.gChRangelist = inputInfo.gChRangeList;
        tU_GlyphSequenceData.gStyleList = inputInfo.styleList;
        tU_GlyphSequenceData.str = inputInfo.txt;
        tU_GlyphSequenceData.mrMode = inputInfo.mrMode;
        tU_GlyphSequenceData.fKernPercent = inputInfo.fKernPercent;
        int i13 = inputInfo.gCnt;
        int i14 = 1;
        int i15 = 0;
        int i16 = 0;
        int i17 = 0;
        int i18 = 0;
        float f16 = f12;
        while (i16 < i13) {
            if (toBeBlankLine(tU_GlyphSequenceData, i16)) {
                if (f16 < f15) {
                    int i19 = i14 + 1;
                    float f17 = i19;
                    QSizeFloat qSizeFloat = new QSizeFloat(0.0f, 0.0f);
                    sb2.delete(0, sb2.length());
                    i14 = i19;
                    f14 = initBaseFontSize / f17;
                    f16 = f12;
                    f10 = f16;
                    glyphsSequenceSize = qSizeFloat;
                    i16 = -1;
                    i12 = 1;
                    i17 = 0;
                    i10 = 0;
                    i11 = 0;
                    f15 = fontHeight / f17;
                } else {
                    sb2.append('\n');
                    f16 -= f15;
                    iArr[i17] = -1;
                    i17++;
                    f10 = f12;
                    i11 = i18;
                    i12 = 1;
                    i10 = i15;
                }
                z10 = false;
            } else if (isNewLineSymbol(tU_GlyphSequenceData, i16)) {
                sb2.append('\n');
                iArr[i17] = -1;
                i10 = i16 + 1;
                i17++;
                f10 = f12;
                z10 = false;
                i12 = 1;
                i11 = 0;
            } else {
                QSizeFloat qSizeFloat2 = glyphsSequenceSize != null ? new QSizeFloat(glyphsSequenceSize) : new QSizeFloat(0.0f, 0.0f);
                f10 = f12;
                int i20 = i18 + 1;
                glyphsSequenceSize = getGlyphsSequenceSize(tU_GlyphSequenceData, new QRange(i15, i20), f14);
                i10 = i15;
                if (glyphsSequenceSize.w > f11) {
                    sb2.append('\n');
                    f16 -= qSizeFloat2.h;
                    iArr[i17] = -1;
                    i17++;
                    i10 = i16;
                    z10 = false;
                    i11 = 0;
                    i16--;
                } else if (f16 < glyphsSequenceSize.h) {
                    int i21 = i14 + 1;
                    float f18 = i21;
                    float f19 = initBaseFontSize / f18;
                    QSizeFloat qSizeFloat3 = new QSizeFloat(0.0f, 0.0f);
                    sb2.delete(0, sb2.length());
                    i14 = i21;
                    f15 = fontHeight / f18;
                    glyphsSequenceSize = qSizeFloat3;
                    z10 = false;
                    i17 = 0;
                    i10 = 0;
                    i11 = 0;
                    i16 = -1;
                    f16 = f10;
                    f14 = f19;
                } else {
                    z10 = false;
                    appendGlyph2DstString(tU_GlyphSequenceData, i16, sb2);
                    iArr[i17] = getGlyphStyleIdx_From_InputInfo(inputInfo, i16);
                    i17++;
                    i11 = i20;
                }
                i12 = 1;
            }
            i16 += i12;
            i15 = i10;
            i18 = i11;
            f12 = f10;
        }
        return createRenderInfoWithBasicCutLineInfo(inputInfo, sb2.toString(), iArr);
    }

    private static TU_ParagraphRenderInfo cutLine_AutoMultiLineNoScale(TU_ParagraphInputInfo inputInfo) {
        float f10 = INIT_FONT_SIZE;
        float f11 = inputInfo.oriTextSize.mWidth;
        float fontHeight = getFontHeight(inputInfo.highestFont, inputInfo.highestSizeFactor * f10);
        float fontHeight2 = getFontHeight(null, INIT_FONT_SIZE * 1.0f);
        if (fontHeight <= fontHeight2) {
            fontHeight = fontHeight2;
        }
        float f12 = (inputInfo.oriTextSize.mHeight * INIT_FONT_SIZE) / fontHeight;
        TU_GlyphSequenceData tU_GlyphSequenceData = new TU_GlyphSequenceData();
        tU_GlyphSequenceData.gChRangelist = inputInfo.gChRangeList;
        tU_GlyphSequenceData.gStyleList = inputInfo.styleList;
        String str = inputInfo.txt;
        tU_GlyphSequenceData.str = str;
        tU_GlyphSequenceData.mrMode = inputInfo.mrMode;
        tU_GlyphSequenceData.fKernPercent = inputInfo.fKernPercent;
        int length = (str.length() * 3) + 1;
        int[] iArr = new int[length];
        StringBuilder sb2 = new StringBuilder(length);
        int i10 = inputInfo.gCnt;
        int i11 = 0;
        int i12 = 0;
        int i13 = 0;
        for (int i14 = 0; i14 < i10; i14++) {
            if (isNewLineSymbol(tU_GlyphSequenceData, i14)) {
                sb2.append('\n');
                i11 = i14 + 1;
                iArr[i13] = -1;
                i13++;
                i12 = 0;
            } else {
                i12++;
                if (getGlyphsSequenceSize(tU_GlyphSequenceData, new QRange(i11, i12), f12).w > f11) {
                    sb2.append('\n');
                    int i15 = i13 + 1;
                    iArr[i13] = -1;
                    appendGlyph2DstString(tU_GlyphSequenceData, i14, sb2);
                    i13 += 2;
                    iArr[i15] = getGlyphStyleIdx_From_InputInfo(inputInfo, i14);
                    i12 = 1;
                    i11 = i14;
                } else {
                    appendGlyph2DstString(tU_GlyphSequenceData, i14, sb2);
                    iArr[i13] = getGlyphStyleIdx_From_InputInfo(inputInfo, i14);
                    i13++;
                }
            }
        }
        TU_ParagraphRenderInfo tU_ParagraphRenderInfoCreateRenderInfoWithBasicCutLineInfo = createRenderInfoWithBasicCutLineInfo(inputInfo, sb2.toString(), iArr);
        tU_ParagraphRenderInfoCreateRenderInfoWithBasicCutLineInfo.baseFontSize = f12;
        return tU_ParagraphRenderInfoCreateRenderInfoWithBasicCutLineInfo;
    }

    private static void decideFontSize_AutoMultiLineAutoScale(TU_ParagraphInputInfo inputInfo, TU_ParagraphRenderInfo rInfo) throws Exception {
        QSizeFloat qSizeFloat;
        if (rInfo != null) {
            TU_LineInfo[] tU_LineInfoArr = rInfo.cutlines;
            if (tU_LineInfoArr.length != 0) {
                int length = tU_LineInfoArr.length;
                float f10 = INIT_FONT_SIZE;
                TU_GlyphSequenceData tU_GlyphSequenceData = new TU_GlyphSequenceData();
                float f11 = 0.0f;
                float f12 = 0.0f;
                for (int i10 = 0; i10 < length; i10++) {
                    TU_LineInfo tU_LineInfo = tU_LineInfoArr[i10];
                    String str = tU_LineInfo.str;
                    tU_GlyphSequenceData.str = str;
                    tU_GlyphSequenceData.mrMode = inputInfo.mrMode;
                    tU_GlyphSequenceData.fKernPercent = inputInfo.fKernPercent;
                    if (1 != tU_LineInfo.styleList.length) {
                        throw new Exception("QTextRender.decideFontSize_AutoMultiLineAutoScale() only support one style");
                    }
                    if (str == null || str.length() == 0) {
                        qSizeFloat = new QSizeFloat();
                        qSizeFloat.h = getFontHeight(null, f10);
                    } else {
                        TU_LineInfo tU_LineInfo2 = tU_LineInfoArr[i10];
                        tU_GlyphSequenceData.gStyleList = tU_LineInfo2.styleList;
                        QRange[] qRangeArr = tU_LineInfo2.gChRangeList;
                        tU_GlyphSequenceData.gChRangelist = qRangeArr;
                        qSizeFloat = getGlyphsSequenceSize(tU_GlyphSequenceData, new QRange(0, qRangeArr.length), f10);
                    }
                    f12 += qSizeFloat.h;
                    float f13 = qSizeFloat.w;
                    if (f13 > f11) {
                        f11 = f13;
                    }
                }
                int i11 = inputInfo.processMode;
                if (1 == i11) {
                    QSize qSize = inputInfo.oriTextSize;
                    rInfo.baseFontSize = (f10 * getUpscaleFitInSize(f11, f12, qSize.mWidth, qSize.mHeight).h) / f12;
                    return;
                } else {
                    if (2 == i11) {
                        throw new Exception("QTextRender.decideFontSize_AutoMultiLineAutoScale() not support AUTO_MULTILINE_AND_NO_SCALE!");
                    }
                    if (3 != i11) {
                        throw new Exception("QTextRender.decideFontSize_AutoMultiLineAutoScale() invalid processMode!");
                    }
                    throw new Exception("QTextRender.decideFontSize_AutoMultiLineAutoScale() not support SINGLELINE_AND_AUTO_SCALE!");
                }
            }
        }
        throw new Exception("QTextRender.decideFontSize_AutoMultiLineAutoScale() rInfo=null or no cutlines");
    }

    public static QBitmap generateParagraphBmp(QParagraphMeasureInfo mInfo, QParagraphMeasureResult pmr) {
        TU_ParagraphRenderInfo tU_ParagraphRenderInfoMeasureParagraph;
        Bitmap bitmapRenderParagraph;
        QBitmap qBitmapCreateQBitmapBlank_noSkia;
        TU_ParagraphInputInfo tU_ParagraphInputInfoTransMeasureInfo2InputInfo = transMeasureInfo2InputInfo(mInfo);
        if (tU_ParagraphInputInfoTransMeasureInfo2InputInfo == null || (tU_ParagraphRenderInfoMeasureParagraph = measureParagraph(tU_ParagraphInputInfoTransMeasureInfo2InputInfo, pmr)) == null || (bitmapRenderParagraph = renderParagraph(tU_ParagraphRenderInfoMeasureParagraph)) == null || (qBitmapCreateQBitmapBlank_noSkia = QBitmapFactory.createQBitmapBlank_noSkia(bitmapRenderParagraph.getWidth(), bitmapRenderParagraph.getHeight(), QColorSpace.QPAF_RGB32_A8R8G8B8)) == null) {
            return null;
        }
        qBitmapCreateQBitmapBlank_noSkia.copyFromAndroidBitmap(bitmapRenderParagraph);
        if (!bitmapRenderParagraph.isRecycled()) {
            bitmapRenderParagraph.recycle();
        }
        return qBitmapCreateQBitmapBlank_noSkia;
    }

    private static float getFontHeight(String fontFile, float fontSize) {
        TextPaint textPaint = new TextPaint(129);
        Typeface typefaceGetTypeFace = QFontCache.GetTypeFace(fontFile);
        if (typefaceGetTypeFace != null) {
            textPaint.setTypeface(typefaceGetTypeFace);
        }
        textPaint.setTextSize(fontSize);
        Paint.FontMetrics fontMetrics = textPaint.getFontMetrics();
        return fontMetrics.descent - fontMetrics.ascent;
    }

    private static TU_GlyphSequencePickingData getGSPData_by_GlyphRange(TU_GlyphSequenceData oriGSD, QRange gRange) {
        int i10;
        int i11 = gRange.start;
        if (i11 >= 0 && (i10 = gRange.length) > 0 && i11 + i10 <= oriGSD.gChRangelist.length) {
            LinkedList linkedList = new LinkedList();
            LinkedList linkedList2 = new LinkedList();
            int length = oriGSD.gStyleList.length;
            QRange qRange = new QRange(gRange);
            for (int i12 = 0; i12 < length && qRange.length > 0; i12++) {
                QGlyphStyle qGlyphStyle = oriGSD.gStyleList[i12];
                int i13 = qGlyphStyle.gStartIdx;
                int i14 = qRange.start;
                if (i13 <= i14 && i14 <= (i13 + qGlyphStyle.gCount) - 1) {
                    linkedList2.add(Integer.valueOf(i12));
                    int i15 = (qGlyphStyle.gStartIdx + qGlyphStyle.gCount) - 1;
                    int i16 = qRange.start;
                    int i17 = (i15 - i16) + 1;
                    int i18 = qRange.length;
                    if (i17 < i18) {
                        i18 = (i15 - i16) + 1;
                    }
                    linkedList.add(new QRange(i16, i18));
                    qRange.start += i18;
                    qRange.length -= i18;
                }
            }
            if (!linkedList.isEmpty() && !linkedList2.isEmpty()) {
                TU_GlyphSequencePickingData tU_GlyphSequencePickingData = new TU_GlyphSequencePickingData();
                tU_GlyphSequencePickingData.gRangeList = new QRange[linkedList.size()];
                tU_GlyphSequencePickingData.sIdxArray = new Integer[linkedList2.size()];
                linkedList.toArray(tU_GlyphSequencePickingData.gRangeList);
                linkedList2.toArray(tU_GlyphSequencePickingData.sIdxArray);
                return tU_GlyphSequencePickingData;
            }
        }
        return null;
    }

    private static int getGlyphStyleIdx_From_InputInfo(TU_ParagraphInputInfo inputInfo, int gIdx) {
        if (inputInfo == null) {
            return -1;
        }
        boolean z10 = false;
        int i10 = 0;
        while (true) {
            QGlyphStyle[] qGlyphStyleArr = inputInfo.styleList;
            if (i10 >= qGlyphStyleArr.length) {
                i10 = -1;
                break;
            }
            QGlyphStyle qGlyphStyle = qGlyphStyleArr[i10];
            int i11 = qGlyphStyle.gStartIdx;
            int i12 = (qGlyphStyle.gCount + i11) - 1;
            if (i11 <= gIdx && gIdx <= i12) {
                z10 = true;
                break;
            }
            i10++;
        }
        if (z10) {
            return i10;
        }
        return -1;
    }

    private static QRange[] getGlyphUnicharRange2_mtbl(String txt) {
        if (txt == null || txt.length() == 0) {
            return null;
        }
        int i10 = 0;
        QRange[] qRangeArr = (QRange[]) newObjectArray(QRange.class, txt.codePointCount(0, txt.length()));
        int i11 = 0;
        int i12 = 0;
        while (true) {
            try {
                int iOffsetByCodePoints = txt.offsetByCodePoints(i10, 1);
                int i13 = iOffsetByCodePoints - i11;
                QRange qRange = qRangeArr[i12];
                qRange.start = i10;
                qRange.length = i13;
                i10 += i13;
                i12++;
                i11 = iOffsetByCodePoints;
            } catch (Exception unused) {
                return qRangeArr;
            }
        }
    }

    private static QRange[] getGlyphUnicharRange_mtbl(String txt) {
        if (txt == null || txt.length() == 0) {
            return null;
        }
        ArrayList arrayList = new ArrayList();
        int i10 = 0;
        while (i10 < txt.length()) {
            if (IsHighSurrogateChar(txt.charAt(i10))) {
                arrayList.add(new QRange(i10, 2));
                i10++;
            } else {
                arrayList.add(new QRange(i10, 1));
            }
            i10++;
        }
        QRange[] qRangeArr = new QRange[arrayList.size()];
        arrayList.toArray(qRangeArr);
        return qRangeArr;
    }

    private static QSizeFloat getGlyphsSequenceSize(TU_GlyphSequenceData gsd, QRange gRange, float baseFontSize) {
        return gsd.mrMode == 0 ? getGlyphsSequenceSize_LineMode(gsd, gRange, baseFontSize) : getGlyphsSequenceSize_GBGMode(gsd, gRange, baseFontSize);
    }

    private static QSizeFloat getGlyphsSequenceSize_GBGMode(TU_GlyphSequenceData gsd, QRange gRange, float baseFontSize) {
        if (1 != gsd.gStyleList.length) {
            return null;
        }
        QSizeFloat qSizeFloat = new QSizeFloat();
        TextPaint textPaintCreateTextPaint = createTextPaint(baseFontSize, gsd.gStyleList[0], false);
        QRange[] qRangeArr = gsd.gChRangelist;
        String str = gsd.str;
        float fontHeight = getFontHeight(textPaintCreateTextPaint);
        float fMeasureText = 0.0f;
        for (int i10 = gRange.start; i10 < gRange.start + gRange.length; i10++) {
            QRange qRange = qRangeArr[i10];
            int i11 = qRange.start;
            fMeasureText += textPaintCreateTextPaint.measureText(str.substring(i11, qRange.length + i11));
        }
        qSizeFloat.w = fMeasureText;
        qSizeFloat.h = fontHeight;
        return qSizeFloat;
    }

    private static QSizeFloat getGlyphsSequenceSize_LineMode(TU_GlyphSequenceData gsd, QRange gRange, float baseFontSize) {
        QSizeFloat qSizeFloat = new QSizeFloat();
        TU_GlyphSequencePickingData gSPData_by_GlyphRange = getGSPData_by_GlyphRange(gsd, gRange);
        if (gSPData_by_GlyphRange == null) {
            return null;
        }
        QRange[] qRangeArr = gSPData_by_GlyphRange.gRangeList;
        int length = qRangeArr.length;
        Integer[] numArr = gSPData_by_GlyphRange.sIdxArray;
        if (length != numArr.length) {
            return null;
        }
        int length2 = qRangeArr.length;
        QRange[] qRangeArr2 = gsd.gChRangelist;
        float fMeasureText = 0.0f;
        float f10 = 0.0f;
        for (int i10 = 0; i10 < length2; i10++) {
            TextPaint textPaintCreateTextPaint = createTextPaint(baseFontSize, gsd.gStyleList[numArr[i10].intValue()], false);
            float fontHeight = getFontHeight(textPaintCreateTextPaint);
            if (fontHeight > f10) {
                f10 = fontHeight;
            }
            QRange qRange = qRangeArr[i10];
            int i11 = qRange.start;
            int i12 = (qRange.length + i11) - 1;
            int i13 = qRangeArr2[i11].start;
            QRange qRange2 = qRangeArr2[i12];
            fMeasureText += textPaintCreateTextPaint.measureText(gsd.str.substring(i13, qRange2.start + qRange2.length));
        }
        qSizeFloat.w = fMeasureText;
        qSizeFloat.h = f10;
        return qSizeFloat;
    }

    private static float getInitBaseFontSize(TU_ParagraphInputInfo inputInfo, int type) {
        float fontHeight;
        if (inputInfo == null) {
            return 0.0f;
        }
        float f10 = INIT_FONT_SIZE;
        if (type == 1) {
            fontHeight = getFontHeight(inputInfo.highestFont, inputInfo.highestSizeFactor * f10);
        } else {
            if (type != 2) {
                return 0.0f;
            }
            fontHeight = getFontHeight(null, 1.0f * f10);
        }
        return (inputInfo.oriTextSize.mHeight * f10) / fontHeight;
    }

    public static int getStringGlyphCount(String txt) {
        if (txt == null || txt.length() == 0) {
            return 0;
        }
        return getGlyphUnicharRange_mtbl(txt).length;
    }

    private static int getTaggedGlyphCnt(QGlyphStyle[] sList) {
        if (sList == null || sList.length == 0) {
            return 0;
        }
        int i10 = 0;
        for (QGlyphStyle qGlyphStyle : sList) {
            i10 += qGlyphStyle.gCount;
        }
        return i10;
    }

    private static void getUICoordinatesRectF(float dW, float dH, float leftX, float bottomY, float wholeH, QRectFloat rectF) {
        rectF.left = leftX;
        rectF.top = bottomY - dH;
        rectF.right = leftX + dW;
        rectF.bottom = bottomY;
    }

    private static QSizeFloat getUpscaleFitInSize(float srcW, float srcH, float W2Fit, float H2Fit) {
        QSizeFloat qSizeFloat = new QSizeFloat(W2Fit, H2Fit);
        if (srcW > W2Fit || srcH > H2Fit) {
            float f10 = H2Fit * srcW;
            float f11 = W2Fit * srcH;
            if (f10 > f11) {
                qSizeFloat.h = f11 / srcW;
            } else {
                qSizeFloat.w = f10 / srcH;
            }
        } else {
            float f12 = H2Fit * srcW;
            float f13 = W2Fit * srcH;
            if (f12 > f13) {
                qSizeFloat.h = f13 / srcW;
            } else {
                qSizeFloat.w = f12 / srcH;
            }
        }
        return qSizeFloat;
    }

    private static boolean hasEffect(QGlyphStyle style) {
        boolean z10 = false;
        if (style == null) {
            return false;
        }
        if (style.DShadowBlurRadius > 0.0f && style.DFontSize > 0.0f) {
            z10 = true;
        }
        if (style.strokeWPercent > 0.0f) {
            return true;
        }
        return z10;
    }

    private static boolean hasEnterSymbol(String srcTxt) {
        if (srcTxt == null || srcTxt.length() == 0) {
            return false;
        }
        for (int i10 = 0; i10 < srcTxt.length(); i10++) {
            if (srcTxt.charAt(i10) == '\n') {
                return true;
            }
        }
        return false;
    }

    private static boolean isNewLineSymbol(TU_GlyphSequenceData gsd, int gIdx) {
        QRange qRange;
        int i10;
        if (gsd != null) {
            QRange[] qRangeArr = gsd.gChRangelist;
            if (gIdx >= qRangeArr.length || gIdx < 0 || (i10 = (qRange = qRangeArr[gIdx]).length) > 1) {
                return false;
            }
            if (i10 < 0) {
                StringBuilder sb2 = new StringBuilder();
                sb2.append("QTextRender.isNewLineSymbolEx() gChRange.length < 0, gIdx=");
                sb2.append(gIdx);
            }
            if (gsd.str.charAt(qRange.start) == '\n') {
                return true;
            }
        }
        return false;
    }

    private static TU_ParagraphRenderInfo measureP_AutoMultiLineAutoScale(TU_ParagraphInputInfo inputInfo, QParagraphMeasureResult pmr) {
        TU_ParagraphRenderInfo tU_ParagraphRenderInfoCutLine_AutoMultiLineAutoScale = cutLine_AutoMultiLineAutoScale(inputInfo);
        if (tU_ParagraphRenderInfoCutLine_AutoMultiLineAutoScale == null) {
            return null;
        }
        try {
            decideFontSize_AutoMultiLineAutoScale(inputInfo, tU_ParagraphRenderInfoCutLine_AutoMultiLineAutoScale);
            try {
                prepareOperateData(inputInfo, pmr, tU_ParagraphRenderInfoCutLine_AutoMultiLineAutoScale);
                return tU_ParagraphRenderInfoCutLine_AutoMultiLineAutoScale;
            } catch (Exception e10) {
                e10.printStackTrace();
                return null;
            }
        } catch (Exception e11) {
            e11.printStackTrace();
            return null;
        }
    }

    private static TU_ParagraphRenderInfo measureP_AutoMultiLineNoScale(TU_ParagraphInputInfo inputInfo, QParagraphMeasureResult pmr) {
        TU_ParagraphRenderInfo tU_ParagraphRenderInfoCutLine_AutoMultiLineNoScale = cutLine_AutoMultiLineNoScale(inputInfo);
        if (tU_ParagraphRenderInfoCutLine_AutoMultiLineNoScale == null) {
            return null;
        }
        try {
            prepareOperateData(inputInfo, pmr, tU_ParagraphRenderInfoCutLine_AutoMultiLineNoScale);
            return tU_ParagraphRenderInfoCutLine_AutoMultiLineNoScale;
        } catch (Exception e10) {
            e10.printStackTrace();
            return null;
        }
    }

    private static TU_ParagraphRenderInfo measureP_SingleLineAutoScale(TU_ParagraphInputInfo inputInfo, QParagraphMeasureResult pmr) {
        if (hasEnterSymbol(inputInfo.txt)) {
            return null;
        }
        float f10 = INIT_FONT_SIZE;
        TU_GlyphSequenceData tU_GlyphSequenceData = new TU_GlyphSequenceData();
        tU_GlyphSequenceData.gChRangelist = inputInfo.gChRangeList;
        tU_GlyphSequenceData.gStyleList = inputInfo.styleList;
        tU_GlyphSequenceData.str = inputInfo.txt;
        tU_GlyphSequenceData.mrMode = inputInfo.mrMode;
        tU_GlyphSequenceData.fKernPercent = inputInfo.fKernPercent;
        QSizeFloat glyphsSequenceSize = getGlyphsSequenceSize(tU_GlyphSequenceData, new QRange(0, inputInfo.gCnt), f10);
        float f11 = glyphsSequenceSize.w;
        float f12 = glyphsSequenceSize.h;
        QSize qSize = inputInfo.oriTextSize;
        float f13 = (getUpscaleFitInSize(f11, f12, qSize.mWidth, qSize.mHeight).h * INIT_FONT_SIZE) / glyphsSequenceSize.h;
        TU_ParagraphRenderInfo tU_ParagraphRenderInfo = new TU_ParagraphRenderInfo();
        TU_LineInfo[] tU_LineInfoArr = (TU_LineInfo[]) newObjectArray(TU_LineInfo.class, 1);
        TU_LineInfo tU_LineInfo = tU_LineInfoArr[0];
        tU_LineInfo.str = inputInfo.txt;
        tU_LineInfo.gChRangeList = inputInfo.gChRangeList;
        tU_LineInfo.styleList = inputInfo.styleList;
        tU_ParagraphRenderInfo.cutlines = tU_LineInfoArr;
        tU_ParagraphRenderInfo.baseFontSize = f13;
        try {
            prepareOperateData(inputInfo, pmr, tU_ParagraphRenderInfo);
            return tU_ParagraphRenderInfo;
        } catch (Exception e10) {
            e10.printStackTrace();
            return null;
        }
    }

    public static TU_ParagraphRenderInfo measureParagraph(TU_ParagraphInputInfo inputInfo, QParagraphMeasureResult pmr) {
        int i10 = inputInfo.processMode;
        if (i10 == 1) {
            return measureP_AutoMultiLineAutoScale(inputInfo, pmr);
        }
        if (i10 == 2) {
            return measureP_AutoMultiLineNoScale(inputInfo, pmr);
        }
        if (i10 != 3) {
            return null;
        }
        return measureP_SingleLineAutoScale(inputInfo, pmr);
    }

    private static <T> T[] newObjectArray(Class<T> cls, int i10) {
        T[] tArr = (T[]) ((Object[]) Array.newInstance((Class<?>) cls, i10));
        for (int i11 = 0; i11 < i10; i11++) {
            try {
                tArr[i11] = cls.newInstance();
            } catch (Exception e10) {
                e10.printStackTrace();
            }
        }
        return tArr;
    }

    private static void prepareLineTypographicInfo_L2R(TU_LineInfo lineInfo, float baseFontSize, int mrMode, float fKernPercent) throws Exception {
        if (lineInfo != null) {
            float f10 = 0.0f;
            if (baseFontSize > 0.0f) {
                int i10 = 1;
                if (1 == lineInfo.styleList.length) {
                    String str = lineInfo.str;
                    if (str == null || str.length() == 0) {
                        lineInfo.TInfo.height = getFontHeight(null, baseFontSize);
                        lineInfo.TInfo.width = 0.0f;
                        return;
                    }
                    int length = lineInfo.gChRangeList.length;
                    int length2 = lineInfo.styleList.length;
                    lineInfo.gTInfoList = (TU_TypographicInfo[]) newObjectArray(TU_TypographicInfo.class, length);
                    lineInfo.slTInfoList = (TU_TypographicInfo[]) newObjectArray(TU_TypographicInfo.class, length2);
                    TU_TypographicInfo tU_TypographicInfo = lineInfo.TInfo;
                    QRange[] qRangeArr = lineInfo.gChRangeList;
                    if (mrMode == 0) {
                        float f11 = 0.0f;
                        int i11 = 0;
                        while (i11 < length2) {
                            QGlyphStyle qGlyphStyle = lineInfo.styleList[i11];
                            TU_TypographicInfo tU_TypographicInfo2 = lineInfo.slTInfoList[i11];
                            int i12 = qGlyphStyle.gStartIdx;
                            int i13 = (qGlyphStyle.gCount + i12) - i10;
                            TextPaint textPaintCreateBasicTextPaint = createBasicTextPaint(qGlyphStyle.auxiliaryFont, qGlyphStyle.sizeFactor * baseFontSize);
                            QRange qRange = qRangeArr[i13];
                            int i14 = qRange.start + qRange.length;
                            Paint.FontMetrics fontMetrics = textPaintCreateBasicTextPaint.getFontMetrics();
                            float f12 = fontMetrics.ascent;
                            tU_TypographicInfo2.ascent = f12;
                            float f13 = fontMetrics.descent;
                            tU_TypographicInfo2.descent = f13;
                            tU_TypographicInfo2.baseline = f10;
                            tU_TypographicInfo2.height = f13 - f12;
                            float fMeasureText = textPaintCreateBasicTextPaint.measureText(lineInfo.str, qRangeArr[i12].start, i14);
                            tU_TypographicInfo2.width = fMeasureText;
                            float f14 = tU_TypographicInfo2.ascent;
                            if (f14 < f11) {
                                f11 = f14;
                            }
                            tU_TypographicInfo.width += fMeasureText;
                            float f15 = tU_TypographicInfo2.height;
                            float f16 = tU_TypographicInfo.height;
                            if (f15 <= f16) {
                                f15 = f16;
                            }
                            tU_TypographicInfo.height = f15;
                            float f17 = tU_TypographicInfo2.width;
                            TU_TypographicInfo[] tU_TypographicInfoArr = lineInfo.gTInfoList;
                            while (i13 >= i12) {
                                float fMeasureText2 = textPaintCreateBasicTextPaint.measureText(lineInfo.str, qRangeArr[i12].start, qRangeArr[i13].start);
                                TU_TypographicInfo tU_TypographicInfo3 = tU_TypographicInfoArr[i13];
                                tU_TypographicInfo3.width = f17 - fMeasureText2;
                                tU_TypographicInfo3.height = tU_TypographicInfo2.height;
                                i13--;
                                f17 = fMeasureText2;
                            }
                            i11++;
                            f10 = 0.0f;
                            i10 = 1;
                        }
                        for (int i15 = 0; i15 < length2; i15++) {
                            lineInfo.slTInfoList[i15].baseline4Draw = 0.0f - f11;
                        }
                        return;
                    }
                    TU_GlyphSequenceData tU_GlyphSequenceData = new TU_GlyphSequenceData();
                    tU_GlyphSequenceData.str = lineInfo.str;
                    tU_GlyphSequenceData.gStyleList = lineInfo.styleList;
                    tU_GlyphSequenceData.gChRangelist = lineInfo.gChRangeList;
                    tU_GlyphSequenceData.mrMode = mrMode;
                    tU_GlyphSequenceData.fKernPercent = fKernPercent;
                    for (int i16 = 0; i16 < length2; i16++) {
                        QGlyphStyle qGlyphStyle2 = lineInfo.styleList[i16];
                        TU_TypographicInfo tU_TypographicInfo4 = lineInfo.slTInfoList[i16];
                        int i17 = qGlyphStyle2.gStartIdx;
                        TextPaint textPaintCreateBasicTextPaint2 = createBasicTextPaint(qGlyphStyle2.auxiliaryFont, qGlyphStyle2.sizeFactor * baseFontSize);
                        Paint.FontMetrics fontMetrics2 = textPaintCreateBasicTextPaint2.getFontMetrics();
                        TU_TypographicInfo[] tU_TypographicInfoArr2 = lineInfo.gTInfoList;
                        int i18 = qGlyphStyle2.gStartIdx;
                        while (i18 < qGlyphStyle2.gStartIdx + qGlyphStyle2.gCount) {
                            QRange qRange2 = qRangeArr[i18];
                            int i19 = qRange2.start;
                            QGlyphStyle qGlyphStyle3 = qGlyphStyle2;
                            tU_TypographicInfoArr2[i18].width = textPaintCreateBasicTextPaint2.measureText(lineInfo.str, i19, qRange2.length + i19);
                            TU_TypographicInfo tU_TypographicInfo5 = tU_TypographicInfoArr2[i18];
                            float f18 = fontMetrics2.descent;
                            float f19 = fontMetrics2.ascent;
                            float f20 = f18 - f19;
                            tU_TypographicInfo5.height = f20;
                            TextPaint textPaint = textPaintCreateBasicTextPaint2;
                            float f21 = f20 * fKernPercent;
                            tU_TypographicInfo5.kern = f21;
                            tU_TypographicInfo5.ascent = f19;
                            tU_TypographicInfo5.descent = f18;
                            tU_TypographicInfo5.baseline = 0.0f;
                            tU_TypographicInfo5.baseline4Draw = 0.0f - f19;
                            tU_TypographicInfo4.width += tU_TypographicInfo5.width + f21;
                            float f22 = tU_TypographicInfo4.height;
                            if (f20 <= f22) {
                                f20 = f22;
                            }
                            tU_TypographicInfo4.height = f20;
                            i18++;
                            qGlyphStyle2 = qGlyphStyle3;
                            textPaintCreateBasicTextPaint2 = textPaint;
                        }
                        tU_TypographicInfo.width = tU_TypographicInfo4.width;
                        float f23 = tU_TypographicInfo4.height;
                        float f24 = tU_TypographicInfo.height;
                        if (f23 <= f24) {
                            f23 = f24;
                        }
                        tU_TypographicInfo.height = f23;
                    }
                    return;
                }
            }
        }
        throw new Exception("QTextRender.prepareLineTypographicInfo() invalid parameter");
    }

    private static void prepareLineTypographicInfo_R2L(TU_LineInfo lineInfo, float baseFontSize) throws Exception {
        throw new Exception("QTextRender.prepareLineTypographicInfo_R2L() is not implemented");
    }

    private static void prepareOperateData(TU_ParagraphInputInfo inputInfo, QParagraphMeasureResult pmr, TU_ParagraphRenderInfo rInfo) throws Exception {
        TU_LineInfo[] tU_LineInfoArr;
        float f10;
        TU_ParagraphInputInfo tU_ParagraphInputInfo = inputInfo;
        TU_ParagraphRenderInfo tU_ParagraphRenderInfo = rInfo;
        TU_LineInfo[] tU_LineInfoArr2 = tU_ParagraphRenderInfo.cutlines;
        if (tU_LineInfoArr2 == null || tU_LineInfoArr2.length == 0) {
            throw new Exception("QTextRender.prepareOperateData() no cutlines!");
        }
        float f11 = 0.0f;
        if (tU_ParagraphRenderInfo.baseFontSize <= 0.0f) {
            throw new Exception("QTextRender.prepareOperateData() baseFontSize is not valid!");
        }
        QSizeFloat qSizeFloat = tU_ParagraphRenderInfo.paragraphSize;
        int i10 = 0;
        int length = 0;
        while (true) {
            tU_LineInfoArr = tU_ParagraphRenderInfo.cutlines;
            if (i10 >= tU_LineInfoArr.length) {
                break;
            }
            TU_LineInfo tU_LineInfo = tU_LineInfoArr2[i10];
            QRange[] qRangeArr = tU_LineInfo.gChRangeList;
            length += qRangeArr != null ? qRangeArr.length : 0;
            prepareLineTypographicInfo_L2R(tU_LineInfo, tU_ParagraphRenderInfo.baseFontSize, tU_ParagraphInputInfo.mrMode, tU_ParagraphInputInfo.fKernPercent);
            TU_TypographicInfo tU_TypographicInfo = tU_LineInfoArr2[i10].TInfo;
            float f12 = tU_TypographicInfo.width;
            float f13 = qSizeFloat.w;
            if (f12 <= f13) {
                f12 = f13;
            }
            qSizeFloat.w = f12;
            qSizeFloat.h += tU_TypographicInfo.height;
            i10++;
        }
        pmr.lineInfoList = (QLineInfo[]) newObjectArray(QLineInfo.class, tU_LineInfoArr.length);
        pmr.gRectList = (QRectFloat[]) newObjectArray(QRectFloat.class, length);
        QSizeFloat qSizeFloat2 = pmr.pgSize;
        qSizeFloat2.w = qSizeFloat.w;
        qSizeFloat2.h = qSizeFloat.h;
        float f14 = 0.0f;
        float f15 = 0.0f;
        int i11 = 0;
        int i12 = 0;
        while (true) {
            TU_LineInfo[] tU_LineInfoArr3 = tU_ParagraphRenderInfo.cutlines;
            if (i11 >= tU_LineInfoArr3.length) {
                rInfo.processMode = tU_ParagraphInputInfo.processMode;
                int i13 = tU_ParagraphInputInfo.mrMode;
                rInfo.mrMode = i13;
                rInfo.fKernPercent = i13;
                rInfo.maxLines = tU_ParagraphInputInfo.maxLines;
                return;
            }
            TU_LineInfo tU_LineInfo2 = tU_LineInfoArr3[i11];
            QLineInfo qLineInfo = pmr.lineInfoList[i11];
            TU_TypographicInfo tU_TypographicInfo2 = tU_LineInfo2.TInfo;
            float f16 = tU_TypographicInfo2.height;
            float f17 = f14 + f16;
            int i14 = tU_ParagraphInputInfo.alignMode;
            float f18 = 1 == i14 ? f11 : 2 == i14 ? qSizeFloat.w - tU_TypographicInfo2.width : (qSizeFloat.w - tU_TypographicInfo2.width) / 2.0f;
            getUICoordinatesRectF(tU_TypographicInfo2.width, f16, f18, f17, qSizeFloat.h, qLineInfo.lineRect);
            String str = tU_LineInfo2.str;
            if (str == null || str.length() == 0) {
                f10 = tU_LineInfo2.TInfo.height;
            } else {
                qLineInfo.gCnt = tU_LineInfo2.gChRangeList.length;
                TU_TypographicInfo[] tU_TypographicInfoArr = tU_LineInfo2.gTInfoList;
                if (tU_ParagraphInputInfo.mrMode == 0) {
                    float f19 = f18;
                    int i15 = 0;
                    while (true) {
                        TU_TypographicInfo[] tU_TypographicInfoArr2 = tU_LineInfo2.slTInfoList;
                        if (i15 >= tU_TypographicInfoArr2.length) {
                            break;
                        }
                        TU_TypographicInfo tU_TypographicInfo3 = tU_TypographicInfoArr2[i15];
                        QGlyphStyle qGlyphStyle = tU_LineInfo2.styleList[i15];
                        float f20 = tU_TypographicInfo3.baseline4Draw + f15;
                        tU_TypographicInfo3.baseline4Draw = f20;
                        tU_TypographicInfo3.xOffset = f19;
                        int i16 = qGlyphStyle.gCount;
                        int i17 = qGlyphStyle.gStartIdx;
                        float f21 = f20 + tU_TypographicInfo3.descent;
                        int i18 = 0;
                        while (i18 < i16) {
                            int i19 = i17 + i18;
                            int i20 = i16;
                            TU_TypographicInfo tU_TypographicInfo4 = tU_TypographicInfoArr[i19];
                            getUICoordinatesRectF(tU_TypographicInfo4.width, tU_TypographicInfo4.height, f19, f21, qSizeFloat.h, pmr.gRectList[i12]);
                            f19 += tU_TypographicInfoArr[i19].width;
                            i12++;
                            i18++;
                            i16 = i20;
                            i17 = i17;
                        }
                        i15++;
                    }
                } else {
                    for (int i21 = 0; i21 < tU_LineInfo2.gTInfoList.length; i21++) {
                        TU_TypographicInfo tU_TypographicInfo5 = tU_TypographicInfoArr[i21];
                        float f22 = tU_TypographicInfo5.baseline4Draw + f15;
                        tU_TypographicInfo5.baseline4Draw = f22;
                        float f23 = f18 + (tU_TypographicInfo5.kern / 2.0f);
                        tU_TypographicInfo5.xOffset = f23;
                        getUICoordinatesRectF(tU_TypographicInfo5.width, tU_TypographicInfo5.height, f23, f22 + tU_TypographicInfo5.descent, qSizeFloat.h, pmr.gRectList[i12]);
                        TU_TypographicInfo tU_TypographicInfo6 = tU_TypographicInfoArr[i21];
                        f18 += tU_TypographicInfo6.width + tU_TypographicInfo6.kern;
                        i12++;
                    }
                }
                f10 = tU_LineInfo2.TInfo.height;
            }
            f14 += f10;
            f15 += f10;
            i11++;
            tU_ParagraphInputInfo = inputInfo;
            tU_ParagraphRenderInfo = rInfo;
            f11 = 0.0f;
        }
    }

    private static QGlyphStyle[] reconstructionStyleList_for_cutLine(TU_ParagraphInputInfo inputInfo, int[] gSIdxArray, QRange gRange) {
        if (gRange.length == 0) {
            return null;
        }
        LinkedList linkedList = new LinkedList();
        int i10 = gRange.start;
        QGlyphStyle[] qGlyphStyleArr = inputInfo.styleList;
        int i11 = 0;
        int i12 = -1;
        for (int i13 = i10; i13 < gRange.start + gRange.length; i13++) {
            int i14 = gSIdxArray[i13];
            if (i12 != i14) {
                QGlyphStyle qGlyphStyle = linkedList.size() > 0 ? (QGlyphStyle) linkedList.getLast() : null;
                if (qGlyphStyle != null) {
                    qGlyphStyle.gCount = i11;
                }
                QGlyphStyle qGlyphStyle2 = -1 == i14 ? new QGlyphStyle() : new QGlyphStyle(qGlyphStyleArr[i14]);
                qGlyphStyle2.gStartIdx = i13 - i10;
                qGlyphStyle2.gCount = 0;
                linkedList.add(qGlyphStyle2);
                i11 = 1;
                i12 = i14;
            } else {
                i11++;
            }
        }
        ((QGlyphStyle) linkedList.getLast()).gCount = i11;
        QGlyphStyle[] qGlyphStyleArr2 = new QGlyphStyle[linkedList.size()];
        linkedList.toArray(qGlyphStyleArr2);
        return qGlyphStyleArr2;
    }

    private static Bitmap renderParagraph(TU_ParagraphRenderInfo rInfo) {
        return rInfo.mrMode == 0 ? renderParagraph_LineMode(rInfo) : renderParagraph_GBGMode(rInfo);
    }

    public static QBitmap renderParagraphEx(TU_ParagraphRenderInfo rInfo) {
        QBitmap qBitmapCreateQBitmapBlank_noSkia;
        Bitmap bitmapRenderParagraph = renderParagraph(rInfo);
        if (bitmapRenderParagraph == null || (qBitmapCreateQBitmapBlank_noSkia = QBitmapFactory.createQBitmapBlank_noSkia(bitmapRenderParagraph.getWidth(), bitmapRenderParagraph.getHeight(), QColorSpace.QPAF_RGB32_A8R8G8B8)) == null) {
            return null;
        }
        qBitmapCreateQBitmapBlank_noSkia.copyFromAndroidBitmap(bitmapRenderParagraph);
        if (!bitmapRenderParagraph.isRecycled()) {
            bitmapRenderParagraph.recycle();
        }
        return qBitmapCreateQBitmapBlank_noSkia;
    }

    /* JADX WARN: Multi-variable type inference failed */
    /* JADX WARN: Type inference failed for: r22v0 */
    /* JADX WARN: Type inference failed for: r8v0 */
    /* JADX WARN: Type inference failed for: r8v1 */
    /* JADX WARN: Type inference failed for: r8v2 */
    /* JADX WARN: Type inference failed for: r8v5 */
    /* JADX WARN: Type inference failed for: r8v7 */
    /* JADX WARN: Type inference failed for: r8v8 */
    private static Bitmap renderParagraph_GBGMode(TU_ParagraphRenderInfo tU_ParagraphRenderInfo) {
        int length;
        int i10;
        QSizeFloat qSizeFloat = tU_ParagraphRenderInfo.paragraphSize;
        boolean z10 = true;
        Bitmap bitmapCreateBitmap = Bitmap.createBitmap(((int) qSizeFloat.w) + 1, ((int) qSizeFloat.h) + 1, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmapCreateBitmap);
        TU_LineInfo[] tU_LineInfoArr = tU_ParagraphRenderInfo.cutlines;
        float f10 = tU_ParagraphRenderInfo.baseFontSize;
        if (2 != tU_ParagraphRenderInfo.processMode || (length = tU_ParagraphRenderInfo.maxLines) >= tU_LineInfoArr.length) {
            length = tU_LineInfoArr.length;
        }
        boolean z11 = false;
        int i11 = 0;
        while (i11 < length) {
            TU_LineInfo tU_LineInfo = tU_LineInfoArr[i11];
            String str = tU_LineInfo.str;
            if (str != null && str.length() != 0) {
                QRange[] qRangeArr = tU_LineInfo.gChRangeList;
                TU_TypographicInfo[] tU_TypographicInfoArr = tU_LineInfo.gTInfoList;
                int r10 = 0;
                while (true) {
                    QGlyphStyle[] qGlyphStyleArr = tU_LineInfo.styleList;
                    if (r10 >= qGlyphStyleArr.length) {
                        break;
                    }
                    QGlyphStyle qGlyphStyle = qGlyphStyleArr[r10];
                    TextPaint textPaintCreateTextPaint = hasEffect(qGlyphStyle) ? createTextPaint(f10, qGlyphStyle, z10) : null;
                    TextPaint textPaintCreateTextPaint2 = createTextPaint(f10, qGlyphStyle, z11);
                    int i12 = qGlyphStyle.gStartIdx;
                    int r11 = r10;
                    while (i12 < qGlyphStyle.gStartIdx + qGlyphStyle.gCount) {
                        QRange qRange = qRangeArr[i12];
                        int i13 = qRange.start;
                        int i14 = qRangeArr[i11].start + qRange.length;
                        if (textPaintCreateTextPaint != null) {
                            String str2 = tU_LineInfo.str;
                            TU_TypographicInfo tU_TypographicInfo = tU_TypographicInfoArr[i12];
                            i10 = i13;
                            canvas.drawText(str2, i10, i14, tU_TypographicInfo.xOffset, tU_TypographicInfo.baseline4Draw, (Paint) textPaintCreateTextPaint);
                        } else {
                            i10 = i13;
                        }
                        String str3 = tU_LineInfo.str;
                        TU_TypographicInfo tU_TypographicInfo2 = tU_TypographicInfoArr[i12];
                        canvas.drawText(str3, i10, i14, tU_TypographicInfo2.xOffset, tU_TypographicInfo2.baseline4Draw, (Paint) textPaintCreateTextPaint2);
                        i12++;
                    }
                    z10 = true;
                    z11 = false;
                    r10 = r11 + 1;
                }
            }
            i11++;
            length = length;
            z10 = true;
            z11 = false;
        }
        return bitmapCreateBitmap;
    }

    /* JADX WARN: Type inference failed for: r3v3, types: [boolean, int] */
    /* JADX WARN: Type inference failed for: r3v6 */
    /* JADX WARN: Type inference failed for: r3v7 */
    private static Bitmap renderParagraph_LineMode(TU_ParagraphRenderInfo tU_ParagraphRenderInfo) {
        int length;
        QSizeFloat qSizeFloat = tU_ParagraphRenderInfo.paragraphSize;
        boolean z10 = true;
        Bitmap bitmapCreateBitmap = Bitmap.createBitmap(((int) qSizeFloat.w) + 1, ((int) qSizeFloat.h) + 1, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmapCreateBitmap);
        TU_LineInfo[] tU_LineInfoArr = tU_ParagraphRenderInfo.cutlines;
        float f10 = tU_ParagraphRenderInfo.baseFontSize;
        if (2 != tU_ParagraphRenderInfo.processMode || (length = tU_ParagraphRenderInfo.maxLines) >= tU_LineInfoArr.length) {
            length = tU_LineInfoArr.length;
        }
        int i10 = 0;
        while (i10 < length) {
            TU_LineInfo tU_LineInfo = tU_LineInfoArr[i10];
            String str = tU_LineInfo.str;
            if (str != null && str.length() != 0) {
                QRange[] qRangeArr = tU_LineInfo.gChRangeList;
                int i11 = 0;
                int r10 = 1;
                TU_TypographicInfo[] tU_TypographicInfoArr = tU_LineInfo.slTInfoList;
                while (i11 < tU_TypographicInfoArr.length) {
                    QGlyphStyle qGlyphStyle = tU_LineInfo.styleList[i11];
                    TU_TypographicInfo tU_TypographicInfo = tU_TypographicInfoArr[i11];
                    TextPaint textPaintCreateTextPaint = hasEffect(qGlyphStyle) ? createTextPaint(f10, qGlyphStyle, true) : null;
                    TextPaint textPaintCreateTextPaint2 = createTextPaint(f10, qGlyphStyle, false);
                    int i12 = qGlyphStyle.gStartIdx;
                    int i13 = qRangeArr[i12].start;
                    int i14 = qGlyphStyle.gCount;
                    int i15 = qRangeArr[(i12 + i14) - r10].start + qRangeArr[(i12 + i14) - r10].length;
                    if (textPaintCreateTextPaint != null) {
                        canvas.drawText(tU_LineInfo.str, i13, i15, tU_TypographicInfo.xOffset, tU_TypographicInfo.baseline4Draw, (Paint) textPaintCreateTextPaint);
                    }
                    canvas.drawText(tU_LineInfo.str, i13, i15, tU_TypographicInfo.xOffset, tU_TypographicInfo.baseline4Draw, (Paint) textPaintCreateTextPaint2);
                    i11++;
                }
            }
            i10++;
            z10 = true;
        }
        return bitmapCreateBitmap;
    }

    private static void saveAndroidBitmap(Bitmap bmp, String format, String fileName) {
        FileOutputStream fileOutputStream;
        String lowerCase = format.toLowerCase();
        if (lowerCase.compareTo("jpg") == 0 || lowerCase.compareTo("png") == 0) {
            File file = new File(fileName);
            if (file.exists()) {
                file.delete();
            }
            try {
                fileOutputStream = new FileOutputStream(file);
            } catch (FileNotFoundException e10) {
                e10.printStackTrace();
                fileOutputStream = null;
            }
            if (lowerCase.compareTo("jpg") == 0) {
                bmp.compress(Bitmap.CompressFormat.JPEG, 100, fileOutputStream);
            } else if (lowerCase.compareTo("png") == 0) {
                bmp.compress(Bitmap.CompressFormat.PNG, 100, fileOutputStream);
            }
            try {
                fileOutputStream.flush();
            } catch (IOException e11) {
                e11.printStackTrace();
            }
            try {
                fileOutputStream.close();
            } catch (IOException e12) {
                e12.printStackTrace();
            }
        }
    }

    private static TU_SplittedLinesData split2Lines(String str) {
        String strSubstring;
        if (str == null || str.length() == 0) {
            return null;
        }
        QRange[] glyphUnicharRange_mtbl = getGlyphUnicharRange_mtbl(str);
        int length = glyphUnicharRange_mtbl.length;
        LinkedList linkedList = new LinkedList();
        LinkedList linkedList2 = new LinkedList();
        int i10 = 0;
        int i11 = 0;
        for (int i12 = 0; i12 < length; i12++) {
            QRange qRange = glyphUnicharRange_mtbl[i12];
            if (1 == qRange.length && '\n' == str.charAt(qRange.start)) {
                QRange qRange2 = new QRange(i11, i10);
                if (i10 == 0) {
                    strSubstring = new String();
                } else {
                    int i13 = glyphUnicharRange_mtbl[i11].start;
                    QRange qRange3 = glyphUnicharRange_mtbl[(i11 + i10) - 1];
                    strSubstring = str.substring(i13, qRange3.start + qRange3.length);
                }
                linkedList.add(strSubstring);
                linkedList2.add(qRange2);
                i11 = i12 + 1;
                i10 = 0;
            } else {
                i10++;
            }
        }
        if (i10 > 0) {
            int i14 = glyphUnicharRange_mtbl[i11].start;
            QRange qRange4 = glyphUnicharRange_mtbl[(i11 + i10) - 1];
            linkedList.add(str.substring(i14, qRange4.start + qRange4.length));
            linkedList2.add(new QRange(i11, i10));
        }
        TU_SplittedLinesData tU_SplittedLinesData = new TU_SplittedLinesData();
        tU_SplittedLinesData.lines = new String[linkedList.size()];
        tU_SplittedLinesData.linesGRange = new QRange[linkedList2.size()];
        linkedList.toArray(tU_SplittedLinesData.lines);
        linkedList2.toArray(tU_SplittedLinesData.linesGRange);
        return tU_SplittedLinesData;
    }

    private static boolean toBeBlankLine(TU_GlyphSequenceData gsd, int gIdx) {
        if (gsd == null || gIdx >= gsd.gChRangelist.length || gIdx < 0 || !isNewLineSymbol(gsd, gIdx)) {
            return false;
        }
        return gsd.gChRangelist.length - 1 == gIdx || isNewLineSymbol(gsd, gIdx + 1);
    }

    public static TU_ParagraphInputInfo transMeasureInfo2InputInfo(QParagraphMeasureInfo mInfo) {
        String str;
        if (mInfo == null || (str = mInfo.txt) == null || str.length() == 0 || 1 != mInfo.styleList.length) {
            return null;
        }
        TU_ParagraphInputInfo tU_ParagraphInputInfo = new TU_ParagraphInputInfo();
        tU_ParagraphInputInfo.txt = mInfo.txt;
        tU_ParagraphInputInfo.processMode = mInfo.processMode;
        tU_ParagraphInputInfo.mrMode = mInfo.mrMode;
        tU_ParagraphInputInfo.fKernPercent = mInfo.fKernPercent / 100.0f;
        tU_ParagraphInputInfo.alignMode = mInfo.alignMode;
        tU_ParagraphInputInfo.oriTextSize.copy(mInfo.oriTextSize);
        tU_ParagraphInputInfo.maxLines = mInfo.maxLines;
        QSize qSize = tU_ParagraphInputInfo.oriTextSize;
        qSize.mHeight = (int) getFontHeight(mInfo.styleList[0].auxiliaryFont, qSize.mHeight);
        QRange[] glyphUnicharRange_mtbl = getGlyphUnicharRange_mtbl(tU_ParagraphInputInfo.txt);
        tU_ParagraphInputInfo.gChRangeList = glyphUnicharRange_mtbl;
        if (glyphUnicharRange_mtbl == null) {
            return null;
        }
        tU_ParagraphInputInfo.gCnt = glyphUnicharRange_mtbl.length;
        QGlyphStyle[] qGlyphStyleArr = mInfo.styleList;
        tU_ParagraphInputInfo.styleList = qGlyphStyleArr;
        int length = qGlyphStyleArr.length;
        float f10 = 0.0f;
        for (int i10 = 0; i10 < length; i10++) {
            QGlyphStyle qGlyphStyle = qGlyphStyleArr[i10];
            float f11 = qGlyphStyle.sizeFactor;
            if (f11 <= 0.0f) {
                return null;
            }
            float fontHeight = getFontHeight(qGlyphStyle.auxiliaryFont, INIT_FONT_SIZE * f11);
            if (fontHeight > f10) {
                QGlyphStyle qGlyphStyle2 = qGlyphStyleArr[i10];
                tU_ParagraphInputInfo.highestFont = qGlyphStyle2.auxiliaryFont;
                tU_ParagraphInputInfo.highestSizeFactor = qGlyphStyle2.sizeFactor;
                f10 = fontHeight;
            }
        }
        return tU_ParagraphInputInfo;
    }

    private static float getFontHeight(Paint p10) {
        if (p10 == null) {
            return 0.0f;
        }
        Paint.FontMetrics fontMetrics = p10.getFontMetrics();
        return fontMetrics.descent - fontMetrics.ascent;
    }
}
