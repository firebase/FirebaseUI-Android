package com.firebase.ui.auth.core;

import android.support.annotation.NonNull;

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
                                List<String> facebookPermissions){
        this.isPasswordProviderEnabled = isPasswordProviderEnabled;
        this.isGoogleProviderEnabled = isGoogleProviderEnabled;
        this.isFacebookProviderEnabled = isFacebookProviderEnabled;
        this.isTwitterProviderEnabled = isTwitterProviderEnabled;
        this.facebookPermissions = facebookPermissions;
    }

    public static class Builder {
        private boolean mIsPasswordProviderEnabled;
        private boolean mIsGoogleProviderEnabled;
        private boolean mIsFacebookProviderEnabled;
        private boolean mIsTwitterProviderEnabled;

        public Builder setFacebookPermissions(@NonNull List<String> mFacebookPermissions) {
            this.mFacebookPermissions = mFacebookPermissions;
            return this;
        }

        public List<String> mFacebookPermissions = new ArrayList<>(Arrays.asList("public_profile"));

        public Builder setGoogleProviderEnabled(boolean isGoogleProviderEnabled) {
            this.mIsGoogleProviderEnabled = isGoogleProviderEnabled;
            return this;
        }

        public Builder setFacebookProviderEnabled(boolean isFacebookProviderEnabled) {
            this.mIsFacebookProviderEnabled = isFacebookProviderEnabled;
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

        public FirebaseLoginConfig build() {
            return new FirebaseLoginConfig(mIsPasswordProviderEnabled, mIsGoogleProviderEnabled,
                    mIsFacebookProviderEnabled, mIsTwitterProviderEnabled, mFacebookPermissions);
        }
    }
}
