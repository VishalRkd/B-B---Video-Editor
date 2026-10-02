package xiaoying.utils;

import android.view.SurfaceHolder;

import javax.microedition.khronos.egl.EGL10;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.egl.EGLContext;
import javax.microedition.khronos.egl.EGLDisplay;
import javax.microedition.khronos.egl.EGLSurface;
import xiaoying.engine.clip.QClip;

/* JADX INFO: loaded from: classes19.dex */
public class QOpenGL {
    private static final int kANDROID_SDK_INT_18 = 18;
    private static final int kEGL_CONTEXT_MAJOR_VERSION_KHR = 12440;
    private static final int kEGL_CONTEXT_MINOR_VERSION_KHR = 12539;
    private static final String kEGL_KHR_create_context = "EGL_KHR_create_context";
    private static final int kEGL_OPENGL_ES2_BIT = 4;
    private static final int kEGL_OPENGL_ES3_BIT_KHR = 64;
    protected EGL10 egl = null;
    protected EGLDisplay dpy = EGL10.EGL_NO_DISPLAY;
    protected EGLSurface surface = EGL10.EGL_NO_SURFACE;
    protected EGLContext context = EGL10.EGL_NO_CONTEXT;
    protected EGLConfig config = null;
    private final String TAG = "QOpenGL";
    int[] attribListPbuffer = {12375, 1024, 12374, 1024, QClip.PROP_CLIP_INVERSE_PLAY_VIDEO_FLAG};

    public Object getConfig() {
        return this.config;
    }

    public Object getContext() {
        return this.context;
    }

    public Object getDisplay() {
        return this.dpy;
    }

    public Object getSurface() {
        return this.surface;
    }

    public synchronized boolean initOpenGL(Object holder, int[] userConfig) {
        if (this.egl != null) {
            return true;
        }
        if (holder != null && !((SurfaceHolder) holder).getSurface().isValid()) {
            return false;
        }
        EGL10 egl10 = (EGL10) EGLContext.getEGL();
        this.egl = egl10;
        EGLDisplay eGLDisplayEglGetDisplay = egl10.eglGetDisplay(EGL10.EGL_DEFAULT_DISPLAY);
        this.dpy = eGLDisplayEglGetDisplay;
        boolean zEglInitialize = this.egl.eglInitialize(eGLDisplayEglGetDisplay, new int[2]);
        if (!zEglInitialize) {
            return zEglInitialize;
        }
        this.egl.eglQueryString(this.dpy, QClip.PROP_CLIP_DISPLAY_3D_TRANSFROM).contains(kEGL_KHR_create_context);
        EGLConfig[] eGLConfigArr = new EGLConfig[1];
        int[] iArr = new int[1];
        boolean zEglChooseConfig = userConfig == null ? this.egl.eglChooseConfig(this.dpy, new int[]{12324, 8, 12323, 8, 12322, 8, 12321, 8, 12352, 4, 12344}, eGLConfigArr, 1, iArr) : this.egl.eglChooseConfig(this.dpy, userConfig, eGLConfigArr, 1, iArr);
        if (!zEglChooseConfig) {
            return zEglChooseConfig;
        }
        this.config = eGLConfigArr[0];
        int[] iArr2 = {kEGL_CONTEXT_MAJOR_VERSION_KHR, 2, 12344, 12344, 12344};
        if (EGL10.EGL_NO_CONTEXT == this.context) {
            for (int i10 = 2; i10 >= 2; i10--) {
                iArr2[0] = kEGL_CONTEXT_MAJOR_VERSION_KHR;
                iArr2[1] = i10;
                iArr2[2] = 12344;
                iArr2[3] = 12344;
                iArr2[4] = 12344;
                EGL10 egl11 = this.egl;
                EGLDisplay eGLDisplay = this.dpy;
                EGLConfig eGLConfig = this.config;
                EGLContext eGLContext = EGL10.EGL_NO_CONTEXT;
                EGLContext eGLContextEglCreateContext = egl11.eglCreateContext(eGLDisplay, eGLConfig, eGLContext, iArr2);
                this.context = eGLContextEglCreateContext;
                if (eGLContext != eGLContextEglCreateContext) {
                    break;
                }
            }
        }
        if (this.context == EGL10.EGL_NO_CONTEXT) {
            StringBuilder sb2 = new StringBuilder();
            sb2.append("init, ERROR context = ");
            sb2.append(this.context);
            return false;
        }
        if (holder != null) {
            EGLSurface eGLSurfaceEglCreateWindowSurface = this.egl.eglCreateWindowSurface(this.dpy, this.config, holder, null);
            this.surface = eGLSurfaceEglCreateWindowSurface;
            if (eGLSurfaceEglCreateWindowSurface == EGL10.EGL_NO_SURFACE) {
                StringBuilder sb3 = new StringBuilder();
                sb3.append("init, ERROR window surface = ");
                sb3.append(this.surface);
            }
        } else {
            EGLSurface eGLSurfaceEglCreatePbufferSurface = this.egl.eglCreatePbufferSurface(this.dpy, this.config, this.attribListPbuffer);
            this.surface = eGLSurfaceEglCreatePbufferSurface;
            if (eGLSurfaceEglCreatePbufferSurface == EGL10.EGL_NO_SURFACE) {
                StringBuilder sb4 = new StringBuilder();
                sb4.append("init, ERROR buffer surface = ");
                sb4.append(this.surface);
            }
        }
        EGL10 egl12 = this.egl;
        EGLDisplay eGLDisplay2 = this.dpy;
        EGLSurface eGLSurface = this.surface;
        return egl12.eglMakeCurrent(eGLDisplay2, eGLSurface, eGLSurface, this.context);
    }

