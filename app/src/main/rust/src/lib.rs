mod parser;

extern crate android_logger;
extern crate jni;
extern crate jni_fn;
extern crate log;
extern crate once_cell;
extern crate quick_xml;
extern crate regex_lite;
extern crate serde;
extern crate tl;

use android_logger::Config;
use jni::objects::JByteBuffer;
use jni::sys::{jint, JavaVM, JNI_VERSION_1_6};
use jni::JNIEnv;
use log::LevelFilter;
use serde::Serialize;
use std::ffi::c_void;
use std::io::Cursor;
use std::ptr::{slice_from_raw_parts, slice_from_raw_parts_mut};
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

fn parse_marshal_inplace<F, R>(env: &mut JNIEnv, str: JByteBuffer, limit: jint, mut f: F) -> i32
where
    F: FnMut(&VDom, &Parser, &str) -> Result<R, &'static str>,
    R: Serialize,
{
    let ptr = env.get_direct_buffer_address(&str).unwrap();
    let html = unsafe {
        let buff = slice_from_raw_parts(ptr, limit as usize);
        from_utf8_unchecked(&*buff)
    };
    let mut f = || {
        let dom = tl::parse(html, tl::ParserOptions::default()).map_err(|_| html)?;
        let parser = dom.parser();
        f(&dom, parser, html)
    };
    match f() {
        // Nothing to marshal
        Err(err) => {
            env.throw_new("java/lang/RuntimeException", err).ok();
            0
        }
        Ok(value) => {
            let mut cursor = unsafe {
                let slice = slice_from_raw_parts_mut(ptr, 0x80000);
                Cursor::new(&mut *slice)
            };
            match serde_cbor::to_writer(&mut cursor, &value) {
                Ok(_) => cursor.position() as i32,
                Err(err) => {
                    env.throw_new("java/lang/RuntimeException", format!("{}", err))
                        .ok();
                    0
                }
            }
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
