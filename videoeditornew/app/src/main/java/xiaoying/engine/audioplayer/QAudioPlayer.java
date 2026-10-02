package xiaoying.engine.audioplayer;

import xiaoying.engine.QEngine;
import xiaoying.engine.base.IQSessionStateListener;
import xiaoying.engine.base.QRange;
import xiaoying.engine.base.QSessionStream;
import xiaoying.engine.base.QSessionStreamOpenParam;
import xiaoying.engine.clip.QClip;
import xiaoying.engine.clip.QMediaSource;
import xiaoying.engine.player.QPlayer;
import xiaoying.engine.player.QPlayerState;
import xiaoying.engine.storyboard.QStoryboard;
import xiaoying.utils.QPoint;

/* JADX INFO: loaded from: classes18.dex */
public class QAudioPlayer {
    public static final int PROP_AUDIO_FADE_IN = 3;
    public static final int PROP_AUDIO_FADE_OUT = 4;
    public static final int PROP_AUDIO_GAIN = 2;
    public static final int PROP_AUDIO_INVERSE_FLAG = 10;
    public static final int PROP_AUDIO_INVERSE_SRC_RANGE = 9;
    public static final int PROP_AUDIO_INVERSE_TRIM_RANGE = 8;
    public static final int PROP_AUDIO_IS_NEED_NSX = 13;
    public static final int PROP_AUDIO_MUTE = 5;
    public static final int PROP_AUDIO_PITCH = 6;
    public static final int PROP_AUDIO_PLAYER_BASE = 0;
    public static final int PROP_AUDIO_RANGE = 1;
    public static final int PROP_AUDIO_TIME_SCALE = 11;
    public static final int PROP_AUDIO_TIME_SCALE_USE_AUDIO_PITCH = 12;
    public static final int PROP_AUDIO_TRIM_RANGE = 7;
    public static final int QERR_AUDIOPLAYER_ERR1 = 10485761;
    public static final int QERR_AUDIOPLAYER_ERR10 = 10485770;
    public static final int QERR_AUDIOPLAYER_ERR11 = 10485771;
    public static final int QERR_AUDIOPLAYER_ERR12 = 10485772;
    public static final int QERR_AUDIOPLAYER_ERR13 = 10485773;
    public static final int QERR_AUDIOPLAYER_ERR14 = 10485774;
    public static final int QERR_AUDIOPLAYER_ERR15 = 10485775;
    public static final int QERR_AUDIOPLAYER_ERR16 = 10485776;
    public static final int QERR_AUDIOPLAYER_ERR17 = 10485777;
    public static final int QERR_AUDIOPLAYER_ERR18 = 10485778;
    public static final int QERR_AUDIOPLAYER_ERR19 = 10485779;
    public static final int QERR_AUDIOPLAYER_ERR2 = 10485762;
    public static final int QERR_AUDIOPLAYER_ERR20 = 10485780;
    public static final int QERR_AUDIOPLAYER_ERR21 = 10485781;
    public static final int QERR_AUDIOPLAYER_ERR3 = 10485763;
    public static final int QERR_AUDIOPLAYER_ERR4 = 10485764;
    public static final int QERR_AUDIOPLAYER_ERR5 = 10485765;
    public static final int QERR_AUDIOPLAYER_ERR6 = 10485766;
    public static final int QERR_AUDIOPLAYER_ERR7 = 10485767;
    public static final int QERR_AUDIOPLAYER_ERR8 = 10485768;
    public static final int QERR_AUDIOPLAYER_ERR9 = 10485769;
    public static final int QERR_AUDIOPLAYER_ERR_BASE = 10485760;
    private QClip mClip = null;
    private QStoryboard mStoryboard = null;
    private QPlayer mPlayer = null;
    private QSessionStream mStream = null;

    public int Init(QEngine engine, String strAudioFile, QRange srcRange, IQSessionStateListener listener) {
        if (engine == null || strAudioFile == null || srcRange == null) {
            return QERR_AUDIOPLAYER_ERR1;
        }
        if (this.mClip != null) {
            return Replace(strAudioFile, srcRange);
        }
        this.mClip = new QClip();
        int iInit = this.mClip.init(engine, new QMediaSource(0, false, strAudioFile));
        if (iInit != 0) {
            return iInit;
        }
        int property = this.mClip.setProperty(12318, srcRange);
        if (property != 0) {
            return property;
        }
        this.mClip.setProperty(QClip.PROP_VIDEO_DISABLED, Boolean.TRUE);
        QStoryboard qStoryboard = new QStoryboard();
        this.mStoryboard = qStoryboard;
        int iInit2 = qStoryboard.init(engine, null);
        if (iInit2 != 0) {
            return iInit2;
        }
        this.mStoryboard.setProperty(QStoryboard.PROP_OUTPUT_RESOLUTION, new QPoint(1920, 1080));
        int iInsertClip = this.mStoryboard.insertClip(this.mClip, 0);
        if (iInsertClip != 0) {
            return iInsertClip;
        }
        this.mStream = new QSessionStream();
        int iOpen = this.mStream.open(1, this.mStoryboard, new QSessionStreamOpenParam());
        if (iOpen != 0) {
            return iOpen;
        }
        QPlayer qPlayer = new QPlayer();
        this.mPlayer = qPlayer;
        int iInit3 = qPlayer.init(engine, listener);
        return iInit3 != 0 ? iInit3 : this.mPlayer.activeStream(this.mStream, 0, false);
    }

