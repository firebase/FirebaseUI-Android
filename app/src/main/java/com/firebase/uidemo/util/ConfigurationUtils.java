package com.firebase.uidemo.util;


import android.content.Context;
import android.support.annotation.NonNull;

import com.firebase.ui.auth.AuthUI;
import com.firebase.uidemo.R;

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

    public static boolean isTwitterMisconfigured(@NonNull Context context) {
        List<String> twitterConfigs = Arrays.asList(
                context.getString(R.string.twitter_consumer_key),
                context.getString(R.string.twitter_consumer_secret)
        );

        return twitterConfigs.contains(AuthUI.UNCONFIGURED_CONFIG_VALUE);
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
        providers.add(new AuthUI.IdpConfig.EmailBuilder().build());
        providers.add(new AuthUI.IdpConfig.PhoneBuilder().build());

        if (!isGoogleMisconfigured(context)) {
            providers.add(new AuthUI.IdpConfig.GoogleBuilder().build());
        }

        if (!isFacebookMisconfigured(context)) {
            providers.add(new AuthUI.IdpConfig.FacebookBuilder().build());
        }

        if (!isTwitterMisconfigured(context)) {
            providers.add(new AuthUI.IdpConfig.TwitterBuilder().build());
        }

        if (!isGitHubMisconfigured(context)) {
            providers.add(new AuthUI.IdpConfig.GitHubBuilder().build());
        }

        return providers;
    }
}
