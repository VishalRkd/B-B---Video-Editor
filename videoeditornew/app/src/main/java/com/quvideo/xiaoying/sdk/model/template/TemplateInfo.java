package com.quvideo.xiaoying.sdk.model.template;

import android.os.Parcel;
import android.os.Parcelable;

/* JADX INFO: loaded from: classes15.dex */
public class TemplateInfo implements Parcelable {
    public static final Parcelable.Creator<TemplateInfo> CREATOR = new Parcelable.Creator<TemplateInfo>() { // from class: com.quvideo.xiaoying.sdk.model.template.TemplateInfo.1
        /* JADX WARN: Can't rename method to resolve collision */
        @Override // android.os.Parcelable.Creator
        public TemplateInfo createFromParcel(Parcel source) {
            return new TemplateInfo(source);
        }

        /* JADX WARN: Can't rename method to resolve collision */
        @Override // android.os.Parcelable.Creator
        public TemplateInfo[] newArray(int size) {
            TemplateInfo[] templateInfoArr = new TemplateInfo[size];
            for (int i10 = 0; i10 < size; i10++) {
                templateInfoArr[i10] = null;
            }
            return templateInfoArr;
        }
    };
    public static final int TEMPLATE_MASK_FLAG_RECOMMEND = 2;
    public int audioFlag;
    public int nDowncount;
    public int nFlag;
    public int nLikecount;
    public int nMark;
    public int nOrderno;
    public int nPoints;
    public int nPreviewtype;
    public int nSize;
    public int nState;
    public int nViewType;
    public String strAppminver;
    public String strAuthorid;
    public String strAuthorname;
    public String strDuration;
    public String strIcon;
    public String strIntro;
    public String strLang;
    public String strMission;
    public String strPreviewurl;
    public String strPublishtime;
    public String strScene;
    public String strSceneCode;
    public String strSceneIcon;
    public String strSceneName;
    public String strSubType;
    public String strTitle;
    public String strUrl;
    public String strVer;
    public String subtcid;
    public String tcid;
    public long ttid;

    public TemplateInfo() {
        this.strVer = null;
        this.tcid = null;
        this.subtcid = null;
        this.strTitle = null;
        this.strIntro = null;
        this.strIcon = null;
        this.strPreviewurl = null;
        this.nPreviewtype = -1;
        this.strLang = null;
        this.nMark = -1;
        this.strAppminver = null;
        this.nSize = -1;
        this.strScene = null;
        this.strAuthorid = null;
        this.strAuthorname = null;
        this.strPublishtime = null;
        this.nLikecount = -1;
        this.nDowncount = -1;
        this.nOrderno = -1;
        this.nPoints = -1;
        this.strUrl = null;
        this.nState = 1;
        this.nFlag = 0;
        this.nViewType = 0;
        this.strMission = null;
        this.strDuration = null;
        this.strSceneCode = null;
        this.strSceneName = null;
        this.strSceneIcon = null;
        this.audioFlag = 0;
    }

    @Override // android.os.Parcelable
    public int describeContents() {
        return 0;
    }

    public boolean equals(Object o10) {
        if (this == o10) {
            return true;
        }
        if ((o10 instanceof TemplateInfo) && this.ttid == ((TemplateInfo) o10).ttid) {
            return true;
        }
        return false;
    }

    public int hashCode() {
        return super.hashCode();
    }

    public boolean isRecommendItem() {
        return (this.nMark & 2) == 2;
    }

    @Override // android.os.Parcelable
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeLong(this.ttid);
        dest.writeString(this.strVer);
        dest.writeString(this.tcid);
        dest.writeString(this.strTitle);
        dest.writeString(this.strIntro);
        dest.writeString(this.strIcon);
        dest.writeString(this.strPreviewurl);
        dest.writeInt(this.nPreviewtype);
        dest.writeString(this.strLang);
        dest.writeInt(this.nMark);
        dest.writeString(this.strAppminver);
        dest.writeInt(this.nSize);
        dest.writeString(this.strScene);
        dest.writeString(this.strAuthorid);
        dest.writeString(this.strAuthorname);
        dest.writeString(this.strPublishtime);
        dest.writeInt(this.nLikecount);
        dest.writeInt(this.nDowncount);
        dest.writeInt(this.nOrderno);
        dest.writeInt(this.nPoints);
        dest.writeString(this.strUrl);
        dest.writeInt(this.nState);
        dest.writeInt(this.nFlag);
        dest.writeInt(this.nViewType);
        dest.writeString(this.strMission);
        dest.writeString(this.strDuration);
        dest.writeString(this.strSceneCode);
        dest.writeString(this.strSceneName);
        dest.writeString(this.strSceneIcon);
        dest.writeInt(this.audioFlag);
        dest.writeString(this.strSubType);
    }

    public TemplateInfo(Parcel in2) {
        this.strVer = null;
        this.tcid = null;
        this.subtcid = null;
        this.strTitle = null;
        this.strIntro = null;
        this.strIcon = null;
        this.strPreviewurl = null;
        this.nPreviewtype = -1;
        this.strLang = null;
        this.nMark = -1;
        this.strAppminver = null;
        this.nSize = -1;
        this.strScene = null;
        this.strAuthorid = null;
        this.strAuthorname = null;
        this.strPublishtime = null;
        this.nLikecount = -1;
        this.nDowncount = -1;
        this.nOrderno = -1;
        this.nPoints = -1;
        this.strUrl = null;
        this.nState = 1;
        this.nFlag = 0;
        this.nViewType = 0;
        this.strMission = null;
        this.strDuration = null;
        this.strSceneCode = null;
        this.strSceneName = null;
        this.strSceneIcon = null;
        this.audioFlag = 0;
        this.ttid = in2.readLong();
        this.strVer = in2.readString();
        this.tcid = in2.readString();
        this.strTitle = in2.readString();
        this.strIntro = in2.readString();
        this.strIcon = in2.readString();
        this.strPreviewurl = in2.readString();
        this.nPreviewtype = in2.readInt();
        this.strLang = in2.readString();
        this.nMark = in2.readInt();
        this.strAppminver = in2.readString();
        this.nSize = in2.readInt();
        this.strScene = in2.readString();
        this.strAuthorid = in2.readString();
        this.strAuthorname = in2.readString();
        this.strPublishtime = in2.readString();
        this.nLikecount = in2.readInt();
        this.nDowncount = in2.readInt();
        this.nOrderno = in2.readInt();
        this.nPoints = in2.readInt();
        this.strUrl = in2.readString();
        this.nState = in2.readInt();
        this.nFlag = in2.readInt();
        this.nViewType = in2.readInt();
        this.strMission = in2.readString();
        this.strDuration = in2.readString();
        this.strSceneCode = in2.readString();
        this.strSceneName = in2.readString();
        this.strSceneIcon = in2.readString();
        this.audioFlag = in2.readInt();
        this.strSubType = in2.readString();
    }
}
