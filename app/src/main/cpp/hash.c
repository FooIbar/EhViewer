#include <errno.h>
#include <stdlib.h>
#include <string.h>
#include <unistd.h>

#include <android/log.h>
#include <jni.h>
#include <sha1.h>

#include "ehviewer.h"

#define LOG_TAG "hash"

#define BUFFER_SIZE 8192

typedef uint8_t byte;

const char hex_digits[] = "0123456789abcdef";

char *bytes_to_hex(const byte *bytes, size_t length) {
    byte byte;
    char *hex = malloc(2 * length + 1);
    for (int i = 0; i < length; i++) {
        byte = bytes[i];
        hex[2 * i] = hex_digits[byte >> 4 & 0xF];
        hex[2 * i + 1] = hex_digits[byte & 0xF];
    }
    hex[2 * length] = '\0';
    return hex;
}

JNIEXPORT jstring JNICALL
Java_com_hippo_ehviewer_jni_HashKt_sha1(JNIEnv *env, jclass clazz, jint fd) {
    EH_UNUSED(clazz);
    byte *buffer = malloc(BUFFER_SIZE);
    if (!buffer) {
        LOGE("%s", strerror(errno));
        return NULL;
    }

    struct sha1_ctx ctx;
    sha1_init(&ctx);

    size_t bytes_read;
    while ((bytes_read = read(fd, buffer, BUFFER_SIZE)) > 0) {
        sha1_update(&ctx, bytes_read, buffer);
    }

    byte digest[SHA1_DIGEST_SIZE];
    sha1_digest(&ctx, SHA1_DIGEST_SIZE, digest);

    char *hex_digest = bytes_to_hex(digest, SHA1_DIGEST_SIZE);
    jstring str = (*env)->NewStringUTF(env, hex_digest);
    free(buffer);
    free(hex_digest);
    return str;
}
