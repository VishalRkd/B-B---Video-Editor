package xiaoying.utils;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.text.TextPaint;
import java.io.File;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.text.BreakIterator;
import java.util.ArrayList;
import xiaoying.utils.text.QFontCache;

/* JADX INFO: loaded from: classes19.dex */
public class QTextUtils {
    private static final float INIT_FONT_SIZE = 20.0f;
    private static final String LOG_TAG = "QTextUtils";
    private static final int ONE_LINE_RESERVED = 1;

    public static class QAutoMultiLineResult {
        public String resultStr = null;
        public int txtLines = 0;
        public int txtMaxW = 0;
    }

    public static class QTextPoint {

                public float x;

                public float y;

        private QTextPoint() {
            this.x = 0.0f;
            this.y = 0.0f;
        }
    }

    public static class QTextWordInfo {
        public int wordEndPos = 0;
        public int wordStartPos = 0;
    }

    public static class Size {

                public float h;

                public float w;

        private Size() {
            this.w = 0.0f;
            this.h = 0.0f;
        }
    }

    public static String ConvertToUTF8String(byte[] strByte) throws UnsupportedEncodingException {
        if (strByte == null) {
            return null;
        }
        String str = new String(strByte, Charset.forName("GB18030"));
        if (str.equals(new String(str.getBytes(Charset.forName("GB18030")), Charset.forName("GB18030")))) {
            return new String(str.getBytes(Charset.forName("UTF-8")), Charset.forName("UTF-8"));
        }
        String str2 = new String(strByte, Charset.forName("BIG5"));
        if (str2.equals(new String(str2.getBytes(Charset.forName("BIG5")), Charset.forName("BIG5")))) {
            return new String(str2.getBytes(Charset.forName("UTF-8")), Charset.forName("UTF-8"));
        }
        return null;
    }

    private static boolean DoesFileExist(String file) {
        return file != null && file.length() > 0 && new File(file).exists();
    }

    private static boolean IsHighSurrogateChar(String str) {
        char cCharAt;
        return str != null && str.length() > 0 && (cCharAt = str.charAt(0)) >= 55296 && cCharAt <= 56319;
    }

    private static void applyShadowByRealTextSize(TextPaint tp2, QTextDrawParam tdp, float realTxtSize) {
        if (tp2 == null || tdp == null || realTxtSize <= 0.0f || tdp.DTextSize <= 0.0f) {
            return;
        }
        tp2.setShadowLayer(tdp.DShadowBlurRadius * realTxtSize, tdp.DShadowXShift * realTxtSize, realTxtSize * tdp.DShadowYShift, tdp.shadowColor);
    }

    private static QAutoMultiLineResult autoMultiLine_AutoScale(TextPaint tp2, String uniformStr, QTextDrawParam tdp) {
        int length;
        QTextWordInfo[] qTextWordInfoArr;
        boolean z10;
        int i10;
        TextPaint textPaint = tp2;
        String str = uniformStr;
        String strSubstring = null;
        if (str == null || tdp == null || tdp.textRegionW <= 0 || tdp.textRegionH <= 0 || (length = uniformStr.length()) <= 0) {
            return null;
        }
        QAutoMultiLineResult qAutoMultiLineResult = new QAutoMultiLineResult();
        setTextSize(textPaint, 20.0f, tdp);
        Paint.FontMetrics fontMetrics = tp2.getFontMetrics();
        float f10 = (tdp.textRegionH * 20.0f) / (fontMetrics.descent - fontMetrics.ascent);
        int originalEnterCount = getOriginalEnterCount(uniformStr);
        int i11 = length * 2;
        StringBuilder sb2 = new StringBuilder(i11 + 1);
        ArrayList<QTextWordInfo> arrayListTextStringToWordList = textStringToWordList(uniformStr);
        if (arrayListTextStringToWordList == null || arrayListTextStringToWordList.size() <= 1) {
            qTextWordInfoArr = null;
            z10 = false;
        } else {
            qTextWordInfoArr = (QTextWordInfo[]) arrayListTextStringToWordList.toArray(new QTextWordInfo[arrayListTextStringToWordList.size()]);
            z10 = true;
        }
        int i12 = 1;
        int i13 = 0;
        int i14 = 0;
        int i15 = 0;
        float f11 = 0.0f;
        int i16 = -1;
        while (i14 < length) {
            setTextSize(textPaint, f10 / ((originalEnterCount + i15) + 2), tdp);
            if (z10) {
                if (i13 < qTextWordInfoArr.length) {
                    QTextWordInfo qTextWordInfo = qTextWordInfoArr[i13];
                    if (i14 == qTextWordInfo.wordStartPos) {
                        strSubstring = str.substring(i14, qTextWordInfo.wordEndPos);
                        i14 = qTextWordInfoArr[i13].wordEndPos - 1;
                    }
                }
                strSubstring = strSubstring;
            } else {
                int i17 = i14 + 1;
                String strSubstring2 = str.substring(i14, i17);
                if (IsHighSurrogateChar(strSubstring2)) {
                    strSubstring2 = str.substring(i14, i14 + 2);
                    i14 = i17;
                }
                strSubstring = strSubstring2;
            }
            float fMeasureText = textPaint.measureText(strSubstring);
            if ('\n' == strSubstring.charAt(0)) {
                sb2.append('\n');
                i12++;
                i10 = 1;
                f11 = 0.0f;
            } else {
                f11 += fMeasureText;
                if (f11 > tdp.textRegionW) {
                    int i18 = i16 + 1;
                    int i19 = i15 + 1;
                    if (i18 >= i19 || i13 == 0) {
                        sb2.delete(0, i11);
                        i15 = i19;
                        i10 = 1;
                        i13 = -1;
                        i14 = -1;
                        i12 = 1;
                        f11 = 0.0f;
                        i16 = -1;
                    } else {
                        sb2.append('\n');
                        sb2.append(strSubstring);
                        i12++;
                        i16 = i18;
                        f11 = fMeasureText;
                    }
                } else {
                    sb2.append(strSubstring);
                }
                i10 = 1;
            }
            i14 += i10;
            i13 += i10;
            textPaint = tp2;
            str = uniformStr;
        }
        qAutoMultiLineResult.resultStr = sb2.toString();
        qAutoMultiLineResult.txtLines = i12;
        return qAutoMultiLineResult;
    }

