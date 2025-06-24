#![cfg(feature = "android")]

use super::jvm::jni_throwing;
use crate::img::border::DetectBorder;
use crate::img::core::{CustomPixel, ImageConsumer, Rgb565, Rgba8888, RgbaF16};
use crate::img::qr_code::QrCode;
use crate::img::webp::{decode_next_frame, get_image_info};
use anyhow::{Ok, Result, anyhow};
use image::ImageBuffer;
use jni::JNIEnv;
use jni::objects::JClass;
use jni::sys::{jboolean, jint, jintArray, jlong, jobject};
use jni_fn::jni_fn;
use libwebp_sys::WebPAnimDecoder;
use ndk::bitmap::{Bitmap, BitmapFormat};
use std::ptr::{copy_nonoverlapping, slice_from_raw_parts};

#[jni_fn("com.hippo.ehviewer.image.ImageKt")]
pub fn detectBorder(mut env: JNIEnv, _class: JClass, object: jobject) -> jintArray {
    jni_throwing(&mut env, |env| {
        let slice = use_bitmap_content(env, object, DetectBorder)?;
        let array = env.new_int_array(4)?;
        env.set_int_array_region(&array, 0, &slice)?;
        Ok(array.into_raw())
    })
}

#[jni_fn("com.hippo.ehviewer.image.ImageKt")]
pub fn hasQrCode(mut env: JNIEnv, _class: JClass, object: jobject) -> jboolean {
    jni_throwing(&mut env, |env| {
        Ok(use_bitmap_content(env, object, QrCode)? as jboolean)
    })
}

#[jni_fn("com.hippo.ehviewer.coil.AnimatedWebPDrawableKt")]
pub fn nativeDecodeNextFrame(
    mut env: JNIEnv,
    _: JClass,
    decoder: jlong,
    reset: jboolean,
    bitmap: jobject,
) -> jint {
    jni_throwing(&mut env, |env| {
        let dec = decoder as *mut WebPAnimDecoder;
        let (buf, timestamp) = unsafe { decode_next_frame(dec, reset != 0) };
        if !buf.is_null() {
            let info = unsafe { get_image_info(dec) };
            let handle = unsafe { Bitmap::from_jni(env.get_raw(), bitmap) };
            let dst = handle.lock_pixels()? as *mut u8;
            let size = info.canvas_width * info.canvas_height * 4;
            unsafe { copy_nonoverlapping(buf, dst, size as usize) };
            handle.unlock_pixels()?;
        }
        Ok(timestamp)
    })
}

fn ptr_as_image<'local, P: CustomPixel>(
    ptr: *const !,
    w: u32,
    h: u32,
) -> ImageBuffer<P, &'local [P::Subpixel]> {
    let size = (w * h * P::CHANNEL_COUNT as u32) as usize;
    let buffer = unsafe { &*slice_from_raw_parts(ptr as *const P::Subpixel, size) };
    ImageBuffer::from_raw(w, h, buffer).unwrap()
}

pub fn use_bitmap_content<R, F: ImageConsumer<R>>(
    env: &mut JNIEnv,
    bitmap: jobject,
    f: F,
) -> Result<R> {
    // SAFETY: kotlin caller must ensure bitmap is valid.
    let handle = unsafe { Bitmap::from_jni(env.get_raw(), bitmap) };

    let info = handle.info()?;
    let (w, h, format) = (info.width(), info.height(), info.format());
    let p = handle.lock_pixels()? as *const !;
    let result = match format {
        BitmapFormat::RGBA_8888 => f.apply(&ptr_as_image::<Rgba8888>(p, w, h)),
        BitmapFormat::RGB_565 => f.apply(&ptr_as_image::<Rgb565>(p, w, h)),
        BitmapFormat::RGBA_F16 => f.apply(&ptr_as_image::<RgbaF16>(p, w, h)),
        _ => Err(anyhow!("Unsupported bitmap format")),
    };
    handle.unlock_pixels()?;
    result
}
