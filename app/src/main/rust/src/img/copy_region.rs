use super::core::{CustomPixel, ImageConsumer};
use anyhow::Result;
use image::{GenericImage, GenericImageView, ImageBuffer};
use std::ptr::slice_from_raw_parts_mut;

fn ptr_as_image_mut<'a, P: CustomPixel>(
    ptr: *mut !,
    w: u32,
    h: u32,
) -> ImageBuffer<P, &'a mut [P::Subpixel]> {
    let size = (w * h * P::CHANNEL_COUNT as u32) as usize;
    let buffer = unsafe { &mut *slice_from_raw_parts_mut(ptr as *mut P::Subpixel, size) };
    ImageBuffer::from_raw(w, h, buffer).unwrap()
}

pub struct CopyRegion {
    pub ptr: *mut !,
    pub target_dim: (u32, u32),
    pub src_rect: (u32, u32, u32, u32),
}

impl ImageConsumer<()> for CopyRegion {
    fn apply<P: CustomPixel>(self, src: &ImageBuffer<P, &[P::Subpixel]>) -> Result<()> {
        let (x, y, w, h) = self.src_rect;
        let (dst_w, dst_h) = self.target_dim;
        let mut dst = ptr_as_image_mut(self.ptr, dst_w, dst_h);
        let view = src.view(x, y, w, h);
        Ok(dst.copy_from(&*view, 0, 0)?)
    }
}
