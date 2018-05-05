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

import com.firebase.ui.auth.data.model.User;
import com.firebase.ui.auth.util.ExtraConstants;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.auth.TwitterAuthProvider;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;

/**
 * A container that encapsulates the result of authenticating with an Identity Provider.
 */
public class IdpResponse implements Parcelable {
    public static final Creator<IdpResponse> CREATOR = new Creator<IdpResponse>() {
        @Override
        public IdpResponse createFromParcel(Parcel in) {
            return new IdpResponse(
                    in.<User>readParcelable(User.class.getClassLoader()),
                    in.readString(),
                    in.readString(),
                    (FirebaseUiException) in.readSerializable()
            );
        }

        @Override
        public IdpResponse[] newArray(int size) {
            return new IdpResponse[size];
        }
    };

    private final User mUser;

    private final String mToken;
    private final String mSecret;

    private final FirebaseUiException mException;

    private IdpResponse(@NonNull FirebaseUiException e) {
        this(null, null, null, e);
    }

    private IdpResponse(
            @NonNull User user,
            @Nullable String token,
            @Nullable String secret) {
        this(user, token, secret, null);
    }

    private IdpResponse(
            User user,
            String token,
            String secret,
            FirebaseUiException e) {
        mUser = user;
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
    public static IdpResponse fromResultIntent(@Nullable Intent resultIntent) {
        if (resultIntent != null) {
            return resultIntent.getParcelableExtra(ExtraConstants.IDP_RESPONSE);
        } else {
            return null;
        }
    }

    @NonNull
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public static IdpResponse from(@NonNull Exception e) {
        if (e instanceof FirebaseUiException) {
            return new IdpResponse((FirebaseUiException) e);
        } else {
            FirebaseUiException wrapped = new FirebaseUiException(ErrorCodes.UNKNOWN_ERROR, e);
            wrapped.setStackTrace(e.getStackTrace());
            return new IdpResponse(wrapped);
        }
    }

    @NonNull
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public static Intent getErrorIntent(@NonNull Exception e) {
        return from(e).toIntent();
    }

    @NonNull
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public Intent toIntent() {
        return new Intent().putExtra(ExtraConstants.IDP_RESPONSE, this);
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public boolean isSuccessful() {
        return mException == null;
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public User getUser() {
        return mUser;
    }

    /**
     * Get the type of provider. e.g. {@link GoogleAuthProvider#PROVIDER_ID}
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
     * Get the error for a failed sign in.
     */
    @Nullable
    public FirebaseUiException getError() {
        return mException;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeParcelable(mUser, flags);
        dest.writeString(mToken);
        dest.writeString(mSecret);

        ObjectOutputStream oos = null;
        try {
            oos = new ObjectOutputStream(new ByteArrayOutputStream());
            oos.writeObject(mException);

            // Success! The entire exception tree is serializable.
            dest.writeSerializable(mException);
        } catch (IOException e) {
            // Somewhere down the line, the exception is holding on to an object that isn't
            // serializable so default to some exception. It's the best we can do in this case.
            FirebaseUiException fake = new FirebaseUiException(ErrorCodes.UNKNOWN_ERROR,
                    "Fake exception created, original: " + mException
                            + ", original cause: " + mException.getCause());
            fake.setStackTrace(mException.getStackTrace());
            dest.writeSerializable(fake);
        } finally {
            if (oos != null) {
                try {
                    oos.close();
                } catch (IOException ignored) {}
            }
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        IdpResponse response = (IdpResponse) o;

        return (mUser == null ? response.mUser == null : mUser.equals(response.mUser))
                && (mToken == null ? response.mToken == null : mToken.equals(response.mToken))
                && (mSecret == null ? response.mSecret == null : mSecret.equals(response.mSecret))
                && (mException == null ? response.mException == null : mException.equals(response.mException));
    }

    @Override
    public int hashCode() {
        int result = mUser == null ? 0 : mUser.hashCode();
        result = 31 * result + (mToken == null ? 0 : mToken.hashCode());
        result = 31 * result + (mSecret == null ? 0 : mSecret.hashCode());
        result = 31 * result + (mException == null ? 0 : mException.hashCode());
        return result;
    }

    @Override
    public String toString() {
        return "IdpResponse{" +
                "mUser=" + mUser +
                ", mToken='" + mToken + '\'' +
                ", mSecret='" + mSecret + '\'' +
                ", mException=" + mException +
                '}';
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public static class Builder {
        private final User mUser;

        private String mToken;
        private String mSecret;

        public Builder(@NonNull User user) {
            mUser = user;
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
            if (!AuthUI.SUPPORTED_PROVIDERS.contains(providerId)) {
                throw new IllegalStateException("Unknown provider: " + providerId);
            }
            if (AuthUI.SOCIAL_PROVIDERS.contains(providerId) && TextUtils.isEmpty(mToken)) {
                throw new IllegalStateException(
                        "Token cannot be null when using a non-email provider.");
            }
            if (providerId.equals(TwitterAuthProvider.PROVIDER_ID)
                    && TextUtils.isEmpty(mSecret)) {
                throw new IllegalStateException(
                        "Secret cannot be null when using the Twitter provider.");
            }

            return new IdpResponse(mUser, mToken, mSecret);
        }
    }
}
