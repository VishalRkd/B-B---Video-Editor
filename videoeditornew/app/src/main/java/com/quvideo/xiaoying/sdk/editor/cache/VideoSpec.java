package com.quvideo.xiaoying.sdk.editor.cache;

import android.os.Parcel;
import android.os.Parcelable;
import androidx.annotation.Nullable;

/* JADX INFO: loaded from: classes15.dex */
public class VideoSpec implements Parcelable {
    public static final Parcelable.Creator<VideoSpec> CREATOR = new C28327a();

    /* JADX INFO: renamed from: X */
    public int f93435X;

    /* JADX INFO: renamed from: Y */
    public int f93436Y;

    /* JADX INFO: renamed from: Z */
    public int f93437Z;

    /* JADX INFO: renamed from: e0 */
    public int f93438e0;

    /* JADX INFO: renamed from: f0 */
    public int f93439f0;

    /* JADX INFO: renamed from: g0 */
    public float f93440g0;

    /* JADX INFO: renamed from: h0 */
    public int f93441h0;

    /* JADX INFO: renamed from: i0 */
    public boolean f93442i0;

    /* JADX INFO: renamed from: com.quvideo.xiaoying.sdk.editor.cache.VideoSpec$a */
    public static class C28327a implements Parcelable.Creator<VideoSpec> {
        @Override // android.os.Parcelable.Creator
        /* JADX INFO: renamed from: a, reason: merged with bridge method [inline-methods] */
        public VideoSpec createFromParcel(Parcel in2) {
            VideoSpec videoSpec = new VideoSpec();
            videoSpec.m97128r(in2);
            return videoSpec;
        }

        @Override // android.os.Parcelable.Creator
        /* JADX INFO: renamed from: b, reason: merged with bridge method [inline-methods] */
        public VideoSpec[] newArray(int size) {
            return new VideoSpec[size];
        }
    }

    public VideoSpec() {
        this.f93440g0 = 1.0f;
        this.f93441h0 = -1;
    }

    @Nullable
    /* JADX INFO: renamed from: e */
    public static VideoSpec m97113e(@Nullable VideoSpec videoSpec) {
        if (videoSpec == null) {
            return null;
        }
        return new VideoSpec(videoSpec);
    }

    /* JADX INFO: renamed from: c */
    public final int m97114c() {
        return (this.f93435X + this.f93437Z) >> 1;
    }

    /* JADX INFO: renamed from: d */
    public final int m97115d() {
        return (this.f93436Y + this.f93438e0) >> 1;
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
        VideoSpec videoSpec = (VideoSpec) o10;
        return this.f93435X == videoSpec.f93435X && this.f93436Y == videoSpec.f93436Y && this.f93437Z == videoSpec.f93437Z && this.f93438e0 == videoSpec.f93438e0 && videoSpec.f93439f0 == this.f93439f0 && videoSpec.f93440g0 == this.f93440g0 && videoSpec.f93441h0 == this.f93441h0 && videoSpec.f93442i0 == this.f93442i0;
    }

    /* JADX INFO: renamed from: f */
    public boolean m97116f(Object o10) {
        if (this == o10) {
            return true;
        }
        if (o10 == null || getClass() != o10.getClass()) {
            return false;
        }
        VideoSpec videoSpec = (VideoSpec) o10;
        return this.f93435X == videoSpec.f93435X && this.f93436Y == videoSpec.f93436Y && this.f93437Z == videoSpec.f93437Z && this.f93438e0 == videoSpec.f93438e0 && videoSpec.f93440g0 == this.f93440g0 && videoSpec.f93441h0 == this.f93441h0 && videoSpec.f93442i0 == this.f93442i0;
    }

    /* JADX INFO: renamed from: g */
    public final float m97117g() {
        return (this.f93435X + this.f93437Z) * 0.5f;
    }

    /* JADX INFO: renamed from: h */
    public final float m97118h() {
        return (this.f93436Y + this.f93438e0) * 0.5f;
    }

    public int hashCode() {
        return (((((int) ((((((((((this.f93435X * 5) + this.f93436Y) * 5) + this.f93437Z) * 5) + this.f93438e0) * 5) + this.f93439f0) * 5) + this.f93440g0)) * 5) + this.f93441h0) * 5) + (this.f93442i0 ? 1 : 0);
    }

    /* JADX INFO: renamed from: i */
    public int m97119i() {
        return this.f93441h0;
    }

    /* JADX INFO: renamed from: j */
    public final int m97120j() {
        return this.f93439f0;
    }

    /* JADX INFO: renamed from: k */
    public float m97121k() {
        return this.f93440g0;
    }

    /* JADX INFO: renamed from: l */
    public final int m97122l() {
        return this.f93438e0 - this.f93436Y;
    }

    /* JADX INFO: renamed from: m */
    public void m97123m(int dx, int dy) {
        this.f93435X += dx;
        this.f93436Y += dy;
        this.f93437Z -= dx;
        this.f93438e0 -= dy;
    }

    /* JADX INFO: renamed from: n */
    public final boolean m97124n() {
        return this.f93435X >= this.f93437Z || this.f93436Y >= this.f93438e0;
    }

