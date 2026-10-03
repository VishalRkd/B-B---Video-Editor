package xiaoying.engine.clip;

import xiaoying.engine.QEngine;
import xiaoying.engine.base.QRange;
import xiaoying.engine.base.QSession;
import xiaoying.engine.base.QSourceExtInfo;
import xiaoying.engine.base.QStyle;
import xiaoying.engine.base.QVEError;
import xiaoying.engine.base.QVideoInfo;
import xiaoying.utils.QBitmap;
import xiaoying.utils.QPoint;
import xiaoying.utils.QRect;

/* JADX INFO: loaded from: classes19.dex */
public class QClip extends QSession {
    public static final int AUDIO_STRCH_ALGO_RB = 1;
    public static final int AUDIO_STRECH_ALGO_ST = 0;
    public static final int CLIP_FLIP_NONE = 0;
    public static final int CLIP_FLIP_X = 1;
    public static final int CLIP_FLIP_Y = 2;
    public static final int CLIP_TYPE_BASE_INTERNAL = 4096;
    public static final int COVER_TYPE_BACKCOVER = 2;
    public static final int COVER_TYPE_COVER = 1;
    public static final int COVER_TYPE_NONE = 0;
    public static final int PROP_AUDIO_ADJUSTDB = 12299;
    public static final int PROP_AUDIO_DISABLED = 12300;
    public static final int PROP_AUDIO_FADEIN = 12297;
    public static final int PROP_AUDIO_FADEOUT = 12298;
    public static final int PROP_AUDIO_MODIFY_BY_ASP = 12332;
    public static final int PROP_AUDIO_PITCH_DELTA = 12331;
    private static final int PROP_BASE = 12288;
    public static final int PROP_BUBBLE_BG_COLOR = 12306;
    public static final int PROP_BUBBLE_HOR_REVERSAL = 12308;
    public static final int PROP_BUBBLE_REGION_RATIO = 12311;
    public static final int PROP_BUBBLE_ROTATE_ANGLE = 12309;
    public static final int PROP_BUBBLE_ROTATE_CENTER = 12310;
    public static final int PROP_BUBBLE_TRANSPARENCY = 12312;
    public static final int PROP_BUBBLE_VER_REVERSAL = 12307;
    public static final int PROP_CLIP_ALGO_CACHE_UUID = 12389;
    public static final int PROP_CLIP_ALGO_FRAME_INTERPOLATE_RANGE = 12383;
    public static final int PROP_CLIP_ALGO_SIZE = 53264;
    public static final int PROP_CLIP_AUDIO_ENABLE_LOUDNESS = 12386;
    public static final int PROP_CLIP_AUDIO_GAIN = 12347;
    public static final int PROP_CLIP_AUDIO_GAIN_KEY_FRAME_ENABLE = 12388;
    public static final int PROP_CLIP_AUDIO_IS_NEED_NSX = 12349;
    public static final int PROP_CLIP_AUDIO_LOUDNESS_VALUE = 12385;
    public static final int PROP_CLIP_AUDIO_MIX_PERCENT = 12319;
    public static final int PROP_CLIP_AUDIO_STRECH_ALGO = 53268;
    public static final int PROP_CLIP_CAM_EXPORT_EFFECT_DATA_LIST = 12343;
    public static final int PROP_CLIP_CLIP_SEG_MASK = 12363;
    public static final int PROP_CLIP_CROP_REGION = 12314;
    public static final int PROP_CLIP_CURVE_SCALE_RANGE = 12366;
    public static final int PROP_CLIP_CURVE_SPEED_POINTS = 12362;
    public static final int PROP_CLIP_CURVE_SRC_RANGE = 12365;
    public static final int PROP_CLIP_CUSTOM_AUDIO_INFO = 12392;
    public static final int PROP_CLIP_DISPLAY_3D_TRANSFROM = 12373;
    public static final int PROP_CLIP_DISPLAY_CROP = 12380;
    public static final int PROP_CLIP_ENABLE_DISPLAY_CROP = 12381;
    public static final int PROP_CLIP_ENABLE_LOOP_MODE = 12356;
    public static final int PROP_CLIP_ENABLE_VIDEO_CROP = 12378;
    public static final int PROP_CLIP_EQ_BAND_FREQUENCY = 12354;
    public static final int PROP_CLIP_EQ_BAND_VALUE = 12353;
    public static final int PROP_CLIP_EQ_BAND_VALUE_LIST = 12355;
    public static final int PROP_CLIP_FACEMORPHING_DISABLE_CROP = 12376;
    public static final int PROP_CLIP_FILE_MISSING = 12320;
    public static final int PROP_CLIP_FLIP = 12342;
    public static final int PROP_CLIP_INVERSE_PLAY_AUDIO_FLAG = 12360;
    public static final int PROP_CLIP_INVERSE_PLAY_SOURCE_RANGE = 12346;
    public static final int PROP_CLIP_INVERSE_PLAY_TRIM_RANGE = 12345;
    public static final int PROP_CLIP_INVERSE_PLAY_VIDEO_FLAG = 12344;
    public static final int PROP_CLIP_IS_FRAME_MODE = 12337;
    public static final int PROP_CLIP_IS_REVERSE_CLIP = 12326;
    public static final int PROP_CLIP_IS_REVERSE_MODE = 12325;
    public static final int PROP_CLIP_IS_TIME_SCALE_USE_AUDIO_PITCH = 12361;
    public static final int PROP_CLIP_LOOP_RANGE = 12357;
    public static final int PROP_CLIP_LYRIC_ENABLE = 12336;
    public static final int PROP_CLIP_MEDIA_DURATION = 12341;
    public static final int PROP_CLIP_NEED_ALPHA = 12391;
    public static final int PROP_CLIP_NORMAL_SOURCE = 12350;
    public static final int PROP_CLIP_ORG_TRIM_RANGE = 12396;
    public static final int PROP_CLIP_PANZOOM_DISABLED = 12321;
    public static final int PROP_CLIP_REPLACE_AUDIO_SOURCE = 12394;
    public static final int PROP_CLIP_REPLACE_AUDIO_SOURCE_POS = 12395;
    public static final int PROP_CLIP_RESET_SEG_MASK = 12367;
    public static final int PROP_CLIP_REVERSE_SOURCE = 12327;
    public static final int PROP_CLIP_REVERSE_SOURCE_CLEAR = 12358;
    public static final int PROP_CLIP_REVERSE_TRIM_MDOE = 12339;
    public static final int PROP_CLIP_REVERSE_TRIM_RANGE = 12340;
    public static final int PROP_CLIP_ROTATION = 12315;
    public static final int PROP_CLIP_SCENE_DURATION = 12333;
    public static final int PROP_CLIP_SCENE_TEXT_DISABLE = 12400;
    public static final int PROP_CLIP_SINGLE_FRAME_PARAM = 12323;
    public static final int PROP_CLIP_SRC_RANGE = 12318;
    public static final int PROP_CLIP_TRANSFORM_INF = 12379;
    public static final int PROP_CLIP_TRANSITION_AUDIO_GAIN = 12390;
    public static final int PROP_CLIP_UNIQUE_IDENTIFIER = 12348;
    public static final int PROP_CLIP_USE_SURFACETEXTURE = 12322;
    public static final int PROP_CLIP_UUID = 12359;
    public static final int PROP_CLIP_VFI_TYPE = 12384;
    public static final int PROP_CLIP_VIDEO_CROP_BOX_JSON_URL = 12377;
    public static final int PROP_CLIP_VIDEO_CROP_MODE = 12382;
    public static final int PROP_CLIP_WATERMARK_CACHED = 12338;
    public static final int PROP_COVER_TYPE = 12313;
    public static final int PROP_ITEM_BASE = 53248;
    public static final int PROP_PRIMAL_AUDIO_DISABLED = 12301;
    public static final int PROP_PRIMAL_VIDEO_DISABLED = 12305;
    public static final int PROP_RESAMPLE_MODE = 12295;
    public static final int PROP_SOURCE = 12290;
    public static final int PROP_SOURCE_INFO = 12291;
    public static final int PROP_TIME_SCALE = 12293;
    public static final int PROP_TRANSITION = 12294;
    public static final int PROP_TRIM_RANGE = 12292;
    public static final int PROP_TYPE = 12289;
    public static final int PROP_USERDATA = 12296;
    public static final int PROP_VIDEO_DISABLED = 12304;
    public static final int PROP_VIDEO_FADEIN = 12302;
    public static final int PROP_VIDEO_FADEOUT = 12303;
    public static final int STORYBOARD_CLIP = 4098;
    public static final int TYPE_AUDIO = 3;
    private static final int TYPE_BASE = 0;
    public static final int TYPE_BUBBLETEXT = 6;
    public static final int TYPE_IMAGE = 2;
    public static final int TYPE_SVG = 4;
    public static final int TYPE_SWF = 5;
    public static final int TYPE_VIDEO = 1;
    public static final Float AUDIO_PITCH_DELTA_VALUE_WOMAN_STYLE = Float.valueOf(4.0f);
    public static final Float AUDIO_PITCH_DELTA_VALUE_MEN_STYLE = Float.valueOf(-7.0f);
    private long tmpbufferhandle = 0;
    private long nativeThumbnailManager = 0;
    private long sphandle = 0;
    private long spweakhandle = 0;
    private long spaehandle = 0;
    protected long spweakaehandle = 0;

