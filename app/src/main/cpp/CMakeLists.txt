cmake_minimum_required(VERSION 3.14)
project(ehviewer C)
include(ExternalProject)
include(FetchContent)

if (NOT CMAKE_BUILD_TYPE STREQUAL "Debug")
    set(CMAKE_C_FLAGS "${CMAKE_C_FLAGS} -Ofast -fvisibility=hidden -fvisibility-inlines-hidden -funroll-loops -flto \
           -mllvm -polly \
           -mllvm -polly-run-dce \
           -mllvm -polly-run-inliner \
           -mllvm -polly-isl-arg=--no-schedule-serialize-sccs \
           -mllvm -polly-ast-use-context \
           -mllvm -polly-detect-keep-going \
           -mllvm -polly-position=before-vectorizer \
           -mllvm -polly-vectorizer=stripmine \
           -mllvm -polly-detect-profitability-min-per-loop-insts=40 \
           -mllvm -polly-invariant-load-hoisting")
endif (NOT CMAKE_BUILD_TYPE STREQUAL "Debug")

option(BUILD_TESTING OFF)
option(XZ_DOC OFF)
option(XZ_LZIP_DECODER OFF)
option(XZ_MICROLZMA_DECODER OFF)
option(XZ_MICROLZMA_ENCODER OFF)
option(XZ_TOOL_LZMADEC OFF)
option(XZ_TOOL_LZMAINFO OFF)
option(XZ_TOOL_XZ OFF)
option(XZ_TOOL_XZDEC OFF)
FetchContent_Declare(
        liblzma
        GIT_REPOSITORY https://github.com/tukaani-project/xz.git
        GIT_TAG v5.8.1
        GIT_SHALLOW 1
)

FetchContent_MakeAvailable(liblzma)
include_directories(${liblzma_SOURCE_DIR}/src/liblzma/api)

# Build GNUTLS libnettle
if (CMAKE_HOST_SYSTEM_NAME STREQUAL Windows)
    execute_process(
            COMMAND wsl wslpath ${CMAKE_CURRENT_LIST_DIR}
            OUTPUT_VARIABLE CURRENT_DIR
            OUTPUT_STRIP_TRAILING_WHITESPACE
    )
else ()
    set(CURRENT_DIR ${CMAKE_CURRENT_LIST_DIR})
endif ()
set(NDK_VERSION "${ANDROID_NDK_MAJOR}.${ANDROID_NDK_MINOR}.${ANDROID_NDK_BUILD}")
ExternalProject_Add(
        nettle
        PREFIX nettle
        GIT_REPOSITORY https://gitlab.com/gnutls/nettle.git
        GIT_TAG nettle_3.10.1_release_20241230
        GIT_SHALLOW 1
        GIT_CONFIG core.autocrlf=input
        CONFIGURE_COMMAND bash ${CURRENT_DIR}/configure_nettle.sh ${NDK_VERSION} ${CMAKE_C_COMPILER_TARGET}
        BUILD_COMMAND bash -c "make libnettle.a CFLAGS='${CMAKE_C_FLAGS} -Qunused-arguments'"
        INSTALL_COMMAND ""
        BUILD_IN_SOURCE 1
        BUILD_BYPRODUCTS nettle/src/nettle/libnettle.a
)

ExternalProject_Get_Property(nettle SOURCE_DIR)
include_directories(${SOURCE_DIR}/..)
add_library(libnettle STATIC IMPORTED)
set_property(TARGET libnettle PROPERTY IMPORTED_LOCATION ${SOURCE_DIR}/libnettle.a)
add_dependencies(libnettle nettle)

# Configure libnettle support for libarchive
set(HAVE_LIBNETTLE 1)
set(HAVE_NETTLE_AES_H 1)
set(HAVE_NETTLE_HMAC_H 1)
set(HAVE_NETTLE_MD5_H 1)
set(HAVE_NETTLE_PBKDF2_H 1)
set(HAVE_NETTLE_RIPEMD160_H 1)
set(HAVE_NETTLE_SHA_H 1)

