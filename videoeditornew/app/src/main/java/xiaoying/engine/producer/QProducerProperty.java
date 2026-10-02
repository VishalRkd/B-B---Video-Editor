package xiaoying.engine.producer;

import xiaoying.engine.base.QRange;

/* JADX INFO: loaded from: classes19.dex */
public class QProducerProperty {
    public static final int AUDIO_FORMAT = 2;
    public static final int BITRATE_MODE_CBR = 2;
    public static final int BITRATE_MODE_VBR = 1;
    public static final int ENCODER_TYPE = 5;
    public static final int ENCODER_TYPE_AUTO = 1024;
    public static final int ENCODER_TYPE_HW = 256;
    public static final int ENCODER_TYPE_SW = 512;
    public static final int FILE_FORMAT = 0;
    public static final int VIDEO_BITRATE = 4;
    public static final int VIDEO_FORMAT = 1;
    public static final int VIDEO_FRAME_RATE = 3;
    public int audioFormat;

    @Deprecated
    public boolean bConstRateOpen;
    public boolean bHasBFrame;
    public String destFile;
    public int encoderType;
    public int fileFormat;
    public int mBitreateMode;
    public boolean mCloseHWAysncEncoder;
    public int mKeyframeInterval;
    public int mLevel;
    public int mProfile;
    public int maxExpFps;
    public long maxFileSize;
    public QRange range;
    public int videoBitrate;
    public int videoFormat;
    public int videoFrameRate;
    public String wmCode;

    public QProducerProperty() {
        this.fileFormat = 0;
        this.videoFormat = 0;
        this.audioFormat = 0;
        this.videoFrameRate = 0;
        this.videoBitrate = 0;
        this.maxFileSize = 0L;
        this.destFile = null;
        this.encoderType = 1024;
        this.range = null;
        this.mProfile = 0;
        this.mLevel = 0;
        this.wmCode = null;
        this.maxExpFps = 30;
        this.mKeyframeInterval = -1;
        this.bConstRateOpen = false;
        this.bHasBFrame = false;
        this.mBitreateMode = 0;
        this.mCloseHWAysncEncoder = false;
    }

    public int get(int field) {
        if (field == 0) {
            return this.fileFormat;
        }
        if (field == 1) {
            return this.videoFormat;
        }
        if (field == 2) {
            return this.audioFormat;
        }
        if (field == 3) {
            return this.videoFrameRate;
        }
        if (field == 4) {
            return this.videoBitrate;
        }
        if (field != 5) {
            return 0;
        }
        return this.encoderType;
    }

    public String getDestFile() {
        return this.destFile;
    }

    public long getMaxFileSize() {
        return this.maxFileSize;
    }

    public QRange getRange() {
        return this.range;
    }

    public void set(int field, int value) {
        if (field == 0) {
            this.fileFormat = value;
            return;
        }
        if (field == 1) {
            this.videoFormat = value;
            return;
        }
        if (field == 2) {
            this.audioFormat = value;
            return;
        }
        if (field == 3) {
            this.videoFrameRate = value;
        } else if (field == 4) {
            this.videoBitrate = value;
        } else {
            if (field != 5) {
                return;
            }
            this.encoderType = value;
        }
    }

    public void setDestFile(String destFile) {
        this.destFile = destFile;
    }

    public void setMaxFileSize(long maxFileSize) {
        this.maxFileSize = maxFileSize;
    }

    public void setRange(QRange range) {
        this.range = range;
    }

    public QProducerProperty(int fileFormat, int videoFormat, int audioFormat, int videoFrameRate, int videoBitrate, long maxFileSize, String destFile, int encoderType, QRange range, int profile, int level, String wmCode) {
        this.maxExpFps = 30;
        this.mKeyframeInterval = -1;
        this.bConstRateOpen = false;
        this.bHasBFrame = false;
        this.mBitreateMode = 0;
        this.mCloseHWAysncEncoder = false;
        this.fileFormat = fileFormat;
        this.videoFormat = videoFormat;
        this.audioFormat = audioFormat;
        this.videoFrameRate = videoFrameRate;
        this.videoBitrate = videoBitrate;
        this.maxFileSize = maxFileSize;
        this.destFile = destFile;
        this.encoderType = encoderType;
        this.range = range;
        this.mProfile = profile;
        this.mLevel = level;
        this.wmCode = wmCode;
    }
}
