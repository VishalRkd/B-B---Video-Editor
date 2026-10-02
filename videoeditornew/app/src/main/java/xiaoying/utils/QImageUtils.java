package xiaoying.utils;

import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.media.ExifInterface;
import android.net.Uri;
import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

/* JADX INFO: loaded from: classes19.dex */
public class QImageUtils {
    private static final int EXIF_AGID_ORIENTATION = 274;
    private static final int FILE_FORMAT_JPEG = 2;
    private static final int FILE_FORMAT_PNG = 8;
    private static final int FILE_FORMAT_WBMP = 32;
    private static final int FLIP_MODE_HORIZ = 1;
    private static final int FLIP_MODE_VERT = 2;
    private static final String TAG = "QImageUtils";

    /* JADX INFO: renamed from: xiaoying.utils.QImageUtils$1 */
    public static /* synthetic */ class C480311 {
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

    public static Bitmap BitmapCropRotFlipResample(Bitmap srcbmp, Rect rtSrc, Rect rtDst, float Degrees, int flipmode) {
        Bitmap bitmapRotateBitmap;
        Bitmap bitmapFlipBitmap;
        Bitmap bitmapResampleBitmap = null;
        if (srcbmp != null && rtSrc != null && rtDst != null) {
            try {
                Bitmap bitmapCropBitmap = CropBitmap(srcbmp, rtSrc);
                if (bitmapCropBitmap == null || (bitmapRotateBitmap = RotateBitmap(bitmapCropBitmap, Degrees)) == null || (bitmapFlipBitmap = FlipBitmap(bitmapRotateBitmap, flipmode)) == null) {
                    return null;
                }
                bitmapResampleBitmap = ResampleBitmap(bitmapFlipBitmap, rtDst.width(), rtDst.height());
                if (!bitmapCropBitmap.isRecycled()) {
                    bitmapCropBitmap.recycle();
                }
                if (!bitmapRotateBitmap.isRecycled()) {
                    bitmapRotateBitmap.recycle();
                }
                if (!bitmapFlipBitmap.isRecycled()) {
                    bitmapFlipBitmap.recycle();
                }
            } catch (Exception e10) {
                e10.printStackTrace();
            } catch (OutOfMemoryError e11) {
                e11.printStackTrace();
            }
        }
        return bitmapResampleBitmap;
    }

    /* JADX WARN: Code duplicated, block: B:17:0x0024  */
    public static int BitmapSave(Bitmap bmp, OutputStream os, int fileformat) {
        boolean zCompress;
        if (bmp == null || os == null) {
            zCompress = false;
        } else {
            try {
                if (fileformat == 2) {
                    zCompress = bmp.compress(Bitmap.CompressFormat.JPEG, 100, os);
                } else if (fileformat != 8) {
                    zCompress = false;
                } else {
                    zCompress = bmp.compress(Bitmap.CompressFormat.PNG, 100, os);
                }
            } catch (Exception e10) {
                e10.printStackTrace();
                return 1;
            }
        }
        return zCompress ? 0 : 1;
    }

    public static int BitmapSave2(Bitmap bmp, String pathName, int fileFormat) {
        try {
            FileOutputStream fileOutputStream = new FileOutputStream(pathName);
            int iBitmapSave = BitmapSave(bmp, fileOutputStream, fileFormat);
            fileOutputStream.close();
            return iBitmapSave;
        } catch (Exception e10) {
            e10.printStackTrace();
            return 1;
        }
    }

    public static void CloseInputFile(InputStream is) {
        if (is != null) {
            try {
                is.close();
            } catch (IOException e10) {
                e10.printStackTrace();
            }
        }
    }

    public static void CloseOutputFile(OutputStream os) {
        if (os != null) {
            try {
                os.close();
            } catch (IOException e10) {
                e10.printStackTrace();
            }
        }
    }

    public static Bitmap ColorConvert(Bitmap srcbmp, int dstcolor) {
        if (srcbmp == null) {
            return null;
        }
        try {
            Bitmap.Config config = Bitmap.Config.RGB_565;
            if (dstcolor == 1) {
                config = Bitmap.Config.ARGB_8888;
            } else if (dstcolor != 4) {
                if (dstcolor == 7) {
                    config = Bitmap.Config.ARGB_4444;
                } else if (dstcolor == 8) {
                    config = Bitmap.Config.ALPHA_8;
                }
            }
            return srcbmp.copy(config, false);
        } catch (Exception e10) {
            e10.printStackTrace();
            return null;
        } catch (OutOfMemoryError e11) {
            e11.printStackTrace();
            return null;
        }
    }

    public static Bitmap CreateBitmap(int width, int height, int color) {
        try {
            Bitmap.Config config = Bitmap.Config.ARGB_8888;
            if (color != 1) {
                if (color == 4) {
                    config = Bitmap.Config.RGB_565;
                } else if (color == 7) {
                    config = Bitmap.Config.ARGB_4444;
                } else if (color == 8) {
                    config = Bitmap.Config.ALPHA_8;
                }
            }
            return Bitmap.createBitmap(width, height, config);
        } catch (Exception unused) {
            return null;
        }
    }

    public static Bitmap CropBitmap(Bitmap srcbmp, Rect CropRect) {
        if (srcbmp == null || CropRect == null) {
            return null;
        }
        try {
            return Bitmap.createBitmap(srcbmp, CropRect.left, CropRect.top, CropRect.width(), CropRect.height());
        } catch (IllegalArgumentException e10) {
            e10.printStackTrace();
            return null;
        } catch (OutOfMemoryError e11) {
            e11.printStackTrace();
            return null;
        }
    }

    public static int FileReSize(String inpath, int width, int height, int maxfilesize, String outpath) {
        Bitmap bitmapCreateScaledBitmap;
        if (inpath != null && outpath != null) {
            try {
                Bitmap bitmapDecodeFile = BitmapFactory.decodeFile(inpath);
                if (bitmapDecodeFile == null || (bitmapCreateScaledBitmap = Bitmap.createScaledBitmap(bitmapDecodeFile, width, height, false)) == null) {
                    return 1;
                }
                return BitmapSave2(bitmapCreateScaledBitmap, outpath, 2);
            } catch (Exception e10) {
                e10.printStackTrace();
            } catch (OutOfMemoryError e11) {
                e11.printStackTrace();
                return 1;
            }
        }
        return 1;
    }

    public static int FillColor(Bitmap bmp, int clrFill, Rect FillRect, Bitmap maskbmp, long opacity) {
        if (bmp != null && FillRect != null) {
            try {
                Paint paint = new Paint();
                paint.setColor(clrFill);
                paint.setAlpha((((int) opacity) * 255) / 100);
                new Canvas(bmp).drawRect(FillRect, paint);
                return 0;
            } catch (Exception e10) {
                e10.printStackTrace();
            } catch (OutOfMemoryError e11) {
                e11.printStackTrace();
            }
        }
        return 1;
    }

    public static Bitmap FlipBitmap(Bitmap srcbmp, int flipmode) {
        if (srcbmp == null) {
            return null;
        }
        try {
            Matrix matrix = new Matrix();
            if (flipmode == 1) {
                matrix.preScale(-1.0f, 1.0f);
            } else if (flipmode == 2) {
                matrix.preScale(1.0f, -1.0f);
            }
            return Bitmap.createBitmap(srcbmp, 0, 0, srcbmp.getWidth(), srcbmp.getHeight(), matrix, false);
        } catch (IllegalArgumentException e10) {
            e10.printStackTrace();
            return null;
        } catch (OutOfMemoryError e11) {
            e11.printStackTrace();
            return null;
        }
    }

    public static int GetBitmapColor(Bitmap bmp) {
        if (bmp != null) {
            int i10 = C480311.$SwitchMap$android$graphics$Bitmap$Config[bmp.getConfig().ordinal()];
            if (i10 == 1) {
                return 8;
            }
            if (i10 == 2) {
                return 7;
            }
            if (i10 == 3) {
                return 1;
            }
            if (i10 == 4) {
                return 4;
            }
        }
        return 0;
    }

    public static int GetBitmapHeight(Bitmap bmp) {
        if (bmp != null) {
            return bmp.getHeight();
        }
        return 0;
    }

    public static int GetBitmapWidth(Bitmap bmp) {
        if (bmp != null) {
            return bmp.getWidth();
        }
        return 0;
    }

    public static Map<String, String> GetImageExifInfo(String strImgPath) {
        ExifInterface exifInterface;
        InputStream inputStreamOpen;
        if (strImgPath == null) {
            return null;
        }
        HashMap map = new HashMap();
        try {
            if (strImgPath.startsWith(QStreamContent.CONTENT_THEME)) {
                inputStreamOpen = QStreamContent.resolver.openInputStream(Uri.parse(strImgPath));
                exifInterface = new ExifInterface(inputStreamOpen);
            } else if (strImgPath.startsWith("assets_android://")) {
                AssetManager assetManager = QStreamAssets.mAssetManager;
                if (assetManager != null) {
                    inputStreamOpen = assetManager.open(strImgPath.replace("assets_android://", ""));
                    exifInterface = new ExifInterface(inputStreamOpen);
                } else {
                    inputStreamOpen = null;
                    exifInterface = null;
                }
            } else {
                exifInterface = new ExifInterface(strImgPath);
                inputStreamOpen = null;
            }
            if (exifInterface == null) {
                if (inputStreamOpen != null) {
                    inputStreamOpen.close();
                }
                return null;
            }
            String attribute = exifInterface.getAttribute(androidx.exifinterface.media.ExifInterface.TAG_APERTURE_VALUE);
            if (attribute != null) {
                map.put(androidx.exifinterface.media.ExifInterface.TAG_APERTURE_VALUE, attribute);
            }
            String attribute2 = exifInterface.getAttribute(androidx.exifinterface.media.ExifInterface.TAG_ARTIST);
            if (attribute2 != null) {
                map.put(androidx.exifinterface.media.ExifInterface.TAG_ARTIST, attribute2);
            }
            String attribute3 = exifInterface.getAttribute(androidx.exifinterface.media.ExifInterface.TAG_BITS_PER_SAMPLE);
            if (attribute3 != null) {
                map.put(androidx.exifinterface.media.ExifInterface.TAG_BITS_PER_SAMPLE, attribute3);
            }
            String attribute4 = exifInterface.getAttribute(androidx.exifinterface.media.ExifInterface.TAG_BRIGHTNESS_VALUE);
            if (attribute4 != null) {
                map.put(androidx.exifinterface.media.ExifInterface.TAG_BRIGHTNESS_VALUE, attribute4);
            }
            String attribute5 = exifInterface.getAttribute(androidx.exifinterface.media.ExifInterface.TAG_CFA_PATTERN);
            if (attribute5 != null) {
                map.put(androidx.exifinterface.media.ExifInterface.TAG_CFA_PATTERN, attribute5);
            }
            String attribute6 = exifInterface.getAttribute(androidx.exifinterface.media.ExifInterface.TAG_COLOR_SPACE);
            if (attribute6 != null) {
                map.put(androidx.exifinterface.media.ExifInterface.TAG_COLOR_SPACE, attribute6);
            }
            String attribute7 = exifInterface.getAttribute(androidx.exifinterface.media.ExifInterface.TAG_COMPONENTS_CONFIGURATION);
            if (attribute7 != null) {
                map.put(androidx.exifinterface.media.ExifInterface.TAG_COMPONENTS_CONFIGURATION, attribute7);
            }
            String attribute8 = exifInterface.getAttribute(androidx.exifinterface.media.ExifInterface.TAG_COMPRESSED_BITS_PER_PIXEL);
            if (attribute8 != null) {
                map.put(androidx.exifinterface.media.ExifInterface.TAG_COMPRESSED_BITS_PER_PIXEL, attribute8);
            }
            String attribute9 = exifInterface.getAttribute(androidx.exifinterface.media.ExifInterface.TAG_COMPRESSION);
            if (attribute9 != null) {
                map.put(androidx.exifinterface.media.ExifInterface.TAG_COMPRESSION, attribute9);
            }
            String attribute10 = exifInterface.getAttribute(androidx.exifinterface.media.ExifInterface.TAG_CONTRAST);
            if (attribute10 != null) {
                map.put(androidx.exifinterface.media.ExifInterface.TAG_CONTRAST, attribute10);
            }
            String attribute11 = exifInterface.getAttribute(androidx.exifinterface.media.ExifInterface.TAG_COPYRIGHT);
            if (attribute11 != null) {
                map.put(androidx.exifinterface.media.ExifInterface.TAG_COPYRIGHT, attribute11);
            }
            String attribute12 = exifInterface.getAttribute(androidx.exifinterface.media.ExifInterface.TAG_CUSTOM_RENDERED);
            if (attribute12 != null) {
                map.put(androidx.exifinterface.media.ExifInterface.TAG_CUSTOM_RENDERED, attribute12);
            }
            String attribute13 = exifInterface.getAttribute(androidx.exifinterface.media.ExifInterface.TAG_DATETIME);
            if (attribute13 != null) {
                map.put(androidx.exifinterface.media.ExifInterface.TAG_DATETIME, attribute13);
            }
            String attribute14 = exifInterface.getAttribute(androidx.exifinterface.media.ExifInterface.TAG_DATETIME_DIGITIZED);
            if (attribute14 != null) {
                map.put(androidx.exifinterface.media.ExifInterface.TAG_DATETIME_DIGITIZED, attribute14);
            }
            String attribute15 = exifInterface.getAttribute(androidx.exifinterface.media.ExifInterface.TAG_DATETIME_ORIGINAL);
            if (attribute15 != null) {
                map.put(androidx.exifinterface.media.ExifInterface.TAG_DATETIME_ORIGINAL, attribute15);
            }
            String attribute16 = exifInterface.getAttribute(androidx.exifinterface.media.ExifInterface.TAG_DEFAULT_CROP_SIZE);
            if (attribute16 != null) {
                map.put(androidx.exifinterface.media.ExifInterface.TAG_DEFAULT_CROP_SIZE, attribute16);
            }
            String attribute17 = exifInterface.getAttribute(androidx.exifinterface.media.ExifInterface.TAG_DEVICE_SETTING_DESCRIPTION);
            if (attribute17 != null) {
                map.put(androidx.exifinterface.media.ExifInterface.TAG_DEVICE_SETTING_DESCRIPTION, attribute17);
            }
            String attribute18 = exifInterface.getAttribute(androidx.exifinterface.media.ExifInterface.TAG_DIGITAL_ZOOM_RATIO);
            if (attribute18 != null) {
                map.put(androidx.exifinterface.media.ExifInterface.TAG_DIGITAL_ZOOM_RATIO, attribute18);
            }
            String attribute19 = exifInterface.getAttribute(androidx.exifinterface.media.ExifInterface.TAG_DNG_VERSION);
            if (attribute19 != null) {
                map.put(androidx.exifinterface.media.ExifInterface.TAG_DNG_VERSION, attribute19);
            }
            String attribute20 = exifInterface.getAttribute(androidx.exifinterface.media.ExifInterface.TAG_EXIF_VERSION);
            if (attribute20 != null) {
                map.put(androidx.exifinterface.media.ExifInterface.TAG_EXIF_VERSION, attribute20);
            }
            String attribute21 = exifInterface.getAttribute(androidx.exifinterface.media.ExifInterface.TAG_EXPOSURE_BIAS_VALUE);
            if (attribute21 != null) {
                map.put(androidx.exifinterface.media.ExifInterface.TAG_EXPOSURE_BIAS_VALUE, attribute21);
            }
            String attribute22 = exifInterface.getAttribute(androidx.exifinterface.media.ExifInterface.TAG_EXPOSURE_INDEX);
            if (attribute22 != null) {
                map.put(androidx.exifinterface.media.ExifInterface.TAG_EXPOSURE_INDEX, attribute22);
            }
            String attribute23 = exifInterface.getAttribute(androidx.exifinterface.media.ExifInterface.TAG_EXPOSURE_MODE);
            if (attribute23 != null) {
                map.put(androidx.exifinterface.media.ExifInterface.TAG_EXPOSURE_MODE, attribute23);
            }
            String attribute24 = exifInterface.getAttribute(androidx.exifinterface.media.ExifInterface.TAG_EXPOSURE_PROGRAM);
            if (attribute24 != null) {
                map.put(androidx.exifinterface.media.ExifInterface.TAG_EXPOSURE_PROGRAM, attribute24);
            }
            String attribute25 = exifInterface.getAttribute(androidx.exifinterface.media.ExifInterface.TAG_EXPOSURE_TIME);
            if (attribute25 != null) {
                map.put(androidx.exifinterface.media.ExifInterface.TAG_EXPOSURE_TIME, attribute25);
            }
            String attribute26 = exifInterface.getAttribute(androidx.exifinterface.media.ExifInterface.TAG_FILE_SOURCE);
            if (attribute26 != null) {
                map.put(androidx.exifinterface.media.ExifInterface.TAG_FILE_SOURCE, attribute26);
            }
            String attribute27 = exifInterface.getAttribute(androidx.exifinterface.media.ExifInterface.TAG_FLASH);
            if (attribute27 != null) {
                map.put(androidx.exifinterface.media.ExifInterface.TAG_FLASH, attribute27);
            }
            String attribute28 = exifInterface.getAttribute(androidx.exifinterface.media.ExifInterface.TAG_FLASHPIX_VERSION);
            if (attribute28 != null) {
                map.put(androidx.exifinterface.media.ExifInterface.TAG_FLASHPIX_VERSION, attribute28);
            }
            String attribute29 = exifInterface.getAttribute(androidx.exifinterface.media.ExifInterface.TAG_FLASH_ENERGY);
            if (attribute29 != null) {
                map.put(androidx.exifinterface.media.ExifInterface.TAG_FLASH_ENERGY, attribute29);
            }
            String attribute30 = exifInterface.getAttribute(androidx.exifinterface.media.ExifInterface.TAG_FOCAL_LENGTH);
            if (attribute30 != null) {
                map.put(androidx.exifinterface.media.ExifInterface.TAG_FOCAL_LENGTH, attribute30);
            }
            String attribute31 = exifInterface.getAttribute(androidx.exifinterface.media.ExifInterface.TAG_FOCAL_LENGTH_IN_35MM_FILM);
            if (attribute31 != null) {
                map.put(androidx.exifinterface.media.ExifInterface.TAG_FOCAL_LENGTH_IN_35MM_FILM, attribute31);
            }
            String attribute32 = exifInterface.getAttribute(androidx.exifinterface.media.ExifInterface.TAG_FOCAL_PLANE_RESOLUTION_UNIT);
            if (attribute32 != null) {
                map.put(androidx.exifinterface.media.ExifInterface.TAG_FOCAL_PLANE_RESOLUTION_UNIT, attribute32);
            }
            String attribute33 = exifInterface.getAttribute(androidx.exifinterface.media.ExifInterface.TAG_FOCAL_PLANE_X_RESOLUTION);
            if (attribute33 != null) {
                map.put(androidx.exifinterface.media.ExifInterface.TAG_FOCAL_PLANE_X_RESOLUTION, attribute33);
            }
            String attribute34 = exifInterface.getAttribute(androidx.exifinterface.media.ExifInterface.TAG_FOCAL_PLANE_Y_RESOLUTION);
            if (attribute34 != null) {
                map.put(androidx.exifinterface.media.ExifInterface.TAG_FOCAL_PLANE_Y_RESOLUTION, attribute34);
            }
            String attribute35 = exifInterface.getAttribute(androidx.exifinterface.media.ExifInterface.TAG_F_NUMBER);
            if (attribute35 != null) {
                map.put(androidx.exifinterface.media.ExifInterface.TAG_F_NUMBER, attribute35);
            }
            String attribute36 = exifInterface.getAttribute(androidx.exifinterface.media.ExifInterface.TAG_GAIN_CONTROL);
            if (attribute36 != null) {
                map.put(androidx.exifinterface.media.ExifInterface.TAG_GAIN_CONTROL, attribute36);
            }
            String attribute37 = exifInterface.getAttribute(androidx.exifinterface.media.ExifInterface.TAG_GPS_ALTITUDE);
            if (attribute37 != null) {
                map.put(androidx.exifinterface.media.ExifInterface.TAG_GPS_ALTITUDE, attribute37);
            }
            String attribute38 = exifInterface.getAttribute(androidx.exifinterface.media.ExifInterface.TAG_GPS_ALTITUDE_REF);
            if (attribute38 != null) {
                map.put(androidx.exifinterface.media.ExifInterface.TAG_GPS_ALTITUDE_REF, attribute38);
            }
            String attribute39 = exifInterface.getAttribute(androidx.exifinterface.media.ExifInterface.TAG_GPS_AREA_INFORMATION);
            if (attribute39 != null) {
                map.put(androidx.exifinterface.media.ExifInterface.TAG_GPS_AREA_INFORMATION, attribute39);
            }
            String attribute40 = exifInterface.getAttribute(androidx.exifinterface.media.ExifInterface.TAG_GPS_DATESTAMP);
            if (attribute40 != null) {
                map.put(androidx.exifinterface.media.ExifInterface.TAG_GPS_DATESTAMP, attribute40);
            }
            String attribute41 = exifInterface.getAttribute(androidx.exifinterface.media.ExifInterface.TAG_GPS_DEST_BEARING);
            if (attribute41 != null) {
                map.put(androidx.exifinterface.media.ExifInterface.TAG_GPS_DEST_BEARING, attribute41);
            }
            String attribute42 = exifInterface.getAttribute(androidx.exifinterface.media.ExifInterface.TAG_GPS_DEST_BEARING_REF);
            if (attribute42 != null) {
                map.put(androidx.exifinterface.media.ExifInterface.TAG_GPS_DEST_BEARING_REF, attribute42);
            }
            String attribute43 = exifInterface.getAttribute(androidx.exifinterface.media.ExifInterface.TAG_GPS_DEST_DISTANCE);
            if (attribute43 != null) {
                map.put(androidx.exifinterface.media.ExifInterface.TAG_GPS_DEST_DISTANCE, attribute43);
            }
            String attribute44 = exifInterface.getAttribute(androidx.exifinterface.media.ExifInterface.TAG_GPS_DEST_DISTANCE_REF);
            if (attribute44 != null) {
                map.put(androidx.exifinterface.media.ExifInterface.TAG_GPS_DEST_DISTANCE_REF, attribute44);
            }
            String attribute45 = exifInterface.getAttribute(androidx.exifinterface.media.ExifInterface.TAG_GPS_DEST_LATITUDE);
            if (attribute45 != null) {
                map.put(androidx.exifinterface.media.ExifInterface.TAG_GPS_DEST_LATITUDE, attribute45);
            }
            String attribute46 = exifInterface.getAttribute(androidx.exifinterface.media.ExifInterface.TAG_GPS_DEST_LATITUDE_REF);
            if (attribute46 != null) {
                map.put(androidx.exifinterface.media.ExifInterface.TAG_GPS_DEST_LATITUDE_REF, attribute46);
            }
            String attribute47 = exifInterface.getAttribute(androidx.exifinterface.media.ExifInterface.TAG_GPS_DEST_LONGITUDE);
            if (attribute47 != null) {
                map.put(androidx.exifinterface.media.ExifInterface.TAG_GPS_DEST_LONGITUDE, attribute47);
            }
            String attribute48 = exifInterface.getAttribute(androidx.exifinterface.media.ExifInterface.TAG_GPS_DEST_LONGITUDE_REF);
            if (attribute48 != null) {
                map.put(androidx.exifinterface.media.ExifInterface.TAG_GPS_DEST_LONGITUDE_REF, attribute48);
            }
            String attribute49 = exifInterface.getAttribute(androidx.exifinterface.media.ExifInterface.TAG_GPS_DIFFERENTIAL);
            if (attribute49 != null) {
                map.put(androidx.exifinterface.media.ExifInterface.TAG_GPS_DIFFERENTIAL, attribute49);
            }
            String attribute50 = exifInterface.getAttribute(androidx.exifinterface.media.ExifInterface.TAG_GPS_DOP);
            if (attribute50 != null) {
                map.put(androidx.exifinterface.media.ExifInterface.TAG_GPS_DOP, attribute50);
            }
            String attribute51 = exifInterface.getAttribute(androidx.exifinterface.media.ExifInterface.TAG_GPS_IMG_DIRECTION);
            if (attribute51 != null) {
                map.put(androidx.exifinterface.media.ExifInterface.TAG_GPS_IMG_DIRECTION, attribute51);
            }
            String attribute52 = exifInterface.getAttribute(androidx.exifinterface.media.ExifInterface.TAG_GPS_IMG_DIRECTION_REF);
            if (attribute52 != null) {
                map.put(androidx.exifinterface.media.ExifInterface.TAG_GPS_IMG_DIRECTION_REF, attribute52);
            }
            String attribute53 = exifInterface.getAttribute(androidx.exifinterface.media.ExifInterface.TAG_GPS_LATITUDE);
            if (attribute53 != null) {
                map.put(androidx.exifinterface.media.ExifInterface.TAG_GPS_LATITUDE, attribute53);
            }
            String attribute54 = exifInterface.getAttribute(androidx.exifinterface.media.ExifInterface.TAG_GPS_LATITUDE_REF);
            if (attribute54 != null) {
                map.put(androidx.exifinterface.media.ExifInterface.TAG_GPS_LATITUDE_REF, attribute54);
            }
            String attribute55 = exifInterface.getAttribute(androidx.exifinterface.media.ExifInterface.TAG_GPS_LONGITUDE);
            if (attribute55 != null) {
                map.put(androidx.exifinterface.media.ExifInterface.TAG_GPS_LONGITUDE, attribute55);
            }
            String attribute56 = exifInterface.getAttribute(androidx.exifinterface.media.ExifInterface.TAG_GPS_LONGITUDE_REF);
            if (attribute56 != null) {
                map.put(androidx.exifinterface.media.ExifInterface.TAG_GPS_LONGITUDE_REF, attribute56);
            }
            String attribute57 = exifInterface.getAttribute(androidx.exifinterface.media.ExifInterface.TAG_GPS_MAP_DATUM);
            if (attribute57 != null) {
                map.put(androidx.exifinterface.media.ExifInterface.TAG_GPS_MAP_DATUM, attribute57);
            }
            String attribute58 = exifInterface.getAttribute(androidx.exifinterface.media.ExifInterface.TAG_GPS_MEASURE_MODE);
            if (attribute58 != null) {
                map.put(androidx.exifinterface.media.ExifInterface.TAG_GPS_MEASURE_MODE, attribute58);
            }
            String attribute59 = exifInterface.getAttribute(androidx.exifinterface.media.ExifInterface.TAG_GPS_PROCESSING_METHOD);
            if (attribute59 != null) {
                map.put(androidx.exifinterface.media.ExifInterface.TAG_GPS_PROCESSING_METHOD, attribute59);
            }
            String attribute60 = exifInterface.getAttribute(androidx.exifinterface.media.ExifInterface.TAG_GPS_SATELLITES);
            if (attribute60 != null) {
                map.put(androidx.exifinterface.media.ExifInterface.TAG_GPS_SATELLITES, attribute60);
            }
            String attribute61 = exifInterface.getAttribute(androidx.exifinterface.media.ExifInterface.TAG_GPS_SPEED);
            if (attribute61 != null) {
                map.put(androidx.exifinterface.media.ExifInterface.TAG_GPS_SPEED, attribute61);
            }
            String attribute62 = exifInterface.getAttribute(androidx.exifinterface.media.ExifInterface.TAG_GPS_SPEED_REF);
            if (attribute62 != null) {
                map.put(androidx.exifinterface.media.ExifInterface.TAG_GPS_SPEED_REF, attribute62);
            }
            String attribute63 = exifInterface.getAttribute(androidx.exifinterface.media.ExifInterface.TAG_GPS_STATUS);
            if (attribute63 != null) {
                map.put(androidx.exifinterface.media.ExifInterface.TAG_GPS_STATUS, attribute63);
            }
            String attribute64 = exifInterface.getAttribute(androidx.exifinterface.media.ExifInterface.TAG_GPS_TIMESTAMP);
            if (attribute64 != null) {
                map.put(androidx.exifinterface.media.ExifInterface.TAG_GPS_TIMESTAMP, attribute64);
            }
            String attribute65 = exifInterface.getAttribute(androidx.exifinterface.media.ExifInterface.TAG_GPS_TRACK);
            if (attribute65 != null) {
                map.put(androidx.exifinterface.media.ExifInterface.TAG_GPS_TRACK, attribute65);
            }
            String attribute66 = exifInterface.getAttribute(androidx.exifinterface.media.ExifInterface.TAG_GPS_TRACK_REF);
            if (attribute66 != null) {
                map.put(androidx.exifinterface.media.ExifInterface.TAG_GPS_TRACK_REF, attribute66);
            }
            String attribute67 = exifInterface.getAttribute(androidx.exifinterface.media.ExifInterface.TAG_GPS_VERSION_ID);
            if (attribute67 != null) {
                map.put(androidx.exifinterface.media.ExifInterface.TAG_GPS_VERSION_ID, attribute67);
            }
            String attribute68 = exifInterface.getAttribute(androidx.exifinterface.media.ExifInterface.TAG_IMAGE_DESCRIPTION);
            if (attribute68 != null) {
                map.put(androidx.exifinterface.media.ExifInterface.TAG_IMAGE_DESCRIPTION, attribute68);
            }
            String attribute69 = exifInterface.getAttribute(androidx.exifinterface.media.ExifInterface.TAG_IMAGE_LENGTH);
            if (attribute69 != null) {
                map.put(androidx.exifinterface.media.ExifInterface.TAG_IMAGE_LENGTH, attribute69);
            }
            String attribute70 = exifInterface.getAttribute(androidx.exifinterface.media.ExifInterface.TAG_IMAGE_UNIQUE_ID);
            if (attribute70 != null) {
                map.put(androidx.exifinterface.media.ExifInterface.TAG_IMAGE_UNIQUE_ID, attribute70);
            }
            String attribute71 = exifInterface.getAttribute(androidx.exifinterface.media.ExifInterface.TAG_IMAGE_WIDTH);
            if (attribute71 != null) {
                map.put(androidx.exifinterface.media.ExifInterface.TAG_IMAGE_WIDTH, attribute71);
            }
            String attribute72 = exifInterface.getAttribute(androidx.exifinterface.media.ExifInterface.TAG_INTEROPERABILITY_INDEX);
            if (attribute72 != null) {
                map.put(androidx.exifinterface.media.ExifInterface.TAG_INTEROPERABILITY_INDEX, attribute72);
            }
            String attribute73 = exifInterface.getAttribute(androidx.exifinterface.media.ExifInterface.TAG_ISO_SPEED_RATINGS);
            if (attribute73 != null) {
                map.put(androidx.exifinterface.media.ExifInterface.TAG_ISO_SPEED_RATINGS, attribute73);
            }
            String attribute74 = exifInterface.getAttribute(androidx.exifinterface.media.ExifInterface.TAG_JPEG_INTERCHANGE_FORMAT);
            if (attribute74 != null) {
                map.put(androidx.exifinterface.media.ExifInterface.TAG_JPEG_INTERCHANGE_FORMAT, attribute74);
            }
            String attribute75 = exifInterface.getAttribute(androidx.exifinterface.media.ExifInterface.TAG_JPEG_INTERCHANGE_FORMAT_LENGTH);
            if (attribute75 != null) {
                map.put(androidx.exifinterface.media.ExifInterface.TAG_JPEG_INTERCHANGE_FORMAT_LENGTH, attribute75);
            }
            String attribute76 = exifInterface.getAttribute(androidx.exifinterface.media.ExifInterface.TAG_LIGHT_SOURCE);
            if (attribute76 != null) {
                map.put(androidx.exifinterface.media.ExifInterface.TAG_LIGHT_SOURCE, attribute76);
            }
            String attribute77 = exifInterface.getAttribute(androidx.exifinterface.media.ExifInterface.TAG_MAKE);
            if (attribute77 != null) {
                map.put(androidx.exifinterface.media.ExifInterface.TAG_MAKE, attribute77);
            }
            String attribute78 = exifInterface.getAttribute(androidx.exifinterface.media.ExifInterface.TAG_MAKER_NOTE);
            if (attribute78 != null) {
                map.put(androidx.exifinterface.media.ExifInterface.TAG_MAKER_NOTE, attribute78);
            }
            String attribute79 = exifInterface.getAttribute(androidx.exifinterface.media.ExifInterface.TAG_MAX_APERTURE_VALUE);
            if (attribute79 != null) {
                map.put(androidx.exifinterface.media.ExifInterface.TAG_MAX_APERTURE_VALUE, attribute79);
            }
            String attribute80 = exifInterface.getAttribute(androidx.exifinterface.media.ExifInterface.TAG_METERING_MODE);
            if (attribute80 != null) {
                map.put(androidx.exifinterface.media.ExifInterface.TAG_METERING_MODE, attribute80);
            }
            String attribute81 = exifInterface.getAttribute("Model");
            if (attribute81 != null) {
                map.put("Model", attribute81);
            }
            String attribute82 = exifInterface.getAttribute(androidx.exifinterface.media.ExifInterface.TAG_NEW_SUBFILE_TYPE);
            if (attribute82 != null) {
                map.put(androidx.exifinterface.media.ExifInterface.TAG_NEW_SUBFILE_TYPE, attribute82);
            }
            String attribute83 = exifInterface.getAttribute(androidx.exifinterface.media.ExifInterface.TAG_OECF);
            if (attribute83 != null) {
                map.put(androidx.exifinterface.media.ExifInterface.TAG_OECF, attribute83);
            }
            String attribute84 = exifInterface.getAttribute(androidx.exifinterface.media.ExifInterface.TAG_OFFSET_TIME);
            if (attribute84 != null) {
                map.put(androidx.exifinterface.media.ExifInterface.TAG_OFFSET_TIME, attribute84);
            }
            String attribute85 = exifInterface.getAttribute(androidx.exifinterface.media.ExifInterface.TAG_OFFSET_TIME_DIGITIZED);
            if (attribute85 != null) {
                map.put(androidx.exifinterface.media.ExifInterface.TAG_OFFSET_TIME_DIGITIZED, attribute85);
            }
            String attribute86 = exifInterface.getAttribute(androidx.exifinterface.media.ExifInterface.TAG_OFFSET_TIME_ORIGINAL);
            if (attribute86 != null) {
                map.put(androidx.exifinterface.media.ExifInterface.TAG_OFFSET_TIME_ORIGINAL, attribute86);
            }
            String attribute87 = exifInterface.getAttribute(androidx.exifinterface.media.ExifInterface.TAG_ORF_ASPECT_FRAME);
            if (attribute87 != null) {
                map.put(androidx.exifinterface.media.ExifInterface.TAG_ORF_ASPECT_FRAME, attribute87);
            }
            String attribute88 = exifInterface.getAttribute(androidx.exifinterface.media.ExifInterface.TAG_ORF_PREVIEW_IMAGE_LENGTH);
            if (attribute88 != null) {
                map.put(androidx.exifinterface.media.ExifInterface.TAG_ORF_PREVIEW_IMAGE_LENGTH, attribute88);
            }
            String attribute89 = exifInterface.getAttribute(androidx.exifinterface.media.ExifInterface.TAG_ORF_PREVIEW_IMAGE_START);
            if (attribute89 != null) {
                map.put(androidx.exifinterface.media.ExifInterface.TAG_ORF_PREVIEW_IMAGE_START, attribute89);
            }
            String attribute90 = exifInterface.getAttribute(androidx.exifinterface.media.ExifInterface.TAG_ORF_THUMBNAIL_IMAGE);
            if (attribute90 != null) {
                map.put(androidx.exifinterface.media.ExifInterface.TAG_ORF_THUMBNAIL_IMAGE, attribute90);
            }
            String attribute91 = exifInterface.getAttribute(androidx.exifinterface.media.ExifInterface.TAG_PHOTOMETRIC_INTERPRETATION);
            if (attribute91 != null) {
                map.put(androidx.exifinterface.media.ExifInterface.TAG_PHOTOMETRIC_INTERPRETATION, attribute91);
            }
            String attribute92 = exifInterface.getAttribute(androidx.exifinterface.media.ExifInterface.TAG_PIXEL_X_DIMENSION);
            if (attribute92 != null) {
                map.put(androidx.exifinterface.media.ExifInterface.TAG_PIXEL_X_DIMENSION, attribute92);
            }
            String attribute93 = exifInterface.getAttribute(androidx.exifinterface.media.ExifInterface.TAG_PIXEL_Y_DIMENSION);
            if (attribute93 != null) {
                map.put(androidx.exifinterface.media.ExifInterface.TAG_PIXEL_Y_DIMENSION, attribute93);
            }
            String attribute94 = exifInterface.getAttribute(androidx.exifinterface.media.ExifInterface.TAG_PLANAR_CONFIGURATION);
            if (attribute94 != null) {
                map.put(androidx.exifinterface.media.ExifInterface.TAG_PLANAR_CONFIGURATION, attribute94);
            }
            String attribute95 = exifInterface.getAttribute(androidx.exifinterface.media.ExifInterface.TAG_PRIMARY_CHROMATICITIES);
            if (attribute95 != null) {
                map.put(androidx.exifinterface.media.ExifInterface.TAG_PRIMARY_CHROMATICITIES, attribute95);
            }
            String attribute96 = exifInterface.getAttribute(androidx.exifinterface.media.ExifInterface.TAG_REFERENCE_BLACK_WHITE);
            if (attribute96 != null) {
                map.put(androidx.exifinterface.media.ExifInterface.TAG_REFERENCE_BLACK_WHITE, attribute96);
            }
            String attribute97 = exifInterface.getAttribute(androidx.exifinterface.media.ExifInterface.TAG_RELATED_SOUND_FILE);
            if (attribute97 != null) {
                map.put(androidx.exifinterface.media.ExifInterface.TAG_RELATED_SOUND_FILE, attribute97);
            }
            String attribute98 = exifInterface.getAttribute(androidx.exifinterface.media.ExifInterface.TAG_RESOLUTION_UNIT);
            if (attribute98 != null) {
                map.put(androidx.exifinterface.media.ExifInterface.TAG_RESOLUTION_UNIT, attribute98);
            }
            String attribute99 = exifInterface.getAttribute(androidx.exifinterface.media.ExifInterface.TAG_ROWS_PER_STRIP);
            if (attribute99 != null) {
                map.put(androidx.exifinterface.media.ExifInterface.TAG_ROWS_PER_STRIP, attribute99);
            }
            String attribute100 = exifInterface.getAttribute(androidx.exifinterface.media.ExifInterface.TAG_RW2_ISO);
            if (attribute100 != null) {
                map.put(androidx.exifinterface.media.ExifInterface.TAG_RW2_ISO, attribute100);
            }
            String attribute101 = exifInterface.getAttribute(androidx.exifinterface.media.ExifInterface.TAG_RW2_JPG_FROM_RAW);
            if (attribute101 != null) {
                map.put(androidx.exifinterface.media.ExifInterface.TAG_RW2_JPG_FROM_RAW, attribute101);
            }
            String attribute102 = exifInterface.getAttribute(androidx.exifinterface.media.ExifInterface.TAG_RW2_SENSOR_BOTTOM_BORDER);
            if (attribute102 != null) {
                map.put(androidx.exifinterface.media.ExifInterface.TAG_RW2_SENSOR_BOTTOM_BORDER, attribute102);
            }
            String attribute103 = exifInterface.getAttribute(androidx.exifinterface.media.ExifInterface.TAG_RW2_SENSOR_LEFT_BORDER);
            if (attribute103 != null) {
                map.put(androidx.exifinterface.media.ExifInterface.TAG_RW2_SENSOR_LEFT_BORDER, attribute103);
            }
            String attribute104 = exifInterface.getAttribute(androidx.exifinterface.media.ExifInterface.TAG_RW2_SENSOR_RIGHT_BORDER);
            if (attribute104 != null) {
                map.put(androidx.exifinterface.media.ExifInterface.TAG_RW2_SENSOR_RIGHT_BORDER, attribute104);
            }
            String attribute105 = exifInterface.getAttribute(androidx.exifinterface.media.ExifInterface.TAG_RW2_SENSOR_TOP_BORDER);
            if (attribute105 != null) {
                map.put(androidx.exifinterface.media.ExifInterface.TAG_RW2_SENSOR_TOP_BORDER, attribute105);
            }
            String attribute106 = exifInterface.getAttribute(androidx.exifinterface.media.ExifInterface.TAG_SAMPLES_PER_PIXEL);
            if (attribute106 != null) {
                map.put(androidx.exifinterface.media.ExifInterface.TAG_SAMPLES_PER_PIXEL, attribute106);
            }
            String attribute107 = exifInterface.getAttribute(androidx.exifinterface.media.ExifInterface.TAG_SATURATION);
            if (attribute107 != null) {
                map.put(androidx.exifinterface.media.ExifInterface.TAG_SATURATION, attribute107);
            }
            String attribute108 = exifInterface.getAttribute(androidx.exifinterface.media.ExifInterface.TAG_SCENE_CAPTURE_TYPE);
            if (attribute108 != null) {
                map.put(androidx.exifinterface.media.ExifInterface.TAG_SCENE_CAPTURE_TYPE, attribute108);
            }
            String attribute109 = exifInterface.getAttribute(androidx.exifinterface.media.ExifInterface.TAG_SCENE_TYPE);
            if (attribute109 != null) {
                map.put(androidx.exifinterface.media.ExifInterface.TAG_SCENE_TYPE, attribute109);
            }
            String attribute110 = exifInterface.getAttribute(androidx.exifinterface.media.ExifInterface.TAG_SENSING_METHOD);
            if (attribute110 != null) {
                map.put(androidx.exifinterface.media.ExifInterface.TAG_SENSING_METHOD, attribute110);
            }
            String attribute111 = exifInterface.getAttribute(androidx.exifinterface.media.ExifInterface.TAG_SHARPNESS);
            if (attribute111 != null) {
                map.put(androidx.exifinterface.media.ExifInterface.TAG_SHARPNESS, attribute111);
            }
            String attribute112 = exifInterface.getAttribute(androidx.exifinterface.media.ExifInterface.TAG_SHUTTER_SPEED_VALUE);
            if (attribute112 != null) {
                map.put(androidx.exifinterface.media.ExifInterface.TAG_SHUTTER_SPEED_VALUE, attribute112);
            }
            String attribute113 = exifInterface.getAttribute(androidx.exifinterface.media.ExifInterface.TAG_SOFTWARE);
            if (attribute113 != null) {
                map.put(androidx.exifinterface.media.ExifInterface.TAG_SOFTWARE, attribute113);
            }
            String attribute114 = exifInterface.getAttribute(androidx.exifinterface.media.ExifInterface.TAG_SPATIAL_FREQUENCY_RESPONSE);
            if (attribute114 != null) {
                map.put(androidx.exifinterface.media.ExifInterface.TAG_SPATIAL_FREQUENCY_RESPONSE, attribute114);
            }
            String attribute115 = exifInterface.getAttribute(androidx.exifinterface.media.ExifInterface.TAG_SPECTRAL_SENSITIVITY);
            if (attribute115 != null) {
                map.put(androidx.exifinterface.media.ExifInterface.TAG_SPECTRAL_SENSITIVITY, attribute115);
            }
            String attribute116 = exifInterface.getAttribute(androidx.exifinterface.media.ExifInterface.TAG_STRIP_BYTE_COUNTS);
            if (attribute116 != null) {
                map.put(androidx.exifinterface.media.ExifInterface.TAG_STRIP_BYTE_COUNTS, attribute116);
            }
            String attribute117 = exifInterface.getAttribute(androidx.exifinterface.media.ExifInterface.TAG_STRIP_OFFSETS);
            if (attribute117 != null) {
                map.put(androidx.exifinterface.media.ExifInterface.TAG_STRIP_OFFSETS, attribute117);
            }
            String attribute118 = exifInterface.getAttribute(androidx.exifinterface.media.ExifInterface.TAG_SUBFILE_TYPE);
            if (attribute118 != null) {
                map.put(androidx.exifinterface.media.ExifInterface.TAG_SUBFILE_TYPE, attribute118);
            }
            String attribute119 = exifInterface.getAttribute(androidx.exifinterface.media.ExifInterface.TAG_SUBJECT_AREA);
            if (attribute119 != null) {
                map.put(androidx.exifinterface.media.ExifInterface.TAG_SUBJECT_AREA, attribute119);
            }
            String attribute120 = exifInterface.getAttribute(androidx.exifinterface.media.ExifInterface.TAG_SUBJECT_DISTANCE);
            if (attribute120 != null) {
                map.put(androidx.exifinterface.media.ExifInterface.TAG_SUBJECT_DISTANCE, attribute120);
            }
            String attribute121 = exifInterface.getAttribute(androidx.exifinterface.media.ExifInterface.TAG_SUBJECT_DISTANCE_RANGE);
            if (attribute121 != null) {
                map.put(androidx.exifinterface.media.ExifInterface.TAG_SUBJECT_DISTANCE_RANGE, attribute121);
            }
            String attribute122 = exifInterface.getAttribute(androidx.exifinterface.media.ExifInterface.TAG_SUBJECT_LOCATION);
            if (attribute122 != null) {
                map.put(androidx.exifinterface.media.ExifInterface.TAG_SUBJECT_LOCATION, attribute122);
            }
            String attribute123 = exifInterface.getAttribute(androidx.exifinterface.media.ExifInterface.TAG_SUBSEC_TIME);
            if (attribute123 != null) {
                map.put(androidx.exifinterface.media.ExifInterface.TAG_SUBSEC_TIME, attribute123);
            }
            String attribute124 = exifInterface.getAttribute(androidx.exifinterface.media.ExifInterface.TAG_SUBSEC_TIME_DIGITIZED);
            if (attribute124 != null) {
                map.put(androidx.exifinterface.media.ExifInterface.TAG_SUBSEC_TIME_DIGITIZED, attribute124);
            }
            String attribute125 = exifInterface.getAttribute(androidx.exifinterface.media.ExifInterface.TAG_SUBSEC_TIME_ORIGINAL);
            if (attribute125 != null) {
                map.put(androidx.exifinterface.media.ExifInterface.TAG_SUBSEC_TIME_ORIGINAL, attribute125);
            }
            String attribute126 = exifInterface.getAttribute(androidx.exifinterface.media.ExifInterface.TAG_SUBSEC_TIME_ORIGINAL);
            if (attribute126 != null) {
                map.put(androidx.exifinterface.media.ExifInterface.TAG_SUBSEC_TIME_ORIGINAL, attribute126);
            }
            String attribute127 = exifInterface.getAttribute(androidx.exifinterface.media.ExifInterface.TAG_THUMBNAIL_IMAGE_LENGTH);
            if (attribute127 != null) {
                map.put(androidx.exifinterface.media.ExifInterface.TAG_THUMBNAIL_IMAGE_LENGTH, attribute127);
            }
            String attribute128 = exifInterface.getAttribute(androidx.exifinterface.media.ExifInterface.TAG_THUMBNAIL_IMAGE_WIDTH);
            if (attribute128 != null) {
                map.put(androidx.exifinterface.media.ExifInterface.TAG_THUMBNAIL_IMAGE_WIDTH, attribute128);
            }
            String attribute129 = exifInterface.getAttribute(androidx.exifinterface.media.ExifInterface.TAG_THUMBNAIL_ORIENTATION);
            if (attribute129 != null) {
                map.put(androidx.exifinterface.media.ExifInterface.TAG_THUMBNAIL_ORIENTATION, attribute129);
            }
            String attribute130 = exifInterface.getAttribute(androidx.exifinterface.media.ExifInterface.TAG_TRANSFER_FUNCTION);
            if (attribute130 != null) {
                map.put(androidx.exifinterface.media.ExifInterface.TAG_TRANSFER_FUNCTION, attribute130);
            }
            String attribute131 = exifInterface.getAttribute(androidx.exifinterface.media.ExifInterface.TAG_USER_COMMENT);
            if (attribute131 != null) {
                map.put(androidx.exifinterface.media.ExifInterface.TAG_USER_COMMENT, attribute131);
            }
            String attribute132 = exifInterface.getAttribute(androidx.exifinterface.media.ExifInterface.TAG_WHITE_BALANCE);
            if (attribute132 != null) {
                map.put(androidx.exifinterface.media.ExifInterface.TAG_WHITE_BALANCE, attribute132);
            }
            String attribute133 = exifInterface.getAttribute(androidx.exifinterface.media.ExifInterface.TAG_WHITE_POINT);
            if (attribute133 != null) {
                map.put(androidx.exifinterface.media.ExifInterface.TAG_WHITE_POINT, attribute133);
            }
            String attribute134 = exifInterface.getAttribute(androidx.exifinterface.media.ExifInterface.TAG_XMP);
            if (attribute134 != null) {
                map.put(androidx.exifinterface.media.ExifInterface.TAG_XMP, attribute134);
            }
            String attribute135 = exifInterface.getAttribute(androidx.exifinterface.media.ExifInterface.TAG_X_RESOLUTION);
            if (attribute135 != null) {
                map.put(androidx.exifinterface.media.ExifInterface.TAG_X_RESOLUTION, attribute135);
            }
            String attribute136 = exifInterface.getAttribute(androidx.exifinterface.media.ExifInterface.TAG_Y_CB_CR_COEFFICIENTS);
            if (attribute136 != null) {
                map.put(androidx.exifinterface.media.ExifInterface.TAG_Y_CB_CR_COEFFICIENTS, attribute136);
            }
            String attribute137 = exifInterface.getAttribute(androidx.exifinterface.media.ExifInterface.TAG_Y_CB_CR_POSITIONING);
            if (attribute137 != null) {
                map.put(androidx.exifinterface.media.ExifInterface.TAG_Y_CB_CR_POSITIONING, attribute137);
            }
            String attribute138 = exifInterface.getAttribute(androidx.exifinterface.media.ExifInterface.TAG_Y_CB_CR_SUB_SAMPLING);
            if (attribute138 != null) {
                map.put(androidx.exifinterface.media.ExifInterface.TAG_Y_CB_CR_SUB_SAMPLING, attribute138);
            }
            String attribute139 = exifInterface.getAttribute(androidx.exifinterface.media.ExifInterface.TAG_Y_RESOLUTION);
            if (attribute139 != null) {
                map.put(androidx.exifinterface.media.ExifInterface.TAG_Y_RESOLUTION, attribute139);
            }
            if (inputStreamOpen != null) {
                inputStreamOpen.close();
            }
            return map;
        } catch (Exception e10) {
            e10.printStackTrace();
            return null;
        }
    }

    public static int GetIntExifInfo(String filePath, int iFieldID) {
        ExifInterface exifInterface;
        InputStream inputStreamOpen;
        int attributeInt;
        int i10 = 0;
        if (filePath == null) {
            return 0;
        }
        try {
            if (filePath.startsWith(QStreamContent.CONTENT_THEME)) {
                inputStreamOpen = QStreamContent.resolver.openInputStream(Uri.parse(filePath));
                exifInterface = new ExifInterface(inputStreamOpen);
            } else if (filePath.startsWith("assets_android://")) {
                AssetManager assetManager = QStreamAssets.mAssetManager;
                if (assetManager != null) {
                    inputStreamOpen = assetManager.open(filePath.replace("assets_android://", ""));
                    exifInterface = new ExifInterface(inputStreamOpen);
                } else {
                    inputStreamOpen = null;
                    exifInterface = null;
                }
            } else {
                exifInterface = new ExifInterface(filePath);
                inputStreamOpen = null;
            }
            if (exifInterface == null) {
                if (inputStreamOpen != null) {
                    inputStreamOpen.close();
                }
                return 0;
            }
            if (iFieldID == EXIF_AGID_ORIENTATION && (attributeInt = exifInterface.getAttributeInt(androidx.exifinterface.media.ExifInterface.TAG_ORIENTATION, -1)) != -1) {
                if (attributeInt == 3) {
                    i10 = 180;
                } else if (attributeInt == 6) {
                    i10 = 90;
                } else if (attributeInt == 8) {
                    i10 = 270;
                }
            }
            if (inputStreamOpen != null) {
                inputStreamOpen.close();
            }
            return i10;
        } catch (Exception e10) {
            e10.printStackTrace();
            return 0;
        }
    }

    public static byte[] GetJPGThumbnail(String szPath) {
        if (szPath == null) {
            return null;
        }
        try {
            ExifInterface exifInterface = new ExifInterface(szPath);
            if (exifInterface.hasThumbnail()) {
                return exifInterface.getThumbnail();
            }
        } catch (Exception unused) {
        }
        return null;
    }

    public static Bitmap LoadBitmap(InputStream is, int width, int height, int color) {
        byte[] bArr;
        if (is == null) {
            return null;
        }
        try {
            Rect rect = new Rect();
            BitmapFactory.Options options = new BitmapFactory.Options();
            int i10 = 1;
            if (color == 1) {
                options.inPreferredConfig = Bitmap.Config.ARGB_8888;
            } else if (color == 4) {
                options.inPreferredConfig = Bitmap.Config.RGB_565;
            } else if (color == 7) {
                options.inPreferredConfig = Bitmap.Config.ARGB_4444;
            } else if (color != 8) {
                options.inPreferredConfig = Bitmap.Config.ARGB_8888;
            } else {
                options.inPreferredConfig = Bitmap.Config.ALPHA_8;
            }
            options.inJustDecodeBounds = true;
            if (is.markSupported()) {
                is.mark(0);
                bArr = null;
            } else {
                bArr = new byte[is.available()];
                System.currentTimeMillis();
                is.read(bArr);
            }
            if (bArr != null) {
                BitmapFactory.decodeByteArray(bArr, 0, bArr.length, options);
            } else {
                BitmapFactory.decodeStream(is, rect, options);
            }
            while (true) {
                int i11 = options.outWidth;
                int i12 = options.outHeight;
                int i13 = width * height;
                if ((i11 / i10) * (i12 / i10) <= i13) {
                    break;
                }
                int i14 = i10 << 1;
                if ((i11 / i14) * (i12 / i14) < i13) {
                    break;
                }
                i10 = i14;
            }
            options.inJustDecodeBounds = false;
            options.inSampleSize = i10;
            if (is.markSupported()) {
                is.reset();
            }
            System.currentTimeMillis();
            Bitmap bitmapDecodeByteArray = bArr != null ? BitmapFactory.decodeByteArray(bArr, 0, bArr.length, options) : BitmapFactory.decodeStream(is, rect, options);
            if (bitmapDecodeByteArray == null) {
                return null;
            }
            Bitmap.Config config = options.inPreferredConfig;
            Bitmap.Config config2 = Bitmap.Config.ARGB_8888;
            if (config != config2 || options.outConfig == config2) {
                return bitmapDecodeByteArray;
            }
            Bitmap bitmapCopy = bitmapDecodeByteArray.copy(config2, false);
            bitmapDecodeByteArray.recycle();
            return bitmapCopy;
        } catch (Exception e10) {
            e10.printStackTrace();
            return null;
        } catch (OutOfMemoryError e11) {
            e11.printStackTrace();
            return null;
        }
    }

    public static Bitmap LoadBitmap2(String pathName, int width, int height, int color) {
        if (pathName == null) {
            return null;
        }
        try {
            BitmapFactory.Options options = new BitmapFactory.Options();
            int i10 = 1;
            if (color == 1) {
                options.inPreferredConfig = Bitmap.Config.ARGB_8888;
            } else if (color == 4) {
                options.inPreferredConfig = Bitmap.Config.RGB_565;
            } else if (color == 7) {
                options.inPreferredConfig = Bitmap.Config.ARGB_4444;
            } else if (color != 8) {
                options.inPreferredConfig = Bitmap.Config.ARGB_8888;
            } else {
                options.inPreferredConfig = Bitmap.Config.ALPHA_8;
            }
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeFile(pathName, options);
            while (true) {
                int i11 = options.outWidth;
                int i12 = options.outHeight;
                int i13 = width * height;
                if ((i11 / i10) * (i12 / i10) <= i13) {
                    break;
                }
                int i14 = i10 << 1;
                if ((i11 / i14) * (i12 / i14) < i13) {
                    break;
                }
                i10 = i14;
            }
            options.inJustDecodeBounds = false;
            options.inSampleSize = i10;
            Bitmap bitmapDecodeFile = BitmapFactory.decodeFile(pathName, options);
            if (bitmapDecodeFile == null) {
                return null;
            }
            Bitmap.Config config = options.inPreferredConfig;
            Bitmap.Config config2 = Bitmap.Config.ARGB_8888;
            if (config != config2 || options.outConfig == config2) {
                return bitmapDecodeFile;
            }
            Bitmap bitmapCopy = bitmapDecodeFile.copy(config2, false);
            bitmapDecodeFile.recycle();
            return bitmapCopy;
        } catch (Exception e10) {
            e10.printStackTrace();
            return null;
        } catch (OutOfMemoryError e11) {
            e11.printStackTrace();
            return null;
        }
    }

    public static Bitmap LoadBitmap3(byte[] data, int offset, int length, int width, int height, int color) {
        if (data == null) {
            return null;
        }
        try {
            BitmapFactory.Options options = new BitmapFactory.Options();
            int i10 = 1;
            if (color == 1) {
                options.inPreferredConfig = Bitmap.Config.ARGB_8888;
                options.inMutable = true;
            } else if (color == 4) {
                options.inPreferredConfig = Bitmap.Config.RGB_565;
            } else if (color == 7) {
                options.inPreferredConfig = Bitmap.Config.ARGB_4444;
            } else if (color != 8) {
                options.inPreferredConfig = Bitmap.Config.ARGB_8888;
                options.inMutable = true;
            } else {
                options.inPreferredConfig = Bitmap.Config.ALPHA_8;
            }
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeByteArray(data, offset, length, options);
            while (true) {
                int i11 = options.outWidth;
                int i12 = options.outHeight;
                int i13 = width * height;
                if ((i11 / i10) * (i12 / i10) <= i13) {
                    break;
                }
                int i14 = i10 << 1;
                if ((i11 / i14) * (i12 / i14) < i13) {
                    break;
                }
                i10 = i14;
            }
            options.inJustDecodeBounds = false;
            options.inSampleSize = i10;
            Bitmap bitmapDecodeByteArray = BitmapFactory.decodeByteArray(data, offset, length, options);
            Bitmap.Config config = options.inPreferredConfig;
            Bitmap.Config config2 = Bitmap.Config.ARGB_8888;
            if (config != config2 || options.outConfig == config2) {
                return bitmapDecodeByteArray;
            }
            Bitmap bitmapCopy = bitmapDecodeByteArray.copy(config2, false);
            bitmapDecodeByteArray.recycle();
            return bitmapCopy;
        } catch (Exception e10) {
            e10.printStackTrace();
            return null;
        } catch (OutOfMemoryError e11) {
            e11.printStackTrace();
            return null;
        }
    }

    public static BitmapFactory.Options LoadBitmapBound(InputStream is) {
        BitmapFactory.Options options = new BitmapFactory.Options();
        if (is == null) {
            return null;
        }
        try {
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeStream(is, new Rect(), options);
            return options;
        } catch (Exception e10) {
            e10.printStackTrace();
            return options;
        }
    }

    public static BitmapFactory.Options LoadBitmapBound2(byte[] data, int offset, int length) {
        BitmapFactory.Options options = new BitmapFactory.Options();
        if (data == null) {
            return null;
        }
        try {
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeByteArray(data, offset, length, options);
        } catch (Exception e10) {
            e10.printStackTrace();
        }
        return options;
    }

    public static int MergeBitmap(Bitmap dstbmp, Bitmap forebmp, int foreleft, int foretop, Bitmap maskbmp, int maskleft, int masktop, long opacity) {
        if (dstbmp != null && forebmp != null) {
            try {
                Bitmap alpha = setAlpha(forebmp, maskbmp, maskleft, masktop, (int) opacity);
                new Canvas(dstbmp).drawBitmap(alpha, foreleft, foretop, (Paint) null);
                if (alpha.isRecycled()) {
                    return 0;
                }
                alpha.recycle();
                return 0;
            } catch (Exception e10) {
                e10.printStackTrace();
            } catch (OutOfMemoryError e11) {
                e11.printStackTrace();
            }
        }
        return 1;
    }

    public static InputStream OpenInputFile(String pathName) {
        InputStream inputStreamOpenInputStream = null;
        if (pathName != null) {
            try {
                if (pathName.startsWith("assets_android://")) {
                    AssetManager assetManager = QStreamAssets.mAssetManager;
                    if (assetManager != null) {
                        inputStreamOpenInputStream = assetManager.open(pathName.replace("assets_android://", ""));
                    }
                } else {
                    inputStreamOpenInputStream = pathName.startsWith(QStreamContent.CONTENT_THEME) ? QStreamContent.resolver.openInputStream(Uri.parse(pathName)) : new FileInputStream(pathName);
                }
            } catch (IOException e10) {
                e10.printStackTrace();
            }
        }
        return inputStreamOpenInputStream;
    }

    public static InputStream OpenInputStreamFromByteArray(byte[] data, int offset, int length) {
        if (data == null) {
            return null;
        }
        try {
            return new ByteArrayInputStream(data, offset, length);
        } catch (Exception e10) {
            e10.printStackTrace();
            return null;
        }
    }

    public static OutputStream OpenOutputFile(String pathName) {
        if (pathName == null) {
            return null;
        }
        try {
            return new FileOutputStream(pathName);
        } catch (IOException e10) {
            e10.printStackTrace();
            return null;
        }
    }

    public static int RecycleBitmap(Bitmap bmp) {
        if (bmp == null || bmp.isRecycled()) {
            return 0;
        }
        bmp.recycle();
        return 0;
    }

    public static Bitmap ResampleBitmap(Bitmap srcbmp, int dstwidth, int dstheight) {
        Bitmap bitmap = null;
        if (srcbmp == null) {
            return null;
        }
        try {
            Rect rect = new Rect(0, 0, srcbmp.getWidth(), srcbmp.getHeight());
            Rect rect2 = new Rect(0, 0, dstwidth, dstheight);
            Bitmap bitmapCreateBitmap = Bitmap.createBitmap(dstwidth, dstheight, srcbmp.getConfig());
            try {
                new Canvas(bitmapCreateBitmap).drawBitmap(srcbmp, rect, rect2, (Paint) null);
            } catch (Exception e10) {
                e10.printStackTrace();
                return bitmapCreateBitmap;
            } catch (OutOfMemoryError e11) {
                e11.printStackTrace();
                return bitmapCreateBitmap;
            }
        } catch (Exception e12) {
            e12.printStackTrace();
        } catch (OutOfMemoryError e13) {
            e13.printStackTrace();
        }
        return bitmap;
    }

    public static Bitmap RotateBitmap(Bitmap srcbmp, float degrees) {
        if (srcbmp == null) {
            return null;
        }
        try {
            Matrix matrix = new Matrix();
            matrix.setRotate(degrees);
            return Bitmap.createBitmap(srcbmp, 0, 0, srcbmp.getWidth(), srcbmp.getHeight(), matrix, false);
        } catch (IllegalArgumentException e10) {
            e10.printStackTrace();
            return null;
        } catch (OutOfMemoryError e11) {
            e11.printStackTrace();
            return null;
        }
    }

    public static int saveBitmapWithExif(Bitmap bmp, String pathName, int fileFormat, Map<String, String> exifMap) {
        try {
            FileOutputStream fileOutputStream = new FileOutputStream(pathName);
            int iBitmapSave = BitmapSave(bmp, fileOutputStream, fileFormat);
            fileOutputStream.close();
            if (fileFormat == 2 && exifMap != null && exifMap.size() > 0) {
                ExifInterface exifInterface = new ExifInterface(pathName);
                for (Map.Entry<String, String> entry : exifMap.entrySet()) {
                    exifInterface.setAttribute(entry.getKey(), entry.getValue());
                }
                exifInterface.saveAttributes();
            }
            return iBitmapSave;
        } catch (Exception e10) {
            e10.printStackTrace();
            return 1;
        }
    }

    public static Bitmap setAlpha(Bitmap sourceImg, Bitmap maskImg, int maskleft, int masktop, int number) {
        int width;
        int height;
        int[] iArr;
        int[] iArr2;
        if (sourceImg == null) {
            return null;
        }
        if (maskImg != null) {
            Bitmap.Config config = maskImg.getConfig();
            Bitmap.Config config2 = Bitmap.Config.ALPHA_8;
            if (config != config2 && maskImg.getConfig() != Bitmap.Config.ARGB_8888) {
                return null;
            }
            width = Math.min(sourceImg.getWidth() - maskleft, maskImg.getWidth());
            height = Math.min(sourceImg.getHeight() - masktop, maskImg.getHeight());
            int i10 = width * height;
            int[] iArr3 = new int[i10];
            sourceImg.getPixels(iArr3, 0, sourceImg.getWidth(), maskleft, masktop, width, height);
            if (maskImg.getConfig() == config2) {
                byte[] bArr = new byte[maskImg.getRowBytes() * maskImg.getHeight()];
                maskImg.copyPixelsToBuffer(ByteBuffer.wrap(bArr));
                for (int i11 = 0; i11 < i10; i11++) {
                    iArr3[i11] = ((((255 - (bArr[i11] & 255)) * number) / 100) << 24) | (iArr3[i11] & 16777215);
                }
                iArr2 = iArr3;
            } else {
                int[] iArr4 = new int[maskImg.getWidth() * maskImg.getHeight()];
                iArr2 = iArr3;
                maskImg.getPixels(iArr4, 0, maskImg.getWidth(), 0, 0, maskImg.getWidth(), maskImg.getHeight());
                for (int i12 = 0; i12 < i10; i12++) {
                    iArr2[i12] = ((((255 - (iArr4[i12] & 255)) * number) / 100) << 24) | (iArr2[i12] & 16777215);
                }
            }
            iArr = iArr2;
        } else {
            width = sourceImg.getWidth();
            height = sourceImg.getHeight();
            int i13 = width * height;
            iArr = new int[i13];
            sourceImg.getPixels(iArr, 0, sourceImg.getWidth(), 0, 0, width, height);
            int i14 = (number * 255) / 100;
            for (int i15 = 0; i15 < i13; i15++) {
                iArr[i15] = (i14 << 24) | (iArr[i15] & 16777215);
            }
        }
        return Bitmap.createBitmap(iArr, width, height, Bitmap.Config.ARGB_8888);
    }
}
