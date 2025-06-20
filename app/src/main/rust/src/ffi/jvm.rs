#![cfg(feature = "jvm")]

use crate::EhError;
use crate::parser::api::parse_vote_tag;
use crate::parser::archive::{parse_archive_url, parse_archives, parse_archives_with_funds};
use crate::parser::config::parse_fav_cat;
use crate::parser::detail::{parse_comments, parse_event_pane, parse_gallery_detail};
use crate::parser::detail::{parse_pages, parse_preview_list};
use crate::parser::fav::parse_fav;
use crate::parser::home::parse_limit;
use crate::parser::list::parse_info_list;
use crate::parser::profile::{parse_profile, parse_profile_url};
use crate::parser::torrent::parse_torrent_list;
use android_logger::Config;
use anyhow::{Context, Result, ensure};
use jni::objects::{JByteBuffer, JClass};
use jni::sys::{JNI_TRUE, JNI_VERSION_1_6, jboolean, jint, jobject};
use jni::{JNIEnv, JavaVM};
use jni_fn::jni_fn;
use log::{LevelFilter, debug};
use serde::Serialize;
use std::ffi::c_void;
use std::io::Cursor;
use std::ptr::slice_from_raw_parts_mut;
use std::str::from_utf8_unchecked;
use tl::{ParserOptions, VDom};

#[jni_fn("com.hippo.ehviewer.client.parser.FavoritesParserKt")]
pub fn parseFav(mut env: JNIEnv, _class: JClass, input: JByteBuffer, limit: jint) -> jint {
    parse_marshal_inplace(&mut env, input, limit, |dom, html| {
        parse_fav(dom, dom.parser(), html)
    })
}

#[jni_fn("com.hippo.ehviewer.client.parser.HomeParserKt")]
pub fn parseLimit(mut env: JNIEnv, _class: JClass, input: JByteBuffer, limit: jint) -> jint {
    parse_marshal_inplace(&mut env, input, limit, |dom, _| {
        parse_limit(dom, dom.parser()).context("Can't parse Limit")
    })
}

#[jni_fn("com.hippo.ehviewer.client.parser.GalleryListParserKt")]
pub fn parseGalleryInfoList(mut env: JNIEnv, _: JClass, buffer: JByteBuffer, limit: jint) -> jint {
    parse_marshal_inplace(&mut env, buffer, limit, |dom, str| {
        parse_info_list(dom, dom.parser(), str)
    })
}

#[jni_fn("com.hippo.ehviewer.client.parser.GalleryDetailParser")]
pub fn nativeParse(mut env: JNIEnv, _: JClass, buffer: JByteBuffer, limit: jint) -> jint {
    let options = ParserOptions::default().track_ids();
    parse_marshal_inplace_with_options(&mut env, buffer, limit, options, |dom, html| {
        parse_gallery_detail(dom, html).map(|detail| (detail, parse_event_pane(dom, dom.parser())))
    })
}

#[jni_fn("com.hippo.ehviewer.client.parser.GalleryDetailParser")]
pub fn nativeParseComments(mut env: JNIEnv, _: JClass, buffer: JByteBuffer, limit: jint) -> jint {
    let options = ParserOptions::default().track_ids();
    parse_marshal_inplace_with_options(&mut env, buffer, limit, options, |dom, _| {
        parse_comments(dom)
    })
}

#[jni_fn("com.hippo.ehviewer.client.parser.GalleryDetailParser")]
pub fn nativeParsePreviews(mut env: JNIEnv, _: JClass, buffer: JByteBuffer, limit: jint) -> jint {
    let options = ParserOptions::default().track_ids();
    parse_marshal_inplace_with_options(&mut env, buffer, limit, options, |dom, _| {
        Ok((
            parse_preview_list(dom, dom.parser())?,
            parse_pages(dom, dom.parser())?,
        ))
    })
}

#[jni_fn("com.hippo.ehviewer.client.parser.VoteTagParser")]
pub fn nativeParse(mut env: JNIEnv, _: JClass, buffer: JByteBuffer, limit: jint) -> jint {
    parse_raw_marshal_inplace(&mut env, buffer, limit, parse_vote_tag)
}

#[jni_fn("com.hippo.ehviewer.client.parser.EventPaneParser")]
pub fn parseEventPane(mut env: JNIEnv, _class: JClass, input: JByteBuffer, limit: jint) -> jint {
    parse_marshal_inplace(&mut env, input, limit, |dom, _| {
        Ok(parse_event_pane(dom, dom.parser()))
    })
}

