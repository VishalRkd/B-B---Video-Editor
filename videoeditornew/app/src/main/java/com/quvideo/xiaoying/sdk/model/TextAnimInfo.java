package com.quvideo.xiaoying.sdk.model;

import android.os.Parcel;
import android.os.Parcelable;
import androidx.annotation.NonNull;
import com.google.gson.Gson;

/* JADX INFO: loaded from: classes15.dex */
public class TextAnimInfo implements Parcelable, Cloneable {
    public static final Parcelable.Creator<PipMixInfo> CREATOR = new Parcelable.Creator<PipMixInfo>() { // from class: com.quvideo.xiaoying.sdk.model.TextAnimInfo.1
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
    private int animationDuration;
    private long animationId;

    public TextAnimInfo() {
    }

    @NonNull
    public Object clone() throws CloneNotSupportedException {
        return super.clone();
    }

    @Override // android.os.Parcelable
    public int describeContents() {
        return 0;
    }

    public int getAnimationDuration() {
        return this.animationDuration;
    }

    public long getAnimationId() {
        return this.animationId;
    }

    public boolean isDataEquals(TextAnimInfo o10) {
        if (o10 != null && this.animationId == o10.animationId && this.animationDuration == o10.animationDuration) {
            return true;
        }
        return false;
    }

    public void setAnimationDuration(int animationDuration) {
        this.animationDuration = animationDuration;
    }

    public void setAnimationId(long animationId) {
        this.animationId = animationId;
    }

    public String toString() {
        return new Gson().toJson(this);
    }

    @Override // android.os.Parcelable
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeLong(this.animationId);
        dest.writeInt(this.animationDuration);
    }

    public TextAnimInfo(Parcel in2) {
        this.animationId = in2.readLong();
        this.animationDuration = in2.readInt();
    }
}
