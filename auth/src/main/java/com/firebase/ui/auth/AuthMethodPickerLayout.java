package com.firebase.ui.auth;

import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.IdRes;
import android.support.annotation.LayoutRes;
import android.support.annotation.NonNull;
import com.google.firebase.auth.*;

import java.util.*;

/**
 * Layout model to help customizing the AuthPicker
 */
public class AuthMethodPickerLayout implements Parcelable {

    @LayoutRes
    private int mainLayout;

    /**
     * PROVIDER_ID -> IdRes of the Button
     */
    private Map<String, Integer> providersButton;

    private AuthMethodPickerLayout() {
    }

    private AuthMethodPickerLayout(@NonNull Parcel in) {
        this.mainLayout = in.readInt();
        Bundle buttonsBundle = in.readBundle(getClass().getClassLoader());
        this.providersButton = new HashMap<>();

        for (String key : buttonsBundle.keySet()) {
            this.providersButton.put(key, buttonsBundle.getInt(key));
        }
    }

    @LayoutRes
    public int getMainLayout() {
        return mainLayout;
    }

    public Map<String, Integer> getProvidersButton() {
        return providersButton;
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

    public static final Creator<AuthMethodPickerLayout> CREATOR = new Creator<AuthMethodPickerLayout>() {

        @Override
        public AuthMethodPickerLayout createFromParcel(Parcel in) {
            return new AuthMethodPickerLayout(in);
        }

        @Override
        public AuthMethodPickerLayout[] newArray(int size) {
            return new AuthMethodPickerLayout[size];
        }
    };

    /**
     * Builder for AuthMethodPickerLayout
     */
    public static class Builder {

        private Map<String, Integer> providersMapping;
        private AuthMethodPickerLayout instance;

        public Builder(@LayoutRes int mainLayout) {
            instance = new AuthMethodPickerLayout();
            instance.mainLayout = mainLayout;
            providersMapping = new HashMap<>();
        }

        public AuthMethodPickerLayout.Builder setupGoogleButtonId(@IdRes int googleBtn) {
            providersMapping.put(GoogleAuthProvider.PROVIDER_ID, googleBtn);
            return this;
        }

        public AuthMethodPickerLayout.Builder setupFacebookButtonId(@IdRes int facebookBtn) {
            providersMapping.put(FacebookAuthProvider.PROVIDER_ID, facebookBtn);
            return this;
        }

        public AuthMethodPickerLayout.Builder setupTwitterButtonId(@IdRes int twitterBtn) {
            providersMapping.put(TwitterAuthProvider.PROVIDER_ID, twitterBtn);
            return this;
        }

        public AuthMethodPickerLayout.Builder setupEmailButtonId(@IdRes int emailButton) {
            providersMapping.put(EmailAuthProvider.PROVIDER_ID, emailButton);
            return this;
        }

        public AuthMethodPickerLayout.Builder setupPhoneButtonId(@IdRes int phoneButton) {
            providersMapping.put(PhoneAuthProvider.PROVIDER_ID, phoneButton);
            return this;
        }

        public AuthMethodPickerLayout.Builder setupAnonymousButtonId(@IdRes int anonymousButton) {
            providersMapping.put(AuthUI.ANONYMOUS_PROVIDER, anonymousButton);
            return this;
        }

        public AuthMethodPickerLayout build() {
            //Validating the button set
            for (String key : providersMapping.keySet()) {
                if (AuthUI.SUPPORTED_PROVIDERS.contains(key)) {
                    throw new IllegalArgumentException("Unknown provider: " + key);
                }
            }
            instance.providersButton = providersMapping;
            return instance;
        }
    }
}
