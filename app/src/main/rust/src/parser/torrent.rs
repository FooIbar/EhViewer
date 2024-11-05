use crate::regex;
use anyhow::{Context, Result};
use quick_xml::escape::unescape;
use serde::Serialize;
use tl::{Parser, VDom};

#[derive(Serialize)]
pub struct Torrent {
    outdated: bool,
    posted: String,
    size: String,
    seeds: i32,
    peers: i32,
    downloads: i32,
    uploader: String,
    url: String,
    name: String,
}

pub fn parse_torrent_list(dom: &VDom, parser: &Parser) -> Result<Vec<Torrent>> {
    let list = dom.query_selector("table").context("No Table")?.filter_map(|e| {
        let html = e.get(parser)?.inner_html(parser);
        if html.contains("Expunged") {
            None
        } else {
            let grp = regex!("<span( style=\"color:red\")?>([0-9-]+) [0-9:]+</span>[\\s\\S]+</span> ([0-9.]+ [KMGT]iB)</td>[\\s\\S]+</span> ([0-9]+)</td>[\\s\\S]+</span> ([0-9]+)</td>[\\s\\S]+</span> ([0-9]+)</td>[\\s\\S]+</span> ([^<]+)</td>[\\s\\S]+onclick=\"document.location='([^\"]+)'[^<]+>([^<]+)</a>").captures(&html)?;
            let name = unescape(&grp[9]).ok()?;
            Some(Torrent {
                outdated: grp.get(1).is_some(),
                posted: grp[2].to_string(),
                size: grp[3].to_string(),
                seeds: grp[4].parse().ok()?,
                peers: grp[5].parse().ok()?,
                downloads: grp[6].parse().ok()?,
                uploader: grp[7].to_string(),
                url: grp[8].to_string(),
                name: name.to_string()
            })
        }
    }).collect::<Vec<Torrent>>();
    Ok(list)
}
