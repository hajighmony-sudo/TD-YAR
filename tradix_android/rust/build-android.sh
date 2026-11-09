#!/usr/bin/env bash
# Builds libtradenex_core.so for Android and copies it into app/src/main/jniLibs.
# Requires: rustup, cargo-ndk (cargo install cargo-ndk), Android NDK (ANDROID_NDK_HOME).
set -euo pipefail
cd "$(dirname "$0")"
rustup target add aarch64-linux-android armv7-linux-androideabi x86_64-linux-android
cargo ndk -t arm64-v8a -t armeabi-v7a -t x86_64 -o ../app/src/main/jniLibs build --release
echo "Done: app/src/main/jniLibs/*/libtradenex_core.so"
