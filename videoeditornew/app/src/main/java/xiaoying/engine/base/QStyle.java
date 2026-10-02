package xiaoying.engine.base;

import androidx.core.app.FrameMetricsAggregator;
import xiaoying.engine.QEngine;
import xiaoying.engine.clip.QEffect;
import xiaoying.engine.clip.QEffectPathFxDefConfig;
import xiaoying.engine.clip.QEffectTextAdvStyle;
import xiaoying.engine.clip.QEffectTextTokenizeType;
import xiaoying.engine.storyboard.QStoryboard;
import xiaoying.utils.QBitmap;
import xiaoying.utils.QBitmapFactory;
import xiaoying.utils.QColorSpace;
import xiaoying.utils.QRect;
import xiaoying.utils.QSize;

/* JADX INFO: loaded from: classes19.dex */
public class QStyle {
    public static final int APP_INPUT_EXPRESSION_TYPE_BEGIN = 11;
    public static final int APP_INPUT_EXPRESSION_TYPE_CLICK = 12;
    public static final int COMBO_PASTER_SUBTYPE_FACIAL = 1;
    public static final int COMBO_PASTER_SUBTYPE_NORMAL = 0;
    public static final int COMBO_PASTER_SUBTYPE_OT = 2;
    public static final int COMBO_SUBTYPE_TEXTANIMATION = 3;
    public static final long DEFAULT_BUBBLE_TEMPLATE_ID = 648518346341351425L;
    public static final int DEMO_EXAMPLE_FILE_ID = 12;
    public static final int DIVA_EFFECT_SUB_TYPE_BG = 1;
    public static final int DIVA_SUB_TYPE_FILTER = 1;
    public static final int DIVA_SUB_TYPE_FREEZE_FRAME = 2;
    public static final int DIVA_SUB_TYPE_LYRIC = 0;
    public static final int EFFECT_ABFACE_TYPE_COMBO = 2;
    public static final int EFFECT_ABFACE_TYPE_INDEX = 3;
    public static final int EFFECT_ABFACE_TYPE_LEFT = 0;
    public static final int EFFECT_ABFACE_TYPE_RIGHT = 1;
    public static final int EU_PLUG_IN_TEMPLATE_TYPE_AE_LAYER = 5;
    public static final int EU_PLUG_IN_TEMPLATE_TYPE_ALPHA_MATTE = 6;
    public static final int EU_PLUG_IN_TEMPLATE_TYPE_DEFALUT = 0;
    public static final int EU_PLUG_IN_TEMPLATE_TYPE_FIT = 1;
    public static final int EU_PLUG_IN_TEMPLATE_TYPE_MOTIONTILE = 3;
    public static final int EU_PLUG_IN_TEMPLATE_TYPE_STORYBOARD = 4;
    public static final int EU_PLUG_IN_TEMPLATE_TYPE_TRANFORM = 2;
    public static final int EXPRESSION_START_MUSIC_FILE_ID = 1002;
    public static final int FACEDT_EXPRESSION_TYPE_EYEBROW_RAISE = 2;
    public static final int FACEDT_EXPRESSION_TYPE_EYE_OPEN = 3;
    public static final int FACEDT_EXPRESSION_TYPE_HEADNOD = 5;
    public static final int FACEDT_EXPRESSION_TYPE_HEADSHAKE = 4;
    public static final int FACEDT_EXPRESSION_TYPE_HEAD_SHAKENOD = 6;
    public static final int FACEDT_EXPRESSION_TYPE_MOUTH_OPEN = 1;
    public static final int FACEDT_EXPRESSION_TYPE_NONE = 0;
    public static final int FX_SUBTYPE_DANCING = 2;
    public static final int FX_SUBTYPE_NORMAL = 0;
    public static final int FX_SUBTYPE_SELFIE = 3;
    public static final int FX_SUBTYPE_SINGING = 1;
    public static final int IE_SUBTYPE_BLEND_CAM_FD = 7;
    public static final int IE_SUBTYPE_FACE_DEFORMATION = 5;
    public static final int IE_SUBTYPE_FB_POSTPROCESS = 2;
    public static final int IE_SUBTYPE_FB_PREPROCESS = 3;
    public static final int IE_SUBTYPE_FUNNY = 1;
    public static final int IE_SUBTYPE_GRAFFITY = 8;
    public static final int IE_SUBTYPE_NORMAL = 0;
    public static final int IE_SUBTYPE_TEXT_ANIMATION = 4;
    public static final int LAYOUT_MASK_ALL = -1;
    public static final int LAYOUT_MASK_W16_H9 = 8;
    public static final int LAYOUT_MASK_W1_H1 = 16;
    public static final int LAYOUT_MASK_W3_H4 = 1;
    public static final int LAYOUT_MASK_W4_H3 = 2;
    public static final int LAYOUT_MASK_W9_H16 = 4;
    public static final int LRC_FILE_ID = 1001;
    public static final int MODE_MASK_ALL = -1;
    public static final int MODE_MASK_ANIMATED_FRAME = 6;
    public static final int MODE_MASK_BUBBLE = 9;
    public static final int MODE_MASK_COMBO_PASTER = 17;
    public static final int MODE_MASK_COVER = 2;
    public static final int MODE_MASK_DIVA = 15;
    public static final int MODE_MASK_EFFECT = 4;
    public static final int MODE_MASK_FREEZE_FRAME = 18;
    public static final int MODE_MASK_LYRIC = 19;
    public static final int MODE_MASK_MUSIC = 7;
    public static final int MODE_MASK_NONE = 0;
    public static final int MODE_MASK_PASTER_FRAME = 5;
    public static final int MODE_MASK_PIP = 12;
    public static final int MODE_MASK_POSTER = 8;
    public static final int MODE_MASK_SOUND = 13;
    public static final int MODE_MASK_THEME = 1;
    public static final int MODE_MASK_TRANSITION = 3;
    public static final int MODE_MASK_VIDEO_CLIP = 14;
    public static final int MODE_MASK_WATERMARK = 11;
    public static final int MUSIC_FILE_ID = 1000;
    public static final int NEW_FRAMEWORK_VERSION = 262144;
    public static final long NONE_ANIMATED_FRAME_TEMPLATE_ID = 432345564227567616L;
    public static final long NONE_BUBBLE_TEMPLATE_ID = 648518346341351424L;
    public static final long NONE_FILTER_TEMPLATE_ID = 288230376151711744L;
    public static final long NONE_MUSIC_TEMPLATE_ID = 504403158265495552L;
    public static final long NONE_POSTER_TEMPLATE_ID = 576460752303423488L;
    public static final long NONE_THEME_TEMPLATE_ID = 72057594037927936L;
    public static final long NONE_TRANSITION_TEMPLATE_ID = 216172782113783808L;
    public static final int PASTER_LOOP_MUISC_FILE_ID = 1003;
    public static final int PASTER_SUBTYPE_DYNAMIC = 5;
    public static final int PASTER_SUBTYPE_FACIAL = 1;
    public static final int PASTER_SUBTYPE_FACIAL_ATTACH = 2;
    public static final int PASTER_SUBTYPE_NORMAL = 0;
    public static final int PASTER_SUBTYPE_OT = 3;
    public static final int PASTER_SUBTYPE_STATIC = 4;
    public static final int SENIOR_TEXT_VERSION = 196608;
    public static final int TEXT_SUBTYPE_ANIMATION = 1;
    public static final int TEXT_SUBTYPE_NORMAL = 0;
    public static final int THEME_SUBTYPE_FUNNY = 1;
    public static final int THEME_SUBTYPE_NORMAL = 0;
    public static final int THEME_SUBTYPE_STORY = 2;
    public static final int THUMBNAIL_FILE_ID = 3;
    public static final int TRANSITION_DURATION_UNEDITABLE = 0;
    public static final int TRANSITION_EDITABLE = 1;
    private long handle = 0;

