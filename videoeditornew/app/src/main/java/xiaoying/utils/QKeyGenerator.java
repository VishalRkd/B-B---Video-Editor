package xiaoying.utils;

import android.util.Base64;
import java.security.Key;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.HashMap;
import java.util.Map;

/* JADX INFO: loaded from: classes19.dex */
public class QKeyGenerator {
    public static final String PRIVATE_KEY = "k2";
    public static final String PUBLIC_KEY = "k1";

    public static String getKeyString(Key key) throws Exception {
        return Base64.encodeToString(key.getEncoded(), 8);
    }

    public static PrivateKey getPrivateKey(String key) throws Exception {
        return KeyFactory.getInstance("RSA").generatePrivate(new PKCS8EncodedKeySpec(Base64.decode(key, 8)));
    }

    public static PublicKey getPublicKey(String key) throws Exception {
        return KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(Base64.decode(key, 8)));
    }

    public static Map<String, String> make(int nKeySize) {
        HashMap map = new HashMap();
        try {
            KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
            keyPairGenerator.initialize(nKeySize);
            KeyPair keyPairGenerateKeyPair = keyPairGenerator.generateKeyPair();
            RSAPublicKey rSAPublicKey = (RSAPublicKey) keyPairGenerateKeyPair.getPublic();
            RSAPrivateKey rSAPrivateKey = (RSAPrivateKey) keyPairGenerateKeyPair.getPrivate();
            map.put(PUBLIC_KEY, getKeyString(rSAPublicKey));
            map.put(PRIVATE_KEY, getKeyString(rSAPrivateKey));
        } catch (Exception e10) {
            e10.printStackTrace();
        }
        return map;
    }
}
