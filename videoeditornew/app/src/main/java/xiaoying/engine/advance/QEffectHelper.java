package xiaoying.engine.advance;

import android.util.Log;
import xiaoying.engine.QEngine;
import xiaoying.engine.base.QVEError;
import xiaoying.engine.clip.QClip;
import xiaoying.engine.clip.QEffect;
import xiaoying.engine.storyboard.QStoryboard;

/**
 * Advanced Java engine helper for filter effects, color grading, and shader overlays.
 */
public class QEffectHelper {
    private static final String TAG = "QEffectHelper";

    // Well-known Xiaoying image effect template IDs from assets
    public static final long EFFECT_VINTAGE = 0x0400000010000014L;
    public static final long EFFECT_BW      = 0x0400000010000015L;
    public static final long EFFECT_CINEMA  = 0x0400000010000016L;
    public static final long EFFECT_WARM    = 0x0400000010000017L;
    public static final long EFFECT_COOL    = 0x0400000010000018L;
    public static final long EFFECT_VIVID   = 0x0400000010000019L;

    public static class FilterModel {
        public final String name;
        public final long templateId;

        public FilterModel(String name, long templateId) {
            this.name = name;
            this.templateId = templateId;
        }
    }

    public static final FilterModel[] BUILTIN_FILTERS = new FilterModel[] {
        new FilterModel("Vintage", EFFECT_VINTAGE),
        new FilterModel("B&W", EFFECT_BW),
        new FilterModel("Cinema", EFFECT_CINEMA),
        new FilterModel("Warm", EFFECT_WARM),
        new FilterModel("Cool", EFFECT_COOL),
        new FilterModel("Vivid", EFFECT_VIVID)
    };

    /**
     * Instantiates a new QEffect filter using native QEffect.create().
     */
    public static QEffect createFilterEffect(QEngine engine, long templateId) {
        if (engine == null) return null;
        try {
            QEffect effect = new QEffect();
            // type=1 (TYPE_VIDEO_IE), trackType=2 (TRACK_TYPE_VIDEO), groupId=0, layerId=0
            int ret = effect.create(engine, QEffect.TYPE_VIDEO_IE, QEffect.TRACK_TYPE_VIDEO, 0, 0.0f);
            if (ret == 0) {
                effect.setProperty(QEffect.PROP_EFFECT_SUB_TEMPLATE_ID, Long.valueOf(templateId));
                return effect;
            }
            Log.e(TAG, "Failed to create QEffect: " + ret);
            return null;
        } catch (Throwable e) {
            Log.e(TAG, "createFilterEffect exception", e);
            return null;
        }
    }

    /**
     * Applies a filter effect to a specific QClip.
     */
    public static int applyFilterToClip(QClip clip, QEngine engine, long templateId) {
        if (clip == null || engine == null) return QVEError.QERR_COMMON_JAVA_INVALID_PARAM;
        QEffect effect = createFilterEffect(engine, templateId);
        if (effect == null) return QVEError.QERR_COMMON_JAVA_NOT_INIT;
        return clip.insertEffect(effect);
    }

    /**
     * Applies a global filter effect to the entire storyboard via its data clip.
     */
    public static int applyFilterToStoryboard(QStoryboard storyboard, QEngine engine, long templateId) {
        if (storyboard == null || engine == null) return QVEError.QERR_COMMON_JAVA_INVALID_PARAM;
        QClip dataClip = storyboard.getDataClip();
        if (dataClip == null) return QVEError.QERR_COMMON_JAVA_NOT_INIT;
        QEffect effect = createFilterEffect(engine, templateId);
        if (effect == null) return QVEError.QERR_COMMON_JAVA_NOT_INIT;
        return dataClip.insertEffect(effect);
    }
}
