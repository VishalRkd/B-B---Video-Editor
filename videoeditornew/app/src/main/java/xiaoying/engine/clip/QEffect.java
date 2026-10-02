package xiaoying.engine.clip;

import xiaoying.basedef.QPointFloat;
import xiaoying.engine.QEngine;
import xiaoying.engine.base.QDrawLayerPaintPen;
import xiaoying.engine.base.QDrawLayerPaintText;
import xiaoying.engine.base.QRange;
import xiaoying.engine.base.QStyle;
import xiaoying.engine.base.QTransformInfo;
import xiaoying.engine.base.QVEError;
import xiaoying.utils.QRect;
import xiaoying.utils.QSize;

/* JADX INFO: loaded from: classes19.dex */
public class QEffect {
    public static final int ANIMATE_POINT_OPERATION_DELETE = 1;
    public static final int ANIMATE_POINT_OPERATION_DELETE_ALL = 2;
    public static final int ANIMATE_POINT_OPERATION_INSERT = 0;
    public static final int ANIMATE_POINT_OPERATION_MODIFY = 3;
    public static final int AUDIO_FRAME_MODE_ALIGN_LEFT = 2;
    public static final int AUDIO_FRAME_MODE_ALIGN_RIGHT = 3;
    public static final int AUDIO_FRAME_MODE_INVERSE = 4;
    public static final int AUDIO_FRAME_MODE_NORMAL = 0;
    public static final int AUDIO_FRAME_MODE_REPEAT = 1;
    public static final int DUPLICATE_PART_EFFECT_TYPE_DRAW = 1;
    public static final int DUPLICATE_PART_EFFECT_TYPE_NONE = 0;
    public static final int EFFECT_REGION_ALIGN_MODE_DEFALUT = 0;
    public static final int EFFECT_REGION_ALIGN_MODE_WIDTH = 0;
    public static final int EFFECT_REGION_ALIGN_MODE_WIDTH_PERCENT = 1;
    public static final int EFFECT_SUB_ITEM_TYPE_ALPHA = 3;
    public static final int EFFECT_SUB_ITEM_TYPE_APPLY_MODE_EFFECT = 1;
    public static final int EFFECT_SUB_ITEM_TYPE_APPLY_MODE_MIX = 3;
    public static final int EFFECT_SUB_ITEM_TYPE_APPLY_MODE_MOTION_TITLE = 2;
    public static final int EFFECT_SUB_ITEM_TYPE_APPLY_MODE_STORYBOARD = 0;
    public static final int EFFECT_SUB_ITEM_TYPE_BASE = 0;
    public static final int EFFECT_SUB_ITEM_TYPE_CHROMA = 1;
    public static final int EFFECT_SUB_ITEM_TYPE_FILTER = 2;
    public static final int EFFECT_SUB_ITEM_TYPE_MASK = 4;
    public static final int EFFECT_SUB_ITEM_TYPE_MIXER = 15;
    public static final int EFFECT_SUB_ITEM_TYPE_MOTION_TITLE = 5;
    public static final int EFFECT_TIME_POSITION_ALIGNMENT_MODE_END = 1;
    public static final int EFFECT_TIME_POSITION_ALIGNMENT_MODE_MID = 2;
    public static final int EFFECT_TIME_POSITION_ALIGNMENT_MODE_RIGHT = 3;
    public static final int EFFECT_TIME_POSITION_ALIGNMENT_MODE_START = 0;
    public static final int PROP_AE_EFFECT_BASE = 45056;
    public static final int PROP_AUDIO_FRAME_ADJUST_DB = 4115;
    public static final int PROP_AUDIO_FRAME_FADEIN = 4116;
    public static final int PROP_AUDIO_FRAME_FADEOUT = 4117;
    public static final int PROP_AUDIO_FRAME_MIXPERCENT = 4114;
    public static final int PROP_AUDIO_FRAME_RANGE = 4112;
    public static final int PROP_AUDIO_FRAME_REPEAT_MODE = 4113;
    public static final int PROP_AUDIO_FRAME_SOURCE = 4111;
    private static final int PROP_BASE = 4096;
    public static final int PROP_EFEFCT_AUDIO_STRECH_ALGO = 53268;
    public static final int PROP_EFFECT_3D_TRANSFORM_MODE = 4325;
    public static final int PROP_EFFECT_ADDBYTHEME = 4128;
    public static final int PROP_EFFECT_ALGO_CACHE_UUID = 4397;
    public static final int PROP_EFFECT_ALGO_SIZE = 53264;
    public static final int PROP_EFFECT_ANIMATE_POINT_GENERATOR = 4131;
    public static final int PROP_EFFECT_APPLIED_OFFSET = 4306;
    public static final int PROP_EFFECT_AUDIO_ADDITIONAL_TIME = 4133;
    public static final int PROP_EFFECT_AUDIO_ENABLE_LOUDNESS = 4388;
    public static final int PROP_EFFECT_AUDIO_FRAME_LRCTMP_ID = 4157;
    public static final int PROP_EFFECT_AUDIO_FRAME_LRC_FILE = 4156;
    public static final int PROP_EFFECT_AUDIO_FRAME_LRC_LIST = 4171;
    public static final int PROP_EFFECT_AUDIO_FRAME_MUTE = 4148;
    public static final int PROP_EFFECT_AUDIO_FRAME_SOURCE_RANGE = 4187;
    public static final int PROP_EFFECT_AUDIO_FRAME_TITLE = 4188;
    public static final int PROP_EFFECT_AUDIO_GAIN = 4209;
    public static final int PROP_EFFECT_AUDIO_IS_NEED_NSX = 4360;
    public static final int PROP_EFFECT_AUDIO_LOUDNESS_VALUE = 4387;
    public static final int PROP_EFFECT_AUDIO_LYRIC_TEXT_INFO_LIST = 4175;
    public static final int PROP_EFFECT_AUDIO_PITCH_DELTA = 4314;
    public static final int PROP_EFFECT_AUDIO_SOURCE = 4311;
    public static final int PROP_EFFECT_AUTO_REFRESH_TIMELINE = 45070;
    public static final int PROP_EFFECT_AV_SOURCE = 4139;
    public static final int PROP_EFFECT_AV_SOURCE_RANGE = 4140;
    public static final int PROP_EFFECT_AV_SOURCE_REPEAT = 4141;
    public static final int PROP_EFFECT_BLEND_ALPHA = 4164;
    public static final int PROP_EFFECT_BLEND_MODE = 4351;
    public static final int PROP_EFFECT_CHANGE_TRACK_TYPE = 4310;
    public static final int PROP_EFFECT_CUR_TEXT_TOKENIZE_STYLE = 4408;
    public static final int PROP_EFFECT_DEL_KEYLINE_ITEM = 4221;
    public static final int PROP_EFFECT_DISABLE_MOTION_TILE = 4382;
    public static final int PROP_EFFECT_DRAW_LAYER_DATA = 4342;
    public static final int PROP_EFFECT_DRAW_LAYER_DATA_CLEAR = 4348;
    public static final int PROP_EFFECT_DRAW_LAYER_LIST_COUNT = 4343;
    public static final int PROP_EFFECT_DRAW_LAYER_RECORD_LIST_COUNT = 4344;
    public static final int PROP_EFFECT_DST_RATIO = 4168;
    public static final int PROP_EFFECT_EFFECT_SEG_MASK = 4330;
    public static final int PROP_EFFECT_ENABLE_DRAW_BACKGROUND = 4297;
    public static final int PROP_EFFECT_ENABLE_SINGLE_TRACK = 4366;
    public static final int PROP_EFFECT_EQ_BAND_FREQUENCY = 4192;
    public static final int PROP_EFFECT_EQ_BAND_VALUE = 4191;
    public static final int PROP_EFFECT_FACE_PASTER_MAX_FACE_ONLY = 4356;
    public static final int PROP_EFFECT_FACE_SET_BY_USER = 4357;
    public static final int PROP_EFFECT_FACE_SWAP_PARAM = 4396;
    public static final int PROP_EFFECT_FD_RESULT = 4169;
    public static final int PROP_EFFECT_FFRAME_AUDIO_RANGE = 7941;
    public static final int PROP_EFFECT_FFRAME_AUDIO_SOURCE = 7940;
    public static final int PROP_EFFECT_FFRAME_ENABLE = 7944;
    public static final int PROP_EFFECT_FFRAME_FRAME_SETTING = 7942;
    public static final int PROP_EFFECT_FFRAME_ORG_RANGE = 7943;
    public static final int PROP_EFFECT_FFRAME_RANGE = 7937;
    public static final int PROP_EFFECT_FFRAME_SOURCE = 7939;
    public static final int PROP_EFFECT_FFRAME_TYPE = 7938;
    public static final int PROP_EFFECT_FRAME_BGSIZE = 4132;
    public static final int PROP_EFFECT_FRAME_SCALE = 4334;
    public static final int PROP_EFFECT_FRAME_STATIC_PICTURE = 4172;
    public static final int PROP_EFFECT_GROUP_COMP_SIZE = 4352;
    public static final int PROP_EFFECT_GROUP_CUSTOM_SOURCE_3D_TRANSFORM = 4350;
    public static final int PROP_EFFECT_GROUP_DATA = 4381;
    public static final int PROP_EFFECT_GROUP_SOURCE_3D_TRANSFORM = 4349;
    public static final int PROP_EFFECT_IMAGE_RESTORE_SIZE = 4398;
    public static final int PROP_EFFECT_INSTANT_VIDEO_REGION = 4213;
    public static final int PROP_EFFECT_INSTANT_VIDEO_ROTATION = 4214;
    public static final int PROP_EFFECT_INSTANT_VIDEO_TRANSFORM_APPLY = 4212;
    public static final int PROP_EFFECT_INSTANT_VIDEO_TRANSFORM_SET = 4211;
    public static final int PROP_EFFECT_INVERSE_AUDIO_FRAME_RANGE = 4375;
    public static final int PROP_EFFECT_INVERSE_PLAY_AUDIO_FLAG = 4338;
    public static final int PROP_EFFECT_INVERSE_PLAY_SOURCE_RANGE = 4339;
    public static final int PROP_EFFECT_INVERSE_PLAY_VIDEO_FLAG = 4337;
    public static final int PROP_EFFECT_IS_DRAWING = 4347;
    public static final int PROP_EFFECT_IS_FRAME_MODE = 4158;
    public static final int PROP_EFFECT_IS_RENDER_EVEN_NOFACE = 4219;
    public static final int PROP_EFFECT_IS_TIME_SCALE_USE_AUDIO_PITCH = 4372;
    public static final int PROP_EFFECT_IS_VISABLE = 4362;
    public static final int PROP_EFFECT_KEYFRAME_2D_TO_3D_TRANSFORM = 4353;
    public static final int PROP_EFFECT_KEYFRAME_AUDIO = 4202;
    public static final int PROP_EFFECT_KEYFRAME_AUDIO_SET = 4203;
    public static final int PROP_EFFECT_KEYFRAME_CLEAR = 4197;
    public static final int PROP_EFFECT_KEYFRAME_COLORA = 4200;
    public static final int PROP_EFFECT_KEYFRAME_COLORA_SET = 4201;
    public static final int PROP_EFFECT_KEYFRAME_COLORCURVE = 4229;
    public static final int PROP_EFFECT_KEYFRAME_COLORCURVE_SET = 4230;
    public static final int PROP_EFFECT_KEYFRAME_LEVEL = 4204;
    public static final int PROP_EFFECT_KEYFRAME_LEVEL_SET = 4205;
    public static final int PROP_EFFECT_KEYFRAME_MASK = 4215;
    public static final int PROP_EFFECT_KEYFRAME_MASK_SET = 4216;
    public static final int PROP_EFFECT_KEYFRAME_OPACITY = 4206;
    public static final int PROP_EFFECT_KEYFRAME_OPACITY_SET = 4207;
    public static final int PROP_EFFECT_KEYFRAME_TRANSFORM = 4198;
    public static final int PROP_EFFECT_KEYFRAME_TRANSFORM_ORIGIN_REGION = 4208;
    public static final int PROP_EFFECT_KEYFRAME_TRANSFORM_POS = 4232;
    public static final int PROP_EFFECT_KEYFRAME_TRANSFORM_ROTATION = 4234;
    public static final int PROP_EFFECT_KEYFRAME_TRANSFORM_SCALE = 4236;
    public static final int PROP_EFFECT_KEYFRAME_TRANSFORM_SET = 4199;
    public static final int PROP_EFFECT_KEYFRAME_UNIFORM_LIST = 4227;
    public static final int PROP_EFFECT_KEYLINE_DISPLAY_RECT = 4224;
    public static final int PROP_EFFECT_KEYLINE_ITEM_SET = 4222;
    public static final int PROP_EFFECT_LAYER_DRAW_END = 4410;
    public static final int PROP_EFFECT_LAYER_POINT_REGION = 4409;
    public static final int PROP_EFFECT_MASK_REVERSE = 4399;
    public static final int PROP_EFFECT_ONSET_THRESHHOLD = 4328;
    public static final int PROP_EFFECT_OT_COORD_FILE_FINISHED = 4153;
    public static final int PROP_EFFECT_OT_RECT = 4151;
    public static final int PROP_EFFECT_PARAM_DATA = 4135;
    public static final int PROP_EFFECT_PATH_FX_STYLE_INFO = 4411;
    public static final int PROP_EFFECT_PERSONINST_ID = 4394;
    public static final int PROP_EFFECT_PIP_DISPLAY_CROP = 4370;
    public static final int PROP_EFFECT_PIP_STORYBOARD_INFO = 4317;
    public static final int PROP_EFFECT_PIP_TRANFORM_INFO = 4369;
    public static final int PROP_EFFECT_POSITION_ALIGNMENT = 4146;
    public static final int PROP_EFFECT_PROPDATA = 4129;
    public static final int PROP_EFFECT_PS_EMITTER_POS_END = 4160;
    public static final int PROP_EFFECT_PS_EMITTER_POS_POINT = 4159;
    public static final int PROP_EFFECT_RANGE_EXT = 4315;
    public static final int PROP_EFFECT_REGION_ALIGN_MODE = 4313;
    public static final int PROP_EFFECT_REPLACE_AUDIO_SOURCE = 4401;
    public static final int PROP_EFFECT_REPLACE_AUDIO_SOURCE_POS = 4402;
    public static final int PROP_EFFECT_RESET_SEG_MASK = 4333;
    public static final int PROP_EFFECT_RTA_ENABLED = 4127;
    public static final int PROP_EFFECT_SCALE_REMULGION_RATIO = 4176;
    public static final int PROP_EFFECT_SEGMENT_TYPE = 4395;
    public static final int PROP_EFFECT_SET_CLIP_DURATION = 4393;
    public static final int PROP_EFFECT_SINGLE_TRACK_CLIP_CUR_POS = 4374;
    public static final int PROP_EFFECT_SINGLE_TRACK_CLIP_TRANSFORM = 4373;
    public static final int PROP_EFFECT_SINGLE_TRACK_FILE_PATH = 4367;
    public static final int PROP_EFFECT_SINGLE_TRACK_MEDIA_TIME = 4368;
    public static final int PROP_EFFECT_SUB_EFFECT_DISABLE = 4332;
    public static final int PROP_EFFECT_SUB_EFFECT_RANGE = 4307;
    public static final int PROP_EFFECT_SUB_SOURCE_CHORMA_COLOR = 4303;
    public static final int PROP_EFFECT_SUB_SOURCE_CHORMA_PROP = 4300;
    public static final int PROP_EFFECT_SUB_TEMPLATE_ID = 4173;
    public static final int PROP_EFFECT_TA_SOURCE = 4136;
    public static final int PROP_EFFECT_TA_SOURCE_LIST = 4134;
    public static final int PROP_EFFECT_TEXT_ADV_RESET = 4389;
    public static final int PROP_EFFECT_TEXT_ADV_STYLE = 4318;
    public static final int PROP_EFFECT_TEXT_ATTACHMENT_DURATION = 4194;
    public static final int PROP_EFFECT_TEXT_ATTACHMENT_ID = 4193;
    public static final int PROP_EFFECT_TEXT_ATTACHMENT_SCALE = 4361;
    public static final int PROP_EFFECT_TEXT_BOARD_CONFIG = 4323;
    public static final int PROP_EFFECT_TEXT_TOKENIZE_STR_STYLE = 4412;
    public static final int PROP_EFFECT_TEXT_TOKENIZE_STYLE = 4405;
    public static final int PROP_EFFECT_TEXT_TOKENIZE_STYLE_CLEAR = 4406;
    public static final int PROP_EFFECT_THEME_POS_TYPE = 4130;
    public static final int PROP_EFFECT_TIME_FACTOR = 4329;
    public static final int PROP_EFFECT_TRANSPARENT_MASK = 4413;
    public static final int PROP_EFFECT_UHD_FRAME = 4383;
    public static final int PROP_EFFECT_UNIQUE_IDENTIFIER = 4302;
    public static final int PROP_EFFECT_UPDATE_GROUP_ID = 4309;
    public static final int PROP_EFFECT_UPDATE_KEYLINE_ITEM = 4220;
    public static final int PROP_EFFECT_USE_NEW_ADUIO_MIX_MODE = 4210;
    public static final int PROP_EFFECT_UUID = 4316;
    public static final int PROP_EFFECT_VFI_RANGE = 4376;
    public static final int PROP_EFFECT_VFI_TYPE = 4380;
    public static final int PROP_EFFECT_VIDEOFRAME_ANCHOR = 4238;
    public static final int PROP_EFFECT_VIDEOFRAME_SRCRANGE = 4189;
    public static final int PROP_EFFECT_VIDEO_FRAME_ANCHOR_APPLY = 4239;
    public static final int PROP_EFFECT_VIDEO_FRAME_CROP_REGION = 4320;
    public static final int PROP_EFFECT_VIDEO_FRAME_REPEAT_MODE = 4415;
    public static final int PROP_EFFECT_VISIBILITY = 4125;
    public static final int PROP_FFRAME_BASE = 7936;
    public static final int PROP_GROUP = 4099;
    public static final int PROP_IS_READ_ONLY = 4118;
    public static final int PROP_ITEM_BASE = 53248;
    public static final int PROP_LAYER = 4100;
    public static final int PROP_ORIGINAL_RANGE = 4119;
    public static final int PROP_RANGE = 4098;
    public static final int PROP_TYPE = 4097;
    public static final int PROP_USERDATA = 4101;
    public static final int PROP_VIDEO_FRAME_BG_RESOLUTION = 4110;
    public static final int PROP_VIDEO_FRAME_EFFECT = 4106;
    public static final int PROP_VIDEO_FRAME_FPS = 4109;
    public static final int PROP_VIDEO_FRAME_MASK = 4105;
    public static final int PROP_VIDEO_FRAME_MULTI_SOURCE = 4185;
    public static final int PROP_VIDEO_FRAME_RANGE = 4108;
    public static final int PROP_VIDEO_FRAME_ROTATION = 4121;
    public static final int PROP_VIDEO_FRAME_SOURCE = 4104;
    public static final int PROP_VIDEO_FRAME_STATIC = 4126;
    public static final int PROP_VIDEO_FRAME_TEMPLATE = 4124;
    public static final int PROP_VIDEO_FRAME_TRANSPARENCY = 4107;
    public static final int PROP_VIDEO_FRAME_X_FLIP = 4122;
    public static final int PROP_VIDEO_FRAME_Y_FLIP = 4123;
    public static final int PROP_VIDEO_IE_CONFIGURE = 4120;
    public static final int PROP_VIDEO_IE_SOURCE = 4103;
    public static final int PROP_VIDEO_REGION_RATIO = 4102;
    public static final int TEXT_ATTACH_COMPLEX_STYLE = 0;
    public static final int TEXT_ATTACH_HEAD_ANIMATE = 2;
    public static final int TEXT_ATTACH_LOOP_ANIMATE = 1;
    public static final int TEXT_ATTACH_TAIL_ANIMATE = 3;
    public static final int TRACK_TYPE_AUDIO = 3;
    private static final int TRACK_TYPE_BASE = 0;
    public static final int TRACK_TYPE_FREEZE_FRAME = 4;
    public static final int TRACK_TYPE_PRIMAL_VIDEO = 1;
    public static final int TRACK_TYPE_VIDEO = 2;
    public static final int TRAJECTORY_IDX_HEAD = 0;
    public static final int TRAJECTORY_IDX_TAIL = -1;
    public static final int TYPE_AUDIO_FRAME = 3;
    private static final int TYPE_BASE = 0;
    public static final int TYPE_COMBO_VIDEO_IE = 6;
    public static final int TYPE_FREEZE_FRAME = 4;
    public static final int TYPE_GROUP_FRAME = 7;
    public static final int TYPE_LOOP_VIDEO_FRAME = 9;
    public static final int TYPE_VIDEO_FRAME = 2;
    public static final int TYPE_VIDEO_FRAME_GROUP = 8;
    public static final int TYPE_VIDEO_IE = 1;
    private boolean bSubEffect;
    private int frameworkVersion;
    private long handle;
    private long masktmpbufferhandle;
    private long spaehandle;
    private long sphandle;
    private long spweakaehandle;
    private long spweakhandle;
    private long tmpbufferhandle;

