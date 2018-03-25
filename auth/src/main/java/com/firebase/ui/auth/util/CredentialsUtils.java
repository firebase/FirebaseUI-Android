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
public class CredentialsUtils {
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
        String profilePictureUri =
                user.getPhotoUrl() == null ? null : user.getPhotoUrl().toString();

        if (TextUtils.isEmpty(email) && TextUtils.isEmpty(phone)
                || password == null && accountType == null) {
            return null;
        }

        Credential.Builder builder =
                new Credential.Builder(TextUtils.isEmpty(email) ? phone : email)
                        .setName(user.getDisplayName())
                        .setProfilePictureUri(Uri.parse(profilePictureUri));

        if (TextUtils.isEmpty(password)) {
            builder.setAccountType(accountType);
        } else {
            builder.setPassword(password);
        }

        return builder.build();
    }
}