    public static class QCamExportedEffectData {
        public long mlTemplateID = 0;
        public QStyle.QEffectPropertyData[] mPropData = null;
    }

    public static class QCurveSpeedPoints {
        public int iMaxScale = 1;
        public QPoint[] points = null;
        public int iDensity = 0;
    }

    public static class QVideoShotInfo {
        public int frameIdx = 0;
        public int shotCropMode = 0;
        public int timestamp = 0;
    }

    private native int nativeAdjustCropBoxByJson(long handle, String strJsonPath, int frameNumber);

    private native int nativeAdjustCropBoxByJsonAEWrapper(long handle, String strJsonPath, int frameNumber);

    private native int nativeAdjustCropBoxByRect(long handle, QRect rect, int frameNumber);

    private native int nativeAdjustCropBoxByRectAEWrapper(long handle, QRect rect, int frameNumber);

    private native int nativeCreate(QEngine engine, QMediaSource source, QClip clip);

    private native int nativeCreateAEWrapper(QEngine engine, QMediaSource source, QClip clip);

    private native int nativeCreateThumbnailManager(int width, int height, int resampleMode, boolean bPrimal, boolean bOnlyOriginalClip);

    private native int nativeCreateThumbnailManagerAEWrapper(int width, int height, int resampleMode, boolean bPrimal, boolean bOnlyOriginalClip);

