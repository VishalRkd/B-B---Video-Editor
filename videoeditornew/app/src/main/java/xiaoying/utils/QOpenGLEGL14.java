package xiaoying.utils;

import android.app.ActivityManager;
import android.app.Application;
import android.content.Context;
import android.opengl.EGL14;
import android.opengl.EGLConfig;
import android.opengl.EGLContext;
import android.opengl.EGLDisplay;
import android.opengl.EGLSurface;
import android.opengl.EGLExt;
import android.view.Surface;


import xiaoying.engine.clip.QClip;

/* JADX INFO: loaded from: classes19.dex */
public class QOpenGLEGL14 {
    private static final int EGL_RECORDABLE_ANDROID = 12610;
    private static final int kANDROID_SDK_INT_18 = 18;
    private static final int kAndroid_SDK_INT_21 = 21;
    private static final int kAndroid_SDK_INT_24 = 24;
    private static final int kEGL_CONTEXT_MAJOR_VERSION_KHR = 12440;
    private static final int kEGL_CONTEXT_MINOR_VERSION_KHR = 12539;
    private static final String kEGL_EXT_gl_colorspace_display_p3 = "EGL_EXT_gl_colorspace_display_p3";
    private static final String kEGL_EXT_surface_SMPTE2086_metadata = "EGL_EXT_surface_SMPTE2086_metadata";
    private static final int kEGL_GL_COLORSPACE_BT2020_PQ_EXT = 13120;
    private static final int kEGL_GL_COLORSPACE_KHR = 12445;
    private static final String kEGL_KHR_create_context = "EGL_KHR_create_context";
    private static final int kEGL_METADATA_SCALING_EXT = 50000;
    private static final int kEGL_OPENGL_ES3_BIT_KHR = 64;
    private static final int kEGL_SMPTE2086_DISPLAY_PRIMARY_BX_EXT = 13125;
    private static final int kEGL_SMPTE2086_DISPLAY_PRIMARY_BY_EXT = 13126;
    private static final int kEGL_SMPTE2086_DISPLAY_PRIMARY_GX_EXT = 13123;
    private static final int kEGL_SMPTE2086_DISPLAY_PRIMARY_GY_EXT = 13124;
    private static final int kEGL_SMPTE2086_DISPLAY_PRIMARY_RX_EXT = 13121;
    private static final int kEGL_SMPTE2086_DISPLAY_PRIMARY_RY_EXT = 13122;
    private static final int kEGL_SMPTE2086_MAX_LUMINANCE_EXT = 13129;
    private static final int kEGL_SMPTE2086_MIN_LUMINANCE_EXT = 13130;
    private static final int kEGL_SMPTE2086_WHITE_POINT_X_EXT = 13127;
    private static final int kEGL_SMPTE2086_WHITE_POINT_Y_EXT = 13128;
    private static final String kEXT_gl_colorspace_bt2020 = "EXT_gl_colorspace_bt2020";
    protected EGLDisplay dpy = EGL14.EGL_NO_DISPLAY;
    protected EGLSurface surface = EGL14.EGL_NO_SURFACE;
    protected EGLContext context = EGL14.EGL_NO_CONTEXT;
    protected EGLConfig config = null;
    protected boolean mHDRDisplay = false;
    protected float mHDRMaxLuminance = 100.0f;
    protected float mHDRMinLuminance = 0.0f;
    protected float mHDRAvgLuminance = 0.0f;
    protected double mDeviceVersionNum = 2.0d;
    private final double kDoubleVersionEpsion = 0.01d;
    private final String TAG = "QOpenGLEGL14";

    public Object getConfig() {
        return this.config;
    }

    public Object getContext() {
        return this.context;
    }

    public synchronized void getDeviceSupportedMaxVersion(Context ctx) {
        try {
            Application application = (Application) Class.forName("android.app.ActivityThread").getMethod("currentApplication").invoke(null);
            if (application != null) {
                ctx = application.getBaseContext();
            }
        } catch (Exception e10) {
            e10.printStackTrace();
        }
        if (ctx == null) {
            return;
        }
        this.mDeviceVersionNum = Double.parseDouble(((ActivityManager) ctx.getSystemService("activity")).getDeviceConfigurationInfo().getGlEsVersion());
    }

    public Object getDisplay() {
        return this.dpy;
    }

    public synchronized int getOpenglVersion() {
        EGLContext eGLContext;
        EGLDisplay eGLDisplay = this.dpy;
        if (eGLDisplay != null && (eGLContext = this.context) != null) {
            int[] iArr = new int[2];
            EGL14.eglQueryContext(eGLDisplay, eGLContext, kEGL_CONTEXT_MAJOR_VERSION_KHR, iArr, 0);
            return iArr[0];
        }
        return 0;
    }

    public Object getSurface() {
        return this.surface;
    }

