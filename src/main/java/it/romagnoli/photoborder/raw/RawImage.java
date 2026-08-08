package it.romagnoli.photoborder.raw;

public class RawImage {

    private final int width;
    private final int height;
    private final short[] pixels;

    public RawImage(int width, int height, short[] pixels) {
        this.width = width;
        this.height = height;
        this.pixels = pixels;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public short[] getPixels() {
        return pixels;
    }
}
