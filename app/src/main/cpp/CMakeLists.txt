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

FetchContent_Declare(
        liblzma
        GIT_REPOSITORY https://github.com/tukaani-project/xz.git
        GIT_TAG v5.6.2
        PATCH_COMMAND git apply ${CMAKE_CURRENT_LIST_DIR}/0001-Don-t-build-other-binaries.patch || cd .
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
set(NETTLE_DIR ${CMAKE_CURRENT_BINARY_DIR}/nettle-src/nettle)
ExternalProject_Add(
        nettle
        URL https://ftp.gnu.org/gnu/nettle/nettle-3.9.1.tar.gz
        URL_MD5 29fcd2dec6bf5b48e5e3ffb3cbc4779e
        SOURCE_DIR ${NETTLE_DIR}
        PATCH_COMMAND bash -c "patch -p1 < ${CURRENT_DIR}/nettle.patch"
        CONFIGURE_COMMAND bash ${CURRENT_DIR}/configure_nettle.sh ${NDK_VERSION} ${CMAKE_C_COMPILER_TARGET}
        BUILD_COMMAND bash -c "make libnettle.a CFLAGS='${CMAKE_C_FLAGS}'"
        INSTALL_COMMAND ""
        BUILD_IN_SOURCE TRUE
        BUILD_BYPRODUCTS ${NETTLE_DIR}/libnettle.a
)

add_library(libnettle STATIC IMPORTED)
set_property(TARGET libnettle PROPERTY IMPORTED_LOCATION ${NETTLE_DIR}/libnettle.a)
add_dependencies(libnettle nettle)

# Configure libnettle support for libarchive
include_directories(${NETTLE_DIR}/..)
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

option(ENABLE_TAR OFF)
option(ENABLE_CPIO OFF)
option(ENABLE_CAT OFF)
option(ENABLE_UNZIP OFF)
option(ENABLE_TEST OFF)

# Configure libarchive link's static lib
SET(LIBARCHIVE_CUSTOM_LIBS "libnettle" "liblzma")

FetchContent_Declare(
        libarchive
        GIT_REPOSITORY https://github.com/libarchive/libarchive.git
        GIT_TAG v3.7.4
        PATCH_COMMAND git apply ${CMAKE_CURRENT_LIST_DIR}/0001-Hide-symbols.patch ${CMAKE_CURRENT_LIST_DIR}/0002-Insert-link-libs.patch || cd .
)

FetchContent_MakeAvailable(libarchive)
include_directories(${libarchive_SOURCE_DIR}/libarchive)

FetchContent_Declare(
        Corrosion
        GIT_REPOSITORY https://github.com/corrosion-rs/corrosion.git
        GIT_TAG v0.5
)

FetchContent_MakeAvailable(Corrosion)
corrosion_import_crate(MANIFEST_PATH ../rust/Cargo.toml)
corrosion_add_target_local_rustflags(ehviewer_rust "-Clink-args=-Wl,-z,max-page-size=16384")

# Build and link our app's native lib
add_library(${PROJECT_NAME} SHARED archive.c border.c gifutils.c natsort/strnatcmp.c)
target_link_libraries(${PROJECT_NAME} ehviewer_rust archive_static log jnigraphics)
