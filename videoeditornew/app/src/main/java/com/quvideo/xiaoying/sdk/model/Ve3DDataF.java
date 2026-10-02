package com.quvideo.xiaoying.sdk.model;

import android.os.Parcel;
import android.os.Parcelable;
import androidx.annotation.Keep;
import java.io.Serializable;
import java.util.Objects;

/* JADX INFO: loaded from: classes15.dex */
@Keep
public class Ve3DDataF implements Serializable, Parcelable, Cloneable {
    public static final Parcelable.Creator<Ve3DDataF> CREATOR = new Parcelable.Creator<Ve3DDataF>() { // from class: com.quvideo.xiaoying.sdk.model.Ve3DDataF.1
        /* JADX WARN: Can't rename method to resolve collision */
        @Override // android.os.Parcelable.Creator
        public Ve3DDataF createFromParcel(Parcel source) {
            return new Ve3DDataF(source);
        }

        /* JADX WARN: Can't rename method to resolve collision */
        @Override // android.os.Parcelable.Creator
        public Ve3DDataF[] newArray(int size) {
            return new Ve3DDataF[size];
        }
    };
    private static final long serialVersionUID = 2849681856012276688L;

    /* JADX INFO: renamed from: x */
    public float f94050x;

    /* JADX INFO: renamed from: y */
    public float f94051y;

    /* JADX INFO: renamed from: z */
    public float f94052z;

    public Ve3DDataF() {
        this.f94050x = 0.0f;
        this.f94051y = 0.0f;
        this.f94052z = 0.0f;
    }

    @Override // android.os.Parcelable
    public int describeContents() {
        return 0;
    }

    public boolean equals(Object o10) {
        if (this == o10) {
            return true;
        }
        if (!(o10 instanceof Ve3DDataF)) {
            return false;
        }
        Ve3DDataF ve3DDataF = (Ve3DDataF) o10;
        return Float.compare(ve3DDataF.f94050x, this.f94050x) == 0 && Float.compare(ve3DDataF.f94051y, this.f94051y) == 0 && Float.compare(ve3DDataF.f94052z, this.f94052z) == 0;
    }

    public int hashCode() {
        return Objects.hash(Float.valueOf(this.f94050x), Float.valueOf(this.f94051y), Float.valueOf(this.f94052z));
    }

    public String toString() {
        return "Ve3DPointF{x=" + this.f94050x + ", y=" + this.f94051y + ", z=" + this.f94052z + '}';
    }

    @Override // android.os.Parcelable
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeFloat(this.f94050x);
        dest.writeFloat(this.f94051y);
        dest.writeFloat(this.f94052z);
    }

    /* JADX INFO: renamed from: clone, reason: merged with bridge method [inline-methods] */
    public Ve3DDataF m178758clone() throws CloneNotSupportedException {
        return (Ve3DDataF) super.clone();
    }

    public Ve3DDataF(Ve3DDataF ve3DDataF) {
        this.f94050x = 0.0f;
        this.f94051y = 0.0f;
        this.f94052z = 0.0f;
        if (ve3DDataF != null) {
            this.f94050x = ve3DDataF.f94050x;
            this.f94051y = ve3DDataF.f94051y;
            this.f94052z = ve3DDataF.f94052z;
        }
    }

    public Ve3DDataF(float x10, float y10, float z10) {
        this.f94050x = x10;
        this.f94051y = y10;
        this.f94052z = z10;
    }

    public Ve3DDataF(Parcel in2) {
        this.f94050x = 0.0f;
        this.f94051y = 0.0f;
        this.f94052z = 0.0f;
        this.f94050x = in2.readFloat();
        this.f94051y = in2.readFloat();
        this.f94052z = in2.readFloat();
    }
}
