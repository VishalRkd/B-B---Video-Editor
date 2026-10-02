package xiaoying.engine.aecomp;

import xiaoying.engine.QEngine;
import xiaoying.engine.base.QBubbleTextSource;
import xiaoying.engine.base.QStyle;
import xiaoying.engine.base.QVEError;
import xiaoying.engine.clip.QEffect;
import xiaoying.engine.clip.QMediaMulSource;
import xiaoying.engine.clip.QMediaSource;

/* JADX INFO: loaded from: classes19.dex */
public class QAEPresetComp extends QAEBaseComp {
    public int GetExternalSource(int index, QEffect.QEffectExternalSource extSource) {
        long j10 = this.wphandle;
        return 0 == j10 ? QVEError.QERR_COMMON_JAVA_NOT_INIT : nativeGetExternalSource(j10, index, extSource);
    }

    public int SetExternalSource(int index, QEffect.QEffectExternalSource extSource) {
        long j10 = this.wphandle;
        return 0 == j10 ? QVEError.QERR_COMMON_JAVA_NOT_INIT : nativeSetExternalSource(j10, index, extSource);
    }

    /* JADX WARN: Code duplicated, block: B:27:0x0068  */
    public int create(QEngine engine, int groupId, float layerId, QMediaSource mediaSource) {
        int i10;
        if (this.sphandle != 0 || this.wphandle != 0) {
            return QVEError.QERR_COMMON_JAVA_FAIL;
        }
        if (mediaSource == null || mediaSource.getSource() == null) {
            return -1;
        }
        if (mediaSource.getSourceType() == 0 || mediaSource.getSourceType() == 2) {
            QStyle qStyle = new QStyle();
            int iCreate = 0;
            if (mediaSource.getSourceType() == 0) {
                iCreate = qStyle.create((String) mediaSource.getSource(), null, 0);
            } else if (mediaSource.getSourceType() == 2) {
                iCreate = qStyle.create(engine.GetTemplateFile(((QBubbleTextSource) mediaSource.getSource()).bubbleTemplateID), null, 0);
            }
            if (iCreate != 0) {
                qStyle.destroy();
                return iCreate;
            }
            int infoVersion = qStyle.getInfoVersion();
            qStyle.destroy();
            if (infoVersion >= 262144) {
                i10 = 11;
            } else {
                i10 = 3;
            }
        } else {
            i10 = 3;
        }
        int iNativeCreate = nativeCreate(engine, groupId, layerId, i10);
        if (iNativeCreate == 0 && (iNativeCreate = nativeSetSource(this.wphandle, QAECompSource.createPresetSource(mediaSource))) != 0) {
            destroy();
        }
        return iNativeCreate;
    }

    @Override // xiaoying.engine.aecomp.QAEBaseComp
    public int insertComp(QAEBaseComp comp) {
        if (comp instanceof QAEAdjustComp) {
            return super.insertComp(comp);
        }
        return 2;
    }

    public int setSource(QMediaSource mediaSource) {
        long j10 = this.wphandle;
        return 0 == j10 ? QVEError.QERR_COMMON_JAVA_NOT_INIT : nativeSetSource(j10, QAECompSource.createPresetSource(mediaSource));
    }

    public int setSource(QMediaMulSource mediaMultiSource) {
        long j10 = this.wphandle;
        return 0 == j10 ? QVEError.QERR_COMMON_JAVA_NOT_INIT : nativeSetSource(j10, QAECompSource.createPresetSource(mediaMultiSource));
    }

    /* JADX WARN: Code duplicated, block: B:19:0x0048  */
    public int create(QEngine engine, int groupId, float layerId, QMediaMulSource mediaMultiSource) {
        int i10;
        if (this.sphandle != 0 || this.wphandle != 0) {
            return QVEError.QERR_COMMON_JAVA_FAIL;
        }
        if (mediaMultiSource == null || mediaMultiSource.getSourceCount() == 0 || mediaMultiSource.getSource() == null) {
            return 0;
        }
        QStyle qStyle = new QStyle();
        if (mediaMultiSource.getSourceType() == 2) {
            qStyle.create(engine.GetTemplateFile(((QBubbleTextSource) mediaMultiSource.getSource()[0]).bubbleTemplateID), null, 0);
            if (qStyle.getInfoVersion() >= 262144) {
                i10 = 11;
            } else {
                i10 = 3;
            }
        } else {
            i10 = 3;
        }
        qStyle.destroy();
        int iNativeCreate = nativeCreate(engine, groupId, layerId, i10);
        if (iNativeCreate == 0 && (iNativeCreate = nativeSetSource(this.wphandle, QAECompSource.createPresetSource(mediaMultiSource))) != 0) {
            destroy();
        }
        return iNativeCreate;
    }
}
