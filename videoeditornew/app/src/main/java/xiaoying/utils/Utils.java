package xiaoying.utils;

import android.util.Pair;


import xiaoying.engine.base.QUtils;

/* JADX INFO: loaded from: classes19.dex */
class Utils {

    /* JADX INFO: renamed from: xiaoying.utils.Utils$1 */
    public static /* synthetic */ class C480351 {
        static final /* synthetic */ int[] $SwitchMap$xiaoying$utils$CodecInspector$Resolution;

        static {
            int[] iArr = new int[CodecInspector.Resolution.values().length];
            $SwitchMap$xiaoying$utils$CodecInspector$Resolution = iArr;
            try {
                iArr[CodecInspector.Resolution.Res4K.ordinal()] = 1;
            } catch (NoSuchFieldError unused) {
            }
            try {
                $SwitchMap$xiaoying$utils$CodecInspector$Resolution[CodecInspector.Resolution.Res2K.ordinal()] = 2;
            } catch (NoSuchFieldError unused2) {
            }
            try {
                $SwitchMap$xiaoying$utils$CodecInspector$Resolution[CodecInspector.Resolution.Res1080p.ordinal()] = 3;
            } catch (NoSuchFieldError unused3) {
            }
            try {
                $SwitchMap$xiaoying$utils$CodecInspector$Resolution[CodecInspector.Resolution.Res720p.ordinal()] = 4;
            } catch (NoSuchFieldError unused4) {
            }
        }
    }

    public static int bitrateForResolution(CodecInspector.Resolution res) {
        int i10 = C480351.$SwitchMap$xiaoying$utils$CodecInspector$Resolution[res.ordinal()];
        if (i10 == 1) {
            return 31457280;
        }
        if (i10 == 2) {
            return 15728640;
        }
        if (i10 != 3) {
            return i10 != 4 ? 0 : 5242880;
        }
        return 8388608;
    }

    public static String nameForResolution(Codec.Type codec, CodecInspector.Resolution res) {
        if (codec == Codec.Type.kAVC) {
            int i10 = C480351.$SwitchMap$xiaoying$utils$CodecInspector$Resolution[res.ordinal()];
            if (i10 == 1) {
                return "test4k.mp4";
            }
            if (i10 == 2) {
                return "test2k.mp4";
            }
            if (i10 == 3) {
                return "test1080p.mp4";
            }
            if (i10 == 4) {
                return "test720p.mp4";
            }
        } else if (codec == Codec.Type.kHEVC) {
            int i11 = C480351.$SwitchMap$xiaoying$utils$CodecInspector$Resolution[res.ordinal()];
            if (i11 == 1) {
                return "test4k-hevc.mp4";
            }
            if (i11 == 2) {
                return "test2k-hevc.mp4";
            }
            if (i11 == 3) {
                return "test1080p-hevc.mp4";
            }
            if (i11 == 4) {
                return "test720p-hevc.mp4";
            }
        }
        return null;
    }

    public static String pathForResolution(Codec.Type codec, CodecInspector.Resolution res, String prefix) {
        String strNameForResolution = nameForResolution(codec, res);
        if (strNameForResolution == null) {
            return strNameForResolution;
        }
        return prefix + "/" + strNameForResolution;
    }

    public static Pair<Integer, Integer> sizeForResolution(CodecInspector.Resolution res) {
        int i10 = C480351.$SwitchMap$xiaoying$utils$CodecInspector$Resolution[res.ordinal()];
        if (i10 == 1) {
            return new Pair<>(Integer.valueOf(QUtils.VIDEO_RES_4K_WIDTH), 2160);
        }
        if (i10 == 2) {
            return new Pair<>(Integer.valueOf(QUtils.VIDEO_RES_2K_WIDTH), 1440);
        }
        if (i10 != 3) {
            return i10 != 4 ? new Pair<>(0, 0) : new Pair<>(1280, 720);
        }
        return new Pair<>(1920, 1080);
    }
}
