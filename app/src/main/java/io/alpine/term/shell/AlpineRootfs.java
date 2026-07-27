package io.alpine.term.shell;

import android.content.Context;
import android.content.res.AssetManager;
import android.os.Build;
import android.system.Os;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public final class AlpineRootfs {
    public static final String ARCH = arch();
    public static final String VERSION = "v4.37.0";
    public static final String ASSET_PROOT_DIR = "proot-" + ARCH;
    public static final String ASSET_ALPINE_DIR = "alpine-" + ARCH + "-pd-" + VERSION + "/alpine-" + ARCH;
    public static final String MARKER_NAME = ".alpine-installed-v1";

    public static File prefix(Context ctx) {
        return new File(ctx.getFilesDir().getParentFile(), "files");
    }

    public static File rootfsDir(Context ctx) {
        return new File(prefix(ctx), "var/lib/proot-distro/installed-rootfs/alpine");
    }

    public static File prootBin(Context ctx) {
        return new File(prefix(ctx), "usr/bin/proot");
    }

    public static void installIfNeeded(Context ctx) throws IOException {
        File markerFile = new File(prefix(ctx), MARKER_NAME);
        if (markerFile.exists()) return;

        File usrBin = new File(prefix(ctx), "usr/bin");
        File usrTmp = new File(prefix(ctx), "usr/tmp");
        File rootfs = rootfsDir(ctx);
        usrBin.mkdirs();
        usrTmp.mkdirs();
        rootfs.mkdirs();

        AssetManager am = ctx.getAssets();
        installProot(am, ctx);
        installAlpineTree(am, ctx);
        markerFile.createNewFile();
    }

    private static void installProot(AssetManager am, Context ctx) throws IOException {
        File out = prootBin(ctx);
        try (InputStream in = am.open(ASSET_PROOT_DIR + "/bin/proot");
             OutputStream os = new FileOutputStream(out)) {
            copyStream(in, os);
        }
        try {
            Os.chmod(out.getAbsolutePath(), 0755);
        } catch (Exception e) {
            new File(out.getParent()).setExecutable(true, false);
        }
    }

    private static void installAlpineTree(AssetManager am, Context ctx) throws IOException {
        File rootfs = rootfsDir(ctx);
        copyAssetDir(am, ASSET_ALPINE_DIR, rootfs);
    }

    private static void copyAssetDir(AssetManager am, String from, File to) throws IOException {
        to.mkdirs();
        for (String child : am.list(from)) {
            String src = from + "/" + child;
            File dst = new File(to, child);
            if (am.list(src).length == 0) {
                try (InputStream in = am.open(src);
                     OutputStream os = new FileOutputStream(dst)) {
                    copyStream(in, os);
                }
            } else {
                copyAssetDir(am, src, dst);
            }
        }
    }

    private static void copyStream(InputStream in, OutputStream out) throws IOException {
        byte[] buf = new byte[8192];
        int n;
        while ((n = in.read(buf)) > 0) out.write(buf, 0, n);
    }

    private static String arch() {
        String abi = Build.SUPPORTED_ABIS[0];
        switch (abi) {
            case "arm64-v8a": return "aarch64";
            case "armeabi-v7a": return "arm";
            case "x86_64": return "x86_64";
            case "x86": return "i686";
            default: return abi;
        }
    }
}
