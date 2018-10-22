package com.firebase.ui.auth;

import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.IdRes;
import android.support.annotation.LayoutRes;
import android.support.annotation.NonNull;
import com.google.firebase.auth.*;

import java.util.HashMap;
import java.util.Map;

/**
 * Layout model to help customizing the AuthPicker
 */
public class AuthLayout implements Parcelable {

    @LayoutRes
    private int mainLayout;

    /**
     * PROVIDER_ID -> IdRes of the Button
     */
    private Map<String, Integer> providersButton;

    private AuthLayout() {
    }

    private AuthLayout(@NonNull Parcel in) {
        this.mainLayout = in.readInt();
        Bundle buttonsBundle = in.readBundle(getClass().getClassLoader());
        this.providersButton = new HashMap<>();

        for (String key : buttonsBundle.keySet()) {
            this.providersButton.put(key, buttonsBundle.getInt(key));
        }
    }

    public int getMainLayout() {
        return mainLayout;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int flags) {
        parcel.writeInt(mainLayout);

        Bundle bundle = new Bundle();
        for (String key : providersButton.keySet()) {
            bundle.putInt(key, providersButton.get(key));
        }
        parcel.writeBundle(bundle);
    }

    public static final Creator<AuthLayout> CREATOR = new Creator<AuthLayout>() {

        @Override
        public AuthLayout createFromParcel(Parcel in) {
            return new AuthLayout(in);
        }

        @Override
        public AuthLayout[] newArray(int size) {
            return new AuthLayout[size];
        }
    };

    /**
     * Builder for AuthLayout
     */
    public static class Builder {

        private Map<String, Integer> providersMapping;
        private AuthLayout instance;

        public Builder(@LayoutRes int mainLayout) {
            instance = new AuthLayout();
            instance.mainLayout = mainLayout;
            providersMapping = new HashMap<>();
        }

        public AuthLayout.Builder setupGoogleButton(@IdRes int googleBtn) {
            providersMapping.put(GoogleAuthProvider.PROVIDER_ID, googleBtn);
            return this;
        }

        public AuthLayout.Builder setupFacebookButton(@IdRes int facebookBtn) {
            providersMapping.put(FacebookAuthProvider.PROVIDER_ID, facebookBtn);
            return this;
        }

        public AuthLayout.Builder setupTwitterButton(@IdRes int twitterBtn) {
            providersMapping.put(TwitterAuthProvider.PROVIDER_ID, twitterBtn);
            return this;
        }

        public AuthLayout.Builder setupEmailButton(@IdRes int emailButton) {
            providersMapping.put(EmailAuthProvider.PROVIDER_ID, emailButton);
            return this;
        }

        public AuthLayout.Builder setupPhoneButton(@IdRes int phoneButton) {
            providersMapping.put(PhoneAuthProvider.PROVIDER_ID, phoneButton);
            return this;
        }

        public AuthLayout.Builder setupAnonymousButton(@IdRes int anonymousButton) {
            providersMapping.put(AuthUI.ANONYMOUS_PROVIDER, anonymousButton);
            return this;
        }

        public AuthLayout build() {
            instance.providersButton = providersMapping;
            return instance;
        }
    }
}
