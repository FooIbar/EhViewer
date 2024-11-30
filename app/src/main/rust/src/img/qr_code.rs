use super::utils::to_luma8;
use image::{ImageBuffer, Luma, Pixel, Primitive};
use rxing::common::HybridBinarizer;
use rxing::qrcode::detector::FinderPatternFinder;
use rxing::{BinaryBitmap, DecodingHintDictionary, Luma8LuminanceSource, Point};
use std::f32::consts::FRAC_1_SQRT_2;

fn image_buffer_to_luma8<S: Primitive, P: Pixel<Subpixel = S>>(
    src: ImageBuffer<P, &[S]>,
) -> Luma8LuminanceSource {
    let (w, h) = src.dimensions();
    let mut dst: ImageBuffer<Luma<u8>, Vec<_>> = ImageBuffer::new(w, h);
    for (x, y, p) in src.enumerate_pixels() {
        dst.put_pixel(x, y, to_luma8(p));
    }
    Luma8LuminanceSource::new(dst.into_raw(), w, h)
}

#[allow(dead_code)]
pub fn detect_image_ad<S: Primitive, P: Pixel<Subpixel = S>>(image: ImageBuffer<P, &[S]>) -> bool {
    let source = image_buffer_to_luma8(image);
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
