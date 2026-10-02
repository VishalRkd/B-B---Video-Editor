package com.quvideo.xiaoying.sdk.model.editor;

import android.graphics.Bitmap;
import com.quvideo.xiaoying.sdk.model.template.TemplateInfo;

/* JADX INFO: loaded from: classes15.dex */
public class StoryBoardItemInfo {
    public boolean bSyncThumb;
    public Bitmap bmpThumbnail;
    public String coverUrl;
    public boolean isGif;
    public boolean isVideo;
    public long lDuration;
    public long lTemplateId;
    public EffectInfoModel mEffectInfo;
    public long lTransDuration = -1;
    public float mRotation = 0.0f;
    public String mFontName = "";
    public String mFontPath = "";
    public boolean bAudioEnable = false;
    public boolean bAudioOn = false;
    public TemplateInfo mFontInfo = null;
    public int orderNo = 0;
}
