package xiaoying.engine.poster;

import xiaoying.engine.QEngine;
import xiaoying.engine.base.QBasicTextInfo;
import xiaoying.engine.base.QUIRFS;
import xiaoying.engine.base.QVEError;
import xiaoying.engine.clip.QMediaSource;
import xiaoying.utils.QBitmap;
import xiaoying.utils.QBitmapFactory;
import xiaoying.utils.QColorSpace;
import xiaoying.utils.QPoint;
import xiaoying.utils.QRect;

/* JADX INFO: loaded from: classes19.dex */
public class QPoster {
    public static final int ITEM_TYPE_IMAGE = 1;
    public static final int ITEM_TYPE_SVGTEXT = 2;
    private long mNativePosterHandle = 0;
    private QBitmap mResultBitmap = null;
    private IQProcessStateListener mListener = null;

    public class QPosterItemAttr {
        public float mAngle;
        public boolean mIsInternalItem;
        public QRect mMergeRect;

        public QPosterItemAttr() {
            QRect qRect = new QRect();
            this.mMergeRect = qRect;
            qRect.left = 0;
            qRect.top = 0;
            qRect.right = 0;
            qRect.bottom = 0;
            this.mAngle = 0.0f;
            this.mIsInternalItem = false;
        }
    }

    public class QPosterItemData {
        public QMediaSource mDataSrc = null;
        public QRect mMergeRect;
        public int mResampleMode;

        public QPosterItemData() {
            QRect qRect = new QRect();
            this.mMergeRect = qRect;
            qRect.left = 0;
            qRect.top = 0;
            qRect.right = 0;
            qRect.bottom = 0;
            this.mResampleMode = 65537;
        }
    }

    private native QBitmap nativeCompose(long nativeHandle, QBitmap resultBitmap);

    private native long nativeCreate(QEngine engine, String templateFile, int layoutMode);

    private native void nativeDestroy(long nativeHandle);

    private native int nativeGetBasicTextInfo(long nativehandle, QBasicTextInfo baseTextInfo, int textItemIdx);

    private native int nativeGetItemAttr(long nativeHandle, QPosterItemAttr itemAttr, int itemType, int index);

    private native int nativeGetItemCount(long nativeHandle, int itemType);

    private native int nativeGetOriginalBGSize(long nativeHandle, QPoint point);

    private native String nativeGetTextItemString(long nativeHandle, int textItemIdx, int languageHexID);

    private native int nativeGetTextItemUIRFS(long nativeHandle, int textItemIdx, int UIRFSIdx, QUIRFS uiRFS);

    private native int nativeGetTextItemUIRFSCount(long nativeHandle, int textItemIdx);

    private native int nativeSetItemData(long nativeHandle, int itemType, int index, QPosterItemData itemData);

    private int onProcessStatus(QPosterProcessStatus status) {
        IQProcessStateListener iQProcessStateListener = this.mListener;
        if (iQProcessStateListener == null) {
            return 0;
        }
        return iQProcessStateListener.onProcessStatus(status);
    }

    public QBitmap compose(int picWidth, int picHeight) {
        int i10 = picWidth & (-4);
        int i11 = picHeight & (-4);
        QBitmap qBitmap = this.mResultBitmap;
        if (qBitmap != null) {
            qBitmap.recycle();
            this.mResultBitmap = null;
        }
        QBitmap qBitmapCreateQBitmapBlank_noSkia = QBitmapFactory.createQBitmapBlank_noSkia(i10, i11, QColorSpace.QPAF_RGB32_A8R8G8B8);
        this.mResultBitmap = qBitmapCreateQBitmapBlank_noSkia;
        if (qBitmapCreateQBitmapBlank_noSkia == null) {
            return null;
        }
        return nativeCompose(this.mNativePosterHandle, qBitmapCreateQBitmapBlank_noSkia);
    }

    public int create(IQProcessStateListener listener, QEngine engine, String templateFile, int layoutMode) {
        if (engine == null || templateFile == null) {
            return QVEError.QERR_COMMON_JAVA_INVALID_PARAM;
        }
        if (0 != this.mNativePosterHandle) {
            return QVEError.QERR_COMMON_JAVA_FAIL;
        }
        long jNativeCreate = nativeCreate(engine, templateFile, layoutMode);
        this.mNativePosterHandle = jNativeCreate;
        if (0 == jNativeCreate) {
            return QVEError.QERR_COMMON_JAVA_FAIL;
        }
        this.mListener = listener;
        return 0;
    }

    public void destroy() {
        QBitmap qBitmap = this.mResultBitmap;
        if (qBitmap != null) {
            qBitmap.recycle();
            this.mResultBitmap = null;
        }
        nativeDestroy(this.mNativePosterHandle);
        this.mNativePosterHandle = 0L;
        this.mListener = null;
    }

    public QPosterItemAttr getItemAttr(int itemType, int index) {
        QPosterItemAttr qPosterItemAttr = new QPosterItemAttr();
        if (nativeGetItemAttr(this.mNativePosterHandle, qPosterItemAttr, itemType, index) != 0) {
            return null;
        }
        return qPosterItemAttr;
    }

    public int getItemCount(int itemType) {
        return nativeGetItemCount(this.mNativePosterHandle, itemType);
    }

    public QPoint getOriginalBGSize() {
        QPoint qPoint = new QPoint();
        if (nativeGetOriginalBGSize(this.mNativePosterHandle, qPoint) != 0) {
            return null;
        }
        return qPoint;
    }

    public QBasicTextInfo getTextItemBasicInfo(int textItemIdx) {
        QBasicTextInfo qBasicTextInfo = new QBasicTextInfo();
        if (nativeGetBasicTextInfo(this.mNativePosterHandle, qBasicTextInfo, textItemIdx) != 0) {
            return null;
        }
        return qBasicTextInfo;
    }

    public String getTextItemString(int textItemIdx, int languageHexID) {
        return nativeGetTextItemString(this.mNativePosterHandle, textItemIdx, languageHexID);
    }

    public QUIRFS getTextItemUIRFS(int textItemIdx, int UIRFSIdx) {
        QUIRFS quirfs = new QUIRFS();
        if (nativeGetTextItemUIRFS(this.mNativePosterHandle, textItemIdx, UIRFSIdx, quirfs) != 0) {
            return null;
        }
        return quirfs;
    }

    public int getTextItemUIRFSCount(int textItemIdx) {
        return nativeGetTextItemUIRFSCount(this.mNativePosterHandle, textItemIdx);
    }

    public int setItemData(int itemType, int index, QPosterItemData itemData) {
        return nativeSetItemData(this.mNativePosterHandle, itemType, index, itemData);
    }

    public int setListener(IQProcessStateListener listener) {
        if (listener == null) {
            return QVEError.QERR_COMMON_JAVA_INVALID_PARAM;
        }
        this.mListener = listener;
        return 0;
    }
}
