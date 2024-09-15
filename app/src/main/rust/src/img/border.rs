use anyhow::anyhow;
use image::{ImageBuffer, Luma, Pixel, Rgba};
use jni::objects::JClass;
use jni::sys::{jintArray, jobject};
use jni::JNIEnv;
use jni_fn::jni_fn;

use crate::{jni_throwing, with_bitmap_content};

/** A line will be considered as having content if 0.25% of it is filled. */
static FILLED_RATIO_LIMIT: f32 = 0.0025;

/** When the threshold is closer to 1, less content will be cropped. **/
static THRESHOLD: f32 = 0.75;

static THRESHOLD_FOR_BLACK: u8 = (255.0 * THRESHOLD) as u8;
static THRESHOLD_FOR_WHITE: u8 = (255.0 - 255.0 * THRESHOLD) as u8;

fn is_white(pixel: &Luma<u8>) -> bool {
    pixel.0[0] > THRESHOLD_FOR_WHITE
}

fn is_black(pixel: &Luma<u8>) -> bool {
    pixel.0[0] < THRESHOLD_FOR_BLACK
}

fn black_white_counter_add(counter: (i32, i32), luma: &Luma<u8>) -> (i32, i32) {
    let (black, white) = counter;
    if is_black(luma) {
        (black + 1, white)
    } else if is_white(luma) {
        (black, white + 1)
    } else {
        counter
    }
}

fn line_not_filled_by<'pixel>(
    line: impl Iterator<Item = &'pixel Rgba<u8>>,
    white: bool,
    limit: i32,
) -> bool {
    let f = if white { is_black } else { is_white };
    line.step_by(2).filter(|p| f(&p.to_luma())).count() as i32 > limit
}

fn try_count_lines<'pixel>(
    iter: impl Iterator<Item = impl Iterator<Item = &'pixel Rgba<u8>>>,
    white: bool,
    limit: i32,
) -> i32 {
    let mut count = 1;
    for line in iter {
        if line_not_filled_by(line, white, limit) {
            break;
        } else {
            count += 1;
        }
    }
    count
}

fn detect_border_lines<'pixel>(
    mut iter: impl Iterator<Item = impl Iterator<Item = &'pixel Rgba<u8>>>,
    line_len: i32,
) -> i32 {
    let first_row = iter.next().expect("Image is empty!");
    let filled_limit = (line_len as f32 * FILLED_RATIO_LIMIT / 2.0).round() as i32;
    let (black, white) = first_row.step_by(2).fold((0, 0), |counter, pixel| {
        black_white_counter_add(counter, &pixel.to_luma())
    });
    match (black > filled_limit, white > filled_limit) {
        // Black is dominant color.
        (true, false) => try_count_lines(iter, false, filled_limit),
        // White is dominant color.
        (false, true) => try_count_lines(iter, true, filled_limit),
        _ => 0,
    }
}

struct ColumnView<'a> {
    buffer: &'a ImageBuffer<Rgba<u8>, &'a [u8]>,
    start: i32,
    end: i32,
}

struct OneRow<'a> {
    buffer: &'a ImageBuffer<Rgba<u8>, &'a [u8]>,
    row: i32,
    column: i32,
}

impl<'a> Iterator for OneRow<'a> {
    type Item = &'a Rgba<u8>;

    fn next(&mut self) -> Option<Self::Item> {
        let item = self
            .buffer
            .get_pixel_checked(self.row as u32, self.column as u32);
        self.column += 1;
        item
    }
}

impl<'a> Iterator for ColumnView<'a> {
    type Item = OneRow<'a>;

    fn next(&mut self) -> Option<Self::Item> {
        let (start, end) = (self.start, self.end);
        if start == end {
            None
        } else {
            let row = OneRow {
                buffer: self.buffer,
                row: start,
                column: 0,
            };
            self.start = start + 1;
            Some(row)
        }
    }
}

impl<'a> DoubleEndedIterator for ColumnView<'a> {
    fn next_back(&mut self) -> Option<Self::Item> {
        let (start, end) = (self.start, self.end);
        if start == end {
            None
        } else {
            let row = OneRow {
                buffer: self.buffer,
                row: end,
                column: 0,
            };
            self.end = end - 1;
            Some(row)
        }
    }
}

// left, top, right, bottom
fn detect_border(image: &ImageBuffer<Rgba<u8>, &[u8]>) -> Option<[i32; 4]> {
    let (w, h) = image.dimensions();
    let top = detect_border_lines(image.rows(), w as i32);
    let bottom = detect_border_lines(image.rows().rev(), w as i32);

    let column = ColumnView {
        buffer: image,
        start: 0,
        end: w as i32 - 1,
    };
    let left = detect_border_lines(column, h as i32);
    let column = ColumnView {
        buffer: image,
        start: 0,
        end: w as i32 - 1,
    };
    let right = detect_border_lines(column.rev(), h as i32);
    Some([left, top, w as i32 - right, h as i32 - bottom])
}

#[no_mangle]
#[allow(non_snake_case)]
#[jni_fn("com.hippo.ehviewer.image.ImageKt")]
pub fn detectBorderRust(mut env: JNIEnv, _class: JClass, object: jobject) -> jintArray {
    jni_throwing(&mut env, |env| {
        let slice = with_bitmap_content(env, object, |img| Ok(detect_border(img)))?;
        let array = env.new_int_array(4)?;
        env.set_int_array_region(&array, 0, &slice.ok_or(anyhow!("Image too small!"))?)?;
        Ok(array.into_raw())
    })
}
