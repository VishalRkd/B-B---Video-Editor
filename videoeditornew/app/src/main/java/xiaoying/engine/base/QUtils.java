package xiaoying.engine.base;

import java.io.File;
import java.io.FileFilter;
import java.io.InputStream;
import java.util.regex.Pattern;
import xiaoying.engine.QEngine;
import xiaoying.engine.clip.QClip;
import xiaoying.engine.clip.QMediaSource;
import xiaoying.engine.clip.QUserData;
import xiaoying.engine.storyboard.QStoryboard;
import xiaoying.utils.QBitmap;
import xiaoying.utils.QBitmapFactory;
import xiaoying.utils.QPoint;
import xiaoying.utils.QSize;

/* JADX INFO: loaded from: classes19.dex */
public class QUtils {
    public static final int ANCHOR_ALIGN = 3;
    private static final int BACK_COVER_CLIP_INDEX = -2;
    public static final int BOTTEM_ALIGN = 1;
    public static final int CHECK_ALLOW_UNSEEKABLE = 131072;
    public static final int CHECK_AUDIO = 1;
    public static final int CHECK_BASE = 0;
    public static final int CHECK_NO_AUDIO_TRACK = 2;
    public static final int CHECK_VIDEO = 65536;
    public static final int COLOR_TYPE_BLACK = 1;
    public static final int COLOR_TYPE_BLUE = 7;
    public static final int COLOR_TYPE_GRAY = 2;
    public static final int COLOR_TYPE_GREEN = 6;
    public static final int COLOR_TYPE_NONE = 0;
    public static final int COLOR_TYPE_PURPLE = 8;
    public static final int COLOR_TYPE_RED = 4;
    public static final int COLOR_TYPE_WHITE = 3;
    public static final int COLOR_TYPE_YELLOW = 5;
    private static final int COVER_CLIP_INDEX = -1;
    public static final int HD_SUPPORT_BETA_TEST_FLAG = 16;
    public static final int HD_SUPPORT_TYPE_1080P = 2;
    public static final int HD_SUPPORT_TYPE_2K = 4;
    public static final int HD_SUPPORT_TYPE_4K = 8;
    public static final int HD_SUPPORT_TYPE_720P = 1;
    public static final int HD_SUPPORT_TYPE_8K = 16;
    public static final int HD_SUPPORT_TYPE_NO = 0;
    public static final int HEAD_ALIGN = 2;
    public static final int HW_BETA_TESTED_FLAG_COUNT = 1;
    public static final int HW_CODEC_CAP_GPURENDER_LEN = 256;
    public static final int HW_DECODER_CAP_COUNT = 35;
    public static final int HW_ENCODER_CAP_COUNT = 5;
    public static final int IMAGE_FMT_BMP = 3;
    public static final int IMAGE_FMT_GIF = 5;
    public static final int IMAGE_FMT_HEIC = 6;
    public static final int IMAGE_FMT_JPEG = 1;
    public static final int IMAGE_FMT_PNG = 2;
    public static final int IMAGE_FMT_SVG = 8;
    public static final int IMAGE_FMT_TIFF = 7;
    public static final int IMAGE_FMT_UNKOWN = 0;
    public static final int IMAGE_FMT_WEBP = 4;
    public static final int LAYOUT_MODE_NONE = 0;
    public static final int LAYOUT_MODE_W16_H9 = 8;
    public static final int LAYOUT_MODE_W1_H1 = 16;
    public static final int LAYOUT_MODE_W3_H4 = 1;
    public static final int LAYOUT_MODE_W4_H3 = 2;
    public static final int LAYOUT_MODE_W9_H16 = 4;
    public static final double MIN_NSX_CPU_FREQ = 1500000.0d;
    public static final int SOURCE_XML_TYPE_BASE = 0;
    public static final int SOURCE_XML_TYPE_EFFECT = 0;
    public static final int TOP_ALIGN = 0;
    public static final int TRSNSCODE_REASON_1CORE_BASE = 1300;
    public static final int TRSNSCODE_REASON_2CORE_BASE = 1400;
    public static final int TRSNSCODE_REASON_4CORE_BASE = 1500;
    public static final int TRSNSCODE_REASON_COMM_BASE = 1000;
    public static final int TRSNSCODE_REASON_PASTER_BASE = 1600;
    public static final int TRSNSCODE_REASON_PIP_BASE = 1100;
    public static final int TRSNSCODE_REASON_RVS_BASE = 1200;
    public static final int UNSUPPORTED_ACODEC = 4;
    public static final int UNSUPPORTED_FILE = 1;
    public static final int UNSUPPORTED_IMAGECODEC = 5;
    public static final int UNSUPPORTED_MPEG4_HEADER = 8;
    public static final int UNSUPPORTED_NOAUDIO = 7;
    public static final int UNSUPPORTED_NONE = 0;
    public static final int UNSUPPORTED_NOVIDEO = 6;
    public static final int UNSUPPORTED_RESOLUTION = 2;
    public static final int UNSUPPORTED_VCODEC = 3;
    public static final int UNSUPPORT_H264_HEADER = 10;
    public static final int UNSUPPORT_UNSEEKABLE = 9;
    public static final int VIDEO_BITRATE_MODE_ABR_HIGH = 2;
    public static final int VIDEO_BITRATE_MODE_ABR_LOW = 1;
    public static final int VIDEO_BITRATE_MODE_ABR_VERY_LOW = 4;
    public static final int VIDEO_BITRATE_MODE_CRF = 3;
    public static final int VIDEO_IMPORT_FORMAT_COUNT = 8;
    public static final int VIDEO_RES_1080P_H264 = 5;
    public static final int VIDEO_RES_1080P_H264_INTERLACE = 12;
    public static final int VIDEO_RES_1080P_HEIGHT = 1080;
    public static final int VIDEO_RES_1080P_MPEG4 = 0;
    public static final int VIDEO_RES_1080P_WIDTH = 1920;
    public static final int VIDEO_RES_2K_H264 = 16;
    public static final int VIDEO_RES_2K_H264_INTERLACE = 17;
    public static final int VIDEO_RES_2K_HEIGHT = 1600;
    public static final int VIDEO_RES_2K_WIDTH = 2560;
    public static final int VIDEO_RES_4K_H264 = 4;
    public static final int VIDEO_RES_4K_H264_INTERLACE = 11;
    public static final int VIDEO_RES_4K_HEIGHT = 2160;
    public static final int VIDEO_RES_4K_WIDTH = 3840;
    public static final int VIDEO_RES_720P_H264 = 6;
    public static final int VIDEO_RES_720P_H264_INTERLACE = 13;
    public static final int VIDEO_RES_720P_HEIGHT = 720;
    public static final int VIDEO_RES_720P_MPEG4 = 1;
    public static final int VIDEO_RES_720P_WIDTH = 1280;
    public static final int VIDEO_RES_8K_HEIGHT = 4320;
    public static final int VIDEO_RES_8K_WIDTH = 7680;
    public static final int VIDEO_RES_FWVGA_H264 = 7;
    public static final int VIDEO_RES_FWVGA_H264_INTERLACE = 14;
    public static final int VIDEO_RES_FWVGA_HEIGHT = 480;
    public static final int VIDEO_RES_FWVGA_MPEG4 = 2;
    public static final int VIDEO_RES_FWVGA_WIDTH = 854;
    public static final int VIDEO_RES_QVGA_H264 = 10;
    public static final int VIDEO_RES_QVGA_HEIGHT = 240;
    public static final int VIDEO_RES_QVGA_MPEG4 = 9;
    public static final int VIDEO_RES_QVGA_WIDTH = 320;
    public static final int VIDEO_RES_VGA_H264 = 8;
    public static final int VIDEO_RES_VGA_H264_INTERLACE = 15;
    public static final int VIDEO_RES_VGA_HEIGHT = 480;
    public static final int VIDEO_RES_VGA_MPEG4 = 3;
    public static final int VIDEO_RES_VGA_WIDTH = 640;

