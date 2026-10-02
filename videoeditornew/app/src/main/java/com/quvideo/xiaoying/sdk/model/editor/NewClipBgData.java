package com.quvideo.xiaoying.sdk.model.editor;

import androidx.annotation.Keep;
import java.io.Serializable;
import java.util.Arrays;
import java.util.Objects;

/* JADX INFO: loaded from: classes15.dex */
@Keep
public class NewClipBgData implements Serializable, Cloneable {
    private static final long serialVersionUID = 3058920962789833895L;
    public int blurLen;
    public ClipBgType clipBgType;
    public int colorAngle;
    public int[] colorArray;
    public String imagePath;
    public float scale;

    @Keep
    public enum ClipBgType {
        BLUR,
        COLOR,
        PICTURE
    }

    public NewClipBgData() {
        this.clipBgType = ClipBgType.COLOR;
        this.blurLen = 0;
        this.imagePath = "";
    }

    public boolean equals(Object o10) {
        if (this == o10) {
            return true;
        }
        if (!(o10 instanceof NewClipBgData)) {
            return false;
        }
        NewClipBgData newClipBgData = (NewClipBgData) o10;
        return this.colorAngle == newClipBgData.colorAngle && this.blurLen == newClipBgData.blurLen && Float.compare(newClipBgData.scale, this.scale) == 0 && getClipBgType() == newClipBgData.getClipBgType() && Arrays.equals(this.colorArray, newClipBgData.colorArray) && getImagePath().equals(newClipBgData.getImagePath());
    }

    public ClipBgType getClipBgType() {
        return this.clipBgType;
    }

    public String getImagePath() {
        return this.imagePath;
    }

    public int hashCode() {
        return (Objects.hash(getClipBgType(), Integer.valueOf(this.colorAngle), Integer.valueOf(this.blurLen), getImagePath(), Float.valueOf(this.scale)) * 31) + Arrays.hashCode(this.colorArray);
    }

    public String toString() {
        return "NewClipBgData{clipBgType=" + this.clipBgType + ", colorArray=" + Arrays.toString(this.colorArray) + ", colorAngle=" + this.colorAngle + ", blurLen=" + this.blurLen + ", imagePath='" + this.imagePath + "', scale=" + this.scale + '}';
    }

    /* JADX INFO: renamed from: clone, reason: merged with bridge method [inline-methods] */
    public NewClipBgData m178761clone() throws CloneNotSupportedException {
        NewClipBgData newClipBgData = (NewClipBgData) super.clone();
        newClipBgData.clipBgType = this.clipBgType;
        int[] iArr = this.colorArray;
        if (iArr != null) {
            int length = iArr.length;
            int[] iArr2 = new int[length];
            for (int i10 = 0; i10 < length; i10++) {
                iArr2[i10] = this.colorArray[i10];
            }
            newClipBgData.colorArray = iArr2;
        }
        return newClipBgData;
    }

    public NewClipBgData(int[] colorArray, int colorAngle) {
        ClipBgType clipBgType = ClipBgType.COLOR;
        this.blurLen = 0;
        this.imagePath = "";
        this.clipBgType = clipBgType;
        this.colorArray = colorArray;
        this.colorAngle = colorAngle;
    }
}
