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

package com.firebase.ui.auth;

import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;

import com.google.firebase.auth.AuthCredential;

/**
 * Internal exception which holds the necessary data to complete sign-in in the event of a
 * recoverable error.
 */
public class FirebaseUiUserCollisionException extends Exception {

    private final int mErrorCode;
    private final String mProviderId;
    private final String mEmail;
    private final AuthCredential mCredential;

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public FirebaseUiUserCollisionException(@ErrorCodes.Code int code,
                                            @NonNull String message,
                                            @NonNull String providerId,
                                            @NonNull String email,
                                            @NonNull AuthCredential credential) {
        super(message);
        mErrorCode = code;
        mProviderId = providerId;
        mEmail = email;
        mCredential = credential;
    }

    @NonNull
    public String getProviderId() {
        return mProviderId;
    }

    @NonNull
    public String getEmail() {
        return mEmail;
    }

    @NonNull
    public AuthCredential getCredential() {
        return mCredential;
    }

    /**
     * @return error code associated with this exception
     * @see ErrorCodes
     */
    @ErrorCodes.Code
    public final int getErrorCode() {
        return mErrorCode;
    }
}