    public static class Geo {
        public int headWidth;
        public int height;
        public QPoint jaw = new QPoint();
        public int width;

                public int x;

                public int y;
    }

    public static class PreprocessArgs {
        public int targetHeadSize;
        public int targetHeight;
        public int targetWidth;
        public int type;
        public Geo geo = new Geo();
        public QPoint anchor = new QPoint();
    }

    public static class QVideoImportFormat {
        public int mHeight;
        public int mVideoFormat;
        public int mWidth;
    }

    public static int CacheSegMaskFile(QEngine engine, String strMaskFile, String strSrcFile, QSize srcSize) {
        return nativeCacheSegMaskFile(engine, strMaskFile, strSrcFile, srcSize);
    }

    public static int ExportAudio(QEngine engine, String inputFilePath, String outputFilePath, QRange trimRange) {
        return nativeExportAudio(engine, inputFilePath, outputFilePath, trimRange);
    }

    public static boolean GetBetaTestedFlag(QEngine engine) {
        return nativeGetHWBetaTestedFlag(engine);
    }

    public static int GetCurveTime(QClip.QCurveSpeedPoints points, int srcLen) {
        return nativeGetCurveTime(points, srcLen);
    }

    public static int GetCurveTimeWithDensity(QClip.QCurveSpeedPoints points, int srcLen, int density) {
        return nativeGetCurveTimeWithDensity(points, srcLen, density);
    }

