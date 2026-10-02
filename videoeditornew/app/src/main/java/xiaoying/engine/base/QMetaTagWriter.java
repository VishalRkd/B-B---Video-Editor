package xiaoying.engine.base;

/* JADX INFO: loaded from: classes19.dex */
public class QMetaTagWriter {
    public static int AddMetaTag(String strFilePath, QMetaTagData metaTagData) {
        return nativeAddMetaTag(strFilePath, metaTagData);
    }

    private static native int nativeAddMetaTag(String strFilePath, QMetaTagData metaTagData);
}
