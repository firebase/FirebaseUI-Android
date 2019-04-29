package com.firebase.uidemo.util;


import android.content.Context;
import android.support.annotation.NonNull;

import com.firebase.ui.auth.AuthUI;
import com.firebase.uidemo.R;
import com.google.firebase.auth.ActionCodeSettings;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public final class ConfigurationUtils {

    private ConfigurationUtils() {
        throw new AssertionError("No instance for you!");
    }

    public static boolean isGoogleMisconfigured(@NonNull Context context) {
        return AuthUI.UNCONFIGURED_CONFIG_VALUE.equals(
                context.getString(R.string.default_web_client_id));
    }

    public static boolean isFacebookMisconfigured(@NonNull Context context) {
        return AuthUI.UNCONFIGURED_CONFIG_VALUE.equals(
                context.getString(R.string.facebook_application_id));
    }

    public static boolean isGitHubMisconfigured(@NonNull Context context) {
        List<String> gitHubConfigs = Arrays.asList(
                context.getString(R.string.firebase_web_host),
                context.getString(R.string.github_client_id),
                context.getString(R.string.github_client_secret)
        );

        return gitHubConfigs.contains(AuthUI.UNCONFIGURED_CONFIG_VALUE);
    }

    @NonNull
    public static List<AuthUI.IdpConfig> getConfiguredProviders(@NonNull Context context) {
        List<AuthUI.IdpConfig> providers = new ArrayList<>();

        if (!isGoogleMisconfigured(context)) {
            providers.add(new AuthUI.IdpConfig.GoogleBuilder().build());
        }

        if (!isFacebookMisconfigured(context)) {
            providers.add(new AuthUI.IdpConfig.FacebookBuilder().build());
        }

        if (!isGitHubMisconfigured(context)) {
            providers.add(new AuthUI.IdpConfig.GitHubBuilder().build());
        }

        ActionCodeSettings actionCodeSettings = ActionCodeSettings.newBuilder()
                .setAndroidPackageName("com.firebase.uidemo", true, null)
                .setHandleCodeInApp(true)
                .setUrl("https://google.com")
                .build();

        providers.add(new AuthUI.IdpConfig.EmailBuilder()
                .setAllowNewAccounts(true)
                .enableEmailLinkSignIn()
                .setActionCodeSettings(actionCodeSettings)
                .build());


        providers.add(new AuthUI.IdpConfig.PhoneBuilder().build());


        return providers;
    }
}
