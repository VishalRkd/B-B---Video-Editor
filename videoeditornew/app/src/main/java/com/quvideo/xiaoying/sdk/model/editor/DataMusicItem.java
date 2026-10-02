package com.quvideo.xiaoying.sdk.model.editor;

import java.io.File;

public class DataMusicItem {
    public int currentTimeStamp;
    public String filePath;
    public int startTimeStamp;
    public int stopTimeStamp;
    public String title;

    public int getSrcLen() {
        int i10 = this.stopTimeStamp - this.startTimeStamp;
        if (i10 < 0) {
            i10 = 0;
        }
        return i10;
    }

    public boolean isValidItem() {
        boolean fileExists = filePath != null && new File(filePath).exists();
        int i10 = this.stopTimeStamp;
        int i11 = this.startTimeStamp;
        int i12 = this.currentTimeStamp;
        return fileExists && i10 > i11 && i12 >= i11 && i12 <= i10;
    }
}
