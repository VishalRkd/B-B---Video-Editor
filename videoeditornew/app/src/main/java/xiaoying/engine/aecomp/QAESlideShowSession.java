package xiaoying.engine.aecomp;

import java.util.ArrayList;
import xiaoying.engine.QEngine;
import xiaoying.engine.base.IQSessionStateListener;
import xiaoying.engine.base.QRange;
import xiaoying.engine.base.QSession;
import xiaoying.engine.base.QTextAnimationInfo;
import xiaoying.engine.slideshowsession.QSlideShowSession;
import xiaoying.engine.storyboard.IQThemeOperationListener;
import xiaoying.engine.storyboard.QThemeOperation;
import xiaoying.utils.QPoint;
import xiaoying.utils.QRect;
import xiaoying.utils.QSize;
import xiaoying.utils.QTransformPara;

/* JADX INFO: loaded from: classes19.dex */
public class QAESlideShowSession extends QSession {
    public static final int DECODER_USAGE_TYPE_AUTO = 4;
    public static final int DECODER_USAGE_TYPE_HW = 1;
    public static final int DECODER_USAGE_TYPE_SW = 2;
    public static final int FEATURE_POINT_COUNT = 106;
    public static final int MAX_FACE_COUNT = 4;
    public static final int PROP_AUDIO_PITCH = 20497;
    private static final int PROP_BASE = 20480;
    public static final int PROP_CLIP_RESAMPLE_MODE = 20483;
    public static final int PROP_DEC_USE_TYPE = 20490;
    public static final int PROP_LANGUAGE_ID = 20489;
    public static final int PROP_LRCTMP_ID = 20494;
    public static final int PROP_LRC_FILE = 20493;
    public static final int PROP_LYRIC_ENABLE = 20492;
    public static final int PROP_MUSIC_FADEIN = 20486;
    public static final int PROP_MUSIC_FADEOUT = 20487;
    public static final int PROP_MUSIC_MIX_PERCENT = 20491;
    public static final int PROP_OUTPUT_RESOLUTION = 20484;
    public static final int PROP_SCENE_RESOLUTION = 20485;
    public static final int PROP_SLIDESHOW_DISABLE_SINGLE_SCENE_MODE = 20501;
    public static final int PROP_SLIDESHOW_VIRTUAL_DISP_ALIGNMENT_MODE = 20500;
    public static final int PROP_TIME_SCALE = 20496;
    public static final int PROP_TRACK_DATA_FILE = 20495;
    public static final int PROP_USE_LOOP_PLAY_MODE = 20498;
    public static final int PROP_USE_MUTI_SOURCE_MODE = 20499;
    public static final int QVET_SLIDESHOW_ALIGNMENT_MODE_CENTER = 1;
    public static final int QVET_SLIDESHOW_ALIGNMENT_MODE_DEFALUT = 0;
    public static final int SLIDESHOW_SESSION_STATUS_APPLY_THEME = 6;
    public static final int SLIDESHOW_SESSION_STATUS_CANCLE = 9;
    public static final int SLIDESHOW_SESSION_STATUS_DESTROY_COMP = 1;
    public static final int SLIDESHOW_SESSION_STATUS_FACE_DETECT = 2;
    public static final int SLIDESHOW_SESSION_STATUS_MAKE_COMP = 5;
    public static final int SLIDESHOW_SESSION_STATUS_NONE = 0;
    public static final int SLIDESHOW_SESSION_STATUS_PARSE_CONFIG = 3;
    public static final int SLIDESHOW_SESSION_STATUS_SET_MUSIC = 7;
    public static final int SLIDESHOW_SESSION_STATUS_STOPPED = 8;
    public static final int SLSH_SCENE_PANZOOM_MODE_BLUR = 1;
    public static final int SLSH_SCENE_PANZOOM_MODE_FILL = 2;
    public static final int SLSH_SCENE_PANZOOM_MODE_NONE = 0;
    public static final int SLSH_SCENE_PANZOOM_MODE_TRANSPARENT = 3;
    public static final int SLSH_SOURCE_TYPE_IMAGE = 1;
    public static final int SLSH_SOURCE_TYPE_NONE = 0;
    public static final int SLSH_SOURCE_TYPE_VIDEO = 2;
    public static final int SLSH_TRANSFORM_TYPE_BLUR = 8;
    public static final int SLSH_TRANSFORM_TYPE_COLOR_FILL = 9;
    private IQThemeOperationListener themeOPListener = null;

