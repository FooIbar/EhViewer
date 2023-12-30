mod parser;

extern crate android_logger;
extern crate apply;
extern crate jni_fn;
extern crate jnix;
extern crate jnix_macros;
extern crate log;
extern crate once_cell;
extern crate quick_xml;
extern crate regex_lite;
extern crate serde;
extern crate tl;

use android_logger::Config;
use jnix::jni::objects::JByteBuffer;
use jnix::jni::sys::{jint, JavaVM, JNI_VERSION_1_6};
use jnix::jni::JNIEnv;
use log::LevelFilter;
use serde::Serialize;
use std::ffi::c_void;
use std::panic::{catch_unwind, UnwindSafe};
use std::ptr::slice_from_raw_parts;
use std::str::from_utf8_unchecked;
use tl::{Bytes, Node, NodeHandle, Parser, VDom};

#[macro_export]
macro_rules! regex {
    ($re:literal $(,)?) => {{
        static RE: once_cell::sync::OnceCell<regex_lite::Regex> = once_cell::sync::OnceCell::new();
        RE.get_or_init(|| regex_lite::Regex::new($re).unwrap())
    }};
}

const EHGT_PREFIX: &str = "https://ehgt.org/";
const EX_PREFIX: &str = "https://s.exhentai.org/";

fn check_html(str: &str) {
    if !str.contains('<') {
        panic!("{}", str)
    }
}

fn get_vdom_first_element_by_class_name<'a>(dom: &'a VDom, name: &str) -> Option<&'a Node<'a>> {
    let handle = dom.get_elements_by_class_name(name).next()?;
    handle.get(dom.parser())
}

fn get_first_element_by_class_name<'a>(
    node: &'a Node,
    parser: &'a Parser,
    id: &str,
) -> Option<&'a Node<'a>> {
    let handle = node.find_node(parser, &mut |n| match n.as_tag() {
        None => false,
        Some(tag) => tag.attributes().is_class_member(id),
    })?;
    handle.get(parser)
}

fn get_element_by_id<'b, S>(node: &'b Node, parser: &'b Parser, id: S) -> Option<&'b Node<'b>>
where
    S: Into<Bytes<'b>>,
{
    let bytes: Bytes = id.into();
    let handle = node.find_node(parser, &mut |n| match n.as_tag() {
        None => false,
        Some(tag) => tag.attributes().id().map_or(false, |x| x.eq(&bytes)),
    })?;
    handle.get(parser)
}

fn parse_marshal_inplace<F, R>(env: &JNIEnv, str: JByteBuffer, limit: jint, mut f: F) -> i32
where
    F: FnMut(&VDom, &Parser, &str) -> Option<R> + UnwindSafe,
    R: Serialize,
{
    let ptr = env.get_direct_buffer_address(str).unwrap();
    let html = unsafe {
        let buff = slice_from_raw_parts(ptr, limit as usize);
        from_utf8_unchecked(&*buff)
    };
    match catch_unwind(move || {
        let dom = tl::parse(html, tl::ParserOptions::default()).unwrap();
        let parser = dom.parser();
        let result = f(&dom, parser, html);
        match result {
            // Nothing to marshal
            None => 0,
            Some(value) => {
                // Figure out how to directly write to ByteBuffer and get bytes count we write
                let str = serde_json::to_vec(&value).unwrap();
                unsafe { ptr.copy_from(str.as_ptr(), str.len()) }
                str.len() as i32
            }
        }
    }) {
        Ok(result) => result,
        Err(err) => {
            let msg = match err.downcast_ref::<&'static str>() {
                Some(s) => *s,
                None => match err.downcast_ref::<String>() {
                    Some(s) => &s[..],
                    None => "Box<dyn Any>",
                },
            };
            env.throw_new("java/lang/RuntimeException", msg).unwrap();
            0
        }
    }
}

fn get_node_handle_attr<'a>(
    node: &NodeHandle,
    parser: &'a Parser,
    attr: &'a str,
) -> Option<&'a str> {
    get_node_attr(node.get(parser)?, attr)
}

fn get_node_attr<'a>(node: &'a Node<'_>, attr: &'a str) -> Option<&'a str> {
    node.as_tag()?.attributes().get(attr)??.try_as_utf8_str()
}

// Should not use it at too upper node, since it will do DFS?
fn query_childs_first_match_attr<'a>(
    node: &'a Node<'_>,
    parser: &'a Parser,
    attr: &'a str,
) -> Option<&'a str> {
    let selector = format!("[{}]", attr);
    let mut iter = node.as_tag()?.query_selector(parser, &selector)?;
    get_node_handle_attr(&iter.next()?, parser, attr)
}

#[no_mangle]
pub extern "system" fn JNI_OnLoad(_: JavaVM, _: *mut c_void) -> jint {
    android_logger::init_once(Config::default().with_max_level(LevelFilter::Debug));
    JNI_VERSION_1_6
}
