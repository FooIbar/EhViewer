use image::{Luma, Pixel, Primitive};

pub fn to_luma8<S: Primitive, P: Pixel<Subpixel = S>>(p: &P) -> Luma<u8> {
    Luma([p.to_luma().0[0].to_u8().unwrap()])
}
