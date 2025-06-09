#[cfg(test)]
mod tests {
    use crate::parser::archive::{parse_archive_url, parse_archives_with_funds};
    use crate::parser::detail::parse_gallery_detail;
    use crate::parser::list::parse_info_list;
    use crate::parser::profile::{parse_profile, parse_profile_url};
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

    #[tokio::test]
    async fn test_parse_profile() {
        let body =
            fs::read_to_string("test_data/profile_url.html").expect("Failed to read html file");
        let dom = tl::parse(&body, ParserOptions::default()).expect("Failed to parse html");
        let result = parse_profile_url(&dom, dom.parser()).expect("Failed to parse profile url");
        let resp = get(result).await.expect("Failed to get!");
        let body = resp.text().await.expect("Failed to receive!");
        let dom = tl::parse(&body, ParserOptions::default()).expect("Failed to parse html");
        let result = parse_profile(&dom, dom.parser()).expect("Failed to parse profile");
        assert_eq!(result.displayName, "Tenboro".to_string());
        assert_eq!(
            result.avatar,
            Some("https://forums.e-hentai.org/ehgt/jdk_180.png".to_string())
        );
    }

    #[tokio::test]
    async fn test_parse_gallery_detail() {
        let resp = get("https://e-hentai.org/g/530350/8b3c7e4a21/")
            .await
            .expect("Failed to get!");
        let body = resp.text().await.expect("Failed to receive!");
        let mut dom =
            tl::parse(&body, ParserOptions::default().track_ids()).expect("Failed to parse HTML");
        let result = parse_gallery_detail(&mut dom, &body).expect("Failed to parse gallery detail");
        dbg!(result);
    }
}
