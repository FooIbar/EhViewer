#![cfg(feature = "android-26")]

use super::android::with_bitmap_content;
use super::jvm::jni_throwing;
use crate::img::copy_region::copy_region;
use crate::img::core::Image;
use anyhow::Context;
use image::{ImageBuffer, Pixel};
use jni::objects::JClass;
use jni::sys::{jint, jobject};
use jni::JNIEnv;
use jni_fn::jni_fn;
use ndk::hardware_buffer::{HardwareBuffer, HardwareBufferUsage};
use std::ffi::c_void;
use std::ptr::slice_from_raw_parts_mut;

fn ptr_as_image_mut<'local, P: Pixel>(
    ptr: *mut c_void,
    w: u32,
    h: u32,
    channel: u32,
) -> Option<ImageBuffer<P, &'local mut [P::Subpixel]>> {
    let size = (w * h * channel) as usize;
    let buffer = unsafe { &mut *slice_from_raw_parts_mut(ptr as *mut P::Subpixel, size) };
    ImageBuffer::from_raw(w, h, buffer)
}

#[no_mangle]
#[allow(non_snake_case)]
#[jni_fn("com.hippo.ehviewer.image.ImageKt")]
pub fn copyBitmapToAHB(mut env: JNIEnv, _: JClass, bm: jobject, ahb: jobject, x: jint, y: jint) {
    let ahb = unsafe { HardwareBuffer::from_jni(env.get_raw(), ahb) };
    jni_throwing(&mut env, |env| {
        with_bitmap_content(env, bm, |img| {
            let desc = ahb.describe();
            let (w, h, stride) = (desc.width, desc.height, desc.stride);
            let ptr = ahb.lock(HardwareBufferUsage::CPU_WRITE_RARELY, None, None)?;
            let result = match img {
                Image::Rgba8(src) => {
                    let dst = ptr_as_image_mut(ptr, stride, h, 4).context("Illegal AHB format!");
                    dst.map(|dst| copy_region(src, dst, x as u32, y as u32, w, h))
                }
                Image::Rgb565(src) => {
                    let dst = ptr_as_image_mut(ptr, stride, h, 1).context("Illegal AHB format!");
                    dst.map(|dst| copy_region(src, dst, x as u32, y as u32, w, h))
                }
                Image::Rgba16F(src) => {
                    let dst = ptr_as_image_mut(ptr, stride, h, 1).context("Illegal AHB format!");
                    dst.map(|dst| copy_region(src, dst, x as u32, y as u32, w, h))
                }
            };
            ahb.unlock()?;
            result??;
            Ok(())
        })
    })
}
