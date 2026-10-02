package com.quvideo.xiaoying.sdk.model.template;

import android.text.TextUtils;
import androidx.annotation.NonNull;
import java.util.List;
import java.util.regex.Pattern;

/* JADX INFO: loaded from: classes15.dex */
public class CategoryTemplateInfo implements Comparable<CategoryTemplateInfo> {
    public int dataType;
    public String sceneCode;
    public String sceneName;
    public List<Long> templateList;

    public CategoryTemplateInfo(String sceneCode, String sceneName, List<Long> templateList, int dataType) {
        this.sceneCode = sceneCode;
        this.sceneName = sceneName;
        this.templateList = templateList;
        this.dataType = dataType;
    }

    private boolean cotainRecomend(int type) {
        boolean z10 = true;
        if (type != 1 && type != 2) {
            z10 = false;
        }
        return z10;
    }

    private boolean isInteger(String string) {
        if (TextUtils.isEmpty(string)) {
            return false;
        }
        return Pattern.compile("[\\-|\\+]?\\d+").matcher(string).matches();
    }

    private int sortBySceneCode(String code1, String code2) {
        if (isInteger(code1) && isInteger(code2)) {
            return Integer.valueOf(code1).intValue() - Integer.valueOf(code2).intValue();
        }
        return isInteger(code2) ? 1 : -1;
    }

    @Override // java.lang.Comparable
    public int compareTo(@NonNull CategoryTemplateInfo o10) {
        if (cotainRecomend(o10.dataType) && cotainRecomend(this.dataType)) {
            return sortBySceneCode(o10.sceneCode, this.sceneCode);
        }
        int i10 = o10.dataType;
        int i11 = this.dataType;
        return i10 == i11 ? sortBySceneCode(o10.sceneCode, this.sceneCode) : i10 - i11;
    }
}
