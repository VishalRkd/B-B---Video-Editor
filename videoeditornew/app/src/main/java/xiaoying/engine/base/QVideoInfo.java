package xiaoying.engine.base;

/* JADX INFO: loaded from: classes19.dex */
public class QVideoInfo {
    public static final int AUDIOFORMAT_AACLC = 4;
    public static final int AUDIOFORMAT_AIFF = 17;
    public static final int AUDIOFORMAT_ALAC = 18;
    public static final int AUDIOFORMAT_AMRNB = 2;
    public static final int AUDIOFORMAT_AMRWB = 7;
    public static final int AUDIOFORMAT_AUTO = 1;
    public static final int AUDIOFORMAT_EVRC = 5;
    public static final int AUDIOFORMAT_FLAC = 16;
    public static final int AUDIOFORMAT_MP3 = 6;
    public static final int AUDIOFORMAT_NONE = 0;
    public static final int AUDIOFORMAT_PCM = 14;
    public static final int AUDIOFORMAT_QCELP = 3;
    public static final int AUDIOFORMAT_UNKNOWN = 1;
    public static final int AUDIOFORMAT_WAV = 15;
    public static final int AUDIOFORMAT_WMA_9LOS = 11;
    public static final int AUDIOFORMAT_WMA_9PRO = 10;
    public static final int AUDIOFORMAT_WMA_V1 = 8;
    public static final int AUDIOFORMAT_WMA_V2 = 9;
    public static final int AUDIO_BITS_PER_SAMPLE = 14;
    public static final int AUDIO_BIT_RATE = 13;
    public static final int AUDIO_BLOCK_ALIGN = 15;
    public static final int AUDIO_CHANNEL = 12;
    public static final int AUDIO_DURATION = 6;
    public static final int AUDIO_FORMAT = 2;
    public static final int AUDIO_SAMPLE_RATE = 11;
    public static final int BIT_RATE = 8;
    public static final int FILEFORMAT_3G2 = 3;
    public static final int FILEFORMAT_3GP = 2;
    public static final int FILEFORMAT_AAC = 6;
    public static final int FILEFORMAT_AIFF = 20;
    public static final int FILEFORMAT_AMR = 5;
    public static final int FILEFORMAT_ASF = 12;
    public static final int FILEFORMAT_AVI = 9;
    public static final int FILEFORMAT_GIF = 18;
    public static final int FILEFORMAT_K3G = 13;
    public static final int FILEFORMAT_M4A = 4;
    public static final int FILEFORMAT_MP3 = 14;
    public static final int FILEFORMAT_MP4 = 1;
    public static final int FILEFORMAT_QCP = 7;
    public static final int FILEFORMAT_RAWVIDEO = 17;
    public static final int FILEFORMAT_SKM = 8;
    public static final int FILEFORMAT_UNKNOWN = 0;
    public static final int FILEFORMAT_WAV = 19;
    public static final int FILEFORMAT_WMA = 10;
    public static final int FILEFORMAT_WMV = 11;
    public static final int FILE_FORMAT = 0;
    public static final int FILE_SIZE = 7;
    public static final int FRAME_HEIGHT = 4;
    public static final int FRAME_WIDTH = 3;
    public static final int H264_ENC_LEVEL_30 = 30;
    public static final int H264_ENC_LEVEL_31 = 31;
    public static final int H264_ENC_LEVEL_40 = 40;
    public static final int H264_ENC_LEVEL_41 = 41;
    public static final int H264_ENC_LEVEL_UNKNOW = 0;
    public static final int H264_ENC_PROFILE_BASELINE = 1;
    public static final int H264_ENC_PROFILE_HIGH = 3;
    public static final int H264_ENC_PROFILE_MAIN = 2;
    public static final int H264_ENC_PROFILE_UNKNOW = 0;
    public static final int HEVC_ENC_LEVEL_AUTO = 1;
    public static final int HEVC_ENC_PROFILE_MAIN = 16;
    public static final int HEVC_ENC_PROFILE_MAIN10 = 17;
    public static final int VIDEOFORMAT_AUTO = 1;
    public static final int VIDEOFORMAT_DX50 = 5;
    public static final int VIDEOFORMAT_GIF = 10;
    public static final int VIDEOFORMAT_H263 = 3;
    public static final int VIDEOFORMAT_H264 = 4;
    public static final int VIDEOFORMAT_H265 = 12;
    public static final int VIDEOFORMAT_MPEG4 = 2;
    public static final int VIDEOFORMAT_NONE = 0;
    public static final int VIDEOFORMAT_UNKNOWN = 1;
    public static final int VIDEOFORMAT_VP8 = 13;
    public static final int VIDEOFORMAT_VP9 = 14;
    public static final int VIDEOFORMAT_WMV = 6;
    public static final int VIDEOFORMAT_XVID = 7;
    public static final int VIDEO_BIT_RATE = 10;
    public static final int VIDEO_DURATION = 5;
    public static final int VIDEO_FORMAT = 1;
    public static final int VIDEO_FRAME_RATE = 9;
    private int audioBitrate;
    private int audioBitsPerSample;
    private int audioBlockAlign;
    private int audioChannel;
    private int audioDuration;
    private int audioFormat;
    private int audioSampleRate;
    private int bitrate;
    private int fileFormat;
    private int fileSize;
    private int frameHeight;
    private int frameWidth;
    private int videoBitrate;
    private int videoDuration;
    private int videoFormat;
    private int videoFrameRate;