    public static class QAnimatedFrameTemplateInfo {
        public QRect defaultRegion;
        public int duration;
        public int examplePos;
        public int frameHeight;
        public int frameWidth;
        public boolean hasAudio;
    }

    public static class QEffect3DMaterialItem {
        public QSize NodeSize;
        public QSize ViewSize;
        public int paramid;
        public int resamplemode;
        public String strMaterialName;
        public int taorigin;
        public int taparamid;
    }

    public static class QEffectKeyLineInterfaceItem {
        public float def_v;
        public int eftIdx;
        public int itemID;
        public int mainType;
        public float max_v;
        public float min_v;
        public String name;
        public int subType;
        public String uniformName;
    }

    public static class QEffectKliiWithUserData {
        public int eftIdx;
        public float endV;
        public int itemID;
        public QRange range;
        public float startV;
        public long templateID;
    }

    public static class QEffectPropertyData {
        public int mID;
        public int mValue;
    }

    public static class QEffectPropertyInfo {
        public int adjust_pos;
        public int control_type;
        public int cur_value;
        public int groupid_index;

                public int id;
        public boolean is_support_key;
        public boolean is_unlimited_mode;
        public String key_name;
        public int max_value;
        public int min_value;
        public String name;
        public int precision;
        public int step;
        public int unit;
        public String wildcard;
    }

    public static class QExternalFileInfo {
        public int fileID;
        public String fileName;
        public int subTemplateID;
        public long templateID;
    }

    public static class QFinder {
        private long handle = 0;

        private native int nativeCreate(QFinderParam param);

        private native int nativeDestroy();

        private native int nativeGetCount(long handle);

        private native String nativeGetFileName(long handle, int index);

        private native int nativeUpdate(long handle);

        public int create(QFinderParam param) {
            if (param == null) {
                return QVEError.QERR_COMMON_JAVA_INVALID_PARAM;
            }
            if (this.handle != 0) {
                destroy();
            }
            return nativeCreate(param);
        }

        public int destroy() {
            return this.handle == 0 ? QVEError.QERR_COMMON_JAVA_NOT_INIT : nativeDestroy();
        }

        public int getCount() {
            long j10 = this.handle;
            if (j10 == 0) {
                return 0;
            }
            return nativeGetCount(j10);
        }

        public String getFileName(int index) {
            long j10 = this.handle;
            if (j10 == 0) {
                return null;
            }
            return nativeGetFileName(j10, index);
        }

        public int update() {
            long j10 = this.handle;
            return j10 == 0 ? QVEError.QERR_COMMON_JAVA_NOT_INIT : nativeUpdate(j10);
        }
    }

    public static class QFinderParam {
        private String path = null;
        private String serialno = null;
        private long themeID = 0;
        private int mode = 0;
        private boolean listCommon = true;

        public void setCommon(boolean common) {
            this.listCommon = common;
        }

        public void setMode(int mode) {
            this.mode = mode;
        }

        public void setPath(String path) {
            this.path = path;
        }

        public void setSerialno(String serialno) {
            this.serialno = serialno;
        }

        public void setThemeID(long themeID) {
            this.themeID = themeID;
        }
    }

    public static class QFreezeFrameBasicInfo {
        public int mDefLength;
        public int mMaxLength;
        public int mMinLength;
        public int mVersion;
        public boolean mbLengthEditable;
    }

    public static class QPasteSwitchInfo {
        public QPasteSwitchGroupInfo[] groupList;
        public int random;
        public int type;

        public static class QPasteSwitchGroupInfo {
            public int[] itemList;
            public int switchExpType;
        }
    }

    public static class QPasterABFaceInfo {
        public int applyface;
        public boolean bABFace;
        public int totalcount;
        public int type;
    }

    public static class QPasterFacialType {
        public static final int FACIAL_POS_TYPE_EYEBROWS_BOUND = 1;
        public static final int FACIAL_POS_TYPE_EYEBROWS_LBOUND = 2;
        public static final int FACIAL_POS_TYPE_EYEBROWS_RBOUND = 3;
        public static final int FACIAL_POS_TYPE_HAIR_BOUND = 0;
        public static final int FACIAL_SUBTYPE_BEARD = 4;
        public static final int FACIAL_SUBTYPE_EYE = 2;
        public static final int FACIAL_SUBTYPE_EYEBROWS = 1;
        public static final int FACIAL_SUBTYPE_HAIR = 0;
        public static final int FACIAL_SUBTYPE_NOSE = 3;
        public int mSubType = 0;
    }

