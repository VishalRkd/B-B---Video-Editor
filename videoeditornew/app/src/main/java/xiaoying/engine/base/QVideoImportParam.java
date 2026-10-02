package xiaoying.engine.base;

/* JADX INFO: loaded from: classes19.dex */
public class QVideoImportParam {
    private String mstrFilePath = null;
    private boolean mbPIP = false;
    private boolean mbHWDec = false;
    private boolean mbHWEnc = false;
    private boolean mbReverse = false;
    private boolean mbHDOutput = false;
    private int mCPUNum = 1;
    private boolean mbPaster = false;

    public int getCPUNum() {
        return this.mCPUNum;
    }

    public String getFilePath() {
        return this.mstrFilePath;
    }

    public boolean getHDOutputFlag() {
        return this.mbHDOutput;
    }

    public boolean getHWDecflag() {
        return this.mbHWDec;
    }

    public boolean getHWEncFlag() {
        return this.mbHWEnc;
    }

    public boolean getPIPFlag() {
        return this.mbPIP;
    }

    public boolean getPasterFlag() {
        return this.mbPaster;
    }

    public boolean getreverseFlag() {
        return this.mbReverse;
    }

    public void setCPUNum(int CPUNum) {
        this.mCPUNum = CPUNum;
    }

    public void setFilePath(String strFilePath) {
        this.mstrFilePath = strFilePath;
    }

    public void setHDOutputFlag(boolean bHDOutput) {
        this.mbHDOutput = bHDOutput;
    }

    public void setHWDecFlag(boolean bHWDec) {
        this.mbHWDec = bHWDec;
    }

    public void setHWEncFlag(boolean bHWEnc) {
        this.mbHWEnc = bHWEnc;
    }

    public void setPIPFlag(boolean bPIP) {
        this.mbPIP = bPIP;
    }

    public void setPasterFlag(boolean bPaster) {
        this.mbPaster = bPaster;
    }

    public void setReverseFlag(boolean bReverse) {
        this.mbReverse = bReverse;
    }
}
