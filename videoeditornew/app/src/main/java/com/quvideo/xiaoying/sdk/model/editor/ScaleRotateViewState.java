package com.quvideo.xiaoying.sdk.model.editor;

import android.graphics.Bitmap;
import android.graphics.Rect;
import android.graphics.RectF;
import androidx.annotation.NonNull;
import com.quvideo.xiaoying.sdk.editor.cache.VideoSpec;
import com.quvideo.xiaoying.sdk.model.StylePositionModel;
import com.quvideo.xiaoying.sdk.model.Ve3DDataF;
import java.io.Serializable;
import xiaoying.engine.base.QTransformInfo;
import xiaoying.engine.clip.QEffectTextAdvStyle;

/* JADX INFO: loaded from: classes15.dex */
public class ScaleRotateViewState implements Serializable, Cloneable {
    private static final long serialVersionUID = 1441663473695516809L;
    public Ve3DDataF anchorForEngine;
    public Ve3DDataF anchorOffset;
    public boolean bNeedTranslate;
    public int groupID;
    public boolean isDftTemplate;
    public boolean isHorFlip;
    public boolean isVerFlip;
    public transient Rect mActRelativeRect;
    public float mAlpha;
    public transient Bitmap mBitmap;
    public VideoSpec mCrop;
    public float mDegree;
    public int mExampleThumbPos;
    public float mFrameHeight;
    public float mFrameWidth;
    public int mLineNum;
    public int mMinDuration;
    public int mOutlineEllipse;
    public int mOutlineStrokeColor;
    public int mPadding;
    public StylePositionModel mPosInfo;
    public float mStrokeWidth;
    public String mStylePath;
    public TextBubbleInfo mTextBubbleInfo;
    public QTransformInfo mTransformInfo;
    public int mVersion;
    public transient RectF mViewRect;
    public int maxCharCount;

    public ScaleRotateViewState() {
        this.mVersion = 0;
        this.groupID = 0;
        this.bNeedTranslate = false;
        this.mTextBubbleInfo = new TextBubbleInfo();
        this.mPosInfo = new StylePositionModel();
        this.mDegree = 0.0f;
        this.maxCharCount = 10;
        this.mFrameWidth = 0.0f;
        this.mFrameHeight = 0.0f;
        this.mExampleThumbPos = 0;
        this.mMinDuration = 0;
        this.mBitmap = null;
        this.isDftTemplate = false;
        this.mLineNum = 1;
        this.mStylePath = "";
        this.isHorFlip = false;
        this.isVerFlip = false;
        this.mAlpha = 1.0f;
        this.mActRelativeRect = null;
        this.mPadding = 10;
        this.mStrokeWidth = 3.0f;
        this.mOutlineEllipse = 12;
        this.mOutlineStrokeColor = -34994;
    }

    public void copyPosInfo(ScaleRotateViewState state) {
        this.mPosInfo = state.mPosInfo;
        this.mDegree = state.mDegree;
        this.mViewRect = state.mViewRect;
        this.isHorFlip = state.isHorFlip;
        this.isVerFlip = state.isVerFlip;
        this.mCrop = state.mCrop;
        this.anchorForEngine = state.anchorForEngine;
        this.anchorOffset = state.anchorOffset;
    }