    private static QAutoMultiLineResult autoMultiLine_NoScale(String uniformStr, QTextDrawParam tdp) {
        QAutoMultiLineResult qAutoMultiLineResult = null;
        if (uniformStr != null && tdp != null && tdp.textRegionW > 0 && tdp.textRegionH > 0) {
            int length = uniformStr.length();
            if (length <= 0) {
                return null;
            }
            qAutoMultiLineResult = new QAutoMultiLineResult();
            TextPaint textPaintCreateBasicTextPaint = createBasicTextPaint(tdp);
            setTextSize(textPaintCreateBasicTextPaint, 20.0f, tdp);
            Paint.FontMetrics fontMetrics = textPaintCreateBasicTextPaint.getFontMetrics();
            setTextSize(textPaintCreateBasicTextPaint, (tdp.textRegionH * 20.0f) / (fontMetrics.descent - fontMetrics.ascent), tdp);
            StringBuilder sb2 = new StringBuilder((length * 2) + 1);
            int i10 = 0;
            int i11 = 0;
            int i12 = 1;
            float f10 = 0.0f;
            float f11 = 0.0f;
            while (i11 < length) {
                int i13 = i11 + 1;
                String strSubstring = uniformStr.substring(i11, i13);
                if (IsHighSurrogateChar(strSubstring)) {
                    strSubstring = uniformStr.substring(i11, i11 + 2);
                    i11 = i13;
                }
                float fMeasureText = textPaintCreateBasicTextPaint.measureText(strSubstring);
                if ('\n' == strSubstring.charAt(i10)) {
                    sb2.append('\n');
                    i12++;
                    f11 = 0.0f;
                } else {
                    f11 += fMeasureText;
                    if (f11 > tdp.textRegionW) {
                        sb2.append('\n');
                        i12++;
                        sb2.append(strSubstring);
                        f11 = fMeasureText;
                    } else {
                        sb2.append(strSubstring);
                        if (f11 > f10) {
                            f10 = f11;
                        }
                    }
                }
                i11++;
                i10 = 0;
            }
            qAutoMultiLineResult.resultStr = sb2.toString();
            qAutoMultiLineResult.txtLines = i12;
            qAutoMultiLineResult.txtMaxW = Math.round(f10);
        }
        return qAutoMultiLineResult;
    }

