use quick_xml::escape::{EscapeError, resolve_xml_entity, unescape_with};
use std::borrow::Cow;

pub fn parse_int_lenient(s: &str, default: i32) -> i32 {
    s.replace(",", "").parse().unwrap_or(default)
}

pub fn unescape(raw: &str) -> Result<Cow<'_, str>, EscapeError> {
    unescape_with(raw, resolve_predefined_entity)
}

#[inline]
const fn resolve_predefined_entity(entity: &str) -> Option<&'static str> {
    match entity.as_bytes() {
        b"nbsp" => Some(" "),
        b"times" => Some("×"),
        _ => resolve_xml_entity(entity),
    }
}