    public static int GetFileMetaData(QEngine engine, QMetaData metaData, String strFilePath) {
        return nativeGetFileMetaData(engine, metaData, strFilePath);
    }

    public static int GetGopTime(QEngine engine, String videoFile) {
        return nativeGetGopTime(engine, videoFile);
    }

    public static int GetHWDecoderVersion() {
        return nativeGetHWVDecoderVersion();
    }

    public static int GetHWEncoderVersion() {
        return nativeGetHWVEncoderVersion();
    }

    public static int GetHWVDecoderCount(QEngine engine) {
        return nativeGetHWVDecoderCount(engine);
    }

    public static int GetImageRealFormat(String strFilePath) {
        return nativeGetImageRealFormat(strFilePath);
    }

    public static Object[] GetMaterialNeedSupportList(String templatePath) {
        return nativetGetMaterialNeedSupportList(templatePath);
    }

    public static String GetPicRealFilePath(String inputFilePath) {
        return nativeGetPicRealFilePath(inputFilePath);
    }

    public static int GetProjectVersion(String strFilePath) {
        return nativeGetProjectVersion(strFilePath);
    }

    public static int GetSegAlgoSize(QSize srcSize, QSize segSize) {
        return nativeGetSegAlgoSize(srcSize, segSize);
    }

    public static String GetSegCacheFilePath(QEngine engine, String strSrcFile, QSize srcSize, QSize segSize) {
        return nativeGetSegCacheFilePath(engine, strSrcFile, srcSize, segSize);
    }

    public static boolean IsInterlaceFile(QEngine engine, String strFilePath) {
        return nativeIsInterlaceFile(engine, strFilePath);
    }

    public static boolean IsNeedTranscode(QEngine engine, QVideoImportParam vImportParam, int[] videoResType) {
        return IsNeedTranscodeWithReason(engine, vImportParam, videoResType, null);
    }

    public static boolean IsNeedTranscodeWithReason(QEngine engine, QVideoImportParam vImportParam, int[] videoResType, int[] reason) {
        boolean hWEncFlag = vImportParam.getHWEncFlag();
        boolean z10 = false;
        vImportParam.setHWEncFlag(hWEncFlag);
        vImportParam.setHWDecFlag(vImportParam.getHWDecflag());
        if (vImportParam.getHDOutputFlag() && hWEncFlag && IsSupportHD(engine) != 0) {
            z10 = true;
        }
        vImportParam.setHDOutputFlag(z10);
        return nativeIsNeedTranscode(engine, vImportParam, videoResType, reason);
    }

