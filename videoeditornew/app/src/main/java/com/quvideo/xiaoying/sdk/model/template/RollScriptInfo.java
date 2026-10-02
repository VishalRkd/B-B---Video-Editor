package com.quvideo.xiaoying.sdk.model.template;

/* JADX INFO: loaded from: classes15.dex */
public class RollScriptInfo implements Cloneable {
    public String rollTitle = "";
    public String rollSimpleIntro = "";
    public String rollDetailIntro = "";
    public String rollCopyRightInfo = "";

    public Object clone() {
        try {
            return (RollScriptInfo) super.clone();
        } catch (CloneNotSupportedException unused) {
            return null;
        }
    }
}
