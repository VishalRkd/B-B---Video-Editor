package com.quvideo.xiaoying.sdk.model.editor;

import android.graphics.Color;
import android.graphics.RectF;
import androidx.annotation.NonNull;
import com.quvideo.xiaoying.sdk.model.SubtitleAnim;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import xiaoying.engine.clip.QEffectTextAdvStyle;

/* JADX INFO: loaded from: classes15.dex */
public class TextBubbleInfo implements Serializable, Cloneable {
    public static final float RESET_STATE = -1.0f;
    public static final float SHADOW_DEFAULT_DISTANCE = 0.05f;
    public static final float SHADOW_DEFAULT_SIZE = 0.05f;
    public static final float SHADOW_MAX_DISTANCE = 0.5f;
    public static final float SHADOW_MAX_LINE_SPACE = 1.0f;
    public static final float SHADOW_MAX_SIZE = 0.5f;
    public static final float SHADOW_MAX_SPREAD = 1.0f;
    public static final float SHADOW_MAX_WORD_SPACE = 1.0f;
    public static final float STROKE_DEFAULT_WIDTH = 0.05f;
    public static final float STROKE_MAX_WIDTH = 0.5f;
    public List<TextBubble> mTextBubbleList;
    public int mBubbleSubtype = 0;
    public boolean bSupportAnim = false;
    public boolean isAnimOn = true;
    public int mTextEditableState = 0;

    public static class TextBubble implements Cloneable {
        public QEffectTextAdvStyle advStyle;

        /* JADX INFO: renamed from: in */
        public SubtitleAnim f94053in;
        public int mParamID;
        public RectF mTextRegion;
        public SubtitleAnim out;
        public int parentParamID;
        public SubtitleAnim repeat;
        public QEffectTextAdvStyle.TextBoardConfig textBoardConfig;
        public String mText = "Hello";
        public int mTextColor = -1;
        public String mTemplateTextContent = "";
        public int mDftTextColor = 0;
        public float mWordSpace = 0.0f;
        public float mLineSpace = 0.0f;
        public String mFontPath = "";
        public float mFontSize = 0.0f;
        public int mTextAlignment = 0;
        public ShadowInfo mShadowInfo = new ShadowInfo();
        public StrokeInfo mStrokeInfo = new StrokeInfo();

        public static QEffectTextAdvStyle cloneAdvStyle(QEffectTextAdvStyle oldAdvStyle) {
            QEffectTextAdvStyle qEffectTextAdvStyle = new QEffectTextAdvStyle();
            QEffectTextAdvStyle.TextAdvanceFill textAdvanceFill = oldAdvStyle.fontFill;
            if (textAdvanceFill != null) {
                qEffectTextAdvStyle.fontFill = cloneTextAdvanceFill(textAdvanceFill);
            }
            QEffectTextAdvStyle.TextShadowItem[] textShadowItemArr = oldAdvStyle.shadows;
            if (textShadowItemArr != null) {
                qEffectTextAdvStyle.shadows = cloneTextShadowItem(textShadowItemArr);
            }
            QEffectTextAdvStyle.TextStrokeItem[] textStrokeItemArr = oldAdvStyle.strokes;
            if (textStrokeItemArr != null) {
                qEffectTextAdvStyle.strokes = cloneTextStrokeItem(textStrokeItemArr);
            }
            return qEffectTextAdvStyle;
        }

        private static QEffectTextAdvStyle.MColorRGB cloneMColorRGB(QEffectTextAdvStyle.MColorRGB oldColor) {
            QEffectTextAdvStyle.MColorRGB mColorRGB = new QEffectTextAdvStyle.MColorRGB();
            mColorRGB.R = oldColor.R;
            mColorRGB.G = oldColor.G;
            mColorRGB.B = oldColor.B;
            return mColorRGB;
        }

        private static QEffectTextAdvStyle.TextAdvanceFill cloneTextAdvanceFill(QEffectTextAdvStyle.TextAdvanceFill oldAdvanceFill) {
            QEffectTextAdvStyle.TextAdvanceFill textAdvanceFill = new QEffectTextAdvStyle.TextAdvanceFill();
            textAdvanceFill.fillType = oldAdvanceFill.fillType;
            textAdvanceFill.fillImagePath = oldAdvanceFill.fillImagePath;
            textAdvanceFill.opacity = oldAdvanceFill.opacity;
            textAdvanceFill.pathStrokeSize = oldAdvanceFill.pathStrokeSize;
            QEffectTextAdvStyle.TextGradientStyle textGradientStyle = oldAdvanceFill.gradient;
            if (textGradientStyle != null) {
                textAdvanceFill.gradient = cloneTextGradientStyle(textGradientStyle);
            }
            QEffectTextAdvStyle.MColorRGB mColorRGB = oldAdvanceFill.fillColor;
            if (mColorRGB != null) {
                textAdvanceFill.fillColor = cloneMColorRGB(mColorRGB);
            }
            return textAdvanceFill;
        }

