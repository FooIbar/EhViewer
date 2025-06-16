use crate::parser::list::{BaseGalleryInfo, get_thumb_key, parse_token_and_gid, to_category_i32};
use crate::{EhError, get_first_child, get_first_element_by_class_name, get_node_handle_attr};
use crate::{get_tag_attr, get_vdom_first_element_by_class_name, regex};
use anyhow::{Context, Result, bail};
use chrono::naive::NaiveDateTime;
use quick_xml::escape::unescape;
use serde::Serialize;
use tl::{Bytes, Node, NodeHandle, Parser, VDom};

#[derive(Serialize, Debug, Default)]
#[allow(non_snake_case)]
pub struct GalleryDetail {
    pub galleryInfo: BaseGalleryInfo,
    pub apiUid: i64,
    pub apiKey: Option<String>,
    pub torrentCount: i32,
    pub parent: Option<String>,
    pub newerVersions: Vec<BaseGalleryInfo>,
    pub visible: Option<String>,
    pub language: Option<String>,
    pub size: Option<String>,
    pub favoriteCount: i32,
    pub ratingCount: i32,
    pub tagGroups: Vec<GalleryTagGroup>,
    pub comments: GalleryCommentList,
    pub previewList: Vec<GalleryPreview>,
}

#[derive(Serialize, Debug)]
pub enum PowerStatus {
    Solid,
    Weak,
    Active,
}

#[derive(Serialize, Debug)]
pub enum VoteStatus {
    None,
    Up,
    Down,
}

#[derive(Serialize, Debug)]
pub struct GalleryTag {
    pub text: String,
    pub power: PowerStatus,
    pub vote: VoteStatus,
}

#[derive(Serialize, Debug)]
pub struct GalleryTagGroup {
    pub namespace: String,
    pub tags: Vec<GalleryTag>,
}

#[derive(Serialize, Debug, Default)]
#[allow(non_snake_case)]
pub struct GalleryComment {
    pub id: i64,
    pub score: i32,
    pub editable: bool,
    pub voteUpAble: bool,
    pub voteUpEd: bool,
    pub voteDownAble: bool,
    pub voteDownEd: bool,
    pub uploader: bool,
    pub voteState: Option<String>,
    pub time: i64,
    pub user: Option<String>,
    pub comment: String,
    pub lastEdited: i64,
}

#[derive(Serialize, Debug, Default)]
#[allow(non_snake_case)]
pub struct GalleryCommentList {
    pub comments: Vec<GalleryComment>,
    pub hasMore: bool,
}

#[derive(Serialize, Debug, Default)]
#[allow(non_snake_case)]
pub struct PreviewData {
    pub url: String,
    pub position: i32,
    pub pToken: String,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub offsetX: Option<i32>,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub clipWidth: Option<i32>,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub clipHeight: Option<i32>,
}

// Should use internally tagged enum, but kotlinx.serialization only support array polymorphism for CBOR
// https://github.com/Kotlin/kotlinx.serialization/issues/2058
#[derive(Serialize, Debug)]
pub struct GalleryPreview(pub String, pub PreviewData);

fn parse_detail_info(gd: &mut GalleryDetail, node: &NodeHandle, parser: &Parser) -> Option<()> {
    let children = node.get(parser)?.children()?;
    let mut iter = children
        .top()
        .iter()
        .filter_map(|n| n.get(parser)?.as_tag());

    let key_tag = iter.next()?;
    let value_tag = iter.next()?;

    let key = key_tag.inner_text(parser);
    let value = value_tag.children().top()[0]
        .get(parser)?
        .inner_text(parser);

    match key.as_ref() {
        "Posted:" => gd.galleryInfo.posted = value.to_string(),
        "Parent:" => {
            gd.parent = get_first_child(value_tag, parser)
                .and_then(|a| get_tag_attr(a, "href").map(str::to_string))
        }
        "Visible:" => gd.visible = Some(value.to_string()),
        "Language:" => gd.language = value.strip_suffix(" &nbsp;").map(str::to_string),
        "File Size:" => gd.size = Some(value.to_string()),
        "Length:" => gd.galleryInfo.pages = value.strip_suffix(" pages")?.parse().ok()?,
        "Favorited:" => {
            gd.favoriteCount = match value.as_ref() {
                "Never" => 0,
                "Once" => 1,
                _ => value.strip_suffix(" times")?.parse().ok()?,
            }
        }
        _ => {}
    }

    Some(())
}

