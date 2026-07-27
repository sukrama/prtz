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

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.tukaani.xz.XZInputStream;

public final class AlpineRootfs {
    public static final String ARCH = arch();
    public static final String VERSION = "v4.37.0";
    public static final String ASSET_DIR = "alpine-" + ARCH + "-pd-" + VERSION;
    public static final String MARKER_NAME = ".alpine-extracted-v1";

    public static File prefix(Context ctx) {
        return new File(ctx.getFilesDir().getParentFile(), "files");
    }

    public static File rootfsDir(Context ctx) {
        return new File(prefix(ctx), "var/lib/proot-distro/installed-rootfs/alpine");
    }

    public static File marker(Context ctx) {
        return new File(rootfsDir(ctx).getParentFile().getParentFile().getParentFile()
                .getParentFile().getParentFile().getParentFile().getParentFile()
                + "/" + MARKER_NAME);
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
        installAlpineRootfs(am, ctx);
        markerFile.createNewFile();
    }

    private static void installProot(AssetManager am, Context ctx) throws IOException {
        File out = prootBin(ctx);
        try (InputStream in = am.open(ASSET_DIR + "/usr/bin/proot");
             OutputStream os = new FileOutputStream(out)) {
            copyStream(in, os);
        }
        try {
            Os.chmod(out.getAbsolutePath(), 0755);
        } catch (Exception e) {
            new File(out.getParent()).setExecutable(true, false);
        }
    }

    private static void installAlpineRootfs(AssetManager am, Context ctx) throws IOException {
        File rootfs = rootfsDir(ctx);
        try (InputStream raw = am.open(ASSET_DIR + "/alpine-rootfs.tar.xz");
             XZInputStream xz = new XZInputStream(raw);
             TarArchiveInputStream tar = new TarArchiveInputStream(xz)) {
            TarArchiveEntry e;
            while ((e = tar.getNextEntry()) != null) {
                File out = new File(rootfs, e.getName());
                if (e.isDirectory()) {
                    out.mkdirs();
                    continue;
                }
                if (e.isSymbolicLink()) {
                    out.getParentFile().mkdirs();
                    if (out.exists()) out.delete();
                    Os.symlink(e.getLinkName(), out.getAbsolutePath());
                    continue;
                }
                File parent = out.getParentFile();
                if (parent != null) parent.mkdirs();
                try (OutputStream os = new FileOutputStream(out)) {
                    copyStream(tar, os);
                }
                if ((e.getMode() & 0100) != 0) {
                    try { Os.chmod(out.getAbsolutePath(), e.getMode() & 0777); } catch (Exception ignored) {}
                }
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
            case "armeabi-v7a": return "armv7";
            case "x86_64": return "x86_64";
            default: return abi;
        }
    }
}
