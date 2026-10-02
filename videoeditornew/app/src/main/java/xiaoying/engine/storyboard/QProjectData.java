package xiaoying.engine.storyboard;

import xiaoying.engine.base.QRange;

/* JADX INFO: loaded from: classes19.dex */
public class QProjectData {
    public int projectID = 0;
    public long themeID = 0;
    public long[] templates = null;
    public QProjectMediaInfo[] mediaInfos = null;

    public static class QProjectMediaInfo {
        public static final int QPROJECT_MEDIA_FATHER_TYPE_CLIP = 1;
        public static final int QPROJECT_MEDIA_FATHER_TYPE_EFFECT_AUDIO = 3;
        public static final int QPROJECT_MEDIA_FATHER_TYPE_EFFECT_EX_FILE = 5;
        public static final int QPROJECT_MEDIA_FATHER_TYPE_EFFECT_TEXT = 4;
        public static final int QPROJECT_MEDIA_FATHER_TYPE_EFFECT_VIDEO = 2;
        public static final int QPROJECT_MEDIA_FATHER_TYPE_NONE = 0;
        public QRange srcRange;
        public QRange trimRange;
        public String strFilePath = null;
        public long lUID = 0;
        public int nFatherType = 0;
        public int nIndex = 0;
    }
}