    public static class QEffectAnimatePointData {
        public int duration;
        public int opacity;
        public QRect rcCrop;
        public QRect rcDisplay;
        public float rotation;
    }

    public static class QEffectAnimatePointOperator {
        private long handle = 0;

        private native int nativeApplyAnimatePointOpt(long handle, QEffectAnimatePointOptData optData);

        private native QEffectAnimatePointData[] nativeGetAnimatePointData(long handle);

        public int ApplyAnimatePointOpt(QEffectAnimatePointOptData optData) {
            return nativeApplyAnimatePointOpt(this.handle, optData);
        }

        public QEffectAnimatePointData[] GetAnimatePointData() {
            return nativeGetAnimatePointData(this.handle);
        }
    }

    public static class QEffectAnimatePointOptData {
        public QEffectAnimatePointData animatepoint;
        public int opttype;
        public int pointindex;
    }

    public enum QEffectBlendMode {
        kQVAEBlendModeNone,
        kQVAEBlendModeAdd,
        kQVAEBlendModeHue,
        kQVAEBlendModeColor,
        kQVAEBlendModeDivide,
        kQVAEBlendModeScreen,
        kQVAEBlendModeNormal,
        kQVAEBlendModeDarken,
        kQVAEBlendModeLighten,
        kQVAEBlendModeOverlay,
        kQVAEBlendModeHardMix,
        kQVAEBlendModePinLight,
        kQVAEBlendModeAlphaAdd,
        kQVAEBlendModeSubstrct,
        kQVAEBlendModeMultiply,
        kQVAEBlendModeDissolve,
        kQVAEBlendModeExclusion,
        kQVAEBlendModeHardLight,
        kQVAEBlendModeSoftLight,
        kQVAEBlendModeColorBurn,
        kQVAEBlendModeColorDodge,
        kQVAEBlendModeVividLight,
        kQVAEBlendModeDifference,
        kQVAEBlendModeSaturation,
        kQVAEBlendModeLuminosity,
        kQVAEBlendModeLinearBurn,
        kQVAEBlendModeLinearDodge,
        kQVAEBlendModeLinearLight,
        kQVAEBlendModeStencilLuma,
        kQVAEBlendModeDarkerColor,
        kQVAEBlendModeLighterColor,
        kQVAEBlendModeStencilAlpha,
        kQVAEBlendModeSilhouetteLuma,
        kQVAEBlendModeSilhouetteAlpha,
        kQVAEBlendModeDancingDissolve,
        kQVAEBlendModeClassicColorBurn,
        kQVAEBlendModeClassicColorDodge,
        kQVAEBlendModeClassicDifference,
        kQVAEBlendModeLuminescentPremul
    }

