package xiaoying.utils;

import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import java.util.ArrayList;
import java.util.Hashtable;

/* JADX INFO: loaded from: classes19.dex */
public class CodecInspector {
    static final String TAG = "CodecInspector";
    private AssetManager assetMgr;
    private String dataDir;
    private DecodeInfo mDecInfo;
    private EncodeInfo mEncInfo;
    private Listener mListener;
    private ArrayList<Resolution> resolutions;

    public class DecodeInfo {
        public Codec.Type codec = Codec.Type.kNone;
        public Hashtable<Resolution, Integer> info = new Hashtable<>();

        public DecodeInfo() {
        }

        public String toString() {
            return "Codec: " + this.codec + ", Capabilities: " + this.info.toString();
        }
    }

    public class EncodeInfo {
        public Codec.Type codec = Codec.Type.kNone;
        public Hashtable<Resolution, Integer> info = new Hashtable<>();

        public EncodeInfo() {
        }

        public String toString() {
            return "Codec: " + this.codec + ", Capabilities: " + this.info.toString();
        }
    }

    public class EncoderThread extends Thread {
        private Codec.Type codec;
        private EvtQueue evt;
        private int idx;
        private Resolution res;

        public EncoderThread(Codec.Type codec, Resolution res, int idx, EvtQueue que) {
            this.idx = -1;
            Codec.Type type = Codec.Type.kNone;
            this.res = res;
            this.idx = idx;
            this.evt = que;
            this.codec = codec;
        }

        @Override // java.lang.Thread, java.lang.Runnable
        public void run() {
            boolean z10;
            int iInit;
            VEncoder vEncoder = new VEncoder(this.codec, this.res);
            boolean z11 = false;
            while (this.evt.waitAction() != 0) {
                if (z11) {
                    int iEncodeNext = vEncoder.encodeNext();
                    MessageCtx.getInstance().Log(CodecInspector.TAG, "encoder : " + this.idx + ", encodeNext : " + iEncodeNext);
                    z10 = z11;
                    iInit = iEncodeNext;
                } else {
                    iInit = vEncoder.Init();
                    MessageCtx.getInstance().Log(CodecInspector.TAG, "encoder : " + this.idx + ", init : " + iInit);
                    z10 = true;
                }
                this.evt.notify(iInit);
                z11 = z10;
            }
            vEncoder.Uninit();
        }
    }

    public interface Listener {
        void onComplete(DecodeInfo decInfo, EncodeInfo encInfo);

        void onMessage(String Tag, String msg);

        void onProgress(float percent);
    }

    public enum Resolution {
        Res4K,
        Res2K,
        Res1080p,
        Res720p
    }

    public CodecInspector(String dataDir) {
        this.mListener = null;
        this.resolutions = new ArrayList<>();
        this.mEncInfo = new EncodeInfo();
        this.mDecInfo = new DecodeInfo();
        this.assetMgr = null;
        this.dataDir = dataDir;
        this.resolutions.add(Resolution.Res720p);
        this.resolutions.add(Resolution.Res1080p);
        this.resolutions.add(Resolution.Res2K);
        this.resolutions.add(Resolution.Res4K);
    }

