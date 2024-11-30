use anyhow::Result;
use image::{GenericImage, GenericImageView, ImageBuffer, Pixel, Primitive};
use std::ops::Deref;

#[allow(dead_code)]
pub fn copy_region<S: Primitive, P: Pixel<Subpixel = S>>(
    src: ImageBuffer<P, &[S]>,
    mut dst: ImageBuffer<P, &mut [S]>,
    x: u32,
    y: u32,
    w: u32,
    h: u32,
) -> Result<()> {
    let view: image::SubImage<&ImageBuffer<P, &[S]>> = src.view(x, y, w, h);
    Ok(dst.copy_from(view.deref(), 0, 0)?)
}
