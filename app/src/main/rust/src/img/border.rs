use super::core::ImageConsumer;
use crate::img::core::CustomPixel;
use anyhow::Result;
use image::{ImageBuffer, Luma};

/** A line will be considered as having content if 0.25% of it is filled. */
static FILLED_RATIO_LIMIT: f32 = 0.0025;

/** When the threshold is closer to 1, less content will be cropped. **/
static THRESHOLD: f32 = 0.75;

static THRESHOLD_FOR_BLACK: u8 = (255.0 * THRESHOLD) as u8;
static THRESHOLD_FOR_WHITE: u8 = (255.0 - 255.0 * THRESHOLD) as u8;

fn is_white(pixel: Luma<u8>) -> bool {
    pixel.0[0] > THRESHOLD_FOR_WHITE
}

fn is_black(pixel: Luma<u8>) -> bool {
    pixel.0[0] < THRESHOLD_FOR_BLACK
}

fn line_not_filled_by<'pixel, P: CustomPixel + 'pixel>(
    line: impl Iterator<Item = &'pixel P>,
    white: bool,
    limit: i32,
) -> bool {
    let f = if white { is_black } else { is_white };
    line.step_by(2).filter(|p| f(p.to_luma8())).count() as i32 > limit
}

fn try_count_lines<'pixel, P: CustomPixel + 'pixel>(
    iter: impl Iterator<Item = impl Iterator<Item = &'pixel P>>,
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

fn detect_border_lines<'pixel, P: CustomPixel + 'pixel>(
    mut iter: impl Iterator<Item = impl Iterator<Item = &'pixel P>>,
    line_len: u32,
) -> i32 {
    let first_row = iter.next().expect("Image is empty!");
    let filled_limit = (line_len as f32 * FILLED_RATIO_LIMIT / 2.0).round() as i32;
    let (mut black, mut white) = (0, 0);
    for pixel in first_row.step_by(2) {
        let luma = pixel.to_luma8();
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

pub struct DetectBorder;

impl ImageConsumer<[i32; 4]> for DetectBorder {
    fn apply<P: CustomPixel>(self, image: &ImageBuffer<P, &[P::Subpixel]>) -> Result<[i32; 4]> {
        let (w, h) = image.dimensions();
        let top = detect_border_lines(image.rows(), w);
        let bottom = detect_border_lines(image.rows().rev(), w);
        let iter = (0..w).map(|row| (0..h).map(move |col| image.get_pixel(row, col)));
        let left = detect_border_lines(iter.clone(), h);
        let right = detect_border_lines(iter.rev(), h);
        Ok([left, top, w as i32 - right, h as i32 - bottom])
    }
}
