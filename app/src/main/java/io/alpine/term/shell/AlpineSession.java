package io.alpine.term.shell;

import android.content.Context;

import com.termux.terminal.TerminalSession;
import com.termux.terminal.TerminalSessionClient;

import java.util.ArrayList;
import java.util.List;

public final class AlpineSession {
    public static final String TERMUX_HOME = "/root";

    public static TerminalSession create(Context ctx, TerminalSessionClient client) {
        String prefix = AlpineRootfs.prefix(ctx).getAbsolutePath();
        String rootfs = AlpineRootfs.rootfsDir(ctx).getAbsolutePath();

        List<String> args = new ArrayList<>();
        args.add("--kill-on-exit");
        args.add("-0");
        args.add("--link2symlink");
        args.add("--sysvipc");
        for (String sysMnt : new String[] {
            "/apex", "/odm", "/product", "/system", "/system_ext", "/vendor",
            "/linkerconfig/ld.config.txt",
            "/linkerconfig/com.android.art/ld.config.txt",
            "/property_contexts", "/plat_property_contexts"
        }) {
            args.add("-b");
            args.add(sysMnt);
        }
        for (String bind : new String[] {
            "/dev", "/proc", "/sys",
            "/dev/urandom:/dev/random",
            "/proc/self/fd:/dev/fd",
            "/proc/self/fd/0:/dev/stdin",
            "/proc/self/fd/1:/dev/stdout",
            "/proc/self/fd/2:/dev/stderr"
        }) {
            args.add("-b");
            args.add(bind);
        }
        args.add("-b");
        args.add(prefix + "/tmp:/dev/shm");
        args.add("-r");
        args.add(rootfs);
        args.add("-w");
        args.add(TERMUX_HOME);
        if ("true".equals(System.getenv("PROOT_DEBUG"))) {
            args.add("-v");
            args.add("2");
        }

        String[] env = new String[] {
            "HOME=" + TERMUX_HOME,
            "USER=root",
            "LOGNAME=root",
            "SHELL=/bin/sh",
            "PREFIX=" + prefix + "/usr",
            "TMPDIR=" + prefix + "/usr/tmp",
            "TERM=xterm-256color",
            "COLORTERM=truecolor",
            "LANG=C.UTF-8",
            "ANDROID_ROOT=" + (System.getenv("ANDROID_ROOT") == null ? "/system" : System.getenv("ANDROID_ROOT")),
            "ANDROID_DATA=" + (System.getenv("ANDROID_DATA") == null ? "/data" : System.getenv("ANDROID_DATA")),
            "PATH=/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin:/system/bin:/system/xbin:" + prefix + "/usr/bin"
        };

        return new TerminalSession(
            AlpineRootfs.prootBin(ctx).getAbsolutePath(),
            TERMUX_HOME,
            args.toArray(new String[0]),
            env,
            null,
            client
        );
    }
}