        public static QEffectTextAdvStyle.TextBoardConfig cloneTextBoardConfig(QEffectTextAdvStyle.TextBoardConfig oldBoardConfig) {
            QEffectTextAdvStyle.TextBoardConfig textBoardConfig = new QEffectTextAdvStyle.TextBoardConfig();
            textBoardConfig.showBoard = oldBoardConfig.showBoard;
            textBoardConfig.boardRound = oldBoardConfig.boardRound;
            textBoardConfig.boardFill = cloneTextAdvanceFill(oldBoardConfig.boardFill);
            return textBoardConfig;
        }

        public static List<TextBubble> cloneTextBubbleLists(List<TextBubble> textBubbleList) {
            if (textBubbleList == null) {
                return null;
            }
            ArrayList arrayList = new ArrayList();
            try {
                Iterator<TextBubble> it = textBubbleList.iterator();
                while (it.hasNext()) {
                    arrayList.add(it.next().m178765clone());
                }
            } catch (Throwable unused) {
            }
            return arrayList;
        }

        private static QEffectTextAdvStyle.TextGradientPoint[] cloneTextGradientPoint(QEffectTextAdvStyle.TextGradientPoint[] oldPoints) {
            QEffectTextAdvStyle.TextGradientPoint[] textGradientPointArr = new QEffectTextAdvStyle.TextGradientPoint[oldPoints.length];
            for (int i10 = 0; i10 < oldPoints.length; i10++) {
                if (oldPoints[i10] != null) {
                    QEffectTextAdvStyle.TextGradientPoint textGradientPoint = new QEffectTextAdvStyle.TextGradientPoint();
                    textGradientPoint.color = cloneMColorRGB(oldPoints[i10].color);
                    textGradientPoint.position = oldPoints[i10].position;
                    textGradientPointArr[i10] = textGradientPoint;
                }
            }
            return textGradientPointArr;
        }

        private static QEffectTextAdvStyle.TextGradientStyle cloneTextGradientStyle(QEffectTextAdvStyle.TextGradientStyle oldGradientStyle) {
            QEffectTextAdvStyle.TextGradientStyle textGradientStyle = new QEffectTextAdvStyle.TextGradientStyle();
            textGradientStyle.scale = oldGradientStyle.scale;
            textGradientStyle.angle = oldGradientStyle.angle;
            QEffectTextAdvStyle.TextGradientPoint[] textGradientPointArr = oldGradientStyle.points;
            if (textGradientPointArr != null) {
                textGradientStyle.points = cloneTextGradientPoint(textGradientPointArr);
            }
            return textGradientStyle;
        }

        private static QEffectTextAdvStyle.TextShadowItem[] cloneTextShadowItem(QEffectTextAdvStyle.TextShadowItem[] oldShadows) {
            QEffectTextAdvStyle.TextShadowItem[] textShadowItemArr = new QEffectTextAdvStyle.TextShadowItem[oldShadows.length];
            for (int i10 = 0; i10 < oldShadows.length; i10++) {
                if (oldShadows[i10] != null) {
                    QEffectTextAdvStyle.TextShadowItem textShadowItem = new QEffectTextAdvStyle.TextShadowItem();
                    QEffectTextAdvStyle.TextShadowItem textShadowItem2 = oldShadows[i10];
                    textShadowItem.opacity = textShadowItem2.opacity;
                    textShadowItem.distance = textShadowItem2.distance;
                    textShadowItem.angle = textShadowItem2.angle;
                    textShadowItem.spread = textShadowItem2.spread;
                    textShadowItem.size = textShadowItem2.size;
                    textShadowItem.color = cloneMColorRGB(textShadowItem2.color);
                    textShadowItemArr[i10] = textShadowItem;
                }
            }
            return textShadowItemArr;
        }

        private static QEffectTextAdvStyle.TextStrokeItem[] cloneTextStrokeItem(QEffectTextAdvStyle.TextStrokeItem[] oldTextStrokeItems) {
            QEffectTextAdvStyle.TextStrokeItem[] textStrokeItemArr = new QEffectTextAdvStyle.TextStrokeItem[oldTextStrokeItems.length];
            for (int i10 = 0; i10 < oldTextStrokeItems.length; i10++) {
                if (oldTextStrokeItems[i10] != null) {
                    QEffectTextAdvStyle.TextStrokeItem textStrokeItem = new QEffectTextAdvStyle.TextStrokeItem();
                    QEffectTextAdvStyle.TextStrokeItem textStrokeItem2 = oldTextStrokeItems[i10];
                    textStrokeItem.size = textStrokeItem2.size;
                    textStrokeItem.opacity = textStrokeItem2.opacity;
                    textStrokeItem.color = cloneMColorRGB(textStrokeItem2.color);
                    textStrokeItemArr[i10] = textStrokeItem;
                }
            }
            return textStrokeItemArr;
        }

        public void deleteAdvShadow(int index) {
            QEffectTextAdvStyle qEffectTextAdvStyle = this.advStyle;
            if (qEffectTextAdvStyle != null) {
                QEffectTextAdvStyle.TextShadowItem[] textShadowItemArr = qEffectTextAdvStyle.shadows;
                if (textShadowItemArr == null) {
                    return;
                }
                if (index <= textShadowItemArr.length - 1) {
                    ArrayList arrayList = new ArrayList(Arrays.asList(this.advStyle.shadows));
                    arrayList.remove(index);
                    this.advStyle.shadows = (QEffectTextAdvStyle.TextShadowItem[]) arrayList.toArray(new QEffectTextAdvStyle.TextShadowItem[arrayList.size()]);
                }
            }
        }

