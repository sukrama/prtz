#!/usr/bin/env bash
set -euo pipefail

VERSION="${VERSION:-v4.37.0}"
BASE_DISTRO="https://easycli.sh/proot-distro"
BASE_PKG="https://packages.termux.dev/apt/termux-main/pool/main/p/proot"
PROOT_DEB_VERSION="5.1.107.87"
APP="app/src/main/assets"

declare -A ARCHES=(
  [aarch64]="aarch64:arm64-v8a"
  [arm]="arm:armeabi-v7a"
  [x86_64]="x86_64:x86_64"
  [i686]="i686:x86"
)

# 1. proot ELF per arch (extracted from .deb -> data.tar.zst -> ./usr/bin/proot)
echo "=== proot ELF ==="
for uarch in "${!ARCHES[@]}"; do
  IFS=: read -r debarch abi <<< "${ARCHES[$uarch]}"
  out="$APP/proot-$uarch"
  if [ -f "$out/bin/proot" ]; then
    echo "skip proot/$uarch"
    continue
  fi
  rm -rf "$out"
  mkdir -p "$out"
  work="$(mktemp -d)"
  echo "fetch proot $uarch from packages.termux.dev ..."
  curl -fSL --retry 3 -o "$work/proot.deb" \
    "$BASE_PKG/proot_${PROOT_DEB_VERSION}_${debarch}.deb"
  cd "$work"
  ar x "proot.deb"
  # data.tar is zst or gz; pick whichever exists
  if [ -f data.tar.zst ]; then
    tar --use-compress-program=unzstd -xf data.tar.zst
  elif [ -f data.tar.gz ]; then
    tar -xzf data.tar.gz
  elif [ -f data.tar.xz ]; then
    tar -xJf data.tar.xz
  else
    echo "no data.tar.* in $uarch deb" >&2
    exit 1
  fi
  mkdir -p "$APP/proot-$uarch/bin"
  cp -v usr/bin/proot "$APP/proot-$uarch/bin/proot"
  chmod 0755 "$APP/proot-$uarch/bin/proot"
  cd /tmp && rm -rf "$work"
done

# 2. Alpine rootfs per arch (proot-distro format -> top-level alpine-<arch>/...)
echo "=== alpine rootfs ==="
for uarch in "${!ARCHES[@]}"; do
  dir="$APP/alpine-$uarch-pd-$VERSION"
  if [ -d "$dir/alpine-$uarch" ]; then
    echo "skip alpine/$uarch"
    continue
  fi
  rm -rf "$dir"
  mkdir -p "$dir"
  echo "fetch alpine $uarch ..."
  curl -fSL --retry 3 -o "/tmp/alpine-$uarch.tar.xz" \
    "$BASE_DISTRO/alpine-$uarch-pd-$VERSION.tar.xz"
  tar -xJf "/tmp/alpine-$uarch.tar.xz" -C "$dir"
  rm "/tmp/alpine-$uarch.tar.xz"
done

echo "done"
