package com.quvideo.xiaoying.sdk.model;

import androidx.annotation.Keep;
import java.io.Serializable;

/* JADX INFO: loaded from: classes15.dex */
@Keep
public class SubtitleAnim implements Serializable, Cloneable {
    private static final long serialVersionUID = 5620694154554304041L;
    public String animPath;
    public int duration;

    public SubtitleAnim() {
        this.duration = -1;
    }

    public SubtitleAnim cloneSafely() {
        try {
            return m178757clone();
        } catch (CloneNotSupportedException e10) {
            e10.printStackTrace();
            return null;
        }
    }

    public String toString() {
        return "SubtitleAnim{animPath=" + this.animPath + ", duration=" + this.duration + '}';
    }

    /* JADX INFO: renamed from: clone, reason: merged with bridge method [inline-methods] */
    public SubtitleAnim m178757clone() throws CloneNotSupportedException {
        return (SubtitleAnim) super.clone();
    }

    public SubtitleAnim(String animPath, int duration) {
        this.animPath = animPath;
        this.duration = duration;
    }
}
