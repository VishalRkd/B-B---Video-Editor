package xiaoying.engine;

import android.app.ActivityManager;
import android.content.Context;
import android.content.pm.ConfigurationInfo;
import android.graphics.Bitmap;
import android.media.MediaCodecInfo;
import android.media.MediaCodecList;
import xiaoying.engine.base.IQAlgoBenchLister;
import xiaoying.engine.base.IQErrorDataLister;
import xiaoying.engine.base.IQFilePathModifier;
import xiaoying.engine.base.IQFontFinder;
import xiaoying.engine.base.IQHWCodecQuery;
import xiaoying.engine.base.IQImageDataListener;
import xiaoying.engine.base.IQSessionStateListener;
import xiaoying.engine.base.IQTemplateAdapter;
import xiaoying.engine.base.IQTemplateVCMAdapter;
import xiaoying.engine.base.IQTextTransformer;
import xiaoying.engine.base.IQUserBubbleText;
import xiaoying.engine.base.QAlgoBenchData;
import xiaoying.engine.base.QBubbleTextSource;
import xiaoying.engine.base.QCBErrorData;
import xiaoying.engine.base.QMaskCache;
import xiaoying.engine.base.QSessionState;
import xiaoying.engine.base.QTextTransformerParam;
import xiaoying.utils.QBitmap;
import xiaoying.utils.text.QFontCache;

/* JADX INFO: loaded from: classes18.dex */
public class QEngine {
    public static final int FRAMEWORK_VERSION_AE = 393216;
    public static final int FRAMEWORK_VERSION_SB = 327680;
    public static final int MSAATYPE_NO = 0;
    public static final int MSAATYPE_X4 = 1;
    public static final int PERCENT_PRECISION = 10000;
    public static final int PROP_3D_FACE_DATA = 40;
    public static final int PROP_APP_CONTEXT = 32;
    public static final int PROP_CONTEXT_AE_GLOBAL_ASSET_PATH = 68;
    public static final int PROP_CONTEXT_ALGO_BENCH_ADAPTER = 81;
    private static final int PROP_CONTEXT_BASE = 0;
    public static final int PROP_CONTEXT_CARTOONLITE_CAMERA_MAX_FACE_COUNT = 110;
    public static final int PROP_CONTEXT_CARTOONLITE_MONTAGE_MAX_FACE_COUNT = 111;
    public static final int PROP_CONTEXT_COMMON_ALGO_CACHE_DIR = 103;
    public static final int PROP_CONTEXT_ERROR_DATA_ADAPTER = 100;
    public static final int PROP_CONTEXT_FRAMEWORK_VERSION = 112;
    public static final int PROP_CONTEXT_IMAGE_DATA_LISTENER = 117;
    public static final int PROP_CONTEXT_MASK_CACHE_ADAPTER = 66;
    public static final int PROP_CONTEXT_MASK_CACHE_FLAG = 65;
    public static final int PROP_CONTEXT_MASK_CACHE_PATH = 64;
    public static final int PROP_CONTEXT_PLAYER_IGNORE_SKIP_FRAME = 79;
    public static final int PROP_CONTEXT_RENDER_MSAATYPE = 50;
    public static final int PROP_CONTEXT_SEGMENT_AREA_THRESHOLD = 113;
    public static final int PROP_CONTEXT_SEGMENT_FIRST_SYNC = 75;
    public static final int PROP_CONTEXT_SEGMENT_MODE = 67;
    public static final int PROP_CONTEXT_SEGMENT_PERFORMANCE_TIME = 98;
    public static final int PROP_CONTEXT_SEGMENT_USE_VIDEO = 78;
    public static final int PROP_CONTEXT_SKELETON_CACHE_ADAPTER = 71;
    public static final int PROP_CONTEXT_SKELETON_CACHE_FLAG = 70;
    public static final int PROP_CONTEXT_SKELETON_CACHE_PATH = 69;
    public static final int PROP_CONTEXT_SMART_THEME_MIN_CLIP_DURATION = 109;
    public static final int PROP_CONTEXT_SVN_MASTER_VERSION = 92;
    public static final int PROP_CONTEXT_SVN_PATCH_VERSION = 93;
    public static final int PROP_CONTEXT_TEMPLATE_VCM_CALLBACK = 104;
    public static final int PROP_CONTEXT_THUMBNAIL_MODE = 114;
    public static final int PROP_CONTEXT_USE_REVERSE_SOURCE = 90;
    public static final int PROP_CONTEXT_VFI_FRAME_BLENDER_ONLY = 99;
    public static final int PROP_CONTEXT_WEBP_ASYNCRENDER = 102;
    public static final int PROP_DEFAULT_OUTPUT_AUDIO_FORMAT = 3;
    public static final int PROP_DEFAULT_OUTPUT_FILE_FORMAT = 4;
    public static final int PROP_DEFAULT_OUTPUT_VIDEO_FORMAT = 2;
    public static final int PROP_DEFAULT_PLAYBACK_MUTE = 7;
    public static final int PROP_DEFAULT_PLAYBACK_VOLUME = 6;
    public static final int PROP_DEFAULT_RESAMPLE_MODE = 5;
    public static final int PROP_DEF_IMAGE_FILE = 35;
    public static final int PROP_DEF_STUFF_COLOR = 41;
    public static final int PROP_DEF_STUFF_IMAGE = 42;
    public static final int PROP_FILE_PATH_MODIFIER = 28;
    public static final int PROP_FONT_FINDER = 33;
    public static final int PROP_HWCODEC_XML_PATH = 38;
    public static final int PROP_HW_CODEC_QUERY_CALLBACK = 31;
    public static final int PROP_HW_READER_LIB_PATH = 26;
    public static final int PROP_HW_WRITER_LIB_PATH = 27;
    public static final int PROP_IMAGE_SOURCE_FPS = 44;
    public static final int PROP_MAX_SUPPORT_RESOLUTION = 9;
    public static final int PROP_PLAY_DOWN_SCALE = 39;
    public static final int PROP_RENDER_API = 36;
    public static final int PROP_SEGMENT_MODEL_FILE = 48;
    public static final int PROP_STATIC_DURATION = 19;
    public static final int PROP_TEMPLATE_ADAPTER_CALLBACK = 25;
    public static final int PROP_TEMPLATE_PATH = 10;
    public static final int PROP_TEMP_PATH = 1;
    public static final int PROP_TEXT_TRANSFORMER = 34;
    public static final int PROP_THEME_DEFAULT_MUSIC_TEMPLATE_ID = 30;
    public static final int PROP_TRCFILE_DECRYPTOR = 29;
    public static final int PROP_TRIM_TYPE = 20;
    public static final int PROP_USER_BUBBLETEXT_CALLBACK = 8;