    /* JADX INFO: renamed from: o */
    public boolean m97125o() {
        return this.f93442i0;
    }

    /* JADX INFO: renamed from: p */
    public void m97126p(int dx, int dy) {
        this.f93435X += dx;
        this.f93436Y += dy;
        this.f93437Z += dx;
        this.f93438e0 += dy;
    }

    /* JADX INFO: renamed from: q */
    public void m97127q(int newLeft, int newTop) {
        this.f93437Z += newLeft - this.f93435X;
        this.f93438e0 += newTop - this.f93436Y;
        this.f93435X = newLeft;
        this.f93436Y = newTop;
    }

    /* JADX INFO: renamed from: r */
    public void m97128r(Parcel in2) {
        this.f93435X = in2.readInt();
        this.f93436Y = in2.readInt();
        this.f93437Z = in2.readInt();
        this.f93438e0 = in2.readInt();
        this.f93439f0 = in2.readInt();
        this.f93440g0 = in2.readFloat();
        this.f93441h0 = in2.readInt();
        boolean z10 = true;
        if (in2.readInt() != 1) {
            z10 = false;
        }
        this.f93442i0 = z10;
    }

    /* JADX INFO: renamed from: s */
    public void m97129s(float scale) {
        if (scale != 1.0f) {
            this.f93435X = (int) ((this.f93435X * scale) + 0.5f);
            this.f93436Y = (int) ((this.f93436Y * scale) + 0.5f);
            this.f93437Z = (int) ((this.f93437Z * scale) + 0.5f);
            this.f93438e0 = (int) ((this.f93438e0 * scale) + 0.5f);
        }
    }

    /* JADX INFO: renamed from: t */
    public void m97130t(int left, int top, int right, int bottom, int length) {
        this.f93435X = left;
        this.f93436Y = top;
        this.f93437Z = right;
        this.f93438e0 = bottom;
        this.f93439f0 = length;
    }

    public String toString() {
        StringBuilder sb2 = new StringBuilder(32);
        sb2.append("Rect(");
        sb2.append(this.f93435X);
        sb2.append(",");
        sb2.append(this.f93436Y);
        sb2.append(" - ");
        sb2.append(this.f93437Z);
        sb2.append(",");
        sb2.append(this.f93438e0);
        sb2.append(",");
        sb2.append(this.f93440g0);
        sb2.append(",");
        sb2.append(this.f93441h0);
        sb2.append(",");
        sb2.append(this.f93439f0);
        sb2.append(",");
        sb2.append(this.f93442i0);
        sb2.append(")");
        return sb2.toString();
    }

    /* JADX INFO: renamed from: u */
    public void m97131u(VideoSpec src) {
        this.f93435X = src.f93435X;
        this.f93436Y = src.f93436Y;
        this.f93437Z = src.f93437Z;
        this.f93438e0 = src.f93438e0;
        this.f93439f0 = src.f93439f0;
    }

    /* JADX INFO: renamed from: v */
    public void m97132v() {
        this.f93439f0 = 0;
        this.f93438e0 = 0;
        this.f93436Y = 0;
        this.f93437Z = 0;
        this.f93435X = 0;
    }

    /* JADX INFO: renamed from: w */
    public void m97133w(float scale) {
        this.f93440g0 = scale;
    }

    @Override // android.os.Parcelable
    public void writeToParcel(Parcel parcel, int i10) {
        parcel.writeInt(this.f93435X);
        parcel.writeInt(this.f93436Y);
        parcel.writeInt(this.f93437Z);
        parcel.writeInt(this.f93438e0);
        parcel.writeInt(this.f93439f0);
        parcel.writeFloat(this.f93440g0);
        parcel.writeInt(this.f93441h0);
        parcel.writeInt(this.f93442i0 ? 1 : 0);
    }

    /* JADX INFO: renamed from: x */
    public final int m97134x() {
        return this.f93437Z - this.f93435X;
    }

    public VideoSpec(int left, int top, int right, int bottom, int length) {
        this.f93440g0 = 1.0f;
        this.f93441h0 = -1;
        this.f93435X = left;
        this.f93436Y = top;
        this.f93437Z = right;
        this.f93438e0 = bottom;
        this.f93439f0 = length;
    }

    public VideoSpec(VideoSpec videoSpec) {
        this.f93440g0 = 1.0f;
        this.f93441h0 = -1;
        if (videoSpec == null) {
            this.f93439f0 = 0;
            this.f93438e0 = 0;
            this.f93437Z = 0;
            this.f93436Y = 0;
            this.f93435X = 0;
            return;
        }
        this.f93435X = videoSpec.f93435X;
        this.f93436Y = videoSpec.f93436Y;
        this.f93437Z = videoSpec.f93437Z;
        this.f93438e0 = videoSpec.f93438e0;
        this.f93439f0 = videoSpec.f93439f0;
        this.f93440g0 = videoSpec.f93440g0;
        this.f93441h0 = videoSpec.f93441h0;
        this.f93442i0 = videoSpec.f93442i0;
    }
}
