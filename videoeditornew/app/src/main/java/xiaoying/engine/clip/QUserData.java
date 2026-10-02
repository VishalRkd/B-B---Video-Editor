package xiaoying.engine.clip;

/* JADX INFO: loaded from: classes19.dex */
public class QUserData {
    public byte[] data;
    public int dataLen;

    public QUserData(int dataLen) {
        this.data = null;
        this.dataLen = dataLen;
        this.data = new byte[dataLen];
    }

    public byte[] getUserData() {
        return this.data;
    }

    public int getUserDataLength() {
        return this.dataLen;
    }

    public void setUserData(byte[] data) {
        this.data = data;
        if (data != null) {
            this.dataLen = data.length;
        } else {
            this.dataLen = 0;
        }
    }

    public QUserData() {
        this.data = null;
        this.dataLen = 0;
    }
}
