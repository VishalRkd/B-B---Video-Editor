package xiaoying.engine.player;

import xiaoying.engine.QEngine;
import xiaoying.engine.aecomp.QAEBaseComp;
import xiaoying.engine.aecomp.QAECompStream;
import xiaoying.engine.base.IQAsyncTagListener;
import xiaoying.engine.base.IQSessionStateListener;
import xiaoying.engine.base.QDisplayContext;
import xiaoying.engine.base.QSession;
import xiaoying.engine.base.QSessionState;
import xiaoying.engine.base.QSessionStream;
import xiaoying.engine.base.QTransformInfo;
import xiaoying.engine.base.QVEError;
import xiaoying.engine.clip.QClip;
import xiaoying.engine.clip.QEffect;
import xiaoying.utils.QBitmap;
import xiaoying.utils.QBitmapFactory;
import xiaoying.utils.QColorSpace;
import xiaoying.utils.QSize;

/* JADX INFO: loaded from: classes19.dex */
public class QPlayer extends QSession {
    public static final int OP_NONE = 0;
    public static final int OP_REFRESH_AUDIO = 1;
    public static final int OP_REFRESH_AUDIO_EX = 2;
    public static final int PLAYBACKMODE_FF2X = 1;
    public static final int PLAYBACKMODE_FF4X = 2;
    public static final int PLAYBACKMODE_I_FRAME = 5;
    public static final int PLAYBACKMODE_NORMAL = 0;
    public static final int PLAYBACKMODE_SF2X = 3;
    public static final int PLAYBACKMODE_SF4X = 4;
    public static final int PLAYER_LAST_POSITION = -1;
    public static final int PLAYER_SEEK_TYPE_NEXT = 1;
    public static final int PLAYER_SEEK_TYPE_PRE = 0;
    public static final int PROP_PLAYER_ASYNC_TAG_BEGIN = 32780;
    public static final int PROP_PLAYER_ASYNC_TAG_END = 32783;
    public static final int PROP_PLAYER_BASE = 32768;
    public static final int PROP_PLAYER_CALLBACK_DELTA = 32775;
    public static final int PROP_PLAYER_DISPLAY_TRANSFORM = 32777;
    public static final int PROP_PLAYER_PREVIEW_FPS = 32772;
    public static final int PROP_PLAYER_RANGE = 32769;
    public static final int PROP_PLAYER_SEEK_DIR = 32770;
    public static final int PROP_PLAYER_STREAM_DURATION = 32776;
    public static final int PROP_PLAYER_STREAM_FRAME_SIZE = 32774;
    public static final int PROP_PLAYER_TIME_SCALE = 32784;
    public static final int PROP_PLAYER_USE_ASYNC_PLAYER = 32779;
    public static final int PROP_PLAYER_VIDEO_PLAYED_TIMESTAMP = 32773;
    public static final int REFRESH_STREAM_OPCODE_ADD_EFFECT = 1;
    public static final int REFRESH_STREAM_OPCODE_AUTO = 13;
    public static final int REFRESH_STREAM_OPCODE_PLAYER_TRANFROM = 12;
    public static final int REFRESH_STREAM_OPCODE_REMOVE_EFFECT = 3;
    public static final int REFRESH_STREAM_OPCODE_REOPEN = 11;
    public static final int REFRESH_STREAM_OPCODE_UPDATE_ALL_CLIP_ALL_EFFECT = 5;
    public static final int REFRESH_STREAM_OPCODE_UPDATE_ALL_EFFECT = 6;
    public static final int REFRESH_STREAM_OPCODE_UPDATE_ALL_MUSIC = 7;
    public static final int REFRESH_STREAM_OPCODE_UPDATE_ALL_TRANSITION = 9;
    public static final int REFRESH_STREAM_OPCODE_UPDATE_EFFECT = 2;
    public static final int REFRESH_STREAM_OPCODE_UPDATE_PANZOOM = 4;
    public static final int REFRESH_STREAM_OPCODE_UPDATE_TIME_SCALE = 10;
    public static final int REFRESH_STREAM_OPCODE_UPDATE_TRANSITION = 8;
    public static final int TRACK_TYPE_AUDIO = 1;
    public static final int TRACK_TYPE_VIDEO = 0;
    private long mGlobalSHRef = 0;
    protected IQAsyncTagListener mAsyncTagListener = null;

