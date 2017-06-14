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

import android.content.Intent;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RestrictTo;
import android.text.TextUtils;

import com.firebase.ui.auth.ui.ExtraConstants;
import com.google.firebase.auth.FacebookAuthProvider;
import com.google.firebase.auth.GithubAuthProvider;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.auth.TwitterAuthProvider;

/**
 * A container that encapsulates the result of authenticating with an Identity Provider.
 */
public class IdpResponse implements Parcelable {
    private final String mProviderId;
    private final String mEmail;
    private final String mPhoneNumber;
    private final String mToken;
    private final String mSecret;
    private final int mErrorCode;

    private IdpResponse(int errorCode) {
        this(null, null, null, null, null, errorCode);
    }

    private IdpResponse(
            String providerId,
            String email,
            String phoneNumber,
            String token,
            String secret,
            int errorCode) {
        mProviderId = providerId;
        mEmail = email;
        mPhoneNumber = phoneNumber;
        mToken = token;
        mSecret = secret;
        mErrorCode = errorCode;
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
    public static Intent getErrorCodeIntent(int errorCode) {
        return new IdpResponse(errorCode).toIntent();
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public Intent toIntent() {
        return new Intent().putExtra(ExtraConstants.EXTRA_IDP_RESPONSE, this);
    }

    /**
     * Get the type of provider. e.g. {@link AuthUI#GOOGLE_PROVIDER}
     */
    @NonNull
    @AuthUI.SupportedProvider
    public String getProviderType() {
        return mProviderId;
    }

    /**
     * Get the email used to sign in.
     */
    @Nullable
    public String getEmail() {
        return mEmail;
    }

    /**
     * Get the phone number used to sign in.
     */
    @Nullable
    public String getPhoneNumber() {
        return mPhoneNumber;
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
        return mErrorCode;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(mProviderId);
        dest.writeString(mEmail);
        dest.writeString(mPhoneNumber);
        dest.writeString(mToken);
        dest.writeString(mSecret);
        dest.writeInt(mErrorCode);
    }

    public static final Creator<IdpResponse> CREATOR = new Creator<IdpResponse>() {
        @Override
        public IdpResponse createFromParcel(Parcel in) {
            return new IdpResponse(
                    in.readString(),
                    in.readString(),
                    in.readString(),
                    in.readString(),
                    in.readString(),
                    in.readInt()
            );
        }

        @Override
        public IdpResponse[] newArray(int size) {
            return new IdpResponse[size];
        }
    };

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public static class Builder {
        private String mProviderId;
        private String mEmail;
        private String mPhoneNumber;
        private String mToken;
        private String mSecret;

        public Builder(@AuthUI.SupportedProvider @NonNull String providerId, @Nullable String email) {
            mProviderId = providerId;
            mEmail = email;
        }

        public Builder setPhoneNumber(String phoneNumber) {
            mPhoneNumber = phoneNumber;
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
            if ((mProviderId.equalsIgnoreCase(GoogleAuthProvider.PROVIDER_ID)
                    || mProviderId.equalsIgnoreCase(FacebookAuthProvider.PROVIDER_ID)
                    || mProviderId.equalsIgnoreCase(TwitterAuthProvider.PROVIDER_ID)
                    || mProviderId.equalsIgnoreCase(GithubAuthProvider.PROVIDER_ID))
                    && TextUtils.isEmpty(mToken)) {
                throw new IllegalStateException(
                        "Token cannot be null when using a non-email provider.");
            }
            if (mProviderId.equalsIgnoreCase(TwitterAuthProvider.PROVIDER_ID)
                    && TextUtils.isEmpty(mSecret)) {
                throw new IllegalStateException(
                        "Secret cannot be null when using the Twitter provider.");
            }

            return new IdpResponse(mProviderId, mEmail, mPhoneNumber, mToken, mSecret, ResultCodes.OK);
        }
    }
}
