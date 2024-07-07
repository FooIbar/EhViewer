use anyhow::{bail, Result};
use jni::objects::{JByteBuffer, JClass};
use jni::sys::jint;
use jni::JNIEnv;
use jni_fn::jni_fn;
use parse_marshal_inplace;
use parser::list::{parse_info_list, GalleryListResult};
use prost::Message;
use quick_xml::escape::unescape;
use tl::Parser;
use tl::VDom;

#[derive(Clone, PartialEq, Message)]
struct FavResult {
    #[prost(string, repeated)]
    cat_array: Vec<String>,
    #[prost(int32, repeated)]
    count_array: Vec<i32>,
    #[prost(message, required)]
    gallery_list_result: GalleryListResult,
}

fn parse_fav(dom: &VDom, parser: &Parser, html: &str) -> Result<FavResult> {
    if html.contains("This page requires you to log on.</p>") {
        bail!("Not logged in!")
    }
    let vec: Vec<(String, i32)> = dom
        .get_elements_by_class_name("fp")
        .enumerate()
        .filter_map(|(i, e)| {
            if i == 10 {
                return None;
            }
            let top = e.get(parser)?.children()?;
            let children = top.top();
            let cat = children[5].get(parser)?.inner_text(parser);
            let name = unescape(&cat).ok()?.trim().to_string();
            let count = children[1]
                .get(parser)?
                .inner_text(parser)
                .parse::<i32>()
                .ok()?;
            Some((name, count))
        })
        .collect();
    if vec.len() == 10 {
        let list = parse_info_list(dom, parser, html)?;
        let cat = vec.iter().cloned().unzip();
        Ok(FavResult {
            cat_array: cat.0,
            count_array: cat.1,
            gallery_list_result: list,
        })
    } else {
        bail!("Illegal fav cat count!")
    }
}

#[no_mangle]
#[allow(non_snake_case)]
#[jni_fn("com.hippo.ehviewer.client.parser.FavoritesParserKt")]
pub fn parseFav(mut env: JNIEnv, _class: JClass, input: JByteBuffer, limit: jint) -> jint {
    parse_marshal_inplace(&mut env, input, limit, |dom, html| {
        parse_fav(dom, dom.parser(), html)
    })
}