    private static PointPair convertAngelToPositions(int w10, int h10, int angle) {
        PointPair pointPair = new PointPair();
        int i10 = w10 / 2;
        int i11 = h10 / 2;
        int i12 = (angle + 90) % 360;
        double d10 = ((double) w10) / ((double) h10);
        double dTan = Math.tan((((double) i12) / 180.0d) * 3.141592654d);
        if (Math.abs(dTan) <= d10) {
            if (i12 < 90 || i12 > 270) {
                int i13 = (int) ((-dTan) * ((double) i11));
                pointPair.x0 = i13;
                pointPair.y0 = i11;
                pointPair.x1 = -i13;
                pointPair.y1 = -i11;
            } else {
                int i14 = (int) (dTan * ((double) i11));
                pointPair.x0 = i14;
                pointPair.y0 = -i11;
                pointPair.x1 = -i14;
                pointPair.y1 = i11;
            }
        } else if (Math.abs(dTan) > d10) {
            if (Math.abs(dTan) > 100.0d) {
                if (i12 < 180) {
                    pointPair.x0 = -i10;
                    pointPair.y0 = 0;
                    pointPair.x1 = i10;
                    pointPair.y1 = 0;
                } else {
                    pointPair.x0 = i10;
                    pointPair.y0 = 0;
                    pointPair.x1 = -i10;
                    pointPair.y1 = 0;
                }
            } else if (i12 < 180) {
                int i15 = (int) (((double) i10) / dTan);
                pointPair.y0 = i15;
                pointPair.y1 = -i15;
                pointPair.x0 = -i10;
                pointPair.x1 = i10;
            } else {
                int i16 = -i10;
                int i17 = (int) (((double) i16) / dTan);
                pointPair.y0 = i17;
                pointPair.y1 = -i17;
                pointPair.x0 = i10;
                pointPair.x1 = i16;
            }
        }
        int i18 = -pointPair.y0;
        int i19 = -pointPair.y1;
        pointPair.x0 += i10;
        pointPair.y0 = i18 + i11;
        pointPair.x1 += i10;
        pointPair.y1 = i19 + i11;
        return pointPair;
    }

    private static TextPaint createBasicTextPaint(QTextDrawParam tdp) {
        int i10;
        if (tdp == null) {
            return null;
        }
        TextPaint textPaint = new TextPaint(129);
        boolean z10 = tdp.isBold;
        if (z10 && tdp.isItalic) {
            i10 = 3;
        } else if (tdp.isItalic) {
            i10 = 2;
        } else {
            i10 = z10 ? 1 : 0;
        }
        Typeface typefaceGetTypeFace = QFontCache.GetTypeFace(tdp.auxiliaryFont, i10);
        if (typefaceGetTypeFace != null) {
            setTF(textPaint, typefaceGetTypeFace, tdp, false);
        } else {
            Typeface typefaceGetTypeFace2 = QFontCache.GetTypeFace(tdp.auxiliaryFont, 0);
            if (typefaceGetTypeFace2 != null) {
                setTF(textPaint, typefaceGetTypeFace2, tdp, true);
            }
        }
        return textPaint;
    }

