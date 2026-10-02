package xiaoying.engine.aecomp;

import xiaoying.engine.base.QSessionStreamOpenParam;
import xiaoying.engine.base.QVEError;

/* JADX INFO: loaded from: classes18.dex */
public class QAECompStream {
    public static final int DECODER_USAGE_TYPE_AUTO = 4;
    public static final int DECODER_USAGE_TYPE_HW = 1;
    public static final int DECODER_USAGE_TYPE_PLAYBACK_THUMBNAIL = 16;
    public static final int DECODER_USAGE_TYPE_SW = 2;
    public static final int DECODER_USAGE_TYPE_THUMBNAIL = 8;
    private static final long PROP_BASE = 2147483648L;
    public static final long PROP_WEBP_EXPORT_MODE = 2147483734L;
    public static final int SOURCE_TYPE_COMPOSITION = 4;
    public static final int SOURCE_TYPE_COMPOSITION_ORIGINAL = 5;
    public static final int SOURCE_TYPE_COMPOSITION_SESSION = 6;
    public static final int SOURCE_TYPE_COMPOSITION_SESSION_ORIGINAL = 7;
    public long handle = 0;

    private native int nativeClose(long handle);

    private native int nativeOpen(int sourceType, Object obj, QSessionStreamOpenParam sop);

    private native int nativeSetConfig(long sessionHandle, long propertyID, Object data);

    public int close() {
        long j10 = this.handle;
        return 0 == j10 ? QVEError.QERR_COMMON_JAVA_NOT_INIT : nativeClose(j10);
    }

    public int open(QAEBaseComp comp, QSessionStreamOpenParam sop) {
        return open(4, comp, sop);
    }

    public int setConfig(long propertyID, Object data) {
        return nativeSetConfig(this.handle, propertyID, data);
    }

    public int open(QAECompSession session, QSessionStreamOpenParam sop) {
        return open(6, session, sop);
    }

    public int open(int sourceType, QAEBaseComp comp, QSessionStreamOpenParam sop) {
        return 0 != this.handle ? QVEError.QERR_COMMON_JAVA_NOT_INIT : nativeOpen(sourceType, comp, sop);
    }

    public int open(int sourceType, QAECompSession session, QSessionStreamOpenParam sop) {
        return 0 != this.handle ? QVEError.QERR_COMMON_JAVA_NOT_INIT : nativeOpen(sourceType, session, sop);
    }
}