    /* JADX WARN: Code duplicated, block: B:45:0x0080  */
    /* JADX WARN: Code duplicated, block: B:49:? A[RETURN, SYNTHETIC] */
    public static int IsSupportHD(QEngine engine) {
        if (getCpuNumber() < 4) {
            return 0;
        }
        int i10 = (QueryHWDecCap(engine, 4, VIDEO_RES_8K_WIDTH, 4320, false) < 3 || !QueryHWEncCap(engine, 4, VIDEO_RES_8K_WIDTH, 4320)) ? 0 : 16;
        if (i10 == 0 && QueryHWDecCap(engine, 4, VIDEO_RES_4K_WIDTH, 2160, false) >= 3 && QueryHWEncCap(engine, 4, VIDEO_RES_4K_WIDTH, 2160)) {
            i10 = 8;
        }
        if (i10 == 0 && QueryHWDecCap(engine, 4, VIDEO_RES_2K_WIDTH, 1600, false) >= 3 && QueryHWEncCap(engine, 4, VIDEO_RES_2K_WIDTH, 1600)) {
            i10 = 4;
        }
        if (i10 != 0) {
            return !GetBetaTestedFlag(engine) ? i10 | 16 : i10;
        }
        int iQueryHWDecCap = QueryHWDecCap(engine, 4, 1920, 1080, false);
        int i11 = 2;
        if (iQueryHWDecCap < 2) {
            return 0;
        }
        if (iQueryHWDecCap >= 3) {
            if (!QueryHWEncCap(engine, 4, 1920, 1080)) {
                if (!QueryHWEncCap(engine, 4, 1280, 720)) {
                    return 0;
                }
            }
            if (GetBetaTestedFlag(engine)) {
                return i11;
            }
            return i11 | 16;
        }
        if (!QueryHWEncCap(engine, 4, 1280, 720)) {
            return 0;
        }
        i11 = 1;
        if (GetBetaTestedFlag(engine)) {
            return i11 | 16;
        }
        return i11;
    }

    public static boolean IsSupportNSX() {
        if (getCpuNumber() < 4) {
            return false;
        }
        String maxCpuFreq = getMaxCpuFreq();
        if (!maxCpuFreq.equals("N/A") && maxCpuFreq.length() != 0) {
            try {
                return Double.parseDouble(maxCpuFreq) > 1500000.0d;
            } catch (Exception e10) {
                e10.printStackTrace();
            }
        }
        return false;
    }

    public static Object ObjectFromXml(QEngine engine, String xmlPath, int nType) {
        return nativeObjectFromXml(engine, xmlPath, nType);
    }

    public static int ObjectToXml(QEngine engine, Object obj, String xmlPath) {
        return nativeObjectToXml(engine, obj, xmlPath);
    }

    public static int QueryHWDecCap(QEngine engine, int videoformat, int width, int height, boolean bInterlace) {
        return nativeQueryHWDecCap(engine, videoformat, width, height, bInterlace);
    }

    public static boolean QueryHWEncCap(QEngine engine, int videoformat, int width, int height) {
        return nativeQueryHWEncCap(engine, videoformat, width, height);
    }

    public static int ReleaseAllHWDecoder(QEngine engine) {
        return nativeReleaseAllHWDecoder(engine);
    }

    public static int SavePngFromQBitmap(QBitmap bitmap, String filename) {
        return nativeSavePngFromQBitmap(bitmap, filename);
    }

    public static int SetEnableHWDecoderPool(QEngine engine, boolean bEnable) {
        return nativeSetEnableHWDecoderPool(engine, bEnable);
    }

    public static QVideoImportFormat TransformVImportFormat(int vImportFormat) {
        QVideoImportFormat qVideoImportFormat = new QVideoImportFormat();
        switch (vImportFormat) {
            case 0:
                qVideoImportFormat.mVideoFormat = 2;
                qVideoImportFormat.mWidth = 1920;
                qVideoImportFormat.mHeight = 1080;
                return qVideoImportFormat;
            case 1:
                qVideoImportFormat.mVideoFormat = 2;
                qVideoImportFormat.mWidth = 1280;
                qVideoImportFormat.mHeight = 720;
                return qVideoImportFormat;
            case 2:
                qVideoImportFormat.mVideoFormat = 2;
                qVideoImportFormat.mWidth = 854;
                qVideoImportFormat.mHeight = 480;
                return qVideoImportFormat;
            case 3:
                qVideoImportFormat.mVideoFormat = 2;
                qVideoImportFormat.mWidth = 640;
                qVideoImportFormat.mHeight = 480;
                return qVideoImportFormat;
            case 4:
            default:
                return null;
            case 5:
                qVideoImportFormat.mVideoFormat = 4;
                qVideoImportFormat.mWidth = 1920;
                qVideoImportFormat.mHeight = 1080;
                return qVideoImportFormat;
            case 6:
                qVideoImportFormat.mVideoFormat = 4;
                qVideoImportFormat.mWidth = 1280;
                qVideoImportFormat.mHeight = 720;
                return qVideoImportFormat;
            case 7:
                qVideoImportFormat.mVideoFormat = 4;
                qVideoImportFormat.mWidth = 854;
                qVideoImportFormat.mHeight = 480;
                return qVideoImportFormat;
            case 8:
                qVideoImportFormat.mVideoFormat = 4;
                qVideoImportFormat.mWidth = 640;
                qVideoImportFormat.mHeight = 480;
                return qVideoImportFormat;
            case 9:
                qVideoImportFormat.mVideoFormat = 2;
                qVideoImportFormat.mWidth = 320;
                qVideoImportFormat.mHeight = 240;
                return qVideoImportFormat;
            case 10:
                qVideoImportFormat.mVideoFormat = 4;
                qVideoImportFormat.mWidth = 320;
                qVideoImportFormat.mHeight = 240;
                return qVideoImportFormat;
        }
    }

