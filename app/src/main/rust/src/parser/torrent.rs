use catch_panic::catch_panic;
use jni_fn::jni_fn;
use jnix::jni::objects::{JByteBuffer, JClass};
use jnix::jni::sys::jint;
use jnix::jni::JNIEnv;
use quick_xml::escape::unescape;
use regex;
use serde::Serialize;
use std::ptr::slice_from_raw_parts;
use std::str::from_utf8_unchecked;
use tl::{Parser, VDom};

#[derive(Serialize)]
struct Torrent {
    posted: String,
    size: String,
    seeds: i32,
    peers: i32,
    downloads: i32,
    url: String,
    name: String,
}

fn parse_torrent_list(dom: &VDom, parser: &Parser) -> Option<Vec<Torrent>> {
    let list = dom.query_selector("table")?.filter_map(|e| {
        let html = e.get(parser)?.inner_html(parser);
        if html.contains("Expunged") {
            None
        } else {
            let grp = regex!("</span> ([0-9-]+) [0-9:]+</td>[\\s\\S]+</span> ([0-9.]+ [KMGT]iB)</td>[\\s\\S]+</span> ([0-9]+)</td>[\\s\\S]+</span> ([0-9]+)</td>[\\s\\S]+</span> ([0-9]+)</td>[\\s\\S]+</span>([^<]+)</td>[\\s\\S]+onclick=\"document.location='([^\"]+)'[^<]+>([^<]+)</a>").captures(&html).unwrap();
            let name = unescape(&grp[8]).ok()?;
            Some(Torrent {
                posted: grp[1].to_string(),
                size: grp[2].to_string(),
                seeds: grp[3].parse().ok()?,
                peers: grp[4].parse().ok()?,
                downloads: grp[5].parse().ok()?,
                url: grp[7].to_string(),
                name: name.to_string()
            })
        }
    }).collect::<Vec<Torrent>>();
    Some(list)
}

#[no_mangle]
#[catch_panic()]
#[allow(non_snake_case)]
#[jni_fn("com.hippo.ehviewer.client.parser.TorrentParserKt")]
pub fn parseTorrent(env: JNIEnv, _class: JClass, buffer: JByteBuffer, limit: jint) -> jint {
    let ptr = env.get_direct_buffer_address(buffer).unwrap();
    let html = unsafe {
        let buff = slice_from_raw_parts(ptr, limit as usize);
        from_utf8_unchecked(&*buff)
    };
    let dom = tl::parse(html, tl::ParserOptions::default()).unwrap();
    let parser = dom.parser();
    let list = parse_torrent_list(&dom, parser);
    return match list {
        None => 0,
        Some(value) => {
            let str = serde_json::to_vec(&value).unwrap();
            unsafe { ptr.copy_from(str.as_ptr(), str.len()) }
            str.len() as i32
        }
    };
}