    public synchronized boolean resume(Object holder) {
        EGL10 egl10 = this.egl;
        if (egl10 == null) {
            return false;
        }
        EGLSurface eGLSurface = this.surface;
        if (eGLSurface != null && eGLSurface != EGL10.EGL_NO_SURFACE) {
            return true;
        }
        if (holder == null) {
            EGLSurface eGLSurfaceEglCreatePbufferSurface = egl10.eglCreatePbufferSurface(this.dpy, this.config, this.attribListPbuffer);
            this.surface = eGLSurfaceEglCreatePbufferSurface;
            if (eGLSurfaceEglCreatePbufferSurface == EGL10.EGL_NO_SURFACE) {
                StringBuilder sb2 = new StringBuilder();
                sb2.append("resume, ERROR buffer surface = ");
                sb2.append(this.surface);
            }
        } else {
            if (!((SurfaceHolder) holder).getSurface().isValid()) {
                return false;
            }
            EGLSurface eGLSurfaceEglCreateWindowSurface = this.egl.eglCreateWindowSurface(this.dpy, this.config, holder, null);
            this.surface = eGLSurfaceEglCreateWindowSurface;
            if (eGLSurfaceEglCreateWindowSurface == EGL10.EGL_NO_SURFACE) {
                StringBuilder sb3 = new StringBuilder();
                sb3.append("ERROR window surface = ");
                sb3.append(this.surface);
            }
        }
        EGL10 egl11 = this.egl;
        EGLDisplay eGLDisplay = this.dpy;
        EGLSurface eGLSurface2 = this.surface;
        return egl11.eglMakeCurrent(eGLDisplay, eGLSurface2, eGLSurface2, this.context);
    }

    public synchronized void suspend() {
        EGL10 egl10 = this.egl;
        if (egl10 == null) {
            return;
        }
        EGLDisplay eGLDisplay = this.dpy;
        EGLSurface eGLSurface = EGL10.EGL_NO_SURFACE;
        egl10.eglMakeCurrent(eGLDisplay, eGLSurface, eGLSurface, EGL10.EGL_NO_CONTEXT);
        EGLSurface eGLSurface2 = this.surface;
        if (eGLSurface2 != null && eGLSurface2 != eGLSurface) {
            this.egl.eglDestroySurface(this.dpy, eGLSurface2);
            this.surface = eGLSurface;
        }
    }

    public synchronized boolean swapBuffers() {
        return this.egl.eglSwapBuffers(this.dpy, this.surface);
    }

    public synchronized void uninitOpenGL() {
        EGLDisplay eGLDisplay;
        try {
            EGL10 egl10 = this.egl;
            if (egl10 == null) {
                return;
            }
            EGLDisplay eGLDisplay2 = this.dpy;
            EGLSurface eGLSurface = EGL10.EGL_NO_SURFACE;
            EGLContext eGLContext = EGL10.EGL_NO_CONTEXT;
            egl10.eglMakeCurrent(eGLDisplay2, eGLSurface, eGLSurface, eGLContext);
            EGLSurface eGLSurface2 = this.surface;
            if (eGLSurface2 != null && eGLSurface2 != eGLSurface) {
                this.egl.eglDestroySurface(this.dpy, eGLSurface2);
                this.surface = eGLSurface;
            }
            EGLContext eGLContext2 = this.context;
            if (eGLContext2 != null && eGLContext2 != eGLContext) {
                this.egl.eglDestroyContext(this.dpy, eGLContext2);
                this.context = eGLContext;
            }
            EGLDisplay eGLDisplay3 = this.dpy;
            if (eGLDisplay3 != null && eGLDisplay3 != (eGLDisplay = EGL10.EGL_NO_DISPLAY)) {
                this.egl.eglTerminate(eGLDisplay3);
                this.dpy = eGLDisplay;
            }
        } catch (Throwable th2) {
            throw th2;
        }
    }

    public synchronized boolean useCurrentContext() {
        boolean zEglMakeCurrent;
        if (this.context.hashCode() != this.egl.eglGetCurrentContext().hashCode()) {
            EGL10 egl10 = this.egl;
            EGLDisplay eGLDisplay = this.dpy;
            EGLSurface eGLSurface = this.surface;
            zEglMakeCurrent = egl10.eglMakeCurrent(eGLDisplay, eGLSurface, eGLSurface, this.context);
        } else {
            zEglMakeCurrent = true;
        }
        return zEglMakeCurrent;
    }
}