#[jni_fn("com.hippo.ehviewer.client.parser.TorrentParserKt")]
pub fn parseTorrent(mut env: JNIEnv, _class: JClass, buffer: JByteBuffer, limit: jint) -> jint {
    parse_marshal_inplace(&mut env, buffer, limit, |dom, _| {
        parse_torrent_list(dom, dom.parser())
    })
}

#[jni_fn("com.hippo.ehviewer.client.parser.UserConfigParser")]
pub fn parseFavCat(mut env: JNIEnv, _class: JClass, buffer: JByteBuffer, limit: jint) -> jint {
    parse_marshal_inplace(&mut env, buffer, limit, |_, body| Ok(parse_fav_cat(body)))
}

#[jni_fn("com.hippo.ehviewer.client.parser.ArchiveParserKt")]
pub fn parseArchives(
    mut env: JNIEnv,
    _class: JClass,
    buffer: JByteBuffer,
    limit: jint,
    parse_funds: jboolean,
) -> jint {
    if parse_funds == JNI_TRUE {
        parse_marshal_inplace(&mut env, buffer, limit, |dom, html| {
            parse_archives_with_funds(dom, dom.parser(), html)
        })
    } else {
        parse_marshal_inplace(&mut env, buffer, limit, |dom, html| {
            parse_archives(dom, dom.parser(), html)
        })
    }
}

#[jni_fn("com.hippo.ehviewer.client.parser.ArchiveParserKt")]
pub fn parseArchiveUrl(mut env: JNIEnv, _class: JClass, buffer: JByteBuffer, limit: jint) -> jint {
    parse_marshal_inplace(&mut env, buffer, limit, |dom, html| {
        parse_archive_url(dom, dom.parser(), html)
    })
}

#[jni_fn("com.hippo.ehviewer.client.parser.ProfileParser")]
pub fn parseProfileUrl(mut env: JNIEnv, _class: JClass, buffer: JByteBuffer, limit: jint) -> jint {
    parse_marshal_inplace(&mut env, buffer, limit, |dom, _| {
        parse_profile_url(dom, dom.parser())
    })
}

#[jni_fn("com.hippo.ehviewer.client.parser.ProfileParser")]
pub fn parseProfile(mut env: JNIEnv, _class: JClass, buffer: JByteBuffer, limit: jint) -> jint {
    parse_marshal_inplace(&mut env, buffer, limit, |dom, _| {
        parse_profile(dom, dom.parser())
    })
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

#[inline]
pub fn parse_marshal_inplace<F, R>(env: &mut JNIEnv, str: JByteBuffer, limit: jint, f: F) -> i32
where
    F: Fn(&mut VDom, &str) -> Result<R>,
    R: Serialize,
{
    parse_marshal_inplace_with_options(env, str, limit, ParserOptions::default(), f)
}

pub fn parse_marshal_inplace_with_options<F, R>(
    env: &mut JNIEnv,
    str: JByteBuffer,
    limit: jint,
    options: ParserOptions,
    f: F,
) -> i32
where
    F: Fn(&mut VDom, &str) -> Result<R>,
    R: Serialize,
{
    parse_raw_marshal_inplace(env, str, limit, |html| {
        let mut dom = tl::parse(html, options)?;
        ensure!(dom.version().is_some(), EhError::Error(html.to_string()));
        f(&mut dom, html)
    })
}

pub fn parse_raw_marshal_inplace<F, R>(env: &mut JNIEnv, str: JByteBuffer, limit: jint, f: F) -> i32
where
    F: Fn(&str) -> Result<R>,
    R: Serialize,
{
    jni_throwing(env, |env| {
        let buffer = deref_mut_direct_bytebuffer(env, str)?;
        let value = {
            // SAFETY: ktor client ensure html content is valid utf-8.
            let body = unsafe { from_utf8_unchecked(&buffer[..limit as usize]) };
            f(body)?
        };
        let mut cursor = Cursor::new(buffer);
        serde_cbor::to_writer(&mut cursor, &value)?;
        Ok(cursor.position() as i32)
    })
}

#[unsafe(no_mangle)]
#[allow(non_snake_case)]
pub extern "system" fn JNI_OnLoad(_: JavaVM, _: *mut c_void) -> jint {
    android_logger::init_once(Config::default().with_max_level(LevelFilter::Debug));
    let libwebp_version = unsafe { libwebp_sys::WebPGetDecoderVersion() };
    debug!("libwebp version: {libwebp_version:#08x}");
    JNI_VERSION_1_6
}
