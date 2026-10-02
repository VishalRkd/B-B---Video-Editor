package com.quvideo.xiaoying.sdk.model.editor;

import androidx.annotation.NonNull;
import java.io.Serializable;

/* JADX INFO: loaded from: classes15.dex */
public class ShadowInfo implements Serializable, Cloneable {
    private static final long serialVersionUID = -2052704248964566815L;
    private boolean bEnableShadow = false;
    private float mShadowXShift = 0.1f;
    private float mShadowYShift = 0.1f;
    private int mShadowColor = -1442840576;
    private float mShadowBlurRadius = 0.1f;

    @NonNull
    public Object clone() throws CloneNotSupportedException {
        return super.clone();
    }

    public boolean equals(Object o10) {
        if (this == o10) {
            return true;
        }
        if (!(o10 instanceof ShadowInfo)) {
            return false;
        }
        ShadowInfo shadowInfo = (ShadowInfo) o10;
        if (this.bEnableShadow == shadowInfo.bEnableShadow && Float.compare(shadowInfo.mShadowXShift, this.mShadowXShift) == 0 && Float.compare(shadowInfo.mShadowYShift, this.mShadowYShift) == 0 && this.mShadowColor == shadowInfo.mShadowColor && Float.compare(shadowInfo.mShadowBlurRadius, this.mShadowBlurRadius) == 0) {
            return true;
        }
        return false;
    }

    public float getmShadowBlurRadius() {
        return this.mShadowBlurRadius;
    }

    public int getmShadowColor() {
        return this.mShadowColor;
    }

    public float getmShadowXShift() {
        return this.mShadowXShift;
    }

    public float getmShadowYShift() {
        return this.mShadowYShift;
    }

    public int hashCode() {
        int i10 = (this.bEnableShadow ? 1 : 0) * 31;
        float f10 = this.mShadowXShift;
        int iFloatToIntBits = 0;
        int iFloatToIntBits2 = (i10 + (f10 != 0.0f ? Float.floatToIntBits(f10) : 0)) * 31;
        float f11 = this.mShadowYShift;
        int iFloatToIntBits3 = (((iFloatToIntBits2 + (f11 != 0.0f ? Float.floatToIntBits(f11) : 0)) * 31) + this.mShadowColor) * 31;
        float f12 = this.mShadowBlurRadius;
        if (f12 != 0.0f) {
            iFloatToIntBits = Float.floatToIntBits(f12);
        }
        return iFloatToIntBits3 + iFloatToIntBits;
    }

    public boolean isbEnableShadow() {
        return this.bEnableShadow;
    }

    public void save(ShadowInfo info) {
        if (info == null) {
            return;
        }
        this.bEnableShadow = info.bEnableShadow;
        this.mShadowXShift = info.mShadowXShift;
        this.mShadowYShift = info.mShadowYShift;
        this.mShadowColor = info.mShadowColor;
        this.mShadowBlurRadius = info.mShadowBlurRadius;
    }

    public void setbEnableShadow(boolean bEnableShadow) {
        this.bEnableShadow = bEnableShadow;
    }

    public void setmShadowBlurRadius(float mShadowBlurRadius) {
        this.mShadowBlurRadius = mShadowBlurRadius;
    }

    public void setmShadowColor(int mShadowColor) {
        this.mShadowColor = mShadowColor;
    }

    public void setmShadowXShift(float mShadowXShift) {
        this.mShadowXShift = mShadowXShift;
    }

    public void setmShadowYShift(float mShadowYShift) {
        this.mShadowYShift = mShadowYShift;
    }
}
