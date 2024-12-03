#![cfg(feature = "android")]

use super::jvm::jni_throwing;
use crate::img::border::DetectBorder;
use crate::img::core::ImageConsumer;
use crate::img::qr_code::QrCode;
use anyhow::{anyhow, Context, Ok, Result};
use image::{ImageBuffer, Luma, Pixel, Rgba};
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
        let slice: [i32; 4] = with_bitmap_content(env, object, DetectBorder)?;
        let array = env.new_int_array(4)?;
        env.set_int_array_region(&array, 0, &slice)?;
        Ok(array.into_raw())
    })
}

#[no_mangle]
#[allow(non_snake_case)]
#[jni_fn("com.hippo.ehviewer.image.ImageKt")]
pub fn hasQrCode(mut env: JNIEnv, _class: JClass, object: jobject) -> jboolean {
    jni_throwing(&mut env, |env| {
        Ok(with_bitmap_content(env, object, QrCode)? as jboolean)
    })
}

fn ptr_as_image<'local, P: Pixel>(
    ptr: *const c_void,
    w: u32,
    h: u32,
    channel: u32,
) -> Result<ImageBuffer<P, &'local [P::Subpixel]>> {
    let size = (w * h * channel) as usize;
    let buffer = unsafe { &*slice_from_raw_parts(ptr as *const P::Subpixel, size) };
    ImageBuffer::from_raw(w, h, buffer).context("Unreachable!!!")
}

pub fn with_bitmap_content<R, F: ImageConsumer<R>>(
    env: &mut JNIEnv,
    bitmap: jobject,
    f: F,
) -> Result<R> {
    // SAFETY: kotlin caller must ensure bitmap is valid.
    let handle = unsafe { Bitmap::from_jni(env.get_raw(), bitmap) };

    let info = handle.info()?;
    let (width, height, format) = (info.width(), info.height(), info.format());
    let ptr = handle.lock_pixels()?;
    let result = match format {
        BitmapFormat::RGBA_8888 => {
            ptr_as_image::<Rgba<u8>>(ptr, width, height, 4).and_then(|p| f.apply(&p))
        }
        BitmapFormat::RGB_565 => {
            ptr_as_image::<Luma<u16>>(ptr, width, height, 1).and_then(|p| f.apply(&p))
        }
        BitmapFormat::RGBA_F16 => {
            ptr_as_image::<Luma<u64>>(ptr, width, height, 1).and_then(|p| f.apply(&p))
        }
        _ => Err(anyhow!("Unsupported bitmap format")),
    };
    handle.unlock_pixels()?;
    result
}
