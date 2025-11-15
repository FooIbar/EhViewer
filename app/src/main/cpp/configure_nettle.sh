SUFFIX=toolchains/llvm/prebuilt/$(uname -s | tr '[:upper:]' '[:lower:]')-x86_64/bin
CI_NDK_LOCATION=$ANDROID_HOME/ndk/$1/$SUFFIX
ARCH_NDK_LOCATION=$ANDROID_HOME/ndk-bundle/$SUFFIX
if test -e $CI_NDK_LOCATION ;then
    TOOLCHAIN=$CI_NDK_LOCATION
else
    TOOLCHAIN=$ARCH_NDK_LOCATION
fi
TARGET=$2
./configure --host=$TARGET --disable-public-key --disable-dependency-tracking \
  AR=$TOOLCHAIN/llvm-ar \
  CC="$TOOLCHAIN/clang -target $TARGET" \
  RANLIB=$TOOLCHAIN/llvm-ranlib