    private native int nativeCreateWithInfo(QEngine engine, QMediaSource source, int clipType, QVideoInfo videoInfo, QSourceExtInfo extInfo);

    private native int nativeCreateWithInfoAEWrapper(QEngine engine, QMediaSource source, int clipType, QVideoInfo videoInfo, QSourceExtInfo extInfo);

    private native int nativeDestroy(QClip clip);

    private native int nativeDestroyAEWrapper(QClip clip);

    private native int nativeDestroyThumbnailManager(long nativeThumbnailManager);

    private native int nativeDestroyThumbnailManagerAEWrapper(long nativeThumbnailManager);

    private native int nativeDuplicate(QClip srcClip, QClip destClip);

    private native int nativeDuplicateAEWrapper(QClip srcClip, QClip destClip);

    private native int nativeExtractAudioSample(long handle, int position, int milliseconds, byte[] leftSampleBuf, byte[] rightSampleBuf, Integer[] values);

    private native int nativeExtractAudioSampleAEWrapper(long handle, int position, int milliseconds, byte[] leftSampleBuf, byte[] rightSampleBuf, Integer[] values);

    private native QEffect nativeGetAudioKeyframeEffect(long handle);

    private native QEffect nativeGetAudioKeyframeEffectAEWrapper(long handle);

    private native QRect nativeGetCropBoxByFrameNumber(long handle, int frame);

    private native QRect nativeGetCropBoxByFrameNumberAEWrapper(long handle, int frame);

    private native QRect nativeGetCropBoxByTimestamp(long handle, int timestamp);

    private native QRect nativeGetCropBoxByTimestampAEWrapper(long handle, int timestamp);

    private native int nativeGetCropBoxTimestampByFrameNumber(long handle, int frame);

    private native int nativeGetCropBoxTimestampByFrameNumberAEWrapper(long handle, int frame);

    private native QRange nativeGetCurveRange(long handle, QRange range, boolean bSrc);

    private native QRange nativeGetCurveRangeAEWrapper(long handle, QRange range, boolean bSrc);

