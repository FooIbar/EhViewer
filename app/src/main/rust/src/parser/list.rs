use crate::parse_marshal_inplace;
use crate::{get_element_by_id, get_vdom_first_element_by_class_name};
use crate::{get_first_element_by_class_name, query_childs_first_match_attr};
use crate::{get_node_attr, get_node_handle_attr, regex};
use crate::{EHGT_PREFIX, EX_PREFIX};
use anyhow::{anyhow, bail, Result};
use jni::objects::{JByteBuffer, JClass};
use jni::sys::jint;
use jni::JNIEnv;
use jni_fn::jni_fn;
use quick_xml::escape::unescape;
use serde::Serialize;
use std::borrow::Cow;
use std::ops::Index;
use tl::{Node, Parser, VDom};

#[derive(Serialize)]
#[allow(non_snake_case)]
struct BaseGalleryInfo {
    gid: i64,
    token: String,
    title: String,
    titleJpn: Option<String>,
    thumbKey: String,
    category: i32,
    posted: String,
    uploader: Option<String>,
    disowned: bool,
    rating: f32,
    rated: bool,
    simpleTags: Vec<String>,
    pages: i32,
    thumbWidth: i32,
    thumbHeight: i32,
    simpleLanguage: Option<String>,
    favoriteSlot: i32,
    favoriteName: Option<String>,
    favoriteNote: Option<String>,
}

#[derive(Serialize)]
#[allow(non_snake_case)]
pub struct GalleryListResult {
    prev: Option<String>,
    next: Option<String>,
    galleryInfoList: Vec<BaseGalleryInfo>,
}

fn to_category_i32(category: &str) -> i32 {
    match category {
        "misc" => 0x1,
        "doujinshi" => 0x2,
        "manga" => 0x4,
        "artistcg" | "artist cg sets" | "artist cg" => 0x8,
        "gamecg" | "game cg sets" | "game cg" => 0x10,
        "imageset" | "image sets" | "image set" => 0x20,
        "cosplay" => 0x40,
        "asianporn" | "asian porn" => 0x80,
        "non-h" => 0x100,
        "western" => 0x200,
        "private" => 0x400,
        _ => 0x800,
    }
}

fn parse_rating(str: &str) -> f32 {
    let mut iter = regex!("\\d+px").find_iter(str);
    let mut next = || iter.next()?.as_str().replace("px", "").parse::<i32>().ok();
    match (next(), next()) {
        (Some(num1), Some(num2)) => {
            let rate = 5 - num1 / 16;
            if num2 == 21 {
                (rate - 1) as f32 + 0.5
            } else {
                rate as f32
            }
        }
        _ => -1.0,
    }
}

fn get_thumb_key(url: &str) -> String {
    url.trim_start_matches(EHGT_PREFIX)
        .trim_start_matches(EX_PREFIX)
        .trim_start_matches("t/")
        .to_string()
}

fn parse_token_and_gid(str: &str) -> Option<(i64, String)> {
    let grp =
        regex!("https?://(?:exhentai.org|e-hentai.org(?:/lofi)?)/(?:g|mpv)/(\\d+)/([0-9a-f]{10})")
            .captures(str)?;
    let token = &grp[2];
    let gid = grp[1].parse().ok()?;
    Some((gid, token.to_string()))
}

