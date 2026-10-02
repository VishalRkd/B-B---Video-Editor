package com.quvideo.xiaoying.sdk.model.editor;

/* JADX INFO: loaded from: classes15.dex */
public class VideoInfo {
    public int duration;
    public int frameHeight;
    public int frameWidth;
    public int videoBitrate;
    public int videoFrameRate;

    public VideoInfo() {
    }

    public VideoInfo(int frameW, int frameH, int dura, int videoFrameRate, int videoBitrate) {
        this.frameWidth = frameW;
        this.frameHeight = frameH;
        this.duration = dura;
        this.videoFrameRate = videoFrameRate;
        this.videoBitrate = videoBitrate;
    }
}
