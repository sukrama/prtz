# prtz

Standalone Android terminal that boots straight into proot-Alpine.

Bundles nothing — on first launch the app extracts `proot` + Alpine rootfs
shipped at `https://easycli.sh/proot-distro/alpine-<arch>-pd-v4.37.0.tar.xz`
into internal storage, then `execve`s into `/bin/sh` inside the rootfs.

## build

```bash
./scripts/fetch-assets.sh   # one-time: populates app/src/main/assets/
./gradlew :app:assembleDebug
```

## license

MIT for code in `app/src/main/java/io/alpine/term/`.
`terminal-emulator/` and `terminal-view/` are Apache-2.0 from
[termux/termux-app](https://github.com/termux/termux-app).
`proot` binary + Alpine rootfs pulled at build time are GPL-2 and MIT
respectively; see upstream repositories for source.
