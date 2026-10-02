package com.quvideo.xiaoying.sdk.editor.cache.keyframe;

import androidx.annotation.Keep;
import java.util.ArrayList;
import java.util.List;
import kotlin.Metadata;
import kotlin.jvm.internal.DefaultConstructorMarker;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/* JADX INFO: loaded from: classes15.dex */
@Keep
public final class MaskModel extends BaseKeyFrameModel {

    /* JADX INFO: renamed from: Companion, reason: from kotlin metadata */
    @NotNull
    public static final Companion INSTANCE = new Companion(null);
    private int centerX;
    private int centerY;
    private int radiusX;
    private int radiusY;
    private int reversed;
    private int rotation;
    private int softness;

    /* JADX INFO: renamed from: com.quvideo.xiaoying.sdk.editor.cache.keyframe.MaskModel$a, reason: from kotlin metadata */
    public static final class Companion {
        public /* synthetic */ Companion(DefaultConstructorMarker defaultConstructorMarker) {
            this();
        }

        @NotNull
        /* JADX INFO: renamed from: a */
        public final List<MaskModel> m97137a(@Nullable List<MaskModel> list) {
            if (list == null) {
                return new ArrayList();
            }
            ArrayList arrayList = new ArrayList();
            for (MaskModel maskModel : list) {
                MaskModel maskModel2 = new MaskModel(maskModel.getCurTime(), maskModel.getRelativeTime());
                maskModel2.setReversed(maskModel.getReversed());
                maskModel2.setCenterX(maskModel.getCenterX());
                maskModel2.setCenterY(maskModel.getCenterY());
                maskModel2.setRadiusX(maskModel.getRadiusX());
                maskModel2.setRadiusY(maskModel.getRadiusY());
                maskModel2.setRotation(maskModel.getRotation());
                maskModel2.setSoftness(maskModel.getSoftness());
                arrayList.add(maskModel2);
            }
            return arrayList;
        }

        public Companion() {
        }
    }

    public MaskModel(int i10, int i11) {
        super(i10, i11, KeyFrameType.MASK, 0, null, 24, null);
    }

    public final int getCenterX() {
        return this.centerX;
    }

    public final int getCenterY() {
        return this.centerY;
    }

    public final int getRadiusX() {
        return this.radiusX;
    }

    public final int getRadiusY() {
        return this.radiusY;
    }

    public final int getReversed() {
        return this.reversed;
    }

    public final int getRotation() {
        return this.rotation;
    }

    public final int getSoftness() {
        return this.softness;
    }

    public final void setCenterX(int i10) {
        this.centerX = i10;
    }

    public final void setCenterY(int i10) {
        this.centerY = i10;
    }

    public final void setRadiusX(int i10) {
        this.radiusX = i10;
    }

    public final void setRadiusY(int i10) {
        this.radiusY = i10;
    }

    public final void setReversed(int i10) {
        this.reversed = i10;
    }

    public final void setRotation(int i10) {
        this.rotation = i10;
    }

    public final void setSoftness(int i10) {
        this.softness = i10;
    }
}
