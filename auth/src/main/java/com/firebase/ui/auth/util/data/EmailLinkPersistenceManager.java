package com.firebase.ui.auth.util.data;

import android.content.Context;
import android.content.SharedPreferences;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RestrictTo;

import com.firebase.ui.auth.IdpResponse;
import com.firebase.ui.auth.data.model.User;
import com.google.android.gms.common.internal.Preconditions;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;


/**
 * Manages saving/reading from SharedPreferences for email link sign in.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class EmailLinkPersistenceManager {

    private static final String SHARED_PREF_NAME =
            "com.firebase.ui.auth.util.data.EmailLinkPersistenceManager";

    private static final String KEY_EMAIL = "com.firebase.ui.auth.data.client.email";
    private static final String KEY_PROVIDER = "com.firebase.ui.auth.data.client.provider";
    private static final String KEY_IDP_TOKEN = "com.firebase.ui.auth.data.client.idpToken";
    private static final String KEY_IDP_SECRET = "com.firebase.ui.auth.data.client.idpSecret";


    private static final Set<String> KEYS =
            Collections.unmodifiableSet(new HashSet<>(Arrays.asList(KEY_EMAIL, KEY_PROVIDER,
                    KEY_IDP_TOKEN, KEY_IDP_SECRET)));


    private static final EmailLinkPersistenceManager instance = new EmailLinkPersistenceManager();

    public static EmailLinkPersistenceManager getInstance() {
        return instance;
    }

    public void saveEmailForLink(@NonNull Context context, @NonNull String email) {
        Preconditions.checkNotNull(context);
        Preconditions.checkNotNull(email);
        SharedPreferences.Editor editor =
                context.getSharedPreferences(SHARED_PREF_NAME, Context.MODE_PRIVATE).edit();
        editor.putString(KEY_EMAIL, email);
        editor.apply();
    }

    public void saveIdpResponseForLinking(@NonNull Context context, @NonNull IdpResponse
            idpResponseForLinking) {
        Preconditions.checkNotNull(context);
        Preconditions.checkNotNull(idpResponseForLinking);
        SharedPreferences.Editor editor =
                context.getSharedPreferences(SHARED_PREF_NAME, Context.MODE_PRIVATE).edit();
        editor.putString(KEY_EMAIL, idpResponseForLinking.getEmail());
        editor.putString(KEY_PROVIDER, idpResponseForLinking.getProviderType());
        editor.putString(KEY_IDP_TOKEN, idpResponseForLinking.getIdpToken());
        editor.putString(KEY_IDP_SECRET, idpResponseForLinking.getIdpSecret());
        editor.apply();
    }

    @Nullable
    public String retrieveEmailForLink(@NonNull Context context) {
        Preconditions.checkNotNull(context);
        SharedPreferences sharedPreferences =
                context.getSharedPreferences(SHARED_PREF_NAME, Context.MODE_PRIVATE);
        return sharedPreferences.getString(KEY_EMAIL, null);

    }

    public IdpResponse retrieveIdpResponseForLinking(@NonNull Context context) {
        Preconditions.checkNotNull(context);
        SharedPreferences sharedPreferences =
                context.getSharedPreferences(SHARED_PREF_NAME, Context.MODE_PRIVATE);
        String email = sharedPreferences.getString(KEY_EMAIL, null);
        String provider = sharedPreferences.getString(KEY_PROVIDER, null);
        String idpToken = sharedPreferences.getString(KEY_IDP_TOKEN, null);
        String idpSecret = sharedPreferences.getString(KEY_IDP_SECRET, null);

        IdpResponse response = null;
        if (email != null && provider != null && idpToken != null) {
            response = new IdpResponse.Builder(new User.Builder(provider, email).build())
                    .setToken(idpToken)
                    .setSecret(idpSecret)
                    .setNewUser(false)
                    .build();
        }
        return response;
    }


    public void clearAllData(@NonNull Context context) {
        Preconditions.checkNotNull(context);
        SharedPreferences sharedPreferences =
                context.getSharedPreferences(SHARED_PREF_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        for (String key : KEYS) {
            editor.remove(key);
        }
        editor.apply();
    }
}
