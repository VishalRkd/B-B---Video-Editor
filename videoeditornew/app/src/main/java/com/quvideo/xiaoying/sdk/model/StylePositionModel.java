package com.quvideo.xiaoying.sdk.model;

import android.graphics.RectF;
import java.io.Serializable;

/* JADX INFO: loaded from: classes15.dex */
public class StylePositionModel implements Serializable, Cloneable {
    private static final long serialVersionUID = -963026009066300819L;
    private float mCenterPosX;
    private float mCenterPosY;
    private float mHeight;
    private float mWidth;

    public StylePositionModel() {
        this.mCenterPosX = 0.0f;
        this.mCenterPosY = 0.0f;
        this.mWidth = 0.0f;
        this.mHeight = 0.0f;
    }

    public Object clone() throws CloneNotSupportedException {
        return super.clone();
    }

    public boolean equals(Object o10) {
        if (this == o10) {
            return true;
        }
        if (!(o10 instanceof StylePositionModel)) {
            return false;
        }
        StylePositionModel stylePositionModel = (StylePositionModel) o10;
        if (Float.compare(stylePositionModel.mCenterPosX, this.mCenterPosX) == 0 && Float.compare(stylePositionModel.mCenterPosY, this.mCenterPosY) == 0 && Float.compare(stylePositionModel.mWidth, this.mWidth) == 0 && Float.compare(stylePositionModel.mHeight, this.mHeight) == 0) {
            return true;
        }
        return false;
    }

    public RectF getRectArea() {
        RectF rectF = new RectF();
        float f10 = this.mCenterPosX;
        float f11 = this.mWidth;
        float f12 = this.mCenterPosY;
        float f13 = this.mHeight;
        rectF.set(f10 - (f11 / 2.0f), f12 - (f13 / 2.0f), f10 + (f11 / 2.0f), f12 + (f13 / 2.0f));
        return rectF;
    }

    public float getmCenterPosX() {
        return this.mCenterPosX;
    }

    public float getmCenterPosY() {
        return this.mCenterPosY;
    }

    public float getmHeight() {
        return this.mHeight;
    }

    public float getmWidth() {
        return this.mWidth;
    }

    public int hashCode() {
        float f10 = this.mCenterPosX;
        int iFloatToIntBits = 0;
        int iFloatToIntBits2 = (f10 != 0.0f ? Float.floatToIntBits(f10) : 0) * 31;
        float f11 = this.mCenterPosY;
        int iFloatToIntBits3 = (iFloatToIntBits2 + (f11 != 0.0f ? Float.floatToIntBits(f11) : 0)) * 31;
        float f12 = this.mWidth;
        int iFloatToIntBits4 = (iFloatToIntBits3 + (f12 != 0.0f ? Float.floatToIntBits(f12) : 0)) * 31;
        float f13 = this.mHeight;
        if (f13 != 0.0f) {
            iFloatToIntBits = Float.floatToIntBits(f13);
        }
        return iFloatToIntBits4 + iFloatToIntBits;
    }

    public void setmCenterPosX(float mCenterPosX) {
        this.mCenterPosX = mCenterPosX;
    }

    public void setmCenterPosY(float mCenterPosY) {
        this.mCenterPosY = mCenterPosY;
    }

    public void setmHeight(float mHeight) {
        this.mHeight = mHeight;
    }

    public void setmWidth(float mWidth) {
        this.mWidth = mWidth;
    }

    public String toString() {
        return "mCenterPosX=" + this.mCenterPosX + ";mCenterPosY=" + this.mCenterPosY + ";mWidth=" + this.mWidth + ";mHeight=" + this.mHeight;
    }

    public StylePositionModel(StylePositionModel model) {
        this.mCenterPosX = 0.0f;
        this.mCenterPosY = 0.0f;
        this.mWidth = 0.0f;
        this.mHeight = 0.0f;
        if (model == null) {
            return;
        }
        this.mCenterPosX = model.mCenterPosX;
        this.mCenterPosY = model.mCenterPosY;
        this.mWidth = model.mWidth;
        this.mHeight = model.mHeight;
    }

    public StylePositionModel(float mCenterPosX, float mCenterPosY, float mWidth, float mHeight) {
        this.mCenterPosX = mCenterPosX;
        this.mCenterPosY = mCenterPosY;
        this.mWidth = mWidth;
        this.mHeight = mHeight;
    }
}