    /* JADX WARN: Code duplicated, block: B:102:0x0307 A[Catch: all -> 0x002e, TRY_LEAVE, TryCatch #0 {all -> 0x002e, blocks: (B:4:0x0005, B:6:0x001c, B:9:0x0031, B:13:0x0050, B:16:0x0064, B:19:0x0070, B:25:0x008b, B:27:0x00c9, B:28:0x00d1, B:31:0x00da, B:33:0x00f5, B:35:0x00f9, B:37:0x0111, B:46:0x015e, B:48:0x0176, B:51:0x017d, B:59:0x019c, B:62:0x01bc, B:64:0x01c1, B:66:0x01c7, B:72:0x01d9, B:75:0x01f5, B:76:0x01f8, B:79:0x0200, B:83:0x021d, B:84:0x0237, B:86:0x023d, B:90:0x0250, B:92:0x0256, B:95:0x025c, B:97:0x0272, B:100:0x0301, B:102:0x0307, B:105:0x0318, B:98:0x02ec, B:99:0x02f7, B:40:0x012c, B:41:0x0142), top: B:110:0x0005 }] */
    /* JADX WARN: Code duplicated, block: B:105:0x0318 A[Catch: all -> 0x002e, TRY_ENTER, TRY_LEAVE, TryCatch #0 {all -> 0x002e, blocks: (B:4:0x0005, B:6:0x001c, B:9:0x0031, B:13:0x0050, B:16:0x0064, B:19:0x0070, B:25:0x008b, B:27:0x00c9, B:28:0x00d1, B:31:0x00da, B:33:0x00f5, B:35:0x00f9, B:37:0x0111, B:46:0x015e, B:48:0x0176, B:51:0x017d, B:59:0x019c, B:62:0x01bc, B:64:0x01c1, B:66:0x01c7, B:72:0x01d9, B:75:0x01f5, B:76:0x01f8, B:79:0x0200, B:83:0x021d, B:84:0x0237, B:86:0x023d, B:90:0x0250, B:92:0x0256, B:95:0x025c, B:97:0x0272, B:100:0x0301, B:102:0x0307, B:105:0x0318, B:98:0x02ec, B:99:0x02f7, B:40:0x012c, B:41:0x0142), top: B:110:0x0005 }] */
    /* JADX WARN: Code duplicated, block: B:112:0x01bb A[SYNTHETIC] */
    /* JADX WARN: Code duplicated, block: B:115:0x01f4 A[SYNTHETIC] */
    /* JADX WARN: Code duplicated, block: B:44:0x015c A[DONT_GENERATE] */
    /* JADX WARN: Code duplicated, block: B:46:0x015e A[Catch: all -> 0x002e, TRY_ENTER, TryCatch #0 {all -> 0x002e, blocks: (B:4:0x0005, B:6:0x001c, B:9:0x0031, B:13:0x0050, B:16:0x0064, B:19:0x0070, B:25:0x008b, B:27:0x00c9, B:28:0x00d1, B:31:0x00da, B:33:0x00f5, B:35:0x00f9, B:37:0x0111, B:46:0x015e, B:48:0x0176, B:51:0x017d, B:59:0x019c, B:62:0x01bc, B:64:0x01c1, B:66:0x01c7, B:72:0x01d9, B:75:0x01f5, B:76:0x01f8, B:79:0x0200, B:83:0x021d, B:84:0x0237, B:86:0x023d, B:90:0x0250, B:92:0x0256, B:95:0x025c, B:97:0x0272, B:100:0x0301, B:102:0x0307, B:105:0x0318, B:98:0x02ec, B:99:0x02f7, B:40:0x012c, B:41:0x0142), top: B:110:0x0005 }] */
    /* JADX WARN: Code duplicated, block: B:48:0x0176 A[Catch: all -> 0x002e, TryCatch #0 {all -> 0x002e, blocks: (B:4:0x0005, B:6:0x001c, B:9:0x0031, B:13:0x0050, B:16:0x0064, B:19:0x0070, B:25:0x008b, B:27:0x00c9, B:28:0x00d1, B:31:0x00da, B:33:0x00f5, B:35:0x00f9, B:37:0x0111, B:46:0x015e, B:48:0x0176, B:51:0x017d, B:59:0x019c, B:62:0x01bc, B:64:0x01c1, B:66:0x01c7, B:72:0x01d9, B:75:0x01f5, B:76:0x01f8, B:79:0x0200, B:83:0x021d, B:84:0x0237, B:86:0x023d, B:90:0x0250, B:92:0x0256, B:95:0x025c, B:97:0x0272, B:100:0x0301, B:102:0x0307, B:105:0x0318, B:98:0x02ec, B:99:0x02f7, B:40:0x012c, B:41:0x0142), top: B:110:0x0005 }] */
    /* JADX WARN: Code duplicated, block: B:49:0x0179  */
    /* JADX WARN: Code duplicated, block: B:51:0x017d A[Catch: all -> 0x002e, TryCatch #0 {all -> 0x002e, blocks: (B:4:0x0005, B:6:0x001c, B:9:0x0031, B:13:0x0050, B:16:0x0064, B:19:0x0070, B:25:0x008b, B:27:0x00c9, B:28:0x00d1, B:31:0x00da, B:33:0x00f5, B:35:0x00f9, B:37:0x0111, B:46:0x015e, B:48:0x0176, B:51:0x017d, B:59:0x019c, B:62:0x01bc, B:64:0x01c1, B:66:0x01c7, B:72:0x01d9, B:75:0x01f5, B:76:0x01f8, B:79:0x0200, B:83:0x021d, B:84:0x0237, B:86:0x023d, B:90:0x0250, B:92:0x0256, B:95:0x025c, B:97:0x0272, B:100:0x0301, B:102:0x0307, B:105:0x0318, B:98:0x02ec, B:99:0x02f7, B:40:0x012c, B:41:0x0142), top: B:110:0x0005 }] */
    /* JADX WARN: Code duplicated, block: B:53:0x018a  */
    /* JADX WARN: Code duplicated, block: B:54:0x018d  */
    /* JADX WARN: Code duplicated, block: B:57:0x0198  */
    /* JADX WARN: Code duplicated, block: B:59:0x019c A[Catch: all -> 0x002e, TryCatch #0 {all -> 0x002e, blocks: (B:4:0x0005, B:6:0x001c, B:9:0x0031, B:13:0x0050, B:16:0x0064, B:19:0x0070, B:25:0x008b, B:27:0x00c9, B:28:0x00d1, B:31:0x00da, B:33:0x00f5, B:35:0x00f9, B:37:0x0111, B:46:0x015e, B:48:0x0176, B:51:0x017d, B:59:0x019c, B:62:0x01bc, B:64:0x01c1, B:66:0x01c7, B:72:0x01d9, B:75:0x01f5, B:76:0x01f8, B:79:0x0200, B:83:0x021d, B:84:0x0237, B:86:0x023d, B:90:0x0250, B:92:0x0256, B:95:0x025c, B:97:0x0272, B:100:0x0301, B:102:0x0307, B:105:0x0318, B:98:0x02ec, B:99:0x02f7, B:40:0x012c, B:41:0x0142), top: B:110:0x0005 }] */
    /* JADX WARN: Code duplicated, block: B:62:0x01bc A[Catch: all -> 0x002e, LOOP:0: B:58:0x019a->B:62:0x01bc, LOOP_END, TryCatch #0 {all -> 0x002e, blocks: (B:4:0x0005, B:6:0x001c, B:9:0x0031, B:13:0x0050, B:16:0x0064, B:19:0x0070, B:25:0x008b, B:27:0x00c9, B:28:0x00d1, B:31:0x00da, B:33:0x00f5, B:35:0x00f9, B:37:0x0111, B:46:0x015e, B:48:0x0176, B:51:0x017d, B:59:0x019c, B:62:0x01bc, B:64:0x01c1, B:66:0x01c7, B:72:0x01d9, B:75:0x01f5, B:76:0x01f8, B:79:0x0200, B:83:0x021d, B:84:0x0237, B:86:0x023d, B:90:0x0250, B:92:0x0256, B:95:0x025c, B:97:0x0272, B:100:0x0301, B:102:0x0307, B:105:0x0318, B:98:0x02ec, B:99:0x02f7, B:40:0x012c, B:41:0x0142), top: B:110:0x0005 }] */
    /* JADX WARN: Code duplicated, block: B:63:0x01bf  */
    /* JADX WARN: Code duplicated, block: B:66:0x01c7 A[Catch: all -> 0x002e, TryCatch #0 {all -> 0x002e, blocks: (B:4:0x0005, B:6:0x001c, B:9:0x0031, B:13:0x0050, B:16:0x0064, B:19:0x0070, B:25:0x008b, B:27:0x00c9, B:28:0x00d1, B:31:0x00da, B:33:0x00f5, B:35:0x00f9, B:37:0x0111, B:46:0x015e, B:48:0x0176, B:51:0x017d, B:59:0x019c, B:62:0x01bc, B:64:0x01c1, B:66:0x01c7, B:72:0x01d9, B:75:0x01f5, B:76:0x01f8, B:79:0x0200, B:83:0x021d, B:84:0x0237, B:86:0x023d, B:90:0x0250, B:92:0x0256, B:95:0x025c, B:97:0x0272, B:100:0x0301, B:102:0x0307, B:105:0x0318, B:98:0x02ec, B:99:0x02f7, B:40:0x012c, B:41:0x0142), top: B:110:0x0005 }] */
    /* JADX WARN: Code duplicated, block: B:68:0x01d2  */
    /* JADX WARN: Code duplicated, block: B:70:0x01d5  */
    /* JADX WARN: Code duplicated, block: B:72:0x01d9 A[Catch: all -> 0x002e, TryCatch #0 {all -> 0x002e, blocks: (B:4:0x0005, B:6:0x001c, B:9:0x0031, B:13:0x0050, B:16:0x0064, B:19:0x0070, B:25:0x008b, B:27:0x00c9, B:28:0x00d1, B:31:0x00da, B:33:0x00f5, B:35:0x00f9, B:37:0x0111, B:46:0x015e, B:48:0x0176, B:51:0x017d, B:59:0x019c, B:62:0x01bc, B:64:0x01c1, B:66:0x01c7, B:72:0x01d9, B:75:0x01f5, B:76:0x01f8, B:79:0x0200, B:83:0x021d, B:84:0x0237, B:86:0x023d, B:90:0x0250, B:92:0x0256, B:95:0x025c, B:97:0x0272, B:100:0x0301, B:102:0x0307, B:105:0x0318, B:98:0x02ec, B:99:0x02f7, B:40:0x012c, B:41:0x0142), top: B:110:0x0005 }] */
    /* JADX WARN: Code duplicated, block: B:75:0x01f5 A[Catch: all -> 0x002e, LOOP:1: B:69:0x01d3->B:75:0x01f5, LOOP_END, TryCatch #0 {all -> 0x002e, blocks: (B:4:0x0005, B:6:0x001c, B:9:0x0031, B:13:0x0050, B:16:0x0064, B:19:0x0070, B:25:0x008b, B:27:0x00c9, B:28:0x00d1, B:31:0x00da, B:33:0x00f5, B:35:0x00f9, B:37:0x0111, B:46:0x015e, B:48:0x0176, B:51:0x017d, B:59:0x019c, B:62:0x01bc, B:64:0x01c1, B:66:0x01c7, B:72:0x01d9, B:75:0x01f5, B:76:0x01f8, B:79:0x0200, B:83:0x021d, B:84:0x0237, B:86:0x023d, B:90:0x0250, B:92:0x0256, B:95:0x025c, B:97:0x0272, B:100:0x0301, B:102:0x0307, B:105:0x0318, B:98:0x02ec, B:99:0x02f7, B:40:0x012c, B:41:0x0142), top: B:110:0x0005 }] */
    /* JADX WARN: Code duplicated, block: B:81:0x021b A[DONT_GENERATE] */
    /* JADX WARN: Code duplicated, block: B:83:0x021d A[Catch: all -> 0x002e, TRY_ENTER, TryCatch #0 {all -> 0x002e, blocks: (B:4:0x0005, B:6:0x001c, B:9:0x0031, B:13:0x0050, B:16:0x0064, B:19:0x0070, B:25:0x008b, B:27:0x00c9, B:28:0x00d1, B:31:0x00da, B:33:0x00f5, B:35:0x00f9, B:37:0x0111, B:46:0x015e, B:48:0x0176, B:51:0x017d, B:59:0x019c, B:62:0x01bc, B:64:0x01c1, B:66:0x01c7, B:72:0x01d9, B:75:0x01f5, B:76:0x01f8, B:79:0x0200, B:83:0x021d, B:84:0x0237, B:86:0x023d, B:90:0x0250, B:92:0x0256, B:95:0x025c, B:97:0x0272, B:100:0x0301, B:102:0x0307, B:105:0x0318, B:98:0x02ec, B:99:0x02f7, B:40:0x012c, B:41:0x0142), top: B:110:0x0005 }] */
    /* JADX WARN: Code duplicated, block: B:86:0x023d A[Catch: all -> 0x002e, TRY_LEAVE, TryCatch #0 {all -> 0x002e, blocks: (B:4:0x0005, B:6:0x001c, B:9:0x0031, B:13:0x0050, B:16:0x0064, B:19:0x0070, B:25:0x008b, B:27:0x00c9, B:28:0x00d1, B:31:0x00da, B:33:0x00f5, B:35:0x00f9, B:37:0x0111, B:46:0x015e, B:48:0x0176, B:51:0x017d, B:59:0x019c, B:62:0x01bc, B:64:0x01c1, B:66:0x01c7, B:72:0x01d9, B:75:0x01f5, B:76:0x01f8, B:79:0x0200, B:83:0x021d, B:84:0x0237, B:86:0x023d, B:90:0x0250, B:92:0x0256, B:95:0x025c, B:97:0x0272, B:100:0x0301, B:102:0x0307, B:105:0x0318, B:98:0x02ec, B:99:0x02f7, B:40:0x012c, B:41:0x0142), top: B:110:0x0005 }] */
    /* JADX WARN: Code duplicated, block: B:89:0x024e  */
    /* JADX WARN: Code duplicated, block: B:92:0x0256 A[Catch: all -> 0x002e, TryCatch #0 {all -> 0x002e, blocks: (B:4:0x0005, B:6:0x001c, B:9:0x0031, B:13:0x0050, B:16:0x0064, B:19:0x0070, B:25:0x008b, B:27:0x00c9, B:28:0x00d1, B:31:0x00da, B:33:0x00f5, B:35:0x00f9, B:37:0x0111, B:46:0x015e, B:48:0x0176, B:51:0x017d, B:59:0x019c, B:62:0x01bc, B:64:0x01c1, B:66:0x01c7, B:72:0x01d9, B:75:0x01f5, B:76:0x01f8, B:79:0x0200, B:83:0x021d, B:84:0x0237, B:86:0x023d, B:90:0x0250, B:92:0x0256, B:95:0x025c, B:97:0x0272, B:100:0x0301, B:102:0x0307, B:105:0x0318, B:98:0x02ec, B:99:0x02f7, B:40:0x012c, B:41:0x0142), top: B:110:0x0005 }] */
    /* JADX WARN: Code duplicated, block: B:98:0x02ec A[Catch: all -> 0x002e, TryCatch #0 {all -> 0x002e, blocks: (B:4:0x0005, B:6:0x001c, B:9:0x0031, B:13:0x0050, B:16:0x0064, B:19:0x0070, B:25:0x008b, B:27:0x00c9, B:28:0x00d1, B:31:0x00da, B:33:0x00f5, B:35:0x00f9, B:37:0x0111, B:46:0x015e, B:48:0x0176, B:51:0x017d, B:59:0x019c, B:62:0x01bc, B:64:0x01c1, B:66:0x01c7, B:72:0x01d9, B:75:0x01f5, B:76:0x01f8, B:79:0x0200, B:83:0x021d, B:84:0x0237, B:86:0x023d, B:90:0x0250, B:92:0x0256, B:95:0x025c, B:97:0x0272, B:100:0x0301, B:102:0x0307, B:105:0x0318, B:98:0x02ec, B:99:0x02f7, B:40:0x012c, B:41:0x0142), top: B:110:0x0005 }] */
    /* JADX WARN: Code duplicated, block: B:99:0x02f7 A[Catch: all -> 0x002e, TryCatch #0 {all -> 0x002e, blocks: (B:4:0x0005, B:6:0x001c, B:9:0x0031, B:13:0x0050, B:16:0x0064, B:19:0x0070, B:25:0x008b, B:27:0x00c9, B:28:0x00d1, B:31:0x00da, B:33:0x00f5, B:35:0x00f9, B:37:0x0111, B:46:0x015e, B:48:0x0176, B:51:0x017d, B:59:0x019c, B:62:0x01bc, B:64:0x01c1, B:66:0x01c7, B:72:0x01d9, B:75:0x01f5, B:76:0x01f8, B:79:0x0200, B:83:0x021d, B:84:0x0237, B:86:0x023d, B:90:0x0250, B:92:0x0256, B:95:0x025c, B:97:0x0272, B:100:0x0301, B:102:0x0307, B:105:0x0318, B:98:0x02ec, B:99:0x02f7, B:40:0x012c, B:41:0x0142), top: B:110:0x0005 }] */
    /* JADX WARN: Instruction removed from duplicated block: B:44:0x015c, please report this as an issue */
    /* JADX WARN: Instruction removed from duplicated block: B:81:0x021b, please report this as an issue */
    public synchronized boolean initOpenGL(Surface viewSurface, int[] userConfig, EGLContext sharecontext) {
        int i10;
        boolean zEglChooseConfig;
        char c10 = 4;
        int[] iArr;
        EGLContext eGLContext;
        EGLConfig[] eGLConfigArr;
        int[] iArr2;
        EGLSurface eGLSurface;
        EGLContext eGLContext2;
        int i11;
        EGLContext eGLContextEglCreateContext;
        double d10;
        int i12;
        EGLContext eGLContextEglCreateContext2;
        try {
            StringBuilder sb2 = new StringBuilder();
            sb2.append("initOpenGL this=");
            sb2.append(hashCode());
            getDeviceSupportedMaxVersion(null);
            if (viewSurface != null) {
                StringBuilder sb3 = new StringBuilder();
                sb3.append("viewSurface=");
                sb3.append(viewSurface.hashCode());
            }
            int[] iArr3 = {12375, 1024, 12374, 1024, QClip.PROP_CLIP_INVERSE_PLAY_VIDEO_FLAG};
            EGLDisplay eGLDisplayEglGetDisplay = EGL14.eglGetDisplay(0);
            this.dpy = eGLDisplayEglGetDisplay;
            int[] iArr4 = new int[2];
            boolean zEglInitialize = EGL14.eglInitialize(eGLDisplayEglGetDisplay, iArr4, 0, iArr4, 1);
            if (!zEglInitialize) {
                return zEglInitialize;
            }
            String strEglQueryString = EGL14.eglQueryString(this.dpy, QClip.PROP_CLIP_DISPLAY_3D_TRANSFROM);
            boolean zContains = strEglQueryString.contains(kEGL_KHR_create_context);
            if (iArr4[1] < 4) {
                zContains = false;
            }
            if (this.mDeviceVersionNum < 2.99d) {
                zContains = false;
            }
            boolean z10 = strEglQueryString.contains(kEXT_gl_colorspace_bt2020) && strEglQueryString.contains(kEGL_EXT_gl_colorspace_display_p3) && strEglQueryString.contains(kEGL_EXT_surface_SMPTE2086_metadata);
            int[] iArr5 = {12324, 8, 12323, 8, 12322, 8, 12321, 8, 12352, 4, EGL_RECORDABLE_ANDROID, 1, 12344};
            if (this.mHDRDisplay) {
                iArr5[1] = 10;
                iArr5[3] = 10;
                iArr5[5] = 10;
                iArr5[7] = 2;
            }
            EGLConfig[] eGLConfigArr2 = new EGLConfig[1];
            int[] iArr6 = new int[1];
            if (userConfig != null) {
                i10 = 5;
                zEglChooseConfig = EGL14.eglChooseConfig(this.dpy, userConfig, 0, eGLConfigArr2, 0, 1, iArr6, 0);
            } else {
                if (zContains) {
                    iArr5[9] = 64;
                    i10 = 5;
                    zEglChooseConfig = EGL14.eglChooseConfig(this.dpy, iArr5, 0, eGLConfigArr2, 0, 1, iArr6, 0);
                    if (!zEglChooseConfig && this.mHDRDisplay) {
                        iArr5[11] = 0;
                        zEglChooseConfig = EGL14.eglChooseConfig(this.dpy, iArr5, 0, eGLConfigArr2, 0, 1, iArr6, 0);
                    }
                    if (zEglChooseConfig) {
                        c10 = '@';
                    } else {
                        iArr5[9] = 4;
                        zEglChooseConfig = EGL14.eglChooseConfig(this.dpy, iArr5, 0, eGLConfigArr2, 0, 1, iArr6, 0);
                    }
                    if (!zEglChooseConfig) {
                        return zEglChooseConfig;
                    }
                    this.config = eGLConfigArr2[0];
                    iArr = new int[i10];
                    iArr[0] = kEGL_CONTEXT_MAJOR_VERSION_KHR;
                    iArr[1] = 2;
                    iArr[2] = 12344;
                    iArr[3] = 12344;
                    iArr[4] = 12344;
                    if (sharecontext == null) {
                        eGLContext = EGL14.EGL_NO_CONTEXT;
                    } else {
                        eGLContext = sharecontext;
                    }
                    if (zContains) {
                        eGLConfigArr = eGLConfigArr2;
                        d10 = this.mDeviceVersionNum;
                        if (d10 > 3.0900000000000003d) {
                            i12 = 1;
                        } else {
                            i12 = 0;
                        }
                        if (d10 > 3.1900000000000004d) {
                            i12 = 2;
                        }
                        while (i12 >= 0) {
                            iArr[0] = kEGL_CONTEXT_MAJOR_VERSION_KHR;
                            iArr[1] = 3;
                            iArr[2] = kEGL_CONTEXT_MINOR_VERSION_KHR;
                            iArr[3] = i12;
                            iArr[4] = 12344;
                            eGLContextEglCreateContext2 = EGL14.eglCreateContext(this.dpy, this.config, eGLContext, iArr, 0);
                            this.context = eGLContextEglCreateContext2;
                            if (EGL14.EGL_NO_CONTEXT != eGLContextEglCreateContext2) {
                                break;
                            }
                            i12--;
                        }
                    } else {
                        eGLConfigArr = eGLConfigArr2;
                    }
                    if (EGL14.EGL_NO_CONTEXT == this.context) {
                        if (this.mDeviceVersionNum < 2.99d) {
                            i11 = 2;
                        } else {
                            i11 = 3;
                        }
                        while (i11 >= 2) {
                            iArr[0] = kEGL_CONTEXT_MAJOR_VERSION_KHR;
                            iArr[1] = i11;
                            iArr[2] = 12344;
                            iArr[3] = 12344;
                            iArr[4] = 12344;
                            eGLContextEglCreateContext = EGL14.eglCreateContext(this.dpy, this.config, eGLContext, iArr, 0);
                            this.context = eGLContextEglCreateContext;
                            if (EGL14.EGL_NO_CONTEXT != eGLContextEglCreateContext) {
                                break;
                            }
                            i11--;
                        }
                    }
                    if (EGL14.EGL_NO_CONTEXT == this.context && 4 != c10) {
                        iArr5[9] = 4;
                        eGLContext2 = eGLContext;
                        if (!EGL14.eglChooseConfig(this.dpy, iArr5, 0, eGLConfigArr, 0, 1, iArr6, 0)) {
                            return false;
                        }
                        EGLConfig eGLConfig = eGLConfigArr[0];
                        this.config = eGLConfig;
                        iArr[0] = kEGL_CONTEXT_MAJOR_VERSION_KHR;
                        iArr[1] = 2;
                        iArr[2] = 12344;
                        iArr[3] = 12344;
                        iArr[4] = 12344;
                        this.context = EGL14.eglCreateContext(this.dpy, eGLConfig, eGLContext2, iArr, 0);
                    }
                    if (this.context == EGL14.EGL_NO_CONTEXT) {
                        StringBuilder sb4 = new StringBuilder();
                        sb4.append("init, ERROR context = ");
                        sb4.append(this.context);
                        return false;
                    }
                    iArr2 = new int[]{QClip.PROP_CLIP_INVERSE_PLAY_VIDEO_FLAG};
                    if (viewSurface != null) {
                        this.surface = EGL14.eglCreatePbufferSurface(this.dpy, eGLConfigArr[0], iArr3, 0);
                    } else if (this.mHDRDisplay || !z10) {
                        this.surface = EGL14.eglCreateWindowSurface(this.dpy, eGLConfigArr[0], viewSurface, iArr2, 0);
                    } else {
                        EGLSurface eGLSurfaceEglCreateWindowSurface = EGL14.eglCreateWindowSurface(this.dpy, eGLConfigArr[0], viewSurface, new int[]{kEGL_GL_COLORSPACE_KHR, kEGL_GL_COLORSPACE_BT2020_PQ_EXT, QClip.PROP_CLIP_INVERSE_PLAY_VIDEO_FLAG}, 0);
                        this.surface = eGLSurfaceEglCreateWindowSurface;
                        if (EGL14.EGL_NO_SURFACE != eGLSurfaceEglCreateWindowSurface) {
                            EGL14.eglSurfaceAttrib(this.dpy, eGLSurfaceEglCreateWindowSurface, kEGL_SMPTE2086_DISPLAY_PRIMARY_RX_EXT, 35400);
                            EGL14.eglSurfaceAttrib(this.dpy, this.surface, kEGL_SMPTE2086_DISPLAY_PRIMARY_RY_EXT, 14600);
                            EGL14.eglSurfaceAttrib(this.dpy, this.surface, kEGL_SMPTE2086_DISPLAY_PRIMARY_GX_EXT, 8500);
                            EGL14.eglSurfaceAttrib(this.dpy, this.surface, kEGL_SMPTE2086_DISPLAY_PRIMARY_GY_EXT, 39850);
                            EGL14.eglSurfaceAttrib(this.dpy, this.surface, kEGL_SMPTE2086_DISPLAY_PRIMARY_BX_EXT, 6550);
                            EGL14.eglSurfaceAttrib(this.dpy, this.surface, kEGL_SMPTE2086_DISPLAY_PRIMARY_BY_EXT, 2300);
                            EGL14.eglSurfaceAttrib(this.dpy, this.surface, kEGL_SMPTE2086_WHITE_POINT_X_EXT, 15635);
                            EGL14.eglSurfaceAttrib(this.dpy, this.surface, kEGL_SMPTE2086_WHITE_POINT_Y_EXT, 16450);
                            EGL14.eglSurfaceAttrib(this.dpy, this.surface, kEGL_SMPTE2086_MAX_LUMINANCE_EXT, (int) ((this.mHDRMaxLuminance * 50000.0f) + 0.5f));
                            EGL14.eglSurfaceAttrib(this.dpy, this.surface, kEGL_SMPTE2086_MIN_LUMINANCE_EXT, (int) ((this.mHDRMinLuminance * 50000.0f) + 0.5f));
                        }
                    }
                    eGLSurface = this.surface;
                    if (eGLSurface == EGL14.EGL_NO_SURFACE) {
                        return EGL14.eglMakeCurrent(this.dpy, eGLSurface, eGLSurface, this.context);
                    }
                    StringBuilder sb5 = new StringBuilder();
                    sb5.append("init, ERROR buffer surface = ");
                    sb5.append(this.surface);
                    return false;
                }
                i10 = 5;
                zEglChooseConfig = EGL14.eglChooseConfig(this.dpy, iArr5, 0, eGLConfigArr2, 0, 1, iArr6, 0);
            }
            c10 = 4;
            if (!zEglChooseConfig) {
                return zEglChooseConfig;
            }
            this.config = eGLConfigArr2[0];
            iArr = new int[i10];
            iArr[0] = kEGL_CONTEXT_MAJOR_VERSION_KHR;
            iArr[1] = 2;
            iArr[2] = 12344;
            iArr[3] = 12344;
            iArr[4] = 12344;
            if (sharecontext == null) {
                eGLContext = EGL14.EGL_NO_CONTEXT;
            } else {
                eGLContext = sharecontext;
            }
            if (zContains) {
                eGLConfigArr = eGLConfigArr2;
                d10 = this.mDeviceVersionNum;
                if (d10 > 3.0900000000000003d) {
                    i12 = 1;
                } else {
                    i12 = 0;
                }
                if (d10 > 3.1900000000000004d) {
                    i12 = 2;
                }
                while (i12 >= 0) {
                    iArr[0] = kEGL_CONTEXT_MAJOR_VERSION_KHR;
                    iArr[1] = 3;
                    iArr[2] = kEGL_CONTEXT_MINOR_VERSION_KHR;
                    iArr[3] = i12;
                    iArr[4] = 12344;
                    eGLContextEglCreateContext2 = EGL14.eglCreateContext(this.dpy, this.config, eGLContext, iArr, 0);
                    this.context = eGLContextEglCreateContext2;
                    if (EGL14.EGL_NO_CONTEXT != eGLContextEglCreateContext2) {
                        break;
                    }
                    i12--;
                }
            } else {
                eGLConfigArr = eGLConfigArr2;
            }
            if (EGL14.EGL_NO_CONTEXT == this.context) {
                if (this.mDeviceVersionNum < 2.99d) {
                    i11 = 2;
                } else {
                    i11 = 3;
                }
                while (i11 >= 2) {
                    iArr[0] = kEGL_CONTEXT_MAJOR_VERSION_KHR;
                    iArr[1] = i11;
                    iArr[2] = 12344;
                    iArr[3] = 12344;
                    iArr[4] = 12344;
                    eGLContextEglCreateContext = EGL14.eglCreateContext(this.dpy, this.config, eGLContext, iArr, 0);
                    this.context = eGLContextEglCreateContext;
                    if (EGL14.EGL_NO_CONTEXT != eGLContextEglCreateContext) {
                        break;
                    }
                    i11--;
                }
            }
            if (EGL14.EGL_NO_CONTEXT == this.context) {
                iArr5[9] = 4;
                eGLContext2 = eGLContext;
                if (!EGL14.eglChooseConfig(this.dpy, iArr5, 0, eGLConfigArr, 0, 1, iArr6, 0)) {
                    return false;
                }
                EGLConfig eGLConfig2 = eGLConfigArr[0];
                this.config = eGLConfig2;
                iArr[0] = kEGL_CONTEXT_MAJOR_VERSION_KHR;
                iArr[1] = 2;
                iArr[2] = 12344;
                iArr[3] = 12344;
                iArr[4] = 12344;
                this.context = EGL14.eglCreateContext(this.dpy, eGLConfig2, eGLContext2, iArr, 0);
            }
            if (this.context == EGL14.EGL_NO_CONTEXT) {
                StringBuilder sb6 = new StringBuilder();
                sb6.append("init, ERROR context = ");
                sb6.append(this.context);
                return false;
            }
            iArr2 = new int[]{QClip.PROP_CLIP_INVERSE_PLAY_VIDEO_FLAG};
            if (viewSurface != null) {
                this.surface = EGL14.eglCreatePbufferSurface(this.dpy, eGLConfigArr[0], iArr3, 0);
            } else if (this.mHDRDisplay) {
                this.surface = EGL14.eglCreateWindowSurface(this.dpy, eGLConfigArr[0], viewSurface, iArr2, 0);
            } else {
                this.surface = EGL14.eglCreateWindowSurface(this.dpy, eGLConfigArr[0], viewSurface, iArr2, 0);
            }
            eGLSurface = this.surface;
            if (eGLSurface == EGL14.EGL_NO_SURFACE) {
                return EGL14.eglMakeCurrent(this.dpy, eGLSurface, eGLSurface, this.context);
            }
            StringBuilder sb7 = new StringBuilder();
            sb7.append("init, ERROR buffer surface = ");
            sb7.append(this.surface);
            return false;
        } catch (Throwable th2) {
            throw th2;
        }
    }

