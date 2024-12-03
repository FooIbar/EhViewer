use super::core::{CustomPixel, ImageConsumer};
use anyhow::{Context, Result};
use image::{GenericImage, GenericImageView, ImageBuffer, Pixel};
use std::{ops::Deref, ptr::slice_from_raw_parts_mut};

fn ptr_as_image_mut<'local, P: Pixel>(
    ptr: *mut u8,
    w: u32,
    h: u32,
    channel: u32,
) -> Option<ImageBuffer<P, &'local mut [P::Subpixel]>> {
    let size = (w * h * channel) as usize;
    let buffer = unsafe { &mut *slice_from_raw_parts_mut(ptr as *mut P::Subpixel, size) };
    ImageBuffer::from_raw(w, h, buffer)
}

#[allow(dead_code)]
pub struct CopyRegion {
    pub ptr: *mut u8,
    pub target_dim: (u32, u32),
    pub src_rect: (u32, u32, u32, u32),
}

impl ImageConsumer<()> for CopyRegion {
    fn apply<P: CustomPixel>(self, src: &ImageBuffer<P, &[P::Subpixel]>) -> Result<()> {
        let (x, y, w, h) = self.src_rect;
        let (dst_w, dst_h) = self.target_dim;
        let mut dst =
            ptr_as_image_mut(self.ptr, dst_w, dst_h, P::FAKE_CHANNEL).context("Unreachable!!!")?;
        let view = src.view(x, y, w, h);
        Ok(dst.copy_from(view.deref(), 0, 0)?)
    }
}