    public int Replace(String strAudioFile, QRange srcRange) {
        if (this.mClip == null || strAudioFile == null || srcRange == null) {
            return QERR_AUDIOPLAYER_ERR21;
        }
        return this.mClip.replaceWithSrc(new QMediaSource(0, false, strAudioFile), srcRange, srcRange);
    }

    public void UnInit() {
        QPlayer qPlayer = this.mPlayer;
        if (qPlayer != null) {
            qPlayer.stop();
            this.mPlayer.deactiveStream();
            this.mPlayer.unInit();
            this.mPlayer = null;
        }
        QSessionStream qSessionStream = this.mStream;
        if (qSessionStream != null) {
            qSessionStream.close();
            this.mStream = null;
        }
        QStoryboard qStoryboard = this.mStoryboard;
        if (qStoryboard != null) {
            qStoryboard.unInit();
            this.mStoryboard = null;
        }
        this.mClip = null;
    }

    public Object getProperty(int propertyID) {
        switch (propertyID) {
            case 1:
                QClip qClip = this.mClip;
                if (qClip == null) {
                    return null;
                }
                return qClip.getProperty(12318);
            case 2:
                QClip qClip2 = this.mClip;
                if (qClip2 == null) {
                    return null;
                }
                return qClip2.getProperty(QClip.PROP_CLIP_AUDIO_GAIN);
            case 3:
                QClip qClip3 = this.mClip;
                if (qClip3 == null) {
                    return null;
                }
                return qClip3.getProperty(12297);
            case 4:
                QClip qClip4 = this.mClip;
                if (qClip4 == null) {
                    return null;
                }
                return qClip4.getProperty(12298);
            case 5:
                QClip qClip5 = this.mClip;
                if (qClip5 == null) {
                    return null;
                }
                return qClip5.getProperty(12300);
            case 6:
                QClip qClip6 = this.mClip;
                if (qClip6 == null) {
                    return null;
                }
                return qClip6.getProperty(QClip.PROP_AUDIO_PITCH_DELTA);
            case 7:
                QClip qClip7 = this.mClip;
                if (qClip7 == null) {
                    return null;
                }
                return qClip7.getProperty(12292);
            case 8:
                QClip qClip8 = this.mClip;
                if (qClip8 == null) {
                    return null;
                }
                return qClip8.getProperty(QClip.PROP_CLIP_INVERSE_PLAY_TRIM_RANGE);
            case 9:
                QClip qClip9 = this.mClip;
                if (qClip9 == null) {
                    return null;
                }
                return qClip9.getProperty(QClip.PROP_CLIP_INVERSE_PLAY_SOURCE_RANGE);
            case 10:
                QClip qClip10 = this.mClip;
                if (qClip10 == null) {
                    return null;
                }
                return qClip10.getProperty(QClip.PROP_CLIP_INVERSE_PLAY_AUDIO_FLAG);
            case 11:
                QClip qClip11 = this.mClip;
                if (qClip11 == null) {
                    return null;
                }
                return qClip11.getProperty(12293);
            case 12:
                QClip qClip12 = this.mClip;
                if (qClip12 == null) {
                    return null;
                }
                return qClip12.getProperty(QClip.PROP_CLIP_IS_TIME_SCALE_USE_AUDIO_PITCH);
            case 13:
                QClip qClip13 = this.mClip;
                if (qClip13 == null) {
                    return null;
                }
                return qClip13.getProperty(QClip.PROP_CLIP_AUDIO_IS_NEED_NSX);
            default:
                return null;
        }
    }

    public QPlayerState getState() {
        QPlayer qPlayer = this.mPlayer;
        if (qPlayer == null) {
            return null;
        }
        return (QPlayerState) qPlayer.getState();
    }

    public int pause() {
        QPlayer qPlayer = this.mPlayer;
        return qPlayer == null ? QERR_AUDIOPLAYER_ERR3 : qPlayer.pause();
    }

