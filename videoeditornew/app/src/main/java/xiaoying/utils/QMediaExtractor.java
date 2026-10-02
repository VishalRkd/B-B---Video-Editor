package xiaoying.utils;

import android.media.MediaExtractor;
import android.media.MediaFormat;
import java.nio.ByteBuffer;

/* JADX INFO: loaded from: classes19.dex */
public class QMediaExtractor {
    private static final String TAG = "MCEXTRACTOR";
    private String audioMime_;
    private MediaExtractor extractor_;
    private String mediaFile_;
    private String videoMime_;
    private int videoIndex_ = -1;
    private int audioIndex_ = -1;
    private boolean videoselected_ = false;
    private boolean audioselected_ = false;
    private boolean hasVideo_ = false;
    private boolean hasAudio_ = false;
    private ByteBuffer[] videoHeaderData_ = new ByteBuffer[2];
    private ByteBuffer[] audioHeaderData_ = new ByteBuffer[2];
    private long videoDuration_ = 0;
    private long audioDuration_ = 0;
    private long videoBitrate_ = 0;
    private long audioBitrate_ = 0;
    private int videoWidth_ = 0;
    private int videoHeight_ = 0;
    private int videoFramerate_ = 0;
    private int videoRotation_ = 0;
    private int audioSampleRate_ = 0;
    private int audioChannels_ = 0;
    private long videoTrackSize_ = 0;
    private long audioTrackSize_ = 0;
    private long videoCurrent_ = 0;
    private long audioCurrent_ = 0;
    private long prevFrameTs_ = 0;
    private long nextFrameTs_ = 0;
    private long nextAudioFrameTs_ = 0;
    private int seekType_ = 0;

    public void close() {
        MediaExtractor mediaExtractor = this.extractor_;
        if (mediaExtractor != null) {
            mediaExtractor.release();
        }
    }

    public long getAudioBitrate() {
        return this.audioBitrate_;
    }

    public int getAudioChannels() {
        return this.audioChannels_;
    }

    public int getAudioCodecMime(byte[] out) {
        int length = out.length;
        byte[] bytes = this.audioMime_.getBytes();
        if (bytes.length < length) {
            length = bytes.length;
        }
        System.arraycopy(bytes, 0, out, 0, length);
        return length;
    }

    public long getAudioDuration() {
        return this.audioDuration_;
    }

    public int getAudioSampleRate() {
        return this.audioSampleRate_;
    }

    public int getAudioSpecData(byte[] buff, int buffsize) {
        int iLimit;
        if (this.audioIndex_ < 0) {
            return 0;
        }
        ByteBuffer byteBuffer = this.audioHeaderData_[0];
        if (byteBuffer != null) {
            iLimit = byteBuffer.limit();
            if (iLimit > buffsize) {
                return 0;
            }
            System.arraycopy(this.audioHeaderData_[0].array(), 0, buff, 0, iLimit);
        } else {
            iLimit = 0;
        }
        ByteBuffer byteBuffer2 = this.audioHeaderData_[1];
        if (byteBuffer2 == null) {
            return iLimit;
        }
        int iLimit2 = byteBuffer2.limit();
        int i10 = iLimit + iLimit2;
        if (i10 > buffsize) {
            return 0;
        }
        System.arraycopy(this.audioHeaderData_[1].array(), 0, buff, iLimit, iLimit2);
        return i10;
    }

    public long getAudioTrackSize() {
        return this.audioTrackSize_;
    }

    public long getDuration() {
        long j10 = this.videoDuration_;
        long j11 = this.audioDuration_;
        return j10 > j11 ? j10 : j11;
    }

    public long getVideoBitrate() {
        return this.videoBitrate_;
    }

    public int getVideoCodecMime(byte[] out) {
        int length = out.length;
        byte[] bytes = this.videoMime_.getBytes();
        if (bytes.length < length) {
            length = bytes.length;
        }
        System.arraycopy(bytes, 0, out, 0, length);
        return length;
    }

    public long getVideoDuration() {
        return this.videoDuration_;
    }

    public int getVideoFramerate() {
        return this.videoFramerate_;
    }

    public int getVideoHeight() {
        return this.videoHeight_;
    }

    public int getVideoRotation() {
        return this.videoRotation_;
    }

