use crate::EhError;
use crate::parser::detail::{GalleryTagGroup, parse_tag_groups};
use anyhow::{Context, Result, bail};
use serde_json::Value;
use tl::ParserOptions;

pub fn parse_vote_tag(str: &str) -> Result<Vec<GalleryTagGroup>> {
    let result = serde_json::from_str::<Value>(str)?;
    if let Some(error) = result.get("error").and_then(Value::as_str) {
        bail!(EhError::Error(error.to_string()))
    };
    let tagpane = result
        .get("tagpane")
        .and_then(Value::as_str)
        .context("Failed to get tagpane")?;
    let dom = tl::parse(tagpane, ParserOptions::default())?;
    parse_tag_groups(&dom, dom.parser(), true)
}
