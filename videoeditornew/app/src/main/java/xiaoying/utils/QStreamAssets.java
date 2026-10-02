package xiaoying.utils;

import android.content.res.AssetManager;

/* JADX INFO: loaded from: classes19.dex */
public class QStreamAssets {
    public static final String ASSETS_THEME = "assets_android://";
    public static AssetManager mAssetManager;

    private QStreamAssets() {
    }

    public static native int native_Init(String strTheme, Object assetManager);
}
