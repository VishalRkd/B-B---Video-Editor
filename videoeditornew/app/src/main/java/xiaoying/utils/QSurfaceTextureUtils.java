package xiaoying.utils;

import android.graphics.SurfaceTexture;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.view.Surface;
import java.lang.ref.WeakReference;
import java.util.concurrent.LinkedBlockingQueue;

/* JADX INFO: loaded from: classes19.dex */
public class QSurfaceTextureUtils implements SurfaceTexture.OnFrameAvailableListener {
    public static final String[] MODEL_NAMES = {"R8007", "SCH-I959", "GT-I9308", "L39t", "SM-G3509", "vivo Xshot", "X907", "SGH-T959"};
    private static final int QSurfaceTexUtils_ERR0 = 536870913;
    private static final int QSurfaceTexUtils_ERR1 = 536870914;
    private static final int QSurfaceTexUtils_ERR2 = 536870915;
    private static final int QSurfaceTexUtils_ERR3 = 536870916;
    private static final int QSurfaceTexUtils_MSG_CREATESURFACETEXTURE = 1;
    private static final int QSurfaceTexUtils_MSG_DESTROYSURFACETEXTURE = 2;
    private static final int QSurfaceTexUtils_NOERR = 0;
    private static final String TAG = "QSurfaceTextureUtils";
    protected SurfaceTexture mSurfaceTexture = null;
    protected Surface mSurface = null;
    private boolean mSurfaceTextureUpdate = false;
    private int mWidth = 0;
    private int mHeight = 0;
    private QSurfaceTextureHandler mHandler = null;
    private HandlerThread mHandlerThread = null;
    private int mTexturename = 0;
    private float mfRotation = 0.0f;
    private final LinkedBlockingQueue<Object> mMessageQueue = new LinkedBlockingQueue<>();

    public static class QSurfaceTextureHandler extends Handler {
        private WeakReference<QSurfaceTextureUtils> mUtil;
        private WeakReference<LinkedBlockingQueue> wmsgqueue;

        public QSurfaceTextureHandler(Looper looper, LinkedBlockingQueue queue, QSurfaceTextureUtils util) {
            super(looper);
            this.wmsgqueue = null;
            this.mUtil = null;
            this.wmsgqueue = new WeakReference<>(queue);
            this.mUtil = new WeakReference<>(util);
        }

