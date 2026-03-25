package com.lingmoyun.minilzo;

/**
 * miniLZO
 * C语言实现，通过jni封装
 *
 * @author
 * guoweifeng, edited by kimo
 *
 * @implNote
 * Since the original "libloader" class did not work for me, so it has been removed and replaced with
 * build-in java's library loader
 */
public class MiniLZO {

    static {
        System.loadLibrary("minilzo_java");
        init();
    }

    public static native void init();

    public static native byte[] compress(byte[] src);

    public static native byte[] decompress(byte[] src);

}
