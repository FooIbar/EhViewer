use crate::regex;
use quick_xml::escape::unescape;

#[allow(dead_code)]
pub fn parse_fav_cat(html: &str) -> Vec<String> {
    regex!("<input type=\"text\" name=\"favorite_\\d\" value=\"([^\"]+)\"")
        .captures_iter(html)
        .filter_map(|c| unescape(&c[1]).map(|s| s.to_string()).ok())
        .collect()
}
