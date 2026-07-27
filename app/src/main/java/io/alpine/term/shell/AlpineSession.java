package io.alpine.term.shell;

import android.content.Context;

import com.termux.terminal.TerminalSession;
import com.termux.terminal.TerminalSessionClient;

public final class AlpineSession {
    public static final String TERMUX_HOME = "/root";

    public static TerminalSession create(Context ctx, TerminalSessionClient client) {
        String prefix = AlpineRootfs.prefix(ctx).getAbsolutePath();
        String[] env = new String[] {
            "HOME=" + TERMUX_HOME,
            "USER=root",
            "PREFIX=" + prefix + "/usr",
            "TMPDIR=" + prefix + "/usr/tmp",
            "PATH=/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin:/system/bin:/system/xbin:" + prefix + "/usr/bin",
            "TERM=xterm-256color",
            "COLORTERM=truecolor",
            "LANG=C.UTF-8",
            "ANDROID_ROOT=/system",
            "ANDROID_DATA=/data"
        };
        String[] args = new String[] {
            "--kill-on-exit",
            "--link2symlink",
            "--rootfs=" + AlpineRootfs.rootfsDir(ctx).getAbsolutePath(),
            "--cwd=/root",
            "--bind=/dev",
            "--bind=/proc",
            "--bind=/sys",
            "--bind=/dev/urandom:/dev/random",
            "--bind=/proc/self/fd:/dev/fd",
            "--bind=/proc/self/fd/0:/dev/stdin",
            "--bind=/proc/self/fd/1:/dev/stdout",
            "--bind=/proc/self/fd/2:/dev/stderr",
            "--bind=/system",
            "--bind=/vendor",
            "--bind=/apex",
            "--bind=/linkerconfig/ld.config.txt",
            "--change-id=0:0",
            "/bin/sh",
            "-l"
        };
        return new TerminalSession(
            AlpineRootfs.prootBin(ctx).getAbsolutePath(),
            TERMUX_HOME,
            args,
            env,
            null,
            client
        );
    }
}
