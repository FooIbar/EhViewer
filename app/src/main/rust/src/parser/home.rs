use anyhow::anyhow;
use jni::objects::{JByteBuffer, JClass};
use jni::sys::jint;
use jni::JNIEnv;
use jni_fn::jni_fn;
use prost::Message;
use tl::Parser;
use tl::VDom;
use {get_vdom_first_element_by_class_name, parse_marshal_inplace};

#[derive(Clone, PartialEq, Message)]
struct Limits {
    #[prost(int32)]
    current: i32,
    #[prost(int32)]
    maximum: i32,
    #[prost(int32)]
    reset_cost: i32,
}

fn parse_limit(dom: &VDom, parser: &Parser) -> Option<Limits> {
    let iter = get_vdom_first_element_by_class_name(dom, "homebox")?
        .as_tag()?
        .query_selector(parser, "strong")?;
    let vec: Vec<i32> = iter
        .filter_map(|e| e.get(parser)?.inner_text(parser).parse::<i32>().ok())
        .collect();
    Some(Limits {
        current: vec[0],
        maximum: vec[1],
        reset_cost: vec[2],
    })
}

#[no_mangle]
#[allow(non_snake_case)]
#[jni_fn("com.hippo.ehviewer.client.parser.HomeParserKt")]
pub fn parseLimit(mut env: JNIEnv, _class: JClass, input: JByteBuffer, limit: jint) -> jint {
    parse_marshal_inplace(&mut env, input, limit, |dom, _| {
        parse_limit(dom, dom.parser()).ok_or(anyhow!("Can't parse Limit"))
    })
}