        public void deleteAdvStroke(int index) {
            QEffectTextAdvStyle qEffectTextAdvStyle = this.advStyle;
            if (qEffectTextAdvStyle != null) {
                QEffectTextAdvStyle.TextStrokeItem[] textStrokeItemArr = qEffectTextAdvStyle.strokes;
                if (textStrokeItemArr == null) {
                    return;
                }
                if (index <= textStrokeItemArr.length - 1) {
                    ArrayList arrayList = new ArrayList(Arrays.asList(this.advStyle.strokes));
                    arrayList.remove(index);
                    this.advStyle.strokes = (QEffectTextAdvStyle.TextStrokeItem[]) arrayList.toArray(new QEffectTextAdvStyle.TextStrokeItem[arrayList.size()]);
                }
            }
        }

        public void deleteAllAdvShadow() {
            QEffectTextAdvStyle qEffectTextAdvStyle = this.advStyle;
            if (qEffectTextAdvStyle == null) {
                return;
            }
            qEffectTextAdvStyle.shadows = new QEffectTextAdvStyle.TextShadowItem[0];
        }

        public void deleteAllAdvStroke() {
            QEffectTextAdvStyle qEffectTextAdvStyle = this.advStyle;
            if (qEffectTextAdvStyle == null) {
                return;
            }
            qEffectTextAdvStyle.strokes = new QEffectTextAdvStyle.TextStrokeItem[0];
        }

        public boolean equals(Object o10) {
            if (this == o10) {
                return true;
            }
            if (o10 == null || getClass() != o10.getClass()) {
                return false;
            }
            TextBubble textBubble = (TextBubble) o10;
            if (this.mParamID == textBubble.mParamID && this.parentParamID == textBubble.parentParamID && this.mTextColor == textBubble.mTextColor && this.mDftTextColor == textBubble.mDftTextColor && this.mTextAlignment == textBubble.mTextAlignment && this.mTextRegion.equals(textBubble.mTextRegion) && this.mText.equals(textBubble.mText) && this.mTemplateTextContent.equals(textBubble.mTemplateTextContent) && this.mFontPath.equals(textBubble.mFontPath) && this.mShadowInfo.equals(textBubble.mShadowInfo) && this.mWordSpace == textBubble.mWordSpace && this.mLineSpace == textBubble.mLineSpace && this.mFontSize == textBubble.mFontSize) {
                return this.mStrokeInfo.equals(textBubble.mStrokeInfo);
            }
            return false;
        }

        public void resetShadow(int index) {
            QEffectTextAdvStyle qEffectTextAdvStyle = this.advStyle;
            if (qEffectTextAdvStyle != null) {
                QEffectTextAdvStyle.TextShadowItem[] textShadowItemArr = qEffectTextAdvStyle.shadows;
                if (textShadowItemArr == null) {
                    return;
                }
                if (index <= textShadowItemArr.length - 1) {
                    QEffectTextAdvStyle.TextShadowItem textShadowItem = textShadowItemArr[index];
                    textShadowItem.opacity = -1.0f;
                    textShadowItem.size = 0.0f;
                    textShadowItem.color = new QEffectTextAdvStyle.MColorRGB();
                    textShadowItem.angle = 0.0f;
                    textShadowItem.spread = 0.0f;
                    textShadowItem.distance = 0.0f;
                }
            }
        }

        public void resetStroke(int index) {
            QEffectTextAdvStyle qEffectTextAdvStyle = this.advStyle;
            if (qEffectTextAdvStyle != null) {
                QEffectTextAdvStyle.TextStrokeItem[] textStrokeItemArr = qEffectTextAdvStyle.strokes;
                if (textStrokeItemArr == null) {
                    return;
                }
                if (index <= textStrokeItemArr.length - 1) {
                    QEffectTextAdvStyle.TextStrokeItem textStrokeItem = textStrokeItemArr[index];
                    textStrokeItem.opacity = -1.0f;
                    textStrokeItem.size = 0.0f;
                    textStrokeItem.color = new QEffectTextAdvStyle.MColorRGB();
                }
            }
        }

        public TextBubble save(TextBubble bubble) {
            if (bubble == null) {
                return this;
            }
            this.mParamID = bubble.mParamID;
            this.parentParamID = bubble.parentParamID;
            this.mTextRegion = bubble.mTextRegion;
            this.mText = bubble.mText;
            this.mTextColor = bubble.mTextColor;
            this.mTemplateTextContent = bubble.mTemplateTextContent;
            this.mDftTextColor = bubble.mDftTextColor;
            this.mFontPath = bubble.mFontPath;
            this.mTextAlignment = bubble.mTextAlignment;
            this.mLineSpace = bubble.mLineSpace;
            this.mFontSize = bubble.mFontSize;
            this.mWordSpace = bubble.mWordSpace;
            this.mShadowInfo.save(bubble.mShadowInfo);
            this.mStrokeInfo.save(bubble.mStrokeInfo);
            this.advStyle = bubble.advStyle;
            this.textBoardConfig = bubble.textBoardConfig;
            this.f94053in = bubble.f94053in;
            this.out = bubble.out;
            this.repeat = bubble.repeat;
            return this;
        }

