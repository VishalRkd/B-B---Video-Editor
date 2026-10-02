package com.quvideo.xiaoying.sdk.editor.cache.keyframe;

import androidx.annotation.Keep;
import kotlin.Metadata;

/* JADX INFO: loaded from: classes15.dex */
@Keep
public final class ScaleModel extends BaseKeyFrameModel {
    private float heightRatio;
    private float offsetHeightRatio;
    private float offsetScaleZ;
    private float offsetWidthRatio;
    private float scaleZ;
    private float widthRatio;

    public ScaleModel(int i10, int i11, float f10, float f11) {
        super(i10, i11, KeyFrameType.SCALE, 0, null, 24, null);
        this.widthRatio = f10;
        this.heightRatio = f11;
        this.offsetWidthRatio = 1.0f;
        this.offsetHeightRatio = 1.0f;
        this.scaleZ = 1.0f;
        this.offsetScaleZ = 1.0f;
    }

    public final float getHeightRatio() {
        return this.heightRatio;
    }

    public final float getOffsetHeightRatio() {
        return this.offsetHeightRatio;
    }

    public final float getOffsetScaleZ() {
        return this.offsetScaleZ;
    }

    public final float getOffsetWidthRatio() {
        return this.offsetWidthRatio;
    }

    public final float getScaleZ() {
        return this.scaleZ;
    }

    public final float getWidthRatio() {
        return this.widthRatio;
    }

    public final void setHeightRatio(float f10) {
        this.heightRatio = f10;
    }

    public final void setOffsetHeightRatio(float f10) {
        this.offsetHeightRatio = f10;
    }

    public final void setOffsetScaleZ(float f10) {
        this.offsetScaleZ = f10;
    }

    public final void setOffsetWidthRatio(float f10) {
        this.offsetWidthRatio = f10;
    }

    public final void setScaleZ(float f10) {
        this.scaleZ = f10;
    }

    public final void setWidthRatio(float f10) {
        this.widthRatio = f10;
    }
}