    private native float nativeGetCurveScaleByTime(long handle, int srcTime);

    private native float nativeGetCurveScaleByTimeAEWrapper(long handle, int srcTime);

    private native QEffect nativeGetEffect(long handle, int effectTrackType, int groupId, int index);

    private native QEffect nativeGetEffectAEWrapper(long handle, int effectTrackType, int groupId, int index);

    private native QEffect nativeGetEffectByUuid(long handle, String strUuid);

    private native QEffect nativeGetEffectByUuidAEWrapper(long handle, String strUuid);

    private native int nativeGetEffectCount(long handle, int effectTrackType, int groupId);

    private native int nativeGetEffectCountAEWrapper(long handle, int effectTrackType, int groupId);

    private native int nativeGetKeyFramePositonFromThumbnailMgr(long handle, int position, boolean bNext);

    private native int nativeGetKeyFramePositonFromThumbnailMgrAEWrapper(long handle, int position, boolean bNext);

    private native int nativeGetKeyframe(long handle, QBitmap bitmap, int position, boolean skipBlackFrame, int decoderUsageType);

    private native int nativeGetKeyframeAEWrapper(long handle, QBitmap bitmap, int position, boolean skipBlackFrame, int decoderUsageType);

    private native Object nativeGetProp(long handle, int propertyId);

    private native Object nativeGetPropAEWrapper(long handle, int propertyId);

    private native int nativeGetThumbnail(long handle, QBitmap bitmap, int position, boolean skipBlackFrame);

    private native int nativeGetThumbnailAEWrapper(long handle, QBitmap bitmap, int position, boolean skipBlackFrame);

    private native QVideoShotInfo[] nativeGetVideoShotArray(long handle);

    private native QVideoShotInfo[] nativeGetVideoShotArrayAEWrapper(long handle);

    private native int[] nativeGetVideoShotTimestampArray(long handle);

    private native int[] nativeGetVideoShotTimestampArrayAEWrapper(long handle);

    private native int nativeInsertEffect(long handle, QEffect effect);

    private native int nativeInsertEffectAEWrapper(long handle, QEffect effect);

    private native QEffect nativeMergeEffect(long handle, QEffect[] effectList);

    private native QEffect nativeMergeEffectAEWrapper(long handle, QEffect[] effectList);

    private native int nativeMoveEffect(long handle, QEffect effect, int index);

    private native int nativeMoveEffectAEWrapper(long handle, QEffect effect, int index);

    private native int nativeRefreshThumbnailStream(long handle, int opCode, QClip clip, QEffect effect);

    private native int nativeRefreshThumbnailStreamAEWrapper(long handle, int opCode, QClip clip, QEffect effect);

    private native int nativeRemoveEffect(long handle, QEffect effect);

    private native int nativeRemoveEffectAEWrapper(long handle, QEffect effect);

    private native int nativeReplaceWithSrc(QMediaSource clipSource, QRange srcRange, QRange trimRange);

    private native int nativeReplaceWithSrcAEWrapper(QMediaSource clipSource, QRange srcRange, QRange trimRange);

    private native QEffect[] nativeSeparationEffect(long handle, QEffect effectGroup);

    private native QEffect[] nativeSeparationEffectAEWrapper(long handle, QEffect effectGroup);

    private native int nativeSetProp(long handle, int propertyId, Object data);

    private native int nativeSetPropAEWrapper(long handle, int propertyId, Object data);

    private native QEffect nativeSwitchToEffect(long handle, float layerID, int groupID);

    public int adjustCropBoxByJson(String strJsonPath, int frameNumber) {
        long j10 = this.handle;
        if (0 == j10 && 0 == this.spweakaehandle) {
            return QVEError.QERR_COMMON_JAVA_NOT_INIT;
        }
        return this.frameworkVersion == 393216 ? nativeAdjustCropBoxByJsonAEWrapper(this.spweakaehandle, strJsonPath, frameNumber) : nativeAdjustCropBoxByJson(j10, strJsonPath, frameNumber);
    }

    public int adjustCropBoxByRect(QRect rect, int frameNumber) {
        long j10 = this.handle;
        if (0 == j10 && 0 == this.spweakaehandle) {
            return QVEError.QERR_COMMON_JAVA_NOT_INIT;
        }
        return this.frameworkVersion == 393216 ? nativeAdjustCropBoxByRectAEWrapper(this.spweakaehandle, rect, frameNumber) : nativeAdjustCropBoxByRect(j10, rect, frameNumber);
    }