    private static ArrayList<TextPaint> createTextPaints(TextPaint txtPaint, QTextDrawParam tdp, float textSize) {
        if (tdp == null || txtPaint == null) {
            return null;
        }
        StringBuilder sb2 = new StringBuilder();
        sb2.append(tdp.txtFillType);
        sb2.append("...");
        sb2.append(tdp.txtFillConfig.color0);
        StringBuilder sb3 = new StringBuilder();
        sb3.append(tdp.txtStrokeType);
        sb3.append("...");
        sb3.append(tdp.txtStrokeConfig.color0);
        ArrayList<TextPaint> arrayList = new ArrayList<>();
        if (tdp.txtStrokeType != 0 && tdp.txtStrokeConfig.widthPercent > 0.0f) {
            TextPaint textPaintCreateBasicTextPaint = createBasicTextPaint(tdp);
            setTextSize(textPaintCreateBasicTextPaint, textSize, tdp);
            setShadow(textPaintCreateBasicTextPaint, textSize, tdp);
            textPaintCreateBasicTextPaint.setStyle(Paint.Style.STROKE);
            textPaintCreateBasicTextPaint.setStrokeCap(Paint.Cap.ROUND);
            textPaintCreateBasicTextPaint.setStrokeJoin(Paint.Join.ROUND);
            Paint.FontMetrics fontMetrics = textPaintCreateBasicTextPaint.getFontMetrics();
            textPaintCreateBasicTextPaint.setStrokeWidth((fontMetrics.descent - fontMetrics.ascent) * tdp.txtStrokeConfig.widthPercent);
            textPaintCreateBasicTextPaint.setColor(0xFF000000);
            arrayList.add(textPaintCreateBasicTextPaint);
        }
        if (tdp.txtFillType != 0) {
            TextPaint textPaintCreateBasicTextPaint2 = createBasicTextPaint(tdp);
            setTextSize(textPaintCreateBasicTextPaint2, textSize, tdp);
            setShadow(textPaintCreateBasicTextPaint2, textSize, tdp);
            textPaintCreateBasicTextPaint2.setColor(0xFF000000);
            arrayList.add(textPaintCreateBasicTextPaint2);
        }
        if (tdp.txtStrokeType != 0 && tdp.txtStrokeConfig.widthPercent > 0.0f) {
            TextPaint textPaintCreateBasicTextPaint3 = createBasicTextPaint(tdp);
            setTextSize(textPaintCreateBasicTextPaint3, textSize, tdp);
            Paint.FontMetrics fontMetrics2 = textPaintCreateBasicTextPaint3.getFontMetrics();
            float f10 = (fontMetrics2.descent - fontMetrics2.ascent) * tdp.txtStrokeConfig.widthPercent;
            textPaintCreateBasicTextPaint3.setStyle(Paint.Style.STROKE);
            textPaintCreateBasicTextPaint3.setStrokeCap(Paint.Cap.ROUND);
            textPaintCreateBasicTextPaint3.setStrokeJoin(Paint.Join.ROUND);
            textPaintCreateBasicTextPaint3.setStrokeWidth(f10);
            int i10 = tdp.txtStrokeType;
            if (i10 == 1) {
                textPaintCreateBasicTextPaint3.setColor(tdp.txtStrokeConfig.color0);
                arrayList.add(textPaintCreateBasicTextPaint3);
            } else if (i10 == 2) {
                PointPair pointPairConvertAngelToPositions = convertAngelToPositions(tdp.textRegionW, tdp.textRegionH, tdp.txtStrokeConfig.angle);
                float f11 = pointPairConvertAngelToPositions.x0;
                float f12 = pointPairConvertAngelToPositions.y0;
                float f13 = pointPairConvertAngelToPositions.x1;
                float f14 = pointPairConvertAngelToPositions.y1;
                QTextDrawParam.TxtStrokeConfig txtStrokeConfig = tdp.txtStrokeConfig;
                textPaintCreateBasicTextPaint3.setShader(new LinearGradient(f11, f12, f13, f14, txtStrokeConfig.color0, txtStrokeConfig.color1, Shader.TileMode.CLAMP));
                arrayList.add(textPaintCreateBasicTextPaint3);
            }
        }
        int i11 = tdp.txtFillType;
        if (i11 == 1) {
            TextPaint textPaintCreateBasicTextPaint4 = createBasicTextPaint(tdp);
            textPaintCreateBasicTextPaint4.setColor(tdp.txtFillConfig.color0);
            setTextSize(textPaintCreateBasicTextPaint4, textSize, tdp);
            arrayList.add(textPaintCreateBasicTextPaint4);
        } else if (i11 == 2) {
            TextPaint textPaintCreateBasicTextPaint5 = createBasicTextPaint(tdp);
            PointPair pointPairConvertAngelToPositions2 = convertAngelToPositions(tdp.textRegionW, tdp.textRegionH, tdp.txtFillConfig.angle);
            float f15 = pointPairConvertAngelToPositions2.x0;
            float f16 = pointPairConvertAngelToPositions2.y0;
            float f17 = pointPairConvertAngelToPositions2.x1;
            float f18 = pointPairConvertAngelToPositions2.y1;
            QTextDrawParam.TxtFillConfig txtFillConfig = tdp.txtFillConfig;
            textPaintCreateBasicTextPaint5.setShader(new LinearGradient(f15, f16, f17, f18, txtFillConfig.color0, txtFillConfig.color1, Shader.TileMode.CLAMP));
            setTextSize(textPaintCreateBasicTextPaint5, textSize, tdp);
            arrayList.add(textPaintCreateBasicTextPaint5);
        }
        return arrayList;
    }

    private static float decideTextSize(TextPaint tp2, QAutoMultiLineResult amlRes, QTextDrawParam tdp) {
        int i10;
        setTextSize(tp2, 20.0f, tdp);
        Paint.FontMetrics fontMetrics = tp2.getFontMetrics();
        float f10 = (tdp.textRegionH * 20.0f) / ((fontMetrics.descent - fontMetrics.ascent) * amlRes.txtLines);
        setTextSize(tp2, f10, tdp);
        float f11 = 0.0f;
        int i11 = 0;
        while (true) {
            i10 = amlRes.txtLines;
            if (i11 >= i10) {
                break;
            }
            String oneLineCharsFromUniformString = getOneLineCharsFromUniformString(amlRes.resultStr, i11);
            if (oneLineCharsFromUniformString != null) {
                float fMeasureText = tp2.measureText(oneLineCharsFromUniformString);
                if (fMeasureText > f11) {
                    f11 = fMeasureText;
                }
            }
            i11++;
        }
        return f11 <= ((float) tdp.textRegionW) ? f10 : getTextSizeByLongestLine(tp2, amlRes.resultStr, i10, tdp);
    }

