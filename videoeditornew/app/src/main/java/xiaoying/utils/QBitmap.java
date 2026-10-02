package xiaoying.utils;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

/* JADX INFO: loaded from: classes19.dex */
public final class QBitmap {
    private static final int FILE_FORMAT_JPEG = 2;
    private static final int FILE_FORMAT_PNG = 8;
    private static final int FILE_FORMAT_WBMP = 32;
    private static final int FLIP_MODE_HORIZ = 1;
    private static final int FLIP_MODE_VERT = 2;
    public static final int RESIZE_BILINEAR = 2;
    public static final int RESIZE_NEAREST_NEIGHBOUR = 1;
    private static final String TAG = "QBitmap";
    protected int mBitmap_ref;
    protected long mNativeBitmap;
    protected boolean mNeedFree;
    protected boolean mRecycled;
    protected int mRefCount;
    protected boolean mShared;
    protected boolean mSharedWithAndroidBmp;
    protected Bitmap m_SKBMP;

    /* JADX INFO: renamed from: xiaoying.utils.QBitmap$1 */
    public static /* synthetic */ class C480301 {
        static final /* synthetic */ int[] $SwitchMap$android$graphics$Bitmap$Config;

        static {
            int[] iArr = new int[Bitmap.Config.values().length];
            $SwitchMap$android$graphics$Bitmap$Config = iArr;
            try {
                iArr[Bitmap.Config.ALPHA_8.ordinal()] = 1;
            } catch (NoSuchFieldError unused) {
            }
            try {
                $SwitchMap$android$graphics$Bitmap$Config[Bitmap.Config.ARGB_4444.ordinal()] = 2;
            } catch (NoSuchFieldError unused2) {
            }
            try {
                $SwitchMap$android$graphics$Bitmap$Config[Bitmap.Config.ARGB_8888.ordinal()] = 3;
            } catch (NoSuchFieldError unused3) {
            }
            try {
                $SwitchMap$android$graphics$Bitmap$Config[Bitmap.Config.RGB_565.ordinal()] = 4;
            } catch (NoSuchFieldError unused4) {
            }
        }
    }

    private QBitmap() {
        this.m_SKBMP = null;
        this.mRecycled = false;
        this.mShared = false;
        this.mSharedWithAndroidBmp = false;
        this.mRefCount = 0;
        this.mNeedFree = false;
        this.mNativeBitmap = 0L;
    }

    private void checkRecycled(String errorMessage) {
        if (this.mRecycled) {
            throw new IllegalStateException(errorMessage);
        }
    }

    private int free() {
        try {
            if (this.mSharedWithAndroidBmp) {
                int i10 = this.mRefCount - 1;
                this.mRefCount = i10;
                if (i10 <= 0) {
                    Bitmap bitmap = this.m_SKBMP;
                    if (bitmap != null && !bitmap.isRecycled()) {
                        this.m_SKBMP.recycle();
                        this.m_SKBMP = null;
                    }
                    native_FreeQBitmapStructOnly(this.mNativeBitmap);
                    this.mNativeBitmap = 0L;
                }
                return 0;
            }
            long j10 = this.mNativeBitmap;
            if (j10 != 0) {
                if (this.mNeedFree) {
                    native_BitmapFree(this);
                } else {
                    native_FreeQBitmapStructOnly(j10);
                }
                this.mNativeBitmap = 0L;
            }
            Bitmap bitmap2 = this.m_SKBMP;
            if (bitmap2 != null) {
                if (!bitmap2.isRecycled()) {
                    this.m_SKBMP.recycle();
                }
                this.m_SKBMP = null;
            }
            return 0;
        } catch (Throwable th2) {
            th2.printStackTrace();
            return 2;
        }
    }

    private void lockRefCount() {
        if (this.mSharedWithAndroidBmp) {
            this.mRefCount++;
        }
    }

    private native int native_BitmapFree(Object mBitmap);

    private native int native_FreeQBitmapStructOnly(long nativeQBitmap);

    private native int native_GetBitmapColorSpace(long nativeBitmap);

    private native int native_GetBitmapHeight(long nativeBitmap);

    private native int[] native_GetBitmapPointColorValue(int x10, int y10, long nativeBitmap);

    private native int[] native_GetBitmapRowBytes(long nativeBitmap);

    private native int native_GetBitmapWidth(long nativeBitmap);

    private native int native_copyFromAndroidBitmap(Bitmap ABmp);

