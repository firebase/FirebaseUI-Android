package com.firebase.ui.auth.provider;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.customtabs.CustomTabsIntent;
import android.support.v4.content.ContextCompat;

import com.firebase.ui.auth.R;
import com.firebase.ui.auth.ui.BaseHelper;
import com.firebase.ui.auth.ui.FlowParameters;
import com.firebase.ui.auth.ui.HelperActivityBase;

public class GitHubLoginHolder extends HelperActivityBase {
    private static final String AUTHORIZE_QUERY = "authorize?client_id=";
    private static final String SCOPE_QUERY = "&scope=";

    public static Intent createIntent(Context context, FlowParameters params) {
        return BaseHelper.createBaseIntent(context, GitHubLoginHolder.class, params);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState == null) {
            CustomTabsIntent intent = new CustomTabsIntent.Builder()
                    .setShowTitle(true)
                    .setToolbarColor(ContextCompat.getColor(this, R.color.colorPrimary))
                    .build();
            intent.launchUrl(this, Uri.parse(
                    GitHubProvider.GITHUB_OAUTH_BASE + AUTHORIZE_QUERY + getString(R.string.github_client_id)
                            + SCOPE_QUERY + getScopeList()));
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        String code = intent.getData().getQueryParameter("code");
        if (code == null) {
            finish(RESULT_CANCELED, new Intent());
        } else {
            finish(RESULT_OK, new Intent().putExtra(GitHubProvider.KEY_GITHUB_CODE, code));
        }
    }

    private String getScopeList() {
        return "user:email";
    }
}
