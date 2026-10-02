package xiaoying.engine.clip;

/* JADX INFO: loaded from: classes19.dex */
public class QMediaMulSource {
    public static final int TYPE_BITMAP = 1;
    public static final int TYPE_BUBBLETEXT = 2;
    public static final int TYPE_FILE = 0;
    public static final int TYPE_PKG_FILE = 3;
    public static final int TYPE_TRC_LYRICS = 4;
    private boolean isTempSource;
    private int mSourceCount;
    private Object[] source;
    private int type;

    public QMediaMulSource() {
        this.type = 0;
        this.isTempSource = false;
        this.source = null;
        this.mSourceCount = 0;
    }

    public Object[] getSource() {
        return this.source;
    }

    public int getSourceCount() {
        return this.mSourceCount;
    }

    public int getSourceType() {
        return this.type;
    }

    public boolean isTempSource() {
        return this.isTempSource;
    }

    public QMediaMulSource(int type, boolean isTempSource, Object[] source) {
        this.mSourceCount = 0;
        this.type = type;
        this.isTempSource = isTempSource;
        this.source = source;
        if (source != null) {
            this.mSourceCount = source.length;
        }
    }
}
