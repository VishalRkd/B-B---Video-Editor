package com.quvideo.xiaoying.sdk.model.editor;

import android.graphics.Rect;
import android.graphics.RectF;
import com.quvideo.xiaoying.sdk.model.SubtitleAnim;
import com.quvideo.xiaoying.sdk.model.Ve3DDataF;
import java.util.ArrayList;
import java.util.List;
import xiaoying.engine.base.QRange;
import xiaoying.engine.clip.QEffect;
import xiaoying.engine.clip.QEffectTextAdvStyle;

public class TextEffectParams {
    private Ve3DDataF mAnchor;
    private float mAngle;
    private String mEffectStylePath;
    public static class MosaicBlurLevel implements Cloneable {
        public int x;
        public int y;
        public MosaicBlurLevel() {}
        public MosaicBlurLevel(int x, int y) { this.x = x; this.y = y; }
    }
    private MosaicBlurLevel mMosaicBlurLevel;
    private Rect mResultRect;
    private long mTemplateId;
    private int mTextRangeLen;
    private int mTextRangeStart;
    private RectF mTextRect;
    private int mVersion;
    private boolean isApplyInWholeClip = false;
    private boolean isAnimOn = false;
    private int nClipIndex = 0;
    private float mAlpha = 1.0f;
    private boolean isVerFlip = false;
    private boolean isHorFlip = false;
    private int mStyleDuration = 0;
    public float mLayerID = 0.0f;
    public boolean bShowStaticPicture = false;
    public List<TextBubbleParams> textBubbleParamsList = new ArrayList();

    public static class TextBubbleParams {
        public QEffectTextAdvStyle advStyle;

        /* JADX INFO: renamed from: in */
        public SubtitleAnim f94054in;
        private int mParentParamID;
        private ShadowInfo mShadowInfo;
        private StrokeInfo mStrokeInfo;
        private RectF mTextRegion;
        private int mTxtColor;
        private String mTxtContent;
        public SubtitleAnim out;
        public SubtitleAnim repeat;
        public QEffectTextAdvStyle.TextBoardConfig textBoardConfig;
        private String mFontPath = "";
        private int mTextAlignment = 0;
        public float mWordSpace = 0.0f;
        public float mLineSpace = 0.0f;
        public float mFontSize = 0.0f;
        private int mParamId = 0;

        public SubtitleAnim getIn() {
            return this.f94054in;
        }

        public SubtitleAnim getOut() {
            return this.out;
        }

        public int getParentParamID() {
            return this.mParentParamID;
        }

        public SubtitleAnim getRepeat() {
            return this.repeat;
        }

        public String getmFontPath() {
            return this.mFontPath;
        }

        public float getmFontSize() {
            return this.mFontSize;
        }

        public float getmLineSpace() {
            return this.mLineSpace;
        }

        public int getmParamId() {
            return this.mParamId;
        }

        public ShadowInfo getmShadowInfo() {
            return this.mShadowInfo;
        }

        public StrokeInfo getmStrokeInfo() {
            return this.mStrokeInfo;
        }

        public int getmTextAlignment() {
            return this.mTextAlignment;
        }

        public RectF getmTextRegion() {
            return this.mTextRegion;
        }

        public int getmTxtColor() {
            return this.mTxtColor;
        }

        public String getmTxtContent() {
            return this.mTxtContent;
        }

        public float getmWordSpace() {
            return this.mWordSpace;
        }

        public void setIn(SubtitleAnim in2) {
            this.f94054in = in2;
        }

        public void setOut(SubtitleAnim out) {
            this.out = out;
        }

        public void setParentParamID(int mParentParamID) {
            this.mParentParamID = mParentParamID;
        }

        public void setRepeat(SubtitleAnim repeat) {
            this.repeat = repeat;
        }

        public void setmFontPath(String mFontPath) {
            if (mFontPath == null) {
                this.mFontPath = "";
            } else {
                this.mFontPath = mFontPath;
            }
        }

        public void setmFontSize(float mFontSize) {
            this.mFontSize = mFontSize;
        }

        public void setmLineSpace(float mLineSpace) {
            this.mLineSpace = mLineSpace;
        }

        public void setmParamId(int mParamId) {
            this.mParamId = mParamId;
        }