        @Override // android.os.Handler
        public void handleMessage(Message msg) {
            LinkedBlockingQueue linkedBlockingQueue = this.wmsgqueue.get();
            QSurfaceTextureUtils qSurfaceTextureUtils = this.mUtil.get();
            int i10 = msg.what;
            if (i10 == 1) {
                if (qSurfaceTextureUtils != null) {
                    qSurfaceTextureUtils.CreateSurfaceTexture();
                }
                if (linkedBlockingQueue != null) {
                    try {
                        linkedBlockingQueue.put(this);
                    } catch (InterruptedException e10) {
                        e10.printStackTrace();
                    }
                }
            } else if (i10 == 2) {
                if (qSurfaceTextureUtils != null) {
                    qSurfaceTextureUtils.DestroySurfaceTexture();
                }
                if (linkedBlockingQueue != null) {
                    try {
                        linkedBlockingQueue.put(this);
                    } catch (InterruptedException e11) {
                        e11.printStackTrace();
                    }
                }
            }
            super.handleMessage(msg);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public int CreateSurfaceTexture() {
        SurfaceTexture surfaceTexture = new SurfaceTexture(this.mTexturename);
        this.mSurfaceTexture = surfaceTexture;
        surfaceTexture.setDefaultBufferSize(this.mWidth, this.mHeight);
        this.mSurfaceTexture.setOnFrameAvailableListener(this);
        this.mSurface = new Surface(this.mSurfaceTexture);
        return 0;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void DestroySurfaceTexture() {
        SurfaceTexture surfaceTexture = this.mSurfaceTexture;
        if (surfaceTexture != null) {
            surfaceTexture.release();
            this.mSurfaceTexture = null;
        }
        Surface surface = this.mSurface;
        if (surface != null) {
            surface.release();
        }
        this.mSurface = null;
    }

    public static int GetSDKVersion() {
        return Build.VERSION.SDK_INT;
    }

    public static boolean IsModelInList() {
        int length = MODEL_NAMES.length;
        for (int i10 = 0; i10 < length; i10++) {
            if (Build.MODEL.equals(MODEL_NAMES[i10])) {
                return true;
            }
        }
        return false;
    }

    public static boolean IsSurfaceTextureSupported() {
        return true;
    }

    private void Mat4_Multiply(float[] matrixRes, float[] matrixA, float[] matrixB) {
        float f10 = matrixA[0];
        float f11 = matrixB[0];
        float f12 = matrixA[1];
        float f13 = matrixB[4];
        float f14 = matrixA[2];
        float f15 = matrixB[8];
        float f16 = matrixA[3];
        float f17 = matrixB[12];
        float f18 = (f10 * f11) + (f12 * f13) + (f14 * f15) + (f16 * f17);
        float f19 = matrixB[1];
        float f20 = matrixB[5];
        float f21 = matrixB[9];
        float f22 = matrixB[13];
        float f23 = (f10 * f19) + (f12 * f20) + (f14 * f21) + (f16 * f22);
        float f24 = matrixB[2];
        float f25 = matrixB[6];
        float f26 = matrixB[10];
        float f27 = matrixB[14];
        float f28 = (f10 * f24) + (f12 * f25) + (f14 * f26) + (f16 * f27);
        float f29 = matrixB[3];
        float f30 = matrixB[7];
        float f31 = matrixB[11];
        float f32 = matrixB[15];
        float f33 = (f10 * f29) + (f12 * f30) + (f14 * f31) + (f16 * f32);
        float f34 = matrixA[4];
        float f35 = matrixA[5];
        float f36 = matrixA[6];
        float f37 = matrixA[7];
        float f38 = (f34 * f11) + (f35 * f13) + (f36 * f15) + (f37 * f17);
        float f39 = (f34 * f19) + (f35 * f20) + (f36 * f21) + (f37 * f22);
        float f40 = (f34 * f24) + (f35 * f25) + (f36 * f26) + (f37 * f27);
        float f41 = (f34 * f29) + (f35 * f30) + (f36 * f31) + (f37 * f32);
        float f42 = matrixA[8];
        float f43 = matrixA[9];
        float f44 = matrixA[10];
        float f45 = matrixA[11];
        float f46 = (f42 * f11) + (f43 * f13) + (f44 * f15) + (f45 * f17);
        float f47 = (f42 * f19) + (f43 * f20) + (f44 * f21) + (f45 * f22);
        float f48 = (f42 * f24) + (f43 * f25) + (f44 * f26) + (f45 * f27);
        float f49 = (f42 * f29) + (f43 * f30) + (f44 * f31) + (f45 * f32);
        float f50 = matrixA[12];
        float f51 = matrixA[13];
        float f52 = (f11 * f50) + (f13 * f51);
        float f53 = matrixA[14];
        float f54 = f52 + (f15 * f53);
        float f55 = matrixA[15];
        System.arraycopy(new float[]{f18, f23, f28, f33, f38, f39, f40, f41, f46, f47, f48, f49, f54 + (f17 * f55), (f19 * f50) + (f20 * f51) + (f21 * f53) + (f22 * f55), (f24 * f50) + (f25 * f51) + (f26 * f53) + (f27 * f55), (f50 * f29) + (f51 * f30) + (f53 * f31) + (f55 * f32)}, 0, matrixRes, 0, 16);
    }

    private void Mat4_RotateZ(float[] matrix, float fAngle) {
        double d10 = (fAngle * 3.1415927f) / 180.0f;
        float fSin = (float) Math.sin(d10);
        float fCos = (float) Math.cos(d10);
        matrix[0] = fCos;
        matrix[4] = -fSin;
        matrix[8] = 0.0f;
        matrix[12] = 0.0f;
        matrix[1] = fSin;
        matrix[5] = fCos;
        matrix[9] = 0.0f;
        matrix[13] = 0.0f;
        matrix[2] = 0.0f;
        matrix[6] = 0.0f;
        matrix[10] = 1.0f;
        matrix[14] = 0.0f;
        matrix[3] = 0.0f;
        matrix[7] = 0.0f;
        matrix[11] = 0.0f;
        matrix[15] = 1.0f;
    }

    private void Mat4_Translation(float[] matrix, float fX, float fY, float fZ) {
        matrix[0] = 1.0f;
        matrix[4] = 0.0f;
        matrix[8] = 0.0f;
        matrix[12] = fX;
        matrix[1] = 0.0f;
        matrix[5] = 1.0f;
        matrix[9] = 0.0f;
        matrix[13] = fY;
        matrix[2] = 0.0f;
        matrix[6] = 0.0f;
        matrix[10] = 1.0f;
        matrix[14] = fZ;
        matrix[3] = 0.0f;
        matrix[7] = 0.0f;
        matrix[11] = 0.0f;
        matrix[15] = 1.0f;
    }

    public int GetTransformMatrix(float[] matrix) {
        SurfaceTexture surfaceTexture = this.mSurfaceTexture;
        if (surfaceTexture != null) {
            surfaceTexture.getTransformMatrix(matrix);
        }
        if (Math.abs(this.mfRotation) <= 0.01f) {
            return 0;
        }
        float[] fArr = new float[16];
        float[] fArr2 = new float[16];
        float[] fArr3 = new float[16];
        Mat4_Translation(fArr3, -0.5f, -0.5f, 0.0f);
        Mat4_RotateZ(fArr2, this.mfRotation);
        Mat4_Multiply(fArr3, fArr3, fArr2);
        Mat4_Translation(fArr, 0.5f, 0.5f, 0.0f);
        Mat4_Multiply(fArr3, fArr3, fArr);
        Mat4_Multiply(matrix, fArr3, matrix);
        return 0;
    }

    public int Init(int texturename, int width, int height, float fRotation) {
        this.mTexturename = texturename;
        this.mWidth = width;
        this.mHeight = height;
        this.mfRotation = fRotation;
        StringBuilder sb2 = new StringBuilder();
        sb2.append("Init texturename=");
        sb2.append(texturename);
        sb2.append("width=");
        sb2.append(width);
        sb2.append("height=");
        sb2.append(height);
        HandlerThread handlerThread = new HandlerThread(TAG + hashCode());
        this.mHandlerThread = handlerThread;
        handlerThread.start();
        this.mHandler = new QSurfaceTextureHandler(this.mHandlerThread.getLooper(), this.mMessageQueue, this);
        try {
            new Thread() { // from class: xiaoying.utils.QSurfaceTextureUtils.1
                @Override // java.lang.Thread, java.lang.Runnable
                public void run() {
                    QSurfaceTextureUtils.this.mHandler.sendMessage(QSurfaceTextureUtils.this.mHandler.obtainMessage(1, 0, 0, QSurfaceTextureUtils.this.mMessageQueue));
                    super.run();
                }
            }.start();
            this.mMessageQueue.take();
            return 0;
        } catch (Throwable th2) {
            th2.printStackTrace();
            return 0;
        }
    }

    public void UnInit() {
        try {
            new Thread() { // from class: xiaoying.utils.QSurfaceTextureUtils.2
                @Override // java.lang.Thread, java.lang.Runnable
                public void run() {
                    QSurfaceTextureUtils.this.mHandler.sendMessage(QSurfaceTextureUtils.this.mHandler.obtainMessage(2, 0, 0, QSurfaceTextureUtils.this.mMessageQueue));
                    super.run();
                }
            }.start();
            this.mMessageQueue.take();
        } catch (Throwable th2) {
            th2.printStackTrace();
        }
        this.mHandlerThread.quit();
    }

    public void UpdateTexture() {
        try {
            synchronized (this) {
                try {
                    if (!this.mSurfaceTextureUpdate) {
                        wait(1000L);
                    }
                    if (this.mSurfaceTextureUpdate) {
                        this.mSurfaceTexture.updateTexImage();
                    }
                    this.mSurfaceTextureUpdate = false;
                } catch (Throwable th2) {
                    throw th2;
                }
            }
        } catch (Exception unused) {
        }
    }

    @Override // android.graphics.SurfaceTexture.OnFrameAvailableListener
    public void onFrameAvailable(SurfaceTexture paramSurfaceTexture) {
        try {
            synchronized (this) {
                this.mSurfaceTextureUpdate = true;
                notify();
            }
        } catch (Exception unused) {
        }
    }
}