    public static int drawText(QBitmap bmp, String srcTxt, QTextDrawParam tdp) {
        if (bmp == null) {
            return 2;
        }
        Bitmap bitmapCreateBitmapFromQBitmap = QAndroidBitmapFactory.createBitmapFromQBitmap(bmp, false);
        if (bitmapCreateBitmapFromQBitmap == null) {
            return 4;
        }
        drawText(bitmapCreateBitmapFromQBitmap, srcTxt, tdp);
        int iCopyFromAndroidBitmap = bmp.copyFromAndroidBitmap(bitmapCreateBitmapFromQBitmap);
        if (!bitmapCreateBitmapFromQBitmap.isRecycled()) {
            bitmapCreateBitmapFromQBitmap.recycle();
        }
        return iCopyFromAndroidBitmap;
    }

    public static int drawText_rotate_bg(long nativeSrcBGBmp, String srcTxt, QTextDrawParam tdp, long nativeOutBmp) {
        QBitmap qBitmapConstructQBitmapFromNativeHandle_noSkia = QBitmapFactory.constructQBitmapFromNativeHandle_noSkia(nativeSrcBGBmp);
        QBitmap qBitmapConstructQBitmapFromNativeHandle_noSkia2 = QBitmapFactory.constructQBitmapFromNativeHandle_noSkia(nativeOutBmp);
        if (qBitmapConstructQBitmapFromNativeHandle_noSkia == null || qBitmapConstructQBitmapFromNativeHandle_noSkia2 == null) {
            return 1;
        }
        Bitmap bitmapCreateBitmapFromQBitmap = QAndroidBitmapFactory.createBitmapFromQBitmap(qBitmapConstructQBitmapFromNativeHandle_noSkia, false);
        Bitmap bitmapCreateBitmap = Bitmap.createBitmap(qBitmapConstructQBitmapFromNativeHandle_noSkia2.getWidth(), qBitmapConstructQBitmapFromNativeHandle_noSkia2.getHeight(), Bitmap.Config.ARGB_8888);
        if (bitmapCreateBitmapFromQBitmap == null || bitmapCreateBitmap == null) {
            return 4;
        }
        drawText_rotate_bg(bitmapCreateBitmapFromQBitmap, srcTxt, tdp, bitmapCreateBitmap);
        int iCopyFromAndroidBitmap = qBitmapConstructQBitmapFromNativeHandle_noSkia2.copyFromAndroidBitmap(bitmapCreateBitmap);
        if (!bitmapCreateBitmapFromQBitmap.isRecycled()) {
            bitmapCreateBitmapFromQBitmap.recycle();
        }
        if (!bitmapCreateBitmap.isRecycled()) {
            bitmapCreateBitmap.recycle();
        }
        return iCopyFromAndroidBitmap;
    }

    public static int getAutoMultiLines_AutoScale(String srcTxt, QTextDrawParam tdp) {
        QAutoMultiLineResult qAutoMultiLineResultAutoMultiLine_AutoScale;
        if (srcTxt == null || tdp == null || (qAutoMultiLineResultAutoMultiLine_AutoScale = autoMultiLine_AutoScale(createBasicTextPaint(tdp), preProcessString(srcTxt), tdp)) == null) {
            return 0;
        }
        return qAutoMultiLineResultAutoMultiLine_AutoScale.txtLines;
    }

    public static QAutoMultiLineResult getAutoMultiLines_NoScale(String srcTxt, QTextDrawParam tdp) {
        if (srcTxt == null || tdp == null) {
            return null;
        }
        return autoMultiLine_NoScale(preProcessString(srcTxt), tdp);
    }

    private static String getOneLineCharsFromUniformString(String src, int lineIdxToFind) {
        String strSubstring = null;
        if (src == null || src.length() <= 0 || lineIdxToFind < 0) {
            return null;
        }
        int length = src.length();
        int i10 = 0;
        int i11 = -1;
        while (true) {
            int iIndexOf = src.indexOf(10, i10);
            if (-1 == iIndexOf) {
                if (i11 + 1 == lineIdxToFind) {
                    strSubstring = src.substring(i10, length);
                    break;
                }
                return strSubstring;
            }
            try {
                i11++;
                if (i11 == lineIdxToFind) {
                    strSubstring = src.substring(i10, iIndexOf);
                    break;
                }
                i10 = iIndexOf + 1;
            } catch (Exception unused) {
            }
        }
        return strSubstring;
    }

    private static int getOriginalEnterCount(String srcTxt) {
        if (srcTxt == null) {
            return 0;
        }
        int length = srcTxt.length();
        int i10 = 0;
        for (int i11 = 0; i11 < length; i11++) {
            if ('\n' == srcTxt.charAt(i11)) {
                i10++;
            }
        }
        return i10;
    }

