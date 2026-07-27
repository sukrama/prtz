#!/usr/bin/env bash
set -euo pipefail

VERSION="${VERSION:-v4.37.0}"
BASE="https://easycli.sh/proot-distro"
APP="app/src/main/assets"

declare -A ARCHES=(
  [aarch64]="arm64-v8a"
  [armv7]="armeabi-v7a"
  [x86_64]="x86_64"
)

for arch in "${!ARCHES[@]}"; do
  dir="$APP/alpine-$arch-pd-$VERSION"
  if [ -f "$dir/alpine-rootfs.tar.xz" ] && [ -f "$dir/usr/bin/proot" ]; then
    echo "skip $arch (already present)"
    continue
  fi
  rm -rf "$dir"
  mkdir -p "$dir"
  echo "fetch $arch ..."
  curl -fSL --retry 3 -o "/tmp/alpine-$arch.tar.xz" \
    "$BASE/alpine-$arch-pd-$VERSION.tar.xz"
  tar -xJf "/tmp/alpine-$arch.tar.xz" -C "$dir"
  rm "/tmp/alpine-$arch.tar.xz"
done

echo "done"