    private native int nativeActiveStream(long handle, QAECompStream stream, int position, boolean syncseek);

    private native int nativeActiveStream(long handle, QSessionStream stream, int position, boolean syncseek);

    private native int nativeAudioRestart(long handle);

    private native int nativeCacheScreenFrame(long handle);

    private native int nativeClose(long handle);

    private native int nativeCreate(QEngine engine, QPlayer player);

    private native int nativeDeActiveStream(long handle);

    private native int nativeDestroy(QPlayer player);

    private native int nativeDisableDisplay(long handle, boolean disable);

    private native int nativeDisableTrack(long handle, int trackType, boolean disable);

    private native int nativeDisplayRefresh(long handle);

    private native QTransformInfo nativeGetCurClip3DTransform(long handle, QClip clip, QEffect effect);

    private native int nativeGetCurClipCropFrame(long handle, QClip clip, QBitmap bitmap);

    private native int nativeGetCurClipOriFrame(long handle, QClip e10, QBitmap bitmap);

    private native QSize nativeGetCurClipSize(long handle, QClip clip);

    private native QSize nativeGetCurClipSizeEx(long handle, QClip clip, int timeout);

    private native int nativeGetCurCompFrame(long handle, QAEBaseComp comp, int position, QBitmap bitmap);

    private native int nativeGetCurEffectFrame(long handle, QEffect e10, int position, QBitmap bitmap);

    private native QSize nativeGetCurEffectSize(long handle, QEffect effect);

    private native int nativeGetCurFrame(long handle, QBitmap bitmap);

    private native int nativeGetCurScreenFrame(long handle, QBitmap bitmap);

    private native int nativeGetCurStoryboardMediaTime(long handle);

    private native int nativeGetDisplayContext(long handle, QDisplayContext displayContext);

    private native int nativeGetVolume(long handle);

    private native int nativeLockStuffUnderEffect(long handle, QEffect e10);

    private native int nativePause(long handle);

    private native int nativePerformOperation(long handle, int opType, Object opParam);

    private native int nativePlay(long handle);

    private native int nativeRefreshStream(long handle, QClip clip, int opCode, QEffect effect);

    private native int nativeSeekTo(long handle, int position);

    private native int nativeSetDisplayContext(QPlayer player, QDisplayContext displayContext);

    private native int nativeSetMode(long handle, int mode);

    private native int nativeSetVolume(long handle, int volume);

    private native int nativeStop(long handle);

    private native int nativeSyncSeekTo(long handle, int position);

    private native int nativeUnlockStuffUnderEffect(long handle, QEffect e10);

    private int onAsyncTagCallback(QSessionState state) {
        IQAsyncTagListener iQAsyncTagListener = this.mAsyncTagListener;
        if (iQAsyncTagListener == null) {
            return 0;
        }
        return iQAsyncTagListener.onAsyncTagCallback(state);
    }

    public int activeStream(QSessionStream stream, int position, boolean syncseek) {
        long j10 = this.handle;
        return 0 == j10 ? QVEError.QERR_COMMON_JAVA_NOT_INIT : nativeActiveStream(j10, stream, position, syncseek);
    }

    public int audioRestart() {
        long j10 = this.handle;
        return 0 == j10 ? QVEError.QERR_COMMON_JAVA_NOT_INIT : nativeAudioRestart(j10);
    }

    public int autoRefreshStream() {
        long j10 = this.handle;
        return 0 == j10 ? QVEError.QERR_COMMON_JAVA_NOT_INIT : nativeRefreshStream(j10, null, 13, null);
    }

    public int cacheScreenFrame() {
        long j10 = this.handle;
        if (0 == j10) {
            return -1;
        }
        return nativeCacheScreenFrame(j10);
    }

