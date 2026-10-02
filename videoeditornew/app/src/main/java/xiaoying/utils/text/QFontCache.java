package xiaoying.utils.text;

import android.graphics.Typeface;
import java.io.File;
import java.util.HashMap;
import java.util.Map;

/* JADX INFO: loaded from: classes19.dex */
public class QFontCache {
    private static final int DEFAULT_CAPACITY = 6;
    private static final String LOG_TAG = "QFontCache";
    private static TextFontTypeFaceCache gCache = new TextFontTypeFaceCache();

    public static void Cleanup() {
        synchronized (QFontCache.class) {
            gCache.Clear();
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static boolean DoesFileExist(String file) {
        return file != null && file.length() > 0 && new File(file).exists();
    }

    public static Typeface GetTypeFace(String path) {
        Typeface typefaceGetTypeFace;
        if (path == null) {
            return null;
        }
        synchronized (QFontCache.class) {
            typefaceGetTypeFace = gCache.GetTypeFace(path, 0);
        }
        return typefaceGetTypeFace;
    }

    public static class TextFontTypeFaceCache {
        private Map<String, TypefaceItem> mCache;
        private int mMaxSize;

        public class TypefaceItem {
            private Typeface face;
            private int weight = 0;
            private Typeface faceBold = null;
            private Typeface faceItalic = null;
            private Typeface faceBoldAndItalic = null;

            public TypefaceItem(Typeface tf2) {
                this.face = tf2;
            }

            private Typeface CreateSubFace(int type) {
                try {
                    return Typeface.create(this.face, type);
                } catch (Exception unused) {
                    return null;
                }
            }

            public Typeface GetFace() {
                return this.face;
            }

            public Typeface GetSubFace(int type) {
                Typeface typeface = this.face;
                if (typeface == null) {
                    return null;
                }
                if (type == 0) {
                    return typeface;
                }
                if (type == 1) {
                    if (this.faceBold == null) {
                        this.faceBold = CreateSubFace(1);
                    }
                    return this.faceBold;
                }
                if (type == 2) {
                    if (this.faceItalic == null) {
                        this.faceItalic = CreateSubFace(2);
                    }
                    return this.faceItalic;
                }
                if (type != 3) {
                    return null;
                }
                if (this.faceBoldAndItalic == null) {
                    this.faceBoldAndItalic = CreateSubFace(3);
                }
                return this.faceBoldAndItalic;
            }

            public int GetWeight() {
                return this.weight;
            }

            public void IncWeight() {
                this.weight++;
            }
        }

        public TextFontTypeFaceCache(int s10) {
            this.mCache = new HashMap();
            this.mMaxSize = s10;
        }

        public void Clear() {
            this.mCache = new HashMap();
        }

        public Typeface GetTypeFace(String font_path, int subType) {
            Typeface typefaceCreateFromFile;
            String key = null;
            if (font_path == null) {
                return null;
            }
            if (this.mCache.containsKey(font_path)) {
                TypefaceItem typefaceItem = this.mCache.get(font_path);
                typefaceItem.IncWeight();
                return typefaceItem.GetSubFace(subType);
            }
            if (QFontCache.DoesFileExist(font_path)) {
                try {
                    typefaceCreateFromFile = Typeface.createFromFile(font_path);
                } catch (Exception unused) {
                    typefaceCreateFromFile = null;
                }
            } else {
                typefaceCreateFromFile = null;
            }
            if (typefaceCreateFromFile == null) {
                return null;
            }
            if (this.mCache.size() >= this.mMaxSize) {
                int i10 = -1;
                for (Map.Entry<String, TypefaceItem> entry : this.mCache.entrySet()) {
                    int iGetWeight = entry.getValue().GetWeight();
                    if (iGetWeight < i10 || i10 == -1) {
                        key = entry.getKey();
                        i10 = iGetWeight;
                    }
                }
                if (key != null) {
                    this.mCache.remove(key);
                }
            }
            TypefaceItem typefaceItem2 = new TypefaceItem(typefaceCreateFromFile);
            this.mCache.put(font_path, typefaceItem2);
            return typefaceItem2.GetSubFace(subType);
        }

        public TextFontTypeFaceCache() {
            this(6);
        }
    }

    public static Typeface GetTypeFace(String path, int subType) {
        Typeface typefaceGetTypeFace;
        if (path == null) {
            return null;
        }
        synchronized (QFontCache.class) {
            typefaceGetTypeFace = gCache.GetTypeFace(path, subType);
        }
        return typefaceGetTypeFace;
    }
}
