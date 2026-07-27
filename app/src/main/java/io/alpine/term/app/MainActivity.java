package io.alpine.term.app;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Color;
import android.os.Bundle;
import android.os.IBinder;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.termux.terminal.TerminalSession;
import com.termux.view.TerminalView;
import com.termux.view.TerminalViewClient;

import io.alpine.term.R;
import io.alpine.term.service.AlpineService;
import io.alpine.term.shell.AlpineRootfs;

public final class MainActivity extends Activity implements ServiceConnection {

    private TerminalView mTerminalView;
    private final TerminalViewClient mViewClient = new TerminalViewClient() {
        @Override public float onScale(float scale) { return 1.0f; }
        @Override public void onSingleTapUp(MotionEvent e) {}
        @Override public boolean shouldBackButtonBeMappedToEscape() { return true; }
        @Override public boolean shouldEnforceCharBasedInput() { return false; }
        @Override public boolean shouldUseCtrlSpaceWorkaround() { return false; }
        @Override public boolean isTerminalViewSelected() { return true; }
        @Override public void copyModeChanged(boolean copyMode) {}
        @Override public boolean onKeyDown(int keyCode, KeyEvent e, TerminalSession session) { return false; }
        @Override public boolean onKeyUp(int keyCode, KeyEvent e) { return false; }
        @Override public boolean onLongPress(MotionEvent event) { return false; }
        @Override public boolean readControlKey() { return false; }
        @Override public boolean readAltKey() { return false; }
        @Override public boolean readShiftKey() { return false; }
        @Override public boolean readFnKey() { return false; }
        @Override public boolean onCodePoint(int codePoint, boolean ctrlDown, TerminalSession session) { return false; }
        @Override public void onEmulatorSet() {}
        @Override public void logError(String tag, String message) {}
        @Override public void logWarn(String tag, String message) {}
        @Override public void logInfo(String tag, String message) {}
        @Override public void logDebug(String tag, String message) {}
        @Override public void logVerbose(String tag, String message) {}
        @Override public void logStackTraceWithMessage(String tag, String message, Exception e) {}
        @Override public void logStackTrace(String tag, Exception e) {}
    };
    private AlpineService mService;
    private TextView mSplash;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(Color.BLACK);
        setContentView(root);

        mSplash = new TextView(this);
        mSplash.setText("preparing alpine…");
        mSplash.setTextColor(Color.WHITE);
        mSplash.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        mSplash.setGravity(Gravity.CENTER);
        root.addView(mSplash);

        startService(new Intent(this, AlpineService.class));
        bindService(new Intent(this, AlpineService.class), this, Context.BIND_AUTO_CREATE);
    }

    @Override
    public void onServiceConnected(ComponentName name, IBinder binder) {
        mService = ((AlpineService.LocalBinder) binder).service();
        new Thread(() -> {
            try {
                AlpineRootfs.installIfNeeded(getApplicationContext());
            } catch (Throwable t) {
                runOnUiThread(() -> mSplash.setText("install failed: " + t.getMessage()));
                return;
            }
            runOnUiThread(() -> {
                mTerminalView = new TerminalView(MainActivity.this, mViewClient);
                mTerminalView.setTerminalViewClient(mViewClient);
                mTerminalView.setOnKeyListener((View v, int kc, KeyEvent ev) -> {
                    if (kc == KeyEvent.KEYCODE_BACK) return false;
                    return false;
                });
                ViewGroup parent = (ViewGroup) findViewById(android.R.id.content);
                parent.removeAllViews();
                parent.addView(mTerminalView,
                        new FrameLayout.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT));
                TerminalSession session = mService.newSession();
                mTerminalView.attachSession(session);
            });
        }, "alpine-bootstrap").start();
    }

    @Override
    public void onServiceDisconnected(ComponentName name) { mService = null; }

    @Override
    protected void onResume() {
        super.onResume();
        if (mTerminalView != null) mTerminalView.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mTerminalView != null) mTerminalView.onPause();
    }

    @Override
    protected void onDestroy() {
        try { unbindService(this); } catch (Exception ignored) {}
        super.onDestroy();
    }
}
