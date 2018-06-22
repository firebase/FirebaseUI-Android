package com.firebase.uidemo.util;


import android.content.Context;
import android.content.res.Resources;

import com.firebase.ui.auth.AuthUI;
import com.firebase.uidemo.R;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ConfigurationUtils {

    public static boolean isGoogleMisconfigured(Context context) {
        return AuthUI.UNCONFIGURED_CONFIG_VALUE.equals(
                context.getString(R.string.default_web_client_id));
    }

    public static boolean isFacebookMisconfigured(Context context) {
        return AuthUI.UNCONFIGURED_CONFIG_VALUE.equals(
                context.getString(R.string.facebook_application_id));
    }

    public static boolean isTwitterMisconfigured(Context context) {
        List<String> twitterConfigs = Arrays.asList(
                context.getString(R.string.twitter_consumer_key),
                context.getString(R.string.twitter_consumer_secret)
        );

        return twitterConfigs.contains(AuthUI.UNCONFIGURED_CONFIG_VALUE);
    }

    public static List<AuthUI.IdpConfig> getConfiguredProviders(Context context) {
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

        return providers;
    }


}