    public static class QSlideShowSceCfgInfo {
        public int mVersion = 0;
        public int mBestDispTime = 0;
        public QSlideShowSceCfgItem[] mCoverItem = null;
        public QSlideShowSceCfgItem[] mBodyItem = null;
        public QSlideShowSceCfgItem[] mBackCoverItem = null;
        public boolean mOnlySceneMode = false;
        public boolean mForceFit = false;
    }

    public static class QSlideShowSceCfgItem {
        public long mID = 0;
        public int mSrcCount = 0;
        public int mRevCount = 0;
        public int[] mPreviewPos = null;
        public int[] mSourceType = null;
        public int[] mContourApply = null;
        public int[] mProcessMode = null;
        public QSize mViewSize = null;
        public QRect[] mRegion = null;
        public QRect[] mBestRegion = null;
    }

    public static class QSlideShowSourceRangeListInfo {
        public boolean mOnlySceneMode = false;
        public QRange[] mSceneRangeList = null;
        public int[] mRangeIndexList = null;
    }

    public static class QTemplateContentInfo {
        public static final int CVEALGOPriorityLevel_AutoLut = 25;
        public static final int CVEALGOPriorityLevel_CartoonLite = 18;
        public static final int CVEALGOPriorityLevel_ColorCorrection = 5;
        public static final int CVEALGOPriorityLevel_ColorMatch = 6;
        public static final int CVEALGOPriorityLevel_FACECARTOON = 4;
        public static final int CVEALGOPriorityLevel_FACE_DETECT = 1;
        public static final int CVEALGOPriorityLevel_FaceMorphing = 14;
        public static final int CVEALGOPriorityLevel_FaceSwap = 11;
        public static final int CVEALGOPriorityLevel_FaceTrack = 17;
        public static final int CVEALGOPriorityLevel_PersonInstSeg = 10;
        public static final int CVEALGOPriorityLevel_Segment = 2;
        public static final int CVEALGOPriorityLevel_SegmentCloth = 3;
        public static final int CVEALGOPriorityLevel_SegmentPeg = 15;
        public static final int CVEALGOPriorityLevel_SingleTrack = 13;
        public static final int CVEALGOPriorityLevel_Skeleton = 7;
        public static final int CVEALGOPriorityLevel_SmartVideoCrop = 8;
        public static final int CVEALGOPriorityLevel_SpliterHead = 9;
        public static final int CVEALGOPriorityLevel_VFI = 12;
        public static final int CVEALGOPriorityLevel_VOS = 16;
        public static final int MACRO_ALGO_SUB_TYPE_FLAG = 268435456;
        public int[] mAlgorithm;
        public boolean mIsNeedAlgorithm;
        public int mIsNeedFaceFeature;
        public boolean mIsNeedSegment;
        public boolean mIsOffline;
        public boolean mIsPhoto;
        public long mReservedID;
        public long mSeqenceID;
        public long mSubSequenceID;
        public int mSubType;

        public static int getAlgoAISubType(int dwType) {
            if ((dwType & 268435456) == 268435456) {
                return (dwType & 4080) >> 4;
            }
            return 0;
        }

        public static int getAlgoAIType(int dwType) {
            return (dwType & 268435456) == 268435456 ? (dwType & 268369920) >> 16 : dwType;
        }
    }

    public static class QTemplateIDUtils {
        private static final int TPBSC_RESERVED = 40;
        private static final int TPBSC_SUB_SEQUENCE = 48;
        private static final int TPBSC_TEMPLATE_ID_TOTAL_BITS = 64;
        private static final int TPBSC_TYPE = 56;
        private static final long TPMASK_GET_OFFLINE_FLAG = 1099511627776L;
        private static final long TPMASK_GET_PICTURE_FLAG = 2199023255552L;
        private static final long TPMASK_GET_RESERVED_ID = 263882790666240L;
        private static final long TPMASK_GET_SEQUENCE_ID = 17592186044415L;
        private static final long TPMASK_GET_SUB_SEQUENCE_ID = 35747322042253312L;
        private static final long TPMASK_IS_PUBLIC = 4611686018427387904L;
        private static final long TPMASK_IS_SELFDEF = 2305843009213693952L;
        private static final long TPMASK_IS_SPECIAL = 267911168;
        private static final long TPMASK_IS_THEME_SUB_TEMPLATE = 36028797018963968L;
        private static final long TPMASK_TYPE = 2233785415175766016L;
        private static QEngine mEngine;

        public static int getTemplateReservedID(long templateID) {
            QTemplateContentInfo templateContentInfo = QStyle.getTemplateContentInfo(mEngine, templateID);
            return (int) (templateContentInfo != null ? templateContentInfo.mReservedID : (int) ((templateID & 263882790666240L) >>> 40));
        }

        public static long getTemplateSeqenceID(long templateID) {
            QTemplateContentInfo templateContentInfo = QStyle.getTemplateContentInfo(mEngine, templateID);
            return templateContentInfo != null ? templateContentInfo.mSeqenceID : templateID & TPMASK_GET_SEQUENCE_ID;
        }

        public static int getTemplateSubSequenceID(long templateID) {
            QTemplateContentInfo templateContentInfo = QStyle.getTemplateContentInfo(mEngine, templateID);
            return (int) (templateContentInfo != null ? templateContentInfo.mSubSequenceID : (templateID & TPMASK_GET_SUB_SEQUENCE_ID) >>> 48);
        }

        public static int getTemplateSubType(long templateID) {
            QTemplateContentInfo templateContentInfo = QStyle.getTemplateContentInfo(mEngine, templateID);
            return templateContentInfo != null ? templateContentInfo.mSubType & FrameMetricsAggregator.EVERY_DURATION : (int) (((templateID & TPMASK_IS_SPECIAL) >> 19) & 511);
        }

        public static int getTemplateType(long templateID) {
            return (int) ((templateID & TPMASK_TYPE) >>> 56);
        }

        public static boolean isFBPostProcessTemplate(long templateID) {
            QTemplateContentInfo templateContentInfo = QStyle.getTemplateContentInfo(mEngine, templateID);
            if (templateContentInfo != null) {
                return ((long) templateContentInfo.mSubType) == 2;
            }
            return ((templateID & TPMASK_IS_SPECIAL) >> 19) == 2;
        }

