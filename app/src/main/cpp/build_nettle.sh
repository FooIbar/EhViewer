TOOLCHAIN=$ANDROID_HOME/ndk/$2/toolchains/llvm/prebuilt/$(uname -s | tr '[:upper:]' '[:lower:]')-x86_64/bin
TARGET=$3
patch -p1 < $1 \
  && ./configure --host=$TARGET --disable-public-key --disable-dependency-tracking \
  AR=$TOOLCHAIN/llvm-ar \
  CC="$TOOLCHAIN/clang -target $TARGET" \
  RANLIB=$TOOLCHAIN/llvm-ranlib \
  && make libnettle.a CFLAGS="$4"
