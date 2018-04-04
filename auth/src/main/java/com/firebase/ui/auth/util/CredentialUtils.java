package com.firebase.ui.auth.util;

import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RestrictTo;
import android.text.TextUtils;

import com.firebase.ui.auth.IdpResponse;
import com.google.android.gms.auth.api.credentials.Credential;
import com.google.firebase.auth.FirebaseUser;

/**
 * Utility class for working with {@link Credential} objects.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class CredentialUtils {
    private CredentialUtils() {
        throw new AssertionError("No instance for you!");
    }

    /**
     * Build a credential for the specified {@link FirebaseUser} with optional password and {@link
     * IdpResponse}.
     * <p>
     * If the credential cannot be built (for example, empty email) then will return {@code null}.
     */
    @Nullable
    public static Credential buildCredential(@NonNull FirebaseUser user,
                                             @Nullable String password,
                                             @Nullable String accountType) {
        String email = user.getEmail();
        String phone = user.getPhoneNumber();
        Uri profilePictureUri =
                user.getPhotoUrl() == null ? null : Uri.parse(user.getPhotoUrl().toString());

        if (TextUtils.isEmpty(email) && TextUtils.isEmpty(phone)) { return null; }
        if (password == null && accountType == null) { return null; }

        Credential.Builder builder =
                new Credential.Builder(TextUtils.isEmpty(email) ? phone : email)
                        .setName(user.getDisplayName())
                        .setProfilePictureUri(profilePictureUri);

        if (TextUtils.isEmpty(password)) {
            builder.setAccountType(accountType);
        } else {
            builder.setPassword(password);
        }

        return builder.build();
    }

    /**
     * @see #buildCredential(FirebaseUser, String, String)
     */
    @NonNull
    public static Credential buildCredentialOrThrow(@NonNull FirebaseUser user,
                                                    @Nullable String password,
                                                    @Nullable String accountType) {
        Credential credential = buildCredential(user, password, accountType);
        if (credential == null) {
            throw new IllegalStateException("Unable to build credential");
        }
        return credential;
    }
}