    public static class QEffectExternalSource {
        public QRect mCropRect;
        public QRange mDataRange;
        public int mRotation;
        public QMediaSource mSource;
    }

    public static class QEffectGroupData {
        public int mGroupID = 0;
        public int mValue = 0;
    }

    public static class QEffectStoryboardInfo {
        public long m_hStoryboard = 0;
        public String m_sProjectPath = null;
    }

    public static class QEffectSubChormaProp {
        public int m_X;
        public int m_Y;
        public boolean m_bEnablePoint;
        public boolean m_bRefreshFrame;
    }

    public static class QEffectSubTemplateID {
        public int index = 0;
        public long templateid = 0;
    }

    public static class QEffectTextAttachDuration {
        public int index;
        public int type = 0;
        public int duration = 0;
    }

    public static class QEffectTextAttachFileInfo {
        public int type = 0;
        public int designTime = 0;
        public long templateid = 0;
        public float scale = 1.0f;
    }

    public static class QEffectTextAttachID {
        public int index;
        public int type = 0;
        public long templateid = 0;
    }

    public static class QEffectTextAttachScale {
        public int index;
        public int type = 0;
        public float scale = 1.0f;
    }

    public static class QFacePasterTransform {
        public QTransformInfo transform = null;
        public QSize bgSize = null;
        public boolean setByUser = false;
    }

    public static class QFaceSwapInfo {
        public int faceId = 0;
        public String file = null;
        public QPointFloat[] facePoint = null;
    }

    public static class QFaceSwapParam {
        public int faceCount = 0;
        public QFaceSwapInfo[] faceSwapInfo = null;
    }

    public QEffect() {
        this.tmpbufferhandle = 0L;
        this.masktmpbufferhandle = 0L;
        this.handle = 0L;
        this.sphandle = 0L;
        this.spweakhandle = 0L;
        this.spaehandle = 0L;
        this.spweakaehandle = 0L;
        this.frameworkVersion = 0;
        this.bSubEffect = false;
    }

    public static QKeyFrameCommonData.Value getCurrentValueForKeyframeCommon(QKeyFrameCommonData data, int nTimeStamp) {
        return nativeGetCurrentValueForKeyframeCommon(data, nTimeStamp);
    }

    public static QKeyFrameTransformData.Value getCurrentValueForKeyframeTransform(QKeyFrameTransformData data, int ts) {
        return nativeGetCurrentValueForKeyframeTransform(data, ts);
    }

    public static QKeyFrameTransformPosData.Value getCurrentValueForKeyframeTransformPos(QKeyFrameTransformPosData data, int ts) {
        return nativeGetCurrentValueForKeyframeTransformPos(data, ts);
    }

    public static int getSubItemType(long lTemplateID) {
        return (int) ((lTemplateID & 16711680) >> 16);
    }

    public static int getTextAttachFileInfo(String fliePath, QEffectTextAttachFileInfo fileInfo) {
        return nativeGetTextAttachFileInfo(fliePath, fileInfo);
    }

    private native int nativeAppendLayerLinePoint(long handle, QPointFloat point);

    private native int nativeAppendLayerLinePointAEWrapper(long handle, QPointFloat point);

    private native int nativeCopyPartFormEffect(long handle, long fromHandle, int type);

    private native int nativeCopyPartFormEffectAEWrapper(long handle, long fromHandle, int type);

    private native int nativeCreate(QEngine engine, QEffect effect, int trackType, int groupId, int type, float layerID);

    private native int nativeCreateAEWrapper(QEngine engine, QEffect effect, int trackType, int groupId, int type, float layerID, boolean isPIP);

    private native QEffectAnimatePointOperator nativeCreateAnimatePointOperator(long handle, QSize viewsize);

    private native QEffectAnimatePointOperator nativeCreateAnimatePointOperatorAEWrapper(long handle, QSize viewsize);

    private native void nativeDestorySubItemEffect(int effectSubType, float fLayerID);

    private native void nativeDestorySubItemEffectAEWrapper(int effectSubType, float fLayerID);

    private native void nativeDestorySubItemList();

    private native void nativeDestorySubItemListAEWrapper();

    private native int nativeDestroy(QEffect effect);

    private native int nativeDestroyAEWrapper(QEffect effect);

    private native int nativeDuplicate(long handle, QEffect effect);

    private native int nativeDuplicateAEWrapper(long handle, QEffect effect);

    private native int nativeEffectGroupDeleteEffect(long handle, long effect);

    private native int nativeEffectGroupDeleteEffectAEWrapper(long handle, long effect);

    private native QEffect nativeEffectGroupGetEffectByIndex(long handle, int index);

    private native QEffect nativeEffectGroupGetEffectByIndexAEWrapper(long handle, int index);

    private native int nativeEffectGroupGetEffectCount(long handle);

    private native int nativeEffectGroupGetEffectCountAEWrapper(long handle);

