package com.quvideo.xiaoying.sdk.editor.cache.keyframe;

import androidx.annotation.Keep;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Keep
public final class EffectKeyFrameCollection {
    private List<MaskModel> maskList;
    private ArrayList<OpacityModel> opacityList;
    private ArrayList<PositionModel> positionList;
    private ArrayList<RotationModel> rotationList;
    private ArrayList<ScaleModel> scaleList;

    public EffectKeyFrameCollection(
        ArrayList<PositionModel> positionList,
        ArrayList<RotationModel> rotationList,
        ArrayList<ScaleModel> scaleList,
        ArrayList<OpacityModel> opacityList,
        List<MaskModel> maskList
    ) {
        this.positionList = positionList;
        this.rotationList = rotationList;
        this.scaleList = scaleList;
        this.opacityList = opacityList;
        this.maskList = maskList;
    }

    public List<MaskModel> getMaskList() {
        return this.maskList;
    }

    public void setMaskList(List<MaskModel> maskList) {
        this.maskList = maskList;
    }

    public ArrayList<OpacityModel> getOpacityList() {
        return this.opacityList;
    }

    public void setOpacityList(ArrayList<OpacityModel> opacityList) {
        this.opacityList = opacityList;
    }

    public ArrayList<PositionModel> getPositionList() {
        return this.positionList;
    }

    public void setPositionList(ArrayList<PositionModel> positionList) {
        this.positionList = positionList;
    }

    public ArrayList<RotationModel> getRotationList() {
        return this.rotationList;
    }

    public void setRotationList(ArrayList<RotationModel> rotationList) {
        this.rotationList = rotationList;
    }

    public ArrayList<ScaleModel> getScaleList() {
        return this.scaleList;
    }

    public void setScaleList(ArrayList<ScaleModel> scaleList) {
        this.scaleList = scaleList;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        EffectKeyFrameCollection that = (EffectKeyFrameCollection) o;
        return Objects.equals(maskList, that.maskList) &&
               Objects.equals(opacityList, that.opacityList) &&
               Objects.equals(positionList, that.positionList) &&
               Objects.equals(rotationList, that.rotationList) &&
               Objects.equals(scaleList, that.scaleList);
    }

    @Override
    public int hashCode() {
        return Objects.hash(maskList, opacityList, positionList, rotationList, scaleList);
    }
}
