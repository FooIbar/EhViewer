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
    line_len: u32,
) -> i32 {
    let first_row = iter.next().expect("Image is empty!");
    let filled_limit = (line_len as f32 * FILLED_RATIO_LIMIT / 2.0).round() as i32;
    let (mut black, mut white) = (0, 0);
    for pixel in first_row.step_by(2) {
        let luma = &pixel.to_luma();
        if is_black(luma) {
            black += 1;
        } else if is_white(luma) {
            white += 1;
        }
    }
    match (black > filled_limit, white > filled_limit) {
        // Black is dominant color.
        (true, false) => try_count_lines(iter, false, filled_limit),
        // White is dominant color.
        (false, true) => try_count_lines(iter, true, filled_limit),
        _ => 0,
    }
}

#[derive(Clone)]
struct ColumnView<'a> {
    buffer: &'a ImageBuffer<Rgba<u8>, &'a [u8]>,
    start: u32,
    end: u32,
}

impl<'a> ColumnView<'a> {
    fn new(buffer: &'a ImageBuffer<Rgba<u8>, &'a [u8]>) -> Self {
        ColumnView {
            buffer,
            start: 0,
            end: buffer.width() - 1,
        }
    }
}

struct OneRow<'a> {
    buffer: &'a ImageBuffer<Rgba<u8>, &'a [u8]>,
    row: u32,
    column: u32,
}

impl<'a> OneRow<'a> {
    fn new(buffer: &'a ImageBuffer<Rgba<u8>, &'a [u8]>, row: u32) -> Self {
        OneRow {
            buffer,
            row,
            column: 0,
        }
    }
}

impl<'a> Iterator for OneRow<'a> {
    type Item = &'a Rgba<u8>;

    fn next(&mut self) -> Option<Self::Item> {
        let column = self.column;
        self.column += 1;
        self.buffer.get_pixel_checked(self.row, column)
    }

    fn nth(&mut self, n: usize) -> Option<Self::Item> {
        self.column += n as u32;
        self.next()
    }
}

impl<'a> Iterator for ColumnView<'a> {
    type Item = OneRow<'a>;

    fn next(&mut self) -> Option<Self::Item> {
        let (start, end) = (self.start, self.end);
        if start == end {
            None
        } else {
            self.start += 1;
            Some(OneRow::new(self.buffer, start))
        }
    }
}

impl<'a> DoubleEndedIterator for ColumnView<'a> {
    fn next_back(&mut self) -> Option<Self::Item> {
        let (start, end) = (self.start, self.end);
        if start == end {
            None
        } else {
            self.end -= 1;
            Some(OneRow::new(self.buffer, end))
        }
    }
}

// left, top, right, bottom
fn detect_border(image: &ImageBuffer<Rgba<u8>, &[u8]>) -> Option<[i32; 4]> {
    let (w, h) = image.dimensions();
    let top = detect_border_lines(image.rows(), w);
    let bottom = detect_border_lines(image.rows().rev(), w);
    let column = ColumnView::new(image);
    let left = detect_border_lines(column.clone(), h);
    let right = detect_border_lines(column.rev(), h);
    Some([left, top, w as i32 - right, h as i32 - bottom])
}

#[no_mangle]
#[allow(non_snake_case)]
#[jni_fn("com.hippo.ehviewer.image.ImageKt")]
pub fn detectBorder(mut env: JNIEnv, _class: JClass, object: jobject) -> jintArray {
    jni_throwing(&mut env, |env| {
        let slice = with_bitmap_content(env, object, |img| Ok(detect_border(img)))?;
        let array = env.new_int_array(4)?;
        env.set_int_array_region(&array, 0, &slice.ok_or(anyhow!("Image too small!"))?)?;
        Ok(array.into_raw())
    })
}
