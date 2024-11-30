#![cfg(feature = "android-26")]

use super::android::with_bitmap_content;
use super::jvm::jni_throwing;
use crate::img::copy_region::copy_region;
use anyhow::Context;
use image::ImageBuffer;
use jni::objects::JClass;
use jni::sys::{jint, jobject};
use jni::JNIEnv;
use jni_fn::jni_fn;
use ndk::hardware_buffer::{HardwareBuffer, HardwareBufferUsage};
use std::ptr::slice_from_raw_parts_mut;

#[no_mangle]
#[allow(non_snake_case)]
#[jni_fn("com.hippo.ehviewer.image.ImageKt")]
pub fn copyBitmapToAHB(mut env: JNIEnv, _: JClass, bm: jobject, ahb: jobject, x: jint, y: jint) {
    let ahb = unsafe { HardwareBuffer::from_jni(env.get_raw(), ahb) };
    jni_throwing(&mut env, |env| {
        with_bitmap_content(env, bm, |src| {
            let desc = ahb.describe();
            let (w, h, stride) = (desc.width, desc.height, desc.stride);
            let ptr = ahb.lock(HardwareBufferUsage::CPU_WRITE_RARELY, None, None)? as *mut u8;
            let s = unsafe { &mut *slice_from_raw_parts_mut(ptr, (stride * h * 4) as usize) };
            let dst = ImageBuffer::from_raw(stride, h, s).context("Illegal AHB format!");
            let result = dst.map(|dst| copy_region(src, dst, x as u32, y as u32, w, h));
            ahb.unlock()?;
            result??;
            Ok(())
        })
    })
}
