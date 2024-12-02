/*
 * Copyright 2022-2024 Tarsin Norbin
 *
 * This file is part of EhViewer
 *
 * EhViewer is free software: you can redistribute it and/or
 * modify it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * EhViewer is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along with
 * EhViewer. If not, see <https://www.gnu.org/licenses/>.
 */

#include <stdbool.h>
#include <stdlib.h>
#include <string.h>
#include <errno.h>
#include <pthread.h>
#include <sys/mman.h>

#include <jni.h>
#include <android/log.h>

#include <archive.h>
#include <archive_entry.h>

#define LOG_TAG "libarchive_wrapper"

#include "natsort/strnatcmp.h"
#include "ehviewer.h"

typedef struct {
    int using;
    int next_index;
    struct archive *arc;
    struct archive_entry *entry;
} archive_ctx;

typedef struct {
    const char *filename;
    int index;
    ssize_t size;
    void *addr;
} entry;

#define CTX_POOL_SIZE 20
#define MAX_PARALLEL_DECOMP 4
#define max(a, b) ((a) > (b) ? (a) : (b))

static pthread_mutex_t ctx_pool_mutex = PTHREAD_MUTEX_INITIALIZER;
static archive_ctx **ctx_pool = NULL;
static pthread_mutex_t buffer_mutex = PTHREAD_MUTEX_INITIALIZER;
static void *decode_buffer[MAX_PARALLEL_DECOMP];
static bool need_encrypt = false;
static char *passwd = NULL;
static void *archiveAddr = MAP_FAILED;
static size_t archiveSize = 0;
static entry *entries = NULL;
static size_t entryCount = 0;
static ssize_t max_file_size = 0;

#define SUPPORT_EXT_COUNT 11

const char supportExt[SUPPORT_EXT_COUNT][5] = {
        "jpeg",
        "jpg",
        "png",
        "gif",
        "webp",
        "bmp",
        "ico",
        "wbmp",
        "heic",
        "heif",
        "avif"
};

static inline int filename_is_playable_file(const char *name) {
    if (!name)
        return false;
    const char *dotptr = strrchr(name, '.');
    if (!dotptr++)
        return false;
    int i;
    for (i = 0; i < SUPPORT_EXT_COUNT; i++)
        if (strcmp(dotptr, supportExt[i]) == 0)
            return true;
    return false;
}

static inline bool archive_entry_is_file(struct archive_entry *entry) {
    return archive_entry_filetype(entry) == AE_IFREG;
}

static inline bool archive_entry_is_playable(struct archive_entry *entry) {
    return archive_entry_is_file(entry) &&
           filename_is_playable_file(archive_entry_pathname(entry));
}

static inline int compare_entries(const void *a, const void *b) {
    const char *fa = ((entry *) a)->filename;
    const char *fb = ((entry *) b)->filename;
    return strnatcmp(fa, fb);
}

#define ADDR_IN_FILE_MAPPING(addr) (addr >= archiveAddr && addr < archiveAddr + archiveSize)

static bool fill_entry_zero_copy(struct archive *arc, entry *entry) {
    void *buffer = NULL;
    size_t buffer_size = 0;
    la_int64_t output_ofs = 0;
    archive_read_data_block(arc, (const void **) &buffer, &buffer_size, &output_ofs);
    bool zero_copy = ADDR_IN_FILE_MAPPING(buffer) && !output_ofs && buffer_size == entry->size;
    entry->addr = zero_copy ? buffer : NULL;
    return zero_copy;
}

static void archive_map_entries_index(archive_ctx *ctx, bool sort) {
    int count = 0;
    bool zero_copy = true;
    while (archive_read_next_header(ctx->arc, &ctx->entry) == ARCHIVE_OK) {
        const char *name = archive_entry_pathname(ctx->entry);
        if (archive_entry_is_file(ctx->entry) && filename_is_playable_file(name)) {
            entries[count].filename = strdup(name);
            entries[count].index = count;
            ssize_t size = archive_entry_size(ctx->entry);
            max_file_size = max(size, max_file_size);
            entries[count].size = size;
            // We don't expect zero copy if first content can't do zero copy
            if (zero_copy) zero_copy = fill_entry_zero_copy(ctx->arc, &entries[count]);
            count++;
        }
    }
    if (sort) qsort(entries, entryCount, sizeof(entry), compare_entries);
}

