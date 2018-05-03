package com.firebase.ui.auth.ui;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.annotation.RestrictTo;
import android.view.View;
import android.widget.ProgressBar;

import com.firebase.ui.auth.R;


/**
 * Base classes for activities that are just simple overlays.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class InvisibleActivityBase extends HelperActivityBase {

    private static final long MIN_SPINNER_MS = 1000;

    private Handler mHandler = new Handler();
    private ProgressBar mProgressBar;
    private long mLastShownTime = 0;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.fui_activity_kickoff);

        // TODO: Set the color based on passed in theme
        mProgressBar = findViewById(R.id.progress_spinner);
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
                mProgressBar.setVisibility(View.INVISIBLE);
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

    private void doAfterTimeout(Runnable runnable) {
        long currentTime = System.currentTimeMillis();
        long diff = currentTime - mLastShownTime;
        long remaining = Math.max(MIN_SPINNER_MS - diff, 0);

        mHandler.postDelayed(runnable, remaining);
    }
}
