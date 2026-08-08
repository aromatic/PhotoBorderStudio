package it.romagnoli.photoborder.raw;

import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.Pointer;

/**
 * Binding JNA verso la libreria nativa LibRaw (libraw.so / libraw.dll / libraw.dylib).
 * <p>
 * Espone solo il sottoinsieme di funzioni della API C di LibRaw necessarie per aprire un file
 * RAW, decodificarlo (demosaicing) ed estrarre l'immagine risultante come buffer di byte RGB.
 * La firma delle funzioni rispecchia quella dichiarata in {@code libraw.h}; i tipi opachi
 * (puntatori a struct) vengono trattati come {@link Pointer} generici perché in questo esempio
 * non serve leggerne i campi interni, ma solo passarli da una chiamata all'altra.
 * <p>
 * Nome della libreria: su Linux JNA cerca automaticamente {@code libraw.so} (o
 * {@code libraw.so.<versione>} tramite i normali meccanismi di risoluzione di ldconfig).
 */
public interface LibRawNative extends Library {

    LibRawNative INSTANCE = Native.load("raw", LibRawNative.class);

    /** Crea un nuovo contesto di elaborazione (equivalente a {@code libraw_init(0)}). */
    Pointer libraw_init(int flags);

    /** Apre e legge l'header/i metadati del file RAW indicato. Ritorna 0 in caso di successo. */
    int libraw_open_file(Pointer rawData, String filename);

    /** Decomprime i dati grezzi del sensore (Bayer/X-Trans) in memoria. Ritorna 0 se ok. */
    int libraw_unpack(Pointer rawData);

    /**
     * Esegue la pipeline di sviluppo RAW->RGB (demosaicing, white balance, gamma, ecc.)
     * usando i parametri di default o quelli eventualmente impostati su {@code rawData.params}.
     * Ritorna 0 se ok.
     */
    int libraw_dcraw_process(Pointer rawData);

    /**
     * Produce l'immagine elaborata in memoria (struct {@code libraw_processed_image_t*}).
     * Il puntatore ritornato va liberato con {@link #libraw_dcraw_clear_mem(Pointer)}.
     * L'ultimo parametro (errcode) è opzionale: passiamo null.
     */
    Pointer libraw_dcraw_make_mem_image(Pointer rawData, Pointer errcode);

    /** Libera la memoria allocata da {@link #libraw_dcraw_make_mem_image}. */
    void libraw_dcraw_clear_mem(Pointer processedImage);

    /** Chiude e libera tutte le risorse associate al contesto RAW. */
    void libraw_close(Pointer rawData);

    /**
     * Layout della struct {@code libraw_processed_image_t} (vedi libraw_types.h):
     * <pre>
     * typedef struct {
     *   enum LibRaw_image_formats type; // int (4 byte)
     *   ushort height, width;           // 2 + 2 byte
     *   ushort colors;                  // 2 byte
     *   ushort bits;                    // 2 byte
     *   unsigned int data_size;         // 4 byte
     *   unsigned char data[1];          // dati pixel a seguire
     * } libraw_processed_image_t;
     * </pre>
     * Gli offset qui sotto sono usati per leggere i campi direttamente dal {@link Pointer}
     * senza dover dichiarare una {@code Structure} JNA completa.
     */
    int OFFSET_TYPE = 0;
    int OFFSET_HEIGHT = 4;
    int OFFSET_WIDTH = 6;
    int OFFSET_COLORS = 8;
    int OFFSET_BITS = 10;
    int OFFSET_DATA_SIZE = 12;
    int OFFSET_DATA = 16;
}
