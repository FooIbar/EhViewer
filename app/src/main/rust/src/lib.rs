mod parser;

use android_logger::Config;
use anyhow::{anyhow, ensure, Result};
use jni::objects::JByteBuffer;
use jni::sys::{jint, jobject, JavaVM, JNI_VERSION_1_6};
use jni::JNIEnv;
use log::LevelFilter;
use serde::Serialize;
use std::ffi::c_void;
use std::io::Cursor;
use std::ptr::slice_from_raw_parts_mut;
use std::str::from_utf8_unchecked;
use tl::ParserOptions;
use tl::{Bytes, Node, NodeHandle, Parser, VDom};

#[macro_export]
macro_rules! regex {
    ($re:literal $(,)?) => {{
        static RE: std::sync::OnceLock<regex_lite::Regex> = std::sync::OnceLock::new();
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

fn deref_mut_direct_bytebuffer(env: &JNIEnv, buffer: JByteBuffer) -> Result<&'static mut [u8]> {
    let ptr = env.get_direct_buffer_address(&buffer)?;
    let cap = env.get_direct_buffer_capacity(&buffer)?;
    Ok(unsafe { &mut *slice_from_raw_parts_mut(ptr, cap) })
}

trait ThrowingHasDefault {
    fn default() -> Self;
}

impl ThrowingHasDefault for jobject {
    fn default() -> Self {
        0 as jobject
    }
}

impl ThrowingHasDefault for i32 {
    fn default() -> Self {
        0
    }
}

fn jni_throwing<F, R>(env: &mut JNIEnv, f: F) -> R
where
    F: FnOnce(&mut JNIEnv) -> Result<R>,
    R: ThrowingHasDefault,
{
    match f(env) {
        Ok(value) => value,
        Err(err) => {
            let msg = format!("{err}");
            env.throw_new("java/lang/RuntimeException", msg).ok();
            R::default()
        }
    }
}

fn parse_marshal_inplace<F, R>(env: &mut JNIEnv, str: JByteBuffer, limit: jint, f: F) -> i32
where
    F: Fn(&VDom, &str) -> Result<R>,
    R: Serialize,
{
    jni_throwing(env, |env| {
        let buffer = deref_mut_direct_bytebuffer(env, str)?;
        let value = {
            let html = unsafe { from_utf8_unchecked(&buffer[..limit as usize]) };
            let dom = tl::parse(html, ParserOptions::default()).map_err(|e| anyhow!(e))?;
            ensure!(dom.version().is_some(), "{html}");
            f(&dom, html)?
        };
        let mut cursor = Cursor::new(buffer);
        serde_cbor::to_writer(&mut cursor, &value)?;
        Ok(cursor.position() as i32)
    })
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
