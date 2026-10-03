package xiaoying.engine.advance;

import android.util.Log;
import xiaoying.engine.base.QVEError;
import xiaoying.engine.clip.QClip;
import xiaoying.engine.clip.QTransition;
import xiaoying.engine.storyboard.QStoryboard;

/**
 * Advanced Java engine helper for creating and managing video transitions.
 */
public class QTransitionHelper {
    private static final String TAG = "QTransitionHelper";

    // Well-known built-in Xiaoying transition template IDs
    public static final long TRANSITION_CROSSFADE = 0x0300000000000000L;
    public static final long TRANSITION_DISSOLVE  = 0x0300000000000122L;
    public static final long TRANSITION_WIPE_L    = 0x0300000000000123L;
    public static final long TRANSITION_WIPE_R    = 0x0300000000000124L;
    public static final long TRANSITION_ZOOM_IN   = 0x0300000000000125L;
    public static final long TRANSITION_ZOOM_OUT  = 0x0300000000000126L;
    public static final long TRANSITION_ROTATE    = 0x0300000000000127L;
    public static final long TRANSITION_GLITCH    = 0x0300600000000010L;
    public static final long TRANSITION_BLUR      = 0x0300600000000018L;

    public static class TransitionModel {
        public final String name;
        public final long templateId;

        public TransitionModel(String name, long templateId) {
            this.name = name;
            this.templateId = templateId;
        }
    }

    public static final TransitionModel[] BUILTIN_TRANSITIONS = new TransitionModel[] {
        new TransitionModel("Fade", TRANSITION_CROSSFADE),
        new TransitionModel("Dissolve", TRANSITION_DISSOLVE),
        new TransitionModel("Wipe Left", TRANSITION_WIPE_L),
        new TransitionModel("Wipe Right", TRANSITION_WIPE_R),
        new TransitionModel("Zoom In", TRANSITION_ZOOM_IN),
        new TransitionModel("Zoom Out", TRANSITION_ZOOM_OUT),
        new TransitionModel("Spin", TRANSITION_ROTATE),
        new TransitionModel("Glitch", TRANSITION_GLITCH),
        new TransitionModel("Blur", TRANSITION_BLUR)
    };

    /**
     * Creates a configured QTransition instance.
     */
    public static QTransition createTransition(long templateId, int durationMs) {
        QTransition trans = new QTransition();
        trans.setDuration(durationMs);
        trans.setAnimatedCfg(QTransition.ANIMATED_CFG_TWO);
        return trans;
    }

    /**
     * Applies a transition to a clip in the storyboard at [clipIndex].
     */
    public static int applyTransition(QStoryboard storyboard, int clipIndex, long templateId, int durationMs) {
        if (storyboard == null) return QVEError.QERR_COMMON_JAVA_INVALID_PARAM;
        QClip clip = storyboard.getClip(clipIndex);
        if (clip == null) return QVEError.QERR_COMMON_JAVA_INVALID_PARAM;

        QTransition transition = createTransition(templateId, durationMs);
        int ret = clip.setTransition(transition);
        if (ret == 0) {
            storyboard.applyTrim();
            Log.d(TAG, "Applied transition 0x" + Long.toHexString(templateId) + " to clip " + clipIndex);
        }
        return ret;
    }

    /**
     * Removes transition from a clip.
     */
    public static int removeTransition(QStoryboard storyboard, int clipIndex) {
        if (storyboard == null) return QVEError.QERR_COMMON_JAVA_INVALID_PARAM;
        QClip clip = storyboard.getClip(clipIndex);
        if (clip == null) return QVEError.QERR_COMMON_JAVA_INVALID_PARAM;
        int ret = clip.setTransition(null);
        if (ret == 0) {
            storyboard.applyTrim();
        }
        return ret;
    }
}