    private native int nativeEffectGroupInsertEffect(long handle, QEffect effect);

    private native int nativeEffectGroupInsertEffectAEWrapper(long handle, QEffect effect);

    private native int nativeEffectGroupRefreshGroup(long handle);

    private native int nativeEffectGroupRefreshGroupAEWrapper(long handle);

    private native int nativeEndLayerPaintLine(long handle);

    private native int nativeEndLayerPaintLineAEWrapper(long handle);

    private native QTransformInfo nativeGet3DTransformInfo(long handle);

    private native QTransformInfo nativeGet3DTransformInfoAEWrapper(long handle);

    private native QTransformInfo nativeGet3DTransformInfoInGroup(long handle, long effect);

    private native QTransformInfo nativeGet3DTransformInfoInGroupAEWrapper(long handle, long effect);

    private native int nativeGetAttachDuration(long handle, int type, int index);

    private native int nativeGetAttachDurationAEWrapper(long handle, int type, int index);

    private native long nativeGetAttachID(long handle, int type, int index);

    private native long nativeGetAttachIDAEWrapper(long handle, int type, int index);

    private native float nativeGetAttachScale(long handle, int type, int index);

    private native float nativeGetAttachScaleAEWrapper(long handle, int type, int index);

    private static native QKeyFrameCommonData.Value nativeGetCurrentValueForKeyframeCommon(QKeyFrameCommonData data, int nTimeStamp);

    private static native QKeyFrameTransformData.Value nativeGetCurrentValueForKeyframeTransform(QKeyFrameTransformData data, int ts);

    private static native QKeyFrameTransformPosData.Value nativeGetCurrentValueForKeyframeTransformPos(QKeyFrameTransformPosData data, int ts);

    private native QEffectDisplayInfo nativeGetDisplayInfo(long handle, int timeStamp);

    private native QEffectDisplayInfo nativeGetDisplayInfoAEWrapper(long handle, int timeStamp);

    private native QEffect nativeGetEffectGroup(long handle);

    private native QEffect nativeGetEffectGroupAEWrapper(long handle);

    private native QEffectGroupData nativeGetEffectGroupData(long handle, int groupID);

    private native QEffectGroupData nativeGetEffectGroupDataAEWrapper(long handle, int groupID);

    private native QStyle.QEffectPropertyData nativeGetEffectPropData(long handle, int propID);

    private native QStyle.QEffectPropertyData nativeGetEffectPropDataAEWrapper(long handle, int propID);

    private native int nativeGetExternalSource(int index, QEffectExternalSource externalSource);

    private native int nativeGetExternalSourceAEWrapper(int index, QEffectExternalSource externalSource);

    private native boolean nativeGetFaceHidden(long handle, int faceidx);

    private native boolean nativeGetFaceHiddenAEWrapper(long handle, int faceidx);

    private native QFacePasterTransform nativeGetFacePasterTransform(long handle, int faceidx, int layerid);

    private native QFacePasterTransform nativeGetFacePasterTransformAEWrapper(long handle, int faceidx, int layerid);

    private native QTransformInfo nativeGetKeyFrame3DTransformInfo(long handle, int nTimeStamp);

    private native QTransformInfo nativeGetKeyFrame3DTransformInfoAEWrapper(long handle, int nTimeStamp);

    private native QKeyFrameCommonData nativeGetKeyFrameCommonData(long handle, int nKey);

    private native QKeyFrameCommonData nativeGetKeyFrameCommonDataAEWrapper(long handle, int nKey);

    private native QKeyFrameCommonData[] nativeGetKeyFrameCommonDataList(long handle);

    private native QKeyFrameCommonData[] nativeGetKeyFrameCommonDataListAEWrapper(long handle);

    private native QKeyFrameColorCurveData.OutValue nativeGetKeyframeColorCurveValue(long handle, int pts);

    private native QKeyFrameColorCurveData.OutValue nativeGetKeyframeColorCurveValueAEWrapper(long handle, int pts);

    private native QKeyFrameFloatData.Value nativeGetKeyframeLevelValue(long handle, int pts);

    private native QKeyFrameFloatData.Value nativeGetKeyframeLevelValueAEWrapper(long handle, int pts);

    private native QKeyFrameMaskData.Value nativeGetKeyframeMaskValue(long handle, int pts);

    private native QKeyFrameMaskData.Value nativeGetKeyframeMaskValueAEWrapper(long handle, int pts);

    private native QKeyFrameTransformPosData.Value nativeGetKeyframeTransformPosValue(long handle, int pts);

    private native QKeyFrameTransformPosData.Value nativeGetKeyframeTransformPosValueAEWrapper(long handle, int pts);

    private native QKeyFrameTransformRotationData.Value nativeGetKeyframeTransformRotationValue(long handle, int pts);

    private native QKeyFrameTransformRotationData.Value nativeGetKeyframeTransformRotationValueAEWrapper(long handle, int pts);

    private native QKeyFrameTransformScaleData.Value nativeGetKeyframeTransformScaleValue(long handle, int pts);

    private native QKeyFrameTransformScaleData.Value nativeGetKeyframeTransformScaleValueAEWrapper(long handle, int pts);

    private native QKeyFrameTransformData.Value nativeGetKeyframeTransformValue(long handle, int pts);

    private native QKeyFrameTransformData.Value nativeGetKeyframeTransformValueAEWrapper(long handle, int pts);

    private native QKeyFrameUniformData.Value nativeGetKeyframeUniform(long handle, String name, int ts);

    private native QKeyFrameUniformData.Value nativeGetKeyframeUniformAEWrapper(long handle, String name, int ts);

    private native QKeyFrameUniformData nativeGetKeyframeUniformData(long handle, String name);

    private native QKeyFrameUniformData nativeGetKeyframeUniformDataAEWrapper(long handle, String name);

    private native Object nativeGetProp(long handle, int propertyId);

    private native Object nativeGetPropAEWrapper(long handle, int propertyId);

    private native QEffectSubItemSource nativeGetSubItemSource(int effectSubType, float fLayerID);

    private native QEffectSubItemSource nativeGetSubItemSourceAEWrapper(int effectSubType, float fLayerID);

    private native QEffectSubItemSource[] nativeGetSubItemSourceList();

    private native QEffectSubItemSource[] nativeGetSubItemSourceListAEWrapper();

    private native QEffectTextAdvStyle nativeGetTextAdvanceStyle(long handle, int index);

    private native QEffectTextAdvStyle nativeGetTextAdvanceStyleAEWrapper(long handle, int index);

    private static native int nativeGetTextAttachFileInfo(String filePath, QEffectTextAttachFileInfo fileInfo);

    private static native int nativeGetTextAttachFileInfoAEWrapper(String filePath, QEffectTextAttachFileInfo fileInfo);

    private native int nativeGetTextAttachFileInfoById(long handle, long templateId, QEffectTextAttachFileInfo fileInfo);

    private native int nativeGetTextAttachFileInfoByIdAEWrapper(long handle, long templateId, QEffectTextAttachFileInfo fileInfo);

    private native QEffectTextAdvStyle.TextBoardConfig nativeGetTextBoardConfig(long handle, int index);

    private native QEffectTextAdvStyle.TextBoardConfig nativeGetTextBoardConfigAEWrapper(long handle, int index);

    private native QRect nativeGetTrackPointRegion(long handle, int timeStamp);

    private native QRect nativeGetTrackPointRegionAEWrapper(long handle, int timeStamp);

    private native QTrajectoryData nativeGetTrajectory(long handle, int trIdx);

    private native QTrajectoryData nativeGetTrajectoryAEWrapper(long handle, int trIdx);

    private native int nativeGetTrajectoryCount(long handle);

    private native int nativeGetTrajectoryCountAEWrapper(long handle);

    private native int nativeInsertNewTrajectory(long handle, int trIdx, QTrajectoryData trData);

    private native int nativeInsertNewTrajectoryAEWrapper(long handle, int trIdx, QTrajectoryData trData);

    private native int nativeInsertOrReplaceKeyFrameCommonValue(long handle, int nKey, QKeyFrameCommonData.Value value);

    private native int nativeInsertOrReplaceKeyFrameCommonValueAEWrapper(long handle, int nKey, QKeyFrameCommonData.Value value);

    private native int nativeLayerPaintRedo(long handle);

    private native int nativeLayerPaintRedoAEWrapper(long handle);

    private native int nativeLayerPaintUndo(long handle);

    private native int nativeLayerPaintUndoAEWrapper(long handle);

    private native int nativeMoveSubItemSourceFromIndex(QEffect subEffect, int nIndex);

    private native int nativeMoveSubItemSourceFromIndexAEWrapper(QEffect subEffect, int nIndex);

    private native void nativeRemoveAllTrajectory(long handle);

    private native void nativeRemoveAllTrajectoryAEWrapper(long handle);

    private native int nativeRemoveKeyFrameCommonValue(long handle, int nKey, float fTimeStamp);

    private native int nativeRemoveKeyFrameCommonValueAEWrapper(long handle, int nKey, float fTimeStamp);

    private native int nativeRemoveTrajectory(long handle, int trIdx);

    private native int nativeRemoveTrajectoryAEWrapper(long handle, int trIdx);

    private native int nativeReplaceEffect(long handle, QEffect[] effectList);

    private native int nativeReplaceEffectAEWrapper(long handle, QEffect[] effectList);

    private native int nativeSet3DTransformInfo(long handle, QTransformInfo info);

    private native int nativeSet3DTransformInfoAEWrapper(long handle, QTransformInfo info);

    private native int nativeSetExternalSource(int index, QEffectExternalSource externalSource);

    private native int nativeSetExternalSourceAEWrapper(int index, QEffectExternalSource externalSource);

