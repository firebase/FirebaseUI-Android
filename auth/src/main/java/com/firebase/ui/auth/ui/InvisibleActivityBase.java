package com.firebase.ui.auth.ui;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.annotation.RestrictTo;
import android.view.ContextThemeWrapper;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import com.firebase.ui.auth.R;

import me.zhanghai.android.materialprogressbar.MaterialProgressBar;

/**
 * Base classes for activities that are just simple overlays.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class InvisibleActivityBase extends HelperActivityBase {

    // Minimum time that the spinner will stay on screen, once it is shown.
    private static final long MIN_SPINNER_MS = 750;

    private Handler mHandler = new Handler();
    private MaterialProgressBar mProgressBar;

    // Last time that the progress bar was actually shown
    private long mLastShownTime = 0;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.fui_activity_invisible);

        // Create an indeterminate, circular progress bar in the app's theme
        mProgressBar = new MaterialProgressBar(new ContextThemeWrapper(this, getFlowParams().themeId));
        mProgressBar.setIndeterminate(true);
        mProgressBar.setVisibility(View.GONE);

        // Set bar to float in the center
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.gravity = Gravity.CENTER;

        // Add to the container
        FrameLayout container = findViewById(R.id.invisible_frame);
        container.addView(mProgressBar, params);
    }

    @Override
    public void showProgress(int message) {
        if (mProgressBar.getVisibility() == View.VISIBLE) {
            mHandler.removeCallbacksAndMessages(null);
            return;
        }

        mLastShownTime = System.currentTimeMillis();
        mProgressBar.setVisibility(View.VISIBLE);
    }

    @Override
    public void hideProgress() {
        doAfterTimeout(new Runnable() {
            @Override
            public void run() {
                mLastShownTime = 0;
                mProgressBar.setVisibility(View.GONE);
            }
        });
    }

    @Override
    public void finish(int resultCode, @Nullable Intent intent) {
        setResult(resultCode, intent);
        doAfterTimeout(new Runnable() {
            @Override
            public void run() {
                finish();
            }
        });
    }

    /**
     * For certain actions (like finishing or hiding the progress dialog) we want to make sure
     * that we have shown the progress state for at least MIN_SPINNER_MS to prevent flickering.
     *
     * This method performs some action after the window has passed, or immediately if we have
     * already waited longer than that.
     */
    private void doAfterTimeout(Runnable runnable) {
        long currentTime = System.currentTimeMillis();
        long diff = currentTime - mLastShownTime;

        // 'diff' is how long it's been since we showed the spinner, so in the
        // case where diff is greater than our minimum spinner duration then our
        // remaining wait time is 0.
        long remaining = Math.max(MIN_SPINNER_MS - diff, 0);

        mHandler.postDelayed(runnable, remaining);
    }
}