    /* JADX WARN: Code duplicated, block: B:100:0x0127  */
    /* JADX WARN: Code duplicated, block: B:101:0x0131  */
    /* JADX WARN: Code duplicated, block: B:103:0x0137  */
    /* JADX WARN: Code duplicated, block: B:104:0x0139  */
    /* JADX WARN: Code duplicated, block: B:73:0x00d6  */
    /* JADX WARN: Code duplicated, block: B:75:0x00e1  */
    /* JADX WARN: Code duplicated, block: B:76:0x00e3  */
    /* JADX WARN: Code duplicated, block: B:78:0x00e9  */
    /* JADX WARN: Code duplicated, block: B:82:0x00f1  */
    /* JADX WARN: Code duplicated, block: B:84:0x00fc  */
    /* JADX WARN: Code duplicated, block: B:85:0x00fe  */
    /* JADX WARN: Code duplicated, block: B:87:0x0104  */
    /* JADX WARN: Code duplicated, block: B:91:0x010c  */
    /* JADX WARN: Code duplicated, block: B:93:0x0117  */
    /* JADX WARN: Code duplicated, block: B:94:0x0119  */
    /* JADX WARN: Code duplicated, block: B:96:0x011f  */
    public boolean equals(Object o10) {
        String str;
        TextBubbleInfo textBubbleInfo;
        Ve3DDataF ve3DDataF;
        Rect rect;
        if (this == o10) {
            return true;
        }
        if (!(o10 instanceof ScaleRotateViewState)) {
            return false;
        }
        ScaleRotateViewState scaleRotateViewState = (ScaleRotateViewState) o10;
        if (this.mVersion == scaleRotateViewState.mVersion && this.groupID == scaleRotateViewState.groupID && this.bNeedTranslate == scaleRotateViewState.bNeedTranslate && Float.compare(scaleRotateViewState.mDegree, this.mDegree) == 0 && this.maxCharCount == scaleRotateViewState.maxCharCount && Float.compare(scaleRotateViewState.mFrameWidth, this.mFrameWidth) == 0 && Float.compare(scaleRotateViewState.mFrameHeight, this.mFrameHeight) == 0 && this.mMinDuration == scaleRotateViewState.mMinDuration && this.isDftTemplate == scaleRotateViewState.isDftTemplate && this.mLineNum == scaleRotateViewState.mLineNum && this.isHorFlip == scaleRotateViewState.isHorFlip && this.isVerFlip == scaleRotateViewState.isVerFlip && this.mAlpha == scaleRotateViewState.mAlpha) {
            StylePositionModel stylePositionModel = this.mPosInfo;
            if (stylePositionModel != null) {
                if (!stylePositionModel.equals(scaleRotateViewState.mPosInfo)) {
                    return false;
                }
                str = this.mStylePath;
                if (str != null) {
                    if (!str.equals(scaleRotateViewState.mStylePath)) {
                        return false;
                    }
                    textBubbleInfo = this.mTextBubbleInfo;
                    if (textBubbleInfo != null) {
                        if (!textBubbleInfo.equals(scaleRotateViewState.mTextBubbleInfo)) {
                            return false;
                        }
                        ve3DDataF = this.anchorOffset;
                        if (ve3DDataF != null) {
                            if (!ve3DDataF.equals(scaleRotateViewState.anchorOffset)) {
                                return false;
                            }
                            rect = this.mActRelativeRect;
                            if (rect != null) {
                                return rect.equals(scaleRotateViewState.mActRelativeRect);
                            }
                            return scaleRotateViewState.mActRelativeRect == null;
                        }
                        if (scaleRotateViewState.anchorOffset != null) {
                            return false;
                        }
                        rect = this.mActRelativeRect;
                        if (rect != null) {
                            return rect.equals(scaleRotateViewState.mActRelativeRect);
                        }
                        if (scaleRotateViewState.mActRelativeRect == null) {
                        }
                    }
                    if (scaleRotateViewState.mTextBubbleInfo != null) {
                        return false;
                    }
                    ve3DDataF = this.anchorOffset;
                    if (ve3DDataF != null) {
                        if (!ve3DDataF.equals(scaleRotateViewState.anchorOffset)) {
                            return false;
                        }
                        rect = this.mActRelativeRect;
                        if (rect != null) {
                            return rect.equals(scaleRotateViewState.mActRelativeRect);
                        }
                        if (scaleRotateViewState.mActRelativeRect == null) {
                        }
                    }
                    if (scaleRotateViewState.anchorOffset != null) {
                        return false;
                    }
                    rect = this.mActRelativeRect;
                    if (rect != null) {
                        return rect.equals(scaleRotateViewState.mActRelativeRect);
                    }
                    if (scaleRotateViewState.mActRelativeRect == null) {
                    }
                }
                if (scaleRotateViewState.mStylePath != null) {
                    return false;
                }
                textBubbleInfo = this.mTextBubbleInfo;
                if (textBubbleInfo != null) {
                    if (!textBubbleInfo.equals(scaleRotateViewState.mTextBubbleInfo)) {
                        return false;
                    }
                    ve3DDataF = this.anchorOffset;
                    if (ve3DDataF != null) {
                        if (!ve3DDataF.equals(scaleRotateViewState.anchorOffset)) {
                            return false;
                        }
                        rect = this.mActRelativeRect;
                        if (rect != null) {
                            return rect.equals(scaleRotateViewState.mActRelativeRect);
                        }
                        if (scaleRotateViewState.mActRelativeRect == null) {
                        }
                    }
                    if (scaleRotateViewState.anchorOffset != null) {
                        return false;
                    }
                    rect = this.mActRelativeRect;
                    if (rect != null) {
                        return rect.equals(scaleRotateViewState.mActRelativeRect);
                    }
                    if (scaleRotateViewState.mActRelativeRect == null) {
                    }
                }
                if (scaleRotateViewState.mTextBubbleInfo != null) {
                    return false;
                }
                ve3DDataF = this.anchorOffset;
                if (ve3DDataF != null) {
                    if (!ve3DDataF.equals(scaleRotateViewState.anchorOffset)) {
                        return false;
                    }
                    rect = this.mActRelativeRect;
                    if (rect != null) {
                        return rect.equals(scaleRotateViewState.mActRelativeRect);
                    }
                    if (scaleRotateViewState.mActRelativeRect == null) {
                    }
                }
                if (scaleRotateViewState.anchorOffset != null) {
                    return false;
                }
                rect = this.mActRelativeRect;
                if (rect != null) {
                    return rect.equals(scaleRotateViewState.mActRelativeRect);
                }
                if (scaleRotateViewState.mActRelativeRect == null) {
                }
            }
            if (scaleRotateViewState.mPosInfo != null) {
                return false;
            }
            str = this.mStylePath;
            if (str != null) {
                if (!str.equals(scaleRotateViewState.mStylePath)) {
                    return false;
                }
                textBubbleInfo = this.mTextBubbleInfo;
                if (textBubbleInfo != null) {
                    if (!textBubbleInfo.equals(scaleRotateViewState.mTextBubbleInfo)) {
                        return false;
                    }
                    ve3DDataF = this.anchorOffset;
                    if (ve3DDataF != null) {
                        if (!ve3DDataF.equals(scaleRotateViewState.anchorOffset)) {
                            return false;
                        }
                        rect = this.mActRelativeRect;
                        if (rect != null) {
                            return rect.equals(scaleRotateViewState.mActRelativeRect);
                        }
                        if (scaleRotateViewState.mActRelativeRect == null) {
                        }
                    }
                    if (scaleRotateViewState.anchorOffset != null) {
                        return false;
                    }
                    rect = this.mActRelativeRect;
                    if (rect != null) {
                        return rect.equals(scaleRotateViewState.mActRelativeRect);
                    }
                    if (scaleRotateViewState.mActRelativeRect == null) {
                    }
                }
                if (scaleRotateViewState.mTextBubbleInfo != null) {
                    return false;
                }
                ve3DDataF = this.anchorOffset;
                if (ve3DDataF != null) {
                    if (!ve3DDataF.equals(scaleRotateViewState.anchorOffset)) {
                        return false;
                    }
                    rect = this.mActRelativeRect;
                    if (rect != null) {
                        return rect.equals(scaleRotateViewState.mActRelativeRect);
                    }
                    if (scaleRotateViewState.mActRelativeRect == null) {
                    }
                }
                if (scaleRotateViewState.anchorOffset != null) {
                    return false;
                }
                rect = this.mActRelativeRect;
                if (rect != null) {
                    return rect.equals(scaleRotateViewState.mActRelativeRect);
                }
                if (scaleRotateViewState.mActRelativeRect == null) {
                }
            }
            if (scaleRotateViewState.mStylePath != null) {
                return false;
            }
            textBubbleInfo = this.mTextBubbleInfo;
            if (textBubbleInfo != null) {
                if (!textBubbleInfo.equals(scaleRotateViewState.mTextBubbleInfo)) {
                    return false;
                }
                ve3DDataF = this.anchorOffset;
                if (ve3DDataF != null) {
                    if (!ve3DDataF.equals(scaleRotateViewState.anchorOffset)) {
                        return false;
                    }
                    rect = this.mActRelativeRect;
                    if (rect != null) {
                        return rect.equals(scaleRotateViewState.mActRelativeRect);
                    }
                    if (scaleRotateViewState.mActRelativeRect == null) {
                    }
                }
                if (scaleRotateViewState.anchorOffset != null) {
                    return false;
                }
                rect = this.mActRelativeRect;
                if (rect != null) {
                    return rect.equals(scaleRotateViewState.mActRelativeRect);
                }
                if (scaleRotateViewState.mActRelativeRect == null) {
                }
            }
            if (scaleRotateViewState.mTextBubbleInfo != null) {
                return false;
            }
            ve3DDataF = this.anchorOffset;
            if (ve3DDataF != null) {
                if (!ve3DDataF.equals(scaleRotateViewState.anchorOffset)) {
                    return false;
                }
                rect = this.mActRelativeRect;
                if (rect != null) {
                    return rect.equals(scaleRotateViewState.mActRelativeRect);
                }
                if (scaleRotateViewState.mActRelativeRect == null) {
                }
            }
            if (scaleRotateViewState.anchorOffset != null) {
                return false;
            }
            rect = this.mActRelativeRect;
            if (rect != null) {
                return rect.equals(scaleRotateViewState.mActRelativeRect);
            }
            if (scaleRotateViewState.mActRelativeRect == null) {
            }
        }
        return false;
    }

