package xiaoying.engine.base.pcm;

/* JADX INFO: loaded from: classes19.dex */
public class QPCMEParam {
    public static final int DATA_TYPE_FLOAT = 2;
    public static final int DATA_TYPE_SHORT = 1;
    public String audioFile = null;
    public int startPos = 0;
    public int len = -1;
    public boolean needLeft = false;
    public boolean needRight = false;
    public boolean needAmplify = false;
    public int dataType = 2;
    public QPCMEListener listener = null;
    public QPCMETurboSetting turboSetting = new QPCMETurboSetting();
}
