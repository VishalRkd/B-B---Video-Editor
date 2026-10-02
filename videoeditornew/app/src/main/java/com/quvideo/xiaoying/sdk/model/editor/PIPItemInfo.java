package com.quvideo.xiaoying.sdk.model.editor;

import com.quvideo.xiaoying.sdk.model.VeRange;
import xiaoying.engine.clip.QClip;

/* JADX INFO: loaded from: classes15.dex */
public class PIPItemInfo {
    private QClip mClip;
    private int mItemIndex = -1;
    private int mSrcDuration;
    private VeRange mVeRange;

    public QClip getmClip() {
        return this.mClip;
    }

    public int getmItemIndex() {
        return this.mItemIndex;
    }

    public VeRange getmRange() {
        return this.mVeRange;
    }

    public int getmSrcDuration() {
        return this.mSrcDuration;
    }

    public void setmClip(QClip mClip) {
        this.mClip = mClip;
    }

    public void setmItemIndex(int mItemIndex) {
        this.mItemIndex = mItemIndex;
    }

    public void setmRange(VeRange mVeRange) {
        this.mVeRange = mVeRange;
    }

    public void setmSrcDuration(int mSrcDuration) {
        this.mSrcDuration = mSrcDuration;
    }
}