        public void setmShadowInfo(ShadowInfo mShadowInfo) {
            this.mShadowInfo = mShadowInfo;
        }

        public void setmStrokeInfo(StrokeInfo mStrokeInfo) {
            this.mStrokeInfo = mStrokeInfo;
        }

        public void setmTextAlignment(int mTextAlignment) {
            this.mTextAlignment = mTextAlignment;
        }

        public void setmTextRegion(RectF mTextRegion) {
            this.mTextRegion = mTextRegion;
        }

        public void setmTxtColor(int mTxtColor) {
            this.mTxtColor = mTxtColor;
        }

        public void setmTxtContent(String mTxtContent) {
            this.mTxtContent = mTxtContent;
        }

        public void setmWordSpace(float mWordSpace) {
            this.mWordSpace = mWordSpace;
        }
    }

    public int getClipIndex() {
        return this.nClipIndex;
    }

    public void getEffectRange(QEffect effect) {
        QRange qRange = (QRange) effect.getProperty(4098);
        if (qRange != null) {
            int i10 = qRange.get(0);
            int i11 = qRange.get(1);
            setmTextRangeStart(i10);
            setmTextRangeLen(i11);
        }
    }

    public MosaicBlurLevel getMosaicBlurLevel() {
        return this.mMosaicBlurLevel;
    }

    public int getVersion() {
        return this.mVersion;
    }

    public float getmAlpha() {
        return this.mAlpha;
    }

    public Ve3DDataF getmAnchor() {
        return this.mAnchor;
    }

    public float getmAngle() {
        return this.mAngle;
    }

    public String getmEffectStylePath() {
        return this.mEffectStylePath;
    }

    public Rect getmResultRect() {
        return this.mResultRect;
    }

    public int getmStyleDuration() {
        return this.mStyleDuration;
    }

    public long getmTemplateId() {
        return this.mTemplateId;
    }

    public int getmTextRangeLen() {
        return this.mTextRangeLen;
    }

    public int getmTextRangeStart() {
        return this.mTextRangeStart;
    }

    public RectF getmTextRect() {
        return this.mTextRect;
    }

    public boolean isAnimOn() {
        return this.isAnimOn;
    }

    public boolean isApplyInWholeClip() {
        return this.isApplyInWholeClip;
    }

    public boolean isHorFlip() {
        return this.isHorFlip;
    }

    public boolean isVerFlip() {
        return this.isVerFlip;
    }

    public void setAnimOn(boolean isAnimOn) {
        this.isAnimOn = isAnimOn;
    }

    public void setApplyInWholeClip(boolean isApplyInWholeClip) {
        this.isApplyInWholeClip = isApplyInWholeClip;
    }

    public void setClipIndex(int index) {
        this.nClipIndex = index;
    }

    public void setHorFlip(boolean isHorFlip) {
        this.isHorFlip = isHorFlip;
    }

    public void setMosaicBlurLevel(MosaicBlurLevel mMosaicBlurLevel) {
        this.mMosaicBlurLevel = mMosaicBlurLevel;
    }

    public void setVerFlip(boolean isVerFlip) {
        this.isVerFlip = isVerFlip;
    }

    public void setVersion(int version) {
        this.mVersion = version;
    }

    public void setmAlpha(float mAlpha) {
        this.mAlpha = mAlpha;
    }

    public void setmAnchor(Ve3DDataF mAnchor) {
        this.mAnchor = mAnchor;
    }

    public void setmAngle(float mAngle) {
        this.mAngle = mAngle;
    }

    public void setmEffectStylePath(String mEffectStylePath) {
        this.mEffectStylePath = mEffectStylePath;
    }

    public void setmResultRect(Rect mResultRect) {
        this.mResultRect = mResultRect;
    }

    public void setmStyleDuration(int mStyleDuration) {
        this.mStyleDuration = mStyleDuration;
    }

    public void setmTemplateId(long mTemplateId) {
        this.mTemplateId = mTemplateId;
    }

    public void setmTextRangeLen(int mTextRangeLen) {
        this.mTextRangeLen = mTextRangeLen;
    }

    public void setmTextRangeStart(int mTextRangeStart) {
        this.mTextRangeStart = mTextRangeStart;
    }

    public void setmTextRect(RectF mTextRect) {
        this.mTextRect = mTextRect;
    }
}