    public static int getProjectVersion(QEngine engine, String projectPath) {
        return nativeGetProjectVersion(engine, projectPath);
    }

    private int getSceneCompCount() {
        return nativeGetSceneCompCount(this.handle);
    }

    private int getStoryboardTAEffectCount() {
        return nativeGetStoryboardTAEffectCount(this.handle);
    }

    private QTextAnimationInfo[] getStoryboardTextAnimationInfoArray(int index) {
        return nativeGetStoryboardTextAnimationInfoArray(this.handle, index);
    }

    private native boolean nativeCanInsertVideoSource(long handle, int virtualSrcIndex);

    private native int nativeCancleMakeComp(long handle);

    private native int nativeClearOrgSourceInfoList(long handle);

    private native int nativeCreate(QEngine engine, QAESlideShowSession session);

    private native int nativeDestroy(QAESlideShowSession session);

    private native int nativeDetectFace(long handle, QSlideShowSession.QSourceInfoNode sourceInfoNode);

    private native QAEComp nativeDuplicateComp(long handle);

    private native QTextAnimationInfo[] nativeGetClipTextAnimationInfoArray(long handle, int clipIndex);

    private native QAEComp nativeGetComp(long handle);

    private native String nativeGetDefaultMusic(long handle);

    private native String nativeGetMusic(long handle);

    private native QRange nativeGetMusicRange(long handle);

    private native boolean nativeGetMute(long handle);

    private native QSlideShowSession.QSourceInfoNode nativeGetOrgSource(long handle, int srcIndex);

    private native int nativeGetOrgSourceCount(long handle);

    public static native int nativeGetProjectVersion(QEngine engine, String projectPath);

    private native int nativeGetSceneCompCount(long handle);

    private native QSlideShowSession.QSourceInfoNode nativeGetSource(long handle, int index);

    private native int nativeGetSourceCount(long handle);

    private native int nativeGetStoryboardTAEffectCount(long handle);

    private native QTextAnimationInfo[] nativeGetStoryboardTextAnimationInfoArray(long handle, int index);

    private native long nativeGetTheme(long handle);

    private native float nativeGetVirtualNodeOrgScaleValue(long handle, int virtualIndex);

    private native QSlideShowSession.QVirtualSourceInfoNode[] nativeGetVirtualSrcInfoNodeList(long handle);

    private native int nativeInsertSource(long handle, QSlideShowSession.QSourceInfoNode sourceInfoNode);

    private native int nativeLoadProject(long handle, String strPrjFile);

    private native int nativeMakeComp(long handle, QSize size);

    private native int nativeMoveVirtualSource(long handle, int srcIndex, int dstIndex);

    private native int nativeReMakeComp(long handle);

    private native int nativeRefreshSourceList(long handle);

    private native void nativeRemoveSource(long handle, int index);

    private native int nativeSaveProject(long handle, String strPrjFile);

    private native int nativeSetMusic(long handle, String strMusicSource, QRange range);

    private native int nativeSetMute(long handle, boolean bMute);

    private native int nativeSetRect2TransParam(QRect inRect, float fAangle, QTransformPara transformPara);

    private native int nativeSetTextAnimationInfo(long handle, QTextAnimationInfo info);

    private native int nativeSetTheme(long handle, long lTemplateID);

    private native int nativeSetTransParam2Rect(QTransformPara transParam, QSize inViewSize, QRect outRect);

    private native int nativeSetVirtualSourceTransformFlag(long handle, int virtualSrcIndex, boolean bTransformFlag);

