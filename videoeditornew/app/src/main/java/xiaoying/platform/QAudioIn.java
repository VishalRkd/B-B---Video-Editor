package xiaoying.platform;

import android.media.AudioRecord;

/* JADX INFO: loaded from: classes19.dex */
public final class QAudioIn extends QAudioBase {
    public static final int EN_IMMEDIATE_RELEASE = 2;
    public static final int EN_IMMEDIATE_STOP = 1;
    private static final int MAX_AUDIO_IN_INTERVAL_TIME = 100;
    private static final int MIN_AUDIO_IN_INTERVAL_TIME = 20;
    private static int mAudioRecFlag = 3;
    private static MyAudioRecorder mMyAudioRecorder;
    private long mAudioCB = 0;
    private long mUserData = 0;
    private int mCBBufSize = 0;
    private volatile boolean mbExit = false;
    private ProcessTask mFetchPCMPTask = null;
    private int mAudioEQ = 0;
    private int mSpectrumVolume = 0;
    private int mMinReadSize = 0;

    public interface BufferFillDoneListener {
        void onBufferDone(byte[] buffer);
    }

    public static class ProcessTask extends Thread {
        private final AudioRecord mAudioRecord;
        private final int mCBBufSize;
        private final int mMinReadSize;
        private boolean mbExit;
        private SpectrumVolumeListener mSpectrumVolumeListener = null;
        private BufferFillDoneListener mBufferFillDoneListener = null;
        private int mDiscardCount = 0;

        public ProcessTask(int nCBBufSize, int nMinReadSize, AudioRecord audioRecord) {
            this.mCBBufSize = nCBBufSize;
            this.mMinReadSize = nMinReadSize;
            this.mAudioRecord = audioRecord;
        }

        public void exit(boolean bSyncWait) {
            this.mbExit = true;
            if (bSyncWait) {
                try {
                    join();
                } catch (InterruptedException e10) {
                    e10.printStackTrace();
                }
            }
        }

        @Override // java.lang.Thread, java.lang.Runnable
        public void run() {
            int i10 = this.mCBBufSize;
            int i11 = this.mMinReadSize;
            byte[] bArr = new byte[i10];
            while (true) {
                int i12 = 0;
                while (!this.mbExit) {
                    int iMin = Math.min(i10 - i12, i11);
                    int i13 = this.mDiscardCount;
                    if (i13 > 0) {
                        iMin = Math.min(iMin, i13);
                    }
                    if (iMin != 0) {
                        int i14 = this.mAudioRecord.read(bArr, i12, iMin);
                        if (i14 <= 0) {
                            return;
                        }
                        int i15 = this.mDiscardCount;
                        if (i15 > 0) {
                            this.mDiscardCount = i15 - i14;
                        } else {
                            i12 += i14;
                        }
                    }
                    if (i12 == i10) {
                        SpectrumVolumeListener spectrumVolumeListener = this.mSpectrumVolumeListener;
                        if (spectrumVolumeListener != null) {
                            spectrumVolumeListener.onSpectrumVolume(bArr);
                        }
                        BufferFillDoneListener bufferFillDoneListener = this.mBufferFillDoneListener;
                        if (bufferFillDoneListener != null) {
                            bufferFillDoneListener.onBufferDone(bArr);
                        }
                    }
                }
                return;
            }
        }

        public void setBufferFillDoneListener(BufferFillDoneListener listener) {
            this.mBufferFillDoneListener = listener;
        }

        public void setDiscardCount(int lDiscardCount) {
            if (this.mDiscardCount > 0) {
                this.mDiscardCount = lDiscardCount;
            }
        }

        public void setSpectrumVolumeListener(SpectrumVolumeListener listener) {
            this.mSpectrumVolumeListener = listener;
        }
    }

    public interface SpectrumVolumeListener {
        void onSpectrumVolume(byte[] buffer);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public native int nativeAudioInCallback(long fnAudioCallback, long userData, int nStatus, byte[] pcm, int nSize);

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

    public static synchronized void release() {
        try {
            MyAudioRecorder myAudioRecorder = mMyAudioRecorder;
            if (myAudioRecorder != null) {
                myAudioRecorder.unInit(true);
                mMyAudioRecorder = null;
            }
        } catch (Throwable th2) {
            th2.printStackTrace();
        }
        setRecFlag(3);
    }

    public static synchronized void setRecFlag(int nRecFlag) {
        mAudioRecFlag = nRecFlag;
    }

    @Override // xiaoying.platform.QAudioBase
    public int GetConfig(int nConfigType, int nValue, int nSize) {
        if (nConfigType == 10) {
            return this.mSpectrumVolume;
        }
        return 0;
    }

