package it.romagnoli.photoborder.raw;

import com.sun.jna.Pointer;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

/**
 * Utility ad alto livello per decodificare file RAW (CR2, NEF, ARW, DNG, RAF, ...) sfruttando
 * la libreria nativa LibRaw tramite il binding JNA {@link LibRawNative}. Nasconde la gestione
 * dei puntatori nativi e la lettura manuale della struct {@code libraw_processed_image_t},
 * restituendo direttamente un {@link BufferedImage} pronto per essere convertito in
 * {@code javafx.scene.image.Image} tramite {@code SwingFXUtils.toFXImage}.
 */
public final class RawImageReader {

    private RawImageReader() {
    }

    /**
     * Decodifica il file RAW indicato ed effettua l'intera pipeline di sviluppo (demosaicing,
     * bilanciamento del bianco automatico della fotocamera, gamma) usando le impostazioni di
     * default di LibRaw.
     *
     * @param rawFile file RAW da leggere (es. .CR2, .NEF, .ARW, .DNG, .RAF)
     * @return immagine decodificata come RGB a 8 bit per canale
     * @throws IOException se l'apertura, la decodifica o l'elaborazione falliscono
     */
    public static BufferedImage read(File rawFile) throws IOException {
        LibRawNative lib = LibRawNative.INSTANCE;

        Pointer rawData = lib.libraw_init(0);
        if (rawData == null) {
            throw new IOException("Impossibile inizializzare LibRaw (libraw_init ha ritornato NULL)");
        }

        try {
            checkResult(lib.libraw_open_file(rawData, rawFile.getAbsolutePath()),
                    "apertura del file " + rawFile.getName());

            checkResult(lib.libraw_unpack(rawData), "decompressione dati sensore");

            checkResult(lib.libraw_dcraw_process(rawData), "elaborazione RAW->RGB");

            Pointer processedImage = lib.libraw_dcraw_make_mem_image(rawData, null);
            if (processedImage == null) {
                throw new IOException("libraw_dcraw_make_mem_image ha ritornato NULL");
            }

            try {
                return toBufferedImage(processedImage);
            } finally {
                lib.libraw_dcraw_clear_mem(processedImage);
            }
        } finally {
            lib.libraw_close(rawData);
        }
    }

    /**
     * Legge i campi della struct {@code libraw_processed_image_t} direttamente dalla memoria
     * nativa tramite gli offset dichiarati in {@link LibRawNative} e copia i pixel in un
     * {@link BufferedImage} Java, gestendo sia l'output a 8 bit che quello a 16 bit per canale.
     */
    private static BufferedImage toBufferedImage(Pointer processedImage) throws IOException {
        int height = processedImage.getShort(LibRawNative.OFFSET_HEIGHT) & 0xFFFF;
        int width = processedImage.getShort(LibRawNative.OFFSET_WIDTH) & 0xFFFF;
        int colors = processedImage.getShort(LibRawNative.OFFSET_COLORS) & 0xFFFF;
        int bits = processedImage.getShort(LibRawNative.OFFSET_BITS) & 0xFFFF;

        if (colors != 3) {
            throw new IOException("Formato colore non supportato (colors=" + colors + "), attesi 3 canali RGB");
        }

        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        int bytesPerSample = bits > 8 ? 2 : 1;
        long dataOffset = LibRawNative.OFFSET_DATA;

        int[] row = new int[width];
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                long pixelOffset = dataOffset + ((long) y * width + x) * colors * bytesPerSample;
                int r = readSample(processedImage, pixelOffset, bytesPerSample);
                int g = readSample(processedImage, pixelOffset + bytesPerSample, bytesPerSample);
                int b = readSample(processedImage, pixelOffset + 2L * bytesPerSample, bytesPerSample);

                if (bytesPerSample == 2) {
                    // LibRaw con output a 16 bit: riportiamo a 8 bit per il BufferedImage.
                    r >>= 8;
                    g >>= 8;
                    b >>= 8;
                }
                row[x] = (r << 16) | (g << 8) | b;
            }
            image.setRGB(0, y, width, 1, row, 0, width);
        }

        return image;
    }

    private static int readSample(Pointer memory, long offset, int bytesPerSample) {
        if (bytesPerSample == 1) {
            return memory.getByte(offset) & 0xFF;
        }
        return memory.getShort(offset) & 0xFFFF;
    }

    private static void checkResult(int libRawErrorCode, String operationDescription) throws IOException {
        if (libRawErrorCode != 0) {
            throw new IOException("Errore LibRaw durante " + operationDescription
                    + " (codice " + libRawErrorCode + ")");
        }
    }
}