    private native int nativeSetVirtualSourceTransformPara(long handle, int virtualSrcIndex, QTransformPara transformPara);

    private native int nativeSetVirtualSourceTrimRange(long handle, int virtualSrcIndex, QRange trimRange, boolean bPlayToEnd);

    private native int nativeUpdateVirtualSource(long handle, int virtualSrcIndex, QSlideShowSession.QSourceInfoNode sourceInfoNode);

    private native int nativeUpdateVirtualSourceFaceCenter(long handle, int virtualSrcIndex, QPoint point);

    private int onThemeOperation(QThemeOperation operation) {
        IQThemeOperationListener iQThemeOperationListener = this.themeOPListener;
        if (iQThemeOperationListener == null) {
            return 0;
        }
        return iQThemeOperationListener.onThemeOperation(operation);
    }

    public int CancleMakeComp() {
        return nativeCancleMakeComp(this.handle);
    }

    public int DetectFace(QSlideShowSession.QSourceInfoNode sourceinfoNode) {
        return nativeDetectFace(this.handle, sourceinfoNode);
    }

    public QAEComp DuplicateComp() {
        return nativeDuplicateComp(this.handle);
    }

    public QAEComp GetComp() {
        return nativeGetComp(this.handle);
    }

    public String GetDefaultMusic() {
        return nativeGetDefaultMusic(this.handle);
    }

    public String GetMusic() {
        return nativeGetMusic(this.handle);
    }

    public QRange GetMusicRange() {
        return nativeGetMusicRange(this.handle);
    }

    public boolean GetMute() {
        return nativeGetMute(this.handle);
    }

    public QSlideShowSession.QSourceInfoNode GetSource(int index) {
        return nativeGetSource(this.handle, index);
    }

    public int GetSourceCount() {
        return nativeGetSourceCount(this.handle);
    }

    public long GetTheme() {
        return nativeGetTheme(this.handle);
    }

    public int InsertSource(QSlideShowSession.QSourceInfoNode sourceInfoNode) {
        return nativeInsertSource(this.handle, sourceInfoNode);
    }

    public int LoadProject(String strPrjFile, IQSessionStateListener listener) {
        this.listener = listener;
        return nativeLoadProject(this.handle, strPrjFile);
    }

    public int MakeComp(QSize size, IQSessionStateListener statelistener, IQThemeOperationListener themeoplisener) {
        this.listener = statelistener;
        this.themeOPListener = themeoplisener;
        return nativeMakeComp(this.handle, size);
    }

    public int ReMakeComp() {
        return nativeReMakeComp(this.handle);
    }

    public int RefreshSourceList() {
        return nativeRefreshSourceList(this.handle);
    }

    public void RemoveSource(int index) {
        nativeRemoveSource(this.handle, index);
    }

    public int SaveProject(String strPrjFile, IQSessionStateListener listener) {
        this.listener = listener;
        return nativeSaveProject(this.handle, strPrjFile);
    }

    public int SetMusic(String strMusicSource, QRange range) {
        return nativeSetMusic(this.handle, strMusicSource, range);
    }

    public int SetMute(boolean bMute) {
        return nativeSetMute(this.handle, bMute);
    }

    public int SetRect2TransParam(QRect inRect, float fAangle, QTransformPara transformPara) {
        if (inRect == null || transformPara == null) {
            return -1;
        }
        return nativeSetRect2TransParam(inRect, fAangle, transformPara);
    }

    public int SetTheme(long lTemplateID) {
        return nativeSetTheme(this.handle, lTemplateID);
    }

    public int SetTransParam2Rect(QTransformPara transParam, QSize inViewSize, QRect outRect) {
        if (transParam == null || inViewSize == null || outRect == null) {
            return -1;
        }
        return nativeSetTransParam2Rect(transParam, inViewSize, outRect);
    }

    public int SetVirtualSourceTransformFlag(QSlideShowSession.QVirtualSourceInfoNode virtualSourceInfoNode, boolean bTransformFlag) {
        if (virtualSourceInfoNode.mVirtualSourceInfoObj == null) {
            return -1;
        }
        virtualSourceInfoNode.mbTransformFlag = bTransformFlag;
        return nativeSetVirtualSourceTransformFlag(this.handle, virtualSourceInfoNode.mVirtualSrcIndex, bTransformFlag);
    }