    public int getVideoSpecData(byte[] buff, int buffsize) {
        int iLimit;
        if (this.videoIndex_ < 0) {
            return 0;
        }
        ByteBuffer byteBuffer = this.videoHeaderData_[0];
        if (byteBuffer != null) {
            iLimit = byteBuffer.limit();
            if (iLimit > buffsize) {
                return 0;
            }
            System.arraycopy(this.videoHeaderData_[0].array(), 0, buff, 0, iLimit);
        } else {
            iLimit = 0;
        }
        ByteBuffer byteBuffer2 = this.videoHeaderData_[1];
        if (byteBuffer2 == null) {
            return iLimit;
        }
        int iLimit2 = byteBuffer2.limit();
        int i10 = iLimit + iLimit2;
        if (i10 > buffsize) {
            return 0;
        }
        System.arraycopy(this.videoHeaderData_[1].array(), 0, buff, iLimit, iLimit2);
        return i10;
    }

    public long getVideoTrackSize() {
        return this.videoTrackSize_;
    }

    public int getVideoWidth() {
        return this.videoWidth_;
    }

    public boolean hasAudioTrack() {
        return this.hasAudio_;
    }

    public boolean hasVideoTrack() {
        return this.hasVideo_;
    }

    public boolean openEx(String path) {
        this.mediaFile_ = path;
        if (path != null && !path.isEmpty()) {
            StringBuilder sb2 = new StringBuilder();
            sb2.append("open file: ");
            sb2.append(path);
            MediaExtractor mediaExtractor = new MediaExtractor();
            this.extractor_ = mediaExtractor;
            try {
                mediaExtractor.setDataSource(path);
                int trackCount = this.extractor_.getTrackCount();
                for (int i10 = 0; i10 < trackCount; i10++) {
                    MediaFormat trackFormat = this.extractor_.getTrackFormat(i10);
                    String string = trackFormat.getString("mime");
                    if (string.contains("audio") && this.audioIndex_ < 0) {
                        this.audioMime_ = string;
                        this.audioIndex_ = i10;
                        this.audioHeaderData_[0] = trackFormat.getByteBuffer("csd-0");
                        this.audioHeaderData_[1] = trackFormat.getByteBuffer("csd-1");
                        if (trackFormat.containsKey("durationUs")) {
                            this.audioDuration_ = trackFormat.getLong("durationUs") / 1000;
                        }
                        this.audioSampleRate_ = trackFormat.getInteger("sample-rate");
                        this.audioChannels_ = trackFormat.getInteger("channel-count");
                        if (trackFormat.containsKey("bitrate")) {
                            this.audioBitrate_ = trackFormat.getInteger("bitrate");
                        }
                        this.hasAudio_ = true;
                    } else if (string.contains("video") && this.videoIndex_ < 0) {
                        this.videoMime_ = string;
                        this.videoIndex_ = i10;
                        this.videoHeaderData_[0] = trackFormat.getByteBuffer("csd-0");
                        this.videoHeaderData_[1] = trackFormat.getByteBuffer("csd-1");
                        if (trackFormat.containsKey("durationUs")) {
                            this.videoDuration_ = trackFormat.getLong("durationUs") / 1000;
                        }
                        this.videoWidth_ = trackFormat.getInteger("width");
                        this.videoHeight_ = trackFormat.getInteger("height");
                        if (trackFormat.containsKey("frame-rate")) {
                            this.videoFramerate_ = trackFormat.getInteger("frame-rate");
                        }
                        if (trackFormat.containsKey("bitrate")) {
                            this.videoBitrate_ = trackFormat.getInteger("bitrate");
                        }
                        if (trackFormat.containsKey("rotation-degrees")) {
                            this.videoRotation_ = trackFormat.getInteger("rotation-degrees");
                        }
                        this.hasVideo_ = true;
                    }
                }
                int i11 = this.audioIndex_;
                if (i11 < 0 && this.videoIndex_ < 0) {
                    return false;
                }
                this.videoTrackSize_ = ((this.videoBitrate_ * this.videoDuration_) / 1000) / 8;
                this.audioTrackSize_ = ((this.audioBitrate_ * this.audioDuration_) / 1000) / 8;
                if (i11 >= 0) {
                    this.extractor_.selectTrack(i11);
                    this.audioselected_ = true;
                }
                int i12 = this.videoIndex_;
                if (i12 >= 0) {
                    this.extractor_.selectTrack(i12);
                    this.videoselected_ = true;
                }
                StringBuilder sb3 = new StringBuilder();
                sb3.append("Video :");
                sb3.append(this.videoHeaderData_[0]);
                sb3.append(" : ");
                sb3.append(this.videoHeaderData_[1]);
                StringBuilder sb4 = new StringBuilder();
                sb4.append("Audio :");
                sb4.append(this.audioHeaderData_[0]);
                sb4.append(" : ");
                sb4.append(this.audioHeaderData_[1]);
                return true;
            } catch (Exception unused) {
                StringBuilder sb5 = new StringBuilder();
                sb5.append("setDataSource(");
                sb5.append(path);
                sb5.append(") failed");
            }
        }
        return false;
    }

