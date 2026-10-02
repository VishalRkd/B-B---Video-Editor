package xiaoying.engine.base.hardware;

import xiaoying.engine.QEngine;
import xiaoying.engine.base.IQSessionStateListener;
import xiaoying.engine.base.QRange;
import xiaoying.engine.base.QSessionStream;
import xiaoying.engine.base.QSessionStreamOpenParam;
import xiaoying.engine.base.QVEError;
import xiaoying.engine.base.QVideoInfo;
import xiaoying.engine.clip.QClip;
import xiaoying.engine.clip.QEffect;
import xiaoying.engine.clip.QMediaSource;
import xiaoying.engine.producer.QProducer;
import xiaoying.engine.producer.QProducerProperty;
import xiaoying.engine.storyboard.QStoryboard;
import xiaoying.utils.QPoint;
import xiaoying.utils.QRect;
import xiaoying.utils.QSize;

/* JADX INFO: loaded from: classes19.dex */
public class QHardwareComposer {
    private static final int m_exportDuration = 1000;
    private QEngine m_engine;
    private String m_inputFileName;
    private IQSessionStateListener m_listener;
    private int m_nCount;
    private int m_nType;
    private String m_outFileName;
    private QProducer m_qProducer;
    private QStoryboard m_qStoryboard;
    private QSessionStream m_qStream;
    private QVideoInfo m_qVideoInfo;

    public QHardwareComposer(QEngine engine, int nType, int nCount, String inputFile, String outFileName, IQSessionStateListener listener) {
        this.m_engine = engine;
        this.m_nType = nType;
        this.m_inputFileName = inputFile;
        this.m_outFileName = outFileName;
        this.m_listener = listener;
        this.m_nCount = nCount;
    }

    public int Create() {
        if (this.m_engine == null || this.m_inputFileName == null || this.m_outFileName == null || this.m_listener == null) {
            return QVEError.QERR_COMMON_JAVA_INVALID_PARAM;
        }
        if (this.m_nCount < 1) {
            return QVEError.QERR_COMMON_JAVA_INVALID_PARAM;
        }
        QStoryboard qStoryboard = new QStoryboard();
        this.m_qStoryboard = qStoryboard;
        int iInit = qStoryboard.init(this.m_engine, this.m_listener);
        if (iInit != 0) {
            return iInit;
        }
        QSize qSize = QHardwareQuery.m_size[this.m_nType];
        this.m_qStoryboard.setProperty(QStoryboard.PROP_OUTPUT_RESOLUTION, new QPoint(qSize.mWidth, qSize.mHeight));
        QClip qClip = new QClip();
        int iInit2 = qClip.init(this.m_engine, new QMediaSource(0, false, this.m_inputFileName));
        if (iInit2 != 0) {
            return iInit2;
        }
        this.m_qVideoInfo = (QVideoInfo) qClip.getProperty(12291);
        qClip.setProperty(12292, new QRange(0, 1000));
        this.m_qStoryboard.insertClip(qClip, 0);
        QRect qRect = new QRect(0, 0, 5000, 5000);
        for (int i10 = 1; i10 < this.m_nCount; i10++) {
            QEffect qEffect = new QEffect();
            int iCreate = qEffect.create(this.m_engine, 2, 2, 0, i10 + 0.0f);
            if (iCreate != 0) {
                return iCreate;
            }
            qEffect.setProperty(4104, new QMediaSource(0, false, this.m_inputFileName));
            qEffect.setProperty(QEffect.PROP_VIDEO_FRAME_RANGE, new QRange(0, 1000));
            qEffect.setProperty(4102, qRect);
            qRect.left += 500;
            qRect.top += 500;
            iInit2 = this.m_qStoryboard.getDataClip().insertEffect(qEffect);
            if (iInit2 != 0) {
                return iInit2;
            }
        }
        return iInit2;
    }

    public void Destroy() {
        QProducer qProducer = this.m_qProducer;
        if (qProducer != null) {
            qProducer.stop();
            this.m_qProducer.deactiveStream();
            this.m_qProducer.unInit();
            this.m_qProducer = null;
        }
        QSessionStream qSessionStream = this.m_qStream;
        if (qSessionStream != null) {
            qSessionStream.close();
            this.m_qStream = null;
        }
        QStoryboard qStoryboard = this.m_qStoryboard;
        if (qStoryboard != null) {
            qStoryboard.unInit();
            this.m_qStoryboard = null;
        }
    }

    public int OpenStream() {
        QSize qSize = QHardwareQuery.m_size[this.m_nType];
        this.m_qStream = new QSessionStream();
        QSessionStreamOpenParam qSessionStreamOpenParam = new QSessionStreamOpenParam();
        qSessionStreamOpenParam.mDecoderUsageType = 1;
        QSize qSize2 = qSessionStreamOpenParam.mFrameSize;
        qSize2.mWidth = 0;
        qSize2.mHeight = 0;
        QSize qSize3 = qSessionStreamOpenParam.mRenderTargetSize;
        qSize3.mWidth = qSize.mWidth;
        qSize3.mHeight = qSize.mHeight;
        qSessionStreamOpenParam.mResampleMode = 65537;
        qSessionStreamOpenParam.mRotation = 0;
        int iOpen = this.m_qStream.open(1, this.m_qStoryboard, qSessionStreamOpenParam);
        this.m_qStream.setConfig(QSessionStream.PROP_HARDWARE_TEST_MODE, new Boolean(true));
        return iOpen;
    }

    public int StartProducer() {
        QProducer qProducer = new QProducer();
        this.m_qProducer = qProducer;
        int iInit = qProducer.init(this.m_engine, this.m_listener);
        if (iInit != 0) {
            return iInit;
        }
        int property = this.m_qProducer.setProperty(24577, new QProducerProperty(1, 4, 4, 25, this.m_qVideoInfo.get(8), 50000000L, this.m_outFileName, 256, new QRange(0, -1), 1, 40, null));
        if (property != 0) {
            return property;
        }
        int iActiveStream = this.m_qProducer.activeStream(this.m_qStream);
        return iActiveStream != 0 ? iActiveStream : this.m_qProducer.start();
    }

    public void Stop() {
        QProducer qProducer = this.m_qProducer;
        if (qProducer != null) {
            qProducer.stop();
        }
    }

    public void finalize() throws Throwable {
        try {
            Destroy();
        } catch (Throwable unused) {
        }
        super.finalize();
    }

    public int getCount() {
        return this.m_nCount;
    }

    public int getType() {
        return this.m_nType;
    }
}