    public synchronized boolean resume(Surface viewSurface) {
        EGLSurface eGLSurface = this.surface;
        if (eGLSurface != null && eGLSurface != EGL14.EGL_NO_SURFACE) {
            return true;
        }
        if (viewSurface != null) {
            this.surface = EGL14.eglCreateWindowSurface(this.dpy, this.config, viewSurface, new int[]{QClip.PROP_CLIP_INVERSE_PLAY_VIDEO_FLAG}, 0);
            if (this.surface == EGL14.EGL_NO_SURFACE) {
                StringBuilder sb2 = new StringBuilder();
                sb2.append("ERROR window surface = ");
                sb2.append(this.surface);
            }
        }
        EGLDisplay eGLDisplay = this.dpy;
        EGLSurface eGLSurface2 = this.surface;
        return EGL14.eglMakeCurrent(eGLDisplay, eGLSurface2, eGLSurface2, this.context);
    }

    public synchronized void setHDRParam(boolean enableHDR, float minLum, float maxLum, float avgLum) {
        this.mHDRDisplay = enableHDR;
        this.mHDRMinLuminance = minLum;
        this.mHDRMaxLuminance = maxLum;
        this.mHDRAvgLuminance = avgLum;
    }

    public synchronized boolean setPresentTime(long presentTime) {
        return EGLExt.eglPresentationTimeANDROID(this.dpy, this.surface, presentTime);
    }

