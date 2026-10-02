package xiaoying.engine.clip;

import xiaoying.utils.QRect;

/* JADX INFO: loaded from: classes19.dex */
public class QTrajectoryData {
    public static final int UPDATE_MODE_EXPAND = 1;
    public static final int UPDATE_MODE_REPLACE = 0;
    public int updateMode = 0;
    public boolean useTimePos = true;

        public int[] ts = null;
    public float[] rotation = null;
    public QRect[] region = null;
}
