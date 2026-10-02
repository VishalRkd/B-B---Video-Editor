package xiaoying.utils;

import android.graphics.Bitmap;

/* JADX INFO: loaded from: classes19.dex */
public final class QBitmapFactory {
    public static QBitmap constructQBitmapFromNativeHandle_noSkia(long nativeQBmpHandle) {
        return new QBitmap(nativeQBmpHandle);
    }

    public static QBitmap createQBitmapBlank(int width, int height, int colorSpace) {
        try {
            QBitmap qBitmap = new QBitmap(0L, true, false);
            if (native_BitmapAlloc(qBitmap, colorSpace, width, height) != 0) {
                return null;
            }
            Bitmap.Config config = Bitmap.Config.ARGB_8888;
            if (colorSpace == 1677721600) {
                config = Bitmap.Config.ALPHA_8;
            } else if (colorSpace == QColorSpace.QPAF_RGB16_R5G6B5) {
                config = Bitmap.Config.RGB_565;
            } else if (colorSpace == QColorSpace.QPAF_RGB16_R4G4B4) {
                config = Bitmap.Config.ARGB_4444;
            } else {
                int i10 = QColorSpace.QPAF_RGB32_A8R8G8B8;
            }
            qBitmap.setBitmap(Bitmap.createBitmap(width, height, config));
            return qBitmap;
        } catch (Throwable th2) {
            th2.printStackTrace();
            return null;
        }
    }

    public static QBitmap createQBitmapBlank_noSkia(int width, int height, int colorSpace) {
        try {
            QBitmap qBitmap = new QBitmap(0L, true, false);
            if (native_BitmapAlloc(qBitmap, colorSpace, width, height) == 0) {
                return qBitmap;
            }
            return null;
        } catch (Throwable th2) {
            th2.printStackTrace();
            return null;
        }
    }

    public static QBitmap createQBitmapFromBitmap(Bitmap bitmap) {
        int i10 = QColorSpace.QPAF_RGB32_A8R8G8B8;
        if (bitmap == null) {
            return null;
        }
        try {
            QBitmap qBitmap = new QBitmap(0L, true, false);
            Bitmap.Config config = bitmap.getConfig();
            int width = bitmap.getWidth();
            int height = bitmap.getHeight();
            if (width != 0 && height != 0) {
                if (config == Bitmap.Config.ALPHA_8) {
                    i10 = QColorSpace.QPAF_GRAY8;
                } else if (config == Bitmap.Config.ARGB_8888) {
                    i10 = QColorSpace.QPAF_RGB32_A8R8G8B8;
                }
                if (native_BitmapAlloc(qBitmap, i10, width, height) == 0 && qBitmap.copyFromAndroidBitmap(bitmap) == 0) {
                    return qBitmap;
                }
                return null;
            }
            return null;
        } catch (Throwable th2) {
            th2.printStackTrace();
            return null;
        }
    }

    public static QBitmap createQBitmapFromNative(long nativeHandle) {
        try {
            return new QBitmap(nativeHandle, false, true);
        } catch (Throwable th2) {
            th2.printStackTrace();
            return null;
        }
    }

    public static QBitmap createQBitmapFromQBitmap(QBitmap srcQBitmap, boolean sharedMemory) {
        if (sharedMemory) {
            return srcQBitmap;
        }
        try {
            if (srcQBitmap.getBitmap() != null) {
                return new QBitmap(srcQBitmap.getBitmap());
            }
            return null;
        } catch (Throwable th2) {
            th2.printStackTrace();
            return null;
        }
    }

    public static QBitmap createQBitmapShareWithAndroidBitmap(int width, int height, int colorSpace) {
        try {
            QBitmap qBitmap = new QBitmap(true);
            Bitmap.Config config = Bitmap.Config.ARGB_8888;
            if (colorSpace == 1677721600) {
                config = Bitmap.Config.ALPHA_8;
            } else if (colorSpace == QColorSpace.QPAF_RGB16_R5G6B5) {
                config = Bitmap.Config.RGB_565;
            } else if (colorSpace == QColorSpace.QPAF_RGB16_R4G4B4) {
                config = Bitmap.Config.ARGB_4444;
            } else {
                int i10 = QColorSpace.QPAF_RGB32_A8R8G8B8;
            }
            Bitmap bitmapCreateBitmap = Bitmap.createBitmap(width, height, config);
            if (bitmapCreateBitmap == null) {
                return null;
            }
            qBitmap.setBitmap(bitmapCreateBitmap);
            if (native_ShareAndroidBitmapBufWithQBitmap(bitmapCreateBitmap, qBitmap) == 0) {
                return qBitmap;
            }
            bitmapCreateBitmap.recycle();
            return null;
        } catch (Throwable th2) {
            th2.printStackTrace();
            return null;
        }
    }

    private static native int native_BitmapAlloc(Object dstQBitmap, int pixelArrayFormat, int width, int height);

    private static native int native_ShareAndroidBitmapBufWithQBitmap(Bitmap sharedABmp, QBitmap mBmp);
}