        public static boolean isFBPreProcessTemplate(long templateID) {
            QTemplateContentInfo templateContentInfo = QStyle.getTemplateContentInfo(mEngine, templateID);
            if (templateContentInfo != null) {
                return ((long) templateContentInfo.mSubType) == 3;
            }
            return ((templateID & TPMASK_IS_SPECIAL) >> 19) == 3;
        }

        public static boolean isFunnyEffectTemplate(long templateID) {
            QTemplateContentInfo templateContentInfo = QStyle.getTemplateContentInfo(mEngine, templateID);
            if (templateContentInfo != null) {
                return ((long) templateContentInfo.mSubType) == 1;
            }
            return templateID == 288230376151711803L || templateID == 288230376151711806L || ((templateID & TPMASK_IS_SPECIAL) >> 19) == 1;
        }

        public static boolean isGraffitiTemplate(long templateID) {
            QTemplateContentInfo templateContentInfo = QStyle.getTemplateContentInfo(mEngine, templateID);
            if (templateContentInfo != null) {
                return templateContentInfo.mSubType == 8;
            }
            return ((templateID & TPMASK_IS_SPECIAL) >> 19) == 8;
        }

        public static boolean isOfflineTemplate(long templateID) {
            QTemplateContentInfo templateContentInfo = QStyle.getTemplateContentInfo(mEngine, templateID);
            if (templateContentInfo != null) {
                return templateContentInfo.mIsOffline;
            }
            return (templateID & 1099511627776L) != 0;
        }

        public static boolean isPhotoTemplate(long templateID) {
            QTemplateContentInfo templateContentInfo = QStyle.getTemplateContentInfo(mEngine, templateID);
            if (templateContentInfo != null) {
                return templateContentInfo.mIsPhoto;
            }
            return (templateID & TPMASK_GET_PICTURE_FLAG) != 0;
        }

        public static boolean isPublicTemplate(long templateID) {
            return 0 == (templateID & 4611686018427387904L);
        }

        public static boolean isSelfDefTemplate(long templateID) {
            return 0 != (templateID & 2305843009213693952L);
        }

        public static boolean isThemeSubTemplate(long templateID) {
            return 0 != (templateID & TPMASK_IS_THEME_SUB_TEMPLATE);
        }

        public static void setEngine(QEngine qEngine) {
            mEngine = qEngine;
        }
    }

    public static QTransStypeClipRangeInfo GetTemplateClipRangeInfo(String templatePath) {
        return nativeGetTemplateClipRangeInfo(templatePath);
    }

    public static long creatEffectThumbnailEngine(QEngine engine, QSize bgSize) {
        return nativeCreatEffectThumbnailEngine(engine, bgSize);
    }

    public static int destroyEffectThumbnailEngine(long lThumbnailEngine) {
        if (lThumbnailEngine == 0) {
            return 0;
        }
        nativeDestroyEffectThumbnailEngine(lThumbnailEngine);
        return 0;
    }

    public static boolean getBubbleIsAdujestAlpha(QEngine engine, long tmeplateID, QSize size) {
        return nativeGetBubbleIsAdujestAlpha(engine, tmeplateID, size);
    }

    public static QBitmap getBubbleThumbnailFromTemplate(QEngine engine, QBubbleTextSource bubbleSource, QSize bgSize, QSize bmpSize, QSize contentSize, int timeStamp) {
        int i10;
        int i11;
        QBitmap qBitmapCreateQBitmapBlank_noSkia;
        if (engine == null || bubbleSource == null || bmpSize == null || bgSize == null || contentSize == null || (i10 = bmpSize.mWidth) <= 0 || (i11 = bmpSize.mHeight) <= 0 || contentSize.mWidth <= 0 || contentSize.mHeight <= 0 || (qBitmapCreateQBitmapBlank_noSkia = QBitmapFactory.createQBitmapBlank_noSkia(i10, i11, QColorSpace.QPAF_RGB32_A8R8G8B8)) == null) {
            return null;
        }
        if (nativeGetBubbleThumbnailByTemplate(engine, qBitmapCreateQBitmapBlank_noSkia, bubbleSource, bgSize, contentSize, timeStamp) == 0) {
            return qBitmapCreateQBitmapBlank_noSkia;
        }
        qBitmapCreateQBitmapBlank_noSkia.recycle();
        return null;
    }

    public static QEffectPropertyInfo[] getIEPropertyInfo(QEngine engine, long templateID) {
        return nativeGetIEPropertyInfo(engine, templateID);
    }

    public static int[] getItemIDsInMotionTemplate(QEngine engine, long templateID) {
        return nativeGetItemIDsInMotionTemplate(engine, templateID);
    }

    public static QEffectKeyLineInterfaceItem[] getKeyLineInterfaceItems(QEngine engine, long templateID) {
        return nativeGetKeyLineInterfaceItems(engine, templateID);
    }

    public static QSlideShowSourceRangeListInfo getSlideSHowSourceRangeList(QEngine engine, long templateID) {
        return nativeGetSlideShowSceneSourceRangeList(engine, templateID);
    }

    public static QTemplateContentInfo getTemplateContentInfo(QEngine engine, long templateID) {
        return nativeGetTemplateContentInfo(engine, templateID);
    }

    public static QPaintStyleInfo getTemplatePaintStyle(QEngine engine, long templateID) {
        return nativeGetTemplatePaintStyle(engine, templateID);
    }

    public static QEffectPathFxDefConfig[] getTemplatePathFxDefColorAndWidth(QEngine engine, long templateID) {
        return nativeGetTemplatePathFxDefColorAndWidth(engine, templateID);
    }

    public static int getTemplateSubType(long templateID) {
        return (int) ((templateID & 267911168) >> 19);
    }

    public static boolean getTemplateSupportChangePathFxColorAndWidth(QEngine engine, long templateID) {
        return nativeGetTemplateSupportChangePathFxColorAndWidth(engine, templateID);
    }