    @Deprecated
    public static final int PROP_VIDEO_SOURCE_FPS = 47;
    public static final int RENDER_API_D3D11 = 128;
    public static final int RENDER_API_Metal10 = 32;
    public static final int RENDER_API_OpenGLES20 = 16;
    public static final int RENDER_API_OpenGLES30 = 17;
    public static final int RENDER_API_OpenGLES31 = 18;
    public static final int RENDER_API_OpenGLES32 = 19;
    public static final int RENDER_API_UNKNOW = 0;
    public static final int RENDER_API_Vulkan10 = 64;
    public static final int TRIM_FLAG_LEFT_KEYFRAME = 1;
    public static final int TRIM_FLAG_NORMAL = 0;
    public static final int TRIM_FLAG_RIGHT_KEYFRAME = 2;
    public static final int USE_CODEC_TYPE_HW_FIRST_SW_SECOND = 2;
    public static final int USE_CODEC_TYPE_HW_ONLY = 0;
    public static final int USE_CODEC_TYPE_SW_FIRST_HW_SECOND = 3;
    public static final int USE_CODEC_TYPE_SW_ONLY = 1;
    public static final int VERSION_NUMBER = 393216;
    private static final long mRemainMem = 606208000;
    private long amcmHandle = 0;
    private long engineHandle = 0;
    private IQUserBubbleText userBubbleText = null;
    private IQTemplateAdapter templateAdapter = null;
    private long jniglobalobjectref = 0;
    private IQHWCodecQuery hwcodecquery = null;
    private IQFontFinder fontfinder = null;
    private IQTextTransformer texttransformer = null;
    private IQFilePathModifier pathModifier = null;
    private IQSessionStateListener maskMgrAdapter = null;
    private IQSessionStateListener skeletonMgrAdapter = null;
    private IQAlgoBenchLister algoBenchAdaptr = null;
    private Context mAppCtx = null;
    private IQErrorDataLister errorDataLister = null;
    private IQTemplateVCMAdapter templateVCMAdapter = null;
    private IQImageDataListener imageDataListener = null;
    private long jniTRCDecryptorGR = 0;

