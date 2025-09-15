use crate::parser::list::{GalleryListResult, parse_info_list};
use anyhow::{Result, bail};
use quick_xml::escape::unescape;
use serde::Serialize;
use tl::Parser;
use tl::VDom;

#[derive(Serialize)]
#[allow(non_snake_case)]
pub struct FavResult {
    catArray: Vec<String>,
    countArray: Vec<i32>,
    galleryListResult: GalleryListResult,
}

pub fn parse_fav(dom: &VDom, parser: &Parser) -> Result<FavResult> {
    let vec: Vec<(String, i32)> = dom
        .get_elements_by_class_name("fp")
        .enumerate()
        .filter_map(|(i, e)| {
            if i == 10 {
                return None;
            }
            let top = e.get(parser)?.children()?;
            let children = top.top();
            let cat = children[5].get(parser)?.inner_text(parser);
            let name = unescape(&cat).ok()?.trim().to_string();
            let count = children[1]
                .get(parser)?
                .inner_text(parser)
                .parse::<i32>()
                .ok()?;
            Some((name, count))
        })
        .collect();
    if vec.len() == 10 {
        let list = parse_info_list(dom, parser)?;
        let cat = vec.iter().cloned().unzip();
        Ok(FavResult {
            catArray: cat.0,
            countArray: cat.1,
            galleryListResult: list,
        })
    } else {
        bail!("Illegal fav cat count!")
    }
}
