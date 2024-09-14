use crate::{jni_throwing, with_bitmap_content};
use anyhow::Ok;
use image::{ImageBuffer, Rgba};
use jni::objects::JClass;
use jni::sys::{jboolean, jobject};
use jni::JNIEnv;
use jni_fn::jni_fn;
use rxing::common::HybridBinarizer;
use rxing::qrcode::detector::Detector;
use rxing::{BinaryBitmap, RGBLuminanceSource};
use std::ptr::slice_from_raw_parts;

fn detect_image_ad(image: ImageBuffer<Rgba<u8>, &[u8]>) -> bool {
    let (width, height) = (image.width() as usize, image.height() as usize);
    let buffer = image.into_raw();
    let pixels = unsafe { &*slice_from_raw_parts(buffer.as_ptr() as *const u32, buffer.len()) };
    let source = RGBLuminanceSource::new_with_width_height_pixels(width, height, pixels);
    let image = BinaryBitmap::new(HybridBinarizer::new(source));
    Detector::new(image.get_black_matrix()).detect().is_ok()
}

#[no_mangle]
#[allow(non_snake_case)]
#[jni_fn("com.hippo.ehviewer.image.ImageKt")]
pub fn hasQrCode(mut env: JNIEnv, _class: JClass, object: jobject) -> jboolean {
    jni_throwing(&mut env, |env| {
        with_bitmap_content(env, object, |image| Ok(detect_image_ad(image) as jboolean))
    })
}
