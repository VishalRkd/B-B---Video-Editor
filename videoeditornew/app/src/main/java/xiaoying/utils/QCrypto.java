package xiaoying.utils;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

/* JADX INFO: loaded from: classes19.dex */
public class QCrypto {
    private QCrypto() {
    }

    public static String aesDecrypt(byte[] bytes, String key) throws Exception {
        if (bytes == null || key == null) {
            return null;
        }
        Cipher cipher = Cipher.getInstance("AES");
        cipher.init(2, new SecretKeySpec(key.getBytes("utf-8"), "AES"));
        return new String(cipher.doFinal(bytes), "utf-8");
    }

    public static byte[] aesEncrypt(String str, String key) throws Exception {
        if (str == null || key == null) {
            return null;
        }
        Cipher cipher = Cipher.getInstance("AES");
        cipher.init(1, new SecretKeySpec(key.getBytes("utf-8"), "AES"));
        return cipher.doFinal(str.getBytes("utf-8"));
    }
}