        public void setAdvTextBackGround(int color, float boardRound) {
            QEffectTextAdvStyle.TextAdvanceFill textAdvanceFill;
            QEffectTextAdvStyle.TextBoardConfig textBoardConfig = this.textBoardConfig;
            float f10 = (textBoardConfig == null || (textAdvanceFill = textBoardConfig.boardFill) == null) ? 1.0f : textAdvanceFill.opacity;
            QEffectTextAdvStyle.TextBoardConfig textBoardConfig2 = new QEffectTextAdvStyle.TextBoardConfig();
            this.textBoardConfig = textBoardConfig2;
            textBoardConfig2.showBoard = true;
            textBoardConfig2.boardRound = boardRound;
            QEffectTextAdvStyle.TextAdvanceFill textAdvanceFill2 = new QEffectTextAdvStyle.TextAdvanceFill();
            this.textBoardConfig.boardFill = textAdvanceFill2;
            textAdvanceFill2.fillType = 0;
            textAdvanceFill2.fillColor = TextBubbleInfo.color2RGB(color);
            textAdvanceFill2.opacity = f10;
        }

        public void setAdvTextBackGroundGradientAngel(float angle) {
            QEffectTextAdvStyle.TextAdvanceFill textAdvanceFill;
            QEffectTextAdvStyle.TextGradientStyle textGradientStyle;
            QEffectTextAdvStyle.TextBoardConfig textBoardConfig = this.textBoardConfig;
            if (textBoardConfig != null && (textAdvanceFill = textBoardConfig.boardFill) != null && (textGradientStyle = textAdvanceFill.gradient) != null) {
                textGradientStyle.angle = angle;
            }
        }

        public void setAdvTextShadowAngle(float angle, int index) {
            QEffectTextAdvStyle qEffectTextAdvStyle = this.advStyle;
            if (qEffectTextAdvStyle != null) {
                QEffectTextAdvStyle.TextShadowItem[] textShadowItemArr = qEffectTextAdvStyle.shadows;
                if (textShadowItemArr == null) {
                    return;
                }
                if (index <= textShadowItemArr.length - 1) {
                    textShadowItemArr[index].angle = angle;
                }
            }
        }

        public void setAdvTextShadowColor(int color, int index) {
            QEffectTextAdvStyle qEffectTextAdvStyle = this.advStyle;
            if (qEffectTextAdvStyle != null) {
                QEffectTextAdvStyle.TextShadowItem[] textShadowItemArr = qEffectTextAdvStyle.shadows;
                if (textShadowItemArr == null) {
                    return;
                }
                if (index > textShadowItemArr.length - 1) {
                    ArrayList arrayList = new ArrayList(Arrays.asList(this.advStyle.shadows));
                    QEffectTextAdvStyle.TextShadowItem textShadowItem = new QEffectTextAdvStyle.TextShadowItem();
                    float fMin = Math.min((this.advStyle.shadows.length + 1) * 0.08f * 0.5f, 0.5f);
                    textShadowItem.color = TextBubbleInfo.color2RGB(color);
                    textShadowItem.size = 0.05f;
                    textShadowItem.angle = 45.0f;
                    textShadowItem.opacity = 0.6f;
                    textShadowItem.distance = fMin;
                    textShadowItem.spread = 0.0f;
                    arrayList.add(textShadowItem);
                    this.advStyle.shadows = (QEffectTextAdvStyle.TextShadowItem[]) arrayList.toArray(new QEffectTextAdvStyle.TextShadowItem[arrayList.size()]);
                    return;
                }
                if (index >= 0 && index <= textShadowItemArr.length - 1) {
                    textShadowItemArr[index].color = TextBubbleInfo.color2RGB(color);
                    float fMin2 = Math.min((index + 1) * 0.08f * 0.5f, 0.5f);
                    QEffectTextAdvStyle.TextShadowItem textShadowItem2 = this.advStyle.shadows[index];
                    if (textShadowItem2.opacity == -1.0f) {
                        textShadowItem2.size = 0.05f;
                        textShadowItem2.angle = 45.0f;
                        textShadowItem2.opacity = 0.6f;
                        textShadowItem2.distance = fMin2;
                        textShadowItem2.spread = 0.0f;
                    }
                }
            }
        }

        public void setAdvTextShadowDistance(float distance, int index) {
            QEffectTextAdvStyle qEffectTextAdvStyle = this.advStyle;
            if (qEffectTextAdvStyle != null) {
                QEffectTextAdvStyle.TextShadowItem[] textShadowItemArr = qEffectTextAdvStyle.shadows;
                if (textShadowItemArr == null) {
                    return;
                }
                if (index <= textShadowItemArr.length - 1) {
                    textShadowItemArr[index].distance = distance;
                }
            }
        }

