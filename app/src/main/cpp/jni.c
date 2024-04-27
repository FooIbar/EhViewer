#include <jni.h>

int register_archive_methods(JNIEnv *env);

int register_gif_utils_methods(JNIEnv *env);

int register_image_methods(JNIEnv *env);

jint JNI_OnLoad(JavaVM *vm, void *reserved) {
    JNIEnv *env;
    if ((*vm)->GetEnv(vm, (void **) &env, JNI_VERSION_1_6) != JNI_OK) {
        return JNI_ERR;
    }

    int result = register_archive_methods(env);
    if (result != JNI_OK) return result;

    result = register_gif_utils_methods(env);
    if (result != JNI_OK) return result;

    result = register_image_methods(env);
    if (result != JNI_OK) return result;

    return JNI_VERSION_1_6;
}
