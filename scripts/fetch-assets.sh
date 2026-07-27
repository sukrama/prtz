#!/usr/bin/env bash
set -euo pipefail

VERSION="${VERSION:-v4.37.0}"
BASE_DISTRO="https://easycli.sh/proot-distro"
BASE_PKG="https://packages.termux.dev/apt/termux-main/pool/main/p/proot"
PROOT_DEB_VERSION="5.1.107.87"
APP="app/src/main/assets"

# uarch_in_tarball : deb_arch : android_abi
declare -a ROWS=(
  "aarch64:aarch64:arm64-v8a"
  "arm:arm:armeabi-v7a"
  "x86_64:x86_64:x86_64"
  "i686:i686:x86"
)

extract_deb() {
  local work="$1" deb="$2"
  cd "$work"
  if command -v dpkg-deb >/dev/null 2>&1; then
    dpkg-deb -x "$deb" "$work/extract"
  else
    ar x "$deb"
    if [ -f data.tar.zst ]; then
      tar --use-compress-program=unzstd -xf data.tar.zst -C "$work/extract"
    elif [ -f data.tar.gz ]; then
      tar -xzf data.tar.gz -C "$work/extract"
    elif [ -f data.tar.xz ]; then
      tar -xJf data.tar.xz -C "$work/extract"
    else
      echo "no data.tar.* in $deb" >&2
      exit 1
    fi
  fi
}

echo "=== proot ELF ==="
for row in "${ROWS[@]}"; do
  IFS=: read -r uarch debarch abi <<< "$row"
  out="$APP/proot-$uarch"
  if [ -x "$out/bin/proot" ]; then
    echo "skip proot/$uarch"
    continue
  fi
  rm -rf "$out"
  mkdir -p "$out/bin" "$out/extract"
  work="$(mktemp -d)"
  echo "fetch proot $uarch ..."
  curl -fSL --retry 3 -o "$work/proot.deb" \
    "$BASE_PKG/proot_${PROOT_DEB_VERSION}_${debarch}.deb"
  extract_deb "$work" "$work/proot.deb"
  if [ ! -f "$work/extract/usr/bin/proot" ]; then
    echo "proot ELF missing for $uarch" >&2
    find "$work/extract" -name proot >&2 || true
    exit 1
  fi
  cp -v "$work/extract/usr/bin/proot" "$out/bin/proot"
  chmod 0755 "$out/bin/proot"
  cd /tmp && rm -rf "$work"
done

echo "=== alpine rootfs ==="
for row in "${ROWS[@]}"; do
  IFS=: read -r uarch debarch abi <<< "$row"
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
