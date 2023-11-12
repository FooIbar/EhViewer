use catch_panic::catch_panic;
use jni_fn::jni_fn;
use jnix::jni::objects::{JByteBuffer, JClass};
use jnix::jni::sys::{jint, jobject};
use jnix::jni::JNIEnv;
use jnix::{IntoJava, JnixEnv};
use jnix_macros::IntoJava;
use {get_vdom_first_element_by_class_name, parse_bytebuffer};

#[derive(Default, IntoJava)]
#[allow(non_snake_case)]
#[jnix(package = "com.hippo.ehviewer.client.parser")]
pub struct Limits {
    current: i32,
    maximum: i32,
    resetCost: i32,
}

#[no_mangle]
#[catch_panic(default = "std::ptr::null_mut()")]
#[allow(non_snake_case)]
#[jni_fn("com.hippo.ehviewer.client.parser.HomeParserKt")]
pub fn parseLimit(env: JNIEnv, _class: JClass, input: JByteBuffer, limit: jint) -> jobject {
    let mut env = JnixEnv { env };
    parse_bytebuffer(&mut env, input, limit, |dom, parser, _, _| {
        let iter = get_vdom_first_element_by_class_name(dom, "homebox")?
            .as_tag()?
            .query_selector(parser, "strong")?;
        let vec: Vec<i32> = iter
            .filter_map(|e| e.get(parser)?.inner_text(parser).parse::<i32>().ok())
            .collect();
        Some(Limits {
            current: vec[0],
            maximum: vec[1],
            resetCost: vec[2],
        })
    })
    .unwrap()
    .into_java(&env)
    .forget()
    .into_raw()
}