    public QVideoInfo() {
        this.fileFormat = 0;
        this.videoFormat = 0;
        this.audioFormat = 0;
        this.frameWidth = 0;
        this.frameHeight = 0;
        this.videoDuration = 0;
        this.audioDuration = 0;
        this.fileSize = 0;
        this.bitrate = 0;
        this.videoFrameRate = 0;
        this.videoBitrate = 0;
        this.audioSampleRate = 0;
        this.audioChannel = 0;
        this.audioBitrate = 0;
        this.audioBitsPerSample = 0;
        this.audioBlockAlign = 0;
    }

    public int get(int field) {
        switch (field) {
            case 0:
                return this.fileFormat;
            case 1:
                return this.videoFormat;
            case 2:
                return this.audioFormat;
            case 3:
                return this.frameWidth;
            case 4:
                return this.frameHeight;
            case 5:
                return this.videoDuration;
            case 6:
                return this.audioDuration;
            case 7:
                return this.fileSize;
            case 8:
                return this.bitrate;
            case 9:
                return this.videoFrameRate;
            case 10:
                return this.videoBitrate;
            case 11:
                return this.audioSampleRate;
            case 12:
                return this.audioChannel;
            case 13:
                return this.audioBitrate;
            case 14:
                return this.audioBitsPerSample;
            case 15:
                return this.audioBlockAlign;
            default:
                return 0;
        }
    }

    public void set(int field, int value) {
        switch (field) {
            case 0:
                this.fileFormat = value;
                break;
            case 1:
                this.videoFormat = value;
                break;
            case 2:
                this.audioFormat = value;
                break;
            case 3:
                this.frameWidth = value;
                break;
            case 4:
                this.frameHeight = value;
                break;
            case 5:
                this.videoDuration = value;
                break;
            case 6:
                this.audioDuration = value;
                break;
            case 7:
                this.fileSize = value;
                break;
            case 8:
                this.bitrate = value;
                break;
            case 9:
                this.videoFrameRate = value;
                break;
            case 10:
                this.videoBitrate = value;
                break;
            case 11:
                this.audioSampleRate = value;
                break;
            case 12:
                this.audioChannel = value;
                break;
            case 13:
                this.audioBitrate = value;
                break;
            case 14:
                this.audioBitsPerSample = value;
                break;
            case 15:
                this.audioBlockAlign = value;
                break;
        }
    }

    public QVideoInfo(QVideoInfo videoInfo) {
        this.fileFormat = 0;
        this.videoFormat = 0;
        this.audioFormat = 0;
        this.frameWidth = 0;
        this.frameHeight = 0;
        this.videoDuration = 0;
        this.audioDuration = 0;
        this.fileSize = 0;
        this.bitrate = 0;
        this.videoFrameRate = 0;
        this.videoBitrate = 0;
        this.audioSampleRate = 0;
        this.audioChannel = 0;
        this.audioBitrate = 0;
        this.audioBitsPerSample = 0;
        this.audioBlockAlign = 0;
        this.fileFormat = videoInfo.fileFormat;
        this.videoFormat = videoInfo.videoFormat;
        this.audioFormat = videoInfo.audioFormat;
        this.frameWidth = videoInfo.frameWidth;
        this.frameHeight = videoInfo.frameHeight;
        this.videoDuration = videoInfo.videoDuration;
        this.audioDuration = videoInfo.audioDuration;
        this.fileSize = videoInfo.fileSize;
        this.bitrate = videoInfo.bitrate;
        this.videoFrameRate = videoInfo.videoFrameRate;
        this.videoBitrate = videoInfo.videoBitrate;
        this.audioSampleRate = videoInfo.audioSampleRate;
        this.audioChannel = videoInfo.audioChannel;
        this.audioBitrate = videoInfo.audioBitrate;
        this.audioBitsPerSample = videoInfo.audioBitsPerSample;
        this.audioBlockAlign = videoInfo.audioBlockAlign;
    }

    public QVideoInfo(int fileFormat, int videoFormat, int audioFormat, int frameWidth, int frameHeight, int videoDuration, int audioDuration, int fileSize, int bitrate, int videoFrameRate, int videoBitrate, int audioSampleRate, int audioChannel, int audioBitrate, int audioBitsPerSample, int audioBlockAlign) {
        this.fileFormat = fileFormat;
        this.videoFormat = videoFormat;
        this.audioFormat = audioFormat;
        this.frameWidth = frameWidth;
        this.frameHeight = frameHeight;
        this.videoDuration = videoDuration;
        this.audioDuration = audioDuration;
        this.fileSize = fileSize;
        this.bitrate = bitrate;
        this.videoFrameRate = videoFrameRate;
        this.videoBitrate = videoBitrate;
        this.audioSampleRate = audioSampleRate;
        this.audioChannel = audioChannel;
        this.audioBitrate = audioBitrate;
        this.audioBitsPerSample = audioBitsPerSample;
        this.audioBlockAlign = audioBlockAlign;
    }
}