    public int close() {
        long j10 = this.handle;
        if (0 == j10) {
            return 0;
        }
        return nativeClose(j10);
    }

    public int deactiveStream() {
        long j10 = this.handle;
        return 0 == j10 ? QVEError.QERR_COMMON_JAVA_NOT_INIT : nativeDeActiveStream(j10);
    }

    public int disableDisplay(boolean disable) {
        long j10 = this.handle;
        return 0 == j10 ? QVEError.QERR_COMMON_JAVA_NOT_INIT : nativeDisableDisplay(j10, disable);
    }

    public int disableTrack(int tracktype, boolean disable) {
        long j10 = this.handle;
        return 0 == j10 ? QVEError.QERR_COMMON_JAVA_NOT_INIT : nativeDisableTrack(j10, tracktype, disable);
    }

    public int displayRefresh() {
        long j10 = this.handle;
        return 0 == j10 ? QVEError.QERR_COMMON_JAVA_NOT_INIT : nativeDisplayRefresh(j10);
    }

    public QTransformInfo getCurClip3DTransform(QClip qClip, QEffect qEffect) {
        long j10 = this.handle;
        if (0 == j10 || qClip == null || qEffect == null) {
            return null;
        }
        return nativeGetCurClip3DTransform(j10, qClip, qEffect);
    }

    public int getCurClipCropFrame(QClip qClip, QBitmap bitmap) {
        long j10 = this.handle;
        return (0 == j10 || qClip == null || bitmap == null) ? QVEError.QERR_COMMON_JAVA_NOT_INIT : nativeGetCurClipCropFrame(j10, qClip, bitmap);
    }

    public int getCurClipOriFrame(QClip qClip, QBitmap bitmap) {
        long j10 = this.handle;
        return (0 == j10 || qClip == null || bitmap == null) ? QVEError.QERR_COMMON_JAVA_NOT_INIT : nativeGetCurClipOriFrame(j10, qClip, bitmap);
    }

    public QSize getCurClipSize(QClip qClip) {
        long j10 = this.handle;
        if (0 == j10 || qClip == null) {
            return null;
        }
        return nativeGetCurClipSize(j10, qClip);
    }

    public QSize getCurClipSizeEx(QClip qClip, int timeout) {
        long j10 = this.handle;
        if (0 == j10 || qClip == null) {
            return null;
        }
        return nativeGetCurClipSizeEx(j10, qClip, timeout);
    }

    public int getCurCompFrame(QAEBaseComp comp, int position, QBitmap bitmap) {
        long j10 = this.handle;
        return (0 == j10 || comp == null || bitmap == null) ? QVEError.QERR_COMMON_JAVA_NOT_INIT : nativeGetCurCompFrame(j10, comp, position, bitmap);
    }

    public int getCurEffectFrame(QEffect qEffect, int position, QBitmap bitmap) {
        long j10 = this.handle;
        return (0 == j10 || qEffect == null || bitmap == null) ? QVEError.QERR_COMMON_JAVA_NOT_INIT : nativeGetCurEffectFrame(j10, qEffect, position, bitmap);
    }

    public QSize getCurEffectSize(QEffect qEffect) {
        long j10 = this.handle;
        if (0 == j10 || qEffect == null) {
            return null;
        }
        return nativeGetCurEffectSize(j10, qEffect);
    }

    public QBitmap getCurFrame(int width, int height, int colorSpace) {
        QBitmap qBitmapCreateQBitmapBlank;
        if (0 == this.handle || (qBitmapCreateQBitmapBlank = QBitmapFactory.createQBitmapBlank(width, height, colorSpace)) == null) {
            return null;
        }
        if (nativeGetCurFrame(this.handle, qBitmapCreateQBitmapBlank) == 0) {
            return qBitmapCreateQBitmapBlank;
        }
        qBitmapCreateQBitmapBlank.recycle();
        return null;
    }

