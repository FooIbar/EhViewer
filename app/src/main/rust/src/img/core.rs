use anyhow::Result;
use image::{ImageBuffer, Luma, Pixel, Rgb, Rgba};

#[allow(dead_code)]
pub trait ImageConsumer<R> {
    fn apply<T: CustomPixel>(self, buffer: &ImageBuffer<T, &[T::Subpixel]>) -> Result<R>;
}

pub trait CustomPixel: Pixel {
    const FAKE_CHANNEL: u32;
    fn to_luma8(&self) -> Luma<u8>;
}

impl CustomPixel for Rgba<u8> {
    fn to_luma8(&self) -> Luma<u8> {
        self.to_luma()
    }
    const FAKE_CHANNEL: u32 = 4;
}

// RGB_565
impl CustomPixel for Luma<u16> {
    fn to_luma8(&self) -> Luma<u8> {
        let packed = self.channels()[0];
        let rgb = Rgb([
            l5_to_l8(packed >> 11),
            l6_to_l8(packed >> 5),
            l5_to_l8(packed),
        ]);
        rgb.to_luma()
    }
    const FAKE_CHANNEL: u32 = 1;
}

#[inline]
fn l5_to_l8(v: u16) -> u8 {
    (((v & 0x1f) * 527 + 23) >> 6) as u8
}

#[inline]
fn l6_to_l8(v: u16) -> u8 {
    (((v & 0x3f) * 259 + 33) >> 6) as u8
}

// RGBA_F16
impl CustomPixel for Luma<u64> {
    fn to_luma8(&self) -> Luma<u8> {
        let packed = self.channels()[0];
        let rgba = Rgba([
            f16_to_f32(packed),
            f16_to_f32(packed >> 16),
            f16_to_f32(packed >> 32),
            f16_to_f32(packed >> 48),
        ]);
        let l = rgba.to_luma().channels()[0];
        Luma([(l.clamp(0.0, 1.0) * u8::MAX as f32).round() as u8])
    }
    const FAKE_CHANNEL: u32 = 1;
}

#[inline]
fn f16_to_f32(v: u64) -> f32 {
    f16::from_bits((v & 0xffff) as u16) as f32
}