    private boolean runDecoder(Codec.Type codec, Resolution res, int concurrency) {
        EvtQueue evtQueue;
        DecoderThread decoderThread;
        ArrayList<EvtQueue> arrayList = new ArrayList();
        ArrayList arrayList2 = new ArrayList();
        MessageCtx.getInstance().Log(TAG, "runDecoder : " + res + ", before start : " + concurrency);
        for (int i10 = 0; i10 < concurrency; i10++) {
            EvtQueue evtQueue2 = new EvtQueue();
            if (this.dataDir != null) {
                evtQueue = evtQueue2;
                decoderThread = new DecoderThread(codec, res, i10, evtQueue2, this.dataDir);
            } else {
                evtQueue = evtQueue2;
                decoderThread = new DecoderThread(codec, res, i10, evtQueue, this.assetMgr);
            }
            arrayList.add(evtQueue);
            arrayList2.add(decoderThread);
            decoderThread.start();
        }
        int iWaitNotify = 0;
        for (int i11 = 0; i11 < 5 && iWaitNotify >= 0; i11++) {
            MessageCtx.getInstance().Log(TAG, "runDecoder : " + res + ", dispatch, iter : " + i11);
            for (EvtQueue evtQueue3 : arrayList) {
                evtQueue3.sendAction(1);
                iWaitNotify = evtQueue3.waitNotify();
                if (iWaitNotify < 0) {
                    MessageCtx.getInstance().Log(TAG, "runDecoder : " + res + ", waitNotify, failure : " + iWaitNotify);
                    break;
                }
            }
            MessageCtx.getInstance().Log(TAG, "runDecoder : " + res + ", completed, iter : " + i11);
        }
        for (int i12 = 0; i12 < concurrency; i12++) {
            ((EvtQueue) arrayList.get(i12)).sendAction(0);
            try {
                ((DecoderThread) arrayList2.get(i12)).join(500L);
            } catch (Exception unused) {
            }
        }
        MessageCtx.getInstance().Log(TAG, "runDecoder : " + res + ", after join, ret : " + iWaitNotify);
        return iWaitNotify >= 0;
    }

    private boolean runEncoder(Codec.Type codec, Resolution res, int concurrency) {
        ArrayList<EvtQueue> arrayList = new ArrayList();
        ArrayList arrayList2 = new ArrayList();
        MessageCtx.getInstance().Log(TAG, "runEncoder : " + res + ", before start : " + concurrency);
        for (int i10 = 0; i10 < concurrency; i10++) {
            EvtQueue evtQueue = new EvtQueue();
            EncoderThread encoderThread = new EncoderThread(codec, res, i10, evtQueue);
            arrayList.add(evtQueue);
            arrayList2.add(encoderThread);
            encoderThread.start();
        }
        int iWaitNotify = 0;
        for (int i11 = 0; i11 < 10 && iWaitNotify >= 0; i11++) {
            MessageCtx.getInstance().Log(TAG, "runEncoder : " + res + ", dispatch, iter : " + i11);
            for (EvtQueue evtQueue2 : arrayList) {
                evtQueue2.sendAction(1);
                iWaitNotify = evtQueue2.waitNotify();
                if (iWaitNotify < 0) {
                    MessageCtx.getInstance().Log(TAG, "runEncoder : " + res + ", waitNotify, failure : " + iWaitNotify);
                    break;
                }
            }
            MessageCtx.getInstance().Log(TAG, "runEncoder : " + res + ", completed, iter : " + i11);
        }
        for (int i12 = 0; i12 < concurrency; i12++) {
            ((EvtQueue) arrayList.get(i12)).sendAction(0);
            try {
                ((EncoderThread) arrayList2.get(i12)).join(500L);
            } catch (Exception unused) {
            }
        }
        MessageCtx.getInstance().Log(TAG, "runEncoder : " + res + ", after join, ret : " + iWaitNotify);
        return iWaitNotify >= 0;
    }