    @Override // xiaoying.platform.QAudioBase
    public int Init(int nFormat, int nChannel, int nBitsPerSample, int nSamplingRate, int nBufLen, long fnAudioCallback, long userData) {
        System.currentTimeMillis();
        this.mAudioCB = fnAudioCallback;
        this.mUserData = userData;
        this.mCBBufSize = nBufLen;
        int iConvertSampleRate = convertSampleRate(nSamplingRate);
        int i10 = nChannel == 1 ? 16 : 12;
        int minBufferSize = AudioRecord.getMinBufferSize(iConvertSampleRate, i10, convertBitPerSample(nBitsPerSample));
        if (minBufferSize < 0) {
            return minBufferSize;
        }
        if (nBufLen < minBufferSize) {
            nBufLen = minBufferSize;
        }
        if (mMyAudioRecorder == null) {
            mMyAudioRecorder = new MyAudioRecorder();
        }
        if (!mMyAudioRecorder.init(iConvertSampleRate, i10, mAudioRecFlag)) {
            mMyAudioRecorder = null;
            return -1;
        }
        int i11 = ((((nChannel * nBitsPerSample) * nSamplingRate) / 8) * 20) / 1000;
        this.mMinReadSize = i11;
        if (i11 < minBufferSize) {
            this.mMinReadSize = minBufferSize;
        }
        if (this.mMinReadSize > nBufLen) {
            this.mMinReadSize = nBufLen;
        }
        this.mCurrentStatus = 0;
        return 0;
    }

    @Override // xiaoying.platform.QAudioBase
    public int Pause() {
        return Stop();
    }

    @Override // xiaoying.platform.QAudioBase
    public int SetConfig(int nConfigType, int nValue, int nSize) {
        if (nConfigType != 9) {
            return 0;
        }
        this.mAudioEQ = nValue;
        return 0;
    }

    @Override // xiaoying.platform.QAudioBase
    public int Start() {
        if (mMyAudioRecorder == null) {
            return -1;
        }
        long jCurrentTimeMillis = System.currentTimeMillis();
        try {
            this.mbExit = false;
            mMyAudioRecorder.start();
            this.mCurrentStatus = 2;
            ProcessTask processTask = new ProcessTask(this.mCBBufSize, this.mMinReadSize, mMyAudioRecorder.getAudioRecord());
            this.mFetchPCMPTask = processTask;
            processTask.setSpectrumVolumeListener(new SpectrumVolumeListener() { // from class: xiaoying.platform.QAudioIn.1
                @Override // xiaoying.platform.QAudioIn.SpectrumVolumeListener
                public void onSpectrumVolume(byte[] buffer) {
                    if (QAudioIn.this.mAudioEQ != 0) {
                        int length = buffer.length >> 1;
                        short s10 = 0;
                        for (int i10 = 0; i10 < length; i10 += 4) {
                            int i11 = i10 * 2;
                            short s11 = (short) (buffer[i11] + (buffer[i11 + 1] << 8));
                            if (s11 < 0) {
                                s11 = (short) (-s11);
                            }
                            if (s10 < s11) {
                                s10 = s11;
                            }
                        }
                        QAudioIn.this.mSpectrumVolume = (((s10 * 100) >> 15) + QAudioIn.this.mSpectrumVolume) >> 1;
                    }
                }
            });
            this.mFetchPCMPTask.setBufferFillDoneListener(new BufferFillDoneListener() { // from class: xiaoying.platform.QAudioIn.2
                @Override // xiaoying.platform.QAudioIn.BufferFillDoneListener
                public void onBufferDone(byte[] buffer) {
                    if (QAudioIn.this.mAudioCB != 0) {
                        QAudioIn qAudioIn = QAudioIn.this;
                        qAudioIn.nativeAudioInCallback(qAudioIn.mAudioCB, QAudioIn.this.mUserData, QAudioIn.this.mCurrentStatus, buffer, buffer.length);
                    }
                }
            });
            this.mFetchPCMPTask.setDiscardCount(MyAudioRecorder.getRecordBufSize(mMyAudioRecorder.mAudioSampleRate, mMyAudioRecorder.mAudioChannel, System.currentTimeMillis() - jCurrentTimeMillis));
            this.mFetchPCMPTask.start();
            return 0;
        } catch (IllegalStateException e10) {
            e10.printStackTrace();
            return -1;
        }
    }

    @Override // xiaoying.platform.QAudioBase
    public int Stop() {
        ProcessTask processTask;
        try {
            if (mMyAudioRecorder != null && (processTask = this.mFetchPCMPTask) != null) {
                processTask.exit(true);
                this.mCurrentStatus = 4;
                mMyAudioRecorder.stop();
                this.mFetchPCMPTask = null;
                return 0;
            }
            return -1;
        } catch (Throwable th2) {
            th2.printStackTrace();
            return 0;
        }
    }

