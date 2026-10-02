package xiaoying.engine.base;

/* JADX INFO: loaded from: classes19.dex */
public class QPKGFileSource {
    public int fileID;
    public String pkgFile;

    public QPKGFileSource() {
        this.pkgFile = null;
        this.fileID = -1;
    }

    public int getFileID() {
        return this.fileID;
    }

    public String getFilePath() {
        return this.pkgFile;
    }

    public void setFileID(int id2) {
        this.fileID = id2;
    }

    public void setFilePath(String file) {
        this.pkgFile = file;
    }

    public QPKGFileSource(String file, int id2) {
        this.pkgFile = file;
        this.fileID = id2;
    }
}
