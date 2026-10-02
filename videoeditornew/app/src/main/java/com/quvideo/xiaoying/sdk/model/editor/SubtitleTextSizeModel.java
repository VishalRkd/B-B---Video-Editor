package com.quvideo.xiaoying.sdk.model.editor;

import java.util.Objects;

/* JADX INFO: loaded from: classes15.dex */
public class SubtitleTextSizeModel implements Cloneable {
    public int currentSize;
    public int initHeight;
    public int initWidth;

    public Object clone() throws CloneNotSupportedException {
        return super.clone();
    }

    public boolean equals(Object o10) {
        if (this == o10) {
            return true;
        }
        if (!(o10 instanceof SubtitleTextSizeModel)) {
            return false;
        }
        SubtitleTextSizeModel subtitleTextSizeModel = (SubtitleTextSizeModel) o10;
        return this.currentSize == subtitleTextSizeModel.currentSize && this.initWidth == subtitleTextSizeModel.initWidth && this.initHeight == subtitleTextSizeModel.initHeight;
    }

    public int hashCode() {
        return Objects.hash(Integer.valueOf(this.currentSize), Integer.valueOf(this.initWidth), Integer.valueOf(this.initHeight));
    }
}
