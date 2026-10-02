package xiaoying.engine.base;

/* JADX INFO: loaded from: classes19.dex */
public class QSessionStream {
    public static final int DECODER_USAGE_TYPE_AUTO = 4;
    public static final int DECODER_USAGE_TYPE_HW = 1;
    public static final int DECODER_USAGE_TYPE_PLAYBACK_THUMBNAIL = 16;
    public static final int DECODER_USAGE_TYPE_SW = 2;
    public static final int DECODER_USAGE_TYPE_THUMBNAIL = 8;
    private static final long PROP_BASE = 2147483648L;
    public static final long PROP_DISPLAY_TRANSFORM = 2147483760L;
    public static final long PROP_HARDWARE_TEST_MODE = 2147483784L;
    public static final long PROP_WEBP_EXPORT_MODE = 2147483734L;
    public static final int SOURCE_TYPE_CLIP = 2;
    public static final int SOURCE_TYPE_STORYBOARD = 1;
    public static final int SOURCE_TYPE_STORYBOARD_ORIGINAL = 3;
    private long handle = 0;
    private int frameworkVersion = 0;

    private native int nativeClose();

    private native int nativeCloseAEWrapper();

    private native int nativeGetBGColor();

    private native int nativeGetBGColorAEWrapper();

    private native int nativeOpen(int sourceType, QSession session, QSessionStreamOpenParam sop);

    private native int nativeOpenAEWrapper(int sourceType, QSession session, QSessionStreamOpenParam sop);

    private native int nativeSetAlkFilePath(String strAlkFile);

    private native int nativeSetAlkFilePathAEWrapper(String strAlkFile);

    private native int nativeSetBGColor(int clrBG);

    private native int nativeSetBGColorAEWrapper(int clrBG);

    private native int nativeSetConfig(long sessionHandle, long propertyID, Object data);

    private native int nativeSetConfigAEWrapper(long sessionHandle, long propertyID, Object data);

    public int close() {
        return this.frameworkVersion == 393216 ? nativeCloseAEWrapper() : nativeClose();
    }

    public int getBGColor() {
        return this.frameworkVersion == 393216 ? nativeGetBGColorAEWrapper() : nativeGetBGColor();
    }

    public int open(int sourceType, QSession session, QSessionStreamOpenParam sop) {
        if (session != null) {
            this.frameworkVersion = session.frameworkVersion;
        }
        return (session == null || session.frameworkVersion != 393216) ? nativeOpen(sourceType, session, sop) : nativeOpenAEWrapper(sourceType, session, sop);
    }

    public int setAlkFilePath(String strAlkFile) {
        return this.frameworkVersion == 393216 ? nativeSetAlkFilePathAEWrapper(strAlkFile) : nativeSetAlkFilePath(strAlkFile);
    }

    public int setBGColor(int clrBG) {
        return this.frameworkVersion == 393216 ? nativeSetBGColorAEWrapper(clrBG) : nativeSetBGColor(clrBG);
    }

    public int setConfig(long propertyID, Object data) {
        return this.frameworkVersion == 393216 ? nativeSetConfigAEWrapper(this.handle, propertyID, data) : nativeSetConfig(this.handle, propertyID, data);
    }
}
