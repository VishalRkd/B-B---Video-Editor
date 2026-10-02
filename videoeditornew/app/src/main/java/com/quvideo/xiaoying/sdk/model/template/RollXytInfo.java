package com.quvideo.xiaoying.sdk.model.template;

/* JADX INFO: loaded from: classes15.dex */
public class RollXytInfo implements Cloneable {
    public String mName;
    public String mXytIconUrl;
    public String mXytPreviewUrl;

    public Object clone() {
        try {
            return (RollXytInfo) super.clone();
        } catch (CloneNotSupportedException unused) {
            return null;
        }
    }
}
