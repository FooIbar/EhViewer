#[cfg(test)]
mod tests {
    use crate::parser::archive::{parse_archive_url, parse_archives_with_funds};
    use crate::parser::list::parse_info_list;
    use reqwest::get;
    use std::fs;
    use tl::ParserOptions;

    #[tokio::test]
    async fn test_parse_info() {
        let resp = get("https://e-hentai.org/").await.expect("Failed to get!");
        let body = resp.text().await.expect("Failed to receive!");
        let dom = tl::parse(&body, ParserOptions::default()).expect("Failed to parse html");
        let result = parse_info_list(&dom, dom.parser(), &body).expect("Failed to parse info list");
        dbg!(result);
    }

    #[test]
    fn test_parse_archives_with_funds() {
        let body = fs::read_to_string("test_data/archives_with_funds.html")
            .expect("Failed to read html file");
        let dom = tl::parse(&body, ParserOptions::default()).expect("Failed to parse html");
        let result =
            parse_archives_with_funds(&dom, dom.parser(), &body).expect("Failed to parse archives");
        dbg!(result);
    }

    #[test]
    fn test_parse_archive_url() {
        let body =
            fs::read_to_string("test_data/archive_url.html").expect("Failed to read html file");
        let dom = tl::parse(&body, ParserOptions::default()).expect("Failed to parse html");
        let result =
            parse_archive_url(&dom, dom.parser(), &body).expect("Failed to parse archives");
        assert_eq!(result, Some("https://0?start=1".to_string()));
    }
}
