package xiaoying.utils;

/* JADX INFO: loaded from: classes19.dex */
public final class QColorSpace {
    static final int QPAF_16BITS = 83886080;
    static final int QPAF_1BITS = 16777216;
    static final int QPAF_24BITS = 100663296;
    static final int QPAF_2BITS = 33554432;
    static final int QPAF_32BITS = 117440512;
    static final int QPAF_4BITS = 50331648;
    static final int QPAF_8BITS = 67108864;
    static final int QPAF_BGR = 4096;
    static final int QPAF_BT601_YCBCR = 4096;
    static final int QPAF_BT601_YUV = 0;
    static final int QPAF_BT709_YCBCR = 12288;
    static final int QPAF_BT709_YUV = 8192;
    public static final int QPAF_GRAY1 = 1627389952;
    public static final int QPAF_GRAY16 = 1694498816;
    public static final int QPAF_GRAY2 = 1644167168;
    public static final int QPAF_GRAY4 = 1660944384;
    public static final int QPAF_GRAY8 = 1677721600;
    static final int QPAF_GRAY_BASE = 1610612736;
    public static final int QPAF_I420;
    public static final int QPAF_I422H;
    public static final int QPAF_I422V;
    public static final int QPAF_I444;
    static final int QPAF_OTHERS = 1879048192;
    public static final int QPAF_OTHERS_DCT = 1879048193;
    public static final int QPAF_OTHERS_NV21 = 1879048194;
    public static final int QPAF_OTHERS_TEXTURE = 1879048201;
    public static final int QPAF_RGB16_B4G4R4;
    public static final int QPAF_RGB16_B5G5R5;
    public static final int QPAF_RGB16_B5G6R5;
    public static final int QPAF_RGB16_R4G4B4;
    public static final int QPAF_RGB16_R5G5B5;
    public static final int QPAF_RGB16_R5G6B5;
    public static final int QPAF_RGB16_TR5G5B5;
    static final int QPAF_RGB1_PAL = 1090519040;
    public static final int QPAF_RGB24_B6G6R6;
    public static final int QPAF_RGB24_B8G8R8;
    public static final int QPAF_RGB24_R6G6B6;
    public static final int QPAF_RGB24_R8G8B8;
    public static final int QPAF_RGB24_TR6G6B6;
    public static final int QPAF_RGB32_A8R8G8B8;
    public static final int QPAF_RGB32_B8G8R8;
    public static final int QPAF_RGB32_B8G8R8A8;
    public static final int QPAF_RGB32_R8G8B8;
    static final int QPAF_RGB4_PAL = 1124073472;
    static final int QPAF_RGB8_PAL = 1140850688;
    static final int QPAF_RGBA_BASE = 805306368;
    static final int QPAF_RGBP_BASE = 1073741824;
    static final int QPAF_RGBT_BASE = 536870912;
    static final int QPAF_RGB_BASE = 268435456;
    public static final int QPAF_UVY;
    public static final int QPAF_UYVY;
    public static final int QPAF_UYVY2;
    public static final int QPAF_VUY;
    public static final int QPAF_VYUY;
    public static final int QPAF_VYUY2;
    public static final int QPAF_YUV;
    static final int QPAF_YUV_BASE = 1342177280;
    static final int QPAF_YUV_PLANAR = 2048;
    static final int QPAF_YUV_UVY = 1024;
    static final int QPAF_YUV_VU = 512;
    static final int QPAF_YUV_Y1Y0 = 256;
    public static final int QPAF_YUYV;
    public static final int QPAF_YUYV2;
    public static final int QPAF_YV12;
    public static final int QPAF_YV16H;
    public static final int QPAF_YV16V;
    public static final int QPAF_YV24;
    public static final int QPAF_YVU;
    public static final int QPAF_YVYU;
    public static final int QPAF_YVYU2;

