use super::core::ImageConsumer;
use crate::img::core::CustomPixel;
use anyhow::Result;
use image::{GrayImage, ImageBuffer};
use rxing::common::HybridBinarizer;
use rxing::qrcode::detector::FinderPatternFinder;
use rxing::{BinaryBitmap, DecodeHints, Luma8LuminanceSource, Point};
use std::f32::consts::FRAC_1_SQRT_2;

fn image_buffer_to_luma8<P: CustomPixel>(
    src: &ImageBuffer<P, &[P::Subpixel]>,
) -> Luma8LuminanceSource {
    let (w, h) = src.dimensions();
    let mut dst = GrayImage::new(w, h);
    for (to, from) in dst.pixels_mut().zip(src.pixels()) {
        *to = from.to_luma8();
    }
    Luma8LuminanceSource::new(dst.into_raw(), w, h)
}

pub struct QrCode;

impl ImageConsumer<bool> for QrCode {
    fn apply<P: CustomPixel>(self, image: &ImageBuffer<P, &[P::Subpixel]>) -> Result<bool> {
        let source = image_buffer_to_luma8(image);
        let image = BinaryBitmap::new(HybridBinarizer::new(source));
        match FinderPatternFinder::new(image.get_black_matrix()).find(&DecodeHints::default()) {
            Ok(info) => {
                // Check if the region is squarish
                let top_left = Point::from(info.getTopLeft());
                let top_right = Point::from(info.getTopRight());
                let bottom_left = Point::from(info.getBottomLeft());
                let length = top_right.distance(top_left);
                let epsilon = length * 0.05;
                Ok((bottom_left.distance(top_left) - length).abs() < epsilon
                    && (bottom_left.distance(top_right) * FRAC_1_SQRT_2 - length).abs() < epsilon)
            }
            Err(_) => Ok(false),
        }
    }
}
