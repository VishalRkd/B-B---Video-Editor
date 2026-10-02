package xiaoying.engine.base;

import xiaoying.engine.QEngine;
import xiaoying.utils.QRect;
import xiaoying.utils.QSize;

/* JADX INFO: loaded from: classes19.dex */
public class QWatermark {
    private long handle = 0;

    private native int nativeClose();

    private native String nativeGetTitle(int index);

    private native int nativeGetTitleCount();

    private native int nativeOpen(QEngine engine, long llTeamplte, QRect displayRect, QSize ViewSize);

    private native int nativeSetImage(String imgFile);

    private native int nativeSetTitle(int index, String title);

    public int close() {
        return nativeClose();
    }

    public String getTitle(int index) {
        return nativeGetTitle(index);
    }

    public int getTitleCount() {
        return nativeGetTitleCount();
    }

    public int open(QEngine engine, long llTeamplte, QRect displayRect, QSize ViewSize) {
        return nativeOpen(engine, llTeamplte, displayRect, ViewSize);
    }

    public int setImage(String imgFile) {
        return nativeSetImage(imgFile);
    }

    public int setTitle(int index, String title) {
        return nativeSetTitle(index, title);
    }
}
