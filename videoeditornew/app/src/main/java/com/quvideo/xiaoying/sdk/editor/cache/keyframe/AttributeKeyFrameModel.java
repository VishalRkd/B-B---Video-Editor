package com.quvideo.xiaoying.sdk.editor.cache.keyframe;

import androidx.annotation.Keep;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@Keep
public class AttributeKeyFrameModel extends BaseKeyFrameModel {

    @NotNull
    private final String attrName;
    private double value;

    public AttributeKeyFrameModel(int i10, int i11, @NotNull String attrName, double d10) {
        super(i10, i11, KeyFrameType.ATTRIBUTE, 0, null, 24, null);
        this.attrName = attrName;
        this.value = d10;
    }

    @Nullable
    public final AttributeKeyFrameModel copy() {
        AttributeKeyFrameModel attributeKeyFrameModel = new AttributeKeyFrameModel(getCurTime(), getRelativeTime(), this.attrName, this.value);
        attributeKeyFrameModel.setEasingInfo(getEasingInfo());
        return attributeKeyFrameModel;
    }

    @NotNull
    public final String getAttrName() {
        return this.attrName;
    }

    public final double getValue() {
        return this.value;
    }

    public final void setValue(double d10) {
        this.value = d10;
    }
}