    public QEffectTextAdvStyle getAdvStyle(int paramId) {
        TextBubbleInfo textBubbleInfo = this.mTextBubbleInfo;
        if (textBubbleInfo != null) {
            return textBubbleInfo.getAdvStyle(paramId);
        }
        return null;
    }

    public Ve3DDataF getAnchorForEngine() {
        return this.anchorForEngine;
    }

    public Ve3DDataF getAnchorOffset() {
        return this.anchorOffset;
    }

    public VideoSpec getCrop() {
        return this.mCrop;
    }

    public RectF getRectArea() {
        StylePositionModel stylePositionModel = this.mPosInfo;
        if (stylePositionModel != null) {
            return stylePositionModel.getRectArea();
        }
        return null;
    }

    public ShadowInfo getShadowInfo(int paramId) {
        TextBubbleInfo textBubbleInfo = this.mTextBubbleInfo;
        if (textBubbleInfo != null) {
            return textBubbleInfo.getTextShadowInfo(paramId);
        }
        return null;
    }

    public StrokeInfo getStrokeInfo(int paramId) {
        TextBubbleInfo textBubbleInfo = this.mTextBubbleInfo;
        if (textBubbleInfo != null) {
            return textBubbleInfo.getTextStrokeInfo(paramId);
        }
        return null;
    }

