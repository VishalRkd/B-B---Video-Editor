package xiaoying.utils;

import androidx.core.view.InputDeviceCompat;

/* JADX INFO: loaded from: classes19.dex */
public class QTextDrawParam {
    public boolean isBold = false;
    public int alignment = 0;
    public float angle = 0.0f;
    public boolean isAutoMultiLine = true;
    public int textRegionTop = 0;
    public int textRegionLeft = 0;
    public int textRegionW = 0;
    public int textRegionH = 0;
    public int contentBGW = 0;
    public int contentBGH = 0;
    public int textLineRatio = 10000;
    public float DTextSize = 20.0f;
    public float DShadowBlurRadius = 0.0f;
    public float DShadowXShift = 0.0f;
    public float DShadowYShift = 0.0f;
    public int shadowColor = -8421505;
    public String auxiliaryFont = null;
    public int txtFillType = 1;
    public TxtFillConfig txtFillConfig = new TxtFillConfig();
    public int txtStrokeType = 1;
    public TxtStrokeConfig txtStrokeConfig = new TxtStrokeConfig();
    public boolean isItalic = false;

    public static class TxtFillConfig {
        public int color0 = -65536;
        public int color1 = -16776961;
        public int angle = 0;
    }

    public static class TxtStrokeConfig {
        public int color0 = InputDeviceCompat.SOURCE_ANY;
        public int color1 = -16711681;
        public int angle = 0;
        public float widthPercent = 0.0f;
    }
}