        public void setAdvTextShadowOpacity(float opacity, int index) {
            QEffectTextAdvStyle qEffectTextAdvStyle = this.advStyle;
            if (qEffectTextAdvStyle != null) {
                QEffectTextAdvStyle.TextShadowItem[] textShadowItemArr = qEffectTextAdvStyle.shadows;
                if (textShadowItemArr == null) {
                    return;
                }
                if (index <= textShadowItemArr.length - 1) {
                    textShadowItemArr[index].opacity = opacity;
                }
            }
        }

        public void setAdvTextShadowSize(float size, int index) {
            QEffectTextAdvStyle qEffectTextAdvStyle = this.advStyle;
            if (qEffectTextAdvStyle != null) {
                QEffectTextAdvStyle.TextShadowItem[] textShadowItemArr = qEffectTextAdvStyle.shadows;
                if (textShadowItemArr == null) {
                    return;
                }
                if (index <= textShadowItemArr.length - 1) {
                    textShadowItemArr[index].size = size;
                }
            }
        }

        public void setAdvTextShadowSpread(float spread, int index) {
            QEffectTextAdvStyle qEffectTextAdvStyle = this.advStyle;
            if (qEffectTextAdvStyle != null) {
                QEffectTextAdvStyle.TextShadowItem[] textShadowItemArr = qEffectTextAdvStyle.shadows;
                if (textShadowItemArr == null) {
                    return;
                }
                if (index <= textShadowItemArr.length - 1) {
                    textShadowItemArr[index].spread = spread;
                }
            }
        }

        public void setAdvTextStrokeColor(int color, int index) {
            QEffectTextAdvStyle qEffectTextAdvStyle = this.advStyle;
            if (qEffectTextAdvStyle != null) {
                QEffectTextAdvStyle.TextStrokeItem[] textStrokeItemArr = qEffectTextAdvStyle.strokes;
                if (textStrokeItemArr == null) {
                    return;
                }
                float f10 = 0.0f;
                if (index > textStrokeItemArr.length - 1) {
                    ArrayList arrayList = new ArrayList(Arrays.asList(this.advStyle.strokes));
                    QEffectTextAdvStyle.TextStrokeItem textStrokeItem = new QEffectTextAdvStyle.TextStrokeItem();
                    textStrokeItem.color = TextBubbleInfo.color2RGB(color);
                    Iterator it = arrayList.iterator();
                    loop0: while (true) {
                        while (true) {
                            if (!it.hasNext()) {
                                break loop0;
                            }
                            float f11 = ((QEffectTextAdvStyle.TextStrokeItem) it.next()).size;
                            if (f11 > f10) {
                                f10 = f11;
                            }
                        }
                    }
                    float f12 = f10 + 0.05f;
                    if (f12 > 0.5f) {
                        textStrokeItem.size = 0.5f;
                    } else {
                        textStrokeItem.size = f12;
                    }
                    arrayList.add(textStrokeItem);
                    this.advStyle.strokes = (QEffectTextAdvStyle.TextStrokeItem[]) arrayList.toArray(new QEffectTextAdvStyle.TextStrokeItem[arrayList.size()]);
                    return;
                }
                if (index >= 0 && index <= textStrokeItemArr.length - 1) {
                    textStrokeItemArr[index].color = TextBubbleInfo.color2RGB(color);
                    QEffectTextAdvStyle.TextStrokeItem[] textStrokeItemArr2 = this.advStyle.strokes;
                    QEffectTextAdvStyle.TextStrokeItem textStrokeItem2 = textStrokeItemArr2[index];
                    if (textStrokeItem2.opacity == -1.0f) {
                        textStrokeItem2.opacity = 1.0f;
                        int i10 = index - 1;
                        if (i10 >= 0 && i10 <= textStrokeItemArr2.length - 1) {
                            for (int i11 = 0; i11 <= i10; i11++) {
                                float f13 = this.advStyle.strokes[i11].size;
                                if (f13 > f10) {
                                    f10 = f13;
                                }
                            }
                            float f14 = f10 + 0.05f;
                            if (f14 > 0.5f) {
                                this.advStyle.strokes[index].size = 0.5f;
                                return;
                            } else {
                                this.advStyle.strokes[index].size = f14;
                                return;
                            }
                        }
                        textStrokeItem2.size = 0.05f;
                    }
                }
            }
        }

        public void setAdvTextStrokeOpacity(float opacity, int index) {
            QEffectTextAdvStyle qEffectTextAdvStyle = this.advStyle;
            if (qEffectTextAdvStyle != null) {
                QEffectTextAdvStyle.TextStrokeItem[] textStrokeItemArr = qEffectTextAdvStyle.strokes;
                if (textStrokeItemArr == null) {
                    return;
                }
                if (index <= textStrokeItemArr.length - 1) {
                    textStrokeItemArr[index].opacity = opacity;
                }
            }
        }

        public void setAdvTextStrokeWidth(float size, int index) {
            QEffectTextAdvStyle qEffectTextAdvStyle = this.advStyle;
            if (qEffectTextAdvStyle != null) {
                QEffectTextAdvStyle.TextStrokeItem[] textStrokeItemArr = qEffectTextAdvStyle.strokes;
                if (textStrokeItemArr == null) {
                    return;
                }
                if (index <= textStrokeItemArr.length - 1) {
                    textStrokeItemArr[index].size = size;
                }
            }
        }