    private static QTextPoint getRotatedCoordinate(QTextPoint RCenter, QTextPoint NRPoint, float RA) {
        QTextPoint qTextPoint = new QTextPoint();
        double d10 = (float) ((((double) RA) * 3.141592653589793d) / 180.0d);
        qTextPoint.x = (float) ((((double) (NRPoint.x - RCenter.x)) * Math.cos(d10)) + (((double) (RCenter.y - NRPoint.y)) * Math.sin(d10)) + ((double) RCenter.x));
        qTextPoint.y = (float) (((((double) (NRPoint.x - RCenter.x)) * Math.sin(d10)) - (((double) (RCenter.y - NRPoint.y)) * Math.cos(d10))) + ((double) RCenter.y));
        return qTextPoint;
    }

    private static QTextPoint getRotatedCoordinateEx(QTextPoint NRPoint, Size contentSize, Size finalSize, float RA) {
        QTextPoint qTextPoint = new QTextPoint();
        QTextPoint qTextPoint2 = new QTextPoint();
        float f10 = contentSize.w / 2.0f;
        qTextPoint2.x = f10;
        qTextPoint2.y = contentSize.h / 2.0f;
        double d10 = (float) ((((double) RA) * 3.141592653589793d) / 180.0d);
        qTextPoint.x = (float) ((((double) (NRPoint.x - f10)) * Math.cos(d10)) + (((double) (qTextPoint2.y - NRPoint.y)) * Math.sin(d10)) + ((double) (finalSize.w / 2.0f)));
        qTextPoint.y = (float) (((((double) (NRPoint.x - qTextPoint2.x)) * Math.sin(d10)) - (((double) (qTextPoint2.y - NRPoint.y)) * Math.cos(d10))) + ((double) (finalSize.h / 2.0f)));
        return qTextPoint;
    }

    public static QSize getSingleLineSize_AutoScale(String srcTxt, QTextDrawParam tdp) {
        if (hasEnterSymbol(srcTxt)) {
            return null;
        }
        return singleLineSize_AutoScale(createBasicTextPaint(tdp), srcTxt, tdp);
    }

    private static float getTextSizeByLongestLine(TextPaint tp2, String uniformStr, int totalLine, QTextDrawParam tdp) {
        setTextSize(tp2, 20.0f, tdp);
        String str = null;
        float f10 = 0.0f;
        for (int i10 = 0; i10 < totalLine; i10++) {
            String oneLineCharsFromUniformString = getOneLineCharsFromUniformString(uniformStr, i10);
            if (oneLineCharsFromUniformString != null) {
                float fMeasureText = tp2.measureText(oneLineCharsFromUniformString);
                if (fMeasureText > f10) {
                    str = oneLineCharsFromUniformString;
                    f10 = fMeasureText;
                }
            }
        }
        if (str != null) {
            return (tdp.textRegionW * 20.0f) / f10;
        }
        return 0.0f;
    }

