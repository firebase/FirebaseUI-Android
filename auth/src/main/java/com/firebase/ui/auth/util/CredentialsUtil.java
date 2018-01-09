package com.firebase.ui.auth.util;

import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import com.firebase.ui.auth.IdpResponse;
import com.firebase.ui.auth.util.data.ProviderUtils;
import com.google.android.gms.auth.api.credentials.Credential;
import com.google.firebase.auth.FirebaseUser;

/**
 * TODO(samstern): Document
 */
public class CredentialsUtil {

    /**
     * TODO: Document
     * @param user
     * @param password
     * @param idpResponse
     * @return
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
     * TODO: Document
     * @return
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
        builder.setPassword(password);
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