    public int getTextAlignment(int paramId) {
        TextBubbleInfo textBubbleInfo = this.mTextBubbleInfo;
        if (textBubbleInfo != null) {
            return textBubbleInfo.getTextAlignment(paramId);
        }
        return 0;
    }

    public QEffectTextAdvStyle.TextBoardConfig getTextBoardConfig(int paramId) {
        TextBubbleInfo textBubbleInfo = this.mTextBubbleInfo;
        if (textBubbleInfo != null) {
            return textBubbleInfo.getTextBoardConfig(paramId);
        }
        return null;
    }

    public TextBubbleInfo.TextBubble getTextBubble(int paramId) {
        TextBubbleInfo textBubbleInfo = this.mTextBubbleInfo;
        if (textBubbleInfo != null) {
            return textBubbleInfo.getDftTextBubble(paramId);
        }
        return null;
    }

    public String getTextBubbleTemplateTextContent(int paramId) {
        TextBubbleInfo textBubbleInfo = this.mTextBubbleInfo;
        return textBubbleInfo != null ? textBubbleInfo.getTemplateTextContent(paramId) : "";
    }

    public String getTextBubbleText(int paramId) {
        TextBubbleInfo textBubbleInfo = this.mTextBubbleInfo;
        return textBubbleInfo != null ? textBubbleInfo.getText(paramId) : "";
    }

    public int getTextColor(int paramId) {
        TextBubbleInfo textBubbleInfo = this.mTextBubbleInfo;
        if (textBubbleInfo != null) {
            return textBubbleInfo.getTextColor(paramId);
        }
        return -1;
    }

