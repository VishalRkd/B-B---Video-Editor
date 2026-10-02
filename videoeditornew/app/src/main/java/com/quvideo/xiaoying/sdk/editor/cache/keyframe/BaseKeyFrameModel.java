package com.quvideo.xiaoying.sdk.editor.cache.keyframe;

import androidx.annotation.Keep;
import java.util.Comparator;
import kotlin.Metadata;
import kotlin.jvm.internal.DefaultConstructorMarker;
import kotlin.jvm.internal.Intrinsics;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import xiaoying.engine.clip.QKeyFrameTransformData;

/* JADX INFO: loaded from: classes15.dex */
@Keep
public class BaseKeyFrameModel implements Comparator<BaseKeyFrameModel> {

    @NotNull
    private final KeyFrameType Type;
    private int curTime;

    @Nullable
    private QKeyFrameTransformData.EasingInfo easingInfo;
    private final int method;
    private int relativeTime;

    public BaseKeyFrameModel(int i10, int i11, @NotNull KeyFrameType Type, int i12, @Nullable QKeyFrameTransformData.EasingInfo easingInfo) {
        Intrinsics.checkNotNullParameter(Type, "Type");
        this.curTime = i10;
        this.relativeTime = i11;
        this.Type = Type;
        this.method = i12;
        this.easingInfo = easingInfo;
    }

    public final int getCurTime() {
        return this.curTime;
    }

    @Nullable
    public final QKeyFrameTransformData.EasingInfo getEasingInfo() {
        return this.easingInfo;
    }

    public final int getMethod() {
        return this.method;
    }

    public final int getRelativeTime() {
        return this.relativeTime;
    }

    @NotNull
    public final KeyFrameType getType() {
        return this.Type;
    }

    public final void setCurTime(int i10) {
        this.curTime = i10;
    }

    public final void setEasingInfo(@Nullable QKeyFrameTransformData.EasingInfo easingInfo) {
        this.easingInfo = easingInfo;
    }

    public final void setRelativeTime(int i10) {
        this.relativeTime = i10;
    }

    @Override // java.util.Comparator
    public int compare(@NotNull BaseKeyFrameModel o10, @NotNull BaseKeyFrameModel o11) {
        Intrinsics.checkNotNullParameter(o10, "o1");
        Intrinsics.checkNotNullParameter(o11, "o2");
        int i10 = o10.curTime;
        int i11 = o11.curTime;
        if (i10 > i11) {
            return 1;
        }
        return i10 == i11 ? 0 : -1;
    }

    public /* synthetic */ BaseKeyFrameModel(int i10, int i11, KeyFrameType keyFrameType, int i12, QKeyFrameTransformData.EasingInfo easingInfo, int i13, DefaultConstructorMarker defaultConstructorMarker) {
        this(i10, i11, keyFrameType, (i13 & 8) != 0 ? 3 : i12, (i13 & 16) != 0 ? null : easingInfo);
    }
}
