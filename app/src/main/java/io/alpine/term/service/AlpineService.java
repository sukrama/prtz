package io.alpine.term.service;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.termux.terminal.TerminalSession;
import com.termux.terminal.TerminalSessionClient;

import io.alpine.term.R;
import io.alpine.term.app.MainActivity;
import io.alpine.term.shell.AlpineSession;

import java.util.ArrayList;
import java.util.List;

public final class AlpineService extends Service {

    public final class LocalBinder extends Binder {
        public AlpineService service() { return AlpineService.this; }
    }

    private static final String CHANNEL_ID = "alpine_session";
    private static final int NOTIF_ID = 1;

    private final IBinder mBinder = new LocalBinder();
    private final List<TerminalSession> mSessions = new ArrayList<>();
    private final TerminalSessionClient mSessionClient = new TerminalSessionClient() {
        @Override public void onTextChanged(@NonNull TerminalSession changedSession) {}
        @Override public void onTitleChanged(@NonNull TerminalSession changedSession) {}
        @Override public void onSessionFinished(@NonNull TerminalSession finishedSession) {
            mSessions.remove(finishedSession);
            if (mSessions.isEmpty()) stopSelf();
        }
        @Override public void onCopyTextToClipboard(@NonNull TerminalSession session, String text) {}
        @Override public void onPasteTextFromClipboard(@Nullable TerminalSession session) {}
        @Override public void onBell(@NonNull TerminalSession session) {}
        @Override public void onColorsChanged(@NonNull TerminalSession session) {}
        @Override public void onTerminalCursorStateChange(boolean state) {}
        @Override public void setTerminalShellPid(@NonNull TerminalSession session, int pid) {}
        @Override public Integer getTerminalCursorStyle() { return null; }
        @Override public void logError(String tag, String message) {}
        @Override public void logWarn(String tag, String message) {}
        @Override public void logInfo(String tag, String message) {}
        @Override public void logDebug(String tag, String message) {}
        @Override public void logVerbose(String tag, String message) {}
        @Override public void logStackTraceWithMessage(String tag, String message, Exception e) {}
        @Override public void logStackTrace(String tag, Exception e) {}
    };

    @Override
    public void onCreate() {
        super.onCreate();
        createChannel();
        startForeground(NOTIF_ID, buildNotification());
    }

    @Override
    public IBinder onBind(Intent intent) { return mBinder; }

    public TerminalSession newSession() {
        TerminalSession s = AlpineSession.create(this, mSessionClient);
        mSessions.add(s);
        return s;
    }

    @Override
    public void onDestroy() {
        for (TerminalSession s : mSessions) s.finishIfRunning();
        mSessions.clear();
        super.onDestroy();
    }

    private void createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel ch = new NotificationChannel(CHANNEL_ID,
                    getString(R.string.notification_channel_name),
                    NotificationManager.IMPORTANCE_LOW);
            getSystemService(NotificationManager.class).createNotificationChannel(ch);
        }
    }

    private Notification buildNotification() {
        Intent i = new Intent(this, MainActivity.class)
                .setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        int piFlags = Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
                ? PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT
                : PendingIntent.FLAG_UPDATE_CURRENT;
        PendingIntent pi = PendingIntent.getActivity(this, 0, i, piFlags);
        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(getString(R.string.app_name))
                .setContentText(getString(R.string.notification_text))
                .setContentIntent(pi)
                .setOngoing(true)
                .build();
    }
}