    public int SetVirtualSourceTrimRange(QSlideShowSession.QVirtualSourceInfoNode virtualSourceInfoNode, QRange trimRange, boolean bPlayToEnd) {
        Object obj;
        if (virtualSourceInfoNode.mSourceType != 2 || (obj = virtualSourceInfoNode.mVirtualSourceInfoObj) == null) {
            return -1;
        }
        ((QSlideShowSession.QVirtualVideoSourceInfo) obj).mtrimRange = trimRange;
        return nativeSetVirtualSourceTrimRange(this.handle, virtualSourceInfoNode.mVirtualSrcIndex, trimRange, bPlayToEnd);
    }

    public int UpdateVirtualSource(QSlideShowSession.QVirtualSourceInfoNode virtualSourceInfoNode, QSlideShowSession.QSourceInfoNode sourceInfoNode) {
        virtualSourceInfoNode.mSourceType = sourceInfoNode.mSourceType;
        new String(sourceInfoNode.mstrSourceFile);
        if (sourceInfoNode.mSourceType == 1) {
            QSlideShowSession.QVirtualImageSourceInfo qVirtualImageSourceInfo = new QSlideShowSession.QVirtualImageSourceInfo();
            QSlideShowSession.QImageSourceInfo qImageSourceInfo = (QSlideShowSession.QImageSourceInfo) sourceInfoNode.mSourceInfoObj;
            qVirtualImageSourceInfo.mbFaceDetected = qImageSourceInfo.mbFaceDetected;
            qVirtualImageSourceInfo.mFaceCenterX = qImageSourceInfo.mFaceCenterX;
            qVirtualImageSourceInfo.mFaceCenterY = qImageSourceInfo.mFaceCenterY;
            virtualSourceInfoNode.mVirtualSourceInfoObj = qVirtualImageSourceInfo;
        } else {
            QSlideShowSession.QVirtualVideoSourceInfo qVirtualVideoSourceInfo = new QSlideShowSession.QVirtualVideoSourceInfo();
            QSlideShowSession.QVideoSourceInfo qVideoSourceInfo = (QSlideShowSession.QVideoSourceInfo) sourceInfoNode.mSourceInfoObj;
            int i10 = qVideoSourceInfo.mSrcRange.get(1);
            int i11 = virtualSourceInfoNode.mSceneDuration;
            if (i10 <= i11) {
                i11 = qVideoSourceInfo.mSrcRange.get(1);
            }
            qVirtualVideoSourceInfo.mtrimRange = new QRange(qVideoSourceInfo.mSrcRange.get(0), i11);
            qVirtualVideoSourceInfo.mPicCenterX = 5000;
            qVirtualVideoSourceInfo.mPicCenterY = 5000;
            virtualSourceInfoNode.mVirtualSourceInfoObj = qVirtualVideoSourceInfo;
        }
        return nativeUpdateVirtualSource(this.handle, virtualSourceInfoNode.mVirtualSrcIndex, sourceInfoNode);
    }

    public int UpdateVirtualSourceFaceCenter(QSlideShowSession.QVirtualSourceInfoNode virtualSourceInfoNode, QPoint point) {
        Object obj = virtualSourceInfoNode.mVirtualSourceInfoObj;
        if (obj == null) {
            return -1;
        }
        if (virtualSourceInfoNode.mSourceType == 1) {
            QSlideShowSession.QVirtualImageSourceInfo qVirtualImageSourceInfo = (QSlideShowSession.QVirtualImageSourceInfo) obj;
            qVirtualImageSourceInfo.mFaceCenterX = point.x;
            qVirtualImageSourceInfo.mFaceCenterY = point.y;
        } else {
            QSlideShowSession.QVirtualVideoSourceInfo qVirtualVideoSourceInfo = (QSlideShowSession.QVirtualVideoSourceInfo) obj;
            qVirtualVideoSourceInfo.mPicCenterX = point.x;
            qVirtualVideoSourceInfo.mPicCenterY = point.y;
        }
        return nativeUpdateVirtualSourceFaceCenter(this.handle, virtualSourceInfoNode.mVirtualSrcIndex, point);
    }

