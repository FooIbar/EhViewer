use crate::{jni_throwing, with_bitmap_content};
use anyhow::{Ok, Result};
use bardecoder::decode::Decode;
use bardecoder::default_builder;
use bardecoder::extract::QRExtractor;
use bardecoder::util::qr::{QRData, QRError};
use image::{GenericImageView, Rgba};
use jni::objects::JClass;
use jni::sys::{jboolean, jobject};
use jni::JNIEnv;
use jni_fn::jni_fn;

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
    jni_throwing(&mut env, |env| {
        with_bitmap_content(env, object, |image| Ok(detect_image_ad(image) as jboolean))
    })
}
