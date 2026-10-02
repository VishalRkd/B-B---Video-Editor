package com.quvideo.xiaoying.sdk.model.editor;

import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;
import java.io.File;
import org.json.JSONObject;

/* JADX INFO: loaded from: classes15.dex */
public class DataItemProject implements Parcelable, Cloneable {
    public static final Parcelable.Creator<DataItemProject> CREATOR = new Parcelable.Creator<DataItemProject>() { // from class: com.quvideo.xiaoying.sdk.model.editor.DataItemProject.1
        /* JADX WARN: Can't rename method to resolve collision */
        @Override // android.os.Parcelable.Creator
        public DataItemProject createFromParcel(Parcel source) {
            return new DataItemProject(source);
        }

        /* JADX WARN: Can't rename method to resolve collision */
        @Override // android.os.Parcelable.Creator
        public DataItemProject[] newArray(int size) {
            return new DataItemProject[size];
        }
    };
    private static final int MASK_FLAG_CAMERA_PIP = 8;
    private static final int MASK_FLAG_EDIT_BGM = 2;
    private static final int MASK_FLAG_EDIT_MV = 65536;
    private static final int PRJ_STATE_FLAG_DEL = 2;
    public static final int PRJ_STATE_FLAG_NORMAL = 0;
    public long _id;
    public String draftLessonTag;
    public long editCostTime;
    public int editStatus;
    public String entrance;
    public int fps;
    public int iCameraCode;
    public int iIsDeleted;
    public int iIsDuplicating;
    public int iIsModified;
    public int iIsTemplateToFreeEditDraft;
    public int iPrjClipCount;
    public int iPrjDuration;
    public int nDurationLimit;
    public int originalStreamtHeight;
    public int originalStreamtWidth;
    public long presetting_id;
    public int prjThemeType;
    public int resolution;
    public String strActivityData;
    public String strCoverURL;
    public String strCreateTime;
    public String strExtra;
    public String strModifyTime;
    public String strPrjExportURL;
    public String strPrjThumbnail;
    public String strPrjTitle;
    public String strPrjURL;
    public String strPrjVersion;
    public String strVideoDesc;
    public int streamHeight;
    public int streamWidth;
    public String templateToFreeEditDraftTemplateId;
    public int todoCode;
    public long usedEffectTempId;
    public String videoTemplateInfo;

    public DataItemProject() {
        this._id = -1L;
        this.strPrjTitle = null;
        this.strPrjURL = null;
        this.strPrjExportURL = null;
        this.iPrjClipCount = 0;
        this.iPrjDuration = 0;
        this.strPrjThumbnail = null;
        this.strPrjVersion = null;
        this.strCreateTime = null;
        this.strModifyTime = null;
        this.iIsDeleted = 0;
        this.iIsModified = 0;
        this.streamWidth = 0;
        this.streamHeight = 0;
        this.usedEffectTempId = 0L;
        this.editStatus = 0;
        this.strVideoDesc = null;
        this.strActivityData = null;
        this.iCameraCode = 0;
        this.strExtra = null;
        this.strCoverURL = null;
        this.nDurationLimit = 0;
        this.fps = 0;
        this.resolution = 0;
        this.presetting_id = -1L;
        this.originalStreamtWidth = 0;
        this.originalStreamtHeight = 0;
        this.iIsDuplicating = 0;
        this.iIsTemplateToFreeEditDraft = 0;
        this.templateToFreeEditDraftTemplateId = null;
        this.draftLessonTag = null;
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
        String str = this.strPrjURL;
        String str2 = ((DataItemProject) o10).strPrjURL;
        if (str != null) {
            return str.equals(str2);
        }
        return str2 == null;
    }

    public String getPrjTodoContent() {
        if (!TextUtils.isEmpty(this.strExtra)) {
            try {
                return new JSONObject(this.strExtra).optString("todo_content");
            } catch (Exception ignored) {}
        }
        return null;
    }

    public String getProjectName() {
        if (TextUtils.isEmpty(this.strPrjURL)) {
            return null;
        }
        return new File(this.strPrjURL).getName();
    }

    public String getProjectNameDir() {
        if (TextUtils.isEmpty(this.strPrjURL)) {
            return null;
        }
        String parent = new File(this.strPrjURL).getParent();
        return (parent != null ? parent : "") + File.separator;
    }