    private static boolean hasEffect(QTextDrawParam tdp) {
        boolean z10 = false;
        if (tdp == null) {
            return false;
        }
        if (tdp.DShadowBlurRadius > 0.0f && tdp.DTextSize > 0.0f) {
            z10 = true;
        }
        if (tdp.txtStrokeConfig.widthPercent > 0.001d) {
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

    private static String preProcessString(String srcTxt) {
        return srcTxt;
    }

    private static void printSubPerformanceLog(long lastTS, String itemName) {
        long jCurrentTimeMillis = System.currentTimeMillis() - lastTS;
        StringBuilder sb2 = new StringBuilder();
        sb2.append(itemName);
        sb2.append(" Draw Cost=");
        sb2.append(jCurrentTimeMillis);
        sb2.append("ms");
    }

    private static void setShadow(Paint p10, float textSize, QTextDrawParam tdp) {
        float f10 = tdp.DShadowBlurRadius;
        if (f10 <= 0.0f || tdp.DTextSize <= 0.0f) {
            return;
        }
        float f11 = f10 * textSize;
        float f12 = tdp.DShadowXShift * textSize;
        float f13 = textSize * tdp.DShadowYShift;
        double d10 = (float) ((((double) tdp.angle) * 3.141592653589793d) / 180.0d);
        double d11 = f12;
        double d12 = f13;
        p10.setShadowLayer(f11, (float) ((Math.cos(d10) * d11) - (Math.sin(d10) * d12)), -((float) ((d11 * Math.sin(d10)) + (d12 * Math.cos(d10)))), tdp.shadowColor);
    }

    private static void setTF(TextPaint tp2, Typeface tf2, QTextDrawParam tdp, boolean bFailCreated) {
        if (tp2 == null || tf2 == null) {
            return;
        }
        if (bFailCreated) {
            if (tdp.isBold) {
                tp2.setFlags(32);
            }
            if (tdp.isItalic) {
                tp2.setTextSkewX(-0.25f);
            }
        }
        tp2.setTypeface(tf2);
    }

    private static void setTextSize(Paint p10, float textSize, QTextDrawParam tdp) {
        if (p10 == null || tdp == null) {
            return;
        }
        if (textSize <= 0.0f) {
            textSize = 20.0f;
        }
        p10.setTextSize(textSize);
    }

    private static void setTextSizeWithEffect(Paint p10, float textSize, QTextDrawParam tdp) {
        if (p10 == null || tdp == null) {
            return;
        }
        if (textSize <= 0.0f) {
            textSize = 20.0f;
        }
        p10.setTextSize(textSize);
        float f10 = tdp.DShadowBlurRadius;
        if (f10 > 0.0f && tdp.DTextSize > 0.0f) {
            float f11 = f10 * textSize;
            float f12 = tdp.DShadowXShift * textSize;
            float f13 = textSize * tdp.DShadowYShift;
            double d10 = (float) ((((double) tdp.angle) * 3.141592653589793d) / 180.0d);
            double d11 = f12;
            double d12 = f13;
            p10.setShadowLayer(f11, (float) ((Math.cos(d10) * d11) - (Math.sin(d10) * d12)), -((float) ((d11 * Math.sin(d10)) + (d12 * Math.cos(d10)))), tdp.shadowColor);
        }
        if (tdp.txtStrokeType != 1 || tdp.txtStrokeConfig.widthPercent <= 0.0f) {
            return;
        }
        Paint.FontMetrics fontMetrics = p10.getFontMetrics();
        float f14 = (fontMetrics.descent - fontMetrics.ascent) * tdp.txtStrokeConfig.widthPercent;
        p10.setStyle(Paint.Style.STROKE);
        p10.setStrokeCap(Paint.Cap.ROUND);
        p10.setStrokeJoin(Paint.Join.ROUND);
        p10.setStrokeWidth(f14);
        p10.setColor(tdp.txtStrokeConfig.color0);
    }

    private static QSize singleLineSize_AutoScale(TextPaint tp2, String srcTxt, QTextDrawParam tdp) {
        if (srcTxt == null || srcTxt.length() == 0) {
            return null;
        }
        QSize qSize = new QSize();
        QAutoMultiLineResult qAutoMultiLineResult = new QAutoMultiLineResult();
        qAutoMultiLineResult.resultStr = srcTxt;
        qAutoMultiLineResult.txtLines = 1;
        setTextSize(tp2, decideTextSize(tp2, qAutoMultiLineResult, tdp), tdp);
        Paint.FontMetrics fontMetrics = tp2.getFontMetrics();
        qSize.mWidth = Math.round(tp2.measureText(srcTxt));
        qSize.mHeight = Math.round(fontMetrics.descent - fontMetrics.ascent);
        return qSize;
    }

    private static ArrayList<QTextWordInfo> textStringToWordList(String inputText) {
        ArrayList<QTextWordInfo> arrayList = new ArrayList<>();
        QTextWordInfo qTextWordInfo = new QTextWordInfo();
        BreakIterator wordInstance = BreakIterator.getWordInstance();
        if (wordInstance == null) {
            return null;
        }
        wordInstance.setText(inputText);
        int iFirst = wordInstance.first();
        QTextWordInfo qTextWordInfo2 = qTextWordInfo;
        for (int next = wordInstance.next(); next != -1; next = wordInstance.next()) {
            qTextWordInfo2.wordStartPos = iFirst;
            qTextWordInfo2.wordEndPos = next;
            arrayList.add(qTextWordInfo2);
            qTextWordInfo2 = new QTextWordInfo();
            iFirst = next;
        }
        return arrayList;
    }

    public static int drawText(long nativeQBmpHandle, String srcTxt, QTextDrawParam tdp) {
        if (0 == nativeQBmpHandle) {
            return 2;
        }
        QBitmap qBitmapConstructQBitmapFromNativeHandle_noSkia = QBitmapFactory.constructQBitmapFromNativeHandle_noSkia(nativeQBmpHandle);
        if (qBitmapConstructQBitmapFromNativeHandle_noSkia == null) {
            return 1;
        }
        Bitmap bitmapCreateBitmapFromQBitmap = QAndroidBitmapFactory.createBitmapFromQBitmap(qBitmapConstructQBitmapFromNativeHandle_noSkia, false);
        if (bitmapCreateBitmapFromQBitmap == null) {
            return 4;
        }
        drawText(bitmapCreateBitmapFromQBitmap, srcTxt, tdp);
        int iCopyFromAndroidBitmap = qBitmapConstructQBitmapFromNativeHandle_noSkia.copyFromAndroidBitmap(bitmapCreateBitmapFromQBitmap);
        if (!bitmapCreateBitmapFromQBitmap.isRecycled()) {
            bitmapCreateBitmapFromQBitmap.recycle();
        }
        return iCopyFromAndroidBitmap;
    }

    private static void drawText_rotate_bg(Bitmap inBmp, String srcTxt, QTextDrawParam tdp, Bitmap outBmp) {
        if (inBmp == null || srcTxt == null || srcTxt.length() == 0 || tdp == null || outBmp == null || tdp.textRegionW <= 0 || tdp.textRegionH <= 0 || inBmp.getHeight() <= 0 || inBmp.getWidth() <= 0 || outBmp.getHeight() <= 0 || outBmp.getWidth() <= 0 || tdp.contentBGW <= 0 || tdp.contentBGH <= 0) {
            return;
        }
        float f10 = tdp.angle;
        tdp.angle = 0.0f;
        drawText(inBmp, srcTxt, tdp);
        Canvas canvas = new Canvas(outBmp);
        Matrix matrix = new Matrix();
        matrix.setRotate(f10, outBmp.getWidth() / 2, outBmp.getHeight() / 2);
        canvas.setMatrix(matrix);
        Paint paint = new Paint();
        paint.setFlags(3);
        canvas.drawBitmap(inBmp, (outBmp.getWidth() - inBmp.getWidth()) / 2, (outBmp.getHeight() - inBmp.getHeight()) / 2, paint);
    }

    private static void drawText(Bitmap bmp, String srcTxt, QTextDrawParam tdp) {
        QAutoMultiLineResult qAutoMultiLineResultAutoMultiLine_AutoScale;
        float f10;
        if (bmp == null || srcTxt == null || srcTxt.length() == 0 || tdp == null || tdp.textRegionW <= 0 || tdp.textRegionH <= 0 || bmp.getHeight() <= 0 || bmp.getWidth() <= 0 || tdp.contentBGW <= 0 || tdp.contentBGH <= 0) {
            return;
        }
        String strPreProcessString = preProcessString(srcTxt);
        TextPaint textPaintCreateBasicTextPaint = createBasicTextPaint(tdp);
        if (tdp.isAutoMultiLine) {
            qAutoMultiLineResultAutoMultiLine_AutoScale = autoMultiLine_AutoScale(textPaintCreateBasicTextPaint, strPreProcessString, tdp);
        } else {
            QAutoMultiLineResult qAutoMultiLineResult = new QAutoMultiLineResult();
            qAutoMultiLineResult.resultStr = strPreProcessString;
            qAutoMultiLineResult.txtLines = 1;
            qAutoMultiLineResultAutoMultiLine_AutoScale = qAutoMultiLineResult;
        }
        float fDecideTextSize = decideTextSize(textPaintCreateBasicTextPaint, qAutoMultiLineResultAutoMultiLine_AutoScale, tdp);
        setTextSize(textPaintCreateBasicTextPaint, fDecideTextSize, tdp);
        Paint.FontMetrics fontMetrics = textPaintCreateBasicTextPaint.getFontMetrics();
        textPaintCreateBasicTextPaint.getFontMetrics(fontMetrics);
        float f11 = fontMetrics.descent;
        float f12 = f11 - fontMetrics.ascent;
        float f13 = tdp.textRegionLeft;
        float f14 = ((tdp.textRegionTop + f12) - f11) + ((tdp.textRegionH - (qAutoMultiLineResultAutoMultiLine_AutoScale.txtLines * f12)) / 2.0f);
        Canvas canvas = new Canvas(bmp);
        QTextPoint qTextPoint = new QTextPoint();
        new QTextPoint();
        Size size = new Size();
        size.w = tdp.contentBGW;
        size.h = tdp.contentBGH;
        Size size2 = new Size();
        size2.w = bmp.getWidth();
        size2.h = bmp.getHeight();
        for (TextPaint textPaint : createTextPaints(textPaintCreateBasicTextPaint, tdp, fDecideTextSize)) {
            for (int i10 = 0; i10 < qAutoMultiLineResultAutoMultiLine_AutoScale.txtLines; i10++) {
                String oneLineCharsFromUniformString = getOneLineCharsFromUniformString(qAutoMultiLineResultAutoMultiLine_AutoScale.resultStr, i10);
                if (oneLineCharsFromUniformString != null) {
                    float fMeasureText = textPaintCreateBasicTextPaint.measureText(oneLineCharsFromUniformString);
                    int i11 = tdp.alignment;
                    if ((i11 & 2) != 0) {
                        f10 = tdp.textRegionW - fMeasureText;
                    } else {
                        f10 = (i11 & 1) != 0 ? 0.0f : (tdp.textRegionW - fMeasureText) / 2.0f;
                    }
                    float f15 = f10 + f13;
                    qTextPoint.x = f15;
                    float f16 = (i10 * f12) + f14;
                    qTextPoint.y = f16;
                    canvas.drawText(oneLineCharsFromUniformString, f15, f16, textPaint);
                }
            }
        }
    }
}