    private native int nativeSetFaceHidden(long handle, int faceidx, boolean hidden);

    private native int nativeSetFaceHiddenAEWrapper(long handle, int faceidx, boolean hidden);

    private native void nativeSetFacePasterTransform(long handle, QFacePasterTransform transform, int faceidx, int layerid);

    private native void nativeSetFacePasterTransformAEWrapper(long handle, QFacePasterTransform transform, int faceidx, int layerid);

    private native int nativeSetKeyFrameCommonData(long handle, QKeyFrameCommonData data);

    private native int nativeSetKeyFrameCommonDataAEWrapper(long handle, QKeyFrameCommonData data);

    private native int nativeSetKeyframeUniformData(long handle, QKeyFrameUniformData data);

    private native int nativeSetKeyframeUniformDataAEWrapper(long handle, QKeyFrameUniformData data);

    private native int nativeSetProp(QEffect effect, int propertyId, Object data);

    private native int nativeSetPropAEWrapper(QEffect effect, int propertyId, Object data);

    private native int nativeSetSubItemSource(QEffectSubItemSource source);

    private native int nativeSetSubItemSourceAEWrapper(QEffectSubItemSource source);

    private native int nativeSetSubItemSourceFromIndex(QEffectSubItemSource source, int nIndex);

    private native int nativeSetSubItemSourceFromIndexAEWrapper(QEffectSubItemSource source, int nIndex);

    private native int nativeSetSubItemSourceList(QEffectSubItemSource[] source);

    private native int nativeSetSubItemSourceListAEWrapper(QEffectSubItemSource[] source);

    private native int nativeStartLayerPaintLine(long handle, QDrawLayerPaintPen pen);

    private native int nativeStartLayerPaintLineAEWrapper(long handle, QDrawLayerPaintPen pen);

    private native int nativeStartLayerPaintTextAEWrapper(long handle, QDrawLayerPaintText pen);

    private native QClip nativeSwitchToClip(long handle);

    private native int nativeUpdateKeyFrameCommonBaseValue(long handle, int nKey, float fOffsetValue);

    private native int nativeUpdateKeyFrameCommonBaseValueAEWrapper(long handle, int nKey, float fOffsetValue);

    private native int nativeUpdateTrajectory(long handle, int trIdx, QTrajectoryData trData);

    private native int nativeUpdateTrajectoryAEWrapper(long handle, int trIdx, QTrajectoryData trData);

    public int LayerPaintClear() {
        return this.frameworkVersion == 393216 ? nativeSetPropAEWrapper(this, PROP_EFFECT_DRAW_LAYER_DATA_CLEAR, Boolean.TRUE) : nativeSetProp(this, PROP_EFFECT_DRAW_LAYER_DATA_CLEAR, Boolean.TRUE);
    }

    public int appendLayerLinePoint(QPointFloat point) {
        return this.frameworkVersion == 393216 ? nativeAppendLayerLinePointAEWrapper(this.spweakaehandle, point) : nativeAppendLayerLinePoint(this.handle, point);
    }

    public int copyPartFormEffect(QEffect fromEffect, int type) {
        return this.frameworkVersion == 393216 ? nativeCopyPartFormEffectAEWrapper(this.spweakaehandle, fromEffect.spweakaehandle, type) : nativeCopyPartFormEffect(this.handle, fromEffect.handle, type);
    }

    public int create(QEngine engine, int type, int trackType, int groupId, float layerID) {
        if (engine == null) {
            return QVEError.QERR_COMMON_JAVA_INVALID_PARAM;
        }
        if (this.frameworkVersion == 0) {
            this.frameworkVersion = ((Integer) engine.getProperty(112)).intValue();
        }
        return this.frameworkVersion == 393216 ? nativeCreateAEWrapper(engine, this, trackType, groupId, type, layerID, false) : nativeCreate(engine, this, trackType, groupId, type, layerID);
    }

    public QEffectAnimatePointOperator createAnimatePointOpertor(QSize viewsize) {
        return this.frameworkVersion == 393216 ? nativeCreateAnimatePointOperatorAEWrapper(this.spweakaehandle, viewsize) : nativeCreateAnimatePointOperator(this.handle, viewsize);
    }

    public int deleteEffect(QEffect effect) {
        return this.frameworkVersion == 393216 ? nativeEffectGroupDeleteEffectAEWrapper(this.spweakaehandle, effect.spweakaehandle) : nativeEffectGroupDeleteEffect(this.handle, effect.handle);
    }

    public int destory() {
        if (0 == this.handle && 0 == this.spweakaehandle) {
            return QVEError.QERR_COMMON_JAVA_NOT_INIT;
        }
        return this.frameworkVersion == 393216 ? nativeDestroyAEWrapper(this) : nativeDestroy(this);
    }

    public void destorySubItemEffect(int effectSubType, float fLayerID) {
        if (this.frameworkVersion == 393216) {
            nativeDestorySubItemEffectAEWrapper(effectSubType, fLayerID);
        } else {
            nativeDestorySubItemEffect(effectSubType, fLayerID);
        }
    }

    public void destorySubItemList() {
        if (this.frameworkVersion == 393216) {
            nativeDestorySubItemListAEWrapper();
        } else {
            nativeDestorySubItemList();
        }
    }

    public QEffect duplicate() {
        if (0 == this.handle && 0 == this.spweakaehandle) {
            return null;
        }
        QEffect qEffect = new QEffect();
        if ((this.frameworkVersion == 393216 ? nativeDuplicateAEWrapper(this.spweakaehandle, qEffect) : nativeDuplicate(this.handle, qEffect)) != 0) {
            return null;
        }
        return qEffect;
    }

    public int endLayerPaintLine() {
        return this.frameworkVersion == 393216 ? nativeEndLayerPaintLineAEWrapper(this.spweakaehandle) : nativeEndLayerPaintLine(this.handle);
    }

    public void finalize() throws Throwable {
        try {
            destory();
        } catch (Throwable unused) {
        }
        super.finalize();
    }

    public QTransformInfo get3DTransformInfo() {
        return this.frameworkVersion == 393216 ? nativeGet3DTransformInfoAEWrapper(this.spweakaehandle) : nativeGet3DTransformInfo(this.handle);
    }

    public QTransformInfo get3DTransformInfoInGroup(QEffect effect) {
        return this.frameworkVersion == 393216 ? nativeGet3DTransformInfoInGroupAEWrapper(this.spweakaehandle, effect.spweakaehandle) : nativeGet3DTransformInfoInGroup(this.handle, effect.handle);
    }

    public QEffectBlendMode getBlendMode() {
        int iIntValue = this.frameworkVersion == 393216 ? ((Integer) nativeGetPropAEWrapper(this.spweakaehandle, PROP_EFFECT_BLEND_MODE)).intValue() : ((Integer) nativeGetProp(this.handle, PROP_EFFECT_BLEND_MODE)).intValue();
        return iIntValue < QEffectBlendMode.values().length ? QEffectBlendMode.values()[iIntValue] : QEffectBlendMode.kQVAEBlendModeNone;
    }

    public QEffectDisplayInfo getDisplayInfo(int timeStamp) {
        long j10 = this.handle;
        if (0 == j10 && 0 == this.spweakaehandle) {
            return null;
        }
        return this.frameworkVersion == 393216 ? nativeGetDisplayInfoAEWrapper(this.spweakaehandle, timeStamp) : nativeGetDisplayInfo(j10, timeStamp);
    }

    public QEffect getEffectByIndex(int index) {
        return this.frameworkVersion == 393216 ? nativeEffectGroupGetEffectByIndexAEWrapper(this.spweakaehandle, index) : nativeEffectGroupGetEffectByIndex(this.handle, index);
    }

    public int getEffectCount() {
        return this.frameworkVersion == 393216 ? nativeEffectGroupGetEffectCountAEWrapper(this.spweakaehandle) : nativeEffectGroupGetEffectCount(this.handle);
    }

    public QEffect getEffectGroup() {
        return this.frameworkVersion == 393216 ? nativeGetEffectGroupAEWrapper(this.spweakaehandle) : nativeGetEffectGroup(this.handle);
    }

    public QEffectGroupData getEffectGroupData(int groupID) {
        return this.frameworkVersion == 393216 ? nativeGetEffectGroupDataAEWrapper(this.spweakaehandle, groupID) : nativeGetEffectGroupData(this.handle, groupID);
    }

    public QStyle.QEffectPropertyData getEffectPropData(int propID) {
        return this.frameworkVersion == 393216 ? nativeGetEffectPropDataAEWrapper(this.spweakaehandle, propID) : nativeGetEffectPropData(this.handle, propID);
    }

    public int getExternalSource(int index, QEffectExternalSource externalSource) {
        return this.frameworkVersion == 393216 ? nativeGetExternalSourceAEWrapper(index, externalSource) : nativeGetExternalSource(index, externalSource);
    }

    public boolean getFaceHidden(int faceidx) {
        return this.frameworkVersion == 393216 ? nativeGetFaceHiddenAEWrapper(this.spweakaehandle, faceidx) : nativeGetFaceHidden(this.handle, faceidx);
    }

    public QFacePasterTransform getFacePasterTransform(int faceidx, int layerid) {
        return this.frameworkVersion == 393216 ? nativeGetFacePasterTransformAEWrapper(this.spweakaehandle, faceidx, layerid) : nativeGetFacePasterTransform(this.handle, faceidx, layerid);
    }

    public QTransformInfo getKeyFrame3DTransformInfo(int nTimeStamp) {
        return this.frameworkVersion == 393216 ? nativeGetKeyFrame3DTransformInfoAEWrapper(this.spweakaehandle, nTimeStamp) : nativeGetKeyFrame3DTransformInfo(this.handle, nTimeStamp);
    }

