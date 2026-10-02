package com.quvideo.xiaoying.sdk.model.editor;

/* JADX INFO: loaded from: classes15.dex */
public class ThumbInfo {
    private int position = 0;
    private int duration = 0;

    public int getDuration() {
        return this.duration;
    }

    public int getPosition() {
        return this.position;
    }

    public void setDuration(int duration) {
        this.duration = duration;
    }

    public void setPosition(int position) {
        this.position = position;
    }

    public String toString() {
        StringBuffer stringBuffer = new StringBuffer();
        stringBuffer.append("position=");
        stringBuffer.append(this.position);
        stringBuffer.append(";duration=");
        stringBuffer.append(this.duration);
        return stringBuffer.toString();
    }
}