    public boolean readAudioFrame(byte[] outbuffer, int[] outinfo) {
        int i10 = this.audioIndex_;
        if (i10 < 0) {
            return false;
        }
        if (!this.audioselected_) {
            this.extractor_.selectTrack(i10);
            this.audioselected_ = true;
        }
        int i11 = this.videoIndex_;
        if (i11 >= 0) {
            this.extractor_.unselectTrack(i11);
            this.videoselected_ = false;
        }
        boolean z10 = false;
        while (!z10) {
            long sampleTime = this.extractor_.getSampleTime();
            if (sampleTime < 0) {
                break;
            }
            if (this.extractor_.getSampleTrackIndex() == this.audioIndex_) {
                int sampleData = this.extractor_.readSampleData(ByteBuffer.wrap(outbuffer, 0, outbuffer.length), 0);
                this.extractor_.getSampleFlags();
                outinfo[0] = sampleData;
                outinfo[1] = (int) (sampleTime / 1000);
                outinfo[2] = 0;
                outinfo[3] = 1;
                z10 = true;
            }
            this.extractor_.advance();
        }
        return z10;
    }

    public boolean readVideoFrame(byte[] outbuffer, int[] outinfo) {
        int i10 = this.videoIndex_;
        if (i10 < 0) {
            return false;
        }
        if (!this.videoselected_) {
            this.extractor_.selectTrack(i10);
            this.videoselected_ = true;
        }
        int i11 = this.audioIndex_;
        if (i11 >= 0) {
            this.extractor_.unselectTrack(i11);
            this.audioselected_ = false;
        }
        boolean z10 = false;
        while (!z10) {
            long sampleTime = this.extractor_.getSampleTime();
            if (sampleTime < 0) {
                break;
            }
            if (this.extractor_.getSampleTrackIndex() == this.videoIndex_) {
                int sampleData = this.extractor_.readSampleData(ByteBuffer.wrap(outbuffer, 0, outbuffer.length), 0);
                int i12 = (int) (sampleTime / 1000);
                int i13 = (this.extractor_.getSampleFlags() & 1) != 0 ? 1 : 0;
                outinfo[0] = sampleData;
                outinfo[1] = i12;
                outinfo[2] = 0;
                outinfo[3] = i13;
                z10 = true;
            }
            this.extractor_.advance();
        }
        return z10;
    }

    public long seekAudioTo(long tsms) {
        int i10 = this.audioIndex_;
        if (i10 < 0) {
            return -1L;
        }
        if (!this.audioselected_) {
            this.extractor_.selectTrack(i10);
            this.audioselected_ = true;
        }
        this.extractor_.seekTo(tsms * 1000, this.seekType_);
        while (true) {
            int sampleTrackIndex = this.extractor_.getSampleTrackIndex();
            long sampleTime = this.extractor_.getSampleTime();
            if (sampleTime < 0) {
                return -1L;
            }
            if (sampleTrackIndex == this.audioIndex_) {
                if (sampleTime < 0) {
                    return -1L;
                }
                return sampleTime / 1000;
            }
            this.extractor_.advance();
        }
    }

    public long seekTo(long tsms) {
        this.extractor_.seekTo(tsms * 1000, this.seekType_);
        long sampleTime = this.extractor_.getSampleTime();
        if (sampleTime < 0) {
            return -1L;
        }
        return sampleTime / 1000;
    }

    public long seekVideoTo(long tsms) {
        int i10 = this.videoIndex_;
        if (i10 < 0) {
            return -1L;
        }
        if (!this.videoselected_) {
            this.extractor_.selectTrack(i10);
            this.videoselected_ = true;
        }
        this.extractor_.seekTo(tsms * 1000, this.seekType_);
        while (true) {
            int sampleTrackIndex = this.extractor_.getSampleTrackIndex();
            long sampleTime = this.extractor_.getSampleTime();
            if (sampleTime < 0) {
                return -1L;
            }
            if (sampleTrackIndex == this.videoIndex_) {
                if (sampleTime < 0) {
                    return -1L;
                }
                return sampleTime / 1000;
            }
            this.extractor_.advance();
        }
    }

    public void setSeekType(int next) {
        if (next != 0) {
            this.seekType_ = 1;
        } else {
            this.seekType_ = 0;
        }
    }
}
