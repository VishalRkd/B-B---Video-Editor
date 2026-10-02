package xiaoying.platform;

import android.media.AudioTrack;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/* JADX INFO: loaded from: classes19.dex */
public final class QAudioOut extends QAudioBase {
    private static final int MAX_WAIT_TIME = 500;
    private long mAudioCB = 0;
    private long mUserData = 0;
    private AudioTrack mAudioTrack = null;
    private byte[] mPCMBuffer = null;
    private int mBufSize = 0;
    private int mVolume = 100;
    private int mBytesInSecond = 0;
    private int mRemainOutBytes = 0;
    private int mOutSeconds = 0;
    private Object mObjOutSync = new Object();
    private volatile boolean mbExit = false;
    private Thread mTask = null;
    private boolean mbReqPause = false;
    private int mState = 0;
    private int mAudioFitStep = 0;
    private final int AUDIO_FIT_STEPS = 1000;
    private boolean mbAudioVolumeUp = true;
    private LinkedBlockingQueue<Long> mResponseQueue = new LinkedBlockingQueue<>();
    private LinkedBlockingQueue<Long> mRequestQueue = new LinkedBlockingQueue<>();
    private LinkedBlockingQueue<Long> mExitQueue = new LinkedBlockingQueue<>();

    public class ProcessTask extends Thread {
        public ProcessTask() {
        }

        @Override // java.lang.Thread, java.lang.Runnable
        public void run() {
            LinkedBlockingQueue linkedBlockingQueue = null;
            while (!QAudioOut.this.mbExit) {
                int i10 = 0;
                if (QAudioOut.this.mbReqPause) {
                    try {
                        QAudioOut.this.mRequestQueue.take();
                        QAudioOut.this.mAudioFitStep = 0;
                        QAudioOut.this.mbReqPause = false;
                        try {
                            linkedBlockingQueue = QAudioOut.this.mResponseQueue;
                        } catch (Exception unused) {
                        }
                    } catch (InterruptedException unused2) {
                        QAudioOut.this.mAudioFitStep = 0;
                        QAudioOut.this.mbReqPause = false;
                        linkedBlockingQueue = QAudioOut.this.mResponseQueue;
                    } catch (Throwable th2) {
                        QAudioOut.this.mAudioFitStep = 0;
                        QAudioOut.this.mbReqPause = false;
                        try {
                            QAudioOut.this.mResponseQueue.add(0L);
                        } catch (Exception unused3) {
                        }
                        throw th2;
                    }
                    linkedBlockingQueue.add(0L);
                } else {
                    QAudioOut qAudioOut = QAudioOut.this;
                    long j10 = qAudioOut.mAudioCB;
                    long j11 = QAudioOut.this.mUserData;
                    QAudioOut qAudioOut2 = QAudioOut.this;
                    int iNativeAudioOutCallback = qAudioOut.nativeAudioOutCallback(j10, j11, qAudioOut2.mCurrentStatus, qAudioOut2.mPCMBuffer, QAudioOut.this.mBufSize);
                    if (iNativeAudioOutCallback <= 0) {
                        try {
                            Thread.sleep(10L);
                        } catch (InterruptedException e10) {
                            e10.printStackTrace();
                        }
                    } else {
                        QAudioOut.this.controlVolumeFit(iNativeAudioOutCallback);
                        while (i10 < iNativeAudioOutCallback && !QAudioOut.this.mbExit) {
                            int iWrite = QAudioOut.this.mAudioTrack.write(QAudioOut.this.mPCMBuffer, i10, iNativeAudioOutCallback - i10);
                            if (iWrite <= 0) {
                                break;
                            } else {
                                i10 += iWrite;
                            }
                        }
                        synchronized (QAudioOut.this.mObjOutSync) {
                            try {
                                QAudioOut.access$1312(QAudioOut.this, i10);
                                if (QAudioOut.this.mRemainOutBytes >= QAudioOut.this.mBytesInSecond) {
                                    int i11 = QAudioOut.this.mRemainOutBytes / QAudioOut.this.mBytesInSecond;
                                    QAudioOut.access$1512(QAudioOut.this, i11);
                                    QAudioOut.this.mRemainOutBytes -= QAudioOut.this.mBytesInSecond * i11;
                                }
                            } catch (Throwable th3) {
                                throw th3;
                            }
                        }
                    }
                }
            }
            QAudioOut.this.mExitQueue.add(0L);
        }
    }

