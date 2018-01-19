package com.firebase.ui.auth.util;

import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RestrictTo;
import android.text.TextUtils;

import com.firebase.ui.auth.IdpResponse;
import com.firebase.ui.auth.util.data.ProviderUtils;
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
     * See {@link #buildCredential(String, String, String, String, IdpResponse)}.
     */
    public static Credential buildCredential(@NonNull FirebaseUser user,
                                             @Nullable String password,
                                             @Nullable IdpResponse idpResponse) {
        String name = user.getDisplayName();
        String email = user.getEmail();
        String profilePicturUri = user.getPhotoUrl() != null
                ? user.getPhotoUrl().toString()
                : null;

        return buildCredential(email, password, name, profilePicturUri, idpResponse);
    }

    /**
     * Build the appropriate credential for the user information passed.
     *
     * If the credential cannot be built (for example, empty email) then
     * will return {@code null}.
     */
    @Nullable
    public static Credential buildCredential(@Nullable String email,
                                             @Nullable String password,
                                             @Nullable String name,
                                             @Nullable String profilePictureUri,
                                             @Nullable IdpResponse idpResponse) {
        if (TextUtils.isEmpty(email)) {
            return null;
        }

        Credential.Builder builder = new Credential.Builder(email);
        if (!TextUtils.isEmpty(password)) {
            builder.setPassword(password);
        }
        if (password == null && idpResponse != null) {
            String translatedProvider =
                    ProviderUtils.providerIdToAccountType(idpResponse.getProviderType());
            if (translatedProvider != null) {
                builder.setAccountType(translatedProvider);
            } else {
                return null;
            }
        }

        if (name != null) {
            builder.setName(name);
        }

        if (profilePictureUri != null) {
            builder.setProfilePictureUri(Uri.parse(profilePictureUri));
        }

        return builder.build();
    }

}