    public static QBitmap getTextThumbnail(long lThumbnailEngine, QBubbleTextSource[] bubbleSource, QSize bmpSize, QSize contentSize, int timeStamp) {
        int i10;
        int i11;
        QBitmap qBitmapCreateQBitmapBlank_noSkia;
        if (lThumbnailEngine == 0 || bubbleSource == null || (i10 = bmpSize.mWidth) <= 0 || (i11 = bmpSize.mHeight) <= 0 || (qBitmapCreateQBitmapBlank_noSkia = QBitmapFactory.createQBitmapBlank_noSkia(i10, i11, QColorSpace.QPAF_RGB32_A8R8G8B8)) == null) {
            return null;
        }
        nativeGetTextThumbnail(lThumbnailEngine, bubbleSource.length, bubbleSource, qBitmapCreateQBitmapBlank_noSkia, contentSize, timeStamp);
        return qBitmapCreateQBitmapBlank_noSkia;
    }

    public static QThemeClipList getThemeClipList(QEngine engine, long templateID) {
        return nativeGetThemeClipList(engine, templateID);
    }

    public static QThemeClipList getThemeClipListWithCover(QEngine engine, long templateID, int maxDuration) {
        return nativeGetThemeClipListWithCover(engine, templateID, maxDuration);
    }

    public static String[] getThemeDefaultMusicPaths(QEngine engine, String themePath) {
        return nativeGetThemeDefaultMusicPaths(engine, themePath);
    }

    public static boolean[] getThemeSegmentInfo(QEngine engine, long templateID) {
        return nativeGetThemeSegmentInfo(engine, templateID);
    }

    public static QThemeStyleList getThemeStyleList(QEngine engine, long templateID) {
        return nativeGetThemeStyleList(engine, templateID);
    }

    public static boolean hasSubEffectType(QEngine engine, long templateID, int type) {
        return nativeStyleHasSubEffectType(engine, templateID, type);
    }

    public static boolean isAsrTextTemplateStatic(String xytFilePath) {
        return nativeIsAsrTextTemplate_static(xytFilePath);
    }

    public static boolean isFacePasterTemplate(long templateID) {
        QTemplateContentInfo templateContentInfo = getTemplateContentInfo(QTemplateIDUtils.mEngine, templateID);
        if (templateContentInfo != null) {
            if (templateContentInfo.mIsNeedFaceFeature == 0) {
                return false;
            }
        } else if ((templateID & 2233785415443677184L) != 360287970190688256L) {
            return false;
        }
        return true;
    }

    public static QBubbleMeasureResult measureBubbleByTemplate(String bubbleTemplate, QSize BGSize, String text, String auxiliaryFont) {
        QBubbleMeasureResult qBubbleMeasureResult = new QBubbleMeasureResult();
        if (nativeMeasureBubbleByTemplate(bubbleTemplate, BGSize, text, auxiliaryFont, qBubbleMeasureResult) != 0) {
            return null;
        }
        return qBubbleMeasureResult;
    }

    public static QBubbleMeasureResult measureBubbleSourceByTemplate(String bubbleTemplate, QSize BGSize, QBubbleTextSource bubbleSource) {
        QBubbleMeasureResult qBubbleMeasureResult = new QBubbleMeasureResult();
        if (nativeMeasureBubbleSourceByTemplate(bubbleTemplate, BGSize, bubbleSource, qBubbleMeasureResult) != 0) {
            return null;
        }
        return qBubbleMeasureResult;
    }

    public static QBubbleMeasureResult measureBubbleSourceByTemplateAndTokenize(String bubbleTemplate, QSize BGSize, QBubbleTextSource bubbleSource, QEffectTextTokenizeType textTokenizeType) {
        QBubbleMeasureResult qBubbleMeasureResult = new QBubbleMeasureResult();
        if (nativeMeasureBubbleSourceByTemplateAndTokenize(bubbleTemplate, BGSize, bubbleSource, textTokenizeType, qBubbleMeasureResult) != 0) {
            return null;
        }
        return qBubbleMeasureResult;
    }

    public static QBubbleMeasureResult measureBubbleSourceByTemplateAndTokenizeTime(String bubbleTemplate, QSize BGSize, QBubbleTextSource bubbleSource, QEffectTextTokenizeType.QTextTokenizeStrType textTokenizeStrType, int timestamp) {
        QBubbleMeasureResult qBubbleMeasureResult = new QBubbleMeasureResult();
        if (nativeMeasureBubbleSourceByTemplateAndTokenizeTime(bubbleTemplate, BGSize, bubbleSource, textTokenizeStrType, timestamp, qBubbleMeasureResult) != 0) {
            return null;
        }
        return qBubbleMeasureResult;
    }

    private static native long nativeCreatEffectThumbnailEngine(QEngine engine, QSize bgSize);

    private native int nativeCreate(String styleFile, String serialNumber, int bgLayoutMode);

    private native int nativeDestroy();

    private static native int nativeDestroyEffectThumbnailEngine(long lThumbnailEngine);

    private native int nativeExtractExampleFile(long handle, String dstBubbleFile);

    private native QEffect3DMaterialItem[] nativeGet3DMaterialItemArray(long handle);

    private native QAnimatedFrameTemplateInfo nativeGetAnimatedFrameTemplateInfo(long handle, int width, int height);

    private static native boolean nativeGetBubbleIsAdujestAlpha(QEngine engine, long templateID, QSize size);

    private native QBubbleTemplateInfo nativeGetBubbleTemplateInfo(QEngine engine, long handle, int langeuageID, int paramID, int width, int height);

    private static native int nativeGetBubbleThumbnailByTemplate(QEngine engine, QBitmap thumb, QBubbleTextSource bubbleSource, QSize bgSize, QSize contentSize, int timeStamp);

    private native QPasterFacialType nativeGetCategroyFacialType(long handle);

    private native int nativeGetCategroyID(long handle);

    private native int nativeGetConfigureCount(long handle);

    private native String nativeGetDescription(long handle, int languageID);

    private native int nativeGetDummyFlag(long handle);

    private native QExternalFileInfo[] nativeGetExternalFileInfos(long handle);

    private native int nativeGetFilterDuration(long handle);

    private native QFreezeFrameBasicInfo nativeGetFreezeFrameBasicInfo(long handle);

    private native long nativeGetID(long handle);

    private static native QEffectPropertyInfo[] nativeGetIEPropertyInfo(QEngine einge, long templateID);

    private native int nativeGetInfoVersion(long handle);

    private static native int[] nativeGetItemIDsInMotionTemplate(QEngine engine, long templateID);

    private static native QEffectKeyLineInterfaceItem[] nativeGetKeyLineInterfaceItems(QEngine engine, long templateID);

