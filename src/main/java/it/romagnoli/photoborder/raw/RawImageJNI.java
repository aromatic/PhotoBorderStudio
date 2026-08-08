package it.romagnoli.photoborder.raw;

public class RawImageJNI {

    static {
        System.loadLibrary("rawjni");
    }

    public native RawImage load(String filename);

}
