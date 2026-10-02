package com.quvideo.xiaoying.sdk.utils;

import android.os.Parcel;
import android.os.Parcelable;

/* JADX INFO: loaded from: classes15.dex */
public class VeMSize implements Parcelable {
    public static final Parcelable.Creator<VeMSize> CREATOR = new C28420a();

    /* JADX INFO: renamed from: X */
    public int f94055X;

    /* JADX INFO: renamed from: Y */
    public int f94056Y;

    /* JADX INFO: renamed from: com.quvideo.xiaoying.sdk.utils.VeMSize$a */
    public static class C28420a implements Parcelable.Creator<VeMSize> {
        @Override // android.os.Parcelable.Creator
        /* JADX INFO: renamed from: a, reason: merged with bridge method [inline-methods] */
        public VeMSize createFromParcel(Parcel in2) {
            return new VeMSize(in2, (C28420a) null);
        }

        @Override // android.os.Parcelable.Creator
        /* JADX INFO: renamed from: b, reason: merged with bridge method [inline-methods] */
        public VeMSize[] newArray(int size) {
            return new VeMSize[size];
        }
    }

    public /* synthetic */ VeMSize(Parcel parcel, C28420a c28420a) {
        this(parcel);
    }

    @Override // android.os.Parcelable
    public int describeContents() {
        return 0;
    }

    public boolean equals(Object o10) {
        if (this == o10) {
            return true;
        }
        if (o10 == null || getClass() != o10.getClass()) {
            return false;
        }
        VeMSize veMSize = (VeMSize) o10;
        if (this.f94055X == veMSize.f94055X && this.f94056Y == veMSize.f94056Y) {
            return true;
        }
        return false;
    }

    public int hashCode() {
        return (this.f94055X * 31) + this.f94056Y;
    }

    public String toString() {
        StringBuffer stringBuffer = new StringBuffer();
        stringBuffer.append("(width:");
        stringBuffer.append(this.f94055X);
        stringBuffer.append(",height:");
        stringBuffer.append(this.f94056Y);
        stringBuffer.append(")");
        return stringBuffer.toString();
    }

    @Override // android.os.Parcelable
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(this.f94055X);
        dest.writeInt(this.f94056Y);
    }

    public VeMSize() {
    }

    public VeMSize(int cx, int cy) {
        this.f94055X = cx;
        this.f94056Y = cy;
    }

    public VeMSize(Parcel in2) {
        this.f94055X = in2.readInt();
        this.f94056Y = in2.readInt();
    }
}
