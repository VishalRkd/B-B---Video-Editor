package xiaoying.utils;

import android.content.ContentResolver;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import java.io.IOException;

/* JADX INFO: loaded from: classes19.dex */
public class QStreamContent {
    public static final String CONTENT_THEME = "content://";
    public static ContentResolver resolver;
    private static IQUriTransformer uriTransformer;

    private QStreamContent() {
    }

    public static String TransUri2Path(String strUri) {
        IQUriTransformer iQUriTransformer = uriTransformer;
        if (iQUriTransformer == null) {
            return null;
        }
        return iQUriTransformer.TransUri2Path(strUri);
    }

    public static int content_Init(String strTheme, Object contentResolver) {
        resolver = (ContentResolver) contentResolver;
        return native_Init(strTheme);
    }

    public static native int native_Init(String strTheme);

    public static int openFileFd(String filePath) {
        if (resolver == null) {
            return -1;
        }
        try {
            ParcelFileDescriptor parcelFileDescriptorOpenFileDescriptor = resolver.openFileDescriptor(Uri.parse(filePath), "r");
            if (parcelFileDescriptorOpenFileDescriptor != null) {
                return parcelFileDescriptorOpenFileDescriptor.detachFd();
            }
            return 0;
        } catch (IOException unused) {
            return -1;
        }
    }

    public static int setUriTranformer(Object data) {
        uriTransformer = (IQUriTransformer) data;
        return 0;
    }
}
