package com.firebase.ui.auth.provider;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;

import com.firebase.ui.auth.ui.HelperActivityBase;

public class GitHubRedirectActivity extends HelperActivityBase {
    @Override
    protected void onCreate(Bundle savedInstance) {
        super.onCreate(savedInstance);
        Intent result;
        Intent baseIntent = new Intent()
                .setAction(GitHubProvider.getGitHubRedirectFilter(this).getAction(0));

        String code = getIntent().getData().getQueryParameter("code");
        if (code == null) {
            result = baseIntent.putExtra(GitHubProvider.RESULT_CODE, RESULT_CANCELED);
        } else {
            result = baseIntent
                    .putExtra(GitHubProvider.RESULT_CODE, RESULT_OK)
                    .putExtra(GitHubProvider.KEY_GITHUB_CODE, code);
        }

        LocalBroadcastManager.getInstance(this).sendBroadcast(result);
        finish();
    }
}
