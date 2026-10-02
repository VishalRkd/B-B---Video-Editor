package xiaoying.utils;

/* JADX INFO: loaded from: classes19.dex */
class Codec {

    /* JADX INFO: renamed from: xiaoying.utils.Codec$1 */
    public static /* synthetic */ class C480291 {
        static final /* synthetic */ int[] $SwitchMap$xiaoying$utils$Codec$Type;

        static {
            int[] iArr = new int[Type.values().length];
            $SwitchMap$xiaoying$utils$Codec$Type = iArr;
            try {
                iArr[Type.kAVC.ordinal()] = 1;
            } catch (NoSuchFieldError unused) {
            }
            try {
                $SwitchMap$xiaoying$utils$Codec$Type[Type.kHEVC.ordinal()] = 2;
            } catch (NoSuchFieldError unused2) {
            }
        }
    }

    public enum Type {
        kNone,
        kAVC,
        kHEVC
    }

    public static String toMediaFormatType(Type type) {
        int i10 = C480291.$SwitchMap$xiaoying$utils$Codec$Type[type.ordinal()];
        if (i10 != 1) {
            return i10 != 2 ? "" : "video/hevc";
        }
        return "video/avc";
    }
}
