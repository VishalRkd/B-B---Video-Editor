package com.quvideo.xiaoying.sdk.model;

import java.util.Comparator;

/* JADX INFO: loaded from: classes15.dex */
public class ComparatorBaseObject implements Comparator<BaseObject> {
    @Override // java.util.Comparator
    public int compare(BaseObject lhs, BaseObject rhs) {
        int i10;
        int i11;
        if (lhs == null) {
            return rhs != null ? -1 : 0;
        }
        if (rhs != null && (i10 = lhs.mOrderNum) <= (i11 = rhs.mOrderNum)) {
            return i10 < i11 ? -1 : 0;
        }
        return 1;
    }
}
