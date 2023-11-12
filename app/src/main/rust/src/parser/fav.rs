use apply::Also;
use catch_panic::catch_panic;
use jni_fn::jni_fn;
use jnix::jni::objects::{JByteBuffer, JClass};
use jnix::jni::sys::{jint, jintArray, jobjectArray};
use jnix::jni::JNIEnv;
use jnix::JnixEnv;
use parse_bytebuffer;
use quick_xml::escape::unescape;

#[no_mangle]
#[catch_panic(default = "std::ptr::null_mut()")]
#[allow(non_snake_case)]
#[jni_fn("com.hippo.ehviewer.client.parser.FavoritesParserKt")]
pub fn parseFav(
    env: JNIEnv,
    _class: JClass,
    input: JByteBuffer,
    limit: jint,
    str: jobjectArray,
) -> jintArray {
    let mut env = JnixEnv { env };
    let vec = parse_bytebuffer(&mut env, input, limit, |dom, parser, env, html| {
        if html.contains("This page requires you to log on.</p>") {
            panic!("Not logged in!")
        }
        let vec: Vec<i32> = dom
            .get_elements_by_class_name("fp")
            .enumerate()
            .filter_map(|(i, e)| {
                if i == 10 {
                    return None;
                }
                let top = e.get(parser)?.children()?;
                let children = top.top();
                let cat = children[5].get(parser)?.inner_text(parser);
                let name = unescape(&cat).ok()?;
                env.set_object_array_element(str, i as i32, env.new_string(name.trim()).ok()?)
                    .ok()?;
                children[1]
                    .get(parser)?
                    .inner_text(parser)
                    .parse::<i32>()
                    .ok()
            })
            .collect();
        if vec.len() == 10 {
            Some(vec)
        } else {
            None
        }
    })
    .unwrap();
    env.new_int_array(10)
        .unwrap()
        .also(|it| env.set_int_array_region(*it, 0, &vec).unwrap())
}
