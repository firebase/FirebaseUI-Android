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
 * Layout model to help customizing layout of the AuthMethodPickerActivity screen,
 * where the user is presented with a list of sign-in providers to choose from.
 *
 * To create a new instance, use {@link AuthMethodPickerLayout.Builder}.
 */
public class AuthMethodPickerLayout implements Parcelable {

    @LayoutRes
    private int mainLayout;

    @IdRes
    private int tosPpView = -1;

    /**
     * PROVIDER_ID -> IdRes of the Button
     */
    private Map<String, Integer> providersButton;

    private AuthMethodPickerLayout() {}

    private AuthMethodPickerLayout(@NonNull Parcel in) {
        this.mainLayout = in.readInt();
        this.tosPpView = in.readInt();

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

    @IdRes
    public int getTosPpView() {
        return tosPpView;
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
        parcel.writeInt(tosPpView);

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
     * Builder for {@link AuthMethodPickerLayout}.
     */
    public static class Builder {

        private Map<String, Integer> providersMapping;
        private AuthMethodPickerLayout instance;

        /**
         * Create a new builder, specifying the ID of the XML layout resource to be sued.
         */
        public Builder(@LayoutRes int mainLayout) {
            instance = new AuthMethodPickerLayout();
            instance.mainLayout = mainLayout;
            providersMapping = new HashMap<>();
        }

        /**
         * Set the ID of the Google sign in button in the custom layout.
         */
        public AuthMethodPickerLayout.Builder setGoogleButtonId(@IdRes int googleBtn) {
            providersMapping.put(GoogleAuthProvider.PROVIDER_ID, googleBtn);
            return this;
        }

        /**
         * Set the ID of the Facebook sign in button in the custom layout.
         */
        public AuthMethodPickerLayout.Builder setFacebookButtonId(@IdRes int facebookBtn) {
            providersMapping.put(FacebookAuthProvider.PROVIDER_ID, facebookBtn);
            return this;
        }

        /**
         * Set the ID of the Email sign in button in the custom layout.
         */
        public AuthMethodPickerLayout.Builder setEmailButtonId(@IdRes int emailButton) {
            providersMapping.put(EmailAuthProvider.PROVIDER_ID, emailButton);
            return this;
        }

        /**
         * Set the ID of the Github sign in button in the custom layout.
         */
        public AuthMethodPickerLayout.Builder setGithubButtonId(@IdRes int githubButton) {
            providersMapping.put(GithubAuthProvider.PROVIDER_ID, githubButton);
            return this;
        }

        /**
         * Set the ID of the Phone Number sign in button in the custom layout.
         */
        public AuthMethodPickerLayout.Builder setPhoneButtonId(@IdRes int phoneButton) {
            providersMapping.put(PhoneAuthProvider.PROVIDER_ID, phoneButton);
            return this;
        }

        /**
         * Set the ID of the Anonymous sign in button in the custom layout.
         */
        public AuthMethodPickerLayout.Builder setAnonymousButtonId(@IdRes int anonymousButton) {
            providersMapping.put(AuthUI.ANONYMOUS_PROVIDER, anonymousButton);
            return this;
        }

        /**
         * Set the ID of a TextView where terms of service and privacy policy should be
         * displayed.
         */
        public AuthMethodPickerLayout.Builder setTosAndPrivacyPolicyId(@IdRes int tosPpView) {
            instance.tosPpView = tosPpView;
            return this;
        }

        public AuthMethodPickerLayout build() {
            if (providersMapping.isEmpty()) {
                throw new IllegalArgumentException("Must configure at least one button.");
            }

            for (String key : providersMapping.keySet()) {
                if (!AuthUI.SUPPORTED_PROVIDERS.contains(key)) {
                    throw new IllegalArgumentException("Unknown provider: " + key);
                }
            }

            instance.providersButton = providersMapping;
            return instance;
        }
    }
}
