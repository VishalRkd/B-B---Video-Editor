package xiaoying.platform;

/* JADX INFO: loaded from: classes19.dex */
public abstract class QAudioBase {
    private static final int DEFAULT_AUDIO_SAMPLERATE = 44100;
    public static final int QAUDIO_BITPERSAMPLE_16 = 2;
    public static final int QAUDIO_BITPERSAMPLE_8 = 1;
    public static final int QAUDIO_CHANNEL_MONO = 1;
    public static final int QAUDIO_CHANNEL_STERO = 2;
    public static final int QAUDIO_FORMAT_AMR = 2;
    public static final int QAUDIO_FORMAT_PCM = 1;
    public static final int QAUDIO_FORMAT_QCELP = 4;
    public static final int QAUDIO_QUERY_BITPERSAMPLE = 3;
    public static final int QAUDIO_QUERY_CHANNEL = 1;
    public static final int QAUDIO_QUERY_FORMAT = 0;
    public static final int QAUDIO_QUERY_SAMPLERATE = 2;
    public static final int QAUDIO_SAMPLERATE_11025 = 2;
    public static final int QAUDIO_SAMPLERATE_12000 = 4;
    public static final int QAUDIO_SAMPLERATE_16000 = 8;
    public static final int QAUDIO_SAMPLERATE_22050 = 16;
    public static final int QAUDIO_SAMPLERATE_24000 = 32;
    public static final int QAUDIO_SAMPLERATE_32000 = 64;
    public static final int QAUDIO_SAMPLERATE_36000 = 128;
    public static final int QAUDIO_SAMPLERATE_44100 = 256;
    public static final int QAUDIO_SAMPLERATE_48000 = 512;
    public static final int QAUDIO_SAMPLERATE_8000 = 1;
    protected int mCurrentStatus = 0;

    public static class QAudioStatus {
        public static final int QACLOSED = 5;
        public static final int QADIED = 6;
        public static final int QAOPENED = 0;
        public static final int QAPAUSED = 3;
        public static final int QAPLAYING = 1;
        public static final int QARECORDING = 2;
        public static final int QASTOPPED = 4;
    }

    public int GetConfig(int nConfigType, int nValue, int nSize) {
        return 0;
    }

    public abstract int Init(int nFormat, int nChannel, int nBitsPerSample, int nSamplingRate, int nBufLen, long fnAudioCallback, long userData);

    public abstract int Pause();

    public int SetConfig(int nConfigType, int nValue, int nSize) {
        return 0;
    }

    public abstract int Start();

    public abstract int Stop();

    public abstract int Uninit();

    public int convertBitPerSample(int nQAudioBitPerSample) {
        return (nQAudioBitPerSample == 1 || nQAudioBitPerSample == 8) ? 3 : 2;
    }

    public int convertSampleRate(int nQAudioSampleRate) {
        if (nQAudioSampleRate == 1) {
            return 8000;
        }
        if (nQAudioSampleRate != 2) {
            switch (nQAudioSampleRate) {
                case 4:
                case 12000:
                    return 12000;
                case 8:
                case 16000:
                    return 16000;
                case 16:
                case 22050:
                    return 22050;
                case 32:
                case 24000:
                    return 24000;
                case 64:
                case 8000:
                case 32000:
                    return 8000;
                case 128:
                case 36000:
                    return 36000;
                case 256:
                case DEFAULT_AUDIO_SAMPLERATE /* 44100 */:
                    return DEFAULT_AUDIO_SAMPLERATE;
                case 512:
                case 48000:
                    return 48000;
                case 11025:
                    break;
                default:
                    if (nQAudioSampleRate < 8000) {
                        nQAudioSampleRate = DEFAULT_AUDIO_SAMPLERATE;
                    }
                    return nQAudioSampleRate;
            }
        }
        return 11025;
    }
}