    public QKeyFrameCommonData getKeyFrameCommonData(int nKey) {
        return this.frameworkVersion == 393216 ? nativeGetKeyFrameCommonDataAEWrapper(this.spweakaehandle, nKey) : nativeGetKeyFrameCommonData(this.handle, nKey);
    }

    public QKeyFrameCommonData[] getKeyFrameCommonDataList() {
        return this.frameworkVersion == 393216 ? nativeGetKeyFrameCommonDataListAEWrapper(this.spweakaehandle) : nativeGetKeyFrameCommonDataList(this.handle);
    }

    public QKeyFrameColorCurveData.OutValue getKeyframeColorCurveValue(int pts) {
        long j10 = this.handle;
        if (0 == j10 && 0 == this.spweakaehandle) {
            return null;
        }
        return this.frameworkVersion == 393216 ? nativeGetKeyframeColorCurveValueAEWrapper(this.spweakaehandle, pts) : nativeGetKeyframeColorCurveValue(j10, pts);
    }

    public QKeyFrameFloatData.Value getKeyframeLevelValue(int pts) {
        long j10 = this.handle;
        if (0 == j10 && 0 == this.spweakaehandle) {
            return null;
        }
        return this.frameworkVersion == 393216 ? nativeGetKeyframeLevelValueAEWrapper(this.spweakaehandle, pts) : nativeGetKeyframeLevelValue(j10, pts);
    }

    public QKeyFrameMaskData.Value getKeyframeMaskValue(int pts) {
        long j10 = this.handle;
        if (0 == j10 && 0 == this.spweakaehandle) {
            return null;
        }
        return this.frameworkVersion == 393216 ? nativeGetKeyframeMaskValueAEWrapper(this.spweakaehandle, pts) : nativeGetKeyframeMaskValue(j10, pts);
    }

    public QKeyFrameTransformPosData.Value getKeyframeTransformPosValue(int pts) {
        long j10 = this.handle;
        if (0 == j10 && 0 == this.spweakaehandle) {
            return null;
        }
        return this.frameworkVersion == 393216 ? nativeGetKeyframeTransformPosValueAEWrapper(this.spweakaehandle, pts) : nativeGetKeyframeTransformPosValue(j10, pts);
    }

    public QKeyFrameTransformRotationData.Value getKeyframeTransformRotationValue(int pts) {
        long j10 = this.handle;
        if (0 == j10 && 0 == this.spweakaehandle) {
            return null;
        }
        return this.frameworkVersion == 393216 ? nativeGetKeyframeTransformRotationValueAEWrapper(this.spweakaehandle, pts) : nativeGetKeyframeTransformRotationValue(j10, pts);
    }

    public QKeyFrameTransformScaleData.Value getKeyframeTransformScaleValue(int pts) {
        long j10 = this.handle;
        if (0 == j10 && 0 == this.spweakaehandle) {
            return null;
        }
        return this.frameworkVersion == 393216 ? nativeGetKeyframeTransformScaleValueAEWrapper(this.spweakaehandle, pts) : nativeGetKeyframeTransformScaleValue(j10, pts);
    }

    public QKeyFrameTransformData.Value getKeyframeTransformValue(int pts) {
        long j10 = this.handle;
        if (0 == j10 && 0 == this.spweakaehandle) {
            return null;
        }
        return this.frameworkVersion == 393216 ? nativeGetKeyframeTransformValueAEWrapper(this.spweakaehandle, pts) : nativeGetKeyframeTransformValue(j10, pts);
    }

    public QKeyFrameUniformData.Value getKeyframeUnifrom(String name, int ts) {
        return this.frameworkVersion == 393216 ? nativeGetKeyframeUniformAEWrapper(this.spweakaehandle, name, ts) : nativeGetKeyframeUniform(this.handle, name, ts);
    }

    public QKeyFrameUniformData getKeyframeUnifromData(String name) {
        return this.frameworkVersion == 393216 ? nativeGetKeyframeUniformDataAEWrapper(this.spweakaehandle, name) : nativeGetKeyframeUniformData(this.handle, name);
    }

    public Object getProperty(int propertyID) {
        long j10 = this.handle;
        if (0 == j10 && 0 == this.spweakaehandle) {
            return null;
        }
        return this.frameworkVersion == 393216 ? nativeGetPropAEWrapper(this.spweakaehandle, propertyID) : nativeGetProp(j10, propertyID);
    }

    public QEffect getSubItemEffect(int effectSubType, float fLayerID) {
        QEffectSubItemSource subItemSource = getSubItemSource(effectSubType, fLayerID);
        if (subItemSource == null) {
            return null;
        }
        QEffect qEffect = new QEffect();
        qEffect.frameworkVersion = this.frameworkVersion;
        qEffect.setEffectHandle(subItemSource.m_lEffectHandle);
        qEffect.setEffectSpWeakHandle(subItemSource.m_lspEffectHandle);
        qEffect.setSubEffectFlag(true);
        return qEffect;
    }

    public QEffectSubItemSource getSubItemSource(int effectSubType, float fLayerID) {
        return this.frameworkVersion == 393216 ? nativeGetSubItemSourceAEWrapper(effectSubType, fLayerID) : nativeGetSubItemSource(effectSubType, fLayerID);
    }

    public QEffectSubItemSource[] getSubItemSourceList() {
        return this.frameworkVersion == 393216 ? nativeGetSubItemSourceListAEWrapper() : nativeGetSubItemSourceList();
    }

    public QEffectTextAdvStyle getTextAdvanceStyle(int index) {
        long j10 = this.handle;
        if (0 == j10 && 0 == this.spweakaehandle) {
            return null;
        }
        return this.frameworkVersion == 393216 ? nativeGetTextAdvanceStyleAEWrapper(this.spweakaehandle, index) : nativeGetTextAdvanceStyle(j10, index);
    }

    public int getTextAttachDuration(int type) {
        long j10 = this.handle;
        if (0 == j10 && 0 == this.spweakaehandle) {
            return QVEError.QERR_COMMON_JAVA_NOT_INIT;
        }
        return this.frameworkVersion == 393216 ? nativeGetAttachDurationAEWrapper(this.spweakaehandle, type, 0) : nativeGetAttachDuration(j10, type, 0);
    }

    public int getTextAttachFileInfoById(long templateID, QEffectTextAttachFileInfo fileInfo) {
        return this.frameworkVersion == 393216 ? nativeGetTextAttachFileInfoByIdAEWrapper(this.spweakaehandle, templateID, fileInfo) : nativeGetTextAttachFileInfoById(this.handle, templateID, fileInfo);
    }

    public long getTextAttachID(int type) {
        long j10 = this.handle;
        if (0 == j10 && 0 == this.spweakaehandle) {
            return 9429001L;
        }
        return this.frameworkVersion == 393216 ? nativeGetAttachIDAEWrapper(this.spweakaehandle, type, 0) : nativeGetAttachID(j10, type, 0);
    }

    public float getTextAttchScale(int type) {
        long j10 = this.handle;
        if (0 == j10 && 0 == this.spweakaehandle) {
            return 9429001.0f;
        }
        return this.frameworkVersion == 393216 ? nativeGetAttachScaleAEWrapper(this.spweakaehandle, type, 0) : nativeGetAttachScale(j10, type, 0);
    }

    public QEffectTextAdvStyle.TextBoardConfig getTextBoardConfig(int index) {
        long j10 = this.handle;
        if (0 == j10 && 0 == this.spweakaehandle) {
            return null;
        }
        return this.frameworkVersion == 393216 ? nativeGetTextBoardConfigAEWrapper(this.spweakaehandle, index) : nativeGetTextBoardConfig(j10, index);
    }

    public QRect getTrackPointRegion(int timeStamp) {
        return this.frameworkVersion == 393216 ? nativeGetTrackPointRegionAEWrapper(this.spweakaehandle, timeStamp) : nativeGetTrackPointRegion(this.handle, timeStamp);
    }

    public QTrajectoryData getTrajectory(int trIdx) {
        long j10 = this.handle;
        if (0 == j10 && 0 == this.spweakaehandle) {
            return null;
        }
        return this.frameworkVersion == 393216 ? nativeGetTrajectoryAEWrapper(this.spweakaehandle, trIdx) : nativeGetTrajectory(j10, trIdx);
    }

    public int getTrajectoryCount() {
        long j10 = this.handle;
        if (0 == j10 && 0 == this.spweakaehandle) {
            return -1;
        }
        return this.frameworkVersion == 393216 ? nativeGetTrajectoryCountAEWrapper(this.spweakaehandle) : nativeGetTrajectoryCount(j10);
    }

    public int insertEffect(QEffect effect) {
        return this.frameworkVersion == 393216 ? nativeEffectGroupInsertEffectAEWrapper(this.spweakaehandle, effect) : nativeEffectGroupInsertEffect(this.handle, effect);
    }

    public int insertNewTrajectory(int trIdx, QTrajectoryData trData) {
        long j10 = this.handle;
        if (0 == j10 && 0 == this.spweakaehandle) {
            return -1;
        }
        return this.frameworkVersion == 393216 ? nativeInsertNewTrajectoryAEWrapper(this.spweakaehandle, trIdx, trData) : nativeInsertNewTrajectory(j10, trIdx, trData);
    }

    public int insertOrReplaceKeyFrameCommonValue(int nKey, QKeyFrameCommonData.Value value) {
        return this.frameworkVersion == 393216 ? nativeInsertOrReplaceKeyFrameCommonValueAEWrapper(this.spweakaehandle, nKey, value) : nativeInsertOrReplaceKeyFrameCommonValue(this.handle, nKey, value);
    }

