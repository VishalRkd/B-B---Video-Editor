package com.quvideo.xiaoying.sdk.model;

import android.os.Parcel;
import android.os.Parcelable;

/* JADX INFO: loaded from: classes15.dex */
public class VeRange implements Comparable<VeRange>, Parcelable {
    public static final Parcelable.Creator<VeRange> CREATOR = new Parcelable.Creator<VeRange>() { // from class: com.quvideo.xiaoying.sdk.model.VeRange.1
        /* JADX WARN: Can't rename method to resolve collision */
        @Override // android.os.Parcelable.Creator
        public VeRange createFromParcel(Parcel in2) {
            return new VeRange(in2);
        }

        /* JADX WARN: Can't rename method to resolve collision */
        @Override // android.os.Parcelable.Creator
        public VeRange[] newArray(int size) {
            return new VeRange[size];
        }
    };
    private int mPosition;
    private int mTimeLength;

    public boolean contains(int value) {
        if (value < this.mPosition || (value >= getLimitValue() && this.mTimeLength >= 0)) {
            return false;
        }
        return true;
    }

    public boolean contains2(int value) {
        if (value < this.mPosition || (value > getLimitValue() && this.mTimeLength >= 0)) {
            return false;
        }
        return true;
    }

    @Override // android.os.Parcelable
    public int describeContents() {
        return 0;
    }

    public boolean equals(Object objR) {
        boolean z10 = false;
        if (!(objR instanceof VeRange)) {
            return false;
        }
        VeRange veRange = (VeRange) objR;
        if (veRange.getmPosition() == this.mPosition && veRange.getmTimeLength() == this.mTimeLength) {
            z10 = true;
        }
        return z10;
    }

    public int getLimitValue() {
        int i10 = this.mTimeLength;
        if (i10 == -1) {
            return Integer.MAX_VALUE;
        }
        return this.mPosition + i10;
    }

    public int getmPosition() {
        return this.mPosition;
    }

    public int getmTimeLength() {
        return this.mTimeLength;
    }

    public int hashCode() {
        return super.hashCode();
    }

    public boolean inRange(int relative) {
        return relative >= 0 && relative <= this.mTimeLength;
    }

    public void setmPosition(int mPosition) {
        this.mPosition = mPosition;
    }

    public void setmTimeLength(int mTimeLength) {
        this.mTimeLength = mTimeLength;
    }

    public String toString() {
        StringBuffer stringBuffer = new StringBuffer();
        stringBuffer.append("(mPosition:" + this.mPosition + ";mTimeLength:" + this.mTimeLength + ")");
        return stringBuffer.toString();
    }

    @Override // android.os.Parcelable
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(this.mPosition);
        dest.writeInt(this.mTimeLength);
    }

    public VeRange() {
    }

    @Override // java.lang.Comparable
    public int compareTo(VeRange another) {
        if (another != null) {
            if (getmPosition() > another.getmPosition()) {
                return 1;
            }
            if (getmPosition() < another.getmPosition()) {
                return -1;
            }
        }
        return 0;
    }

    public VeRange(VeRange veRange) {
        if (veRange != null) {
            this.mPosition = veRange.mPosition;
            this.mTimeLength = veRange.mTimeLength;
        }
    }

    public VeRange(int start, int len) {
        this.mPosition = start;
        this.mTimeLength = len;
    }

    private VeRange(Parcel in2) {
        this.mPosition = in2.readInt();
        this.mTimeLength = in2.readInt();
    }
}
