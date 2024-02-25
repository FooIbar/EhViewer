mod parser;

extern crate android_logger;
extern crate base16ct;
extern crate jni;
extern crate jni_fn;
extern crate log;
extern crate once_cell;
extern crate quick_xml;
extern crate regex_lite;
extern crate serde;
extern crate sha1;
extern crate tl;

use android_logger::Config;
use base16ct::lower::encode_str;
use jni::objects::{JByteBuffer, JClass};
use jni::sys::{jint, jstring, JavaVM, JNI_VERSION_1_6};
use jni::JNIEnv;
use jni_fn::jni_fn;
use log::LevelFilter;
use serde::Serialize;
use sha1::{Digest, Sha1};
use std::ffi::c_void;
use std::fs::File;
use std::io::{copy, BufReader, Cursor};
use std::os::fd::BorrowedFd;
use std::ptr::slice_from_raw_parts_mut;
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

fn deref_mut_direct_bytebuffer<'a>(env: &JNIEnv, buffer: JByteBuffer) -> &'a mut [u8] {
    let ptr = env.get_direct_buffer_address(&buffer).unwrap();
    let cap = env.get_direct_buffer_capacity(&buffer).unwrap();
    unsafe { &mut *slice_from_raw_parts_mut(ptr, cap) }
}

fn throw_msg(env: &mut JNIEnv, msg: &str) -> i32 {
    env.throw_new("java/lang/RuntimeException", msg).ok();
    0
}

fn parse_marshal_inplace<F, R>(env: &mut JNIEnv, str: JByteBuffer, limit: jint, f: F) -> i32
where
    F: Fn(&VDom, &str) -> Result<R, &'static str>,
    R: Serialize,
{
    let buffer = deref_mut_direct_bytebuffer(env, str);
    let html = unsafe { from_utf8_unchecked(&buffer[..limit as usize]) };
    let value = match tl::parse(html, tl::ParserOptions::default()) {
        Ok(dom) => match f(&dom, html) {
            Err(err) => return throw_msg(env, err),
            Ok(value) => value,
        },
        Err(_) => return throw_msg(env, html),
    };
    let mut cursor = Cursor::new(buffer);
    match serde_cbor::to_writer(&mut cursor, &value) {
        Ok(_) => cursor.position() as i32,
        Err(err) => throw_msg(env, &format!("{}", err)),
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

#[no_mangle]
#[allow(non_snake_case)]
#[jni_fn("com.hippo.ehviewer.jni.HashKt")]
pub fn sha1(mut env: JNIEnv, _class: JClass, fd: jint) -> jstring {
    let mut sha = Sha1::new();
    let borrowed_fd = unsafe { BorrowedFd::borrow_raw(fd) };
    let owned_fd = match borrowed_fd.try_clone_to_owned() {
        Ok(fd) => fd,
        Err(err) => return throw_msg(&mut env, &format!("{}", err)) as jstring,
    };
    let mut reader = BufReader::new(File::from(owned_fd));
    if let Err(err) = copy(&mut reader, &mut sha) {
        return throw_msg(&mut env, &format!("{}", err)) as jstring;
    };
    let digest = sha.finalize();
    let mut hex_digest = [0u8; 40];
    let hex = match encode_str(&digest, &mut hex_digest) {
        Ok(str) => str,
        Err(err) => return throw_msg(&mut env, &format!("{}", err)) as jstring,
    };
    match env.new_string(hex) {
        Ok(obj) => obj.into_raw(),
        Err(err) => throw_msg(&mut env, &format!("{}", err)) as jstring,
    }
}