    public synchronized int createThumbnailManager(int width, int height, int resampleMode, boolean bOnlyOriginalClip) {
        if (this.frameworkVersion == 393216) {
            return nativeCreateThumbnailManagerAEWrapper(width, height, resampleMode, false, bOnlyOriginalClip);
        }
        return nativeCreateThumbnailManager(width, height, resampleMode, false, bOnlyOriginalClip);
    }

    public synchronized int destroyThumbnailManager() {
        try {
            long j10 = this.nativeThumbnailManager;
            if (0 != j10) {
                if (this.frameworkVersion == 393216) {
                    nativeDestroyThumbnailManagerAEWrapper(j10);
                } else {
                    nativeDestroyThumbnailManager(j10);
                }
            }
        } catch (Throwable th2) {
            throw th2;
        }
        return 0;
    }

    public int duplicate(QClip clipDst) {
        clipDst.frameworkVersion = this.frameworkVersion;
        return this.frameworkVersion == 393216 ? nativeDuplicateAEWrapper(this, clipDst) : nativeDuplicate(this, clipDst);
    }

    public int extractAudioSample(int position, int milliseconds, byte[] leftSampleBuf, byte[] rightSampleBuf, Integer[] values) {
        long j10 = this.handle;
        if (0 == j10 && 0 == this.spweakaehandle) {
            return QVEError.QERR_COMMON_JAVA_NOT_INIT;
        }
        return this.frameworkVersion == 393216 ? nativeExtractAudioSampleAEWrapper(this.spweakaehandle, position, milliseconds, leftSampleBuf, rightSampleBuf, values) : nativeExtractAudioSample(j10, position, milliseconds, leftSampleBuf, rightSampleBuf, values);
    }

    public void finalize() throws Throwable {
        try {
            unInit();
        } catch (Throwable unused) {
        }
        super.finalize();
    }

    public QEffect getAudioKeyframeEffect() {
        long j10 = this.handle;
        if (0 == j10 && 0 == this.spweakaehandle) {
            return null;
        }
        return this.frameworkVersion == 393216 ? nativeGetAudioKeyframeEffectAEWrapper(this.spweakaehandle) : nativeGetAudioKeyframeEffect(j10);
    }

    public QRect getCropBoxByFrameNumber(int frame) {
        long j10 = this.handle;
        if (0 == j10 && 0 == this.spweakaehandle) {
            return null;
        }
        return this.frameworkVersion == 393216 ? nativeGetCropBoxByFrameNumberAEWrapper(this.spweakaehandle, frame) : nativeGetCropBoxByFrameNumber(j10, frame);
    }

    public QRect getCropBoxByTimestamp(int timestamp) {
        long j10 = this.handle;
        if (0 == j10 && 0 == this.spweakaehandle) {
            return null;
        }
        return this.frameworkVersion == 393216 ? nativeGetCropBoxByTimestampAEWrapper(this.spweakaehandle, timestamp) : nativeGetCropBoxByTimestamp(j10, timestamp);
    }

    public int getCropBoxTimestampByFrameNumber(int frame) {
        long j10 = this.handle;
        if (0 == j10 && 0 == this.spweakaehandle) {
            return QVEError.QERR_COMMON_JAVA_NOT_INIT;
        }
        return this.frameworkVersion == 393216 ? nativeGetCropBoxTimestampByFrameNumberAEWrapper(this.spweakaehandle, frame) : nativeGetCropBoxTimestampByFrameNumber(j10, frame);
    }

    public QRange getCurveRange(QRange range, boolean bSrc) {
        if (range == null) {
            return null;
        }
        long j10 = this.handle;
        if (0 == j10 && 0 == this.spweakaehandle) {
            return null;
        }
        return this.frameworkVersion == 393216 ? nativeGetCurveRangeAEWrapper(this.spweakaehandle, range, bSrc) : nativeGetCurveRange(j10, range, bSrc);
    }