    public static class QEngineGLInfo {
        public Boolean bSupportAE;
        public int glVersion;
    }

    public static class QEngineSupportArray {
        public String[] notSupportContent;
        public byte[] supportContent;
        public byte[] supportStream;
    }

    public static class QHardWareModelGpuInfo {
        public String GpuName;
        public boolean bGpuInWhiteList;
        public boolean bNeedCheck265Decoder;
    }

    public static class QMobileHardWareModelInfo {
        public long lCurRemainBytes;
        public int nDecodeSupportMaxUnit;
        public int nSupportSpliterInstanceCount;
    }

    private int callUserBubbleText(QBitmap cbRGB32Data, QBubbleTextSource source, int iDstWidth, int iDstHeight, Object userData) {
        IQUserBubbleText iQUserBubbleText = this.userBubbleText;
        if (iQUserBubbleText == null) {
            return 0;
        }
        return iQUserBubbleText.onUserBubbleText(cbRGB32Data, source, iDstWidth, iDstHeight, userData);
    }

    public static QEngineSupportArray getEngineSupportArray(Object[] List) {
        return nativeGetEngineSupportArray(List);
    }

    public static Object[] getEngineSupportList() {
        return nativeGetEngineSupportList();
    }

    public static QEngineGLInfo getOpenGLVersion(Context context) {
        ActivityManager activityManager;
        ConfigurationInfo deviceConfigurationInfo;
        if (context == null || (activityManager = (ActivityManager) context.getSystemService("activity")) == null || (deviceConfigurationInfo = activityManager.getDeviceConfigurationInfo()) == null) {
            return null;
        }
        QEngineGLInfo qEngineGLInfo = new QEngineGLInfo();
        qEngineGLInfo.bSupportAE = Boolean.FALSE;
        int i10 = deviceConfigurationInfo.reqGlEsVersion;
        qEngineGLInfo.glVersion = i10;
        if (((i10 >> 16) & 15) >= 3) {
            qEngineGLInfo.bSupportAE = Boolean.TRUE;
        }
        return qEngineGLInfo;
    }

    public static boolean isSupportSize(boolean bEncoder, String mime, int width, int height) {
        MediaCodecInfo.VideoCapabilities videoCapabilities;
        MediaCodecInfo.VideoCapabilities videoCapabilities2;
        int codecCount = MediaCodecList.getCodecCount();
        boolean zIsSizeSupported = false;
        for (int i10 = 0; i10 < codecCount; i10++) {
            MediaCodecInfo codecInfoAt = MediaCodecList.getCodecInfoAt(i10);
            if (codecInfoAt != null && codecInfoAt.isEncoder() == bEncoder) {
                for (String str : codecInfoAt.getSupportedTypes()) {
                    MediaCodecInfo.CodecCapabilities capabilitiesForType = codecInfoAt.getCapabilitiesForType(str);
                    if (mime != null && !mime.isEmpty() && str.compareTo(mime) == 0) {
                        return (capabilitiesForType == null || (videoCapabilities2 = capabilitiesForType.getVideoCapabilities()) == null) ? zIsSizeSupported : videoCapabilities2.isSizeSupported(width, height);
                    }
                    if ((mime == null || mime.isEmpty()) && capabilitiesForType != null && (videoCapabilities = capabilitiesForType.getVideoCapabilities()) != null && (zIsSizeSupported = videoCapabilities.isSizeSupported(width, height))) {
                        return zIsSizeSupported;
                    }
                }
            }
        }
        return zIsSizeSupported;
    }

    private native int nativeCleanCommonAlgoCache(long engineHandle, int fileSize);

    private native int nativeCleanMaskCache(long engineHandle, int fileSize);

    private native int nativeCreate(QEngine engine, String licensePath);

    private native int nativeCreateGlobalCartoonLite(long engineHandle);

    private native int nativeCreateGlobalClothSeg(long engineHandle);

