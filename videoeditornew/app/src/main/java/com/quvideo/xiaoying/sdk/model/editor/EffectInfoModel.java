package com.quvideo.xiaoying.sdk.model.editor;

import android.os.Parcel;
import android.os.Parcelable;

/* JADX INFO: loaded from: classes15.dex */
public class EffectInfoModel implements Parcelable, Cloneable {
    public static final Parcelable.Creator<EffectInfoModel> CREATOR = new Parcelable.Creator<EffectInfoModel>() { // from class: com.quvideo.xiaoying.sdk.model.editor.EffectInfoModel.1
        /* JADX WARN: Can't rename method to resolve collision */
        @Override // android.os.Parcelable.Creator
        public EffectInfoModel createFromParcel(Parcel source) {
            return new EffectInfoModel(source);
        }

        /* JADX WARN: Can't rename method to resolve collision */
        @Override // android.os.Parcelable.Creator
        public EffectInfoModel[] newArray(int size) {
            EffectInfoModel[] effectInfoModelArr = new EffectInfoModel[size];
            for (int i10 = 0; i10 < size; i10++) {
                effectInfoModelArr[i10] = null;
            }
            return effectInfoModelArr;
        }
    };
    public boolean bHasEditText;
    private boolean bNeedDownload;
    private boolean isDownloaded;
    private boolean isDownloading;
    private int mConfigureCount;
    public int mFavorite;
    public String mName;
    public String mPath;
    public String mTCID;
    public long mTemplateId;
    public String mType;
    private String mUrl;
    public String strSceneName;

    public EffectInfoModel() {
        this.mType = "";
        this.mTCID = "";
        this.mFavorite = 0;
        this.strSceneName = "";
        this.isDownloaded = false;
        this.bNeedDownload = false;
        this.isDownloading = false;
        this.bHasEditText = false;
        this.mUrl = "";
        this.mConfigureCount = 1;
        this.mName = "";
        this.mPath = null;
    }

    @Override // android.os.Parcelable
    public int describeContents() {
        return 0;
    }

    public int getmConfigureCount() {
        return this.mConfigureCount;
    }

    public String getmUrl() {
        return this.mUrl;
    }

    public boolean isDownloaded() {
        return this.isDownloaded;
    }

    public boolean isDownloading() {
        return this.isDownloading;
    }

    public boolean isNoneTheme() {
        return 72057594037927936L == this.mTemplateId;
    }

    public boolean isbNeedDownload() {
        return this.bNeedDownload;
    }

    public void setDownloaded(boolean isDownloaded) {
        this.isDownloaded = isDownloaded;
    }

    public void setDownloading(boolean isDownloading) {
        this.isDownloading = isDownloading;
    }

    public void setbNeedDownload(boolean bNeedDownload) {
        this.bNeedDownload = bNeedDownload;
    }

    public void setmConfigureCount(int mConfigureCount) {
        this.mConfigureCount = mConfigureCount;
    }

    public void setmUrl(String mUrl) {
        this.mUrl = mUrl;
    }

    @Override // android.os.Parcelable
    public void writeToParcel(Parcel parcel, int i10) {
        parcel.writeLong(this.mTemplateId);
        parcel.writeString(this.mName);
        parcel.writeString(this.mPath);
        parcel.writeString(this.mType);
        parcel.writeString(this.mTCID);
        parcel.writeInt(this.mFavorite);
        parcel.writeByte(this.isDownloaded ? (byte) 1 : (byte) 0);
        parcel.writeByte(this.bNeedDownload ? (byte) 1 : (byte) 0);
        parcel.writeByte(this.isDownloading ? (byte) 1 : (byte) 0);
        parcel.writeByte(this.bHasEditText ? (byte) 1 : (byte) 0);
        parcel.writeString(this.mUrl);
        parcel.writeInt(this.mConfigureCount);
    }

    /* JADX INFO: renamed from: clone, reason: merged with bridge method [inline-methods] */
    public EffectInfoModel m178760clone() {
        try {
            return (EffectInfoModel) super.clone();
        } catch (CloneNotSupportedException e10) {
            e10.printStackTrace();
            return null;
        }
    }

    public EffectInfoModel(long lId, String path) {
        this.mType = "";
        this.mTCID = "";
        this.mFavorite = 0;
        this.strSceneName = "";
        this.isDownloaded = false;
        this.bNeedDownload = false;
        this.isDownloading = false;
        this.bHasEditText = false;
        this.mUrl = "";
        this.mConfigureCount = 1;
        this.mTemplateId = lId;
        this.mPath = path;
    }

    public EffectInfoModel(Parcel in2) {
        this.mType = "";
        this.mTCID = "";
        boolean z10 = false;
        this.mFavorite = 0;
        this.strSceneName = "";
        this.isDownloaded = false;
        this.bNeedDownload = false;
        this.isDownloading = false;
        this.bHasEditText = false;
        this.mUrl = "";
        this.mConfigureCount = 1;
        this.mTemplateId = in2.readLong();
        this.mName = in2.readString();
        this.mPath = in2.readString();
        this.mType = in2.readString();
        this.mTCID = in2.readString();
        this.mFavorite = in2.readInt();
        this.isDownloaded = in2.readByte() != 0;
        this.bNeedDownload = in2.readByte() != 0;
        this.isDownloading = in2.readByte() != 0;
        this.bHasEditText = in2.readByte() != 0 ? true : z10;
        this.mUrl = in2.readString();
        this.mConfigureCount = in2.readInt();
    }
}
