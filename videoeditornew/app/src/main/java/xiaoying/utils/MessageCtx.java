package xiaoying.utils;

/* JADX INFO: loaded from: classes19.dex */
class MessageCtx {
    private static final MessageCtx instance = new MessageCtx();
    private CodecInspector.Listener listener_;

    private MessageCtx() {
    }

    public static MessageCtx getInstance() {
        return instance;
    }

    public void Log(String TAG, String msg) {
        CodecInspector.Listener listener = this.listener_;
        if (listener != null) {
            listener.onMessage(TAG, msg);
        }
    }

    public void setListener(CodecInspector.Listener listener) {
        this.listener_ = listener;
    }
}