    private native int nativeCreateGlobalFaceSwap(long engineHandle);

    private native int nativeCreateGlobalHandle(long engineHandle, int type);

    private native int nativeCreateGlobalPegSeg(long engineHandle);

    private native int nativeCreateGlobalSeg(long engineHandle);

    private native int nativeCreateGlobalSkeleton(long engineHandle);

    private native int nativeCreateMaskCache(long engineHandle, String filePath, QMaskCache result);

    private native int nativeDestoryGlobalCartoonLite(long engineHandle);

    private native int nativeDestoryGlobalFaceSwap(long engineHandle);

    private native int nativeDestoryGlobalHandle(long engineHandle, int type);

    private native int nativeDestoryMaskCache(long engineHandle, QMaskCache result);

    private native int nativeDestroy(QEngine engine);

    private static native QEngineSupportArray nativeGetEngineSupportArray(Object[] List);

    private static native Object[] nativeGetEngineSupportList();

    private native int[] nativeGetGlobalHandleType(long engineHandle);

    private native QHardWareModelGpuInfo nativeGetHardWareModelGpuInfo(String hw_codec_cap_xml_path);

    private native QMobileHardWareModelInfo nativeGetMobileHardWareModeInfo();

    private native Object nativeGetProp(long engineHandle, int propertyID);

    private native int nativeQueryMaskCache(long engineHandle, QMaskCache result);

    private native int nativeResetGlobalClothSeg(long engineHandle);

    private native int nativeResetGlobalPegSeg(long engineHandle);

    private native int nativeResetGlobalSeg(long engineHandle);

    private native int nativeResetGlobalSkeleton(long engineHandle);

    private native int nativeSetAlgoResult(long taskId, int errCode, String path);

    private native int nativeSetProp(long engineHandle, int propertyID, Object data);

    private int onAlgoBenchStatus(QAlgoBenchData state) {
        IQAlgoBenchLister iQAlgoBenchLister;
        if (state == null || (iQAlgoBenchLister = this.algoBenchAdaptr) == null) {
            return 0;
        }
        return iQAlgoBenchLister.onCallBack(state);
    }

    private int onErrorDataStatus(QCBErrorData state) {
        IQErrorDataLister iQErrorDataLister;
        if (state == null || (iQErrorDataLister = this.errorDataLister) == null) {
            return 0;
        }
        return iQErrorDataLister.onCallBack(state);
    }

    private int onImageDataReceived(long buffer, int width, int height, int timestamp, int timespan) {
        IQImageDataListener iQImageDataListener = this.imageDataListener;
        if (iQImageDataListener == null) {
            return 0;
        }
        return iQImageDataListener.onImageDataReceived(buffer, width, height, timestamp, timespan);
    }

    private int onMaskMgrStatus(QSessionState state) {
        IQSessionStateListener iQSessionStateListener = this.maskMgrAdapter;
        if (iQSessionStateListener == null) {
            return 0;
        }
        return iQSessionStateListener.onSessionStatus(state);
    }

    private int onSkeletonMgrStatus(QSessionState state) {
        IQSessionStateListener iQSessionStateListener = this.skeletonMgrAdapter;
        if (iQSessionStateListener == null) {
            return 0;
        }
        return iQSessionStateListener.onSessionStatus(state);
    }

    public String FindFont(int fontid) {
        IQFontFinder iQFontFinder = this.fontfinder;
        if (iQFontFinder == null) {
            return null;
        }
        return iQFontFinder.FindFont(fontid);
    }

    public boolean GetHWBetaTestedFlag() {
        IQHWCodecQuery iQHWCodecQuery = this.hwcodecquery;
        if (iQHWCodecQuery == null) {
            return false;
        }
        return iQHWCodecQuery.getBetaTestedFlag();
    }

    public int GetMAXHWDecCount(int index) {
        IQHWCodecQuery iQHWCodecQuery = this.hwcodecquery;
        if (iQHWCodecQuery == null) {
            return 0;
        }
        return iQHWCodecQuery.getMAXHWDecCount(index);
    }

    public String GetTemplateExternalFile(long templateID, int subTemplateID, int fileID) {
        IQTemplateAdapter iQTemplateAdapter = this.templateAdapter;
        if (iQTemplateAdapter == null) {
            return null;
        }
        return iQTemplateAdapter.getTemplateExternalFile(templateID, subTemplateID, fileID);
    }