    /* JADX WARN: Code duplicated, block: B:28:0x0039  */
    /* JADX WARN: Code duplicated, block: B:30:0x003f  */
    /* JADX WARN: Code duplicated, block: B:43:0x0054  */
    public static int caculateVideoBitrate(QEngine engine, int codecType, int fps, int width, int height, int bitratemode, int encType, int profile) {
        float f10;
        float f11;
        boolean z10 = encType == 512;
        if (encType == 1024 && !QueryHWEncCap(engine, codecType, width, height)) {
            z10 = true;
        }
        if (z10 && bitratemode == 3 && codecType == 4) {
            return 0;
        }
        if (codecType == 4) {
            if (width <= 0 || height <= 0 || fps <= 0) {
                return 0;
            }
            f11 = 0.18518518f;
            if (z10) {
                if (profile == 3) {
                    if (bitratemode == 1) {
                        f11 = 0.11111111f;
                    } else if (bitratemode != 4) {
                        f11 = 0.14814815f;
                    } else {
                        f11 = 0.074074075f;
                    }
                } else if (profile == 2) {
                    if (bitratemode == 1) {
                        f11 = 0.14814815f;
                    } else if (bitratemode == 4) {
                        f11 = 0.11111111f;
                    }
                } else if (bitratemode != 1) {
                    if (bitratemode != 4) {
                        f11 = 0.22222222f;
                    } else {
                        f11 = 0.14814815f;
                    }
                }
            } else if (bitratemode != 1) {
                if (bitratemode == 4) {
                    f11 = 0.14814815f;
                } else {
                    f11 = 0.22222222f;
                }
            }
            if (fps < 10) {
                fps = 10;
            }
            f10 = width;
        } else {
            if (codecType != 12) {
                int i10 = (int) (width * height * fps * 0.6666667f);
                if (i10 > 768000) {
                    return i10;
                }
                return 768000;
            }
            if (width <= 0 || height <= 0 || fps <= 0) {
                return 0;
            }
            if (fps < 10) {
                fps = 10;
            }
            f10 = width;
            f11 = 0.055555556f;
        }
        return (int) (f11 * f10 * height * fps);
    }