    public float getCurveScaleByTime(int srcTime) {
        long j10 = this.handle;
        if (0 == j10 && 0 == this.spweakaehandle) {
            return 1.0f;
        }
        return this.frameworkVersion == 393216 ? nativeGetCurveScaleByTimeAEWrapper(this.spweakaehandle, srcTime) : nativeGetCurveScaleByTime(j10, srcTime);
    }

    public QEffect getEffectByGroup(int effectTrackType, int groupId, int index) {
        long j10 = this.handle;
        if (0 == j10 && 0 == this.spweakaehandle) {
            return null;
        }
        return this.frameworkVersion == 393216 ? nativeGetEffectAEWrapper(this.spweakaehandle, effectTrackType, groupId, index) : nativeGetEffect(j10, effectTrackType, groupId, index);
    }

    public QEffect getEffectByUuid(String strUuid) {
        long j10 = this.handle;
        if (0 == j10 && 0 == this.spweakaehandle) {
            return null;
        }
        return this.frameworkVersion == 393216 ? nativeGetEffectByUuidAEWrapper(this.spweakaehandle, strUuid) : nativeGetEffectByUuid(j10, strUuid);
    }

    public int getEffectCountByGroup(int trackType, int groupId) {
        long j10 = this.handle;
        if (0 == j10 && 0 == this.spweakaehandle) {
            return QVEError.QERR_COMMON_JAVA_NOT_INIT;
        }
        return this.frameworkVersion == 393216 ? nativeGetEffectCountAEWrapper(this.spweakaehandle, trackType, groupId) : nativeGetEffectCount(j10, trackType, groupId);
    }

    public synchronized int getKeyFramePositionFromThumbnailMgr(int position, boolean bNext) {
        long j10 = this.nativeThumbnailManager;
        if (0 == j10) {
            return -1;
        }
        if (this.frameworkVersion == 393216) {
            return nativeGetKeyFramePositonFromThumbnailMgrAEWrapper(j10, position, bNext);
        }
        return nativeGetKeyFramePositonFromThumbnailMgr(j10, position, bNext);
    }

    public synchronized int getKeyframe(QBitmap thumbnail, int position, boolean skipBlackFrame, int decoderUsageType) {
        long j10 = this.handle;
        if (0 == j10 && 0 == this.spweakaehandle) {
            return QVEError.QERR_COMMON_JAVA_NOT_INIT;
        }
        if (thumbnail == null) {
            return QVEError.QERR_COMMON_JAVA_INVALID_PARAM;
        }
        return this.frameworkVersion == 393216 ? nativeGetKeyframeAEWrapper(this.spweakaehandle, thumbnail, position, skipBlackFrame, decoderUsageType) : nativeGetKeyframe(j10, thumbnail, position, skipBlackFrame, decoderUsageType);
    }

    @Override // xiaoying.engine.base.QSession
    public Object getProperty(int propertyID) {
        long j10 = this.handle;
        if (0 == j10 && 0 == this.spweakaehandle) {
            return null;
        }
        return this.frameworkVersion == 393216 ? nativeGetPropAEWrapper(this.spweakaehandle, propertyID) : nativeGetProp(j10, propertyID);
    }

    public int getRealVideoDuration() {
        Object property = getProperty(12291);
        Object property2 = getProperty(12293);
        Object property3 = getProperty(12289);
        if (property == null) {
            return 0;
        }
        int i10 = ((QVideoInfo) property).get(5);
        int iFloatValue = 10000;
        if ((property3 != null ? ((Integer) property3).intValue() : 0) != 4098 && property2 != null) {
            iFloatValue = (int) (((Float) property2).floatValue() * 10000.0f);
        }
        return (int) (((((long) i10) * ((long) iFloatValue)) + 9999) / 10000);
    }

    public synchronized int getThumbnail(QBitmap thumbnail, int position, boolean skipBlackFrame) {
        long j10 = this.handle;
        if (0 == j10 && 0 == this.spweakaehandle) {
            return QVEError.QERR_COMMON_JAVA_NOT_INIT;
        }
        if (thumbnail == null) {
            return QVEError.QERR_COMMON_JAVA_INVALID_PARAM;
        }
        return this.frameworkVersion == 393216 ? nativeGetThumbnailAEWrapper(this.spweakaehandle, thumbnail, position, skipBlackFrame) : nativeGetThumbnail(j10, thumbnail, position, skipBlackFrame);
    }

