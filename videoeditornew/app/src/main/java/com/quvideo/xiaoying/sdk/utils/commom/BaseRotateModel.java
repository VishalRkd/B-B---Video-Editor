package com.quvideo.xiaoying.sdk.utils.commom;

import android.os.Parcel;
import android.os.Parcelable;

/* JADX INFO: loaded from: classes15.dex */
public class BaseRotateModel implements Parcelable {
    public static final Parcelable.Creator<BaseRotateModel> CREATOR = new C28421a();

    /* JADX INFO: renamed from: X */
    public int f94057X;

    /* JADX INFO: renamed from: Y */
    public boolean f94058Y;

    /* JADX INFO: renamed from: com.quvideo.xiaoying.sdk.utils.commom.BaseRotateModel$a */
    public static class C28421a implements Parcelable.Creator<BaseRotateModel> {
        @Override // android.os.Parcelable.Creator
        /* JADX INFO: renamed from: a, reason: merged with bridge method [inline-methods] */
        public BaseRotateModel createFromParcel(Parcel in2) {
            return new BaseRotateModel(in2);
        }

        @Override // android.os.Parcelable.Creator
        /* JADX INFO: renamed from: b, reason: merged with bridge method [inline-methods] */
        public BaseRotateModel[] newArray(int size) {
            return new BaseRotateModel[size];
        }
    }

    public BaseRotateModel(int turnNum, boolean isClockWise) {
        this.f94057X = turnNum;
        this.f94058Y = isClockWise;
    }

    @Override // android.os.Parcelable
    public int describeContents() {
        return 0;
    }

    @Override // android.os.Parcelable
    public void writeToParcel(Parcel parcel, int i10) {
        parcel.writeInt(this.f94057X);
        parcel.writeByte(this.f94058Y ? (byte) 1 : (byte) 0);
    }

    public BaseRotateModel(Parcel in2) {
        this.f94057X = in2.readInt();
        this.f94058Y = in2.readByte() != 0;
    }
}
