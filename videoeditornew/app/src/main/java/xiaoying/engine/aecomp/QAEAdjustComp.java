package xiaoying.engine.aecomp;

import xiaoying.engine.QEngine;
import xiaoying.engine.base.QStyle;
import xiaoying.engine.base.QVEError;
import xiaoying.engine.clip.QEffect;
import xiaoying.engine.clip.QMediaSource;

/* JADX INFO: loaded from: classes19.dex */
public class QAEAdjustComp extends QAEBaseComp {
    public int GetExternalSource(int index, QEffect.QEffectExternalSource extSource) {
        long j10 = this.wphandle;
        return 0 == j10 ? QVEError.QERR_COMMON_JAVA_NOT_INIT : nativeGetExternalSource(j10, index, extSource);
    }

    public int SetExternalSource(int index, QEffect.QEffectExternalSource extSource) {
        long j10 = this.wphandle;
        return 0 == j10 ? QVEError.QERR_COMMON_JAVA_NOT_INIT : nativeSetExternalSource(j10, index, extSource);
    }

    public int create(QEngine engine, int groupId, float layerId, QMediaSource mediaSource) {
        return create(engine, groupId, layerId, 0, mediaSource);
    }

    @Override // xiaoying.engine.aecomp.QAEBaseComp
    public int insertComp(QAEBaseComp comp) {
        return 2;
    }

    public int setSource(QMediaSource mediaSource, int effectMode) {
        long j10 = this.wphandle;
        return 0 == j10 ? QVEError.QERR_COMMON_JAVA_NOT_INIT : nativeSetSource(j10, QAECompSource.createAdjustSource(mediaSource, effectMode));
    }

    /* JADX WARN: Code duplicated, block: B:20:0x0043  */
    public int create(QEngine engine, int groupId, float layerId, int effectMode, QMediaSource mediaSource) {
        int i10;
        if (this.sphandle != 0 || this.wphandle != 0) {
            return QVEError.QERR_COMMON_JAVA_FAIL;
        }
        if (mediaSource == null || mediaSource.getSource() == null) {
            return -1;
        }
        if (mediaSource.getSourceType() != 0) {
            i10 = 4;
        } else {
            QStyle qStyle = new QStyle();
            int iCreate = qStyle.create((String) mediaSource.getSource(), null, 0);
            if (iCreate != 0) {
                qStyle.destroy();
                return iCreate;
            }
            int infoVersion = qStyle.getInfoVersion();
            qStyle.destroy();
            if (infoVersion >= 262144) {
                i10 = 11;
            } else {
                i10 = 4;
            }
        }
        int iNativeCreate = nativeCreate(engine, groupId, layerId, i10);
        if (iNativeCreate == 0 && (iNativeCreate = nativeSetSource(this.wphandle, QAECompSource.createAdjustSource(mediaSource, effectMode))) != 0) {
            destroy();
        }
        return iNativeCreate;
    }
}
