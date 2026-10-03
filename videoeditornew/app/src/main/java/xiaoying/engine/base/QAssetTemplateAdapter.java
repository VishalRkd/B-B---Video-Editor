package xiaoying.engine.base;

import android.content.Context;
import android.util.Log;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * High-performance Java adapter bridging Android assets to the native Xiaoying C++ engine.
 * Automatically resolves and extracts template binaries (.xyt) for transitions, filters, and overlays.
 */
public class QAssetTemplateAdapter implements IQTemplateAdapter {
    private static final String TAG = "QAssetTemplateAdapter";

    private final Context context;
    private final File templateDir;
    private final Map<Long, String> templatePathCache = new ConcurrentHashMap<>();
    private final Map<String, Long> templateIdCache = new ConcurrentHashMap<>();

    private static final String[] ASSET_SUBFOLDERS = {
        "transition",
        "imageeffect",
        "bubbleframe",
        "glitchcover",
        "easecurve",
        "subtitlestyle",
        "watermark"
    };

    public QAssetTemplateAdapter(Context context) {
        this.context = context.getApplicationContext();
        this.templateDir = new File(this.context.getFilesDir(), "engine_templates");
        if (!this.templateDir.exists()) {
            this.templateDir.mkdirs();
        }
        indexAssets();
    }

    private void indexAssets() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    for (String folder : ASSET_SUBFOLDERS) {
                        String[] files = context.getAssets().list(folder);
                        if (files == null) continue;
                        for (String filename : files) {
                            if (filename.endsWith(".xyt")) {
                                parseAndRegister(folder, filename);
                            }
                        }
                    }
                    Log.d(TAG, "Indexed " + templatePathCache.size() + " native template assets");
                } catch (Exception e) {
                    Log.w(TAG, "Error indexing assets", e);
                }
            }
        }).start();
    }

    private void parseAndRegister(String folder, String filename) {
        try {
            String nameWithoutExt = filename.substring(0, filename.length() - 4);
            long id;
            if (nameWithoutExt.startsWith("0x") || nameWithoutExt.startsWith("0X")) {
                id = Long.parseUnsignedLong(nameWithoutExt.substring(2), 16);
            } else {
                id = Long.parseUnsignedLong(nameWithoutExt, 16);
            }
            String assetPath = folder + "/" + filename;
            templateIdCache.put(assetPath, id);
            templateIdCache.put(filename, id);
        } catch (Exception ignored) {
        }
    }

    @Override
    public String getTemplateFile(long templateID) {
        String cached = templatePathCache.get(templateID);
        if (cached != null && new File(cached).exists()) {
            return cached;
        }

        String hexName = String.format(Locale.US, "0x%016X.xyt", templateID);

        // Search in subfolders
        for (String folder : ASSET_SUBFOLDERS) {
            String assetPath = folder + "/" + hexName;
            File extracted = extractAssetIfPresent(assetPath, hexName);
            if (extracted != null) {
                templatePathCache.put(templateID, extracted.getAbsolutePath());
                return extracted.getAbsolutePath();
            }
        }

        // Return virtual uri if not found locally
        return "assets_android://transition/" + hexName;
    }

    private File extractAssetIfPresent(String assetPath, String destFilename) {
        try {
            File outFile = new File(templateDir, destFilename);
            if (outFile.exists() && outFile.length() > 0) {
                return outFile;
            }

            InputStream in = context.getAssets().open(assetPath);
            FileOutputStream out = new FileOutputStream(outFile);
            byte[] buf = new byte[8192];
            int len;
            while ((len = in.read(buf)) > 0) {
                out.write(buf, 0, len);
            }
            out.flush();
            out.close();
            in.close();
            return outFile;
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public long getTemplateID(String templateFile) {
        if (templateFile == null || templateFile.isEmpty()) return 0;

        Long cachedId = templateIdCache.get(templateFile);
        if (cachedId != null) return cachedId;

        String name = new File(templateFile).getName();
        if (name.endsWith(".xyt")) {
            name = name.substring(0, name.length() - 4);
        }

        try {
            long id;
            if (name.startsWith("0x") || name.startsWith("0X")) {
                id = Long.parseUnsignedLong(name.substring(2), 16);
            } else {
                id = Long.parseUnsignedLong(name, 16);
            }
            templateIdCache.put(templateFile, id);
            return id;
        } catch (Exception e) {
            return 0;
        }
    }

    @Override
    public String getTemplateExternalFile(long templateID, int subTemplateID, int fileID) {
        return "";
    }
}
