#include <jni.h>
#include <stdlib.h>
#include <android/log.h>
#include <lame.h>
#include <FLAC/stream_encoder.h>

#define TAG "gravocodecs"
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, TAG, __VA_ARGS__)

/* ============================= LAME ============================= */

JNIEXPORT jlong JNICALL
Java_com_gravo_grabadora_audio_encode_NativeCodecs_lameInit(
        JNIEnv *env, jobject thiz, jint sample_rate, jint channels, jint bitrate_kbps) {
    lame_global_flags *gf = lame_init();
    if (!gf) return 0;
    lame_set_in_samplerate(gf, sample_rate);
    lame_set_out_samplerate(gf, sample_rate);
    lame_set_num_channels(gf, channels);
    lame_set_mode(gf, channels == 1 ? MONO : JOINT_STEREO);
    lame_set_brate(gf, bitrate_kbps);
    lame_set_quality(gf, 2);
    if (lame_init_params(gf) < 0) {
        lame_close(gf);
        return 0;
    }
    return (jlong) (intptr_t) gf;
}

JNIEXPORT jint JNICALL
Java_com_gravo_grabadora_audio_encode_NativeCodecs_lameEncode(
        JNIEnv *env, jobject thiz, jlong handle, jfloatArray pcm, jint frames, jbyteArray out) {
    lame_global_flags *gf = (lame_global_flags *) (intptr_t) handle;
    jfloat *pcm_buf = (*env)->GetFloatArrayElements(env, pcm, NULL);
    jbyte *out_buf = (*env)->GetByteArrayElements(env, out, NULL);
    jsize out_len = (*env)->GetArrayLength(env, out);
    int channels = lame_get_num_channels(gf);
    int written;
    if (channels == 1) {
        written = lame_encode_buffer_ieee_float(gf, pcm_buf, pcm_buf, frames,
                                                (unsigned char *) out_buf, out_len);
    } else {
        written = lame_encode_buffer_interleaved_ieee_float(gf, pcm_buf, frames,
                                                            (unsigned char *) out_buf, out_len);
    }
    (*env)->ReleaseFloatArrayElements(env, pcm, pcm_buf, JNI_ABORT);
    (*env)->ReleaseByteArrayElements(env, out, out_buf, 0);
    if (written < 0) LOGE("lame_encode error %d", written);
    return written;
}

JNIEXPORT jint JNICALL
Java_com_gravo_grabadora_audio_encode_NativeCodecs_lameFlush(
        JNIEnv *env, jobject thiz, jlong handle, jbyteArray out) {
    lame_global_flags *gf = (lame_global_flags *) (intptr_t) handle;
    jbyte *out_buf = (*env)->GetByteArrayElements(env, out, NULL);
    jsize out_len = (*env)->GetArrayLength(env, out);
    int written = lame_encode_flush(gf, (unsigned char *) out_buf, out_len);
    (*env)->ReleaseByteArrayElements(env, out, out_buf, 0);
    return written;
}

JNIEXPORT void JNICALL
Java_com_gravo_grabadora_audio_encode_NativeCodecs_lameClose(
        JNIEnv *env, jobject thiz, jlong handle) {
    lame_close((lame_global_flags *) (intptr_t) handle);
}

/* ============================= FLAC ============================= */

JNIEXPORT jlong JNICALL
Java_com_gravo_grabadora_audio_encode_NativeCodecs_flacInit(
        JNIEnv *env, jobject thiz, jstring path, jint sample_rate, jint channels, jint bps) {
    FLAC__StreamEncoder *enc = FLAC__stream_encoder_new();
    if (!enc) return 0;
    FLAC__stream_encoder_set_channels(enc, (unsigned) channels);
    FLAC__stream_encoder_set_bits_per_sample(enc, (unsigned) bps);
    FLAC__stream_encoder_set_sample_rate(enc, (unsigned) sample_rate);
    FLAC__stream_encoder_set_compression_level(enc, 5);
    const char *cpath = (*env)->GetStringUTFChars(env, path, NULL);
    FLAC__StreamEncoderInitStatus st =
            FLAC__stream_encoder_init_file(enc, cpath, NULL, NULL);
    (*env)->ReleaseStringUTFChars(env, path, cpath);
    if (st != FLAC__STREAM_ENCODER_INIT_STATUS_OK) {
        LOGE("flac init error %d", st);
        FLAC__stream_encoder_delete(enc);
        return 0;
    }
    return (jlong) (intptr_t) enc;
}

JNIEXPORT jboolean JNICALL
Java_com_gravo_grabadora_audio_encode_NativeCodecs_flacWrite(
        JNIEnv *env, jobject thiz, jlong handle, jintArray samples, jint frames) {
    FLAC__StreamEncoder *enc = (FLAC__StreamEncoder *) (intptr_t) handle;
    jint *buf = (*env)->GetIntArrayElements(env, samples, NULL);
    FLAC__bool ok = FLAC__stream_encoder_process_interleaved(enc, (const FLAC__int32 *) buf,
                                                             (unsigned) frames);
    (*env)->ReleaseIntArrayElements(env, samples, buf, JNI_ABORT);
    return ok ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT jboolean JNICALL
Java_com_gravo_grabadora_audio_encode_NativeCodecs_flacFinish(
        JNIEnv *env, jobject thiz, jlong handle) {
    FLAC__StreamEncoder *enc = (FLAC__StreamEncoder *) (intptr_t) handle;
    FLAC__bool ok = FLAC__stream_encoder_finish(enc);
    FLAC__stream_encoder_delete(enc);
    return ok ? JNI_TRUE : JNI_FALSE;
}