fn parse_detail(gd: &mut GalleryDetail, dom: &VDom, parser: &Parser, body: &str) -> Option<()> {
    let detail_regex = regex!(
        r#"var gid = (\d+);\s*var token = "([a-f0-9]+)";\s*var apiuid = ([-\d]+);\s*var apikey = "([a-f0-9]+)";\s*var average_rating = ([.\d]+);"#
    );
    let caps = detail_regex.captures(body)?;
    gd.galleryInfo.gid = caps[1].parse().ok()?;
    gd.galleryInfo.token = caps[2].to_string();
    gd.apiUid = caps[3].parse().ok()?;
    gd.apiKey = Some(caps[4].to_string());
    gd.galleryInfo.rating = caps[5].parse().ok()?;

    let regex = regex!(r#"[^(]+\(([^)]+)"#);
    let gd5 = dom.get_element_by_id("gd5")?.get(parser)?.as_tag()?;
    let node = gd5.query_selector(parser, "a[onclick]")?.nth(1)?;
    let text = node.get(parser)?.inner_text(parser);
    let caps = regex.captures(&text)?;
    gd.torrentCount = caps[1].parse().ok()?;

    let gd1 = dom.get_element_by_id("gd1")?.get(parser)?.as_tag()?;
    let style = get_tag_attr(get_first_child(gd1, parser)?, "style")?;
    let caps = regex.captures(style)?;
    gd.galleryInfo.thumbKey = get_thumb_key(&caps[1]);

    let title = dom.get_element_by_id("gn")?.get(parser)?.inner_text(parser);
    gd.galleryInfo.title = unescape(&title).ok()?.to_string();
    let title_jpn = dom.get_element_by_id("gj")?.get(parser)?.inner_text(parser);
    gd.galleryInfo.titleJpn = unescape(&title_jpn).ok().map(|s| s.to_string());

    let category = dom
        .get_element_by_id("gdc")?
        .get(parser)?
        .inner_text(parser);
    gd.galleryInfo.category = to_category_i32(&category.to_lowercase());

    gd.galleryInfo.uploader = dom.get_element_by_id("gdn").and_then(|n| {
        let gdn = n.get(parser)?.as_tag()?;
        gd.galleryInfo.disowned =
            get_tag_attr(gdn, "style").is_some_and(|s| s.contains("opacity:0.5"));
        let raw = get_first_child(gdn, parser)
            .unwrap_or(gdn)
            .inner_text(parser);
        unescape(&raw).ok().map(|s| s.to_string())
    });

    dom.get_element_by_id("gdd")?
        .get(parser)?
        .as_tag()?
        .query_selector(parser, "tr")?
        .for_each(|node| {
            parse_detail_info(gd, &node, parser);
        });

    gd.ratingCount = dom
        .get_element_by_id("rating_count")?
        .get(parser)?
        .inner_text(parser)
        .parse()
        .ok()?;

    let fav = dom.get_element_by_id("fav")?.get(parser)?.as_tag()?;
    gd.galleryInfo.favoriteSlot = get_first_child(fav, parser)
        .and_then(|div| {
            gd.galleryInfo.favoriteName = get_tag_attr(div, "title")
                .and_then(|s| unescape(s).ok())
                .map(|s| s.to_string());
            let fav_slot_regex = regex!(r#"\D+\d+\D+(\d+)"#);
            fav_slot_regex
                .captures(get_tag_attr(div, "style")?)
                .map(|caps| (caps[1].parse().unwrap_or(2) - 2) / 19)
        })
        .unwrap_or(-2);

    gd.newerVersions = dom
        .get_element_by_id("gnd")
        .and_then(|n| n.get(parser))
        .map(|gnd_node| {
            let newer_versions_regex = regex!(r#"<a href="([^"]+)">([^<]+)</a>, added ([^<]+)<"#);
            newer_versions_regex
                .captures_iter(&gnd_node.inner_html(parser))
                .filter_map(|caps| {
                    let (gid, token) = parse_token_and_gid(&caps[1])?;
                    Some(BaseGalleryInfo {
                        gid,
                        token,
                        title: unescape(&caps[2]).ok()?.to_string(),
                        posted: caps[3].to_string(),
                        ..Default::default()
                    })
                })
                .collect()
        })
        .unwrap_or_default();

    Some(())
}

pub fn parse_gallery_detail(dom: &mut VDom, body: &str) -> Result<GalleryDetail> {
    let parser = dom.parser();
    let error = get_vdom_first_element_by_class_name(dom, "d");
    if let Some(tag) = error.and_then(|n| get_first_child(n.as_tag()?, parser)) {
        let node = tag.children().top()[0].get(parser).unwrap();
        bail!(EhError::Error(node.inner_text(parser).to_string()));
    }

    let mut gallery_detail = GalleryDetail {
        tagGroups: parse_tag_groups(dom, parser, false)?,
        previewList: parse_preview_list(dom, parser)?,
        comments: parse_comments(dom)?,
        ..Default::default()
    };

    parse_detail(&mut gallery_detail, dom, dom.parser(), body)
        .context("Failed to parse gallery detail")?;

    Ok(gallery_detail)
}

pub fn parse_event_pane(dom: &VDom, parser: &Parser) -> Option<String> {
    dom.get_element_by_id("eventpane")?
        .get(parser)
        .map(|n| n.inner_html(parser).to_string())
}

pub fn parse_tag_groups(
    dom: &VDom,
    parser: &Parser,
    is_root: bool,
) -> Result<Vec<GalleryTagGroup>> {
    let nodes: Vec<_> = if is_root {
        dom.query_selector("tr").unwrap().collect()
    } else {
        dom.get_element_by_id("taglist")
            .and_then(|n| n.get(parser)?.as_tag())
            .context("Failed to find taglist")?
            .query_selector(parser, "tr")
            .unwrap()
            .collect()
    };
    nodes
        .iter()
        .filter_map(|n| n.get(parser)?.children())
        .map(|children| {
            let mut iter = children
                .top()
                .iter()
                .filter_map(|n| n.get(parser)?.as_tag());

            let namespace = iter
                .next()?
                .inner_text(parser)
                .strip_suffix(':')?
                .to_string();
            let tags = iter
                .next()?
                .children()
                .top()
                .iter()
                .filter_map(|n| n.get(parser)?.as_tag())
                .map(|tag| {
                    let text = unescape(&tag.inner_text(parser))
                        .ok()?
                        .split('|')
                        .next()?
                        .trim()
                        .to_string();
                    let power = match tag.attributes().class()?.try_as_utf8_str()? {
                        "gtw" => PowerStatus::Weak,
                        "gtl" => PowerStatus::Active,
                        "gt" => PowerStatus::Solid,
                        _ => return None,
                    };
                    let vote = match get_first_child(tag, parser)?
                        .attributes()
                        .class()
                        .and_then(Bytes::try_as_utf8_str)
                    {
                        Some("tup") => VoteStatus::Up,
                        Some("tdn") => VoteStatus::Down,
                        _ => VoteStatus::None,
                    };
                    Some(GalleryTag { text, power, vote })
                })
                .collect::<Option<Vec<_>>>()?;

            Some(GalleryTagGroup { namespace, tags })
        })
        .collect::<Option<Vec<_>>>()
        .context("Failed to parse tag groups")
}

fn parse_comment_time(str: &str, prefix: &str) -> Option<i64> {
    str.strip_prefix(prefix)
        .and_then(|s| NaiveDateTime::parse_and_remainder(s, "%d %B %Y, %R").ok())
        .map(|(dt, _)| dt.and_utc().timestamp_millis())
}

pub fn parse_comments(dom: &mut VDom) -> Result<GalleryCommentList> {
    let parser = dom.parser();
    let cdiv = dom
        .get_element_by_id("cdiv")
        .and_then(|n| n.get(parser)?.as_tag())
        .context("Failed to find cdiv")?;

    // Mimic "#chd + p" as tl does not support selecting siblings
    let chd = dom.get_element_by_id("chd").context("Failed to find chd")?;
    let children = cdiv.children();
    let slice = children.top().as_slice();
    let i = slice
        .iter()
        .rposition(|n| *n == chd)
        .context("chd is not a direct child of cdiv")?;
    if let Some(tag) = slice
        .iter()
        .skip(i + 1)
        .find_map(|n| n.get(parser)?.as_tag())
        && tag.name() == "p"
    {
        bail!(EhError::Error(tag.inner_text(parser).to_string()));
    };

    let has_more = chd
        .get(parser)
        .and_then(|n| get_first_child(n.as_tag()?, parser))
        .is_some_and(|tag| tag.attributes().id() != Some(&"postnewcomment".into()));

    let comment_nodes = cdiv
        .query_selector(parser, ".c1")
        .unwrap()
        .collect::<Vec<_>>();
    let comments = comment_nodes
        .iter()
        .map(|node_handle| {
            let parser = dom.parser_mut();
            let node = node_handle.get(parser).unwrap();
            let mut c = GalleryComment::default();

            let c3 = get_first_element_by_class_name(node, parser, "c3")
                .and_then(Node::as_tag)
                .context("Failed to find c3")?;
            let posted_node = c3.children().top()[0].get(parser).unwrap();
            c.time = parse_comment_time(&posted_node.inner_text(parser), "Posted on ")
                .context("Failed to parse posted date")?;
            c.user = get_first_child(c3, parser)
                .map(|n| unescape(&n.inner_text(parser)).map(|s| s.to_string()))
                .transpose()?;

            if let Some(c4) =
                get_first_element_by_class_name(node, parser, "c4").and_then(Node::as_tag)
            {
                c4.children()
                    .top()
                    .iter()
                    .filter_map(|n| n.get(parser)?.as_tag())
                    .for_each(|tag| match tag.inner_text(parser).as_ref() {
                        "Vote+" => {
                            c.voteUpAble = true;
                            c.voteUpEd = get_tag_attr(tag, "style").is_some_and(|s| !s.is_empty());
                        }
                        "Vote-" => {
                            c.voteDownAble = true;
                            c.voteDownEd =
                                get_tag_attr(tag, "style").is_some_and(|s| !s.is_empty());
                        }
                        "Edit" => c.editable = true,
                        _ => {}
                    })
            };

            c.score = get_first_element_by_class_name(node, parser, "c5")
                .and_then(|c5| {
                    get_first_child(c5.as_tag()?, parser)?
                        .inner_text(parser)
                        .parse()
                        .ok()
                })
                .unwrap_or(0);

            c.voteState = get_first_element_by_class_name(node, parser, "c7")
                .map(|c7| unescape(&c7.inner_text(parser)).map(|s| s.to_string()))
                .transpose()?;

            c.lastEdited = get_first_element_by_class_name(node, parser, "c8")
                .and_then(|c8| parse_comment_time(&c8.inner_text(parser), "Last edited on "))
                .unwrap_or(0);

            let c6_handle = node
                .find_node(parser, &mut |n| {
                    n.as_tag()
                        .is_some_and(|tag| tag.attributes().is_class_member("c6"))
                })
                .context("Failed to find c6")?;
            let c6 = c6_handle.get(parser).and_then(Node::as_tag).unwrap();
            let nodes = c6
                .query_selector(parser, "span[style='text-decoration:underline;']")
                .unwrap()
                .collect::<Vec<_>>();
            for node in nodes {
                let tag = node.get_mut(parser).and_then(Node::as_tag_mut).unwrap();
                *tag.name_mut() = "u".into();
                tag.attributes_mut().remove("style");
            }
            let c6 = c6_handle.get(parser).and_then(Node::as_tag).unwrap();
            c.id = c6
                .attributes()
                .id()
                .and_then(|id| id.try_as_utf8_str()?.strip_prefix("comment_")?.parse().ok())
                .context("Failed to parse comment id")?;
            c.uploader = c.id == 0;
            c.comment = c6.inner_html(parser).to_string();

            Ok(c)
        })
        .collect::<Result<Vec<_>>>()?;

    Ok(GalleryCommentList {
        comments,
        hasMore: has_more,
    })
}

pub fn parse_pages(dom: &VDom, parser: &Parser) -> Result<i32> {
    dom.get_element_by_id("gdd")
        .and_then(|gdd| {
            gdd.get(parser)?
                .as_tag()?
                .query_selector(parser, "td.gdt2")?
                .find_map(|n| {
                    n.get(parser)?.children()?.top()[0]
                        .get(parser)?
                        .inner_text(parser)
                        .strip_suffix(" pages")?
                        .parse()
                        .ok()
                })
        })
        .context("Failed to parse pages")
}

pub fn parse_preview_list(dom: &VDom, parser: &Parser) -> Result<Vec<GalleryPreview>> {
    dom.get_element_by_id("gdt")
        .and_then(|n| n.get(parser)?.children())
        .context("Failed to find gdt")?
        .top()
        .iter()
        .filter_map(|n| n.get(parser)?.as_tag())
        .map(|a| {
            let href_regex = regex!(r#"https[^s#"]+(?:s/([0-9a-f]{10})/\d+-|#page)(\d+)"#);
            let caps = href_regex.captures(get_tag_attr(a, "href")?)?;
            let p_token = caps.get(1).map_or("", |m| m.as_str()).to_string();
            let position = caps[2].parse::<i32>().unwrap() - 1;

            let div = a.query_selector(parser, "div[title]")?.next()?;
            let style_regex = regex!(r#"\D+(\d+)\D+(\d+)[^(]+\(([^)]+)\)(?: -(\d+))?"#);
            let caps = style_regex.captures(get_node_handle_attr(&div, parser, "style")?)?;
            let url = caps[3].to_string();
            let mut data = PreviewData {
                url,
                position,
                pToken: p_token,
                ..Default::default()
            };
            let ty = if let Some(offset) = caps.get(4) {
                data.offsetX = offset.as_str().parse().ok();
                data.clipWidth = caps[1].parse().ok();
                data.clipHeight = caps[2].parse().ok();
                "V2"
            } else {
                "V1"
            };

            Some(GalleryPreview(ty.to_string(), data))
        })
        .collect::<Option<Vec<_>>>()
        .context("Failed to parse preview list")
}
