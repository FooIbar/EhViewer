#![cfg(feature = "jvm")]

use crate::parser::config::parse_fav_cat;
use crate::parser::fav::parse_fav;
use crate::parser::home::parse_limit;
use crate::parser::list::parse_info_list;
use crate::parser::torrent::parse_torrent_list;
use crate::EhError;
use android_logger::Config;
use anyhow::{ensure, Context, Result};
use jni::objects::{JByteBuffer, JClass};
use jni::sys::{jboolean, jint, jobject, JNI_VERSION_1_6};
use jni::{JNIEnv, JavaVM};
use jni_fn::jni_fn;
use log::LevelFilter;
use serde::Serialize;
use std::ffi::c_void;
use std::io::Cursor;
use std::ptr::slice_from_raw_parts_mut;
use std::str::from_utf8_unchecked;
use tl::{ParserOptions, VDom};

#[no_mangle]
#[allow(non_snake_case)]
#[jni_fn("com.hippo.ehviewer.client.parser.FavoritesParserKt")]
pub fn parseFav(mut env: JNIEnv, _class: JClass, input: JByteBuffer, limit: jint) -> jint {
    parse_marshal_inplace(&mut env, input, limit, |dom, html| {
        parse_fav(dom, dom.parser(), html)
    })
}

#[no_mangle]
#[allow(non_snake_case)]
#[jni_fn("com.hippo.ehviewer.client.parser.HomeParserKt")]
pub fn parseLimit(mut env: JNIEnv, _class: JClass, input: JByteBuffer, limit: jint) -> jint {
    parse_marshal_inplace(&mut env, input, limit, |dom, _| {
        parse_limit(dom, dom.parser()).context("Can't parse Limit")
    })
}

#[no_mangle]
#[allow(non_snake_case)]
#[jni_fn("com.hippo.ehviewer.client.parser.GalleryListParserKt")]
pub fn parseGalleryInfoList(mut env: JNIEnv, _: JClass, buffer: JByteBuffer, limit: jint) -> jint {
    parse_marshal_inplace(&mut env, buffer, limit, |dom, str| {
        parse_info_list(dom, dom.parser(), str)
    })
}

#[no_mangle]
#[allow(non_snake_case)]
#[jni_fn("com.hippo.ehviewer.client.parser.TorrentParserKt")]
pub fn parseTorrent(mut env: JNIEnv, _class: JClass, buffer: JByteBuffer, limit: jint) -> jint {
    parse_marshal_inplace(&mut env, buffer, limit, |dom, _| {
        parse_torrent_list(dom, dom.parser())
    })
}

#[no_mangle]
#[allow(non_snake_case)]
#[jni_fn("com.hippo.ehviewer.client.parser.UserConfigParser")]
pub fn parseFavCat(mut env: JNIEnv, _class: JClass, buffer: JByteBuffer, limit: jint) -> jint {
    parse_marshal_inplace(&mut env, buffer, limit, |_, body| Ok(parse_fav_cat(body)))
}

fn deref_mut_direct_bytebuffer<'local>(
    env: &JNIEnv,
    buffer: JByteBuffer<'local>,
) -> Result<&'local mut [u8]> {
    let ptr = env.get_direct_buffer_address(&buffer)?;
    let cap = env.get_direct_buffer_capacity(&buffer)?;

    // SAFETY: jni contract, buffer must alive through native call.
    Ok(unsafe { &mut *slice_from_raw_parts_mut(ptr, cap) })
}

pub trait ThrowingHasDefault {
    fn default() -> Self;
}

impl ThrowingHasDefault for jboolean {
    fn default() -> Self {
        0 as jboolean
    }
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

impl ThrowingHasDefault for () {
    fn default() -> Self {}
}

pub fn jni_throwing<F, R>(env: &mut JNIEnv, f: F) -> R
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

pub fn parse_marshal_inplace<F, R>(env: &mut JNIEnv, str: JByteBuffer, limit: jint, f: F) -> i32
where
    F: Fn(&VDom, &str) -> Result<R>,
    R: Serialize,
{
    jni_throwing(env, |env| {
        let buffer = deref_mut_direct_bytebuffer(env, str)?;
        let value = {
            // SAFETY: ktor client ensure html content is valid utf-8.
            let html = unsafe { from_utf8_unchecked(&buffer[..limit as usize]) };

            let dom = tl::parse(html, ParserOptions::default())?;
            ensure!(dom.version().is_some(), EhError::Error(html.to_string()));
            f(&dom, html)?
        };
        let mut cursor = Cursor::new(buffer);
        serde_cbor::to_writer(&mut cursor, &value)?;
        Ok(cursor.position() as i32)
    })
}

#[no_mangle]
pub extern "system" fn JNI_OnLoad(_: JavaVM, _: *mut c_void) -> jint {
    android_logger::init_once(Config::default().with_max_level(LevelFilter::Debug));
    JNI_VERSION_1_6
}
