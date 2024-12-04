#![cfg(feature = "android-26")]

use super::android::use_bitmap_content;
use super::jvm::jni_throwing;
use crate::img::copy_region::CopyRegion;
use jni::objects::JClass;
use jni::sys::{jint, jobject};
use jni::JNIEnv;
use jni_fn::jni_fn;
use ndk::hardware_buffer::{HardwareBuffer, HardwareBufferUsage};

#[no_mangle]
#[allow(non_snake_case)]
#[jni_fn("com.hippo.ehviewer.image.ImageKt")]
pub fn copyBitmapToAHB(mut env: JNIEnv, _: JClass, bm: jobject, ahb: jobject, x: jint, y: jint) {
    let ahb = unsafe { HardwareBuffer::from_jni(env.get_raw(), ahb) };
    jni_throwing(&mut env, |env| {
        let desc = ahb.describe();
        let (w, h, stride) = (desc.width, desc.height, desc.stride);
        let ptr = ahb.lock(HardwareBufferUsage::CPU_WRITE_RARELY, None, None)?;
        let s = CopyRegion {
            ptr: ptr as *mut !,
            target_dim: (stride, h),
            src_rect: (x as u32, y as u32, w, h),
        };
        let result = use_bitmap_content(env, bm, s);
        ahb.unlock()?;
        result
    })
}
