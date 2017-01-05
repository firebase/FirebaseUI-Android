package com.firebase.ui.auth.ui;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RestrictTo;

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class User implements Parcelable {
    public static final Parcelable.Creator<User> CREATOR = new Parcelable.Creator<User>() {
        @Override
        public User createFromParcel(Parcel in) {
            return new User(in.readString(),
                            in.readString(),
                            in.readString(),
                            in.<Uri>readParcelable(Uri.class.getClassLoader()));
        }

        @Override
        public User[] newArray(int size) {
            return new User[size];
        }
    };

    private String mEmail;
    private String mName;
    private String mProvider;
    private Uri mPhotoUri;

    private User(String email, String name, String provider, Uri photoUri) {
        mEmail = email;
        mName = name;
        mProvider = provider;
        mPhotoUri = photoUri;
    }

    public static User getUser(Intent intent) {
        return intent.getParcelableExtra(ExtraConstants.EXTRA_USER);
    }

    public static User getUser(Bundle arguments) {
        return arguments.getParcelable(ExtraConstants.EXTRA_USER);
    }

    @NonNull
    public String getEmail() {
        return mEmail;
    }

    @Nullable
    public String getName() {
        return mName;
    }

    @Nullable
    public String getProvider() {
        return mProvider;
    }

    @Nullable
    public Uri getPhotoUri() {
        return mPhotoUri;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        dest.writeString(mEmail);
        dest.writeString(mName);
        dest.writeString(mProvider);
        dest.writeParcelable(mPhotoUri, flags);
    }

    public static final class Builder {
        private String mEmail;
        private String mName;
        private String mProvider;
        private Uri mPhotoUri;

        public Builder(@NonNull String email) {
            mEmail = email;
        }

        public Builder setName(String name) {
            mName = name;
            return this;
        }

        public Builder setProvider(String provider) {
            mProvider = provider;
            return this;
        }

        public Builder setPhotoUri(Uri photoUri) {
            mPhotoUri = photoUri;
            return this;
        }

        public User build() {
            return new User(mEmail, mName, mProvider, mPhotoUri);
        }
    }
}