    public QBitmap getCurScreenFrame() {
        QDisplayContext displayContext;
        QBitmap qBitmapCreateQBitmapBlank;
        if (0 == this.handle || (displayContext = getDisplayContext()) == null || (qBitmapCreateQBitmapBlank = QBitmapFactory.createQBitmapBlank(displayContext.getScreenRect().right - displayContext.getScreenRect().left, displayContext.getScreenRect().bottom - displayContext.getScreenRect().top, QColorSpace.QPAF_RGB32_A8R8G8B8)) == null) {
            return null;
        }
        if (nativeGetCurScreenFrame(this.handle, qBitmapCreateQBitmapBlank) == 0) {
            return qBitmapCreateQBitmapBlank;
        }
        qBitmapCreateQBitmapBlank.recycle();
        return null;
    }

    public int getCurStoryboardMediaTime() {
        long j10 = this.handle;
        if (0 == j10) {
            return -1;
        }
        return nativeGetCurStoryboardMediaTime(j10);
    }

    public QDisplayContext getDisplayContext() {
        if (0 == this.handle) {
            return null;
        }
        QDisplayContext qDisplayContext = new QDisplayContext();
        if (nativeGetDisplayContext(this.handle, qDisplayContext) != 0) {
            return null;
        }
        return qDisplayContext;
    }

    public int getVolume() {
        return nativeGetVolume(this.handle);
    }

    @Override // xiaoying.engine.base.QSession
    public int init(QEngine engine, IQSessionStateListener listener) {
        super.init(engine, listener);
        return nativeCreate(engine, this);
    }

    public int lockStuffUnderEffect(QEffect e10) {
        return nativeLockStuffUnderEffect(this.handle, e10);
    }

    public int pause() {
        long j10 = this.handle;
        return 0 == j10 ? QVEError.QERR_COMMON_JAVA_NOT_INIT : nativePause(j10);
    }

    public int performOperation(int opType, Object opParam) {
        return nativePerformOperation(this.handle, opType, opParam);
    }

    public int play() {
        long j10 = this.handle;
        return 0 == j10 ? QVEError.QERR_COMMON_JAVA_NOT_INIT : nativePlay(j10);
    }

    public int refreshStream(QClip clip, int opCode, QEffect effect) {
        long j10 = this.handle;
        return 0 == j10 ? QVEError.QERR_COMMON_JAVA_NOT_INIT : nativeRefreshStream(j10, clip, opCode, effect);
    }

    public int seekTo(int position) {
        long j10 = this.handle;
        return 0 == j10 ? QVEError.QERR_COMMON_JAVA_NOT_INIT : nativeSeekTo(j10, position);
    }

    public int setDisplayContext(QDisplayContext displayContext) {
        return nativeSetDisplayContext(this, displayContext);
    }

    public int setMode(int mode) {
        long j10 = this.handle;
        return 0 == j10 ? QVEError.QERR_COMMON_JAVA_NOT_INIT : nativeSetMode(j10, mode);
    }

    public int setVolume(int volume) {
        long j10 = this.handle;
        return 0 == j10 ? QVEError.QERR_COMMON_JAVA_NOT_INIT : nativeSetVolume(j10, volume);
    }

    public int stop() {
        long j10 = this.handle;
        return 0 == j10 ? QVEError.QERR_COMMON_JAVA_NOT_INIT : nativeStop(j10);
    }

    public int syncSeekTo(int position) {
        long j10 = this.handle;
        return 0 == j10 ? QVEError.QERR_COMMON_JAVA_NOT_INIT : nativeSyncSeekTo(j10, position);
    }

    @Override // xiaoying.engine.base.QSession
    public int unInit() {
        return 0 == this.handle ? QVEError.QERR_COMMON_JAVA_NOT_INIT : nativeDestroy(this);
    }

    public int unlockStuffUnderEffect(QEffect e10) {
        return nativeUnlockStuffUnderEffect(this.handle, e10);
    }

    public int activeStream(QAECompStream stream, int position, boolean syncseek) {
        long j10 = this.handle;
        return 0 == j10 ? QVEError.QERR_COMMON_JAVA_NOT_INIT : nativeActiveStream(j10, stream, position, syncseek);
    }
}
