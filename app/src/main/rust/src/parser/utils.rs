pub fn parse_int_lenient(s: &str, default: i32) -> i32 {
    s.replace(",", "").parse().unwrap_or(default)
}