    public String GetTemplateFile(long templateID) {
        IQTemplateAdapter iQTemplateAdapter = this.templateAdapter;
        if (iQTemplateAdapter == null) {
            return null;
        }
        return iQTemplateAdapter.getTemplateFile(templateID);
    }

    public long GetTemplateID(String templateFile) {
        IQTemplateAdapter iQTemplateAdapter = this.templateAdapter;
        if (iQTemplateAdapter == null) {
            return 0L;
        }
        return iQTemplateAdapter.getTemplateID(templateFile);
    }

    public String GetTemplateVCMConfig(long templateID) {
        IQTemplateVCMAdapter iQTemplateVCMAdapter = this.templateVCMAdapter;
        if (iQTemplateVCMAdapter == null) {
            return null;
        }
        return iQTemplateVCMAdapter.getTemplateVCMConfig(templateID);
    }

    public String ModifyFilePath(String strFilePath) {
        IQFilePathModifier iQFilePathModifier = this.pathModifier;
        if (iQFilePathModifier == null) {
            return null;
        }
        return iQFilePathModifier.ModifyPaht(strFilePath);
    }

    public boolean QueryHWEncCap(int index) {
        IQHWCodecQuery iQHWCodecQuery = this.hwcodecquery;
        if (iQHWCodecQuery == null) {
            return false;
        }
        return iQHWCodecQuery.queryHWEncCap(index);
    }

    public int QueryVideoImportFormat(int videoImpType, int[] videoImpFormat) {
        IQHWCodecQuery iQHWCodecQuery = this.hwcodecquery;
        if (iQHWCodecQuery == null) {
            return -1;
        }
        videoImpFormat[0] = iQHWCodecQuery.queryVideoImportFormat(videoImpType);
        return 0;
    }

    public String TransformText(String orgStr, QTextTransformerParam param) {
        IQTextTransformer iQTextTransformer = this.texttransformer;
        if (iQTextTransformer == null) {
            return null;
        }
        return iQTextTransformer.TransformText(orgStr, param);
    }

    public int ViewBitmap(byte[] data, int width, int height, int color, String id2) {
        if (color == 1) {
            int length = data.length / 4;
            int[] iArr = new int[length];
            int i10 = 0;
            for (int i11 = 0; i11 < length; i11++) {
                int i12 = data[i10] & 255;
                int i13 = data[i10 + 1] & 255;
                int i14 = i10 + 3;
                int i15 = data[i10 + 2] & 255;
                i10 += 4;
                iArr[i11] = (i13 << 8) | (i12 << 16) | ((data[i14] & 255) << 24) | i15;
            }
            Bitmap bitmapCreateBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            bitmapCreateBitmap.setPixels(iArr, 0, width, 0, 0, width, height);
            bitmapCreateBitmap.getWidth();
        }
        return 0;
    }

    public int cleanCommonAlgoCache(int fileSize) {
        return nativeCleanCommonAlgoCache(this.engineHandle, fileSize);
    }

    public int cleanMaskCache(int fileSize) {
        return nativeCleanMaskCache(this.engineHandle, fileSize);
    }

    public int create(String licensePath) {
        return nativeCreate(this, licensePath);
    }

    public int createGlobalCartoonLite() {
        return nativeCreateGlobalCartoonLite(this.engineHandle);
    }

    public int createGlobalClothSeg() {
        return nativeCreateGlobalClothSeg(this.engineHandle);
    }

    public int createGlobalFaceSwap() {
        return nativeCreateGlobalFaceSwap(this.engineHandle);
    }

    public int createGlobalHandle(int type) {
        return nativeCreateGlobalHandle(this.engineHandle, type);
    }

    public int createGlobalPegSeg() {
        return nativeCreateGlobalPegSeg(this.engineHandle);
    }

    public int createGlobalSeg() {
        return nativeCreateGlobalSeg(this.engineHandle);
    }

    public int createGlobalSkeleton() {
        return nativeCreateGlobalSkeleton(this.engineHandle);
    }

    public int createMaskCache(String filePath, QMaskCache result) {
        return nativeCreateMaskCache(this.engineHandle, filePath, result);
    }

