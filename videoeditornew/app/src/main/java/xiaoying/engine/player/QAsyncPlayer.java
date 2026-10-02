package xiaoying.engine.player;

import xiaoying.engine.aecomp.QAECompStream;
import xiaoying.engine.base.IQAsyncTagListener;
import xiaoying.engine.base.QDisplayContext;
import xiaoying.engine.base.QSessionStream;
import xiaoying.engine.clip.QClip;
import xiaoying.engine.clip.QEffect;

/* JADX INFO: loaded from: classes19.dex */
public class QAsyncPlayer extends QPlayer {
    public int activeStream(QSessionStream stream, int position, boolean syncseek, int tag) {
        int iActiveStream;
        synchronized (this) {
            try {
                addAsyncTagBegin(tag);
                iActiveStream = activeStream(stream, position, syncseek);
                if (iActiveStream == 0) {
                    addAsyncTagEnd(tag);
                }
            } catch (Throwable th2) {
                throw th2;
            }
        }
        return iActiveStream;
    }

    public int addAsyncTagBegin(int tag) {
        return setProperty(QPlayer.PROP_PLAYER_ASYNC_TAG_BEGIN, Integer.valueOf(tag));
    }

    public int addAsyncTagEnd(int tag) {
        return setProperty(QPlayer.PROP_PLAYER_ASYNC_TAG_END, Integer.valueOf(tag));
    }

    public int autoRefreshStream(int tag) {
        int iAutoRefreshStream;
        synchronized (this) {
            try {
                addAsyncTagBegin(tag);
                iAutoRefreshStream = autoRefreshStream();
                if (iAutoRefreshStream == 0) {
                    addAsyncTagEnd(tag);
                }
            } catch (Throwable th2) {
                throw th2;
            }
        }
        return iAutoRefreshStream;
    }

    public int deactiveStream(int tag) {
        int iDeactiveStream;
        synchronized (this) {
            try {
                addAsyncTagBegin(tag);
                iDeactiveStream = deactiveStream();
                if (iDeactiveStream == 0) {
                    addAsyncTagEnd(tag);
                }
            } catch (Throwable th2) {
                throw th2;
            }
        }
        return iDeactiveStream;
    }

    public int disableDisplay(boolean disable, int tag) {
        int iDisableDisplay;
        synchronized (this) {
            try {
                addAsyncTagBegin(tag);
                iDisableDisplay = disableDisplay(disable);
                if (iDisableDisplay == 0) {
                    addAsyncTagEnd(tag);
                }
            } catch (Throwable th2) {
                throw th2;
            }
        }
        return iDisableDisplay;
    }

    public int displayRefresh(int tag) {
        int iDisplayRefresh;
        synchronized (this) {
            try {
                addAsyncTagBegin(tag);
                iDisplayRefresh = displayRefresh();
                if (iDisplayRefresh == 0) {
                    addAsyncTagEnd(tag);
                }
            } catch (Throwable th2) {
                throw th2;
            }
        }
        return iDisplayRefresh;
    }

    public int lockStuffUnderEffect(QEffect e10, int tag) {
        int iLockStuffUnderEffect;
        synchronized (this) {
            try {
                addAsyncTagBegin(tag);
                iLockStuffUnderEffect = lockStuffUnderEffect(e10);
                if (iLockStuffUnderEffect == 0) {
                    addAsyncTagEnd(tag);
                }
            } catch (Throwable th2) {
                throw th2;
            }
        }
        return iLockStuffUnderEffect;
    }

    public int pause(int tag) {
        int iPause;
        synchronized (this) {
            try {
                addAsyncTagBegin(tag);
                iPause = pause();
                if (iPause == 0) {
                    addAsyncTagEnd(tag);
                }
            } catch (Throwable th2) {
                throw th2;
            }
        }
        return iPause;
    }

    public int play(int tag) {
        int iPlay;
        synchronized (this) {
            try {
                addAsyncTagBegin(tag);
                iPlay = play();
                if (iPlay == 0) {
                    addAsyncTagEnd(tag);
                }
            } catch (Throwable th2) {
                throw th2;
            }
        }
        return iPlay;
    }

    public int refreshStream(QClip clip, int opCode, QEffect effect, int tag) {
        int iRefreshStream;
        synchronized (this) {
            try {
                addAsyncTagBegin(tag);
                iRefreshStream = refreshStream(clip, opCode, effect);
                if (iRefreshStream == 0) {
                    addAsyncTagEnd(tag);
                }
            } catch (Throwable th2) {
                throw th2;
            }
        }
        return iRefreshStream;
    }

    public int seekTo(int position, int tag) {
        int iSeekTo;
        synchronized (this) {
            try {
                addAsyncTagBegin(tag);
                iSeekTo = seekTo(position);
                if (iSeekTo == 0) {
                    addAsyncTagEnd(tag);
                }
            } catch (Throwable th2) {
                throw th2;
            }
        }
        return iSeekTo;
    }

    public int setAsyncTagListener(IQAsyncTagListener listener) {
        this.mAsyncTagListener = listener;
        return 0;
    }

    public int setDisplayContext(QDisplayContext displayContext, int tag) {
        int displayContext2;
        synchronized (this) {
            try {
                addAsyncTagBegin(tag);
                displayContext2 = setDisplayContext(displayContext);
                if (displayContext2 == 0) {
                    addAsyncTagEnd(tag);
                }
            } catch (Throwable th2) {
                throw th2;
            }
        }
        return displayContext2;
    }

    public int stop(int tag) {
        int iStop;
        synchronized (this) {
            try {
                addAsyncTagBegin(tag);
                iStop = stop();
                if (iStop == 0) {
                    addAsyncTagEnd(tag);
                }
            } catch (Throwable th2) {
                throw th2;
            }
        }
        return iStop;
    }

    public int syncSeekTo(int position, int tag) {
        int iSyncSeekTo;
        synchronized (this) {
            try {
                addAsyncTagBegin(tag);
                iSyncSeekTo = syncSeekTo(position);
                if (iSyncSeekTo == 0) {
                    addAsyncTagEnd(tag);
                }
            } catch (Throwable th2) {
                throw th2;
            }
        }
        return iSyncSeekTo;
    }

    public int unlockStuffUnderEffect(QEffect e10, int tag) {
        int iUnlockStuffUnderEffect;
        synchronized (this) {
            try {
                addAsyncTagBegin(tag);
                iUnlockStuffUnderEffect = unlockStuffUnderEffect(e10);
                if (iUnlockStuffUnderEffect == 0) {
                    addAsyncTagEnd(tag);
                }
            } catch (Throwable th2) {
                throw th2;
            }
        }
        return iUnlockStuffUnderEffect;
    }

    public int activeStream(QAECompStream stream, int position, boolean syncseek, int tag) {
        int iActiveStream;
        synchronized (this) {
            try {
                addAsyncTagBegin(tag);
                iActiveStream = activeStream(stream, position, syncseek);
                if (iActiveStream == 0) {
                    addAsyncTagEnd(tag);
                }
            } catch (Throwable th2) {
                throw th2;
            }
        }
        return iActiveStream;
    }
}
