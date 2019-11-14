package com.firebase.ui.auth.util.data;

import android.content.Context;
import android.content.SharedPreferences;

import com.firebase.ui.auth.IdpResponse;
import com.firebase.ui.auth.data.model.User;
import com.google.android.gms.common.internal.Preconditions;
import com.google.firebase.auth.AuthCredential;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;


/** Manages saving/retrieving from SharedPreferences for email link sign in. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class EmailLinkPersistenceManager {

    private static final String SHARED_PREF_NAME =
            "com.firebase.ui.auth.util.data.EmailLinkPersistenceManager";

    private static final String KEY_EMAIL = "com.firebase.ui.auth.data.client.email";
    private static final String KEY_PROVIDER = "com.firebase.ui.auth.data.client.provider";
    private static final String KEY_IDP_TOKEN = "com.firebase.ui.auth.data.client.idpToken";
    private static final String KEY_IDP_SECRET = "com.firebase.ui.auth.data.client.idpSecret";
    private static final String KEY_ANONYMOUS_USER_ID = "com.firebase.ui.auth.data.client.auid";
    private static final String KEY_SESSION_ID = "com.firebase.ui.auth.data.client.sid";

    private static final Set<String> KEYS =
            Collections.unmodifiableSet(new HashSet<>(Arrays.asList(KEY_EMAIL, KEY_PROVIDER,
                    KEY_IDP_TOKEN, KEY_IDP_SECRET)));

    private static final EmailLinkPersistenceManager instance = new EmailLinkPersistenceManager();

    public static EmailLinkPersistenceManager getInstance() {
        return instance;
    }

    private AuthCredential mCredentialForLinking;

    public void saveEmail(@NonNull Context context,
                          @NonNull String email,
                          @NonNull String sessionId,
                          @Nullable String anonymousUserId) {
        Preconditions.checkNotNull(context);
        Preconditions.checkNotNull(email);
        SharedPreferences.Editor editor =
                context.getSharedPreferences(SHARED_PREF_NAME, Context.MODE_PRIVATE).edit();
        editor.putString(KEY_EMAIL, email);
        editor.putString(KEY_ANONYMOUS_USER_ID, anonymousUserId);
        editor.putString(KEY_SESSION_ID, sessionId);
        editor.apply();
    }

    public void saveIdpResponseForLinking(@NonNull Context context,
                                          @NonNull IdpResponse idpResponseForLinking) {
        if (idpResponseForLinking.hasCredentialForLinking()) {
            // Signing in via Generic IDP returns a credential that must be used for sign-in.
            // We can't sign-in by rebuilding using the id token/access token, so we need to use the
            // credential directly.
            // We also can't store this credential in SharedPrefs, so this is just
            // best effort.
            mCredentialForLinking = idpResponseForLinking.getCredentialForLinking();
        }
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
    public SessionRecord retrieveSessionRecord(@NonNull Context context) {
        Preconditions.checkNotNull(context);
        SharedPreferences sharedPreferences =
                context.getSharedPreferences(SHARED_PREF_NAME, Context.MODE_PRIVATE);
        String email = sharedPreferences.getString(KEY_EMAIL, null);
        String sessionId = sharedPreferences.getString(KEY_SESSION_ID, null);
        if (email == null || sessionId == null) {
            return null;
        }
        String anonymousUserId = sharedPreferences.getString(KEY_ANONYMOUS_USER_ID, null);
        String provider = sharedPreferences.getString(KEY_PROVIDER, null);
        String idpToken = sharedPreferences.getString(KEY_IDP_TOKEN, null);
        String idpSecret = sharedPreferences.getString(KEY_IDP_SECRET, null);

        SessionRecord sessionRecord = new SessionRecord(sessionId, anonymousUserId).setEmail(email);
        if (provider != null && (idpToken != null || mCredentialForLinking != null)) {
            IdpResponse response = new IdpResponse.Builder(
                        new User.Builder(provider, email).build())
                    .setPendingCredential(mCredentialForLinking)
                    .setToken(idpToken)
                    .setSecret(idpSecret)
                    .setNewUser(false)
                    .build();
            sessionRecord.setIdpResponseForLinking(response);
        }
        mCredentialForLinking = null;
        return sessionRecord;
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

    /** Holds the necessary information to complete the email link sign in flow */
    public static class SessionRecord {
        private String mSessionId;
        private String mEmail;
        @Nullable private String mAnonymousUserId;
        @Nullable private IdpResponse mIdpResponseForLinking;

        public SessionRecord(@NonNull String sessionId,
                             @Nullable String anonymousUserId) {
            Preconditions.checkNotNull(sessionId);
            this.mSessionId = sessionId;
            this.mAnonymousUserId = anonymousUserId;
        }

        public String getSessionId() {
            return mSessionId;
        }

        public String getEmail() {
            return mEmail;
        }

        public SessionRecord setEmail(@NonNull String email) {
            this.mEmail = email;
            return this;
        }

        @Nullable
        public IdpResponse getIdpResponseForLinking() {
            return mIdpResponseForLinking;
        }

        public SessionRecord setIdpResponseForLinking(@NonNull IdpResponse idpResponseForLinking) {
            this.mIdpResponseForLinking = idpResponseForLinking;
            return this;
        }

        @Nullable
        public String getAnonymousUserId() {
            return mAnonymousUserId;
        }
    }
}