    private native int nativeGetMode(long handle);

    private static native QPasteSwitchInfo nativeGetPasteSwitchInfo(long handle);

    private native QPasterABFaceInfo nativeGetPasterABFaceInfo(long handle);

    private native int nativeGetPasterExpressionType(long handle);

    private native int nativeGetPreviewData(long handle, QStoryboard storyboard);

    private native int nativeGetSceneDuration(long handle);

    private native QSlideShowSceCfgInfo nativeGetSlideShowSceCfgInfo(long handle);

    private static native QSlideShowSourceRangeListInfo nativeGetSlideShowSceneSourceRangeList(QEngine engine, long templateID);

    private native long[] nativeGetSubPasterID(long handle);

    private native int nativeGetSupportedLayouts(long handle);

    private static native QTransStypeClipRangeInfo nativeGetTemplateClipRangeInfo(String templatePath);

    private static native QTemplateContentInfo nativeGetTemplateContentInfo(QEngine engine, long templateID);

    private static native QTemplateGroupInfo nativeGetTemplateGroupInfo(long handle);

    private native String nativeGetTemplateName(long handle, int languageID);

    private static native QPaintStyleInfo nativeGetTemplatePaintStyle(QEngine engine, long templateID);

    private static native QEffectPathFxDefConfig[] nativeGetTemplatePathFxDefColorAndWidth(QEngine engine, long templateID);

    private static native boolean nativeGetTemplateSupportChangePathFxColorAndWidth(QEngine engine, long templateID);

    private native QEffectTextAdvStyle nativeGetTemplateTextAdvanceStyle(QEngine engine, long handle, int langeuageID, int paramID, int width, int height, boolean acceptsNull);

    private native QEffect.QEffectTextAttachFileInfo[] nativeGetTemplateTextAnimateTemplateList(QEngine engine, long handle, int langeuageID, int paramID, int width, int height);

    private native QTextMulInfo nativeGetTextMulInfo(QEngine engine, long handle, QSize bGSize, int languageID);

    private static native int nativeGetTextThumbnail(long lThumbnailEngine, int textCount, QBubbleTextSource[] bubbleSource, QBitmap bmp, QSize contextSize, int timeStamp);

    private static native QThemeClipList nativeGetThemeClipList(QEngine engine, long templateID);

    private static native QThemeClipList nativeGetThemeClipListWithCover(QEngine engine, long templateID, int maxDuration);

    private native int nativeGetThemeCoverPosition(long handle);

    private static native String[] nativeGetThemeDefaultMusicPaths(QEngine engine, String themePath);

    private native int nativeGetThemeExportSize(long handle, QSize exportSize);

    private native QSize[] nativeGetThemeExportSizeList(long handle);

    private static native boolean[] nativeGetThemeSegmentInfo(QEngine engine, long templateID);

    private static native QThemeStyleList nativeGetThemeStyleList(QEngine engine, long templateID);

    private static native int nativeGetThumbnail(long lThumbnailEngine, long lTemplateID, QBitmap bmp, int timeStamp);

    private native int nativeGetThumbnail(QEngine engine, long handle, QBitmap thumbnail);

    private native int nativeGetTransAudioID(long handle);

    private native int nativeGetTransDuration(long handle);

    private native int nativeGetTransEditable(long handle);

    private native int nativeGetTransformType(long handle);

    private native int nativeGetVersion(long handle);

    private native boolean nativeIsARTemplate(long handle);

    private native int nativeIsAsrTextTemplate(long handle);

    private static native boolean nativeIsAsrTextTemplate_static(String xytFilePath);

    private native boolean nativeIsAudioVisualizationTemplate(long handle);

    private native boolean nativeIsFixedSizeTheme(long handle);

    private native boolean nativeIsOTSupportTemplate(long handle);

    private native boolean nativeIsSlideShowTheme(long handle);

    private native boolean nativeIsSupportAlphaAdjust(long handle);

    private static native int nativeMeasureBubbleByTemplate(String template, QSize BGSize, String text, String auxiliaryFont, QBubbleMeasureResult bmr);

    private static native int nativeMeasureBubbleSourceByTemplate(String template, QSize BGSize, QBubbleTextSource bubbleSource, QBubbleMeasureResult bmr);

    private static native int nativeMeasureBubbleSourceByTemplateAndTokenize(String template, QSize BGSize, QBubbleTextSource bubbleSource, QEffectTextTokenizeType textTokenizeType, QBubbleMeasureResult bmr);

    private static native int nativeMeasureBubbleSourceByTemplateAndTokenizeTime(String template, QSize BGSize, QBubbleTextSource bubbleSource, QEffectTextTokenizeType.QTextTokenizeStrType textTokenizeStrType, int timestamp, QBubbleMeasureResult bmr);

    private static native boolean nativeStyleHasSubEffectType(QEngine einge, long templateID, int type);

    public int create(String styleFile, String serialNumber, int bgLayoutMode) {
        if (styleFile == null) {
            return QVEError.QERR_COMMON_JAVA_INVALID_PARAM;
        }
        if (this.handle != 0) {
            destroy();
        }
        return nativeCreate(styleFile, serialNumber, bgLayoutMode);
    }

    public int destroy() {
        return this.handle == 0 ? QVEError.QERR_COMMON_JAVA_NOT_INIT : nativeDestroy();
    }

    public int extractExampleFile(String dstBubbleFile) {
        return nativeExtractExampleFile(this.handle, dstBubbleFile);
    }

    public QEffect3DMaterialItem[] get3DMaterialItemArray() {
        return nativeGet3DMaterialItemArray(this.handle);
    }

    public QAnimatedFrameTemplateInfo getAnimatedFrameTemplateInfo(int width, int height) {
        return nativeGetAnimatedFrameTemplateInfo(this.handle, width, height);
    }

    public QBubbleTemplateInfo getBubbleTemplateInfo(QEngine engine, int languageID, int width, int height) {
        return nativeGetBubbleTemplateInfo(engine, this.handle, languageID, -1, width, height);
    }

    public QPasterFacialType getCategroyFacialType() {
        long j10 = this.handle;
        if (j10 == 0) {
            return null;
        }
        return nativeGetCategroyFacialType(j10);
    }

    public int getCategroyID() {
        long j10 = this.handle;
        if (j10 == 0) {
            return 0;
        }
        return nativeGetCategroyID(j10);
    }

