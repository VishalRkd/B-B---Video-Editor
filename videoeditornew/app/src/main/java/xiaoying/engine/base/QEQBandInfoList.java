package xiaoying.engine.base;

/* JADX INFO: loaded from: classes19.dex */
public class QEQBandInfoList {
    public static final int MAX_QEQBANDINFO_NODE_COUNT = 20;
    public int iEQBandInfoNodeCount;
    public QEQBandInfo[] pQEQBandInfoArray = new QEQBandInfo[20];

    public QEQBandInfoList() {
        this.iEQBandInfoNodeCount = 0;
        for (int i10 = 0; i10 < 20; i10++) {
            this.pQEQBandInfoArray[i10] = null;
        }
        this.iEQBandInfoNodeCount = 0;
    }
}
