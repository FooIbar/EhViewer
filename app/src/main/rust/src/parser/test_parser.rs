#[cfg(test)]
mod tests {
    use crate::parser::list::parse_info_list;
    use reqwest::get;
    use tl::ParserOptions;

    #[tokio::test]
    async fn test_parse_info() {
        let resp = get("https://e-hentai.org/").await.expect("Failed to get!");
        let body = resp.text().await.expect("Failed to receive!");
        let dom = tl::parse(&body, ParserOptions::default()).expect("Failed to parse html");
        let result = parse_info_list(&dom, dom.parser(), &body).expect("Failed to parse info list");
        dbg!(result);
    }
}
