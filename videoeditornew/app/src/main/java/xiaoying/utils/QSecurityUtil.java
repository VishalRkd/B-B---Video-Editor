package xiaoying.utils;

/* JADX INFO: loaded from: classes19.dex */
public class QSecurityUtil {
    public static native String makeAppSecretKey(String appKey, String date, String mac);

    public static native String makeDigestKey(String appKey, String appSecretKey, String auid);

    public static native String makeDigestMethod(String strSeed);

    public static native String makeSecretKey(String strSeed);
}
