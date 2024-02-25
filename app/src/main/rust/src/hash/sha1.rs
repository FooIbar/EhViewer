use anyhow::Result;
use base16ct::lower::encode_str;
use jni::objects::JClass;
use jni::sys::{jint, jstring};
use jni::JNIEnv;
use jni_fn::jni_fn;
use sha1::{Digest, Sha1};
use std::{fs::File, io::copy, os::fd::BorrowedFd};
use throw_msg;

pub fn file_sha1(mut file: File, digest: &mut [u8; 40]) -> Result<&str> {
    let mut sha = Sha1::new();
    copy(&mut file, &mut sha)?;
    Ok(encode_str(&sha.finalize(), digest)?)
}

#[no_mangle]
#[allow(non_snake_case)]
#[jni_fn("com.hippo.ehviewer.jni.HashKt")]
pub fn sha1(mut env: JNIEnv, _class: JClass, fd: jint) -> jstring {
    let borrowed_fd = unsafe { BorrowedFd::borrow_raw(fd) };
    let owned_fd = match borrowed_fd.try_clone_to_owned() {
        Ok(fd) => fd,
        Err(err) => return throw_msg(&mut env, &format!("{err}")) as jstring,
    };
    let mut hex_digest = [0u8; 40];
    let hex = match file_sha1(File::from(owned_fd), &mut hex_digest) {
        Ok(str) => str,
        Err(err) => return throw_msg(&mut env, &format!("{err}")) as jstring,
    };
    match env.new_string(hex) {
        Ok(obj) => obj.into_raw(),
        Err(err) => throw_msg(&mut env, &format!("{err}")) as jstring,
    }
}