    public int getTextDftColor(int paramId) {
        TextBubbleInfo textBubbleInfo = this.mTextBubbleInfo;
        if (textBubbleInfo != null) {
            return textBubbleInfo.getTextDftColor(paramId);
        }
        return -1;
    }

    public String getTextFontPath(int paramId) {
        TextBubbleInfo textBubbleInfo = this.mTextBubbleInfo;
        return textBubbleInfo != null ? textBubbleInfo.getFontPath(paramId) : "";
    }

    public QTransformInfo getTransformInfo() {
        return this.mTransformInfo;
    }

    public boolean isAnimOn() {
        TextBubbleInfo textBubbleInfo = this.mTextBubbleInfo;
        if (textBubbleInfo != null) {
            return textBubbleInfo.isAnimOn();
        }
        return false;
    }

    public boolean isSupportAnim() {
        TextBubbleInfo textBubbleInfo = this.mTextBubbleInfo;
        if (textBubbleInfo != null) {
            return textBubbleInfo.isSupportAnim();
        }
        return false;
    }

    public void setAdvStyle(int paramId, QEffectTextAdvStyle advStyle) {
        TextBubbleInfo textBubbleInfo = this.mTextBubbleInfo;
        if (textBubbleInfo != null) {
            textBubbleInfo.setAdvStyle(paramId, advStyle);
        }
    }

    public void setAnchorForEngine(Ve3DDataF anchorForEngine) {
        this.anchorForEngine = anchorForEngine;
    }

    public void setAnchorOffset(Ve3DDataF anchorOffset) {
        this.anchorOffset = anchorOffset;
    }

    public void setAnimOn(boolean animOn) {
        TextBubbleInfo textBubbleInfo = this.mTextBubbleInfo;
        if (textBubbleInfo != null) {
            textBubbleInfo.setAnimOn(animOn);
        }
    }

    public void setCrop(VideoSpec mCrop) {
        this.mCrop = mCrop;
    }

    public void setFontPath(int paramId, String path) {
        TextBubbleInfo textBubbleInfo = this.mTextBubbleInfo;
        if (textBubbleInfo != null) {
            textBubbleInfo.setFontPath(paramId, path);
        }
    }

    public void setHorFlip(boolean isHorFlip) {
        this.isHorFlip = isHorFlip;
    }

    public void setShadowInfo(int paramId, ShadowInfo shadowInfo) {
        TextBubbleInfo textBubbleInfo = this.mTextBubbleInfo;
        if (textBubbleInfo != null) {
            textBubbleInfo.setTextShadowInfo(paramId, shadowInfo);
        }
    }

    public void setStrokeInfo(int paramId, StrokeInfo strokeInfo) {
        TextBubbleInfo textBubbleInfo = this.mTextBubbleInfo;
        if (textBubbleInfo != null) {
            textBubbleInfo.setTextStrokeInfo(paramId, strokeInfo);
        }
    }

    public void setSupportAnim(boolean bSupportAnim) {
        TextBubbleInfo textBubbleInfo = this.mTextBubbleInfo;
        if (textBubbleInfo != null) {
            textBubbleInfo.setSupportAnim(bSupportAnim);
        }
    }

    public void setTextBubbleText(int paramId, String text) {
        TextBubbleInfo textBubbleInfo = this.mTextBubbleInfo;
        if (textBubbleInfo != null) {
            textBubbleInfo.setText(paramId, text);
        }
    }

    public void setTextColor(int paramId, int color) {
        TextBubbleInfo textBubbleInfo = this.mTextBubbleInfo;
        if (textBubbleInfo != null) {
            textBubbleInfo.setTextColor(paramId, color);
        }
    }

    public void setTransformInfo(QTransformInfo mTransformInfo) {
        this.mTransformInfo = mTransformInfo;
    }

    public void setVerFlip(boolean isVerFlip) {
        this.isVerFlip = isVerFlip;
    }