    /* JADX WARN: Code duplicated, block: B:24:0x0072 A[PHI: r3
      0x0072: PHI (r3v13 int) = (r3v12 int), (r3v20 int) binds: [B:23:0x0070, B:20:0x0061] A[DONT_GENERATE, DONT_INLINE]] */
    public static int calStoryboardFps(QStoryboard storyboard, int maxExpFPS) {
        QClip clip;
        int iFloatValue;
        int iIntValue = 0;
        if (storyboard != null) {
            int i10 = 0;
            while (iIntValue < storyboard.getClipCount() + 2) {
                if (iIntValue == storyboard.getClipCount()) {
                    clip = storyboard.getClip(-1);
                } else {
                    clip = iIntValue == storyboard.getClipCount() + 1 ? storyboard.getClip(-2) : storyboard.getClip(iIntValue);
                }
                if (clip != null) {
                    if (((Integer) clip.getProperty(12289)).intValue() == 1) {
                        iFloatValue = ((((QVideoInfo) clip.getProperty(12291)).get(9) / 1000) * 10000) / ((int) (((Float) clip.getProperty(12293)).floatValue() * 10000.0f));
                        if (iFloatValue > maxExpFPS) {
                            iFloatValue = maxExpFPS;
                        }
                        if (iFloatValue > i10) {
                            i10 = iFloatValue;
                        }
                    } else {
                        iFloatValue = ((QVideoInfo) clip.getProperty(12291)).get(9) / 1000;
                        if (iFloatValue > i10) {
                            i10 = iFloatValue;
                        }
                    }
                }
                iIntValue++;
            }
            iIntValue = ((Integer) storyboard.getProperty(QStoryboard.PROP_STUFF_CLIP_FPS)).intValue();
            if (iIntValue > maxExpFPS) {
                iIntValue = maxExpFPS;
            }
            if (iIntValue <= i10) {
                iIntValue = i10;
            }
        }
        int iFloatValue2 = (iIntValue * 10000) / ((int) (((Float) storyboard.getProperty(QStoryboard.PROP_TIME_SCALE)).floatValue() * 10000.0f));
        if (iFloatValue2 <= maxExpFPS) {
            maxExpFPS = iFloatValue2;
        }
        if (maxExpFPS < 10) {
            return 10;
        }
        return maxExpFPS;
    }

    public static int convertPosition(int position, float ftimescale, boolean bToOriginal) {
        if (ftimescale < 1.0E-6f) {
            ftimescale = 1.0f;
        }
        int i10 = (int) (ftimescale * 10000.0f);
        return (int) (bToOriginal ? (((long) position) * 10000) / ((long) i10) : (((long) position) * ((long) i10)) / 10000);
    }

    public static int geGPURender(byte[] gpuRender, int[] strLen) {
        return nativeGPURender(gpuRender, strLen);
    }

    public static int generateSVGFile(String svgFile, String fontFile, String bubbleFile, String text, int bubbleColor, int textColor) {
        return nativeGenerateSVGFile(svgFile, fontFile, bubbleFile, text, bubbleColor, textColor);
    }

    public static int getAnimatedFrameBitmap(QEngine engine, String szFrameFile, int pos, QBitmap bitmap) {
        return nativeGetAnimatedFrameBitmap(engine, szFrameFile, pos, bitmap);
    }

    public static QStyle.QAnimatedFrameTemplateInfo getAnimatedFrameInfo(QEngine engine, String szFrameFile, QSize bgSize) {
        return nativeGetAnimatedFrameInfo(engine, szFrameFile, bgSize);
    }

    public static float getAudioDeltaPitch(float fTimeScale) {
        if (fTimeScale < 0.1f || fTimeScale > 10.0f) {
            return 0.0f;
        }
        float f10 = 1.0f / fTimeScale;
        if (f10 >= 1.0f) {
            return (f10 - 1.0f) * 12.0f;
        }
        if (0.0f >= f10 || f10 >= 1.0f) {
            return 0.0f;
        }
        return (-6.0f) / f10;
    }

    private static int getCpuNumber() {
        try {
            return new File("/sys/devices/system/cpu/").listFiles(new FileFilter() { // from class: xiaoying.engine.base.QUtils.1CpuFilter
                @Override // java.io.FileFilter
                public boolean accept(File pathname) {
                    return Pattern.matches("cpu[0-9]", pathname.getName());
                }
            }).length;
        } catch (Exception e10) {
            e10.printStackTrace();
            return 1;
        }
    }

    public static int getHWCodecCap(String strXMLFile, int[] MAXDecCount, boolean[] isEncSupported, int[] vImportFormat, byte[] gpuRender, int[] strLen, boolean[] betaTestedFlag) {
        return nativeGetHWCodecCap(strXMLFile, MAXDecCount, isEncSupported, vImportFormat, gpuRender, strLen, betaTestedFlag);
    }

