package com.quvideo.xiaoying.sdk.editor.cache.keyframe;

import androidx.annotation.Keep;
import kotlin.Metadata;

/* JADX INFO: loaded from: classes15.dex */
@Keep
public final class PositionModel extends BaseKeyFrameModel {
    private int centerX;
    private int centerY;
    private int lineMode;
    private float offsetShiftZ;
    private int offsetX;
    private int offsetY;
    private float shiftZ;

    public PositionModel(int i10, int i11, int i12, int i13) {
        super(i10, i11, KeyFrameType.POSITION, 0, null, 24, null);
        this.centerX = i12;
        this.centerY = i13;
    }

    public final int getCenterX() {
        return this.centerX;
    }

    public final int getCenterY() {
        return this.centerY;
    }

    public final int getLineMode() {
        return this.lineMode;
    }

    public final float getOffsetShiftZ() {
        return this.offsetShiftZ;
    }

    public final int getOffsetX() {
        return this.offsetX;
    }

    public final int getOffsetY() {
        return this.offsetY;
    }

    public final float getShiftZ() {
        return this.shiftZ;
    }

    public final void setCenterX(int i10) {
        this.centerX = i10;
    }

    public final void setCenterY(int i10) {
        this.centerY = i10;
    }

    public final void setLineMode(int i10) {
        this.lineMode = i10;
    }

    public final void setOffsetShiftZ(float f10) {
        this.offsetShiftZ = f10;
    }

    public final void setOffsetX(int i10) {
        this.offsetX = i10;
    }

    public final void setOffsetY(int i10) {
        this.offsetY = i10;
    }

    public final void setShiftZ(float f10) {
        this.shiftZ = f10;
    }
}
