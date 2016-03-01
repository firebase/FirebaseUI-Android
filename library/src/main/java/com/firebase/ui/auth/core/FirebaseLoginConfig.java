package com.firebase.ui.auth.core;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class FirebaseLoginConfig {
    public final boolean isPasswordProviderEnabled;
    public final boolean isGoogleProviderEnabled;
    public final boolean isFacebookProviderEnabled;
    public final boolean isTwitterProviderEnabled;
    public List<String> facebookPermissions;

    private FirebaseLoginConfig(boolean isPasswordProviderEnabled,
                                boolean isGoogleProviderEnabled,
                                boolean isFacebookProviderEnabled,
                                boolean isTwitterProviderEnabled,
                                @Nullable List<String> facebookPermissions){
        this.isPasswordProviderEnabled = isPasswordProviderEnabled;
        this.isGoogleProviderEnabled = isGoogleProviderEnabled;
        this.isFacebookProviderEnabled = isFacebookProviderEnabled;
        this.isTwitterProviderEnabled = isTwitterProviderEnabled;
        if (facebookPermissions == null) {
            this.facebookPermissions = new ArrayList<>(Arrays.asList("public_profile"));
        } else {
            this.facebookPermissions = facebookPermissions;
        }
    }

    public static class Builder {
        private boolean mIsPasswordProviderEnabled;
        private boolean mIsGoogleProviderEnabled;
        private boolean mIsFacebookProviderEnabled;
        private boolean mIsTwitterProviderEnabled;
        public List<String> mFacebookPermissions;

        public Builder setGoogleProviderEnabled(boolean isGoogleProviderEnabled) {
            this.mIsGoogleProviderEnabled = isGoogleProviderEnabled;
            return this;
        }

        public Builder setTwitterProviderEnabled(boolean isTwitterProviderEnabled) {
            this.mIsTwitterProviderEnabled = isTwitterProviderEnabled;
            return this;
        }

        public Builder setPasswordProviderEnabled(boolean isPasswordProviderEnabled) {
            mIsPasswordProviderEnabled = isPasswordProviderEnabled;
            return this;
        }

        public Builder setFacebookProviderEnabled(boolean isFacebookProviderEnabled) {
            this.mIsFacebookProviderEnabled = isFacebookProviderEnabled;
            return this;
        }

        public Builder setFacebookPermissions(@NonNull List<String> mFacebookPermissions) {
            this.mFacebookPermissions = mFacebookPermissions;
            return this;
        }

        public FirebaseLoginConfig build() {
            if (mFacebookPermissions != null && !mIsFacebookProviderEnabled) {
                throw new IllegalArgumentException("Facebook provider needs to be enabled if Facebook permissions are set.");
            }
            return new FirebaseLoginConfig(mIsPasswordProviderEnabled, mIsGoogleProviderEnabled,
                    mIsFacebookProviderEnabled, mIsTwitterProviderEnabled, mFacebookPermissions);
        }
    }
}