    public static Bitmap setAlpha(Bitmap sourceImg, Bitmap maskImg, int maskleft, int masktop, int number) {
        int width;
        int height;
        int[] iArr;
        if (sourceImg == null) {
            return null;
        }
        try {
            if (maskImg != null) {
                Bitmap.Config config = maskImg.getConfig();
                Bitmap.Config config2 = Bitmap.Config.ALPHA_8;
                if (config != config2 && maskImg.getConfig() != Bitmap.Config.ARGB_8888) {
                    return null;
                }
                width = Math.min(sourceImg.getWidth() - maskleft, maskImg.getWidth());
                height = Math.min(sourceImg.getHeight() - masktop, maskImg.getHeight());
                int i10 = width * height;
                int[] iArr2 = new int[i10];
                sourceImg.getPixels(iArr2, 0, sourceImg.getWidth(), maskleft, masktop, width, height);
                if (maskImg.getConfig() == config2) {
                    byte[] bArr = new byte[maskImg.getRowBytes() * maskImg.getHeight()];
                    maskImg.copyPixelsToBuffer(ByteBuffer.wrap(bArr));
                    for (int i11 = 0; i11 < i10; i11++) {
                        iArr2[i11] = ((((255 - (bArr[i11] & 255)) * number) / 100) << 24) | (iArr2[i11] & 16777215);
                    }
                } else {
                    int[] iArr3 = new int[maskImg.getWidth() * maskImg.getHeight()];
                    maskImg.getPixels(iArr3, 0, maskImg.getWidth(), 0, 0, maskImg.getWidth(), maskImg.getHeight());
                    for (int i12 = 0; i12 < i10; i12++) {
                        iArr2[i12] = ((((255 - (iArr3[i12] & 255)) * number) / 100) << 24) | (iArr2[i12] & 16777215);
                    }
                }
                iArr = iArr2;
            } else {
                width = sourceImg.getWidth();
                height = sourceImg.getHeight();
                int i13 = width * height;
                int[] iArr4 = new int[i13];
                sourceImg.getPixels(iArr4, 0, sourceImg.getWidth(), 0, 0, width, height);
                int i14 = (number * 255) / 100;
                for (int i15 = 0; i15 < i13; i15++) {
                    iArr4[i15] = (i14 << 24) | (iArr4[i15] & 16777215);
                }
                iArr = iArr4;
            }
            return Bitmap.createBitmap(iArr, width, height, Bitmap.Config.ARGB_8888);
        } catch (Throwable th2) {
            th2.printStackTrace();
            return null;
        }
    }

    private void unlockRefCount() {
        if (this.mSharedWithAndroidBmp) {
            if (this.mRecycled) {
                free();
            } else {
                this.mRefCount--;
            }
        }
    }

    public int colorConvert(QBitmap dstQBitmap) {
        Bitmap bitmap;
        try {
            if (this.m_SKBMP != null && dstQBitmap != null && (bitmap = dstQBitmap.m_SKBMP) != null) {
                dstQBitmap.m_SKBMP = this.m_SKBMP.copy(bitmap.getConfig(), false);
                return 0;
            }
            return 2;
        } catch (Throwable th2) {
            th2.printStackTrace();
            return 2;
        }
    }

    public int copyFromAndroidBitmap(Bitmap ABmp) {
        return native_copyFromAndroidBitmap(ABmp);
    }

    public int crop(QBitmap dstQBitmap, QRect crop) {
        try {
            Bitmap bitmap = this.m_SKBMP;
            if (bitmap != null && dstQBitmap != null && crop != null) {
                int i10 = crop.left;
                int i11 = crop.top;
                dstQBitmap.m_SKBMP = Bitmap.createBitmap(bitmap, i10, i11, crop.right - i10, crop.bottom - i11);
                return 0;
            }
            return 2;
        } catch (Throwable th2) {
            th2.printStackTrace();
            return 0;
        }
    }

    public int cropRotFlipResample(QBitmap dstQBitmap, QRect srcRect, QRect dstRect, int degree, int flipMode) {
        try {
            if (this.m_SKBMP != null && dstQBitmap != null && srcRect != null && dstRect != null) {
                QBitmap qBitmap = new QBitmap();
                if (crop(qBitmap, srcRect) != 0) {
                    return 1;
                }
                QBitmap qBitmap2 = new QBitmap();
                if (qBitmap.rotate(qBitmap2, degree) != 0) {
                    return 1;
                }
                QBitmap qBitmap3 = new QBitmap();
                qBitmap2.flip(qBitmap3, flipMode);
                qBitmap3.resample(dstQBitmap, 0);
                if (!qBitmap.isRecycled()) {
                    qBitmap.recycle();
                }
                if (!qBitmap2.isRecycled()) {
                    qBitmap2.recycle();
                }
                if (!qBitmap3.isRecycled()) {
                    qBitmap3.recycle();
                }
                return 0;
            }
            return 2;
        } catch (Throwable th2) {
            th2.printStackTrace();
            return 2;
        }
    }