static void *acquire_decode_buffer() {
    void *addr = NULL;
    pthread_mutex_lock(&buffer_mutex);
    for (int i = 0; i < MAX_PARALLEL_DECOMP; ++i) {
        addr = decode_buffer[i];
        if (addr) {
            decode_buffer[i] = NULL;
            break;
        }
    }
    pthread_mutex_unlock(&buffer_mutex);
    if (!addr) addr = malloc(max_file_size);
    return addr;
}

static void release_decode_buffer(void *buffer) {
    pthread_mutex_lock(&buffer_mutex);
    for (int i = 0; i < MAX_PARALLEL_DECOMP; ++i) {
        void *addr = decode_buffer[i];
        if (!addr) {
            decode_buffer[i] = buffer;
            pthread_mutex_unlock(&buffer_mutex);
            return;
        }
    }
    pthread_mutex_unlock(&buffer_mutex);
    free(buffer);
}

static int archive_list_all_entries(archive_ctx *ctx) {
    int count = 0;
    while (archive_read_next_header(ctx->arc, &ctx->entry) == ARCHIVE_OK)
        if (archive_entry_is_playable(ctx->entry))
            count++;
    return count;
}

static void archive_release_ctx(archive_ctx *ctx) {
    if (ctx) {
        archive_read_close(ctx->arc);
        archive_read_free(ctx->arc);
        free(ctx);
    }
}

static archive_ctx *archive_alloc_ctx() {
    archive_ctx *ctx = calloc(1, sizeof(archive_ctx));
    ctx->arc = archive_read_new();
    ctx->using = 1;
    archive_read_support_format_tar(ctx->arc);
    archive_read_support_format_7zip(ctx->arc);
    archive_read_support_format_rar5(ctx->arc);
    archive_read_support_format_zip(ctx->arc);
    archive_read_support_filter_gzip(ctx->arc);
    archive_read_support_filter_xz(ctx->arc);
    archive_read_set_option(ctx->arc, "zip", "ignorecrc32", "1");
    if (passwd)
        archive_read_add_passphrase(ctx->arc, passwd);
    int err = archive_read_open_memory(ctx->arc, archiveAddr, archiveSize);
    if (err < ARCHIVE_OK) {
        LOGE("%s%s", "Open archive failed: ", archive_error_string(ctx->arc));
        archive_read_free(ctx->arc);
        free(ctx);
        return NULL;
    }
    return ctx;
}

static int archive_skip_to_index(archive_ctx *ctx, int index) {
    while (archive_read_next_header(ctx->arc, &ctx->entry) == ARCHIVE_OK) {
        if (!archive_entry_is_playable(ctx->entry))
            continue;
        if (ctx->next_index++ == index) {
            return ctx->next_index - 1;
        }
    }
    return ARCHIVE_FATAL;
}

static int archive_get_ctx(archive_ctx **ctxptr, int idx) {
    int ret;
    archive_ctx *ctx = NULL;
    pthread_mutex_lock(&ctx_pool_mutex);
    for (int i = 0; i < CTX_POOL_SIZE; i++) {
        if (!ctx_pool[i])
            continue;
        if (ctx_pool[i]->using)
            continue;
        if (ctx_pool[i]->next_index > idx)
            continue;
        if (!ctx || ctx_pool[i]->next_index > ctx->next_index)
            ctx = ctx_pool[i];
        if (ctx->next_index == idx)
            break;
    }
    if (ctx)
        ctx->using = 1;
    pthread_mutex_unlock(&ctx_pool_mutex);

    if (!ctx) {
        archive_ctx *victimCtx = NULL;
        int victimIdx = 0;
        int replace = 1;
        ctx = archive_alloc_ctx();
        pthread_mutex_lock(&ctx_pool_mutex);
        for (int i = 0; i < CTX_POOL_SIZE; i++) {
            if (!ctx_pool[i]) {
                ctx_pool[i] = ctx;
                replace = 0;
                break;
            }
            if (ctx_pool[i]->using)
                continue;
            if (!victimCtx || ctx_pool[i]->next_index > victimCtx->next_index) {
                victimCtx = ctx_pool[i];
                victimIdx = i;
            }
        }
        if (replace) ctx_pool[victimIdx] = ctx;
        pthread_mutex_unlock(&ctx_pool_mutex);
        if (replace) archive_release_ctx(victimCtx);
    }
    ret = archive_skip_to_index(ctx, idx);
    if (ret != idx) {
        ret = archive_errno(ctx->arc);
        LOGE("Skip to index failed: %s", archive_error_string(ctx->arc));
        archive_release_ctx(ctx);
        return ret;
    }
    *ctxptr = ctx;
    return 0;
}