    public static int getLayoutMode(int width, int height) {
        if (width == 0 || height == 0) {
            return 2;
        }
        if (width == height) {
            return 16;
        }
        float f10 = width / height;
        if (width > height) {
            float f11 = f10 - 1.3333334f;
            float f12 = f10 - 1.7777778f;
            if (f11 < 0.0f) {
                f11 = -f11;
            }
            if (f12 < 0.0f) {
                f12 = -f12;
            }
            return f11 < f12 ? 2 : 8;
        }
        float f13 = f10 - 0.75f;
        float f14 = f10 - 0.5625f;
        if (f13 < 0.0f) {
            f13 = -f13;
        }
        if (f14 < 0.0f) {
            f14 = -f14;
        }
        return f13 < f14 ? 1 : 4;
    }

    public static String getMaxCpuFreq() {
        String str;
        try {
            InputStream inputStream = new ProcessBuilder("/system/bin/cat", "/sys/devices/system/cpu/cpu0/cpufreq/cpuinfo_max_freq").start().getInputStream();
            byte[] bArr = new byte[24];
            str = "";
            while (inputStream.read(bArr) != -1) {
                str = str + new String(bArr);
            }
            inputStream.close();
        } catch (Exception e10) {
            e10.printStackTrace();
            str = "N/A";
        }
        return str.trim();
    }

    public static QSize getSVGOriginalSize(String svgFile) {
        return nativeGetSVGOriginalSize(svgFile);
    }

    public static QBitmap getSVGThumbnail(QEngine engine, QBubbleTextSource bubbleSource, int colorSpace, int width, int height, int contentWidth, int contentHeight) {
        QBitmap qBitmapCreateQBitmapBlank = QBitmapFactory.createQBitmapBlank(width, height, colorSpace);
        if (qBitmapCreateQBitmapBlank == null) {
            return null;
        }
        if (nativeGetSVGThumbnail(engine, qBitmapCreateQBitmapBlank, bubbleSource, contentWidth, contentHeight) == 0) {
            return qBitmapCreateQBitmapBlank;
        }
        qBitmapCreateQBitmapBlank.recycle();
        return null;
    }

    public static QSourceExtInfo getSourceExtInfo(QEngine engine, String srcFile) {
        return nativeGetSourceExtInfo(engine, srcFile);
    }

    public static QUserData getTemplateParamData(QEngine engine, String template, int cfgIdx, QSize resolution) {
        return nativeGetTemplateParamData(engine, template, cfgIdx, resolution);
    }

    public static int getThemeCover(QEngine engine, String template, QMediaSource[] source, int width, int height, String coverFile) {
        return nativeGetThemeCover(engine, template, source, width, height, coverFile);
    }

    public static QVideoInfo getVideoInfo(QEngine engine, String sourceVideo) {
        return nativeGetVideoInfo(engine, sourceVideo);
    }

    public static int getVideoInfoAndSrcExtInfo(QEngine engine, String srcFile, QVideoInfo videoInfo, QSourceExtInfo extInfo) {
        return nativeGetVideoInfoAndSrcExtInfo(engine, srcFile, videoInfo, extInfo);
    }

    public static String getWMTagFromFile(String videoFile) {
        return nativeGetWMTagFromFile(videoFile);
    }

    public static int isFileEditable(QEngine engine, String fileName, int flag) {
        return nativeFileEditable(engine, fileName, flag);
    }

    public static boolean isPureBG(QBitmap bitmap, int[] color, QPoint point, byte[] colorType) {
        return nativeIsPureBG(bitmap, color, point, colorType);
    }

    public static boolean isSupportExtractAudioOnly(QEngine engine, String inputFilePath, String outputFilePath) {
        return nativeIsSupportExtractAudioOnly(engine, inputFilePath, outputFilePath);
    }

    private static native int nativeCacheSegMaskFile(QEngine engine, String strMaskFile, String strSrcFile, QSize srcSize);

    private static native int nativeExportAudio(QEngine engine, String inputFilePath, String outputFilePath, QRange trimRange);

    private static native int nativeFileEditable(QEngine engine, String strFileName, int iFlag);

    private static native int nativeGPURender(byte[] gpuRender, int[] strLen);

    private static native int nativeGenerateSVGFile(String strSVGFile, String fontFile, String bubbleFile, String text, int bubbleColor, int textColor);

