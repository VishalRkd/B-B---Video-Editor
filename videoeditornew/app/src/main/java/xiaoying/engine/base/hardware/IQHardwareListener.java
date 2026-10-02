package xiaoying.engine.base.hardware;

/* JADX INFO: loaded from: classes19.dex */
public interface IQHardwareListener {
    int onStatus(int type, boolean bSupported, int supportMaxCount);

    int onStatusEnd(int nType, int supportMaxCount, int res);

    int onStatusOver(QHardwareQuery.QHardwareResult[] result);

    int onStatusStart(int nType, int supportMaxCount);
}
