use std::ops::Deref;

use image::{ImageBuffer, Luma, Pixel, Rgba};
use jni::{
    objects::JClass,
    sys::{jintArray, jobject},
    JNIEnv,
};
use jni_fn::jni_fn;

use crate::{jni_throwing, with_bitmap_content};

/** A line will be considered as having content if 0.25% of it is filled. */
static FILLED_RATIO_LIMIT: f32 = 0.0025;

/** When the threshold is closer to 1, less content will be cropped. **/
static THRESHOLD: f32 = 0.75;

static THRESHOLD_FOR_BLACK: i8 = (255.0 * THRESHOLD) as i8;
static THRESHOLD_FOR_WHITE: i8 = (255.0 - 255.0 * THRESHOLD) as i8;

fn white_black_counter_add(counter: (i32, i32), luma: &Luma<u8>) -> (i32, i32) {
    let grey = luma.0[0] as i8;
    let (black, white) = counter;
    if grey < THRESHOLD_FOR_BLACK {
        (black + 1, white)
    } else if grey > THRESHOLD_FOR_WHITE {
        (black, white + 1)
    } else {
        counter
    }
}

fn detect_border<Container>(image: &ImageBuffer<Rgba<u8>, Container>) -> Option<[i32; 4]>
where
    Container: Deref<Target = [u8]>,
{
    let mut left_iter = image.enumerate_rows();
    let (_, first_row) = left_iter.next()?;
    let (black, white) = first_row.fold((0, 0), |counter, (_, _, pixel)| {
        white_black_counter_add(counter, &pixel.to_luma())
    });
    Some([0, 0, 0, 0])
}

#[no_mangle]
#[allow(non_snake_case)]
#[jni_fn("com.hippo.ehviewer.image.ImageKt")]
pub fn detectBorderRust(mut env: JNIEnv, _class: JClass, object: jobject) -> jintArray {
    jni_throwing(&mut env, |env| {
        with_bitmap_content(env, object, |_| Ok(0 as jobject))
    })
}
