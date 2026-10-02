package com.quvideo.xiaoying.sdk.model.editor;

/* JADX INFO: loaded from: classes15.dex */
public class SymbolStringInfo {
    private boolean isSymbolStr = true;
    private int mStartPSPosInOriString = -1;
    private int mEndPSPosInOriString = -1;
    private String mSymbolString = null;

    public int getmEndPSPosInOriString() {
        return this.mEndPSPosInOriString;
    }

    public int getmStartPSPosInOriString() {
        return this.mStartPSPosInOriString;
    }

    public String getmSymbolString() {
        return this.mSymbolString;
    }

    public boolean isSymbolStr() {
        return this.isSymbolStr;
    }

    public void setSymbolStr(boolean isSymbolStr) {
        this.isSymbolStr = isSymbolStr;
    }

    public void setmEndPSPosInOriString(int mEndPSPosInOriString) {
        this.mEndPSPosInOriString = mEndPSPosInOriString;
    }

    public void setmStartPSPosInOriString(int mStartPSPosInOriString) {
        this.mStartPSPosInOriString = mStartPSPosInOriString;
    }

    public void setmSymbolString(String mSymbolString) {
        this.mSymbolString = mSymbolString;
    }
}
