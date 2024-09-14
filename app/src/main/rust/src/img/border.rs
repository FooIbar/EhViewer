use image::{GenericImageView, Rgba};
use jni::{
    objects::JClass,
    sys::{jintArray, jobject},
    JNIEnv,
};
use jni_fn::jni_fn;

use crate::{jni_throwing, with_bitmap_content};

fn detect_border<D>(image: &D) -> [i32; 4]
where
    D: GenericImageView<Pixel = Rgba<u8>>,
{
    [0, 0, 0, 0]
}

#[no_mangle]
#[allow(non_snake_case)]
#[jni_fn("com.hippo.ehviewer.image.ImageKt")]
pub fn detectBorderRust(mut env: JNIEnv, _class: JClass, object: jobject) -> jintArray {
    jni_throwing(&mut env, |env| {
        with_bitmap_content(env, object, |_| Ok(0 as jobject))
    })
}