JNIEXPORT jint JNICALL
Java_com_hippo_ehviewer_jni_ArchiveKt_openArchive(JNIEnv *env, jclass thiz, jint fd, jlong size, jboolean sort_entries) {
    EH_UNUSED(env);
    EH_UNUSED(thiz);
    archive_ctx *ctx = NULL;
    archiveAddr = mmap(0, size, PROT_READ, MAP_PRIVATE, fd, 0);
    if (archiveAddr == MAP_FAILED) {
        LOGE("%s%s", "mmap failed with error ", strerror(errno));
        return 0;
    }
    archiveSize = size;
    ctx_pool = calloc(CTX_POOL_SIZE, sizeof(archive_ctx **));
    ctx = archive_alloc_ctx();
    if (!ctx) return 0;

    entryCount = archive_list_all_entries(ctx);
    LOGI("%s%zu%s", "Found ", entryCount, " images in archive");
    if (!entryCount) {
        LOGE("%s%s", "Archive read failed: ", archive_error_string(ctx->arc));
        archive_release_ctx(ctx);
        return 0;
    }

    // We must read through the file|vm then we can know whether it is encrypted
    int encryptRet = archive_read_has_encrypted_entries(ctx->arc);
    switch (encryptRet) {
        case 1: // At lease 1 encrypted entry
            need_encrypt = true;
            break;
        case 0: // format supports but no encrypted entry found
        default:
            need_encrypt = false;
    }

    int format = archive_format(ctx->arc);
    switch (format) {
        case ARCHIVE_FORMAT_ZIP:
        case ARCHIVE_FORMAT_RAR_V5:
            madvise_log_if_error(archiveAddr, archiveSize, MADV_SEQUENTIAL);
            break;
        case ARCHIVE_FORMAT_7ZIP: // Seek is bad
            madvise_log_if_error(archiveAddr, archiveSize, MADV_RANDOM);
            break;
        default:;
    }
    archive_release_ctx(ctx);

    ctx = archive_alloc_ctx();
    if (!ctx) return 0;
    entries = calloc(entryCount, sizeof(entry));
    archive_map_entries_index(ctx, sort_entries);
    archive_release_ctx(ctx);
    return (int) entryCount;
}

JNIEXPORT jobject JNICALL
Java_com_hippo_ehviewer_jni_ArchiveKt_extractToByteBuffer(JNIEnv *env, jclass thiz, jint index) {
    EH_UNUSED(env);
    EH_UNUSED(thiz);
    entry *entry = &entries[index];
    ssize_t size = entry->size;
    if (entry->addr) {
        return (*env)->NewDirectByteBuffer(env, entry->addr, size);
    } else {
        archive_ctx *ctx = NULL;
        if (!archive_get_ctx(&ctx, entry->index)) {
            void *addr = acquire_decode_buffer();
            ssize_t bytes = archive_read_data(ctx->arc, addr, size);
            ctx->using = 0;
            if (bytes == size) {
                return (*env)->NewDirectByteBuffer(env, addr, size);
            } else {
                if (bytes < 0) {
                    LOGE("%s%s", "Archive read failed: ", archive_error_string(ctx->arc));
                } else {
                    LOGE("%s", "No enough data read, WTF?");
                }
            }
            release_decode_buffer(addr);
        }
    }
    return 0;
}

JNIEXPORT void JNICALL
Java_com_hippo_ehviewer_jni_ArchiveKt_closeArchive(JNIEnv *env, jclass thiz) {
    EH_UNUSED(env);
    EH_UNUSED(thiz);
    if (ctx_pool) {
        for (int i = 0; i < CTX_POOL_SIZE; i++)
            archive_release_ctx(ctx_pool[i]);
        free(ctx_pool);
        ctx_pool = NULL;
    }
    free(passwd);
    passwd = NULL;
    need_encrypt = false;
    if (archiveAddr != MAP_FAILED) {
        munmap(archiveAddr, archiveSize);
        archiveAddr = MAP_FAILED;
    }
    for (int i = 0; i < MAX_PARALLEL_DECOMP; ++i) {
        free(decode_buffer[i]);
        decode_buffer[i] = NULL;
    }
    max_file_size = 0;
    if (entries) {
        for (int i = 0; i < entryCount; ++i) {
            free((void *) entries[i].filename);
        }
        free(entries);
        entries = NULL;
    }
}

