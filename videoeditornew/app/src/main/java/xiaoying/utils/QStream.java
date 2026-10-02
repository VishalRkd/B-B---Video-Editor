package xiaoying.utils;

/* JADX INFO: loaded from: classes19.dex */
public final class QStream {
    private static final int INVALID_HMSTREAM = 0;
    public static final int STREAM_APPEND = 3;
    public static final int STREAM_ASYNC_WRITE = 7;
    public static final int STREAM_A_PLUS = 6;
    public static final int STREAM_BEGIN = 0;
    public static final int STREAM_CONSUME_NORMAL = 1;
    public static final int STREAM_CONSUME_PREVIEW = 2;
    public static final int STREAM_CUR = 2;
    public static final int STREAM_DRM_READ = 257;
    public static final int STREAM_END = 1;
    public static final int STREAM_READ = 1;
    public static final int STREAM_R_PLUS = 4;
    public static final int STREAM_WRITE = 2;
    public static final int STREAM_W_PLUS = 5;
    protected long mNativeStream = 0;

    public static boolean fileCopy(String dstName, String srcName, boolean failIfExists) {
        return native_StreamFileCopy(dstName, srcName, failIfExists);
    }

    public static boolean fileDelete(String fileName) {
        return native_StreamFileDelete(fileName);
    }

    public static boolean fileExists(String fileName) {
        return native_StreamFileExists(fileName);
    }

    public static int fileGetSize(String fileName) {
        return native_StreamFileGetSize(fileName);
    }

    public static boolean fileMove(String dstName, String srcName) {
        return native_StreamFileMove(dstName, srcName);
    }

    public static boolean fileRename(String oldName, String newName) {
        return native_StreamFileRename(oldName, newName);
    }

    private native boolean native_StreamClose(long hStream);

    private native long native_StreamCreate(String fileName);

    private static native boolean native_StreamFileCopy(String dstName, String srcName, boolean failIfExists);

    private static native boolean native_StreamFileDelete(String fileName);

    private static native boolean native_StreamFileExists(String fileName);

    private static native int native_StreamFileGetSize(String fileName);

    private static native boolean native_StreamFileMove(String dstName, String srcName);

    private static native boolean native_StreamFileRename(String oldName, String newName);

    private native boolean native_StreamFlush(long hStream);

    private native int native_StreamGetSize(long hStream);

    private native long native_StreamOpen(String fileName, int mode);

    private native long native_StreamOpenFromMemoryBlock(byte[] memBlock, int size);

    private native int native_StreamRead(long hStream, byte[] buf, int size);

    private native int native_StreamSeek(long hStream, int start, int offset);

    private native int native_StreamSetSize(long hStream, int length);

    private native int native_StreamTell(long hStream);

    private native int native_StreamWrite(long hStream, byte[] buf, int size);

    public boolean close() {
        return native_StreamClose(this.mNativeStream);
    }

    public boolean flush() {
        return native_StreamFlush(this.mNativeStream);
    }

    public long getNativeHandle() {
        return this.mNativeStream;
    }

    public int getSize() {
        return native_StreamGetSize(this.mNativeStream);
    }

    public long getStreamHandle() {
        return this.mNativeStream;
    }

    public boolean isValidStream() {
        return this.mNativeStream != 0;
    }

    public boolean open(String fileName, int mode) {
        long jNative_StreamOpen = native_StreamOpen(fileName, mode);
        this.mNativeStream = jNative_StreamOpen;
        return jNative_StreamOpen != 0;
    }

    public int read(byte[] buf, int size) {
        return native_StreamRead(this.mNativeStream, buf, size);
    }

    public int seek(int start, int offset) {
        return native_StreamSeek(this.mNativeStream, start, offset);
    }

    public int setSize(int size) {
        return native_StreamSetSize(this.mNativeStream, size);
    }

    public int tell() {
        return native_StreamTell(this.mNativeStream);
    }

    public int write(byte[] buf, int size) {
        return native_StreamWrite(this.mNativeStream, buf, size);
    }

    public boolean open(byte[] memBlock) {
        long jNative_StreamOpenFromMemoryBlock = native_StreamOpenFromMemoryBlock(memBlock, memBlock.length);
        this.mNativeStream = jNative_StreamOpenFromMemoryBlock;
        return jNative_StreamOpenFromMemoryBlock != 0;
    }
}
