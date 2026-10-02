package xiaoying.engine.base.hardware;


import xiaoying.engine.QEngine;
import xiaoying.engine.base.IQSessionStateListener;
import xiaoying.engine.base.QSessionState;
import xiaoying.engine.base.QUtils;
import xiaoying.engine.base.QVEError;
import xiaoying.utils.QSize;

/* JADX INFO: loaded from: classes19.dex */
public class QHardwareQuery implements IQSessionStateListener {
    public static final int QHARDWARE_QUERY_SUPPORT_264_1080P = 0;
    public static final int QHARDWARE_QUERY_SUPPORT_264_2K = 1;
    public static final int QHARDWARE_QUERY_SUPPORT_264_4K = 2;
    public static final int QHARDWARE_QUERY_SUPPORT_264_8K = 3;
    public static final int QHARDWARE_QUERY_SUPPORT_265_1080P = 4;
    public static final int QHARDWARE_QUERY_SUPPORT_265_2K = 5;
    public static final int QHARDWARE_QUERY_SUPPORT_265_4K = 6;
    public static final int QHARDWARE_QUERY_SUPPORT_265_8K = 7;
    private QEngine m_engine;
    private Thread m_hThread;
    private IQHardwareListener m_hardwareListener;
    private QHardwareQueryInitParam[] m_initParam;
    private volatile int m_nCurType;
    private QHardwareResult[] m_result;
    public static final QSize[] m_size = {new QSize(1920, 1080), new QSize(QUtils.VIDEO_RES_2K_WIDTH, 1600), new QSize(QUtils.VIDEO_RES_4K_WIDTH, 2160), new QSize(QUtils.VIDEO_RES_8K_WIDTH, 4320), new QSize(1920, 1080), new QSize(QUtils.VIDEO_RES_2K_WIDTH, 1600), new QSize(QUtils.VIDEO_RES_4K_WIDTH, 2160), new QSize(QUtils.VIDEO_RES_8K_WIDTH, 4320)};
    public static final int[] m_maxSupportCount = {8, 4, 2, 0, 8, 4, 2, 0};
    private volatile boolean m_bRuning = false;
    private volatile boolean m_bExit = false;
    private volatile boolean m_bInit = false;
    QHardwareComposer m_qHardwareComposer = null;
    private volatile int m_nIndex = 0;

    public static class QHardwareQueryInitParam {
        public String m_exportFileDir;
        public String m_inputFilePath;
        public int m_nType;

        public QHardwareQueryInitParam(int nType, String inputFilePath, String exportFileDir) {
            this.m_nType = nType;
            this.m_inputFilePath = inputFilePath;
            this.m_exportFileDir = exportFileDir;
        }
    }

    public static class QHardwareResult {
        public int m_nType = 0;
        public boolean m_bResult = false;
        public int m_nMaxCount = 0;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public int Run() {
        int iStartProducer = 0;
        for (int i10 = 0; i10 < this.m_initParam.length; i10++) {
            this.m_nIndex = i10;
            if (this.m_bExit) {
                break;
            }
            this.m_nCurType = this.m_initParam[i10].m_nType;
            for (int i11 = m_maxSupportCount[this.m_nCurType]; i11 <= m_maxSupportCount[this.m_nCurType] && !this.m_bExit; i11++) {
                this.m_hardwareListener.onStatusStart(this.m_nCurType, i11);
                QHardwareComposer qHardwareComposer = new QHardwareComposer(this.m_engine, this.m_nCurType, i11, this.m_initParam[i10].m_inputFilePath, this.m_initParam[i10].m_exportFileDir + "_" + this.m_initParam[i10].m_nType + "_" + i11 + ".mp4", this);
                this.m_qHardwareComposer = qHardwareComposer;
                int iCreate = qHardwareComposer.Create();
                if (iCreate != 0) {
                    this.m_hardwareListener.onStatusEnd(this.m_nCurType, i11, iCreate);
                    return iCreate;
                }
                int iOpenStream = this.m_qHardwareComposer.OpenStream();
                if (iOpenStream != 0) {
                    this.m_hardwareListener.onStatusEnd(this.m_nCurType, i11, iOpenStream);
                    return iOpenStream;
                }
                iStartProducer = this.m_qHardwareComposer.StartProducer();
                if (iStartProducer != 0) {
                    this.m_hardwareListener.onStatusEnd(this.m_nCurType, i11, iStartProducer);
                    return iStartProducer;
                }
                synchronized (this.m_qHardwareComposer) {
                    try {
                        this.m_qHardwareComposer.wait();
                    } catch (InterruptedException unused) {
                    }
                }
                this.m_qHardwareComposer.Destroy();
                this.m_hardwareListener.onStatusEnd(this.m_nCurType, this.m_result[this.m_nIndex].m_nMaxCount, 0);
            }
        }
        return iStartProducer;
    }