    private static native int nativeGenerateSVGFileFromTemplate(int layoutMode, String dstSVGFile, String fontFile, String bubbleTemplateFile, String text, int bubbleColor, int textColor, Integer textLines);

    private static native int nativeGetAnimatedFrameBitmap(QEngine engine, String szFrameFile, int pos, QBitmap bitmap);

    private static native QStyle.QAnimatedFrameTemplateInfo nativeGetAnimatedFrameInfo(QEngine engine, String szFrameFile, QSize bgSize);

    private static native int nativeGetCurveTime(QClip.QCurveSpeedPoints points, int srcLen);

    private static native int nativeGetCurveTimeWithDensity(QClip.QCurveSpeedPoints points, int srcLen, int density);

    private static native int nativeGetFileMetaData(QEngine engine, QMetaData metaData, String strFilePath);

    private static native int nativeGetGopTime(QEngine engine, String file);

    private static native boolean nativeGetHWBetaTestedFlag(QEngine engine);

    private static native int nativeGetHWCodecCap(String strXMLFile, int[] MAXHWDecCount, boolean[] isEncSupported, int[] vImportFormat, byte[] gpuRender, int[] strLen, boolean[] betaTestedFlag);

    private static native int nativeGetHWVDecoderCount(QEngine engine);

    private static native int nativeGetHWVDecoderVersion();

    private static native int nativeGetHWVEncoderVersion();

    private static native int nativeGetImageRealFormat(String strFilePath);

    private static native String nativeGetPicRealFilePath(String inputFilePath);

    private static native int nativeGetProjectVersion(String strFilePath);

    private static native QSize nativeGetSVGOriginalSize(String svgFile);

    private static native int nativeGetSVGThumbnail(QEngine engine, QBitmap thumbnail, QBubbleTextSource bubbleSource, int contentWidth, int contentHeight);

    private static native int nativeGetSegAlgoSize(QSize srcSize, QSize segSize);

    private static native String nativeGetSegCacheFilePath(QEngine engine, String strSrcFile, QSize srcSize, QSize segSize);

    private static native QSourceExtInfo nativeGetSourceExtInfo(QEngine engine, String srcFile);

    private static native QUserData nativeGetTemplateParamData(QEngine engine, String template, int cfgIdx, QSize resolution);

    private static native int nativeGetThemeCover(QEngine engine, String template, QMediaSource[] source, int width, int height, String coverFile);

    private static native QVideoInfo nativeGetVideoInfo(QEngine engine, String sourceVideo);

    private static native int nativeGetVideoInfoAndSrcExtInfo(QEngine engine, String srcFile, QVideoInfo videoInfo, QSourceExtInfo srcExtInfo);

    private static native String nativeGetWMTagFromFile(String videoFile);

    private static native boolean nativeIsInterlaceFile(QEngine engine, String strFilePath);

    private static native boolean nativeIsNeedTranscode(QEngine engine, QVideoImportParam vImporParam, int[] videoResType, int[] codeType);

    private static native boolean nativeIsPureBG(QBitmap bitmap, int[] color, QPoint point, byte[] colorType);

    private static native boolean nativeIsSupportExtractAudioOnly(QEngine engine, String inputFilePath, String outputFilePath);

    private static native Object nativeObjectFromXml(QEngine engine, String xmlPath, int nType);

    private static native int nativeObjectToXml(QEngine engine, Object obj, String xmlPath);

    private static native int nativePreprocessImg(QEngine engine, String inputImg, String outputImg, PreprocessArgs args);

    private static native int nativeQueryHWDecCap(QEngine engine, int videoformat, int width, int height, boolean bInterlace);

    private static native boolean nativeQueryHWEncCap(QEngine engine, int videoformat, int width, int height);

    private static native int nativeReleaseAllHWDecoder(QEngine engine);

    private static native int nativeSavePngFromQBitmap(QBitmap bitmap, String filePath);

    private static native int nativeSetEnableHWDecoderPool(QEngine engine, boolean bEnable);

    private static native Object[] nativetGetMaterialNeedSupportList(String templatePath);

    public static int preprocessImg(QEngine engine, String inputImg, String outputImg, PreprocessArgs args) {
        return nativePreprocessImg(engine, inputImg, outputImg, args);
    }
}
