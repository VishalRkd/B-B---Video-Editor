package xiaoying.utils;

import android.os.Handler;
import android.view.View;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;


/* JADX INFO: loaded from: classes19.dex */
public class QComUtils {
    public static void resetInstanceMembers(Object objReset) {
        if (objReset == null) {
            return;
        }
        try {
            for (Field field : Class.forName(objReset.getClass().getName()).getDeclaredFields()) {
                try {
                    Class<?> type = field.getType();
                    String string = Modifier.toString(field.getModifiers());
                    if (string.indexOf("final") == -1 && string.indexOf("static") == -1 && !type.isPrimitive()) {
                        field.setAccessible(true);
                        Object obj = field.get(objReset);
                        if (obj != null) {
                            if (obj instanceof View) {
                                ((View) obj).setVisibility(8);
                            } else if (obj instanceof Handler) {
                                ((Handler) obj).removeCallbacksAndMessages(null);
                            }
                            field.set(objReset, null);
                        }
                    }
                } catch (Throwable unused) {
                }
            }
        } catch (Throwable th2) {
            th2.printStackTrace();
        }
    }
}
