package com.quvideo.xiaoying.sdk.model.editor;

import xiaoying.engine.storyboard.QStoryboard;
import java.util.Objects;

public class ProjectItem implements Cloneable {
    public DataItemProject mProjectDataItem;
    public QStoryboard mStoryBoard;
    public long lLastUpdateTime = 0;

    public ProjectItem(DataItemProject projectDataItem, QStoryboard storyboard) {
        this.mProjectDataItem = projectDataItem;
        this.mStoryBoard = storyboard;
    }

    public QStoryboard getStoryboard() {
        return this.mStoryBoard;
    }

    public void setStoryboard(QStoryboard storyboard) {
        this.mStoryBoard = storyboard;
    }

    public void setItem(DataItemProject projectDataItem, QStoryboard storyboard) {
        this.mProjectDataItem = projectDataItem;
        this.mStoryBoard = storyboard;
    }

    public void release() {
        if (mStoryBoard != null) {
            mStoryBoard.unInit();
            mStoryBoard = null;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ProjectItem that = (ProjectItem) o;
        return Objects.equals(mProjectDataItem, that.mProjectDataItem) &&
               Objects.equals(mStoryBoard, that.mStoryBoard);
    }

    @Override
    public int hashCode() {
        return Objects.hash(mProjectDataItem, mStoryBoard);
    }
}
