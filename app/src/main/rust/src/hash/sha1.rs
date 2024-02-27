use anyhow::{Ok, Result};
use base16ct::lower::encode_str;
use jni::objects::JClass;
use jni::sys::{jint, jstring};
use jni::JNIEnv;
use jni_fn::jni_fn;
use jni_throwing;
use sha1::{Digest, Sha1};
use std::mem::forget;
use std::os::fd::FromRawFd;
use std::{fs::File, io::copy};

fn file_sha1<'a>(file: &mut File, digest: &'a mut [u8; 40]) -> Result<&'a str> {
    let mut sha = Sha1::new();
    copy(file, &mut sha)?;
    Ok(encode_str(&sha.finalize(), digest)?)
}

#[no_mangle]
#[allow(non_snake_case)]
#[jni_fn("com.hippo.ehviewer.jni.HashKt")]
pub fn sha1(mut env: JNIEnv, _class: JClass, fd: jint) -> jstring {
    jni_throwing(&mut env, |env| {
        let mut hex_digest = [0u8; 40];
        let mut file = unsafe { File::from_raw_fd(fd) };
        let hex = file_sha1(&mut file, &mut hex_digest);
        // Avoid double close, we close it on Java side
        forget(file);
        Ok(env.new_string(hex?)?.into_raw())
    })
}