    static {
        int iQPAF_MAKE_R = QPAF_MAKE_R(5) | 352321536 | QPAF_MAKE_G(6) | QPAF_MAKE_B(5);
        QPAF_RGB16_R5G6B5 = iQPAF_MAKE_R;
        int iQPAF_MAKE_R2 = QPAF_MAKE_R(5) | 352321536 | QPAF_MAKE_G(5) | QPAF_MAKE_B(5);
        QPAF_RGB16_R5G5B5 = iQPAF_MAKE_R2;
        int iQPAF_MAKE_R3 = 352321536 | QPAF_MAKE_R(4) | QPAF_MAKE_G(4) | QPAF_MAKE_B(4);
        QPAF_RGB16_R4G4B4 = iQPAF_MAKE_R3;
        QPAF_RGB16_TR5G5B5 = 620756992 | QPAF_MAKE_R(5) | QPAF_MAKE_G(5) | QPAF_MAKE_B(5);
        QPAF_RGB16_B5G6R5 = iQPAF_MAKE_R | 4096;
        QPAF_RGB16_B5G5R5 = iQPAF_MAKE_R2 | 4096;
        QPAF_RGB16_B4G4R4 = iQPAF_MAKE_R3 | 4096;
        int iQPAF_MAKE_R4 = QPAF_MAKE_R(8) | 369098752 | QPAF_MAKE_G(8) | QPAF_MAKE_B(8);
        QPAF_RGB24_R8G8B8 = iQPAF_MAKE_R4;
        int iQPAF_MAKE_B = QPAF_MAKE_B(6) | 369098752 | QPAF_MAKE_R(6) | QPAF_MAKE_G(6);
        QPAF_RGB24_R6G6B6 = iQPAF_MAKE_B;
        QPAF_RGB24_TR6G6B6 = QPAF_MAKE_B(5) | 637534208 | QPAF_MAKE_R(5) | QPAF_MAKE_G(5);
        QPAF_RGB24_B8G8R8 = iQPAF_MAKE_R4 | 4096;
        QPAF_RGB24_B6G6R6 = iQPAF_MAKE_B | 4096;
        int iQPAF_MAKE_R5 = 385875968 | QPAF_MAKE_R(8) | QPAF_MAKE_G(8) | QPAF_MAKE_B(8);
        QPAF_RGB32_R8G8B8 = iQPAF_MAKE_R5;
        int iQPAF_MAKE_B2 = QPAF_MAKE_B(8) | 922746880 | QPAF_MAKE_R(8) | QPAF_MAKE_G(8);
        QPAF_RGB32_A8R8G8B8 = iQPAF_MAKE_B2;
        QPAF_RGB32_B8G8R8 = iQPAF_MAKE_R5 | 4096;
        QPAF_RGB32_B8G8R8A8 = iQPAF_MAKE_B2 | 4096;
        int iQPAF_MAKE_H = QPAF_MAKE_H(1) | 1342177280 | QPAF_MAKE_V(1);
        QPAF_YUV = iQPAF_MAKE_H;
        QPAF_YVU = iQPAF_MAKE_H | 512;
        QPAF_UVY = iQPAF_MAKE_H | 1024;
        QPAF_VUY = iQPAF_MAKE_H | 1536;
        int iQPAF_MAKE_H2 = 1342177280 | QPAF_MAKE_H(2) | QPAF_MAKE_V(1);
        QPAF_YUYV = iQPAF_MAKE_H2;
        QPAF_YVYU = iQPAF_MAKE_H2 | 512;
        QPAF_UYVY = iQPAF_MAKE_H2 | 1024;
        QPAF_VYUY = iQPAF_MAKE_H2 | 1536;
        QPAF_YUYV2 = iQPAF_MAKE_H2 | 256;
        QPAF_YVYU2 = iQPAF_MAKE_H2 | 768;
        QPAF_UYVY2 = iQPAF_MAKE_H2 | 1280;
        QPAF_VYUY2 = iQPAF_MAKE_H2 | 1792;
        int iQPAF_MAKE_H3 = QPAF_MAKE_H(2) | 1342179328 | QPAF_MAKE_V(2);
        QPAF_I420 = iQPAF_MAKE_H3;
        int iQPAF_MAKE_H4 = QPAF_MAKE_H(1) | 1342179328 | QPAF_MAKE_V(2);
        QPAF_I422V = iQPAF_MAKE_H4;
        int iQPAF_MAKE_H5 = QPAF_MAKE_H(2) | 1342179328 | QPAF_MAKE_V(1);
        QPAF_I422H = iQPAF_MAKE_H5;
        int iQPAF_MAKE_V = QPAF_MAKE_V(1) | 1342179328 | QPAF_MAKE_H(1);
        QPAF_I444 = iQPAF_MAKE_V;
        QPAF_YV12 = iQPAF_MAKE_H3 | 512;
        QPAF_YV16V = iQPAF_MAKE_H4 | 512;
        QPAF_YV16H = iQPAF_MAKE_H5 | 512;
        QPAF_YV24 = iQPAF_MAKE_V | 512;
    }

    public static final int MRGB(int r10, int g10, int b10) {
        return (r10 & 255) | ((g10 & 255) << 8) | ((b10 & 255) << 16);
    }

    public static final int MRGBA(int r10, int g10, int b10, int a10) {
        return (r10 & 255) | ((g10 & 255) << 8) | ((b10 & 255) << 16) | ((a10 & 255) << 24);
    }

    public static final int MRGBA_A(int rgb) {
        return (rgb >> 24) & 255;
    }

    public static final int MRGB_B(int rgb) {
        return (rgb >> 16) & 255;
    }

    public static final int MRGB_G(int rgb) {
        return (rgb >> 8) & 255;
    }

    public static final int MRGB_R(int rgb) {
        return rgb & 255;
    }

    public static final int MakeARGB(int a10, int r10, int g10, int b10) {
        return ((a10 & 255) << 24) | ((r10 & 255) << 16) | ((g10 & 255) << 8) | (b10 & 255);
    }

    public static final int MakeRGB(int r10, int g10, int b10) {
        return ((r10 & 255) << 16) | ((g10 & 255) << 8) | (b10 & 255);
    }

    public static final int QPAF_MAKE_B(int n10) {
        return n10 - 1;
    }

    public static final int QPAF_MAKE_G(int n10) {
        return (n10 - 1) << 4;
    }

    public static final int QPAF_MAKE_H(int n10) {
        return (n10 - 1) << 4;
    }

    public static final int QPAF_MAKE_R(int n10) {
        return (n10 - 1) << 8;
    }

    public static final int QPAF_MAKE_V(int n10) {
        return n10 - 1;
    }
}
