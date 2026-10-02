package xiaoying.engine.clip;

/* JADX INFO: loaded from: classes19.dex */
public class QMediaSource {
    public static final int TYPE_BITMAP = 1;
    public static final int TYPE_BUBBLETEXT = 2;
    public static final int TYPE_FACE_MORPHING = 16;
    public static final int TYPE_FILE = 0;
    public static final int TYPE_PKG_FILE = 3;
    public static final int TYPE_TRC_LYRICS = 4;
    private boolean isTempSource;
    private Object source;
    private int type;

    public QMediaSource(int type, boolean isTempSource, Object source) {
        this.type = type;
        this.isTempSource = isTempSource;
        this.source = source;
    }

    public Object getSource() {
        return this.source;
    }

    public int getSourceType() {
        return this.type;
    }

    public boolean isTempSource() {
        return this.isTempSource;
    }

    public QMediaSource() {
        this.type = 0;
        this.isTempSource = false;
        this.source = null;
    }
}
