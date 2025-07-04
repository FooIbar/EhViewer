#![feature(f16)]
#![feature(never_type)]

mod ffi;
pub mod img;
pub mod parser;

use std::fmt::{Debug, Display, Formatter};
use tl::{Bytes, HTMLTag, Node, NodeHandle, Parser, VDom};

#[macro_export]
macro_rules! regex {
    ($re:literal $(,)?) => {{
        static RE: std::sync::OnceLock<regex_lite::Regex> = std::sync::OnceLock::new();
        RE.get_or_init(|| regex_lite::Regex::new($re).unwrap())
    }};
}

const EHGT_PREFIX: &str = "https://ehgt.org/";
const EX_PREFIX: &str = "https://s.exhentai.org/";

fn get_vdom_first_element_by_class_name<'a>(dom: &'a VDom, name: &str) -> Option<&'a Node<'a>> {
    let handle = dom.get_elements_by_class_name(name).next()?;
    handle.get(dom.parser())
}

fn get_first_element_by_class_name<'a>(
    node: &'a Node,
    parser: &'a Parser,
    id: &str,
) -> Option<&'a Node<'a>> {
    let handle = node.find_node(parser, &mut |n| match n.as_tag() {
        None => false,
        Some(tag) => tag.attributes().is_class_member(id),
    })?;
    handle.get(parser)
}

// tl does not support selecting child elements
// Workaround for https://github.com/y21/tl/issues/22
fn select_first<'a>(tag: &'a HTMLTag, parser: &'a Parser, id: &str) -> Option<&'a HTMLTag<'a>> {
    for n in tag.children().top().iter() {
        let tag = n.get(parser).and_then(Node::as_tag);
        if tag.is_some_and(|t| t.name() == id) {
            return tag;
        }
    }
    None
}

fn get_first_child<'a>(tag: &'a HTMLTag, parser: &'a Parser) -> Option<&'a HTMLTag<'a>> {
    tag.children()
        .top()
        .iter()
        .find_map(|n| n.get(parser)?.as_tag())
}

fn get_element_by_id<'b, S>(node: &'b Node, parser: &'b Parser, id: S) -> Option<&'b Node<'b>>
where
    S: Into<Bytes<'b>>,
{
    let bytes: Bytes = id.into();
    let handle = node.find_node(parser, &mut |n| match n.as_tag() {
        None => false,
        Some(tag) => tag.attributes().id().is_some_and(|x| x.eq(&bytes)),
    })?;
    handle.get(parser)
}

#[derive(Debug)]
enum EhError {
    NoHits,
    NoWatched,
    NeedLogin,
    NoHathClient,
    InsufficientFunds,
    Error(String),
}

impl Display for EhError {
    fn fmt(&self, f: &mut Formatter<'_>) -> std::fmt::Result {
        let msg = match self {
            EhError::NoHits => "0",
            EhError::NoWatched => "1",
            EhError::NeedLogin => "2",
            EhError::NoHathClient => "3",
            EhError::InsufficientFunds => "4",
            EhError::Error(s) => &format!("5{s}"),
        };
        f.write_str(msg)
    }
}

fn get_node_handle_attr<'a>(
    node: &NodeHandle,
    parser: &'a Parser,
    attr: &'a str,
) -> Option<&'a str> {
    get_node_attr(node.get(parser)?, attr)
}

fn get_node_attr<'a>(node: &'a Node<'_>, attr: &'a str) -> Option<&'a str> {
    get_tag_attr(node.as_tag()?, attr)
}

fn get_tag_attr<'a>(tag: &'a HTMLTag<'_>, attr: &'a str) -> Option<&'a str> {
    tag.attributes().get(attr)??.try_as_utf8_str()
}

// Should not use it at too upper node, since it will do DFS?
fn query_childs_first_match_attr<'a>(
    node: &'a Node<'_>,
    parser: &'a Parser,
    attr: &'a str,
) -> Option<&'a str> {
    let selector = format!("[{attr}]");
    let mut iter = node.as_tag()?.query_selector(parser, &selector)?;
    get_node_handle_attr(&iter.next()?, parser, attr)
}