    public int destory() {
        QFontCache.Cleanup();
        return nativeDestroy(this);
    }

    public int destoryGlobalCartoonLite() {
        return nativeDestoryGlobalCartoonLite(this.engineHandle);
    }

    public int destoryGlobalFaceSwap() {
        return nativeDestoryGlobalFaceSwap(this.engineHandle);
    }

    public int destoryGlobalHandle(int type) {
        return nativeDestoryGlobalHandle(this.engineHandle, type);
    }

    public int destoryMaskCache(QMaskCache result) {
        return nativeDestoryMaskCache(this.engineHandle, result);
    }

    public int[] getGlobalHandleType() {
        return nativeGetGlobalHandleType(this.engineHandle);
    }

    public QHardWareModelGpuInfo getHardWareModelGpuInfo(String hw_codec_cap_xml_path) {
        return nativeGetHardWareModelGpuInfo(hw_codec_cap_xml_path);
    }

    public QMobileHardWareModelInfo getMobileHardWareModelInfo() {
        return nativeGetMobileHardWareModeInfo();
    }

    public Object getProperty(int propertyID) {
        return nativeGetProp(this.engineHandle, propertyID);
    }

    public long getRemainMemory(boolean[] bLowMem) {
        Context context = this.mAppCtx;
        if (context == null) {
            return mRemainMem;
        }
        ActivityManager activityManager = (ActivityManager) context.getSystemService("activity");
        ActivityManager.MemoryInfo memoryInfo = new ActivityManager.MemoryInfo();
        activityManager.getMemoryInfo(memoryInfo);
        bLowMem[0] = memoryInfo.lowMemory;
        return memoryInfo.availMem;
    }

    public int queryMaskCache(QMaskCache result) {
        return nativeQueryMaskCache(this.engineHandle, result);
    }

    public int resetGlobalClothSeg() {
        return nativeResetGlobalClothSeg(this.engineHandle);
    }

    public int resetGlobalPegSeg() {
        return nativeResetGlobalPegSeg(this.engineHandle);
    }

    public int resetGlobalSeg() {
        return nativeResetGlobalSeg(this.engineHandle);
    }

    public int resetGlobalSkeleton() {
        return nativeResetGlobalSkeleton(this.engineHandle);
    }

    public int setAlgoResult(long taskId, int errCode, String path) {
        return nativeSetAlgoResult(taskId, errCode, path);
    }

    public int setProperty(int propertyID, Object data) {
        if (propertyID == 8) {
            this.userBubbleText = (IQUserBubbleText) data;
            return 0;
        }
        if (propertyID == 25) {
            this.templateAdapter = (IQTemplateAdapter) data;
            return 0;
        }
        if (propertyID == 28) {
            this.pathModifier = (IQFilePathModifier) data;
            return 0;
        }
        if (propertyID == 66) {
            this.maskMgrAdapter = (IQSessionStateListener) data;
            return 0;
        }
        if (propertyID == 71) {
            this.skeletonMgrAdapter = (IQSessionStateListener) data;
            return 0;
        }
        if (propertyID == 81) {
            this.algoBenchAdaptr = (IQAlgoBenchLister) data;
            nativeSetProp(this.engineHandle, propertyID, data);
            return 0;
        }
        if (propertyID == 100) {
            this.errorDataLister = (IQErrorDataLister) data;
            nativeSetProp(this.engineHandle, propertyID, data);
            return 0;
        }
        if (propertyID == 104) {
            this.templateVCMAdapter = (IQTemplateVCMAdapter) data;
            return 0;
        }
        if (propertyID == 117) {
            this.imageDataListener = (IQImageDataListener) data;
            nativeSetProp(this.engineHandle, propertyID, data);
            return 0;
        }
        switch (propertyID) {
            case 31:
                this.hwcodecquery = (IQHWCodecQuery) data;
                return 0;
            case 32:
                this.mAppCtx = (Context) data;
                nativeSetProp(this.engineHandle, propertyID, data);
                return 0;
            case 33:
                this.fontfinder = (IQFontFinder) data;
                return 0;
            case 34:
                this.texttransformer = (IQTextTransformer) data;
                return 0;
            default:
                return nativeSetProp(this.engineHandle, propertyID, data);
        }
    }
}
