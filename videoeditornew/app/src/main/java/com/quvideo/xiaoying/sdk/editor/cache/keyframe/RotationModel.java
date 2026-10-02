package com.quvideo.xiaoying.sdk.editor.cache.keyframe;

import androidx.annotation.Keep;
import kotlin.Metadata;
import kotlin.jvm.internal.DefaultConstructorMarker;

/* JADX INFO: loaded from: classes15.dex */
@Keep
public final class RotationModel extends BaseKeyFrameModel {
    private float offsetRotate;
    private float rotation;
    private int rotationType;

    public /* synthetic */ RotationModel(int i10, int i11, float f10, int i12, int i13, DefaultConstructorMarker defaultConstructorMarker) {
        this(i10, i11, f10, (i13 & 8) != 0 ? 0 : i12);
    }

    public final float getOffsetRotate() {
        return this.offsetRotate;
    }

    public final float getRotation() {
        return this.rotation;
    }

    public final int getRotationType() {
        return this.rotationType;
    }

    public final void setOffsetRotate(float f10) {
        this.offsetRotate = f10;
    }

    public final void setRotation(float f10) {
        this.rotation = f10;
    }

    public final void setRotationType(int i10) {
        this.rotationType = i10;
    }

    public RotationModel(int i10, int i11, float f10, int i12) {
        super(i10, i11, KeyFrameType.ROTATION, 0, null, 24, null);
        this.rotation = f10;
        this.rotationType = i12;
    }
}