    public boolean inspect(Codec.Type codec) {
        this.mEncInfo.codec = codec;
        this.mDecInfo.codec = codec;
        int size = this.resolutions.size() * 2;
        float size2 = (this.resolutions.size() * 5) + size;
        int i10 = 0;
        int i11 = 0;
        while (true) {
            if (i10 >= this.resolutions.size()) {
                break;
            }
            Resolution resolution = this.resolutions.get(i10);
            for (int i12 = 1; i12 <= 2; i12++) {
                if (!runEncoder(codec, resolution, i12)) {
                    this.mEncInfo.info.put(resolution, new Integer(i12));
                    break;
                }
                i11++;
                Listener listener = this.mListener;
                if (listener != null) {
                    listener.onProgress(i11 / size2);
                }
            }
            i10++;
        }
        Listener listener2 = this.mListener;
        if (listener2 != null) {
            listener2.onProgress(size / size2);
        }
        for (int i13 = 0; i13 < this.resolutions.size(); i13++) {
            Resolution resolution2 = this.resolutions.get(i13);
            for (int i14 = 1; i14 <= 5; i14++) {
                if (!runDecoder(codec, resolution2, i14)) {
                    this.mDecInfo.info.put(resolution2, new Integer(i14));
                    break;
                }
                size++;
                Listener listener3 = this.mListener;
                if (listener3 != null) {
                    listener3.onProgress(size / size2);
                }
            }
        }
        Listener listener4 = this.mListener;
        if (listener4 != null) {
            listener4.onProgress(size2 / size2);
        }
        Listener listener5 = this.mListener;
        if (listener5 != null) {
            listener5.onComplete(this.mDecInfo, this.mEncInfo);
        }
        MessageCtx.getInstance().Log(TAG, "ENC INFO==\n" + this.mEncInfo.toString());
        MessageCtx.getInstance().Log(TAG, "DEC INFO==\n" + this.mDecInfo.toString());
        return true;
    }

    public boolean setListener(Listener listener) {
        if (listener == null) {
            return false;
        }
        this.mListener = listener;
        MessageCtx.getInstance().setListener(this.mListener);
        return true;
    }

    public class DecoderThread extends Thread {
        private AssetManager assetMgr;
        private Codec.Type codec;
        private EvtQueue evt;
        private String fileDir;
        private int idx;
        private Resolution res;

        public DecoderThread(Codec.Type codec, Resolution res, int idx, EvtQueue que, String dir) {
            this.codec = codec;
            this.res = res;
            this.idx = idx;
            this.evt = que;
            this.fileDir = dir;
        }

        @Override // java.lang.Thread, java.lang.Runnable
        public void run() {
            boolean z10;
            int iInit;
            VDecoder vDecoder = null;
            AssetFileDescriptor assetFileDescriptorOpenFd = null;
            if (this.assetMgr != null) {
                try {
                    assetFileDescriptorOpenFd = this.assetMgr.openFd(Utils.nameForResolution(this.codec, this.res));
                } catch (Exception unused) {
                }
                vDecoder = new VDecoder(assetFileDescriptorOpenFd);
            } else {
                String str = this.fileDir;
                if (str != null) {
                    vDecoder = new VDecoder(Utils.pathForResolution(this.codec, this.res, str));
                }
            }
            boolean z11 = false;
            while (this.evt.waitAction() != 0) {
                if (z11) {
                    int iDecodeNext = vDecoder.decodeNext();
                    MessageCtx.getInstance().Log(CodecInspector.TAG, "decoder : " + this.idx + ", decode : " + iDecodeNext);
                    z10 = z11;
                    iInit = iDecodeNext;
                } else {
                    iInit = vDecoder.Init();
                    MessageCtx.getInstance().Log(CodecInspector.TAG, "decoder : " + this.idx + ", init : " + iInit);
                    z10 = true;
                }
                this.evt.notify(iInit);
                z11 = z10;
            }
            vDecoder.Uninit();
        }

        public DecoderThread(Codec.Type codec, Resolution res, int idx, EvtQueue que, AssetManager am2) {
            this.codec = codec;
            this.res = res;
            this.idx = idx;
            this.evt = que;
            this.assetMgr = am2;
        }
    }

    public CodecInspector(AssetManager assetMgr) {
        this.mListener = null;
        this.resolutions = new ArrayList<>();
        this.mEncInfo = new EncodeInfo();
        this.mDecInfo = new DecodeInfo();
        this.dataDir = null;
        this.assetMgr = assetMgr;
        this.resolutions.add(Resolution.Res720p);
        this.resolutions.add(Resolution.Res1080p);
        this.resolutions.add(Resolution.Res2K);
        this.resolutions.add(Resolution.Res4K);
    }
}