        public void setTextColorAdv(int color) {
            this.advStyle.fontFill.fillColor = TextBubbleInfo.color2RGB(color);
            this.advStyle.fontFill.fillType = 0;
        }

        public void setTextFillColorAngle(int angle) {
            QEffectTextAdvStyle.TextAdvanceFill textAdvanceFill;
            QEffectTextAdvStyle qEffectTextAdvStyle = this.advStyle;
            if (qEffectTextAdvStyle != null && (textAdvanceFill = qEffectTextAdvStyle.fontFill) != null) {
                QEffectTextAdvStyle.TextGradientStyle textGradientStyle = textAdvanceFill.gradient;
                if (textGradientStyle == null) {
                } else {
                    textGradientStyle.angle = angle;
                }
            }
        }

        public void setTextFillColorOpacity(float opacity) {
            QEffectTextAdvStyle qEffectTextAdvStyle = this.advStyle;
            if (qEffectTextAdvStyle != null) {
                QEffectTextAdvStyle.TextAdvanceFill textAdvanceFill = qEffectTextAdvStyle.fontFill;
                if (textAdvanceFill == null) {
                } else {
                    textAdvanceFill.opacity = opacity;
                }
            }
        }

        public void setTextGriantColor(int[] griantcolor, float[] positions, float angle, float scale) {
            QEffectTextAdvStyle qEffectTextAdvStyle = this.advStyle;
            if (qEffectTextAdvStyle != null && griantcolor.length == positions.length) {
                qEffectTextAdvStyle.fontFill.gradient = new QEffectTextAdvStyle.TextGradientStyle();
                QEffectTextAdvStyle.TextGradientPoint[] textGradientPointArr = new QEffectTextAdvStyle.TextGradientPoint[positions.length];
                for (int i10 = 0; i10 < positions.length; i10++) {
                    QEffectTextAdvStyle.TextGradientPoint textGradientPoint = new QEffectTextAdvStyle.TextGradientPoint();
                    textGradientPointArr[i10] = textGradientPoint;
                    textGradientPoint.position = positions[i10];
                    textGradientPoint.color = TextBubbleInfo.color2RGB(griantcolor[i10]);
                }
                QEffectTextAdvStyle.TextAdvanceFill textAdvanceFill = this.advStyle.fontFill;
                QEffectTextAdvStyle.TextGradientStyle textGradientStyle = textAdvanceFill.gradient;
                textGradientStyle.points = textGradientPointArr;
                textGradientStyle.angle = angle;
                textGradientStyle.scale = scale;
                textAdvanceFill.fillType = 2;
            }
        }

        @NonNull
        /* JADX INFO: renamed from: clone, reason: merged with bridge method [inline-methods] */
        public TextBubble m178765clone() throws CloneNotSupportedException {
            TextBubble textBubble = (TextBubble) super.clone();
            ShadowInfo shadowInfo = this.mShadowInfo;
            if (shadowInfo != null) {
                textBubble.mShadowInfo = (ShadowInfo) shadowInfo.clone();
            }
            StrokeInfo strokeInfo = this.mStrokeInfo;
            if (strokeInfo != null) {
                textBubble.mStrokeInfo = (StrokeInfo) strokeInfo.clone();
            }
            QEffectTextAdvStyle qEffectTextAdvStyle = this.advStyle;
            if (qEffectTextAdvStyle != null) {
                textBubble.advStyle = cloneAdvStyle(qEffectTextAdvStyle);
            }
            QEffectTextAdvStyle.TextBoardConfig textBoardConfig = this.textBoardConfig;
            if (textBoardConfig != null) {
                textBubble.textBoardConfig = cloneTextBoardConfig(textBoardConfig);
            }
            SubtitleAnim subtitleAnim = this.f94053in;
            if (subtitleAnim != null) {
                textBubble.f94053in = subtitleAnim.cloneSafely();
            }
            SubtitleAnim subtitleAnim2 = this.out;
            if (subtitleAnim2 != null) {
                textBubble.out = subtitleAnim2.cloneSafely();
            }
            SubtitleAnim subtitleAnim3 = this.repeat;
            if (subtitleAnim3 != null) {
                textBubble.repeat = subtitleAnim3.cloneSafely();
            }
            return textBubble;
        }

