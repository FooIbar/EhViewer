use crate::{EhError, get_node_handle_attr, get_tag_attr, select_first};
use anyhow::{Context, Result};
use quick_xml::escape::unescape;
use serde::Serialize;
use tl::{Parser, VDom};

const HOST_FORUMS: &str = "https://forums.e-hentai.org";

#[derive(Serialize, Debug)]
#[allow(non_snake_case)]
pub struct Profile {
    pub displayName: String,
    pub avatar: Option<String>,
}

pub fn parse_profile_url(dom: &VDom, parser: &Parser) -> Result<String> {
    dom.get_element_by_id("userlinks")
        .and_then(|n| {
            let mut iter = n.get(parser)?.as_tag()?.query_selector(parser, "a")?;
            get_node_handle_attr(&iter.next()?, parser, "href").map(str::to_string)
        })
        .context(EhError::NeedLogin)
}

pub fn parse_profile(dom: &VDom, parser: &Parser) -> Result<Profile> {
    dom.get_elements_by_class_name("row1")
        .next()
        .and_then(|n| {
            let mut iter = n.get(parser)?.as_tag()?.query_selector(parser, "div")?;
            let str = iter.next()?.get(parser)?.inner_text(parser);
            let display_name = unescape(&str).as_deref().unwrap_or(&str).to_string();
            let avatar = iter.next().and_then(|n| {
                let img = select_first(n.get(parser)?.as_tag()?, parser, "img")?;
                get_tag_attr(img, "src").map(|src| {
                    if src.starts_with("http") {
                        src.to_string()
                    } else {
                        format!("{HOST_FORUMS}{src}")
                    }
                })
            });
            Some(Profile {
                displayName: display_name,
                avatar,
            })
        })
        .context("Failed to parse profile")
}
