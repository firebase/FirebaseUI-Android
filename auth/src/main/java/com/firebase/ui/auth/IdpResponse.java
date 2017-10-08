/*
 * Copyright 2016 Google Inc. All Rights Reserved.
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

import android.app.Activity;
import android.content.Intent;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RestrictTo;
import android.text.TextUtils;

import com.firebase.ui.auth.data.model.NetworkException;
import com.firebase.ui.auth.data.model.User;
import com.firebase.ui.auth.util.ExtraConstants;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FacebookAuthProvider;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.auth.TwitterAuthProvider;

/**
 * A container that encapsulates the result of authenticating with an Identity Provider.
 */
public class IdpResponse implements Parcelable {
    private final User mUser;

    private final String mPassword;
    private final String mToken;
    private final String mSecret;

    @Nullable private final Exception mException;

    private IdpResponse(Exception e) {
        this(null, null, null, null, e);
    }

    private IdpResponse(
            User user,
            String password,
            String token,
            String secret,
            Exception e) {
        mUser = user;
        mPassword = password;
        mToken = token;
        mSecret = secret;
        mException = e;
    }

    /**
     * Extract the {@link IdpResponse} from the flow's result intent.
     *
     * @param resultIntent The intent which {@code onActivityResult} was called with.
     * @return The IdpResponse containing the token(s) from signing in with the Idp
     */
    @Nullable
    public static IdpResponse fromResultIntent(Intent resultIntent) {
        if (resultIntent != null) {
            return resultIntent.getParcelableExtra(ExtraConstants.EXTRA_IDP_RESPONSE);
        } else {
            return null;
        }
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public static IdpResponse fromError(@NonNull Exception e) {
        return new IdpResponse(e);
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public Intent toIntent() {
        return new Intent().putExtra(ExtraConstants.EXTRA_IDP_RESPONSE, this);
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public Boolean isSuccessful() {
        return mException == null;
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public User getUser() {
        return mUser;
    }

    /**
     * Get the type of provider. e.g. {@link AuthUI#GOOGLE_PROVIDER}
     */
    @NonNull
    @AuthUI.SupportedProvider
    public String getProviderType() {
        return mUser.getProviderId();
    }

    /**
     * Get the email used to sign in.
     */
    @Nullable
    public String getEmail() {
        return mUser.getEmail();
    }

    /**
     * Get the phone number used to sign in.
     */
    @Nullable
    public String getPhoneNumber() {
        return mUser.getPhoneNumber();
    }

    /**
     * Get the user's password if it's an email account.
     */
    @Nullable
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public String getPassword() {
        return mPassword;
    }

    /**
     * Get the token received as a result of logging in with the specified IDP
     */
    @Nullable
    public String getIdpToken() {
        return mToken;
    }

    /**
     * Twitter only. Return the token secret received as a result of logging in with Twitter.
     */
    @Nullable
    public String getIdpSecret() {
        return mSecret;
    }

    /**
     * Get the error code for a failed sign in
     */
    public int getErrorCode() {
        if (isSuccessful()) {
            return Activity.RESULT_OK;
        } else if (mException instanceof NetworkException) {
            return ErrorCodes.NO_NETWORK;
        } else {
            return ErrorCodes.UNKNOWN_ERROR;
        }
    }

    @Nullable
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public Exception getException() {
        return mException;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeParcelable(mUser, flags);
        dest.writeString(mPassword);
        dest.writeString(mToken);
        dest.writeString(mSecret);
        dest.writeSerializable(mException);
    }

    public static final Creator<IdpResponse> CREATOR = new Creator<IdpResponse>() {
        @Override
        public IdpResponse createFromParcel(Parcel in) {
            return new IdpResponse(
                    in.<User>readParcelable(User.class.getClassLoader()),
                    in.readString(),
                    in.readString(),
                    in.readString(),
                    (FirebaseAuthException) in.readSerializable()
            );
        }

        @Override
        public IdpResponse[] newArray(int size) {
            return new IdpResponse[size];
        }
    };

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public static class Builder {
        private final User mUser;

        private String mPassword;
        private String mToken;
        private String mSecret;

        public Builder(@NonNull User user) {
            mUser = user;
        }

        public Builder setPassword(String password) {
            mPassword = password;
            return this;
        }

        public Builder setToken(String token) {
            mToken = token;
            return this;
        }

        public Builder setSecret(String secret) {
            mSecret = secret;
            return this;
        }

        public IdpResponse build() {
            String providerId = mUser.getProviderId();
            if (providerId.equalsIgnoreCase(EmailAuthProvider.PROVIDER_ID)
                    && TextUtils.isEmpty(mPassword)) {
                throw new IllegalStateException(
                        "Password cannot be null when using the email provider.");
            }
            if ((providerId.equalsIgnoreCase(GoogleAuthProvider.PROVIDER_ID)
                    || providerId.equalsIgnoreCase(FacebookAuthProvider.PROVIDER_ID)
                    || providerId.equalsIgnoreCase(TwitterAuthProvider.PROVIDER_ID))
                    && TextUtils.isEmpty(mToken)) {
                throw new IllegalStateException(
                        "Token cannot be null when using a non-email provider.");
            }
            if (providerId.equalsIgnoreCase(TwitterAuthProvider.PROVIDER_ID)
                    && TextUtils.isEmpty(mSecret)) {
                throw new IllegalStateException(
                        "Secret cannot be null when using the Twitter provider.");
            }

            return new IdpResponse(mUser, mPassword, mToken, mSecret, null);
        }
    }
}
