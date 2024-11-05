use crate::get_vdom_first_element_by_class_name;
use serde::Serialize;
use tl::Parser;
use tl::VDom;

#[allow(non_snake_case)]
#[derive(Serialize)]
pub struct Limits {
    current: i32,
    maximum: i32,
    resetCost: i32,
}

#[allow(dead_code)]
pub fn parse_limit(dom: &VDom, parser: &Parser) -> Option<Limits> {
    let limit_box = get_vdom_first_element_by_class_name(dom, "homebox")?.as_tag()?;
    let vec = limit_box
        .query_selector(parser, "strong")?
        .filter_map(|e| {
            e.get(parser)?
                .inner_text(parser)
                .replace(",", "")
                .parse::<i32>()
                .ok()
        })
        .collect::<Vec<_>>();
    match vec.len() {
        3 => Some(Limits {
            current: vec[0],
            maximum: vec[1],
            resetCost: vec[2],
        }),
        1 => Some(Limits {
            current: 0,
            maximum: if limit_box.inner_text(parser).contains("No restrictions") {
                0
            } else {
                -1
            },
            resetCost: 0,
        }),
        _ => None,
    }
}