        public void setAdvTextBackGround(int[] colorArray, float boardRound) {
            QEffectTextAdvStyle.TextAdvanceFill textAdvanceFill;
            QEffectTextAdvStyle.TextGradientStyle textGradientStyle;
            QEffectTextAdvStyle.TextAdvanceFill textAdvanceFill2;
            if (colorArray != null) {
                if (colorArray.length <= 0) {
                    return;
                }
                QEffectTextAdvStyle.TextBoardConfig textBoardConfig = this.textBoardConfig;
                float f10 = (textBoardConfig == null || (textAdvanceFill2 = textBoardConfig.boardFill) == null) ? 1.0f : textAdvanceFill2.opacity;
                float f11 = (textBoardConfig == null || (textAdvanceFill = textBoardConfig.boardFill) == null || (textGradientStyle = textAdvanceFill.gradient) == null) ? -90.0f : textGradientStyle.angle;
                QEffectTextAdvStyle.TextBoardConfig textBoardConfig2 = new QEffectTextAdvStyle.TextBoardConfig();
                this.textBoardConfig = textBoardConfig2;
                textBoardConfig2.showBoard = true;
                textBoardConfig2.boardRound = boardRound;
                QEffectTextAdvStyle.TextAdvanceFill textAdvanceFill3 = new QEffectTextAdvStyle.TextAdvanceFill();
                this.textBoardConfig.boardFill = textAdvanceFill3;
                if (colorArray.length <= 1) {
                    textAdvanceFill3.fillType = 0;
                    textAdvanceFill3.fillColor = TextBubbleInfo.color2RGB(colorArray[0]);
                    textAdvanceFill3.opacity = f10;
                } else {
                    QEffectTextAdvStyle.TextGradientPoint[] textGradientPointArr = new QEffectTextAdvStyle.TextGradientPoint[colorArray.length];
                    for (int i10 = 0; i10 < colorArray.length; i10++) {
                        QEffectTextAdvStyle.TextGradientPoint textGradientPoint = new QEffectTextAdvStyle.TextGradientPoint();
                        textGradientPoint.position = (1.0f / (colorArray.length - 1)) * i10;
                        QEffectTextAdvStyle.MColorRGB mColorRGB = new QEffectTextAdvStyle.MColorRGB();
                        textGradientPoint.color = mColorRGB;
                        mColorRGB.R = Color.red(colorArray[i10]);
                        textGradientPoint.color.G = Color.green(colorArray[i10]);
                        textGradientPoint.color.B = Color.blue(colorArray[i10]);
                        textGradientPointArr[i10] = textGradientPoint;
                    }
                    textAdvanceFill3.fillType = 2;
                    QEffectTextAdvStyle.TextGradientStyle textGradientStyle2 = new QEffectTextAdvStyle.TextGradientStyle();
                    textAdvanceFill3.gradient = textGradientStyle2;
                    textGradientStyle2.angle = f11;
                    textGradientStyle2.points = textGradientPointArr;
                    textAdvanceFill3.opacity = f10;
                }
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static QEffectTextAdvStyle.MColorRGB color2RGB(int color) {
        return new QEffectTextAdvStyle.MColorRGB(Color.red(color), Color.green(color), Color.blue(color));
    }

    public boolean equals(Object o10) {
        if (this == o10) {
            return true;
        }
        if (o10 == null || getClass() != o10.getClass()) {
            return false;
        }
        TextBubbleInfo textBubbleInfo = (TextBubbleInfo) o10;
        if (this.mBubbleSubtype == textBubbleInfo.mBubbleSubtype && this.bSupportAnim == textBubbleInfo.bSupportAnim && this.isAnimOn == textBubbleInfo.isAnimOn && this.mTextEditableState == textBubbleInfo.mTextEditableState) {
            List<TextBubble> list = this.mTextBubbleList;
            if (list == null || textBubbleInfo.mTextBubbleList == null) {
                return list == null && textBubbleInfo.mTextBubbleList == null;
            }
            return list.size() == textBubbleInfo.mTextBubbleList.size();
        }
        return false;
    }

    public QEffectTextAdvStyle getAdvStyle(int paramId) {
        TextBubble dftTextBubble = getDftTextBubble(paramId);
        if (dftTextBubble != null) {
            return dftTextBubble.advStyle;
        }
        return null;
    }

    public TextBubble getDftTextBubble(int paramId) {
        List<TextBubble> list = this.mTextBubbleList;
        if (list == null || list.size() <= 0) {
            return null;
        }
        for (TextBubble textBubble : this.mTextBubbleList) {
            if (textBubble.mParamID == paramId) {
                return textBubble;
            }
        }
        return this.mTextBubbleList.get(0);
    }

    public String getFontPath(int paramId) {
        TextBubble dftTextBubble = getDftTextBubble(paramId);
        if (dftTextBubble != null) {
            return dftTextBubble.mFontPath;
        }
        return null;
    }

    public String getTemplateTextContent(int paramId) {
        TextBubble dftTextBubble = getDftTextBubble(paramId);
        if (dftTextBubble != null) {
            return dftTextBubble.mTemplateTextContent;
        }
        return null;
    }

    public String getText(int paramId) {
        TextBubble dftTextBubble = getDftTextBubble(paramId);
        return dftTextBubble != null ? dftTextBubble.mText : "";
    }

    public int getTextAlignment(int paramId) {
        TextBubble dftTextBubble = getDftTextBubble(paramId);
        if (dftTextBubble != null) {
            return dftTextBubble.mTextAlignment;
        }
        return 0;
    }

    public QEffectTextAdvStyle.TextBoardConfig getTextBoardConfig(int paramId) {
        TextBubble dftTextBubble = getDftTextBubble(paramId);
        if (dftTextBubble != null) {
            return dftTextBubble.textBoardConfig;
        }
        return null;
    }

    public int getTextColor(int paramId) {
        TextBubble dftTextBubble = getDftTextBubble(paramId);
        if (dftTextBubble != null) {
            return dftTextBubble.mTextColor;
        }
        return -1;
    }

    public int getTextDftColor(int paramId) {
        TextBubble dftTextBubble = getDftTextBubble(paramId);
        if (dftTextBubble != null) {
            return dftTextBubble.mDftTextColor;
        }
        return -1;
    }

    public ShadowInfo getTextShadowInfo(int paramId) {
        TextBubble dftTextBubble = getDftTextBubble(paramId);
        if (dftTextBubble != null) {
            return dftTextBubble.mShadowInfo;
        }
        return null;
    }

    public StrokeInfo getTextStrokeInfo(int paramId) {
        TextBubble dftTextBubble = getDftTextBubble(paramId);
        if (dftTextBubble != null) {
            return dftTextBubble.mStrokeInfo;
        }
        return null;
    }

    public boolean isAnimOn() {
        return this.isAnimOn;
    }

    public boolean isSupportAnim() {
        return this.bSupportAnim;
    }

    public void save(TextBubbleInfo bubbleInfo) {
        if (bubbleInfo == null) {
            return;
        }
        this.bSupportAnim = bubbleInfo.bSupportAnim;
        this.isAnimOn = bubbleInfo.isAnimOn;
        this.mTextEditableState = bubbleInfo.mTextEditableState;
        List<TextBubble> list = bubbleInfo.mTextBubbleList;
        if (list != null) {
            if (list.isEmpty()) {
                return;
            }
            this.mTextBubbleList = new ArrayList(bubbleInfo.mTextBubbleList.size());
            Iterator<TextBubble> it = bubbleInfo.mTextBubbleList.iterator();
            while (it.hasNext()) {
                this.mTextBubbleList.add(new TextBubble().save(it.next()));
            }
        }
    }

    public void setAdvStyle(int paramId, QEffectTextAdvStyle advStyle) {
        TextBubble dftTextBubble = getDftTextBubble(paramId);
        if (dftTextBubble != null) {
            dftTextBubble.advStyle = advStyle;
        }
    }

    public void setAnimOn(boolean animOn) {
        this.isAnimOn = animOn;
    }

    public void setFontPath(int paramId, String path) {
        TextBubble dftTextBubble = getDftTextBubble(paramId);
        if (dftTextBubble != null) {
            dftTextBubble.mFontPath = path;
        }
    }

    public void setSupportAnim(boolean bSupportAnim) {
        this.bSupportAnim = bSupportAnim;
    }

    public void setTemplateTextContent(int paramId, String text) {
        TextBubble dftTextBubble = getDftTextBubble(paramId);
        if (dftTextBubble != null) {
            dftTextBubble.mTemplateTextContent = text;
        }
    }

    public void setText(int paramId, String text) {
        TextBubble dftTextBubble = getDftTextBubble(paramId);
        if (dftTextBubble != null) {
            dftTextBubble.mText = text;
        }
    }

    public void setTextAlignment(int paramId, int alignment) {
        TextBubble dftTextBubble = getDftTextBubble(paramId);
        if (dftTextBubble != null) {
            dftTextBubble.mTextAlignment = alignment;
        }
    }

    public void setTextColor(int paramId, int color) {
        TextBubble dftTextBubble = getDftTextBubble(paramId);
        if (dftTextBubble != null && dftTextBubble.advStyle != null) {
            dftTextBubble.setTextColorAdv(color);
        }
    }

    public void setTextFontSize(int paramId, float fontSize) {
        TextBubble dftTextBubble = getDftTextBubble(paramId);
        if (dftTextBubble != null) {
            dftTextBubble.mFontSize = fontSize;
        }
    }

    public void setTextLineSpace(int paramId, float space) {
        TextBubble dftTextBubble = getDftTextBubble(paramId);
        if (dftTextBubble != null) {
            dftTextBubble.mLineSpace = space;
        }
    }

    public void setTextShadowInfo(int paramId, ShadowInfo shadowInfo) {
        TextBubble dftTextBubble = getDftTextBubble(paramId);
        if (dftTextBubble != null) {
            dftTextBubble.mShadowInfo = shadowInfo;
        }
    }

    public void setTextStrokeInfo(int paramId, StrokeInfo strokeInfo) {
        TextBubble dftTextBubble = getDftTextBubble(paramId);
        if (dftTextBubble != null) {
            dftTextBubble.mStrokeInfo = strokeInfo;
        }
    }

    public void setTextWordSpace(int paramId, float space) {
        TextBubble dftTextBubble = getDftTextBubble(paramId);
        if (dftTextBubble != null) {
            dftTextBubble.mWordSpace = space;
        }
    }

    @NonNull
    /* JADX INFO: renamed from: clone, reason: merged with bridge method [inline-methods] */
    public TextBubbleInfo m178764clone() throws CloneNotSupportedException {
        TextBubbleInfo textBubbleInfo = (TextBubbleInfo) super.clone();
        List<TextBubble> list = this.mTextBubbleList;
        if (list != null) {
            textBubbleInfo.mTextBubbleList = TextBubble.cloneTextBubbleLists(list);
        }
        return textBubbleInfo;
    }
}