    @NonNull
    /* JADX INFO: renamed from: clone, reason: merged with bridge method [inline-methods] */
    public ScaleRotateViewState m178763clone() throws CloneNotSupportedException {
        ScaleRotateViewState scaleRotateViewState = (ScaleRotateViewState) super.clone();
        TextBubbleInfo textBubbleInfo = this.mTextBubbleInfo;
        if (textBubbleInfo != null) {
            scaleRotateViewState.mTextBubbleInfo = textBubbleInfo.m178764clone();
        }
        StylePositionModel stylePositionModel = this.mPosInfo;
        if (stylePositionModel != null) {
            scaleRotateViewState.mPosInfo = (StylePositionModel) stylePositionModel.clone();
        }
        QTransformInfo qTransformInfo = this.mTransformInfo;
        if (qTransformInfo != null) {
            scaleRotateViewState.mTransformInfo = cloneTransformInfo(qTransformInfo);
        }
        return scaleRotateViewState;
    }

    private static QTransformInfo cloneTransformInfo(QTransformInfo info) {
        if (info == null) return null;
        QTransformInfo copy = new QTransformInfo();
        copy.mScaleX = info.mScaleX;
        copy.mScaleY = info.mScaleY;
        copy.mScaleZ = info.mScaleZ;
        copy.mShiftX = info.mShiftX;
        copy.mShiftY = info.mShiftY;
        copy.mShiftZ = info.mShiftZ;
        copy.mAngleX = info.mAngleX;
        copy.mAngleY = info.mAngleY;
        copy.mAngleZ = info.mAngleZ;
        return copy;
    }

    public ScaleRotateViewState(ScaleRotateViewState state) {
        this.mVersion = 0;
        this.groupID = 0;
        this.bNeedTranslate = false;
        this.mTextBubbleInfo = new TextBubbleInfo();
        this.mPosInfo = new StylePositionModel();
        this.mDegree = 0.0f;
        this.maxCharCount = 10;
        this.mFrameWidth = 0.0f;
        this.mFrameHeight = 0.0f;
        this.mExampleThumbPos = 0;
        this.mMinDuration = 0;
        this.mBitmap = null;
        this.isDftTemplate = false;
        this.mLineNum = 1;
        this.mStylePath = "";
        this.isHorFlip = false;
        this.isVerFlip = false;
        this.mAlpha = 1.0f;
        this.mActRelativeRect = null;
        this.mPadding = 10;
        this.mStrokeWidth = 3.0f;
        this.mOutlineEllipse = 12;
        this.mOutlineStrokeColor = -34994;
        if (state == null) {
            return;
        }
        this.mVersion = state.mVersion;
        this.groupID = state.groupID;
        this.bNeedTranslate = state.bNeedTranslate;
        this.mTextBubbleInfo.save(state.mTextBubbleInfo);
        this.mPadding = state.mPadding;
        this.mPosInfo = new StylePositionModel(state.mPosInfo.getmCenterPosX(), state.mPosInfo.getmCenterPosY(), state.mPosInfo.getmWidth(), state.mPosInfo.getmHeight());
        this.mDegree = state.mDegree;
        this.mStrokeWidth = state.mStrokeWidth;
        this.mOutlineEllipse = state.mOutlineEllipse;
        this.mOutlineStrokeColor = state.mOutlineStrokeColor;
        this.maxCharCount = state.maxCharCount;
        this.mFrameWidth = state.mFrameWidth;
        this.mFrameHeight = state.mFrameHeight;
        this.mExampleThumbPos = state.mExampleThumbPos;
        this.mMinDuration = state.mMinDuration;
        this.mViewRect = state.mViewRect;
        this.isDftTemplate = state.isDftTemplate;
        this.mLineNum = state.mLineNum;
        this.mStylePath = state.mStylePath;
        this.isHorFlip = state.isHorFlip;
        this.isVerFlip = state.isVerFlip;
        this.mActRelativeRect = state.mActRelativeRect;
        this.mAlpha = state.mAlpha;
        this.mCrop = state.mCrop != null ? new com.quvideo.xiaoying.sdk.editor.cache.VideoSpec(state.mCrop) : null;
        this.mTransformInfo = cloneTransformInfo(state.mTransformInfo);
        if (state.anchorForEngine != null) {
            this.anchorForEngine = new Ve3DDataF(state.anchorForEngine);
        }
        if (state.anchorOffset != null) {
            this.anchorOffset = new Ve3DDataF(state.anchorOffset);
        }
    }
}