    public int hashCode() {
        String str = this.strPrjURL;
        if (str != null) {
            return str.hashCode();
        }
        return 0;
    }

    public boolean isCameraPipMode() {
        return (this.editStatus & 8) != 0;
    }

    public boolean isMVPrj() {
        return (this.editStatus & 65536) != 0;
    }

    public boolean isProjectDel() {
        return this.iIsDeleted == 2;
    }

    public boolean isProjectModified() {
        return this.iIsModified == 1;
    }

    public void setBGMMode(boolean advance) {
        if (advance) {
            this.editStatus |= 2;
        } else {
            this.editStatus &= -3;
        }
    }

    public void setCameraPipMode(boolean isCameraPipmode) {
        if (isCameraPipmode) {
            this.editStatus |= 8;
        } else {
            this.editStatus &= -9;
        }
    }

    public void setMVPrjFlag(boolean isMV) {
        if (isMV) {
            this.editStatus |= 65536;
        } else {
            this.editStatus &= -65537;
        }
    }

    public void setPrjDelete(boolean bDelPrj) {
        if (bDelPrj) {
            this.iIsDeleted = 2;
        } else {
            this.iIsDeleted = 0;
        }
    }

    @Override // android.os.Parcelable
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeLong(this._id);
        dest.writeString(this.strPrjTitle);
        dest.writeString(this.strPrjURL);
        dest.writeString(this.strPrjExportURL);
        dest.writeInt(this.iPrjClipCount);
        dest.writeInt(this.iPrjDuration);
        dest.writeString(this.strPrjThumbnail);
        dest.writeString(this.strPrjVersion);
        dest.writeString(this.strCreateTime);
        dest.writeString(this.strModifyTime);
        dest.writeInt(this.iIsDeleted);
        dest.writeInt(this.iIsModified);
        dest.writeInt(this.streamWidth);
        dest.writeInt(this.streamHeight);
        dest.writeLong(this.usedEffectTempId);
        dest.writeInt(this.todoCode);
        dest.writeInt(this.editStatus);
        dest.writeString(this.strVideoDesc);
        dest.writeString(this.strActivityData);
        dest.writeInt(this.iCameraCode);
        dest.writeString(this.strExtra);
        dest.writeString(this.strCoverURL);
        dest.writeString(this.entrance);
        dest.writeString(this.videoTemplateInfo);
        dest.writeInt(this.nDurationLimit);
        dest.writeInt(this.prjThemeType);
        dest.writeLong(this.editCostTime);
        dest.writeInt(this.iIsDuplicating);
        dest.writeInt(this.iIsTemplateToFreeEditDraft);
        dest.writeString(this.templateToFreeEditDraftTemplateId);
        dest.writeString(this.draftLessonTag);
    }

    /* JADX INFO: renamed from: clone, reason: merged with bridge method [inline-methods] */
    public DataItemProject m178759clone() throws CloneNotSupportedException {
        return (DataItemProject) super.clone();
    }

    public DataItemProject(DataItemProject item) {
        this._id = -1L;
        this.strPrjTitle = null;
        this.strPrjURL = null;
        this.strPrjExportURL = null;
        this.iPrjClipCount = 0;
        this.iPrjDuration = 0;
        this.strPrjThumbnail = null;
        this.strPrjVersion = null;
        this.strCreateTime = null;
        this.strModifyTime = null;
        this.iIsDeleted = 0;
        this.iIsModified = 0;
        this.streamWidth = 0;
        this.streamHeight = 0;
        this.usedEffectTempId = 0L;
        this.editStatus = 0;
        this.strVideoDesc = null;
        this.strActivityData = null;
        this.iCameraCode = 0;
        this.strExtra = null;
        this.strCoverURL = null;
        this.nDurationLimit = 0;
        this.fps = 0;
        this.resolution = 0;
        this.presetting_id = -1L;
        this.originalStreamtWidth = 0;
        this.originalStreamtHeight = 0;
        this.iIsDuplicating = 0;
        this.iIsTemplateToFreeEditDraft = 0;
        this.templateToFreeEditDraftTemplateId = null;
        this.draftLessonTag = null;
        this._id = item._id;
        this.strPrjTitle = item.strPrjTitle;
        this.strPrjURL = item.strPrjURL;
        this.strPrjExportURL = item.strPrjExportURL;
        this.iPrjClipCount = item.iPrjClipCount;
        this.iPrjDuration = item.iPrjDuration;
        this.strPrjThumbnail = item.strPrjThumbnail;
        this.strPrjVersion = item.strPrjVersion;
        this.strCreateTime = item.strCreateTime;
        this.strModifyTime = item.strModifyTime;
        this.iIsDeleted = item.iIsDeleted;
        this.iIsModified = item.iIsModified;
        this.streamWidth = item.streamWidth;
        this.streamHeight = item.streamHeight;
        this.usedEffectTempId = item.usedEffectTempId;
        this.todoCode = item.todoCode;
        this.editStatus = item.editStatus;
        this.strVideoDesc = item.strVideoDesc;
        this.strActivityData = item.strActivityData;
        this.iCameraCode = item.iCameraCode;
        this.strExtra = item.strExtra;
        this.strCoverURL = item.strCoverURL;
        this.entrance = item.entrance;
        this.videoTemplateInfo = item.videoTemplateInfo;
        this.nDurationLimit = item.nDurationLimit;
        this.prjThemeType = item.prjThemeType;
        this.editCostTime = item.editCostTime;
        this.iIsDuplicating = item.iIsDuplicating;
        this.iIsTemplateToFreeEditDraft = item.iIsTemplateToFreeEditDraft;
        this.templateToFreeEditDraftTemplateId = item.templateToFreeEditDraftTemplateId;
        this.draftLessonTag = item.draftLessonTag;
    }

    public DataItemProject(Parcel in2) {
        this._id = -1L;
        this.strPrjTitle = null;
        this.strPrjURL = null;
        this.strPrjExportURL = null;
        this.iPrjClipCount = 0;
        this.iPrjDuration = 0;
        this.strPrjThumbnail = null;
        this.strPrjVersion = null;
        this.strCreateTime = null;
        this.strModifyTime = null;
        this.iIsDeleted = 0;
        this.iIsModified = 0;
        this.streamWidth = 0;
        this.streamHeight = 0;
        this.usedEffectTempId = 0L;
        this.editStatus = 0;
        this.strVideoDesc = null;
        this.strActivityData = null;
        this.iCameraCode = 0;
        this.strExtra = null;
        this.strCoverURL = null;
        this.nDurationLimit = 0;
        this.fps = 0;
        this.resolution = 0;
        this.presetting_id = -1L;
        this.originalStreamtWidth = 0;
        this.originalStreamtHeight = 0;
        this.iIsDuplicating = 0;
        this.iIsTemplateToFreeEditDraft = 0;
        this.templateToFreeEditDraftTemplateId = null;
        this.draftLessonTag = null;
        this._id = in2.readLong();
        this.strPrjTitle = in2.readString();
        this.strPrjURL = in2.readString();
        this.strPrjExportURL = in2.readString();
        this.iPrjClipCount = in2.readInt();
        this.iPrjDuration = in2.readInt();
        this.strPrjThumbnail = in2.readString();
        this.strPrjVersion = in2.readString();
        this.strCreateTime = in2.readString();
        this.strModifyTime = in2.readString();
        this.iIsDeleted = in2.readInt();
        this.iIsModified = in2.readInt();
        this.streamWidth = in2.readInt();
        this.streamHeight = in2.readInt();
        this.usedEffectTempId = in2.readLong();
        this.todoCode = in2.readInt();
        this.editStatus = in2.readInt();
        this.strVideoDesc = in2.readString();
        this.strActivityData = in2.readString();
        this.iCameraCode = in2.readInt();
        this.strExtra = in2.readString();
        this.strCoverURL = in2.readString();
        this.entrance = in2.readString();
        this.videoTemplateInfo = in2.readString();
        this.nDurationLimit = in2.readInt();
        this.prjThemeType = in2.readInt();
        this.editCostTime = in2.readLong();
        this.iIsDuplicating = in2.readInt();
        this.iIsTemplateToFreeEditDraft = in2.readInt();
        this.templateToFreeEditDraftTemplateId = in2.readString();
        this.draftLessonTag = in2.readString();
    }
}
