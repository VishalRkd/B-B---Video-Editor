package xiaoying.engine.clip;

/* JADX INFO: loaded from: classes19.dex */
public class QTransition {
    public static final int ANIMATED_CFG_AUTO = 4;
    public static final int ANIMATED_CFG_DURATION_UNCHANGE = 7;
    public static final int ANIMATED_CFG_LEFT = 1;
    public static final int ANIMATED_CFG_RIGHT = 2;
    public static final int ANIMATED_CFG_TWO = 3;
    public static final int ANIMATED_CFG_ZERO = 0;
    private int animatedCfg;
    private int cfgIndex;
    private int duration;
    private boolean setbyengine;
    private String template;

    public QTransition() {
        this.template = null;
        this.cfgIndex = -1;
        this.duration = 0;
        this.animatedCfg = 0;
        this.setbyengine = false;
    }

    public int getAnimatedCfg() {
        return this.animatedCfg;
    }

    public int getCfgIndex() {
        return this.cfgIndex;
    }

    public int getDuration() {
        return this.duration;
    }

    public String getTemplate() {
        return this.template;
    }

    public boolean isAutomatizm() {
        return this.setbyengine;
    }

    public void setAnimatedCfg(int animatedCfg) {
        this.animatedCfg = animatedCfg;
    }

    public void setAutomatizm(boolean setbyengine) {
        this.setbyengine = setbyengine;
    }

    public void setCfgIndex(int cfgIndex) {
        this.cfgIndex = cfgIndex;
    }

    public void setDuration(int duration) {
        this.duration = duration;
    }

    public void setTemplate(String template) {
        this.template = template;
    }

    public QTransition(String template, int duration, int animatedCfg, int cfgIndex) {
        this.template = template;
        this.duration = duration;
        this.animatedCfg = animatedCfg;
        this.setbyengine = false;
        this.cfgIndex = cfgIndex;
    }

    public QTransition(QTransition transition) {
        this.template = null;
        this.cfgIndex = -1;
        this.duration = 0;
        this.animatedCfg = 0;
        this.setbyengine = false;
        this.template = transition.template;
        this.duration = transition.duration;
        this.animatedCfg = transition.animatedCfg;
        this.setbyengine = transition.setbyengine;
        this.cfgIndex = transition.cfgIndex;
    }
}
