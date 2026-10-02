package xiaoying.utils;

import android.graphics.Bitmap;

/* JADX INFO: loaded from: classes19.dex */
public final class QAndroidBitmapFactory {
    public static Bitmap createBitmapFromQBitmap(QBitmap srcQBitmap, boolean sharedMemory) {
        if (srcQBitmap == null) {
            return null;
        }
        try {
            if (srcQBitmap.getBitmap() != null && sharedMemory) {
                return Bitmap.createBitmap(srcQBitmap.getBitmap());
            }
            int colorSpace = srcQBitmap.getColorSpace();
            Bitmap.Config config = Bitmap.Config.ARGB_8888;
            if (QColorSpace.QPAF_RGB32_A8R8G8B8 != colorSpace) {
                if (QColorSpace.QPAF_RGB16_R5G6B5 == colorSpace) {
                    config = Bitmap.Config.RGB_565;
                } else {
                    if (1677721600 != colorSpace) {
                        return null;
                    }
                    config = Bitmap.Config.ALPHA_8;
                }
            }
            Bitmap bitmapCreateBitmap = Bitmap.createBitmap(srcQBitmap.getWidth(), srcQBitmap.getHeight(), config);
            transformQBitmapIntoBitmap(srcQBitmap, bitmapCreateBitmap);
            return bitmapCreateBitmap;
        } catch (Throwable th2) {
            th2.printStackTrace();
            return null;
        }
    }

    public static QBitmap createQBitmapFromBitmap(Bitmap srcBitmap, boolean sharedMemory) {
        try {
            return new QBitmap(srcBitmap);
        } catch (Throwable th2) {
            th2.printStackTrace();
            return null;
        }
    }

    private static native int native_TransformQBitmapIntoBitmap(Object srcQBitmap, Object dstBitmap);

    public static int transformQBitmapIntoBitmap(QBitmap srcQBitmap, Bitmap dstBitmap) {
        if (srcQBitmap != null && dstBitmap != null) {
            try {
                return native_TransformQBitmapIntoBitmap(srcQBitmap, dstBitmap);
            } catch (Throwable th2) {
                th2.printStackTrace();
            }
        }
        return 2;
    }
}
