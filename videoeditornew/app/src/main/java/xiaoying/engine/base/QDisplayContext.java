package xiaoying.engine.base;

import xiaoying.utils.QRect;

/* JADX INFO: loaded from: classes19.dex */
public class QDisplayContext {
    public static final int DISPLAY_ORIENTATION_LEFT_HANDED = 1;
    public static final int DISPLAY_ORIENTATION_NORMAL = 0;
    public static final int DISPLAY_ORIENTATION_REVERSAL = 3;
    public static final int DISPLAY_ORIENTATION_RIGHT_HANDED = 2;
    private static final int DISPLAY_ORIENTATION_TOTAL = 4;
    public static final int DISPLAY_ROTATION_180 = 180;
    public static final int DISPLAY_ROTATION_270 = 270;
    public static final int DISPLAY_ROTATION_90 = 90;
    public static final int DISPLAY_ROTATION_NONE = 0;
    public static final int QREND_TARGET_MASK_BUFFER = 2;
    public static final int QREND_TARGET_MASK_SCREEN = 1;
    public static final int RESAMPLE_MODE_FILL = 3;
    public static final int RESAMPLE_MODE_FITIN = 1;
    public static final int RESAMPLE_MODE_FITOUT = 2;
    public static final int RESAMPLE_MODE_UPSCALE_FITIN = 65537;
    public static final int RESAMPLE_MODE_UPSCALE_FITOUT = 65538;
    private boolean bNeedReCreateHolder;
    private int backgroundColor;
    private QRect clipRect;
    private int orientation;
    private int renderTarget;
    private int resampleMode;
    private int rotation;
    private QRect screenRect;
    private Object surfaceHolder;

    public QDisplayContext() {
        this.screenRect = null;
        this.clipRect = null;
        this.backgroundColor = 0;
        this.rotation = 0;
        this.orientation = 0;
        this.resampleMode = 65537;
        this.surfaceHolder = null;
        this.renderTarget = 0;
        this.bNeedReCreateHolder = false;
        this.screenRect = new QRect();
        this.clipRect = new QRect();
    }

    public int getBackgroundColor() {
        return this.backgroundColor;
    }

    public QRect getClipRect() {
        return this.clipRect;
    }

    public int getOrientation() {
        return this.orientation;
    }

    public int getResampleMode() {
        return this.resampleMode;
    }

    public int getRotation() {
        return this.rotation;
    }

    public QRect getScreenRect() {
        return this.screenRect;
    }

    public Object getSurfaceHolder() {
        return this.surfaceHolder;
    }

    public void setBackgroundColor(int backgroundColor) {
        this.backgroundColor = backgroundColor;
    }

    public void setClipRect(QRect clipRect) {
        if (clipRect == null) {
            return;
        }
        QRect qRect = this.clipRect;
        if (qRect == null) {
            this.clipRect = new QRect(clipRect);
        } else {
            qRect.set(clipRect.left, clipRect.top, clipRect.right, clipRect.bottom);
        }
    }

    public void setNeedReCreateHolderFlag(boolean flag) {
        this.bNeedReCreateHolder = flag;
    }

    public void setOrientation(int orientation) {
        if (orientation < 0 || orientation >= 4) {
            return;
        }
        this.orientation = orientation;
    }

    public void setResampleMode(int resampleMode) {
        if (resampleMode == 1 || resampleMode == 2 || resampleMode == 3 || resampleMode == 65537 || resampleMode == 65538) {
            this.resampleMode = resampleMode;
        }
    }

    public void setRotation(int rotation) {
        if (rotation == 0 || rotation == 90 || rotation == 180 || rotation == 270) {
            this.rotation = rotation;
        }
    }

    public void setScreenRect(QRect screenRect) {
        if (screenRect == null) {
            return;
        }
        QRect qRect = this.screenRect;
        if (qRect == null) {
            this.screenRect = new QRect(screenRect);
        } else {
            qRect.set(screenRect.left, screenRect.top, screenRect.right, screenRect.bottom);
        }
    }

    public void setSurfaceHolder(Object sh2) {
        this.surfaceHolder = sh2;
    }

    public QDisplayContext(QDisplayContext displayContext) {
        this.screenRect = null;
        this.clipRect = null;
        this.backgroundColor = 0;
        this.rotation = 0;
        this.orientation = 0;
        this.resampleMode = 65537;
        this.surfaceHolder = null;
        this.renderTarget = 0;
        this.bNeedReCreateHolder = false;
        this.screenRect = displayContext.screenRect;
        this.clipRect = displayContext.clipRect;
        this.backgroundColor = displayContext.backgroundColor;
        this.orientation = displayContext.orientation;
        this.rotation = displayContext.rotation;
        this.resampleMode = displayContext.resampleMode;
        this.renderTarget = displayContext.renderTarget;
        this.surfaceHolder = displayContext.surfaceHolder;
    }

    public QDisplayContext(QRect screenRect, QRect clipRect, int backgroundColor, int rotation, int orientation, int resampleMode, int renderTarget) {
        this.rotation = 0;
        this.orientation = 0;
        this.resampleMode = 65537;
        this.surfaceHolder = null;
        this.bNeedReCreateHolder = false;
        this.screenRect = screenRect;
        this.clipRect = clipRect;
        this.backgroundColor = backgroundColor;
        this.renderTarget = renderTarget;
        if (orientation >= 0 && orientation < 4) {
            this.orientation = orientation;
        } else {
            this.orientation = 0;
        }
        if (rotation != 0 && rotation != 90 && rotation != 180 && rotation != 270) {
            this.rotation = 0;
        } else {
            this.rotation = rotation;
        }
        if (resampleMode != 1 && resampleMode != 2 && resampleMode != 3 && resampleMode != 65537 && resampleMode != 65538) {
            this.resampleMode = 65537;
        } else {
            this.resampleMode = resampleMode;
        }
    }
}