    @Override // xiaoying.platform.QAudioBase
    public int Uninit() {
        if (mMyAudioRecorder == null) {
            return -1;
        }
        System.currentTimeMillis();
        Stop();
        mMyAudioRecorder.unInit();
        this.mCurrentStatus = 5;
        this.mFetchPCMPTask = null;
        return 0;
    }

    public static class MyAudioRecorder {
        private int mAudioChannel;
        private AudioRecord mAudioRecord;
        private int mAudioSampleRate;
        private int mBufLen;
        private ProcessTask mInnerTask;
        private int mRecFlag;

        private MyAudioRecorder() {
            this.mAudioRecord = null;
            this.mAudioSampleRate = 0;
            this.mAudioChannel = 0;
            this.mBufLen = 0;
            this.mRecFlag = 0;
            this.mInnerTask = null;
        }

        /* JADX INFO: Access modifiers changed from: private */
        public static int getRecordBufSize(int nAudioSampleRate, int nAudioChannel, long lMillsTime) {
            return (((int) ((((long) ((((nAudioChannel == 1 || nAudioChannel == 2 || !(nAudioChannel == 3 || nAudioChannel == 12)) ? 1 : 2) * nAudioSampleRate) * 2)) * lMillsTime) / 1000)) >> 1) << 1;
        }

        public synchronized AudioRecord getAudioRecord() {
            return this.mAudioRecord;
        }

        public synchronized boolean init(int nAudioSampleRate, int nAudioChannel, int nRecFlag) {
            if (this.mAudioRecord != null && this.mAudioSampleRate == nAudioSampleRate && this.mAudioChannel == nAudioChannel) {
                return true;
            }
            try {
                int minBufferSize = AudioRecord.getMinBufferSize(nAudioSampleRate, nAudioChannel, 2);
                if (minBufferSize < 0) {
                    return false;
                }
                int recordBufSize = getRecordBufSize(nAudioSampleRate, nAudioChannel, 100L);
                if (recordBufSize >= minBufferSize) {
                    minBufferSize = recordBufSize;
                }
                if (this.mAudioRecord != null) {
                    unInit(true);
                }
                int i10 = minBufferSize * 2;
                this.mAudioRecord = new AudioRecord(1, nAudioSampleRate, nAudioChannel, 2, i10);
                this.mAudioSampleRate = nAudioSampleRate;
                this.mAudioChannel = nAudioChannel;
                this.mBufLen = i10;
                this.mRecFlag = nRecFlag;
            } catch (Throwable unused) {
                this.mAudioRecord = null;
            }
            return this.mAudioRecord != null;
        }

        public synchronized void start() {
            try {
                if (this.mAudioRecord != null) {
                    ProcessTask processTask = this.mInnerTask;
                    if (processTask != null) {
                        processTask.exit(true);
                        this.mInnerTask = null;
                    }
                    if (this.mAudioRecord.getRecordingState() != 3) {
                        try {
                            this.mAudioRecord.startRecording();
                        } catch (Exception unused) {
                        }
                    }
                }
            } catch (Throwable th2) {
                throw th2;
            }
        }

        public synchronized void stop() {
            AudioRecord audioRecord = this.mAudioRecord;
            if (audioRecord != null) {
                if ((this.mRecFlag & 1) == 0) {
                    int i10 = this.mAudioSampleRate;
                    if (i10 == 0) {
                        i10 = 16000;
                    }
                    int i11 = this.mAudioChannel;
                    if (i11 == 0) {
                        i11 = 16;
                    }
                    int recordBufSize = getRecordBufSize(i10, i11, 20L);
                    ProcessTask processTask = new ProcessTask(recordBufSize, recordBufSize, this.mAudioRecord);
                    this.mInnerTask = processTask;
                    processTask.start();
                } else if (audioRecord.getRecordingState() == 3) {
                    try {
                        this.mAudioRecord.stop();
                    } catch (Exception e10) {
                        e10.printStackTrace();
                    }
                }
            }
        }

        public synchronized void unInit() {
            unInit((this.mRecFlag & 2) != 0);
        }

        public synchronized void unInit(boolean bForceUninit) {
            try {
                if (this.mAudioRecord != null && bForceUninit) {
                    ProcessTask processTask = this.mInnerTask;
                    if (processTask != null) {
                        processTask.exit(true);
                        this.mInnerTask = null;
                    }
                    if (this.mAudioRecord.getRecordingState() == 3) {
                        try {
                            this.mAudioRecord.stop();
                        } catch (Exception e10) {
                            e10.printStackTrace();
                        }
                    }
                    this.mAudioRecord.release();
                    this.mAudioRecord = null;
                }
            } catch (Throwable th2) {
                throw th2;
            }
        }
    }
}