    public int getConfigureCount() {
        long j10 = this.handle;
        if (j10 == 0) {
            return 0;
        }
        return nativeGetConfigureCount(j10);
    }

    public String getDescription(int languageID) {
        long j10 = this.handle;
        if (j10 == 0) {
            return null;
        }
        return nativeGetDescription(j10, languageID);
    }

    public boolean getDummyFlag() {
        long j10 = this.handle;
        return (j10 == 0 || nativeGetDummyFlag(j10) == 0) ? false : true;
    }

    public QExternalFileInfo[] getExternalFileInfos() {
        long j10 = this.handle;
        if (j10 == 0) {
            return null;
        }
        return nativeGetExternalFileInfos(j10);
    }

    public int getFilterDuration() {
        return nativeGetFilterDuration(this.handle);
    }

    public QFreezeFrameBasicInfo getFreezeFrameBasicInfo() {
        long j10 = this.handle;
        if (j10 == 0) {
            return null;
        }
        return nativeGetFreezeFrameBasicInfo(j10);
    }

    public long getID() {
        long j10 = this.handle;
        if (j10 == 0) {
            return 0L;
        }
        return nativeGetID(j10);
    }

    public int getInfoVersion() {
        long j10 = this.handle;
        if (j10 == 0) {
            return 0;
        }
        return nativeGetInfoVersion(j10);
    }

    public int getMode() {
        long j10 = this.handle;
        return j10 == 0 ? QVEError.QERR_COMMON_JAVA_NOT_INIT : nativeGetMode(j10);
    }

    public QPasteSwitchInfo getPasteSwitchInfo() {
        return nativeGetPasteSwitchInfo(this.handle);
    }

    public QPasterABFaceInfo getPasterABFaceInfo() {
        return nativeGetPasterABFaceInfo(this.handle);
    }

    public int getPasterExpressionType() {
        long j10 = this.handle;
        if (j10 == 0) {
            return 0;
        }
        return nativeGetPasterExpressionType(j10);
    }

    public int getPreviewData(QStoryboard storyboard, IQSessionStateListener stateListener) {
        if (this.handle == 0) {
            return QVEError.QERR_COMMON_JAVA_NOT_INIT;
        }
        if (storyboard == null || stateListener == null) {
            return QVEError.QERR_COMMON_JAVA_INVALID_PARAM;
        }
        storyboard.setSessionStateListener(stateListener);
        return nativeGetPreviewData(this.handle, storyboard);
    }

    public int getSceneDuration() {
        return nativeGetSceneDuration(this.handle);
    }

    public QSlideShowSceCfgInfo getSlideShowSceCfgInfo() {
        return nativeGetSlideShowSceCfgInfo(this.handle);
    }

    public long[] getSubPasterID() {
        long j10 = this.handle;
        if (j10 == 0) {
            return null;
        }
        return nativeGetSubPasterID(j10);
    }

    public int getSupportedLayouts() {
        long j10 = this.handle;
        if (j10 == 0) {
            return 0;
        }
        return nativeGetSupportedLayouts(j10);
    }

    public QTemplateGroupInfo getTemplateGroupInfo() {
        return nativeGetTemplateGroupInfo(this.handle);
    }

    public String getTemplateName(int languageID) {
        long j10 = this.handle;
        if (j10 == 0) {
            return null;
        }
        return nativeGetTemplateName(j10, languageID);
    }

    public QEffectTextAdvStyle getTemplateTextAdvanceStyle(QEngine engine, int languageID, int width, int height) {
        return nativeGetTemplateTextAdvanceStyle(engine, this.handle, languageID, -1, width, height, false);
    }

    public QEffect.QEffectTextAttachFileInfo[] getTemplateTextAnimateTemplateList(QEngine engine, int languageID, int paramID, int width, int height) {
        return nativeGetTemplateTextAnimateTemplateList(engine, this.handle, languageID, paramID, width, height);
    }

    public QTextMulInfo getTextMulInfo(QEngine engine, QSize bgSize, int languageID) {
        return nativeGetTextMulInfo(engine, this.handle, bgSize, languageID);
    }

    public int getThemeCoverPosition() {
        return nativeGetThemeCoverPosition(this.handle);
    }

    public int getThemeExportSize(QSize exportSize) {
        return nativeGetThemeExportSize(this.handle, exportSize);
    }

    public QSize[] getThemeExportSizeList() {
        return nativeGetThemeExportSizeList(this.handle);
    }

    public int getThumbnail(QEngine engine, QBitmap thumbnail) {
        long j10 = this.handle;
        if (j10 == 0) {
            return QVEError.QERR_COMMON_JAVA_NOT_INIT;
        }
        return (engine == null || thumbnail == null) ? QVEError.QERR_COMMON_JAVA_INVALID_PARAM : nativeGetThumbnail(engine, j10, thumbnail);
    }

    public int getTransformType() {
        return nativeGetTransformType(this.handle);
    }

    public int getTransitionAudioID() {
        long j10 = this.handle;
        if (j10 == 0) {
            return 0;
        }
        return nativeGetTransAudioID(j10);
    }

    public int getTransitionDuration() {
        long j10 = this.handle;
        if (j10 == 0) {
            return 0;
        }
        return nativeGetTransDuration(j10);
    }

    public int getTransitionEditable() {
        long j10 = this.handle;
        if (j10 == 0) {
            return 0;
        }
        return nativeGetTransEditable(j10);
    }

    public int getVersion() {
        long j10 = this.handle;
        if (j10 == 0) {
            return 0;
        }
        return nativeGetVersion(j10);
    }

    public int isAsrTextTemplate() {
        long j10 = this.handle;
        if (j10 == 0) {
            return 0;
        }
        return nativeIsAsrTextTemplate(j10);
    }

    public boolean isAudioVisualizationTemplate() {
        long j10 = this.handle;
        if (0 == j10) {
            return false;
        }
        return nativeIsAudioVisualizationTemplate(j10);
    }

    public boolean isFixedSizeTheme() {
        return nativeIsFixedSizeTheme(this.handle);
    }

    public boolean isGraffitiTemplate() {
        return QTemplateIDUtils.isGraffitiTemplate(getID());
    }

