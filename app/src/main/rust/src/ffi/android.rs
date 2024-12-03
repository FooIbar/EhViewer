#![cfg(feature = "android")]

use super::jvm::jni_throwing;
use crate::img::border::detect_border;
use crate::img::core::Image;
use crate::img::qr_code::detect_image_ad;
use anyhow::{Context, Result};
use image::{ImageBuffer, Pixel};
use jni::objects::JClass;
use jni::sys::jboolean;
use jni::sys::{jintArray, jobject};
use jni::JNIEnv;
use jni_fn::jni_fn;
use ndk::bitmap::{Bitmap, BitmapFormat};
use std::ffi::c_void;
use std::ptr::slice_from_raw_parts;

#[no_mangle]
#[allow(non_snake_case)]
#[jni_fn("com.hippo.ehviewer.image.ImageKt")]
pub fn detectBorder(mut env: JNIEnv, _class: JClass, object: jobject) -> jintArray {
    jni_throwing(&mut env, |env| {
        let slice = with_bitmap_content(env, object, |img| {
            let result = match img {
                Image::Rgba8(src) => detect_border(&src),
                Image::Rgb565(src) => detect_border(&src),
                Image::Rgba16F(src) => detect_border(&src),
            };
            Ok(result)
        })?;
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
        with_bitmap_content(env, object, |img| {
            let result = match img {
                Image::Rgba8(src) => detect_image_ad(src),
                Image::Rgb565(src) => detect_image_ad(src),
                Image::Rgba16F(src) => detect_image_ad(src),
            };
            Ok(result as jboolean)
        })
    })
}

fn ptr_as_image<'local, P: Pixel>(
    ptr: *const c_void,
    w: u32,
    h: u32,
    channel: u32,
) -> Option<ImageBuffer<P, &'local [P::Subpixel]>> {
    let size = (w * h * channel) as usize;
    let buffer = unsafe { &*slice_from_raw_parts(ptr as *const P::Subpixel, size) };
    ImageBuffer::from_raw(w, h, buffer)
}

pub fn with_bitmap_content<F, R>(env: &mut JNIEnv, bitmap: jobject, f: F) -> Result<R>
where
    F: FnOnce(Image) -> Result<R>,
{
    // SAFETY: kotlin caller must ensure bitmap is valid.
    let handle = unsafe { Bitmap::from_jni(env.get_raw(), bitmap) };

    let info = handle.info()?;
    let (width, height, format) = (info.width(), info.height(), info.format());
    let ptr = handle.lock_pixels()?;
    let image = match format {
        BitmapFormat::RGBA_8888 => ptr_as_image(ptr, width, height, 4).map(Image::Rgba8),
        BitmapFormat::RGB_565 => ptr_as_image(ptr, width, height, 1).map(Image::Rgb565),
        BitmapFormat::RGBA_F16 => ptr_as_image(ptr, width, height, 1).map(Image::Rgba16F),
        _ => None,
    };
    let result = image.context("Unsupported bitmap format").and_then(f);
    handle.unlock_pixels()?;
    result
}