    public QVideoShotInfo[] getVideoShotArray() {
        long j10 = this.handle;
        if (0 == j10 && 0 == this.spweakaehandle) {
            return null;
        }
        return this.frameworkVersion == 393216 ? nativeGetVideoShotArrayAEWrapper(this.spweakaehandle) : nativeGetVideoShotArray(j10);
    }

    public int[] getVideoShotTimestampArray() {
        long j10 = this.handle;
        if (0 == j10 && 0 == this.spweakaehandle) {
            return null;
        }
        return this.frameworkVersion == 393216 ? nativeGetVideoShotTimestampArrayAEWrapper(this.spweakaehandle) : nativeGetVideoShotTimestampArray(j10);
    }

    public int init(QEngine engine, QMediaSource clipSource) {
        if (engine == null) {
            return QVEError.QERR_COMMON_JAVA_INVALID_PARAM;
        }
        int iIntValue = ((Integer) engine.getProperty(112)).intValue();
        this.frameworkVersion = iIntValue;
        return iIntValue == 393216 ? nativeCreateAEWrapper(engine, clipSource, this) : nativeCreate(engine, clipSource, this);
    }

    public int insertEffect(QEffect effect) {
        long j10 = this.handle;
        if (0 == j10 && 0 == this.spweakaehandle) {
            return QVEError.QERR_COMMON_JAVA_NOT_INIT;
        }
        return this.frameworkVersion == 393216 ? nativeInsertEffectAEWrapper(this.spweakaehandle, effect) : nativeInsertEffect(j10, effect);
    }

    public QEffect mergeEffect(QEffect[] effectList) {
        long j10 = this.handle;
        if (0 == j10 && 0 == this.spweakaehandle) {
            return null;
        }
        return this.frameworkVersion == 393216 ? nativeMergeEffectAEWrapper(this.spweakaehandle, effectList) : nativeMergeEffect(j10, effectList);
    }

    public int moveEffect(QEffect effect, int index) {
        long j10 = this.handle;
        if (0 == j10 && 0 == this.spweakaehandle) {
            return QVEError.QERR_COMMON_JAVA_NOT_INIT;
        }
        return this.frameworkVersion == 393216 ? nativeMoveEffectAEWrapper(this.spweakaehandle, effect, index) : nativeMoveEffect(j10, effect, index);
    }

    public int refreshThumbnailStream(int opCode, QClip clip, QEffect effect) {
        long j10 = this.handle;
        if (0 == j10 && 0 == this.spweakaehandle) {
            return QVEError.QERR_COMMON_JAVA_NOT_INIT;
        }
        return this.frameworkVersion == 393216 ? nativeRefreshThumbnailStreamAEWrapper(j10, opCode, clip, effect) : nativeRefreshThumbnailStream(j10, opCode, clip, effect);
    }

    public int removeEffect(QEffect effect) {
        long j10 = this.handle;
        if (0 == j10 && 0 == this.spweakaehandle) {
            return QVEError.QERR_COMMON_JAVA_NOT_INIT;
        }
        return this.frameworkVersion == 393216 ? nativeRemoveEffectAEWrapper(this.spweakaehandle, effect) : nativeRemoveEffect(j10, effect);
    }

    public int replaceWithSrc(QMediaSource clipSource, QRange srcRange, QRange trimRange) {
        return this.frameworkVersion == 393216 ? nativeReplaceWithSrcAEWrapper(clipSource, srcRange, trimRange) : nativeReplaceWithSrc(clipSource, srcRange, trimRange);
    }

    public QEffect[] separationEffect(QEffect effectGroup) {
        long j10 = this.handle;
        if (0 == j10 && 0 == this.spweakaehandle) {
            return null;
        }
        return this.frameworkVersion == 393216 ? nativeSeparationEffectAEWrapper(this.spweakaehandle, effectGroup) : nativeSeparationEffect(j10, effectGroup);
    }

    @Override // xiaoying.engine.base.QSession
    public int setProperty(int propertyID, Object data) {
        long j10 = this.handle;
        if (0 == j10 && 0 == this.spweakaehandle) {
            return QVEError.QERR_COMMON_JAVA_NOT_INIT;
        }
        return this.frameworkVersion == 393216 ? nativeSetPropAEWrapper(this.spweakaehandle, propertyID, data) : nativeSetProp(j10, propertyID, data);
    }

