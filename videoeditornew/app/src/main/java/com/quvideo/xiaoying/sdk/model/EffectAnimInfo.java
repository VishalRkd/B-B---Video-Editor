package com.quvideo.xiaoying.sdk.model;

import androidx.annotation.NonNull;

/* JADX INFO: loaded from: classes15.dex */
public class EffectAnimInfo implements Cloneable {
    private String animationPath = "";
    private int animationDuration = 0;

    @NonNull
    public Object clone() throws CloneNotSupportedException {
        return super.clone();
    }

    public int getAnimationDuration() {
        return this.animationDuration;
    }

    public String getAnimationPath() {
        return this.animationPath;
    }

    public void setAnimationDuration(int animationDuration) {
        this.animationDuration = animationDuration;
    }

    public void setAnimationPath(String animationPath) {
        this.animationPath = animationPath;
    }
}
