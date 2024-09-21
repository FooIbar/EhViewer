use crate::{jni_throwing, with_bitmap_content};
use image::{ImageBuffer, Rgba};
use jni::objects::JClass;
use jni::sys::{jboolean, jobject};
use jni::JNIEnv;
use jni_fn::jni_fn;
use rxing::common::HybridBinarizer;
use rxing::qrcode::detector::FinderPatternFinder;
use rxing::{BinaryBitmap, DecodingHintDictionary, Point, RGBLuminanceSource};
use std::f32::consts::FRAC_1_SQRT_2;
use std::ptr::slice_from_raw_parts;

fn detect_image_ad(image: ImageBuffer<Rgba<u8>, &[u8]>) -> bool {
    let (width, height) = (image.width() as usize, image.height() as usize);
    let buffer = image.into_raw();
    let pixels = unsafe { &*slice_from_raw_parts(buffer.as_ptr() as *const u32, buffer.len()) };
    let source = RGBLuminanceSource::new_with_width_height_pixels(width, height, pixels);
    let image = BinaryBitmap::new(HybridBinarizer::new(source));
    match FinderPatternFinder::new(image.get_black_matrix()).find(&DecodingHintDictionary::new()) {
        Ok(info) => {
            // Check if the region is squarish
            let top_left = Point::from(info.getTopLeft());
            let top_right = Point::from(info.getTopRight());
            let bottom_left = Point::from(info.getBottomLeft());
            let length = top_right.distance(top_left);
            let epsilon = length * 0.05;
            (bottom_left.distance(top_left) - length).abs() < epsilon
                && (bottom_left.distance(top_right) * FRAC_1_SQRT_2 - length).abs() < epsilon
        }
        Err(_) => false,
    }
}

#[no_mangle]
#[allow(non_snake_case)]
#[jni_fn("com.hippo.ehviewer.image.ImageKt")]
pub fn hasQrCode(mut env: JNIEnv, _class: JClass, object: jobject) -> jboolean {
    jni_throwing(&mut env, |env| {
        with_bitmap_content(env, object, |image| Ok(detect_image_ad(image) as jboolean))
    })
}