    public static /* synthetic */ int access$1312(QAudioOut qAudioOut, int i10) {
        int i11 = qAudioOut.mRemainOutBytes + i10;
        qAudioOut.mRemainOutBytes = i11;
        return i11;
    }

    public static /* synthetic */ int access$1512(QAudioOut qAudioOut, int i10) {
        int i11 = qAudioOut.mOutSeconds + i10;
        qAudioOut.mOutSeconds = i11;
        return i11;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public boolean controlVolumeFit(int nAudioRenderBytes) {
        int i10 = this.mAudioFitStep;
        if (i10 >= 1000) {
            return true;
        }
        int i11 = i10 + ((nAudioRenderBytes * 1000) / this.mBytesInSecond);
        this.mAudioFitStep = i11;
        if (i11 > 1000) {
            this.mAudioFitStep = 1000;
        }
        int i12 = (this.mAudioFitStep * 100) / 1000;
        int i13 = this.mVolume;
        int i14 = (i12 * i13) / 100;
        if (!this.mbAudioVolumeUp) {
            i14 = i13 - i14;
        }
        setVolumeInternal(i14);
        return false;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public native int nativeAudioOutCallback(long fnAudioCallback, long userData, int nStatus, byte[] pcm, int nSize);

    public static int querySupportType(int nQueryType) {
        if (nQueryType == 0) {
            return 2;
        }
        if (nQueryType == 1) {
            return 3;
        }
        if (nQueryType != 2) {
            return nQueryType != 3 ? 0 : 2;
        }
        return 1023;
    }

    private void resume() {
        if (this.mTask == null) {
            return;
        }
        this.mbReqPause = false;
        this.mRequestQueue.add(0L);
        try {
            this.mResponseQueue.take();
        } catch (Exception unused) {
        }
        this.mResponseQueue.clear();
        this.mRequestQueue.clear();
        this.mCurrentStatus = 1;
    }

    private void setVolumeInternal(int lVolume) {
        if (this.mAudioTrack == null) {
            return;
        }
        try {
            float minVolume = AudioTrack.getMinVolume();
            float maxVolume = (((AudioTrack.getMaxVolume() - minVolume) * lVolume) / 100.0f) + minVolume;
            this.mAudioTrack.setStereoVolume(maxVolume, maxVolume);
        } catch (Exception unused) {
        }
    }

    public int GetPosition() {
        AudioTrack audioTrack = this.mAudioTrack;
        if (audioTrack == null) {
            return -1;
        }
        try {
            return audioTrack.getPlaybackHeadPosition();
        } catch (Throwable unused) {
            return -1;
        }
    }

    public int GetVolume() {
        if (this.mAudioTrack == null) {
            return 0;
        }
        return this.mVolume;
    }

    @Override // xiaoying.platform.QAudioBase
    public synchronized int Init(int nFormat, int nChannel, int nBitsPerSample, int nSamplingRate, int nBufLen, long fnAudioCallback, long userData) {
        if (fnAudioCallback == 0) {
            return -1;
        }
        try {
            this.mAudioCB = fnAudioCallback;
            this.mUserData = userData;
            int iConvertSampleRate = convertSampleRate(nSamplingRate);
            int i10 = nChannel == 1 ? 4 : 12;
            int iConvertBitPerSample = convertBitPerSample(nBitsPerSample);
            int i11 = nBufLen < 16 ? 16 : nBufLen;
            this.mBytesInSecond = iConvertSampleRate;
            if (i10 == 12) {
                this.mBytesInSecond = iConvertSampleRate * 2;
            }
            if (iConvertBitPerSample == 2) {
                this.mBytesInSecond *= 2;
            }
            this.mAudioTrack = new AudioTrack(3, iConvertSampleRate, i10, iConvertBitPerSample, i11 * 2, 1);
            this.mPCMBuffer = new byte[i11];
            this.mBufSize = i11;
            this.mbReqPause = false;
            this.mbExit = false;
            this.mResponseQueue.clear();
            this.mRequestQueue.clear();
            this.mCurrentStatus = 0;
            return 0;
        } catch (Throwable th2) {
            throw th2;
        }
    }

    @Override // xiaoying.platform.QAudioBase
    public synchronized int Pause() {
        int i10 = this.mCurrentStatus;
        if (i10 == 3) {
            return 0;
        }
        if (i10 != 1) {
            return -1;
        }
        setVolumeInternal(0);
        this.mRequestQueue.clear();
        this.mbReqPause = true;
        while (this.mTask.getState() != Thread.State.WAITING) {
            try {
                Thread.sleep(20L);
            } catch (InterruptedException e10) {
                e10.printStackTrace();
            }
        }
        this.mCurrentStatus = 3;
        return 0;
    }

    public int SetVolume(int lVolume) {
        setVolumeInternal(lVolume);
        this.mVolume = lVolume;
        return 0;
    }

    @Override // xiaoying.platform.QAudioBase
    public synchronized int Start() {
        if (this.mAudioTrack == null) {
            return -1;
        }
        if (this.mCurrentStatus == 1) {
            return 0;
        }
        this.mbAudioVolumeUp = true;
        this.mAudioFitStep = 0;
        controlVolumeFit(0);
        if (this.mCurrentStatus == 3) {
            resume();
            return 0;
        }
        try {
            this.mbExit = false;
            for (int i10 = 0; i10 < 2; i10++) {
                int iNativeAudioOutCallback = nativeAudioOutCallback(this.mAudioCB, this.mUserData, 1, this.mPCMBuffer, this.mBufSize);
                if (iNativeAudioOutCallback > 0) {
                    int i11 = 0;
                    while (i11 < iNativeAudioOutCallback) {
                        int iWrite = this.mAudioTrack.write(this.mPCMBuffer, i11, iNativeAudioOutCallback - i11);
                        if (iWrite <= 0) {
                            break;
                        }
                        i11 += iWrite;
                    }
                    synchronized (this.mObjOutSync) {
                        try {
                            int i12 = this.mRemainOutBytes + i11;
                            this.mRemainOutBytes = i12;
                            int i13 = this.mBytesInSecond;
                            if (i12 >= i13) {
                                int i14 = i12 / i13;
                                this.mOutSeconds += i14;
                                this.mRemainOutBytes = i12 - (i13 * i14);
                            }
                        } catch (Throwable th2) {
                            throw th2;
                        }
                    }
                }
            }
            this.mAudioTrack.play();
            this.mTask = new ProcessTask();
            this.mResponseQueue.clear();
            this.mRequestQueue.clear();
            this.mExitQueue.clear();
            this.mTask.start();
            this.mCurrentStatus = 1;
            return 0;
        } catch (IllegalStateException e10) {
            e10.printStackTrace();
            return -1;
        }
    }

    @Override // xiaoying.platform.QAudioBase
    public synchronized int Stop() {
        try {
            if (this.mAudioTrack == null) {
                return -1;
            }
            if (this.mCurrentStatus == 4) {
                return 0;
            }
            this.mbExit = true;
            try {
                if (this.mCurrentStatus == 3) {
                    resume();
                }
                this.mExitQueue.poll(200L, TimeUnit.MILLISECONDS);
            } catch (Exception e10) {
                StringBuilder sb2 = new StringBuilder();
                sb2.append("Stop join ");
                sb2.append(e10.toString());
            }
            try {
                Thread thread = this.mTask;
                if (thread != null) {
                    thread.interrupt();
                    this.mTask = null;
                }
            } catch (Throwable unused) {
            }
            try {
                this.mAudioTrack.flush();
                this.mAudioTrack.stop();
            } catch (Exception unused2) {
            }
            this.mCurrentStatus = 4;
            return 0;
        } catch (Throwable th2) {
            throw th2;
        }
    }

    @Override // xiaoying.platform.QAudioBase
    public synchronized int Uninit() {
        if (this.mAudioTrack == null) {
            return -1;
        }
        Stop();
        try {
            this.mAudioTrack.release();
        } catch (Exception unused) {
        }
        this.mAudioTrack = null;
        this.mTask = null;
        return 0;
    }
}
