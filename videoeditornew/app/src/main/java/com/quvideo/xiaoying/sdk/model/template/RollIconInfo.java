package com.quvideo.xiaoying.sdk.model.template;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/* JADX INFO: loaded from: classes15.dex */
public class RollIconInfo implements Cloneable {
    public String mBigIconUrl;
    public String mIconUrl;
    public List<RollXytInfo> mXytList;

    public static ArrayList<RollXytInfo> cloneList(List<RollXytInfo> list) throws CloneNotSupportedException {
        ArrayList<RollXytInfo> arrayList = new ArrayList<>(list.size());
        Iterator<RollXytInfo> it = list.iterator();
        while (it.hasNext()) {
            arrayList.add((RollXytInfo) it.next().clone());
        }
        return arrayList;
    }

    public Object clone() {
        RollIconInfo rollIconInfo = null;
        try {
            RollIconInfo rollIconInfo2 = (RollIconInfo) super.clone();
            try {
                rollIconInfo2.mXytList = cloneList(rollIconInfo2.mXytList);
                return rollIconInfo2;
            } catch (CloneNotSupportedException unused) {
                rollIconInfo = rollIconInfo2;
                return rollIconInfo;
            }
        } catch (CloneNotSupportedException unused2) {
            return null;
        }
    }
}
