package com.firebase.ui.auth.ui.provider;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.RestrictTo;
import android.support.customtabs.CustomTabsIntent;
import android.support.v4.content.ContextCompat;

import com.firebase.ui.auth.R;
import com.firebase.ui.auth.data.remote.GitHubSignInHandler;
import com.firebase.ui.auth.ui.HelperActivityBase;
import com.firebase.ui.auth.util.ExtraConstants;

@SuppressLint("GoogleAppIndexingApiWarning")
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class GitHubLoginActivity extends HelperActivityBase {
    private static final String REFRESH_ACTION = "refresh_action";

    private boolean mShouldCloseCustomTab;

    @NonNull
    public static Intent createIntent(Context context, Uri starter) {
        return new Intent(context, GitHubLoginActivity.class)
                .putExtra(ExtraConstants.PARAMS, starter);
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
                    .launchUrl(this, (Uri) getIntent().getParcelableExtra(ExtraConstants.PARAMS));
            mShouldCloseCustomTab = false;
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
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        mShouldCloseCustomTab = false;

        if (REFRESH_ACTION.equals(intent.getAction())) {
            finish(RESULT_OK, (Intent) intent.getParcelableExtra(ExtraConstants.PARAMS));
            return;
        }

        Intent result = new Intent().setAction(GitHubSignInHandler.REDIRECT_ACTION);

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
}
