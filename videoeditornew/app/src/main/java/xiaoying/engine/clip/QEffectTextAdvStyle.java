package xiaoying.engine.clip;

/* JADX INFO: loaded from: classes19.dex */
public class QEffectTextAdvStyle {
    public TextAdvanceFill fontFill = new TextAdvanceFill();
    public TextStrokeItem[] strokes = new TextStrokeItem[0];
    public TextShadowItem[] shadows = new TextShadowItem[0];
    public int index = 0;

    public static class TextAdvanceFill {
        public static final int FILL_IMAGE = 3;
        public static final int GRIENDT_COLOR = 2;
        public static final int PATH_STROKE = 1;
        public static final int PURE_COLOR = 0;
        public int fillType = 0;
        public float opacity = 1.0f;
        public MColorRGB fillColor = new MColorRGB(255, 255, 255);
        public float pathStrokeSize = 0.0f;
        public TextGradientStyle gradient = null;
        public String fillImagePath = null;
    }

    public static class TextBoardConfig {
        public boolean showBoard = false;
        public float boardRound = 0.0f;
        public TextAdvanceFill boardFill = new TextAdvanceFill();
        public int index = 0;
        public boolean enableBoardAutoFade = false;
    }

    public static class TextGradientPoint {
        public float position = 0.0f;
        public MColorRGB color = new MColorRGB(255, 255, 255);
    }

    public static class TextGradientStyle {
        public float angle = 0.0f;
        public float scale = 1.0f;
        public TextGradientPoint[] points = new TextGradientPoint[2];

        public TextGradientStyle() {
            TextGradientPoint textGradientPoint = new TextGradientPoint();
            textGradientPoint.position = 0.0f;
            textGradientPoint.color = new MColorRGB(0, 0, 0);
            this.points[0] = textGradientPoint;
            TextGradientPoint textGradientPoint2 = new TextGradientPoint();
            textGradientPoint2.position = 1.0f;
            textGradientPoint2.color = new MColorRGB(255, 255, 255);
            this.points[1] = textGradientPoint2;
        }
    }

    public static class TextShadowItem {
        public float opacity = 1.0f;
        public float size = 0.0f;
        public float spread = 0.0f;
        public float angle = 0.0f;
        public float distance = 0.0f;
        public MColorRGB color = new MColorRGB(255, 255, 255);
    }

    public static class TextStrokeItem {
        public float opacity = 1.0f;
        public float size = 0.0f;
        public MColorRGB color = new MColorRGB(255, 255, 255);
    }

    public static class MColorRGB {

                public int B;

                public int G;

                public int R;

        public MColorRGB() {
            this.R = 255;
            this.G = 255;
            this.B = 255;
        }

        public MColorRGB(int r10, int g10, int b10) {
            this.R = r10;
            this.G = g10;
            this.B = b10;
        }
    }
}
