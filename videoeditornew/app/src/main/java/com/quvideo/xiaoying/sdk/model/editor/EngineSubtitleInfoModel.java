package com.quvideo.xiaoying.sdk.model.editor;

import com.quvideo.xiaoying.sdk.model.VeRange;
import java.io.Serializable;

/* JADX INFO: loaded from: classes15.dex */
public class EngineSubtitleInfoModel implements Serializable {
    public VeRange mTimeVeRange = null;
    public String mTemplatePath = "";
    public int mIndex = 0;
    public int mClipIndex = 0;
    public int mGroupType = 0;
    public String mText = "";
    public ScaleRotateViewState textState = null;
    public int mClipStartPos = 0;
    public int groupId = 0;

    public int getEffectLen() {
        VeRange veRange = this.mTimeVeRange;
        if (veRange == null) {
            return 0;
        }
        return veRange.getmTimeLength();
    }

    public int getPreviewTime() {
        VeRange veRange = this.mTimeVeRange;
        return veRange != null ? this.mClipStartPos + veRange.getmPosition() + 1 : this.mClipStartPos;
    }

    public String toString() {
        VeRange veRange = this.mTimeVeRange;
        if (veRange != null) {
            veRange.getmPosition();
        }
        return super.toString();
    }
}