    public boolean isOTSupportTemplate() {
        long id2 = getID();
        if (QTemplateIDUtils.getTemplateType(id2) == 17 && QTemplateIDUtils.getTemplateSubType(id2) == 2) {
            return true;
        }
        if (QTemplateIDUtils.getTemplateType(id2) == 5 && QTemplateIDUtils.getTemplateSubType(id2) == 3) {
            return true;
        }
        return nativeIsOTSupportTemplate(this.handle);
    }

    public boolean isSlideShowTheme() {
        return nativeIsSlideShowTheme(this.handle);
    }

    public boolean isSupportAlphaAdjust() {
        return nativeIsSupportAlphaAdjust(this.handle);
    }

    public QBubbleTemplateInfo getBubbleTemplateInfo(QEngine engine, int languageID, int paramID, int width, int height) {
        return nativeGetBubbleTemplateInfo(engine, this.handle, languageID, paramID, width, height);
    }

    public QEffectTextAdvStyle getTemplateTextAdvanceStyle(QEngine engine, int languageID, int paramID, int width, int height) {
        return nativeGetTemplateTextAdvanceStyle(engine, this.handle, languageID, paramID, width, height, false);
    }

    public static QBitmap getThumbnail(long lThumbnailEngine, long lTemplateID, QSize bmpSize, int timeStamp) {
        int i10;
        int i11;
        QBitmap qBitmapCreateQBitmapBlank_noSkia;
        if (lThumbnailEngine == 0 || (i10 = bmpSize.mWidth) <= 0 || (i11 = bmpSize.mHeight) <= 0 || (qBitmapCreateQBitmapBlank_noSkia = QBitmapFactory.createQBitmapBlank_noSkia(i10, i11, QColorSpace.QPAF_RGB32_A8R8G8B8)) == null) {
            return null;
        }
        nativeGetThumbnail(lThumbnailEngine, lTemplateID, qBitmapCreateQBitmapBlank_noSkia, timeStamp);
        return qBitmapCreateQBitmapBlank_noSkia;
    }

    public QEffectTextAdvStyle getTemplateTextAdvanceStyle(QEngine engine, int languageID, int paramID, int width, int height, boolean acceptsNull) {
        return nativeGetTemplateTextAdvanceStyle(engine, this.handle, languageID, paramID, width, height, acceptsNull);
    }

    public static class QTextInfo {
        public static final int TEXTINFO_ALIGNMENT = 2;
        public static final int TEXTINFO_ALIGNMENT_BOTTOM = 8;
        public static final int TEXTINFO_ALIGNMENT_FREE_STYLE = 0;
        public static final int TEXTINFO_ALIGNMENT_HOR_CENTER = 32;
        public static final int TEXTINFO_ALIGNMENT_HOR_FULLFILL = 128;
        public static final int TEXTINFO_ALIGNMENT_LEFT = 1;
        public static final int TEXTINFO_ALIGNMENT_MIDDLE = 16;
        public static final int TEXTINFO_ALIGNMENT_NONE = 0;
        public static final int TEXTINFO_ALIGNMENT_RIGHT = 2;
        public static final int TEXTINFO_ALIGNMENT_TOP = 4;
        public static final int TEXTINFO_ALIGNMENT_VER_ABOVE_CENTER = 1024;
        public static final int TEXTINFO_ALIGNMENT_VER_CENTER = 64;
        public static final int TEXTINFO_ALIGNMENT_VER_FULLFILL = 256;
        public static final int TEXTINFO_ALIGNMENT_VER_UNDER_CENTER = 512;
        public static final int TEXTINFO_COLOR = 1;
        public static final int TEXTINFO_EDITABLE = 5;
        public static final int TEXTINFO_EDITABLE_MASK_ALIGNMENT = 8;
        public static final int TEXTINFO_EDITABLE_MASK_COLOR = 4;
        public static final int TEXTINFO_EDITABLE_MASK_FONT = 32;
        public static final int TEXTINFO_EDITABLE_MASK_NONE = 0;
        public static final int TEXTINFO_EDITABLE_MASK_REGION = 1;
        public static final int TEXTINFO_EDITABLE_MASK_ROTATE = 16;
        public static final int TEXTINFO_EDITABLE_MASK_SIZE = 2;
        public static final int TEXTINFO_EDITABLE_MASK_TEXTALL = 63;
        public static final int TEXTINFO_ROTATE = 3;
        public static final int TEXTINFO_SIZE = 0;
        public static final int TEXTINFO_TIMESTAMP = 4;
        public int absHeight;
        public int alignment;
        public int bgArea;
        public int color;
        public int editable;
        public QI18NItemInfo[] i18nInfoArray;
        public boolean isAbsPadding;
        public QRect paddingRect;
        public QRect region;
        public int rotate;
        public int size;
        public int timestamp;

        public QTextInfo() {
            this.region = null;
            this.size = 0;
            this.color = 0;
            this.alignment = 0;
            this.rotate = 0;
            this.timestamp = 0;
            this.editable = 0;
            this.i18nInfoArray = null;
            this.bgArea = 0;
            this.absHeight = 0;
            this.isAbsPadding = false;
            this.paddingRect = new QRect(0, 0, 0, 0);
        }

        public int GetTextInfo(int type) {
            if (type == 0) {
                return this.size;
            }
            if (type == 1) {
                return this.color;
            }
            if (type == 2) {
                return this.alignment;
            }
            if (type == 3) {
                return this.rotate;
            }
            if (type == 4) {
                return this.timestamp;
            }
            if (type != 5) {
                return 0;
            }
            return this.editable;
        }

        public QRect GetTextRect() {
            return this.region;
        }

        public QTextInfo(QTextInfo info) {
            if (info != null) {
                this.region = info.region;
                this.size = info.size;
                this.color = info.color;
                this.alignment = info.alignment;
                this.rotate = info.rotate;
                this.timestamp = info.timestamp;
                this.editable = info.editable;
                this.i18nInfoArray = info.i18nInfoArray;
                this.bgArea = info.bgArea;
                this.absHeight = info.absHeight;
                this.isAbsPadding = info.isAbsPadding;
                QRect qRect = info.paddingRect;
                this.paddingRect = new QRect(qRect.left, qRect.top, qRect.right, qRect.bottom);
            }
        }
    }
}
