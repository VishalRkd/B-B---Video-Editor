package com.quvideo.xiaoying.sdk.model.editor;

import android.util.SparseArray;

/* JADX INFO: loaded from: classes15.dex */
public class TemplateItemData {
    public int coverPos;
    public final long lID;
    public final long lUpdateTime;
    public SparseArray<String> mTitleList;
    public int nConfigureCount;
    public int nDelFlag;
    public int nFavorite;
    public final int nFromType;
    public int nLayoutFlag;
    private int nNeedDownloadFlag;
    public int nOrder;
    public int nOriOrder;
    public int nSubOrder;
    public final int nVersion;
    public String strExtInfo;
    public String strIcon;
    public String strIntro;
    public String strMission;
    public String strMissionResult;
    public final String strPath;
    public String strScene;
    public String strSceneCode;
    public String strSceneName;
    public String strTitle;
    public String strTitleJSON;
    public int streamHeight;
    public int streamWidth;

    public static final class Builder {
        private int coverPos;
        private final long lID;
        private final long lUpdateTime;
        private SparseArray<String> mTitleList;
        private int nConfigureCount;
        private int nDelFlag;
        private int nFavorite;
        private final int nFromType;
        private int nLayoutFlag;
        private int nNeedDownloadFlag;
        private int nOrder;
        private int nOriOrder;
        private int nSubOrder;
        private final int nVersion;
        private String strExtInfo;
        private String strIcon;
        private String strIntro;
        private String strMission;
        private String strMissionResult;
        private final String strPath;
        private String strScene;
        private String strSceneCode;
        private String strSceneName;
        private String strTitle;
        private String strTitleJSON;
        private int streamHeight;
        private int streamWidth;

        public Builder(String strPath, long lID, int nVersion, int nFromType, long lUpdateTime) {
            this.strPath = strPath;
            this.lID = lID;
            this.nVersion = nVersion;
            this.nFromType = nFromType;
            this.lUpdateTime = lUpdateTime;
        }

        public TemplateItemData build() {
            return new TemplateItemData(this);
        }

        public Builder coverPos(int val) {
            this.coverPos = val;
            return this;
        }

        public Builder mTitleList(SparseArray<String> val) {
            this.mTitleList = val;
            return this;
        }

        public Builder nConfigureCount(int val) {
            this.nConfigureCount = val;
            return this;
        }

        public Builder nDelFlag(int val) {
            this.nDelFlag = val;
            return this;
        }

        public Builder nFavorite(int val) {
            this.nFavorite = val;
            return this;
        }

        public Builder nLayoutFlag(int val) {
            this.nLayoutFlag = val;
            return this;
        }

        public Builder nNeedDownloadFlag(int val) {
            this.nNeedDownloadFlag = val;
            return this;
        }

        public Builder nOrder(int val) {
            this.nOrder = val;
            return this;
        }

        public Builder nOriOrder(int val) {
            this.nOriOrder = val;
            return this;
        }

        public Builder nSubOrder(int val) {
            this.nSubOrder = val;
            return this;
        }

        public Builder setStrIcon(String strIcon) {
            this.strIcon = strIcon;
            return this;
        }

        public Builder strExtInfo(String val) {
            this.strExtInfo = val;
            return this;
        }

        public Builder strIntro(String val) {
            this.strIntro = val;
            return this;
        }

        public Builder strMission(String val) {
            this.strMission = val;
            return this;
        }

        public Builder strMissionResult(String val) {
            this.strMissionResult = val;
            return this;
        }

        public Builder strScene(String val) {
            this.strScene = val;
            return this;
        }

        public Builder strSceneCode(String val) {
            this.strSceneCode = val;
            return this;
        }

        public Builder strSceneName(String val) {
            this.strSceneName = val;
            return this;
        }

        public Builder strTitle(String val) {
            this.strTitle = val;
            return this;
        }

        public Builder strTitleJSON(String val) {
            this.strTitleJSON = val;
            return this;
        }

        public Builder streamHeight(int val) {
            this.streamHeight = val;
            return this;
        }

        public Builder streamWidth(int val) {
            this.streamWidth = val;
            return this;
        }
    }

    public int getNeedDownloadFlag() {
        return this.nNeedDownloadFlag;
    }

    public boolean shouldOnlineDownload() {
        return this.nNeedDownloadFlag != 0;
    }

    public String toString() {
        return "TemplateItemData{strPath='" + this.strPath + "', lID=" + this.lID + ", nVersion=" + this.nVersion + ", nOrder=" + this.nOrder + ", nFromType=" + this.nFromType + ", lUpdateTime=" + this.lUpdateTime + ", nFavorite=" + this.nFavorite + ", nOriOrder=" + this.nOriOrder + ", mTitleList=" + this.mTitleList + ", strTitleJSON='" + this.strTitleJSON + "', nSubOrder=" + this.nSubOrder + ", nLayoutFlag=" + this.nLayoutFlag + ", strExtInfo='" + this.strExtInfo + "', nConfigureCount=" + this.nConfigureCount + ", nNeedDownloadFlag=" + this.nNeedDownloadFlag + ", strMission='" + this.strMission + "', strMissionResult='" + this.strMissionResult + "', strSceneCode='" + this.strSceneCode + "', strSceneName='" + this.strSceneName + "', strTitle='" + this.strTitle + "', strScene='" + this.strScene + "', strIntro='" + this.strIntro + "', streamWidth=" + this.streamWidth + ", streamHeight=" + this.streamHeight + ", coverPos=" + this.coverPos + ", nDelFlag=" + this.nDelFlag + ", strIcon='" + this.strIcon + "'}";
    }

    private TemplateItemData(Builder builder) {
        this.strPath = builder.strPath;
        this.lID = builder.lID;
        this.nVersion = builder.nVersion;
        this.nOrder = builder.nOrder;
        this.nFromType = builder.nFromType;
        this.lUpdateTime = builder.lUpdateTime;
        this.nFavorite = builder.nFavorite;
        this.nOriOrder = builder.nOriOrder;
        this.mTitleList = builder.mTitleList;
        this.strTitleJSON = builder.strTitleJSON;
        this.nSubOrder = builder.nSubOrder;
        this.nLayoutFlag = builder.nLayoutFlag;
        this.strExtInfo = builder.strExtInfo;
        this.nConfigureCount = builder.nConfigureCount;
        this.nNeedDownloadFlag = builder.nNeedDownloadFlag;
        this.strMission = builder.strMission;
        this.strMissionResult = builder.strMissionResult;
        this.strSceneCode = builder.strSceneCode;
        this.strSceneName = builder.strSceneName;
        this.strTitle = builder.strTitle;
        this.strScene = builder.strScene;
        this.strIntro = builder.strIntro;
        this.streamWidth = builder.streamWidth;
        this.streamHeight = builder.streamHeight;
        this.coverPos = builder.coverPos;
        this.nDelFlag = builder.nDelFlag;
        this.strIcon = builder.strIcon;
    }
}
