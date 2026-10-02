package xiaoying.engine.base;

/* JADX INFO: loaded from: classes19.dex */
public class QDrawLayerPaintPen {
    public static final int EU_DRAW_LINE_DOTTED = 1;
    public static final int EU_DRAW_LINE_NORMAL = 0;
    public static final int EU_DRAW_PAINT_BRUSH = 3;
    public static final int EU_DRAW_PAINT_ERASURE = 5;
    public static final int EU_DRAW_PAINT_MARK_PEN = 4;
    public static final int EU_DRAW_PAINT_PEN = 1;
    public static final int EU_DRAW_PAINT_PENCIL = 0;
    public static final int EU_DRAW_PAINT_WATER_PEN = 2;
    public int nType;
    public int nLineColor = -1;
    public int nLineType = 0;
    public float fLineThickness = 0.01f;
    public float fLightRadius = 0.02f;
    public float fDottedLine = 0.01f;
    public boolean bEnableLight = false;
    public int nLightColor = -1;
    public float fEdgeFeathering = 0.015f;
    public long templateID = 0;
}
