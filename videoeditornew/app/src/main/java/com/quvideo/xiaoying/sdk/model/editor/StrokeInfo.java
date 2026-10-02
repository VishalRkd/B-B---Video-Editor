package com.quvideo.xiaoying.sdk.model.editor;

import androidx.annotation.NonNull;
import java.io.Serializable;

/* JADX INFO: loaded from: classes15.dex */
public class StrokeInfo implements Serializable, Cloneable {
    private static final long serialVersionUID = 787425019132700380L;
    public int strokeColor = -1;
    public float strokeWPersent = 0.0f;

    @NonNull
    public Object clone() throws CloneNotSupportedException {
        return super.clone();
    }

    public boolean equals(Object o10) {
        if (this == o10) {
            return true;
        }
        if (!(o10 instanceof StrokeInfo)) {
            return false;
        }
        StrokeInfo strokeInfo = (StrokeInfo) o10;
        return this.strokeColor == strokeInfo.strokeColor && Float.compare(strokeInfo.strokeWPersent, this.strokeWPersent) == 0;
    }

    public int hashCode() {
        int i10 = this.strokeColor * 31;
        float f10 = this.strokeWPersent;
        return i10 + (f10 != 0.0f ? Float.floatToIntBits(f10) : 0);
    }

    public void save(StrokeInfo info) {
        if (info == null) {
            return;
        }
        this.strokeColor = info.strokeColor;
        this.strokeWPersent = info.strokeWPersent;
    }
}
