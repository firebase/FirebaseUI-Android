/*
 * Copyright 2025 Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.firebase.ui.auth.util;

import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;

import com.google.firebase.auth.FirebaseUser;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;

/**
 * Utility class for extracting credential data from a {@link FirebaseUser} for the new CredentialManager.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public final class CredentialUtils {

    private static final String TAG = "CredentialUtils";

    private CredentialUtils() {
        throw new AssertionError("No instance for you!");
    }

    /**
     * Extracts the necessary data from the specified {@link FirebaseUser} along with the user's password.
     * <p>
     * If both the email and phone number are missing or the password is empty, this method returns {@code null}.
     *
     * @param user     the FirebaseUser from which to extract data.
     * @param password the password the user signed in with.
     * @return a {@link CredentialData} instance containing the user’s sign-in information, or {@code null} if insufficient data.
     */
    @Nullable
    public static CredentialData buildCredentialData(@NonNull FirebaseUser user,
                                                     @Nullable String password) {
        String email = user.getEmail();
        String phone = user.getPhoneNumber();
        Uri profilePictureUri = (user.getPhotoUrl() != null)
                ? Uri.parse(user.getPhotoUrl().toString())
                : null;

        if (TextUtils.isEmpty(email) && TextUtils.isEmpty(phone)) {
            Log.w(TAG, "User has no email or phone number; cannot build credential data.");
            return null;
        }
        if (TextUtils.isEmpty(password)) {
            Log.w(TAG, "Password is required to build credential data.");
            return null;
        }

        // Prefer email if available; otherwise fall back to phone.
        String identifier = !TextUtils.isEmpty(email) ? email : phone;
        return new CredentialData(identifier, user.getDisplayName(), password, profilePictureUri);
    }

    /**
     * Same as {@link #buildCredentialData(FirebaseUser, String)} but throws an exception if data cannot be built.
     *
     * @param user     the FirebaseUser.
     * @param password the password the user signed in with.
     * @return a non-null {@link CredentialData} instance.
     * @throws IllegalStateException if credential data cannot be constructed.
     */
    @NonNull
    public static CredentialData buildCredentialDataOrThrow(@NonNull FirebaseUser user,
                                                            @Nullable String password) {
        CredentialData credentialData = buildCredentialData(user, password);
        if (credentialData == null) {
            throw new IllegalStateException("Unable to build credential data");
        }
        return credentialData;
    }

    /**
     * A simple data class representing the information required by the new CredentialManager.
     */
    public static final class CredentialData {
        private final String identifier;
        private final String displayName;
        private final String password;
        private final Uri profilePictureUri;

        public CredentialData(String identifier, String displayName, String password, Uri profilePictureUri) {
            this.identifier = identifier;
            this.displayName = displayName;
            this.password = password;
            this.profilePictureUri = profilePictureUri;
        }

        public String getIdentifier() {
            return identifier;
        }

        public String getDisplayName() {
            return displayName;
        }

        public String getPassword() {
            return password;
        }

        public Uri getProfilePictureUri() {
            return profilePictureUri;
        }
    }
}
