package com.quvideo.xiaoying.sdk.model;

import android.os.Parcel;
import android.os.Parcelable;
import com.quvideo.xiaoying.sdk.utils.VeMSize;

/* JADX INFO: loaded from: classes15.dex */
public class GifExpModel implements Parcelable {
    public static final Parcelable.Creator<GifExpModel> CREATOR = new Parcelable.Creator<GifExpModel>() { // from class: com.quvideo.xiaoying.sdk.model.GifExpModel.1
        /* JADX WARN: Can't rename method to resolve collision */
        @Override // android.os.Parcelable.Creator
        public GifExpModel createFromParcel(Parcel source) {
            return new GifExpModel(source);
        }

        /* JADX WARN: Can't rename method to resolve collision */
        @Override // android.os.Parcelable.Creator
        public GifExpModel[] newArray(int size) {
            return new GifExpModel[size];
        }
    };
    public int expFps;
    public VeMSize expSize;
    public VeRange mExpVeRange;

    public GifExpModel() {
        this.expFps = 0;
    }

    @Override // android.os.Parcelable
    public int describeContents() {
        return 0;
    }

    @Override // android.os.Parcelable
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeParcelable(this.expSize, flags);
        dest.writeInt(this.expFps);
        dest.writeParcelable(this.mExpVeRange, flags);
    }

    public GifExpModel(Parcel in2) {
        this.expFps = 0;
        this.expSize = (VeMSize) in2.readParcelable(VeMSize.class.getClassLoader());
        this.expFps = in2.readInt();
        this.mExpVeRange = (VeRange) in2.readParcelable(VeRange.class.getClassLoader());
    }
}
