package com.firebase.ui.auth.ui;

import android.os.Bundle;
import android.os.Handler;
import android.view.ContextThemeWrapper;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import com.firebase.ui.auth.R;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import me.zhanghai.android.materialprogressbar.MaterialProgressBar;

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class InvisibleFragmentBase extends FragmentBase {

    // Minimum time that the spinner will stay on screen, once it is shown.
    private static final long MIN_SPINNER_MS = 750;
    protected FrameLayout mFrameLayout;
    protected View mTopLevelView;
    private Handler mHandler = new Handler();
    private MaterialProgressBar mProgressBar;
    // Last time that the progress bar was actually shown
    private long mLastShownTime = 0;

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {

        // Create an indeterminate, circular progress bar in the app's theme
        mProgressBar = new MaterialProgressBar(new ContextThemeWrapper(getContext(),
                getFlowParams().themeId));
        mProgressBar.setIndeterminate(true);
        mProgressBar.setVisibility(View.GONE);

        // Set bar to float in the center
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.gravity = Gravity.CENTER;

        // Add to the container
        mFrameLayout = view.findViewById(R.id.invisible_frame);
        mFrameLayout.addView(mProgressBar, params);
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
                mFrameLayout.setVisibility(View.GONE);
            }
        });
    }

    /**
     * For certain actions (like finishing or hiding the progress dialog) we want to make sure
     * that we have shown the progress state for at least MIN_SPINNER_MS to prevent flickering.
     * <p>
     * This method performs some action after the window has passed, or immediately if we have
     * already waited longer than that.
     */
    protected void doAfterTimeout(Runnable runnable) {
        long currentTime = System.currentTimeMillis();
        long diff = currentTime - mLastShownTime;

        // 'diff' is how long it's been since we showed the spinner, so in the
        // case where diff is greater than our minimum spinner duration then our
        // remaining wait time is 0.
        long remaining = Math.max(MIN_SPINNER_MS - diff, 0);

        mHandler.postDelayed(runnable, remaining);
    }
}