    public synchronized void suspend() {
        EGLDisplay eGLDisplay = this.dpy;
        EGLSurface eGLSurface = EGL14.EGL_NO_SURFACE;
        EGL14.eglMakeCurrent(eGLDisplay, eGLSurface, eGLSurface, EGL14.EGL_NO_CONTEXT);
        EGLSurface eGLSurface2 = this.surface;
        if (eGLSurface2 != null && eGLSurface2 != EGL14.EGL_NO_SURFACE) {
            EGL14.eglDestroySurface(this.dpy, eGLSurface2);
            this.surface = EGL14.EGL_NO_SURFACE;
        }
    }

    public synchronized boolean swapBuffers() {
        return EGL14.eglSwapBuffers(this.dpy, this.surface);
    }

    public synchronized void uninitOpenGL() {
        try {
            EGLDisplay eGLDisplay = this.dpy;
            EGLSurface eGLSurface = EGL14.EGL_NO_SURFACE;
            EGL14.eglMakeCurrent(eGLDisplay, eGLSurface, eGLSurface, EGL14.EGL_NO_CONTEXT);
            EGLSurface eGLSurface2 = this.surface;
            if (eGLSurface2 != null && eGLSurface2 != EGL14.EGL_NO_SURFACE) {
                EGL14.eglDestroySurface(this.dpy, eGLSurface2);
                this.surface = EGL14.EGL_NO_SURFACE;
            }
            EGLContext eGLContext = this.context;
            if (eGLContext != null && eGLContext != EGL14.EGL_NO_CONTEXT) {
                EGL14.eglDestroyContext(this.dpy, eGLContext);
                this.context = EGL14.EGL_NO_CONTEXT;
            }
            EGLDisplay eGLDisplay2 = this.dpy;
            if (eGLDisplay2 != null && eGLDisplay2 != EGL14.EGL_NO_DISPLAY) {
                EGL14.eglReleaseThread();
                EGL14.eglTerminate(this.dpy);
                this.dpy = EGL14.EGL_NO_DISPLAY;
            }
        } catch (Throwable th2) {
            throw th2;
        }
    }

    public synchronized boolean useCurrentContext() {
        boolean zEglMakeCurrent;
        if (EGL14.eglGetCurrentContext().hashCode() != this.context.hashCode()) {
            EGLDisplay eGLDisplay = this.dpy;
            EGLSurface eGLSurface = this.surface;
            zEglMakeCurrent = EGL14.eglMakeCurrent(eGLDisplay, eGLSurface, eGLSurface, this.context);
        } else {
            zEglMakeCurrent = true;
        }
        return zEglMakeCurrent;
    }
}