    public QEffect switchToEffect(float layerID, int groupID) {
        if (this.frameworkVersion != 393216) {
            return null;
        }
        return nativeSwitchToEffect(this.spweakaehandle, layerID, groupID);
    }

    @Override // xiaoying.engine.base.QSession
    public int unInit() {
        if (0 == this.handle && 0 == this.spweakaehandle) {
            return QVEError.QERR_COMMON_JAVA_NOT_INIT;
        }
        return this.frameworkVersion == 393216 ? nativeDestroyAEWrapper(this) : nativeDestroy(this);
    }

    public synchronized int createThumbnailManager(int width, int height, int resampleMode, boolean bPrimal, boolean bOnlyOriginalClip) {
        if (this.frameworkVersion == 393216) {
            return nativeCreateThumbnailManagerAEWrapper(width, height, resampleMode, bPrimal, bOnlyOriginalClip);
        }
        return nativeCreateThumbnailManager(width, height, resampleMode, bPrimal, bOnlyOriginalClip);
    }

    public int init(QEngine engine, QMediaSource clipSource, int clipType, QVideoInfo videoInfo, QSourceExtInfo extInfo) {
        if (engine == null) {
            return QVEError.QERR_COMMON_JAVA_INVALID_PARAM;
        }
        int iIntValue = ((Integer) engine.getProperty(112)).intValue();
        this.frameworkVersion = iIntValue;
        if (iIntValue == 393216) {
            return nativeCreateWithInfoAEWrapper(engine, clipSource, clipType, videoInfo, extInfo);
        }
        return nativeCreateWithInfo(engine, clipSource, clipType, videoInfo, extInfo);
    }

    // --- Advanced Engine Ergonomic APIs ---

    public int setTrimRange(int startMs, int lengthMs) {
        return setProperty(PROP_TRIM_RANGE, new xiaoying.engine.base.QRange(startMs, lengthMs));
    }

    public xiaoying.engine.base.QRange getTrimRange() {
        Object val = getProperty(PROP_TRIM_RANGE);
        return val instanceof xiaoying.engine.base.QRange ? (xiaoying.engine.base.QRange) val : null;
    }

    public int setTimeScale(float scale) {
        return setProperty(PROP_TIME_SCALE, Float.valueOf(scale));
    }

    public float getTimeScale() {
        Object val = getProperty(PROP_TIME_SCALE);
        return val instanceof Float ? ((Float) val).floatValue() : 1.0f;
    }

    public int setRotation(int degrees) {
        return setProperty(PROP_CLIP_ROTATION, Integer.valueOf(degrees));
    }

    public int getRotation() {
        Object val = getProperty(PROP_CLIP_ROTATION);
        return val instanceof Integer ? ((Integer) val).intValue() : 0;
    }

    public int setAudioDisabled(boolean disabled) {
        return setProperty(PROP_AUDIO_DISABLED, Boolean.valueOf(disabled));
    }

    public boolean isAudioDisabled() {
        Object val = getProperty(PROP_AUDIO_DISABLED);
        return val instanceof Boolean ? ((Boolean) val).booleanValue() : false;
    }

    public int setAudioVolume(int percent) {
        return setProperty(PROP_CLIP_AUDIO_MIX_PERCENT, Integer.valueOf(percent));
    }

    public int getAudioVolume() {
        Object val = getProperty(PROP_CLIP_AUDIO_MIX_PERCENT);
        return val instanceof Integer ? ((Integer) val).intValue() : 100;
    }

    public int setTransition(QTransition transition) {
        return setProperty(PROP_TRANSITION, transition);
    }

    public QTransition getTransition() {
        Object val = getProperty(PROP_TRANSITION);
        return val instanceof QTransition ? (QTransition) val : null;
    }

    public int setVideoFade(int fadeInMs, int fadeOutMs) {
        setProperty(PROP_VIDEO_FADEIN, Integer.valueOf(fadeInMs));
        return setProperty(PROP_VIDEO_FADEOUT, Integer.valueOf(fadeOutMs));
    }

    public int setAudioFade(int fadeInMs, int fadeOutMs) {
        setProperty(PROP_AUDIO_FADEIN, Integer.valueOf(fadeInMs));
        return setProperty(PROP_AUDIO_FADEOUT, Integer.valueOf(fadeOutMs));
    }
}
