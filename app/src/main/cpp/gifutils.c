#include <jni.h>
#include <string.h>
#include <stdbool.h>
#include <unistd.h>
#include <android/log.h>
#include <sys/stat.h>
#include <sys/mman.h>

#define LOG_TAG "gifUtils"

#include "ehviewer.h"

#define GIF_HEADER_87A "GIF87a"
#define GIF_HEADER_89A "GIF89a"
#define GIF_HEADER_LENGTH 6

static int FRAME_DELAY_START_MARKER = 0x0021F904;

typedef signed char byte;

#define FRAME_DELAY_START_MARKER ((byte*)(&FRAME_DELAY_START_MARKER))
#define MINIMUM_FRAME_DELAY 2
#define DEFAULT_FRAME_DELAY 10

static inline bool isGif(void *addr) {
    return !memcmp(addr, GIF_HEADER_87A, GIF_HEADER_LENGTH) ||
           !memcmp(addr, GIF_HEADER_89A, GIF_HEADER_LENGTH);
}

static void doRewrite(byte *addr, size_t size) {
    if (size < 7 || !isGif(addr)) return;
    for (size_t i = 0; i < size - 8; i++) {
        // TODO: Optimize this hex find with SIMD?
        if (addr[i] == FRAME_DELAY_START_MARKER[3] && addr[i + 1] == FRAME_DELAY_START_MARKER[2] &&
            addr[i + 2] == FRAME_DELAY_START_MARKER[1] &&
            addr[i + 3] == FRAME_DELAY_START_MARKER[0]) {
            byte *end = addr + i + 4;
            if (end[4] != 0) continue;
            int frameDelay = end[2] << 8 | end[1];
            if (frameDelay > MINIMUM_FRAME_DELAY)
                break; // Quit if the first block looks normal, for performance
            end[1] = DEFAULT_FRAME_DELAY;
            end[2] = 0;
        }
    }
}

JNIEXPORT jboolean JNICALL
Java_com_hippo_ehviewer_jni_GifUtilsKt_isGif(JNIEnv *env, jclass clazz, jint fd) {
    byte buffer[GIF_HEADER_LENGTH];
    return read(fd, buffer, GIF_HEADER_LENGTH) == GIF_HEADER_LENGTH &&
           isGif(buffer);
}

JNIEXPORT void JNICALL
Java_com_hippo_ehviewer_jni_GifUtilsKt_rewriteGifSource(JNIEnv *env, jclass clazz, jobject buffer) {
    byte *addr = (*env)->GetDirectBufferAddress(env, buffer);
    size_t size = (*env)->GetDirectBufferCapacity(env, buffer);
    doRewrite(addr, size);
}

JNIEXPORT jobject JNICALL
Java_com_hippo_ehviewer_jni_GifUtilsKt_mmap(JNIEnv *env, jclass clazz, jint fd) {
    struct stat64 st;
    fstat64(fd, &st);
    size_t size = st.st_size;
    byte *addr = mmap64(0, size, PROT_READ | PROT_WRITE, MAP_PRIVATE, fd, 0);
    if (addr == MAP_FAILED) return NULL;
    return (*env)->NewDirectByteBuffer(env, addr, size);
}

JNIEXPORT void JNICALL
Java_com_hippo_ehviewer_jni_GifUtilsKt_munmap(JNIEnv *env, jclass clazz, jobject buffer) {
    byte *addr = (*env)->GetDirectBufferAddress(env, buffer);
    size_t size = (*env)->GetDirectBufferCapacity(env, buffer);
    munmap(addr, size);
}