# Configure lzma support for libarchive
SET(HAVE_LIBLZMA 1)
SET(HAVE_LZMA_H 1)
SET(HAVE_LZMA_STREAM_ENCODER_MT 1)
SET(HAVE_LZMADEC_H 1)
SET(HAVE_LIBLZMADEC 1)

option(ENABLE_OPENSSL OFF)
option(ENABLE_TAR OFF)
option(ENABLE_CPIO OFF)
option(ENABLE_CAT OFF)
option(ENABLE_UNZIP OFF)
option(ENABLE_TEST OFF)

# Configure libarchive link's static lib
SET(LIBARCHIVE_CUSTOM_LIBS "libnettle" "liblzma")

set(LIBARCHIVE_PATCH
        ${CMAKE_CURRENT_LIST_DIR}/0001-Insert-link-libs.patch
        ${CMAKE_CURRENT_LIST_DIR}/0002-Fix-zip_time-performance.patch
        ${CMAKE_CURRENT_LIST_DIR}/0003-Use-UTF-8-as-default-charset-on-bionic.patch
)
FetchContent_Declare(
        libarchive
        GIT_REPOSITORY https://github.com/libarchive/libarchive.git
        GIT_TAG v3.8.1
        GIT_SHALLOW 1
        PATCH_COMMAND git apply --check -R ${LIBARCHIVE_PATCH} || git apply ${LIBARCHIVE_PATCH}
)

FetchContent_MakeAvailable(libarchive)
include_directories(${libarchive_SOURCE_DIR}/libarchive)

set(LIBWEBP_PATCH ${CMAKE_CURRENT_LIST_DIR}/0001-Disable-runtime-detection-of-NEON-support.patch)
FetchContent_Declare(
        libwebp
        GIT_REPOSITORY https://chromium.googlesource.com/webm/libwebp
        GIT_TAG v1.5.0
        GIT_SHALLOW 1
        PATCH_COMMAND git apply --check -R ${LIBWEBP_PATCH} || git apply ${LIBWEBP_PATCH}
)

option(WEBP_BUILD_ANIM_UTILS OFF)
option(WEBP_BUILD_CWEBP OFF)
option(WEBP_BUILD_DWEBP OFF)
option(WEBP_BUILD_VWEBP OFF)
option(WEBP_BUILD_WEBPINFO OFF)
option(WEBP_BUILD_LIBWEBPMUX OFF)
option(WEBP_BUILD_EXTRAS OFF)
option(WEBP_ENABLE_SWAP_16BIT_CSP ON)

FetchContent_MakeAvailable(libwebp)

FetchContent_Declare(
        Corrosion
        GIT_REPOSITORY https://github.com/corrosion-rs/corrosion.git
        GIT_TAG v0.5.2
        GIT_SHALLOW 1
)

FetchContent_MakeAvailable(Corrosion)
corrosion_import_crate(
        MANIFEST_PATH ../rust/Cargo.toml
        LOCKED
        FEATURES $<IF:$<VERSION_GREATER_EQUAL:${ANDROID_NATIVE_API_LEVEL},26>,android-26,android>
        FLAGS $<$<NOT:$<CONFIG:Debug>>:-Z build-std=std,panic_abort -Z build-std-features=panic_immediate_abort>
)
corrosion_link_libraries(ehviewer_rust webp)
corrosion_add_target_local_rustflags(ehviewer_rust "-Clink-args=-Wl,-z,max-page-size=16384")

# Build and link our app's native lib
add_library(${PROJECT_NAME} SHARED archive.c gifutils.c hash.c natsort/strnatcmp.c)
target_link_libraries(${PROJECT_NAME} ehviewer_rust archive_static log jnigraphics -Wl,--exclude-libs,ALL)