    public int layerPaintRedo() {
        return this.frameworkVersion == 393216 ? nativeLayerPaintRedoAEWrapper(this.spweakaehandle) : nativeLayerPaintRedo(this.handle);
    }

    public int layerPaintRedoCount() {
        return this.frameworkVersion == 393216 ? ((Integer) nativeGetPropAEWrapper(this.spweakaehandle, PROP_EFFECT_DRAW_LAYER_RECORD_LIST_COUNT)).intValue() : ((Integer) nativeGetProp(this.handle, PROP_EFFECT_DRAW_LAYER_RECORD_LIST_COUNT)).intValue();
    }

    public int layerPaintUndo() {
        return this.frameworkVersion == 393216 ? nativeLayerPaintUndoAEWrapper(this.spweakaehandle) : nativeLayerPaintUndo(this.handle);
    }

    public int layerPaintUndoCount() {
        return this.frameworkVersion == 393216 ? ((Integer) nativeGetPropAEWrapper(this.spweakaehandle, PROP_EFFECT_DRAW_LAYER_LIST_COUNT)).intValue() : ((Integer) nativeGetProp(this.handle, PROP_EFFECT_DRAW_LAYER_LIST_COUNT)).intValue();
    }

    public int moveSubItemSourceFromIndex(QEffect subEffect, int nIndex) {
        return this.frameworkVersion == 393216 ? nativeMoveSubItemSourceFromIndexAEWrapper(subEffect, nIndex) : nativeMoveSubItemSourceFromIndex(subEffect, nIndex);
    }

    public int refreshGroup() {
        return this.frameworkVersion == 393216 ? nativeEffectGroupRefreshGroupAEWrapper(this.spweakaehandle) : nativeEffectGroupRefreshGroup(this.handle);
    }

    public void removeAllTrajectory() {
        long j10 = this.handle;
        if (0 == j10 && 0 == this.spweakaehandle) {
            return;
        }
        if (this.frameworkVersion == 393216) {
            nativeRemoveAllTrajectoryAEWrapper(this.spweakaehandle);
        } else {
            nativeRemoveAllTrajectory(j10);
        }
    }

    public int removeKeyFrameCommonValue(int nKey, float fTimeStamp) {
        return this.frameworkVersion == 393216 ? nativeRemoveKeyFrameCommonValueAEWrapper(this.spweakaehandle, nKey, fTimeStamp) : nativeRemoveKeyFrameCommonValue(this.handle, nKey, fTimeStamp);
    }

    public int removeTrajectory(int trIdx) {
        long j10 = this.handle;
        if (0 == j10 && 0 == this.spweakaehandle) {
            return -1;
        }
        return this.frameworkVersion == 393216 ? nativeRemoveTrajectoryAEWrapper(this.spweakaehandle, trIdx) : nativeRemoveTrajectory(j10, trIdx);
    }

    public int replaceEffect(QEffect[] effectList) {
        return this.frameworkVersion == 393216 ? nativeReplaceEffectAEWrapper(this.spweakaehandle, effectList) : nativeReplaceEffect(this.handle, effectList);
    }

    public int set3DTransformInfo(QTransformInfo info) {
        return this.frameworkVersion == 393216 ? nativeSet3DTransformInfoAEWrapper(this.spweakaehandle, info) : nativeSet3DTransformInfo(this.handle, info);
    }

    public int setBlendMode(QEffectBlendMode blendMode) {
        return this.frameworkVersion == 393216 ? nativeSetPropAEWrapper(this, PROP_EFFECT_BLEND_MODE, Integer.valueOf(blendMode.ordinal())) : nativeSetProp(this, PROP_EFFECT_BLEND_MODE, Integer.valueOf(blendMode.ordinal()));
    }

    public void setEffectHandle(long lEffectHandle) {
        if (this.frameworkVersion == 393216) {
            this.spaehandle = lEffectHandle;
        } else {
            this.handle = lEffectHandle;
        }
    }

    public void setEffectSpWeakHandle(long lspEffectHandle) {
        if (this.frameworkVersion == 393216) {
            this.spweakaehandle = lspEffectHandle;
        } else {
            this.spweakhandle = lspEffectHandle;
        }
    }

    public int setExternalSource(int index, QEffectExternalSource externalSource) {
        return this.frameworkVersion == 393216 ? nativeSetExternalSourceAEWrapper(index, externalSource) : nativeSetExternalSource(index, externalSource);
    }

    public int setFaceHidden(int faceidx, boolean hidden) {
        return this.frameworkVersion == 393216 ? nativeSetFaceHiddenAEWrapper(this.spweakaehandle, faceidx, hidden) : nativeSetFaceHidden(this.handle, faceidx, hidden);
    }

    public void setFacePasterTransform(QFacePasterTransform transform, int faceidx, int layerid) {
        if (this.frameworkVersion == 393216) {
            nativeSetFacePasterTransformAEWrapper(this.spweakaehandle, transform, faceidx, layerid);
        } else {
            nativeSetFacePasterTransform(this.handle, transform, faceidx, layerid);
        }
    }

    public int setKeyFrameCommonData(QKeyFrameCommonData data) {
        return this.frameworkVersion == 393216 ? nativeSetKeyFrameCommonDataAEWrapper(this.spweakaehandle, data) : nativeSetKeyFrameCommonData(this.handle, data);
    }

    public int setKeyframeUnifromData(QKeyFrameUniformData data) {
        return this.frameworkVersion == 393216 ? nativeSetKeyframeUniformDataAEWrapper(this.spweakaehandle, data) : nativeSetKeyframeUniformData(this.handle, data);
    }

    public int setProperty(int propertyID, Object data) {
        if (0 == this.handle && 0 == this.spweakaehandle) {
            return QVEError.QERR_COMMON_JAVA_NOT_INIT;
        }
        return this.frameworkVersion == 393216 ? nativeSetPropAEWrapper(this, propertyID, data) : nativeSetProp(this, propertyID, data);
    }

    public void setSubEffectFlag(boolean bSubEft) {
        this.bSubEffect = bSubEft;
    }

    public int setSubItemSource(QEffectSubItemSource source) {
        return this.frameworkVersion == 393216 ? nativeSetSubItemSourceAEWrapper(source) : nativeSetSubItemSource(source);
    }

    public int setSubItemSourceFromIndex(QEffectSubItemSource source, int nIndex) {
        return this.frameworkVersion == 393216 ? nativeSetSubItemSourceFromIndexAEWrapper(source, nIndex) : nativeSetSubItemSourceFromIndex(source, nIndex);
    }

    public int setSubItemSourceList(QEffectSubItemSource[] source) {
        return this.frameworkVersion == 393216 ? nativeSetSubItemSourceListAEWrapper(source) : nativeSetSubItemSourceList(source);
    }

    public int setTextAdvanceStyle(int index, QEffectTextAdvStyle style) {
        if (0 == this.handle && 0 == this.spweakaehandle) {
            return QVEError.QERR_COMMON_JAVA_NOT_INIT;
        }
        style.index = index;
        return this.frameworkVersion == 393216 ? nativeSetPropAEWrapper(this, PROP_EFFECT_TEXT_ADV_STYLE, style) : nativeSetProp(this, PROP_EFFECT_TEXT_ADV_STYLE, style);
    }

    public int setTextAttachDuration(int type, int duration) {
        if (0 == this.handle && 0 == this.spweakaehandle) {
            return QVEError.QERR_COMMON_JAVA_NOT_INIT;
        }
        QEffectTextAttachDuration qEffectTextAttachDuration = new QEffectTextAttachDuration();
        if (duration < 0) {
            return QVEError.QERR_COMMON_JAVA_INVALID_PARAM;
        }
        qEffectTextAttachDuration.type = type;
        qEffectTextAttachDuration.duration = duration;
        return this.frameworkVersion == 393216 ? nativeSetPropAEWrapper(this, PROP_EFFECT_TEXT_ATTACHMENT_DURATION, qEffectTextAttachDuration) : nativeSetProp(this, PROP_EFFECT_TEXT_ATTACHMENT_DURATION, qEffectTextAttachDuration);
    }

    public int setTextAttachID(int type, long templateID) {
        if (0 == this.handle && 0 == this.spweakaehandle) {
            return QVEError.QERR_COMMON_JAVA_NOT_INIT;
        }
        QEffectTextAttachID qEffectTextAttachID = new QEffectTextAttachID();
        qEffectTextAttachID.type = type;
        qEffectTextAttachID.templateid = templateID;
        return this.frameworkVersion == 393216 ? nativeSetPropAEWrapper(this, PROP_EFFECT_TEXT_ATTACHMENT_ID, qEffectTextAttachID) : nativeSetProp(this, PROP_EFFECT_TEXT_ATTACHMENT_ID, qEffectTextAttachID);
    }

    public int setTextAttachScale(int type, float scale) {
        if (0 == this.handle && 0 == this.spweakaehandle) {
            return QVEError.QERR_COMMON_JAVA_NOT_INIT;
        }
        QEffectTextAttachScale qEffectTextAttachScale = new QEffectTextAttachScale();
        if (0.0f > scale) {
            return QVEError.QERR_COMMON_JAVA_INVALID_PARAM;
        }
        qEffectTextAttachScale.type = type;
        qEffectTextAttachScale.scale = scale;
        return this.frameworkVersion == 393216 ? nativeSetPropAEWrapper(this, PROP_EFFECT_TEXT_ATTACHMENT_SCALE, qEffectTextAttachScale) : nativeSetProp(this, PROP_EFFECT_TEXT_ATTACHMENT_SCALE, qEffectTextAttachScale);
    }