    public int Create(QEngine engine, QHardwareQueryInitParam[] param, IQHardwareListener hardwareListener) {
        if (engine == null || param == null || hardwareListener == null) {
            return QVEError.QERR_COMMON_JAVA_INVALID_PARAM;
        }
        this.m_engine = engine;
        this.m_initParam = param;
        this.m_hardwareListener = hardwareListener;
        this.m_bInit = true;
        this.m_result = new QHardwareResult[param.length];
        for (int i10 = 0; i10 < param.length; i10++) {
            this.m_result[i10] = new QHardwareResult();
        }
        return 0;
    }

    public void Destroy() {
    }

    public QHardwareResult[] GetResult() {
        return this.m_result;
    }

    public synchronized int Start() {
        if (!this.m_bInit) {
            return QVEError.QERR_COMMON_JAVA_INVALID_PARAM;
        }
        if (this.m_hThread != null) {
            return 0;
        }
        Thread thread = new Thread(new Runnable() { // from class: xiaoying.engine.base.hardware.QHardwareQuery.1
            @Override // java.lang.Runnable
            public void run() {
                QHardwareQuery.this.m_bRuning = true;
                QHardwareQuery.this.Run();
                QHardwareQuery.this.m_hardwareListener.onStatusOver(QHardwareQuery.this.m_result);
                if (QHardwareQuery.this.m_bExit) {
                    notify();
                }
                QHardwareQuery.this.m_bRuning = false;
            }
        });
        this.m_hThread = thread;
        thread.start();
        return 0;
    }

    public synchronized int Stop() {
        if (!this.m_bRuning) {
            return 0;
        }
        this.m_bExit = true;
        try {
            wait(3000L);
        } catch (InterruptedException unused) {
        }
        if (this.m_bRuning) {
            return QVEError.QERR_COMMON_JAVA_FAIL;
        }
        return 0;
    }

    @Override // xiaoying.engine.base.IQSessionStateListener
    public int onSessionStatus(QSessionState state) {
        if (state.status == 4) {
            StringBuilder sb2 = new StringBuilder();
            sb2.append("vDecErr=");
            sb2.append(state.vDecErr);
            sb2.append("vPrcErr=");
            sb2.append(state.vPrcErr);
            if (state.openglErr == 0 && state.vDecErr == 0 && state.vPrcErr == 0 && state.errorCode == 0) {
                this.m_result[this.m_nIndex].m_nType = this.m_qHardwareComposer.getType();
                this.m_result[this.m_nIndex].m_nMaxCount = this.m_qHardwareComposer.getCount();
                this.m_result[this.m_nIndex].m_bResult = true;
            } else {
                this.m_result[this.m_nIndex] = new QHardwareResult();
                this.m_result[this.m_nIndex].m_nType = this.m_qHardwareComposer.getType();
                this.m_result[this.m_nIndex].m_nMaxCount = 0;
                this.m_result[this.m_nIndex].m_bResult = false;
            }
            this.m_hardwareListener.onStatus(this.m_result[this.m_nIndex].m_nType, true, this.m_result[this.m_nIndex].m_nMaxCount);
            synchronized (this.m_qHardwareComposer) {
                this.m_qHardwareComposer.notify();
            }
        }
        return 0;
    }
}
