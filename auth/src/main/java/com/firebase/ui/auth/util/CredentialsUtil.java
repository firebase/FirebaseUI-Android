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
public class CredentialsUtil {

    /**
     * Build a credential for the specified {@link FirebaseUser} with optional
     * password and {@link IdpResponse}.
     *
     * See {@link #buildCredential(String, String, String, String, String, String)}.
     */
    @Nullable
    public static Credential buildCredential(@NonNull FirebaseUser user,
                                             @Nullable String password,
                                             @Nullable String accountType) {
        return buildCredential(
                user.getEmail(),
                password,
                user.getPhoneNumber(),
                user.getDisplayName(),
                user.getPhotoUrl() != null ? user.getPhotoUrl().toString() : null,
                accountType);
    }

    /**
     * Build the appropriate credential for the user information passed.
     *
     * If the credential cannot be built (for example, empty email) then
     * will return {@code null}.
     */
    @Nullable
    private static Credential buildCredential(@Nullable String email,
                                              @Nullable String password,
                                              @Nullable String phone,
                                              @Nullable String name,
                                              @Nullable String profilePictureUri,
                                              @Nullable String accountType) {
        if (TextUtils.isEmpty(email) && TextUtils.isEmpty(phone)) {
            return null;
        }

        Credential.Builder builder =
                new Credential.Builder(TextUtils.isEmpty(email) ? phone : email);

        builder.setName(name);
        builder.setProfilePictureUri(Uri.parse(profilePictureUri));

        builder.setPassword(password);
        if (password == null) {
            if (accountType != null) {
                builder.setAccountType(accountType);
            } else {
                return null;
            }
        }

        return builder.build();
    }

}