    public int play() {
        QPlayer qPlayer = this.mPlayer;
        return qPlayer == null ? QERR_AUDIOPLAYER_ERR2 : qPlayer.play();
    }

    public int refreshStream() {
        QPlayer qPlayer = this.mPlayer;
        if (qPlayer == null || this.mStoryboard == null) {
            return QERR_AUDIOPLAYER_ERR12;
        }
        QPlayerState qPlayerState = (QPlayerState) qPlayer.getState();
        int i10 = qPlayerState != null ? qPlayerState.currentTime : 0;
        int iRefreshStream = this.mPlayer.refreshStream(this.mStoryboard.getDataClip(), 11, null);
        return iRefreshStream != 0 ? iRefreshStream : this.mPlayer.seekTo(i10);
    }

    public int seekTo(int position) {
        QPlayer qPlayer = this.mPlayer;
        return qPlayer == null ? QERR_AUDIOPLAYER_ERR5 : qPlayer.seekTo(position);
    }

    public int setProperty(int propertyID, Object data) {
        switch (propertyID) {
            case 1:
                QClip qClip = this.mClip;
                return qClip == null ? QERR_AUDIOPLAYER_ERR6 : qClip.setProperty(12318, data);
            case 2:
                QClip qClip2 = this.mClip;
                return qClip2 == null ? QERR_AUDIOPLAYER_ERR7 : qClip2.setProperty(QClip.PROP_CLIP_AUDIO_GAIN, data);
            case 3:
                QClip qClip3 = this.mClip;
                return qClip3 == null ? QERR_AUDIOPLAYER_ERR8 : qClip3.setProperty(12297, data);
            case 4:
                QClip qClip4 = this.mClip;
                return qClip4 == null ? QERR_AUDIOPLAYER_ERR9 : qClip4.setProperty(12298, data);
            case 5:
                QClip qClip5 = this.mClip;
                return qClip5 == null ? QERR_AUDIOPLAYER_ERR10 : qClip5.setProperty(12300, data);
            case 6:
                QClip qClip6 = this.mClip;
                if (qClip6 == null) {
                    return QERR_AUDIOPLAYER_ERR11;
                }
                qClip6.setProperty(QClip.PROP_AUDIO_MODIFY_BY_ASP, Boolean.TRUE);
                return this.mClip.setProperty(QClip.PROP_AUDIO_PITCH_DELTA, data);
            case 7:
                QClip qClip7 = this.mClip;
                return qClip7 == null ? QERR_AUDIOPLAYER_ERR14 : qClip7.setProperty(12292, data);
            case 8:
                QClip qClip8 = this.mClip;
                return qClip8 == null ? QERR_AUDIOPLAYER_ERR15 : qClip8.setProperty(QClip.PROP_CLIP_INVERSE_PLAY_TRIM_RANGE, data);
            case 9:
                QClip qClip9 = this.mClip;
                return qClip9 == null ? QERR_AUDIOPLAYER_ERR16 : qClip9.setProperty(QClip.PROP_CLIP_INVERSE_PLAY_SOURCE_RANGE, data);
            case 10:
                QClip qClip10 = this.mClip;
                return qClip10 == null ? QERR_AUDIOPLAYER_ERR17 : qClip10.setProperty(QClip.PROP_CLIP_INVERSE_PLAY_AUDIO_FLAG, data);
            case 11:
                QClip qClip11 = this.mClip;
                return qClip11 == null ? QERR_AUDIOPLAYER_ERR18 : qClip11.setProperty(12293, data);
            case 12:
                QClip qClip12 = this.mClip;
                if (qClip12 == null) {
                    return QERR_AUDIOPLAYER_ERR19;
                }
                qClip12.setProperty(QClip.PROP_AUDIO_MODIFY_BY_ASP, Boolean.TRUE);
                return this.mClip.setProperty(QClip.PROP_CLIP_IS_TIME_SCALE_USE_AUDIO_PITCH, data);
            case 13:
                QClip qClip13 = this.mClip;
                if (qClip13 == null) {
                    return QERR_AUDIOPLAYER_ERR20;
                }
                qClip13.setProperty(QClip.PROP_AUDIO_MODIFY_BY_ASP, Boolean.TRUE);
                return this.mClip.setProperty(QClip.PROP_CLIP_AUDIO_IS_NEED_NSX, data);
            default:
                return 0;
        }
    }

    public int setVolume(int volume) {
        QPlayer qPlayer = this.mPlayer;
        return qPlayer == null ? QERR_AUDIOPLAYER_ERR13 : qPlayer.setVolume(volume);
    }

    public int stop() {
        QPlayer qPlayer = this.mPlayer;
        return qPlayer == null ? QERR_AUDIOPLAYER_ERR4 : qPlayer.stop();
    }
}
