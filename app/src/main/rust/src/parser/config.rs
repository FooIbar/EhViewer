use crate::{parse_marshal_inplace, regex};
use jni::objects::{JByteBuffer, JClass};
use jni::sys::jint;
use jni::JNIEnv;
use jni_fn::jni_fn;
use quick_xml::escape::unescape;

#[no_mangle]
#[allow(non_snake_case)]
#[jni_fn("com.hippo.ehviewer.client.parser.UserConfigParser")]
pub fn parseFavCat(mut env: JNIEnv, _class: JClass, buffer: JByteBuffer, limit: jint) -> jint {
    parse_marshal_inplace(&mut env, buffer, limit, |_, body| {
        let cat = regex!("<input type=\"text\" name=\"favorite_\\d\" value=\"([^\"]+)\"")
            .captures_iter(body)
            .filter_map(|c| unescape(&c[1]).map(|s| s.to_string()).ok())
            .collect::<Vec<_>>();
        Ok(cat)
    })
}
