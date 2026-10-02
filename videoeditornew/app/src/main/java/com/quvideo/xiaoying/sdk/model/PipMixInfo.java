package com.quvideo.xiaoying.sdk.model;

import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;
import androidx.annotation.NonNull;
import com.google.gson.Gson;

/* JADX INFO: loaded from: classes15.dex */
public class PipMixInfo implements Parcelable, Cloneable {
    public static final Parcelable.Creator<PipMixInfo> CREATOR = new Parcelable.Creator<PipMixInfo>() { // from class: com.quvideo.xiaoying.sdk.model.PipMixInfo.1
        /* JADX WARN: Can't rename method to resolve collision */
        @Override // android.os.Parcelable.Creator
        public PipMixInfo createFromParcel(Parcel source) {
            return new PipMixInfo(source);
        }

        /* JADX WARN: Can't rename method to resolve collision */
        @Override // android.os.Parcelable.Creator
        public PipMixInfo[] newArray(int size) {
            return new PipMixInfo[size];
        }
    };
    private String path;

    public PipMixInfo() {
    }

    @NonNull
    public Object clone() throws CloneNotSupportedException {
        PipMixInfo pipMixInfo = (PipMixInfo) super.clone();
        pipMixInfo.setPath(getPath());
        return pipMixInfo;
    }

    @Override // android.os.Parcelable
    public int describeContents() {
        return 0;
    }

    public String getPath() {
        return this.path;
    }

    public boolean isDataEquals(PipMixInfo o10) {
        if (o10 != null && !TextUtils.isEmpty(this.path) && this.path.equals(o10.path)) {
            return true;
        }
        return false;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String toString() {
        return new Gson().toJson(this);
    }

    @Override // android.os.Parcelable
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.path);
    }

    public PipMixInfo(Parcel in2) {
        this.path = in2.readString();
    }
}