    public boolean canInsretVideoSource(int virtualSrcIndex) {
        return nativeCanInsertVideoSource(this.handle, virtualSrcIndex);
    }

    public int clearOrgSourceInfoList() {
        return nativeClearOrgSourceInfoList(this.handle);
    }

    public QTextAnimationInfo[] getClipTextAnimationInfoArray(int clipIndex) {
        return nativeGetClipTextAnimationInfoArray(this.handle, clipIndex);
    }

    public QSlideShowSession.QSourceInfoNode getOrgSource(int srcIndex) {
        return nativeGetOrgSource(this.handle, srcIndex);
    }

    public int getOrgSourceCount() {
        return nativeGetOrgSourceCount(this.handle);
    }

    public QTextAnimationInfo[] getTextAnimationInfoArray() {
        int sceneCompCount = getSceneCompCount();
        int storyboardTAEffectCount = getStoryboardTAEffectCount();
        ArrayList arrayList = new ArrayList();
        int length = 0;
        for (int i10 = 0; i10 < storyboardTAEffectCount; i10++) {
            QTextAnimationInfo[] storyboardTextAnimationInfoArray = getStoryboardTextAnimationInfoArray(i10);
            if (storyboardTextAnimationInfoArray != null) {
                arrayList.add(storyboardTextAnimationInfoArray);
                length += storyboardTextAnimationInfoArray.length;
            }
        }
        for (int i11 = 0; i11 < sceneCompCount; i11++) {
            QTextAnimationInfo[] clipTextAnimationInfoArray = getClipTextAnimationInfoArray(i11);
            if (clipTextAnimationInfoArray != null) {
                arrayList.add(clipTextAnimationInfoArray);
                length += clipTextAnimationInfoArray.length;
            }
        }
        if (length <= 0) {
            return null;
        }
        QTextAnimationInfo[] qTextAnimationInfoArr = new QTextAnimationInfo[length];
        int i12 = 0;
        for (int i13 = 0; i13 < arrayList.size(); i13++) {
            for (QTextAnimationInfo qTextAnimationInfo : (QTextAnimationInfo[]) arrayList.get(i13)) {
                qTextAnimationInfoArr[i12] = qTextAnimationInfo;
                i12++;
            }
        }
        return qTextAnimationInfoArr;
    }

    public float getVirtualNodeOrgScaleValue(int virtualnodeIndex) {
        return nativeGetVirtualNodeOrgScaleValue(this.handle, virtualnodeIndex);
    }

    public QSlideShowSession.QVirtualSourceInfoNode[] getVirtualSourceInfoNodeList() {
        return nativeGetVirtualSrcInfoNodeList(this.handle);
    }

    @Override // xiaoying.engine.base.QSession
    public int init(QEngine engine, IQSessionStateListener listener) {
        super.init(engine, listener);
        return nativeCreate(engine, this);
    }

    public int moveVirtualSource(int srcIndex, int dstIndex) {
        return nativeMoveVirtualSource(this.handle, srcIndex, dstIndex);
    }

    public int setTextAnimationInfo(QTextAnimationInfo info) {
        return nativeSetTextAnimationInfo(this.handle, info);
    }

    public int setVirtualSourceTransformPara(QSlideShowSession.QVirtualSourceInfoNode virtualNode, QTransformPara transformPara) {
        virtualNode.mbTransformFlag = true;
        virtualNode.mTransformPara = transformPara;
        return nativeSetVirtualSourceTransformPara(this.handle, virtualNode.mVirtualSrcIndex, transformPara);
    }

    @Override // xiaoying.engine.base.QSession
    public int unInit() {
        return nativeDestroy(this);
    }
}
