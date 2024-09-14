use crate::{get_bitmap_handle, jni_throwing};
use anyhow::{anyhow, Result};
use bardecoder::decode::Decode;
use bardecoder::default_builder;
use bardecoder::extract::QRExtractor;
use bardecoder::util::qr::{QRData, QRError};
use image::{GenericImageView, ImageBuffer, Rgba};
use jni::objects::JClass;
use jni::sys::{jboolean, jobject};
use jni::JNIEnv;
use jni_fn::jni_fn;
use std::ptr::slice_from_raw_parts;

#[allow(dead_code)]
struct Nothing {}

impl Decode<QRData, String, QRError> for Nothing {
    fn decode(&self, result: Result<QRData, QRError>) -> Result<String, QRError> {
        result.map(|_| String::new())
    }
}

fn detect_image_ad<D>(image: &D) -> bool
where
    D: GenericImageView<Pixel = Rgba<u8>>,
{
    let mut decoder = default_builder();
    decoder.qr(Box::new(QRExtractor {}), Box::new(Nothing {}));
    decoder.build().decode(image).iter().any(|i| i.is_ok())
}

#[no_mangle]
#[allow(non_snake_case)]
#[jni_fn("com.hippo.ehviewer.image.ImageKt")]
pub fn hasQrCode(mut env: JNIEnv, _class: JClass, object: jobject) -> jboolean {
    let handle = get_bitmap_handle(&mut env, object);
    jni_throwing(&mut env, |_| {
        let bitmap_info = handle.info()?;
        let (width, height) = (bitmap_info.width(), bitmap_info.height());
        let ptr = handle.lock_pixels()? as *const u8;
        let buffer = unsafe { &*slice_from_raw_parts(ptr, (width * height * 4) as usize) };
        let image = ImageBuffer::from_raw(width, height, buffer);
        let result = image.map(|img| detect_image_ad(&img));
        handle.unlock_pixels()?;
        Ok(result.ok_or(anyhow!("Internal Error!"))? as jboolean)
    })
}