JNIEXPORT jboolean JNICALL
Java_com_hippo_ehviewer_jni_ArchiveKt_needPassword(JNIEnv *env, jclass thiz) {
    EH_UNUSED(env);
    EH_UNUSED(thiz);
    return need_encrypt;
}

JNIEXPORT jboolean JNICALL
Java_com_hippo_ehviewer_jni_ArchiveKt_providePassword(JNIEnv *env, jclass thiz, jstring str) {
    EH_UNUSED(thiz);
    struct archive_entry *entry;
    archive_ctx *ctx;
    jboolean ret = true;
    int len = (*env)->GetStringUTFLength(env, str);
    passwd = realloc(passwd, len + 1);
    (*env)->GetStringUTFRegion(env, str, 0, len, passwd);
    passwd[len] = 0;
    ctx = archive_alloc_ctx();
    char tmpBuf[4096];
    while (archive_read_next_header(ctx->arc, &entry) == ARCHIVE_OK) {
        if (!archive_entry_is_playable(entry))
            continue;
        if (!archive_entry_is_encrypted(entry))
            continue;
        if (archive_read_data(ctx->arc, tmpBuf, 4096) < ARCHIVE_OK) {
            LOGE("%s%s", "Archive read failed: ", archive_error_string(ctx->arc));
            ret = false;
        }
        break;
    }
    archive_release_ctx(ctx);
    return ret;
}

JNIEXPORT jstring JNICALL
Java_com_hippo_ehviewer_jni_ArchiveKt_getExtension(JNIEnv *env, jclass thiz, jint index) {
    EH_UNUSED(env);
    EH_UNUSED(thiz);
    const char *ext = strrchr(entries[index].filename, '.') + 1;
    return (*env)->NewStringUTF(env, ext);
}

JNIEXPORT jboolean JNICALL
Java_com_hippo_ehviewer_jni_ArchiveKt_extractToFd(JNIEnv *env, jclass thiz, jint index, jint fd) {
    EH_UNUSED(env);
    EH_UNUSED(thiz);
    index = entries[index].index;
    archive_ctx *ctx = NULL;
    int ret;
    ret = archive_get_ctx(&ctx, index);
    if (!ret) {
        ret = archive_read_data_into_fd(ctx->arc, fd);
        ctx->using = 0;
    }
    return ret == ARCHIVE_OK;
}

JNIEXPORT void JNICALL
Java_com_hippo_ehviewer_jni_ArchiveKt_releaseByteBuffer(JNIEnv *env, jclass thiz, jobject buffer) {
    EH_UNUSED(thiz);
    void *addr = (*env)->GetDirectBufferAddress(env, buffer);
    if (!ADDR_IN_FILE_MAPPING(addr)) {
        release_decode_buffer(addr);
    }
}

JNIEXPORT void JNICALL
Java_com_hippo_ehviewer_jni_ArchiveKt_archiveFdBatch(JNIEnv *env, jclass clazz, jintArray fd_batch, jobjectArray names, jint arc_fd, jint size) {
    EH_UNUSED(clazz);
    struct archive *arc = archive_write_new();
    struct stat st;
    char buff[8192];
    jint fdBatch[size];
    (*env)->GetIntArrayRegion(env, fd_batch, 0, size, fdBatch);
    archive_write_set_format_zip(arc);
    archive_write_zip_set_compression_store(arc);
    archive_write_open_fd(arc, arc_fd);
    struct archive_entry *entry = archive_entry_new();
    for (int i = 0; i < size; i++) {
        int fd = fdBatch[i];
        jobject name = (*env)->GetObjectArrayElement(env, names, i);
        const char *cname = (*env)->GetStringUTFChars(env, name, false);
        archive_entry_set_pathname(entry, cname);
        (*env)->ReleaseStringUTFChars(env, name, cname);
        fstat(fd, &st);
        archive_entry_copy_stat(entry, &st);
        archive_entry_set_perm(entry, 0644);
        archive_write_header(arc, entry);
        size_t len;
        do {
            len = read(fd, buff, sizeof(buff));
            archive_write_data(arc, buff, len);
        } while (len > 0);
        archive_write_finish_entry(arc);
        archive_entry_clear(entry);
    }
    archive_entry_free(entry);
    archive_write_close(arc);
    archive_write_free(arc);
}
