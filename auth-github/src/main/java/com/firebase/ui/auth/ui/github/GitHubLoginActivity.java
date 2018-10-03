package com.firebase.ui.auth.ui.github;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.RestrictTo;
import android.support.customtabs.CustomTabsIntent;
import android.support.v4.content.ContextCompat;

import com.firebase.ui.auth.data.remote.GitHubSignInHandler;
import com.firebase.ui.auth.ui.HelperActivityBase;
import com.firebase.ui.auth.util.ExtraConstants;

/**
 * These are our goals for GitHub login:
 * <p>- Launch CCT
 * <p>- If user presses back, close resources related to CCT
 * <p>- Same for success and failure, but send result data too
 * <p>
 * Given that CCT is going to redirect to our activity, we need a wrapper with special stuff like
 * `singleTop` and ignored config changes so the stack doesn't nest itself—hence this activity. Now
 * that we're guaranteed to have a single activity with a CCT layer on top, we can safely assume
 * that `onCreate` is the only place to start CCT. So the current flow now looks like this:
 * <p>- Launch CCT in `onCreate`
 * <p>- Receive redirects in `onNewIntent`
 * <p>
 * That flow creates a problem though: how do we close CCT? Android doesn't give you a nice way to
 * close all activities on top of the stack, so we're forced to relaunch our wrapper activity with
 * the CLEAR_TOP flag. That will recurse while killing CCT to bring us back to `onNewIntent` again
 * where we check for the refresh action. At that point, we can finally finish with our result.
 * <p>
 * Now for the `onResume` stuff. Remember how we always have a CCT layer on top? That means
 * `onResume` will only ever be called once... unless the user presses back. At that point, our
 * wrapper activity gains focus for the second time and we can safely kill it knowing it was a back
 * event.
 */
@SuppressLint("GoogleAppIndexingApiWarning")
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class GitHubLoginActivity extends HelperActivityBase {
    private static final String REFRESH_ACTION = "refresh_action";
    private static final String SHOULD_CLOSE_CCT_KEY = "should_close_cct_key";

    private boolean mShouldCloseCustomTab;

    @NonNull
    public static Intent createIntent(Context context, Uri starter) {
        return new Intent(context, GitHubLoginActivity.class)
                .putExtra(ExtraConstants.GITHUB_URL, starter);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState == null) {
            new CustomTabsIntent.Builder()
                    .setShowTitle(true)
                    .enableUrlBarHiding()
                    .setToolbarColor(ContextCompat.getColor(this, R.color.colorPrimary))
                    .build()
                    .launchUrl(this,
                            (Uri) getIntent().getParcelableExtra(ExtraConstants.GITHUB_URL));
            mShouldCloseCustomTab = false;
        } else {
            mShouldCloseCustomTab = savedInstanceState.getBoolean(SHOULD_CLOSE_CCT_KEY);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mShouldCloseCustomTab) { // User pressed back
            finish(RESULT_CANCELED, null);
        }
        mShouldCloseCustomTab = true;
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(SHOULD_CLOSE_CCT_KEY, mShouldCloseCustomTab);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        mShouldCloseCustomTab = false;

        if (REFRESH_ACTION.equals(intent.getAction())) {
            finish(RESULT_OK, (Intent) intent.getParcelableExtra(ExtraConstants.PARAMS));
            return;
        }

        Intent result = new Intent();

        String code = intent.getData().getQueryParameter("code");
        if (code == null) {
            result.putExtra(GitHubSignInHandler.RESULT_CODE, RESULT_CANCELED);
        } else {
            result.putExtra(GitHubSignInHandler.RESULT_CODE, RESULT_OK)
                    .putExtra(GitHubSignInHandler.KEY_GITHUB_CODE, code);
        }

        // Force a recursive launch to clear the Custom Tabs activity
        startActivity(new Intent(this, GitHubLoginActivity.class)
                .putExtra(ExtraConstants.PARAMS, result)
                .setAction(REFRESH_ACTION)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP));
    }

    @Override
    public void showProgress(int message) {
        throw new UnsupportedOperationException(
                "GitHubLoginActivity is just a wrapper around Chrome Custom Tabs");
    }

    @Override
    public void hideProgress() {
        throw new UnsupportedOperationException(
                "GitHubLoginActivity is just a wrapper around Chrome Custom Tabs");
    }
}
