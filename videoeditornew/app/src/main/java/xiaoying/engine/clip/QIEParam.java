package xiaoying.engine.clip;

import xiaoying.utils.QRect;

/* JADX INFO: loaded from: classes19.dex */
public class QIEParam {
    public static final int IE_PARAM_TYPE_MOTION = 1;
    public static final int IE_PARAM_TYPE_NONE = 0;
    private int type = 0;
    private Object param = null;

    public Object getParam() {
        return this.param;
    }

    public int getType() {
        return this.type;
    }

    public void setParam(int type, Object param) {
        this.type = type;
        if (param == null) {
            this.param = null;
        } else if (type == 1 && (param instanceof QMotion)) {
            this.param = new QMotion((QMotion) param);
        }
    }

    public class QMotion {
        private QRect end;
        private QRect start;

        public QMotion() {
            this.start = null;
            this.end = null;
        }

        public QRect getEnd() {
            return this.end;
        }

        public QRect getStart() {
            return this.start;
        }

        public void setEnd(QRect end) {
            this.end = end;
        }

        public void setStart(QRect start) {
            this.start = start;
        }

        public QMotion(QMotion motioninfo) {
            this.start = null;
            this.end = null;
            if (motioninfo != null) {
                this.start = motioninfo.start;
                this.end = motioninfo.end;
            }
        }
    }
}
