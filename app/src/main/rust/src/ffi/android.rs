#![cfg(feature = "android")]

use super::jvm::jni_throwing;
use crate::img::border::detect_border;
use crate::img::qr_code::detect_image_ad;
use anyhow::{Context, Result};
use image::{ImageBuffer, Rgba};
use jni::objects::JClass;
use jni::sys::jboolean;
use jni::sys::{jintArray, jobject};
use jni::JNIEnv;
use jni_fn::jni_fn;
use ndk::bitmap::Bitmap;
use std::ptr::slice_from_raw_parts;

#[no_mangle]
#[allow(non_snake_case)]
#[jni_fn("com.hippo.ehviewer.image.ImageKt")]
pub fn detectBorder(mut env: JNIEnv, _class: JClass, object: jobject) -> jintArray {
    jni_throwing(&mut env, |env| {
        let slice = with_bitmap_content(env, object, |img| Ok(detect_border(&img)))?;
        let array = env.new_int_array(4)?;
        env.set_int_array_region(&array, 0, &slice.context("Image too small!")?)?;
        Ok(array.into_raw())
    })
}

#[no_mangle]
#[allow(non_snake_case)]
#[jni_fn("com.hippo.ehviewer.image.ImageKt")]
pub fn hasQrCode(mut env: JNIEnv, _class: JClass, object: jobject) -> jboolean {
    jni_throwing(&mut env, |env| {
        with_bitmap_content(env, object, |image| Ok(detect_image_ad(image) as jboolean))
    })
}

pub fn with_bitmap_content<F, R>(env: &mut JNIEnv, bitmap: jobject, f: F) -> Result<R>
where
    F: FnOnce(ImageBuffer<Rgba<u8>, &[u8]>) -> Result<R>,
{
    // SAFETY: kotlin caller must ensure bitmap is valid.
    let handle = unsafe { Bitmap::from_jni(env.get_raw(), bitmap) };

    let info = handle.info()?;
    let (width, height) = (info.width(), info.height());
    let ptr = handle.lock_pixels()? as *const u8;

    // SAFETY: maybe unsafe if bitmap buffer not RGBA8888 format.
    let buffer = unsafe { &*slice_from_raw_parts(ptr, (width * height * 4) as usize) };

    let image = ImageBuffer::from_raw(width, height, buffer);
    let result = image.context("Image buffer not RGBA8888!!!").and_then(f);
    handle.unlock_pixels()?;
    result
}
