package xiaoying.engine.aecomp;

import xiaoying.engine.base.QRange;

/* JADX INFO: loaded from: classes18.dex */
public class QAEProjectData {
    public long[] templates = null;
    public QAEProjectMediaInfo[] mediaInfos = null;

    public static class QAEProjectMediaInfo {
        public String strFilePath = null;
        public long lUID = 0;
        public QRange trimRange = null;
        public QRange srcRange = null;
    }
}
