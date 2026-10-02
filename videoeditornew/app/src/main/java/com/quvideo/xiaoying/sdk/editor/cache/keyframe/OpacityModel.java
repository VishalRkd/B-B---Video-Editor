package com.quvideo.xiaoying.sdk.editor.cache.keyframe;

import androidx.annotation.Keep;
import kotlin.Metadata;

/* JADX INFO: loaded from: classes15.dex */
@Keep
public final class OpacityModel extends BaseKeyFrameModel {
    private float degree;
    private float offsetOpacity;

    public OpacityModel(int i10, int i11, float f10) {
        super(i10, i11, KeyFrameType.OPACITY, 0, null, 24, null);
        this.degree = f10;
        this.offsetOpacity = 1.0f;
    }

    public final float getDegree() {
        return this.degree;
    }

    public final float getOffsetOpacity() {
        return this.offsetOpacity;
    }

    public final void setDegree(float f10) {
        this.degree = f10;
    }

    public final void setOffsetOpacity(float f10) {
        this.offsetOpacity = f10;
    }
}