    public int fillColor(int fillClr, QRect fillRect, QBitmap maskQBitmap, int opacity) {
        if (fillRect == null) {
            return 2;
        }
        try {
            if (this.m_SKBMP == null) {
                return 2;
            }
            Paint paint = new Paint();
            paint.setColor(fillClr);
            paint.setAlpha((opacity * 255) / 100);
            new Canvas(this.m_SKBMP).drawRect(new Rect(fillRect.left, fillRect.top, fillRect.right, fillRect.bottom), paint);
            return 0;
        } catch (Throwable th2) {
            th2.printStackTrace();
            return 0;
        }
    }

    public void finalize() throws Throwable {
        try {
            free();
        } catch (Throwable unused) {
        }
        super.finalize();
    }

    public int flip(QBitmap dstQBitmap, int flipMode) {
        try {
            if (this.m_SKBMP != null && dstQBitmap != null) {
                Matrix matrix = new Matrix();
                if (flipMode == 1) {
                    matrix.preScale(-1.0f, 1.0f);
                } else if (flipMode == 2) {
                    matrix.preScale(1.0f, -1.0f);
                }
                Bitmap bitmap = this.m_SKBMP;
                dstQBitmap.m_SKBMP = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), this.m_SKBMP.getHeight(), matrix, false);
                return 0;
            }
            return 2;
        } catch (Throwable th2) {
            th2.printStackTrace();
            return 0;
        }
    }

    public Bitmap getBitmap() {
        return this.m_SKBMP;
    }

    public int[] getBitmapPointColorValue(int x10, int y10) {
        return native_GetBitmapPointColorValue(x10, y10, this.mNativeBitmap);
    }

    public final int getColorSpace() {
        Bitmap.Config config = Bitmap.Config.ARGB_8888;
        int iNative_GetBitmapColorSpace = 0;
        try {
            Bitmap bitmap = this.m_SKBMP;
            if (bitmap != null) {
                int i10 = C480301.$SwitchMap$android$graphics$Bitmap$Config[bitmap.getConfig().ordinal()];
                if (i10 == 1) {
                    iNative_GetBitmapColorSpace = QColorSpace.QPAF_GRAY8;
                } else if (i10 == 2) {
                    iNative_GetBitmapColorSpace = QColorSpace.QPAF_RGB16_R4G4B4;
                } else if (i10 == 3) {
                    iNative_GetBitmapColorSpace = QColorSpace.QPAF_RGB32_A8R8G8B8;
                } else if (i10 == 4) {
                    iNative_GetBitmapColorSpace = QColorSpace.QPAF_RGB16_R5G6B5;
                }
            } else {
                iNative_GetBitmapColorSpace = native_GetBitmapColorSpace(this.mNativeBitmap);
            }
        } catch (Throwable th2) {
            th2.printStackTrace();
        }
        return iNative_GetBitmapColorSpace;
    }

    public final int getHeight() {
        try {
            Bitmap bitmap = this.m_SKBMP;
            return bitmap != null ? bitmap.getHeight() : native_GetBitmapHeight(this.mNativeBitmap);
        } catch (Throwable th2) {
            th2.printStackTrace();
            return 0;
        }
    }

    public final int[] getRowBytes() {
        try {
            Bitmap bitmap = this.m_SKBMP;
            return bitmap != null ? new int[]{bitmap.getRowBytes()} : native_GetBitmapRowBytes(this.mNativeBitmap);
        } catch (Throwable th2) {
            th2.printStackTrace();
            return null;
        }
    }

    public final int getWidth() {
        try {
            Bitmap bitmap = this.m_SKBMP;
            return bitmap != null ? bitmap.getWidth() : native_GetBitmapWidth(this.mNativeBitmap);
        } catch (Throwable th2) {
            th2.printStackTrace();
            return 0;
        }
    }

    public final boolean isRecycled() {
        return this.mRecycled;
    }

    public int merge(QBitmap foreQBitmap, QPoint forePos, QBitmap maskQBitmap, QPoint maskPos, int opacity) {
        Bitmap bitmap;
        try {
            if (this.m_SKBMP != null && foreQBitmap != null && (bitmap = foreQBitmap.m_SKBMP) != null && forePos != null) {
                Bitmap alpha = maskQBitmap != null ? setAlpha(bitmap, maskQBitmap.m_SKBMP, maskPos.x, maskPos.y, opacity) : setAlpha(bitmap, null, maskPos.x, maskPos.y, opacity);
                new Canvas(this.m_SKBMP).drawBitmap(alpha, forePos.x, forePos.y, (Paint) null);
                if (alpha.isRecycled()) {
                    return 0;
                }
                alpha.recycle();
                return 0;
            }
            return 2;
        } catch (Throwable th2) {
            th2.printStackTrace();
            return 0;
        }
    }

    public void recycle() {
        if (this.mRecycled) {
            return;
        }
        free();
        this.mRecycled = true;
    }

    public int resample(QBitmap dstQBitmap, int resampleAlg) {
        Bitmap bitmap;
        if (dstQBitmap == null) {
            return 2;
        }
        try {
            if (dstQBitmap.m_SKBMP != null && (bitmap = this.m_SKBMP) != null) {
                new Canvas(dstQBitmap.m_SKBMP).drawBitmap(this.m_SKBMP, new Rect(0, 0, bitmap.getWidth(), this.m_SKBMP.getHeight()), new Rect(0, 0, dstQBitmap.getWidth(), dstQBitmap.getHeight()), (Paint) null);
                return 0;
            }
            return 2;
        } catch (Throwable th2) {
            th2.printStackTrace();
            return 2;
        }
    }

    public int rotate(QBitmap dstQBitmap, int degree) {
        if (dstQBitmap == null) {
            return 2;
        }
        try {
            if (this.m_SKBMP == null) {
                return 2;
            }
            Matrix matrix = new Matrix();
            matrix.setRotate(degree);
            Bitmap bitmap = this.m_SKBMP;
            dstQBitmap.m_SKBMP = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), this.m_SKBMP.getHeight(), matrix, false);
            return 0;
        } catch (Throwable th2) {
            th2.printStackTrace();
            return 0;
        }
    }

    public int save(int imgFormat, QStream stream, int quality) {
        return 0;
    }

    public int setBitmap(Bitmap bmp) {
        this.m_SKBMP = bmp;
        return 0;
    }

    public int save(int imgFormat, String pathName, int quality) {
        try {
            if (this.m_SKBMP != null) {
                try {
                    FileOutputStream fileOutputStream = new FileOutputStream(pathName);
                    Bitmap.CompressFormat compressFormat = Bitmap.CompressFormat.JPEG;
                    if (imgFormat != 2 && imgFormat == 8) {
                        compressFormat = Bitmap.CompressFormat.PNG;
                    }
                    this.m_SKBMP.compress(compressFormat, quality, fileOutputStream);
                    try {
                        fileOutputStream.close();
                        return 0;
                    } catch (IOException e10) {
                        e10.printStackTrace();
                        return 1;
                    }
                } catch (IOException e11) {
                    e11.printStackTrace();
                    return 1;
                }
            }
        } catch (Throwable th2) {
            th2.printStackTrace();
        }
        return 1;
    }

    public QBitmap(Bitmap bmp) {
        this.m_SKBMP = null;
        this.mNeedFree = false;
        this.mRecycled = false;
        this.mShared = false;
        this.mSharedWithAndroidBmp = false;
        this.mRefCount = 0;
        try {
            this.m_SKBMP = bmp.copy(bmp.getConfig(), false);
        } catch (Throwable th2) {
            th2.printStackTrace();
        }
        this.mNeedFree = false;
        this.mNativeBitmap = 0L;
    }

    public QBitmap(long nativeQBitmap, boolean needFree, boolean shared) {
        this.m_SKBMP = null;
        this.mRecycled = false;
        this.mSharedWithAndroidBmp = false;
        this.mRefCount = 0;
        this.mNativeBitmap = nativeQBitmap;
        this.mNeedFree = needFree;
        this.mShared = shared;
        if (nativeQBitmap != 0) {
            try {
                this.m_SKBMP = QAndroidBitmapFactory.createBitmapFromQBitmap(this, shared);
            } catch (Throwable unused) {
            }
        }
    }

    public QBitmap(long nativeQBitmapHandle) {
        this.m_SKBMP = null;
        this.mRecycled = false;
        this.mShared = false;
        this.mSharedWithAndroidBmp = false;
        this.mRefCount = 0;
        this.mNativeBitmap = nativeQBitmapHandle;
        this.mNeedFree = false;
    }

    public QBitmap(boolean shareWithAndroidBitmap) {
        this.m_SKBMP = null;
        this.mNeedFree = false;
        this.mRecycled = false;
        this.mShared = false;
        this.mRefCount = 0;
        this.mNativeBitmap = 0L;
        this.mSharedWithAndroidBmp = shareWithAndroidBitmap;
    }
}
