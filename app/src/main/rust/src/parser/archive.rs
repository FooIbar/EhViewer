use crate::parser::utils::parse_int_lenient;
use crate::{EhError, get_tag_attr, regex, select_first};
use anyhow::{Context, Result, bail};
use serde::Serialize;
use tl::{Parser, VDom};

const ERROR_NEED_HATH_CLIENT: &str =
    "You must have a H@H client assigned to your account to use this feature.";
const ERROR_INSUFFICIENT_FUNDS: &str = "You do not have enough funds to download this archive.";

#[derive(Serialize, Debug)]
#[allow(non_snake_case)]
pub struct Archive {
    pub res: String,
    pub name: String,
    pub size: String,
    pub cost: String,
    pub isHath: bool,
}

#[derive(Serialize, Debug)]
pub struct Funds {
    pub gp: i32,
    pub credit: i32,
}

#[derive(Serialize, Debug)]
#[allow(non_snake_case)]
pub struct ArchiveListResult {
    pub archiveList: Vec<Archive>,
    pub funds: Funds,
}

pub fn parse_archive_url(dom: &VDom, parser: &Parser, body: &str) -> Result<Option<String>> {
    if body.contains(ERROR_NEED_HATH_CLIENT) {
        bail!(EhError::NoHathClient);
    }
    if body.contains(ERROR_INSUFFICIENT_FUNDS) {
        bail!(EhError::InsufficientFunds);
    }
    let url = dom.get_element_by_id("continue").and_then(|node| {
        let tag = select_first(node.get(parser)?.as_tag()?, parser, "a")?;
        let href = get_tag_attr(tag, "href")?;
        Some(format!("{href}?start=1"))
    });
    Ok(url)
}

pub fn parse_archives(dom: &VDom, parser: &Parser, body: &str) -> Result<Vec<Archive>> {
    let mut archive_list = dom
        .get_element_by_id("db")
        .and_then(|n| select_first(n.get(parser)?.as_tag()?, parser, "div"))
        .context("Failed to parse archives")?
        .children()
        .top()
        .iter()
        .filter_map(|node_handle| {
            let tag = node_handle.get(parser)?.as_tag()?;
            if tag.name() == "div" && !get_tag_attr(tag, "style")?.contains("color:#CCCCCC") {
                let res = select_first(select_first(tag, parser, "form")?, parser, "input")?;
                let size = select_first(select_first(tag, parser, "p")?, parser, "strong")?;
                let cost = select_first(select_first(tag, parser, "div")?, parser, "strong")?;
                Some(Archive {
                    res: get_tag_attr(res, "value")?.to_string(),
                    name: "".to_string(),
                    size: size.inner_text(parser).to_string(),
                    cost: cost.inner_text(parser).replace(",", ""),
                    isHath: false,
                })
            } else {
                None
            }
        })
        .collect::<Vec<_>>();

    let hath_regex = regex!(
        r#"<p><a href="[^"]*" onclick="return do_hathdl\('([0-9]+|org)'\)">([^<]+)</a></p>\s*<p>([\w. ]+)</p>\s*<p>([\w. ]+)</p>"#
    );
    for cap in hath_regex.captures_iter(body) {
        archive_list.push(Archive {
            res: cap[1].to_string(),
            name: cap[2].to_string(),
            size: cap[3].to_string(),
            cost: cap[4].to_string(),
            isHath: true,
        });
    }

    Ok(archive_list)
}

pub fn parse_archives_with_funds(
    dom: &VDom,
    parser: &Parser,
    body: &str,
) -> Result<ArchiveListResult> {
    let archive_list = parse_archives(dom, parser, body)?;
    let cap = regex!(r#"<p>([\d,]+) GP \[[^]]*] &nbsp; ([\d,]+) Credits \[[^]]*]</p>"#)
        .captures(body)
        .context("Failed to parse funds")?;
    let funds = Funds {
        gp: parse_int_lenient(&cap[1], 0) / 1000,
        credit: parse_int_lenient(&cap[2], 0),
    };
    Ok(ArchiveListResult {
        archiveList: archive_list,
        funds,
    })
}