    public int setTextBoardConfig(int index, QEffectTextAdvStyle.TextBoardConfig config) {
        if (0 == this.handle && 0 == this.spweakaehandle) {
            return QVEError.QERR_COMMON_JAVA_NOT_INIT;
        }
        config.index = index;
        return this.frameworkVersion == 393216 ? nativeSetPropAEWrapper(this, PROP_EFFECT_TEXT_BOARD_CONFIG, config) : nativeSetProp(this, PROP_EFFECT_TEXT_BOARD_CONFIG, config);
    }

    public int startLayerPaintLine(QDrawLayerPaintPen pen) {
        return this.frameworkVersion == 393216 ? nativeStartLayerPaintLineAEWrapper(this.spweakaehandle, pen) : nativeStartLayerPaintLine(this.handle, pen);
    }

    public int startLayerPaintText(QDrawLayerPaintText pen) {
        if (this.frameworkVersion == 393216) {
            return nativeStartLayerPaintTextAEWrapper(this.spweakaehandle, pen);
        }
        return -1;
    }

    public QClip switchToClip() {
        if (this.frameworkVersion != 393216) {
            return null;
        }
        return nativeSwitchToClip(this.spweakaehandle);
    }

    public int updateKeyFrameCommonBaseValue(int nKey, float fOffsetValue) {
        return this.frameworkVersion == 393216 ? nativeUpdateKeyFrameCommonBaseValueAEWrapper(this.spweakaehandle, nKey, fOffsetValue) : nativeUpdateKeyFrameCommonBaseValue(this.handle, nKey, fOffsetValue);
    }

    public int updateTrajectory(int trIdx, QTrajectoryData trData) {
        long j10 = this.handle;
        if (0 == j10 && 0 == this.spweakaehandle) {
            return -1;
        }
        return this.frameworkVersion == 393216 ? nativeUpdateTrajectoryAEWrapper(this.spweakaehandle, trIdx, trData) : nativeUpdateTrajectory(j10, trIdx, trData);
    }

    public QEffectSubItemSource[] getSubItemSourceList(int min, int max) {
        QEffectSubItemSource[] qEffectSubItemSourceArrNativeGetSubItemSourceList = nativeGetSubItemSourceList();
        if (qEffectSubItemSourceArrNativeGetSubItemSourceList == null) {
            return null;
        }
        int length = qEffectSubItemSourceArrNativeGetSubItemSourceList.length;
        int i10 = 0;
        for (QEffectSubItemSource qEffectSubItemSource : qEffectSubItemSourceArrNativeGetSubItemSourceList) {
            int i11 = qEffectSubItemSource.m_nEffctSubType;
            if (i11 >= min && i11 < max) {
                i10++;
            }
        }
        if (i10 == 0) {
            return null;
        }
        QEffectSubItemSource[] qEffectSubItemSourceArr = new QEffectSubItemSource[i10];
        int i12 = 0;
        for (int i13 = 0; i13 < length; i13++) {
            QEffectSubItemSource qEffectSubItemSource2 = qEffectSubItemSourceArrNativeGetSubItemSourceList[i13];
            int i14 = qEffectSubItemSource2.m_nEffctSubType;
            if (i14 >= min && i14 < max) {
                qEffectSubItemSourceArr[i12] = new QEffectSubItemSource(i14, qEffectSubItemSource2.m_fLayerID, qEffectSubItemSource2.m_nFrameType, qEffectSubItemSource2.m_nEffectMode, qEffectSubItemSource2.m_lEffectHandle, qEffectSubItemSourceArrNativeGetSubItemSourceList[i13].m_mediaSource);
                i12++;
            }
        }
        return qEffectSubItemSourceArr;
    }

    public int getTextAttachDuration(int type, int index) {
        long j10 = this.handle;
        if (0 == j10 && 0 == this.spweakaehandle) {
            return QVEError.QERR_COMMON_JAVA_NOT_INIT;
        }
        if (this.frameworkVersion == 393216) {
            return nativeGetAttachDurationAEWrapper(this.spweakaehandle, type, index);
        }
        return nativeGetAttachDuration(j10, type, index);
    }

    public long getTextAttachID(int type, int index) {
        long j10 = this.handle;
        if (0 == j10 && 0 == this.spweakaehandle) {
            return 9429001L;
        }
        if (this.frameworkVersion == 393216) {
            return nativeGetAttachIDAEWrapper(this.spweakaehandle, type, index);
        }
        return nativeGetAttachID(j10, type, index);
    }

    public float getTextAttchScale(int type, int index) {
        long j10 = this.handle;
        if (0 == j10 && 0 == this.spweakaehandle) {
            return 9429001.0f;
        }
        if (this.frameworkVersion == 393216) {
            return nativeGetAttachScaleAEWrapper(this.spweakaehandle, type, index);
        }
        return nativeGetAttachScale(j10, type, index);
    }

    public int create(QEngine engine, int type, int trackType, int groupId, float layerID, boolean isPIP) {
        if (engine == null) {
            return QVEError.QERR_COMMON_JAVA_INVALID_PARAM;
        }
        int iIntValue = ((Integer) engine.getProperty(112)).intValue();
        this.frameworkVersion = iIntValue;
        if (iIntValue == 393216) {
            return nativeCreateAEWrapper(engine, this, trackType, groupId, type, layerID, isPIP);
        }
        return nativeCreate(engine, this, trackType, groupId, type, layerID);
    }

    public static class QEffectSubItemSource {
        public float m_fLayerID;
        private long m_lEffectHandle;
        private long m_lspEffectHandle;
        public QMediaSource m_mediaSource;
        public int m_nEffctSubType;
        public int m_nEffectMode;
        public int m_nFrameType;

        public QEffectSubItemSource() {
            this.m_nEffctSubType = 0;
            this.m_fLayerID = 0.0f;
            this.m_nFrameType = 0;
            this.m_nEffectMode = 0;
            this.m_lEffectHandle = 0L;
            this.m_lspEffectHandle = 0L;
            this.m_mediaSource = null;
        }

        public QEffectSubItemSource(int nEffectSubType, float fLayerId, int nFrameType, int effectMode, long lHandle, QMediaSource mediaSource) {
            this.m_lspEffectHandle = 0L;
            this.m_mediaSource = null;
            this.m_nEffctSubType = nEffectSubType;
            this.m_fLayerID = fLayerId;
            this.m_nFrameType = nFrameType;
            this.m_nEffectMode = effectMode;
            this.m_lEffectHandle = lHandle;
            if (mediaSource == null || mediaSource.getSource() == null || mediaSource.getSourceType() != 0) {
                return;
            }
            this.m_mediaSource = new QMediaSource(mediaSource.getSourceType(), mediaSource.isTempSource(), new String((String) mediaSource.getSource()));
        }
    }

    public int setTextAttachDuration(int type, int duration, int index) {
        if (0 == this.handle && 0 == this.spweakaehandle) {
            return QVEError.QERR_COMMON_JAVA_NOT_INIT;
        }
        QEffectTextAttachDuration qEffectTextAttachDuration = new QEffectTextAttachDuration();
        if (duration < 0) {
            return QVEError.QERR_COMMON_JAVA_INVALID_PARAM;
        }
        qEffectTextAttachDuration.type = type;
        qEffectTextAttachDuration.duration = duration;
        qEffectTextAttachDuration.index = index;
        if (this.frameworkVersion == 393216) {
            return nativeSetPropAEWrapper(this, PROP_EFFECT_TEXT_ATTACHMENT_DURATION, qEffectTextAttachDuration);
        }
        return nativeSetProp(this, PROP_EFFECT_TEXT_ATTACHMENT_DURATION, qEffectTextAttachDuration);
    }

    public int setTextAttachID(int type, long templateID, int index) {
        if (0 == this.handle && 0 == this.spweakaehandle) {
            return QVEError.QERR_COMMON_JAVA_NOT_INIT;
        }
        QEffectTextAttachID qEffectTextAttachID = new QEffectTextAttachID();
        qEffectTextAttachID.type = type;
        qEffectTextAttachID.templateid = templateID;
        qEffectTextAttachID.index = index;
        if (this.frameworkVersion == 393216) {
            return nativeSetPropAEWrapper(this, PROP_EFFECT_TEXT_ATTACHMENT_ID, qEffectTextAttachID);
        }
        return nativeSetProp(this, PROP_EFFECT_TEXT_ATTACHMENT_ID, qEffectTextAttachID);
    }

    public int setTextAttachScale(int type, float scale, int index) {
        if (0 == this.handle && 0 == this.spweakaehandle) {
            return QVEError.QERR_COMMON_JAVA_NOT_INIT;
        }
        QEffectTextAttachScale qEffectTextAttachScale = new QEffectTextAttachScale();
        if (0.0f > scale) {
            return QVEError.QERR_COMMON_JAVA_INVALID_PARAM;
        }
        qEffectTextAttachScale.type = type;
        qEffectTextAttachScale.scale = scale;
        qEffectTextAttachScale.index = index;
        if (this.frameworkVersion == 393216) {
            return nativeSetPropAEWrapper(this, PROP_EFFECT_TEXT_ATTACHMENT_SCALE, qEffectTextAttachScale);
        }
        return nativeSetProp(this, PROP_EFFECT_TEXT_ATTACHMENT_SCALE, qEffectTextAttachScale);
    }

    public QEffect(int frameworkVer) {
        this.tmpbufferhandle = 0L;
        this.masktmpbufferhandle = 0L;
        this.handle = 0L;
        this.sphandle = 0L;
        this.spweakhandle = 0L;
        this.spaehandle = 0L;
        this.spweakaehandle = 0L;
        this.bSubEffect = false;
        this.frameworkVersion = frameworkVer;
    }
}
