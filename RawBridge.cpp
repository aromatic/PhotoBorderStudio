#include <jni.h>
#include <libraw/libraw.h>
#include <string>

extern "C" {

JNIEXPORT jobject JNICALL
Java_RawImageJNI_load(JNIEnv *env, jobject obj, jstring filename)
{
    const char *file = env->GetStringUTFChars(filename, nullptr);

    LibRaw raw;

    // Apertura file
    int ret = raw.open_file(file);
    if (ret != LIBRAW_SUCCESS) {
        env->ReleaseStringUTFChars(filename, file);
        return nullptr;
    }

    // Estrazione dati RAW
    ret = raw.unpack();
    if (ret != LIBRAW_SUCCESS) {
        raw.recycle();
        env->ReleaseStringUTFChars(filename, file);
        return nullptr;
    }

    // Parametri di sviluppo
    raw.imgdata.params.output_bps = 16;
    raw.imgdata.params.use_camera_wb = 1;
    raw.imgdata.params.no_auto_bright = 1;
    raw.imgdata.params.output_color = 1;   // sRGB

    // Demosaicizzazione
    ret = raw.dcraw_process();
    if (ret != LIBRAW_SUCCESS) {
        raw.recycle();
        env->ReleaseStringUTFChars(filename, file);
        return nullptr;
    }

    // Immagine RGB risultante
    int err = 0;
    libraw_processed_image_t *img =
        raw.dcraw_make_mem_image(&err);

    env->ReleaseStringUTFChars(filename, file);

    if (!img)
        return nullptr;

    int width  = img->width;
    int height = img->height;
    int colors = img->colors;

    int elements = width * height * colors;

    jshortArray pixels = env->NewShortArray(elements);

    env->SetShortArrayRegion(
            pixels,
            0,
            elements,
            reinterpret_cast<jshort*>(img->data));

    // Costruzione dell'oggetto Java RawImage
    jclass cls = env->FindClass("RawImage");

    jmethodID ctor =
        env->GetMethodID(
            cls,
            "<init>",
            "(II[S)V");

    jobject result =
        env->NewObject(
            cls,
            ctor,
            width,
            height,
            pixels);

    LibRaw::dcraw_clear_mem(img);
    raw.recycle();

    return result;
}

}