fn parse_uploader_and_pages(str: &str) -> (Option<String>, bool, i32) {
    let uploader =
        regex!(r#"<a href="https://e[x-]hentai.org/uploader/.*?">(.*?)</a>|(\(Disowned\))"#)
            .captures(str)
            .map(|grp| {
                grp.get(1)
                    .or_else(|| grp.get(2))
                    .unwrap()
                    .as_str()
                    .to_string()
            });
    let pages = match regex!(r"<div>(\d+) pages</div>").captures(str) {
        None => 0,
        Some(grp) => grp[1].parse().unwrap_or(0),
    };
    (uploader, str.contains("style=\"opacity:0.5\""), pages)
}

fn parse_thumb_resolution(str: &str) -> (i32, i32) {
    match regex!(r"height:(\d+)px;width:(\d+)px").captures(str) {
        None => (0, 0),
        Some(grp) => (grp[1].parse().unwrap_or(0), grp[2].parse().unwrap_or(0)),
    }
}

fn parse_gallery_info(node: &Node, parser: &Parser) -> Option<BaseGalleryInfo> {
    let tag = node.as_tag()?;
    let title = get_first_element_by_class_name(node, parser, "glink")?.inner_text(parser);
    let glname = get_first_element_by_class_name(node, parser, "glname")?;
    let gdlink = match query_childs_first_match_attr(glname, parser, "href") {
        None => query_childs_first_match_attr(node, parser, "href")?,
        Some(attr) => attr,
    };
    let (gid, token) = parse_token_and_gid(gdlink)?;
    let simple_tags = tag
        .query_selector(parser, ".gt, .gtl")?
        .filter_map(|tag| get_node_handle_attr(&tag, parser, "title").map(str::to_string))
        .collect();
    let (thumb, (thumb_height, thumb_width)) =
        match tag.query_selector(parser, "[data-src]")?.next() {
            None => match tag.query_selector(parser, "[src]")?.next() {
                None => return None,
                Some(thumb) => (
                    get_node_handle_attr(&thumb, parser, "src")?,
                    parse_thumb_resolution(get_node_handle_attr(&thumb, parser, "style")?),
                ),
            },
            Some(thumb) => (
                get_node_handle_attr(&thumb, parser, "data-src")?,
                parse_thumb_resolution(get_node_handle_attr(&thumb, parser, "style")?),
            ),
        };
    let category = match get_first_element_by_class_name(node, parser, "cn") {
        None => match get_first_element_by_class_name(node, parser, "cs") {
            None => Cow::from("unknown"),
            Some(cs) => cs.inner_text(parser),
        },
        Some(cn) => cn.inner_text(parser),
    };
    let (posted, favorite_name) =
        match get_element_by_id(node, parser, format!("posted_{gid}").as_str()) {
            None => ("".to_string(), None),
            Some(node) => (
                node.inner_text(parser).trim().to_string(),
                get_node_attr(node, "title").map(str::to_string),
            ),
        };
    let ir = get_first_element_by_class_name(node, parser, "ir")?
        .as_tag()?
        .attributes();
    let ir_c = ir.class()?.try_as_utf8_str()?;
    let rating = ir.get("style")??.try_as_utf8_str()?;
    let (uploader, disowned, pages) =
        match tag.query_selector(parser, ".glhide, .gl3e, .gl5t")?.next() {
            None => (None, false, 0),
            Some(node) => parse_uploader_and_pages(&node.get(parser)?.inner_html(parser)),
        };
    let favorite_note = get_element_by_id(node, parser, format!("favnote_{gid}").as_str())
        .map(|e| e.inner_text(parser).to_string());
    Some(BaseGalleryInfo {
        gid,
        token,
        title: unescape(title.trim()).ok()?.to_string(),
        titleJpn: None,
        thumbKey: get_thumb_key(thumb),
        category: to_category_i32(&category.trim().to_lowercase()),
        posted,
        uploader,
        disowned,
        rating: parse_rating(rating),
        rated: ir_c.contains("irr") || ir_c.contains("irg") || ir_c.contains("irb"),
        simpleTags: simple_tags,
        pages,
        thumbWidth: thumb_width,
        thumbHeight: thumb_height,
        simpleLanguage: None,
        favoriteSlot: if favorite_name.is_some() { 0 } else { -2 },
        favoriteName: favorite_name,
        favoriteNote: favorite_note,
    })
}

pub fn parse_info_list(dom: &VDom, parser: &Parser, str: &str) -> Result<GalleryListResult> {
    if str.contains("<p>You do not have any watched tags") {
        bail!("No watched tags!")
    }
    if str.contains("No hits found</p>") || str.contains("No unfiltered results") {
        bail!("No hits found!")
    }
    let f = || {
        let itg = get_vdom_first_element_by_class_name(dom, "itg")?;
        let children = itg.children()?;
        let iter = children.top().iter();
        let info: Vec<BaseGalleryInfo> = iter
            .filter_map(|x| parse_gallery_info(x.get(parser)?, parser))
            .collect();
        let prev = dom.get_element_by_id("uprev").and_then(|e| {
            let str = get_node_handle_attr(&e, parser, "href")?;
            Some(
                regex!("prev=(\\d+(-\\d+)?)")
                    .captures(str)?
                    .index(1)
                    .to_string(),
            )
        });
        let next = dom.get_element_by_id("unext").and_then(|e| {
            let str = get_node_handle_attr(&e, parser, "href")?;
            Some(
                regex!("next=(\\d+(-\\d+)?)")
                    .captures(str)?
                    .index(1)
                    .to_string(),
            )
        });
        (!info.is_empty()).then_some(GalleryListResult {
            prev,
            next,
            galleryInfoList: info,
        })
    };
    f().ok_or(anyhow!("No content"))
}

#[no_mangle]
#[allow(non_snake_case)]
#[jni_fn("com.hippo.ehviewer.client.parser.GalleryListParserKt")]
pub fn parseGalleryInfoList(mut env: JNIEnv, _: JClass, buffer: JByteBuffer, limit: jint) -> jint {
    parse_marshal_inplace(&mut env, buffer, limit, |dom, str| {
        parse_info_list(dom, dom.parser(), str)
    })
}
