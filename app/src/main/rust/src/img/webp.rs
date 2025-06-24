use libwebp_sys::{
    MODE_rgbA, WebPAnimDecoder, WebPAnimDecoderGetInfo, WebPAnimDecoderGetNext,
    WebPAnimDecoderHasMoreFrames, WebPAnimDecoderNew, WebPAnimDecoderOptions, WebPAnimDecoderReset,
    WebPAnimInfo, WebPData,
};
use std::mem::zeroed;
use std::ptr::null_mut;

pub fn create_decoder(data: &[u8]) -> *mut WebPAnimDecoder {
    let webp_data = WebPData {
        bytes: data.as_ptr(),
        size: data.len(),
    };
    let dec_options = WebPAnimDecoderOptions {
        color_mode: MODE_rgbA,
        use_threads: 1,
        padding: [0; 7],
    };
    unsafe { WebPAnimDecoderNew(&webp_data, &dec_options) }
}

pub(crate) unsafe fn get_image_info(dec: *const WebPAnimDecoder) -> WebPAnimInfo {
    let mut info = unsafe { zeroed() };
    let _ = unsafe { WebPAnimDecoderGetInfo(dec, &mut info) };
    info
}

pub fn pack_image_info(info: WebPAnimInfo) -> u64 {
    (info.canvas_width as u64) << 40 | (info.canvas_height as u64) << 16 | (info.loop_count as u64)
}

pub(crate) unsafe fn decode_next_frame(dec: *mut WebPAnimDecoder, reset: bool) -> (*const u8, i32) {
    let mut buf = null_mut();
    let mut timestamp = 0;
    unsafe {
        if reset || WebPAnimDecoderHasMoreFrames(dec) == 0 {
            WebPAnimDecoderReset(dec);
        }
        let _ = WebPAnimDecoderGetNext(dec, &mut buf, &mut timestamp);
    }
    (buf, timestamp)
}
