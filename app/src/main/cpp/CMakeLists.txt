cmake_minimum_required(VERSION 3.22)
project(gravocodecs C)

set(LAME_DIR ${CMAKE_CURRENT_SOURCE_DIR}/third_party/lame)
set(FLAC_DIR ${CMAKE_CURRENT_SOURCE_DIR}/third_party/flac)

# ---- libmp3lame (LGPL, se enlaza como .so separado) ----
file(GLOB LAME_SOURCES ${LAME_DIR}/libmp3lame/*.c)
# mpglib_interface es el decoder (HAVE_MPGLIB); no lo usamos
list(REMOVE_ITEM LAME_SOURCES ${LAME_DIR}/libmp3lame/mpglib_interface.c)

add_library(mp3lame SHARED ${LAME_SOURCES})
target_include_directories(mp3lame PUBLIC ${LAME_DIR}/include PRIVATE ${LAME_DIR}/libmp3lame)
target_compile_definitions(mp3lame PRIVATE STDC_HEADERS HAVE_STDINT_H HAVE_MEMCPY HAVE_ERRNO_H
    ieee754_float32_t=float)
target_compile_options(mp3lame PRIVATE -O3 -fno-strict-aliasing -Wno-everything)

# ---- libFLAC (BSD) ----
file(GLOB FLAC_SOURCES ${FLAC_DIR}/src/*.c)
add_library(flacenc STATIC ${FLAC_SOURCES})
target_include_directories(flacenc
    PUBLIC ${FLAC_DIR}/include
    PRIVATE ${FLAC_DIR}/src/include)
target_compile_definitions(flacenc PRIVATE
    PACKAGE_VERSION="1.4.3"
    FLAC__HAS_OGG=0
    HAVE_LROUND
    HAVE_STDINT_H
    HAVE_SYS_PARAM_H
    NDEBUG)
target_compile_options(flacenc PRIVATE -O3 -fno-strict-aliasing -Wno-everything)

# ---- JNI ----
add_library(gravocodecs SHARED gravocodecs.c)
target_link_libraries(gravocodecs mp3lame flacenc log m)
