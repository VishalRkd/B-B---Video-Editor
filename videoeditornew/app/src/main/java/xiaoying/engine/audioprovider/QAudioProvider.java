package xiaoying.engine.audioprovider;

import xiaoying.engine.QEngine;
import xiaoying.engine.base.IQSessionStateListener;
import xiaoying.engine.base.QSession;
import xiaoying.engine.base.QSessionStream;
import xiaoying.engine.base.QVEError;

/* JADX INFO: loaded from: classes19.dex */
public class QAudioProvider extends QSession {
    public static final int PROP_AUDIO_INFO = 36865;
    public static final int PROP_AUDIO_PROVIDE_MODE = 36869;
    public static final int PROP_AUDIO_PROVIDE_MODE_NO_VAD = 0;
    public static final int PROP_AUDIO_PROVIDE_MODE_WITH_VAD = 1;
    public static final int PROP_AUDIO_PROVIDE_SEND_TIME = 36870;
    public static final int PROP_AUDIO_RANGE = 36866;
    private static final int PROP_BASE = 36864;
    protected AudioSourceObserver mAudioObserver = null;

    private native int nativeActiveStream(long handle, QSessionStream stream);

    private native int nativeCancel(long handle);

    private native int nativeCreate(QEngine engine, QAudioProvider provider);

    private native int nativeDeActiveStream(long handle);

    private native int nativeDestroy(QAudioProvider provider);

    private native int nativePause(long handle);

    private native int nativeResume(long handle);

    private native int nativeStart(long handle);

    private native int nativeStop(long handle);

    public int activeStream(QSessionStream stream) {
        long j10 = this.handle;
        return 0 == j10 ? QVEError.QERR_COMMON_JAVA_NOT_INIT : nativeActiveStream(j10, stream);
    }

    public int cancel() {
        long j10 = this.handle;
        return 0 == j10 ? QVEError.QERR_COMMON_JAVA_NOT_INIT : nativeCancel(j10);
    }

    public int deactiveStream() {
        long j10 = this.handle;
        return 0 == j10 ? QVEError.QERR_COMMON_JAVA_NOT_INIT : nativeDeActiveStream(j10);
    }

    @Override // xiaoying.engine.base.QSession
    public int init(QEngine engine, IQSessionStateListener listener) {
        super.init(engine, listener);
        return nativeCreate(engine, this);
    }

    public void onAudioSourcePacket(byte[] buf, int time) {
        AudioSourceObserver audioSourceObserver = this.mAudioObserver;
        if (audioSourceObserver != null) {
            audioSourceObserver.onSourcePacket(buf, time);
        }
    }

    public int pause() {
        long j10 = this.handle;
        return 0 == j10 ? QVEError.QERR_COMMON_JAVA_NOT_INIT : nativePause(j10);
    }

    public int resume() {
        long j10 = this.handle;
        return 0 == j10 ? QVEError.QERR_COMMON_JAVA_NOT_INIT : nativeResume(j10);
    }

    public int setAudioObserver(AudioSourceObserver observer) {
        this.mAudioObserver = observer;
        return 0;
    }

    @Override // xiaoying.engine.base.QSession
    public int setProperty(int propertyID, Object data) {
        return super.setProperty(propertyID, data);
    }

    public int start() {
        long j10 = this.handle;
        return 0 == j10 ? QVEError.QERR_COMMON_JAVA_NOT_INIT : nativeStart(j10);
    }

    public int stop() {
        long j10 = this.handle;
        return 0 == j10 ? QVEError.QERR_COMMON_JAVA_NOT_INIT : nativeStop(j10);
    }

    @Override // xiaoying.engine.base.QSession
    public int unInit() {
        return 0 == this.handle ? QVEError.QERR_COMMON_JAVA_NOT_INIT : nativeDestroy(this);
    }
}
