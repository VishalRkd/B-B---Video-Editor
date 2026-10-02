package com.quvideo.xiaoying.sdk.model.editor;

import android.graphics.Rect;
import xiaoying.utils.ITRCLyricsDecryptListener;

/* JADX INFO: loaded from: classes15.dex */
public class DataLyricsItem {
    public static String FONT_FAMILY_FILE = "/system/fonts/DroidSansFallback.ttf";
    public ITRCLyricsDecryptListener decryptor;
    public String strLrcFontFile = FONT_FAMILY_FILE;
    public String strLrcTRCFile = "";
    public int nLrcBgColor = 16777215;
    public int nLrcForeColor = 0;
    public int nLrcStartPos = 0;
    public int nLrcLength = 240000;
    public int nDstStartPos = 0;
    public Rect rect = new Rect(1250, 9050, 8750, 9800);
}
