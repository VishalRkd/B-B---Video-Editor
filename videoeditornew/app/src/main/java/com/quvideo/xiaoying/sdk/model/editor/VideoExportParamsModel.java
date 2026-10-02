package com.quvideo.xiaoying.sdk.model.editor;

import android.graphics.Rect;
import com.quvideo.xiaoying.sdk.model.GifExpModel;
import com.quvideo.xiaoying.sdk.model.VeRange;
import com.quvideo.xiaoying.sdk.utils.VeMSize;

/* JADX INFO: loaded from: classes15.dex */
public class VideoExportParamsModel {
    public static final int ACTION_TYPE_CONVERT = 2;
    public static final int ACTION_TYPE_DIREC_AUDIO = 3;
    public static final int ACTION_TYPE_NORMAL = 1;
    public static final int ACTION_TYPE_REVERSE = 4;
    public static final int VIDEO_EXP_TYPE_1080P = 2;
    public static final int VIDEO_EXP_TYPE_720P = 1;
    public static final int VIDEO_EXP_TYPE_GIF = 3;
    public static final int VIDEO_EXP_TYPE_NORMAL = 0;
    public static final int VIDEO_EXP_TYPE_QHD = 4;
    public static final int VIDEO_EXP_TYPE_UHD = 5;
    public static final int VIDEO_EXP_TYPE_VVC = 50;
    public static final int VIDEO_EXP_TYPE_WEBP = 6;
    public String assignedPath;
    public boolean bHDExport;
    public GifExpModel gifParam;
    public boolean isBlack;
    public boolean isCreatorExport;
    public boolean isExportFitOut;
    public boolean isSlidePrj;
    public VeRange mExportVeRange;
    public String mPrjPath;
    public VeMSize mStreamSizeVe;
    public int actionType = 1;
    public boolean isMvPrj = true;
    public boolean bTransitionStatic = false;
    public Rect mCropRegion = null;
    public boolean bNeedUpdatePathToPrj = true;
    public boolean bShowWaterMark = true;
    public Long mWaterMarkTemplateId = 0L;
    public String username = "";
    public String auid = "";
    public String duid = "";
    public boolean bShowNicknameInWaterMark = true;
    public Integer expType = 0;
    public boolean isSingleHW = false;
    public int decodeType = 2;
    public int encodeType = 512;
    public float videoBitrateScales = 1.0f;
    public long videoBitrate = 0;
    public int fps = -1;
    public boolean isExportLocal = false;
    public boolean isForceSWExp = false;
    public boolean isTemplateExport = false;
    public boolean isMetaTemplate = false;

    public boolean is2k4kExp() {
        return this.expType.intValue() == 4 || this.expType.intValue() == 5;
    }

    public boolean isAudioExp() {
        return this.actionType == 3;
    }

    public boolean isConvert() {
        return this.actionType == 2;
    }

    public boolean isExport() {
        return this.actionType == 1;
    }

    public boolean isGifExp() {
        return this.expType.intValue() == 3;
    }

    public boolean isResvert() {
        return this.actionType == 4;
    }

    public boolean isWebpExp() {
        return this.expType.intValue() == 6;
    }

    public String toString() {
        return "VideoExportParamsModel{assignedPath='" + this.assignedPath + "', mPrjPath='" + this.mPrjPath + "', bHDExport=" + this.bHDExport + ", mStreamSizeVe=" + this.mStreamSizeVe + ", isBlack=" + this.isBlack + ", isMvPrj=" + this.isMvPrj + ", mExportVeRange=" + this.mExportVeRange + ", bNeedUpdatePathToPrj=" + this.bNeedUpdatePathToPrj + ", mCropRegion=" + this.mCropRegion + ", bShowWaterMark=" + this.bShowWaterMark + ", mWaterMarkTemplateId=" + this.mWaterMarkTemplateId + ", username='" + this.username + "', auid='" + this.auid + "', duid='" + this.duid + "', bShowNicknameInWaterMark=" + this.bShowNicknameInWaterMark + ", expType=" + this.expType + ", isSingleHW=" + this.isSingleHW + ", gifParam=" + this.gifParam + ", decodeType=" + this.decodeType + ", encodeType=" + this.encodeType + ", isSlidePrj=" + this.isSlidePrj + '}';
    }
}
