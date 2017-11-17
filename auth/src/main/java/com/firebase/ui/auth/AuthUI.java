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

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.CallSuper;
import android.support.annotation.DrawableRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RestrictTo;
import android.support.annotation.StringDef;
import android.support.annotation.StyleRes;
import android.text.TextUtils;

import com.facebook.login.LoginManager;
import com.firebase.ui.auth.provider.TwitterProvider;
import com.firebase.ui.auth.ui.ExtraConstants;
import com.firebase.ui.auth.ui.FlowParameters;
import com.firebase.ui.auth.ui.idp.AuthMethodPickerActivity;
import com.firebase.ui.auth.util.GoogleApiUtils;
import com.firebase.ui.auth.util.Preconditions;
import com.firebase.ui.auth.util.signincontainer.SaveSmartLock;
import com.google.android.gms.auth.api.credentials.Credential;
import com.google.android.gms.auth.api.credentials.CredentialsClient;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.CommonStatusCodes;
import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FacebookAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidUserException;
import com.google.firebase.auth.FirebaseAuthProvider;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.auth.PhoneAuthProvider;
import com.google.firebase.auth.TwitterAuthProvider;
import com.google.firebase.auth.UserInfo;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;

/**
 * The entry point to the AuthUI authentication flow, and related utility methods. If your
 * application uses the default {@link FirebaseApp} instance, an AuthUI instance can be retrieved
 * simply by calling {@link AuthUI#getInstance()}. If an alternative app instance is in use, call
 * {@link AuthUI#getInstance(FirebaseApp)} instead, passing the appropriate app instance.
 * <p>
 * <p>
 * See the <a href="https://github.com/firebase/FirebaseUI-Android/blob/master/auth/README.md#table-of-contents">README</a>
 * for examples on how to get started with FirebaseUI Auth.
 */
public class AuthUI {
    @StringDef({
                       EmailAuthProvider.PROVIDER_ID, EMAIL_PROVIDER,
                       PhoneAuthProvider.PROVIDER_ID, PHONE_VERIFICATION_PROVIDER,
                       GoogleAuthProvider.PROVIDER_ID, GOOGLE_PROVIDER,
                       FacebookAuthProvider.PROVIDER_ID, FACEBOOK_PROVIDER,
                       TwitterAuthProvider.PROVIDER_ID, TWITTER_PROVIDER
               })
    @Retention(RetentionPolicy.SOURCE)
    public @interface SupportedProvider {}

    /**
     * Provider identifier for email and password credentials, for use with {@link
     * SignInIntentBuilder#setAvailableProviders(List)}.
     */
    public static final String EMAIL_PROVIDER = EmailAuthProvider.PROVIDER_ID;

    /**
     * Provider identifier for Google, for use with {@link SignInIntentBuilder#setAvailableProviders(List)}.
     */
    public static final String GOOGLE_PROVIDER = GoogleAuthProvider.PROVIDER_ID;

    /**
     * Provider identifier for Facebook, for use with {@link SignInIntentBuilder#setAvailableProviders(List)}.
     */
    public static final String FACEBOOK_PROVIDER = FacebookAuthProvider.PROVIDER_ID;

    /**
     * Provider identifier for Twitter, for use with {@link SignInIntentBuilder#setAvailableProviders(List)}.
     */
    public static final String TWITTER_PROVIDER = TwitterAuthProvider.PROVIDER_ID;

    /**
     * Provider identifier for Phone, for use with {@link SignInIntentBuilder#setAvailableProviders(List)}.
     */
    public static final String PHONE_VERIFICATION_PROVIDER = PhoneAuthProvider.PROVIDER_ID;

    /**
     * Bundle key for the default full phone number parameter.
     */
    public static final String EXTRA_DEFAULT_PHONE_NUMBER = ExtraConstants.EXTRA_PHONE;

    /**
     * Bundle key for the default phone country code parameter.
     */
    public static final String EXTRA_DEFAULT_COUNTRY_CODE = ExtraConstants.EXTRA_COUNTRY_CODE;

    /**
     * Bundle key for the default national phone number parameter.
     */
    public static final String EXTRA_DEFAULT_NATIONAL_NUMBER = ExtraConstants.EXTRA_NATIONAL_NUMBER;

    /**
     * Default value for logo resource, omits the logo from the {@link AuthMethodPickerActivity}.
     */
    public static final int NO_LOGO = -1;

    /**
     * The set of authentication providers supported in Firebase Auth UI.
     */
    public static final Set<String> SUPPORTED_PROVIDERS =
            Collections.unmodifiableSet(new HashSet<>(Arrays.asList(
                    EMAIL_PROVIDER,
                    GOOGLE_PROVIDER,
                    FACEBOOK_PROVIDER,
                    TWITTER_PROVIDER,
                    PHONE_VERIFICATION_PROVIDER
            )));

    /**
     * The set of social authentication providers supported in Firebase Auth UI.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public static final Set<String> SOCIAL_PROVIDERS =
            Collections.unmodifiableSet(new HashSet<>(Arrays.asList(
                    GoogleAuthProvider.PROVIDER_ID,
                    FacebookAuthProvider.PROVIDER_ID,
                    TwitterAuthProvider.PROVIDER_ID)));

    private static final IdentityHashMap<FirebaseApp, AuthUI> INSTANCES = new IdentityHashMap<>();

    private final FirebaseApp mApp;
    private final FirebaseAuth mAuth;

    private AuthUI(FirebaseApp app) {
        mApp = app;
        mAuth = FirebaseAuth.getInstance(mApp);

        mAuth.useAppLanguage();
    }

    /**
     * Retrieves the {@link AuthUI} instance associated with the default app, as returned by {@code
     * FirebaseApp.getInstance()}.
     *
     * @throws IllegalStateException if the default app is not initialized.
     */
    @NonNull
    public static AuthUI getInstance() {
        return getInstance(FirebaseApp.getInstance());
    }

    /**
     * Retrieves the {@link AuthUI} instance associated the the specified app.
     */
    @NonNull
    public static AuthUI getInstance(@NonNull FirebaseApp app) {
        AuthUI authUi;
        synchronized (INSTANCES) {
            authUi = INSTANCES.get(app);
            if (authUi == null) {
                authUi = new AuthUI(app);
                INSTANCES.put(app, authUi);
            }
        }
        return authUi;
    }

    /**
     * Default theme used by {@link SignInIntentBuilder#setTheme(int)} if no theme customization is
     * required.
     */
    @StyleRes
    public static int getDefaultTheme() {
        return R.style.FirebaseUI;
    }

    /**
     * Signs the current user out, if one is signed in.
     *
     * @param context the context requesting the user be signed out
     * @return A task which, upon completion, signals that the user has been signed out ({@link
     * Task#isSuccessful()}, or that the sign-out attempt failed unexpectedly !{@link
     * Task#isSuccessful()}).
     */
    @NonNull
    public Task<Void> signOut(@NonNull Context context) {
        mAuth.signOut();
        return Tasks.whenAll(
                signOutIdps(context),
                GoogleApiUtils.getCredentialsClient(context).disableAutoSignIn());
    }

    /**
     * Delete the use from FirebaseAuth and delete any associated credentials from the Credentials
     * API. Returns a {@link Task} that succeeds if the Firebase Auth user deletion succeeds and
     * fails if the Firebase Auth deletion fails. Credentials deletion failures are handled
     * silently.
     *
     * @param context the calling {@link Context}.
     */
    @NonNull
    public Task<Void> delete(@NonNull Context context) {
        final FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            return Tasks.forException(new FirebaseAuthInvalidUserException(
                    String.valueOf(CommonStatusCodes.SIGN_IN_REQUIRED),
                    "No currently signed in user."));
        }

        final List<Credential> credentials = getCredentialsFromFirebaseUser(currentUser);
        final CredentialsClient client = GoogleApiUtils.getCredentialsClient(context);

        // Ensure the order in which tasks are executed properly destructures the user.
        return signOutIdps(context).continueWithTask(new Continuation<Void, Task<Void>>() {
            @Override
            public Task<Void> then(@NonNull Task<Void> task) {
                task.getResult(); // Propagate exception if there was one
                return currentUser.delete();
            }
        }).continueWithTask(new Continuation<Void, Task<Void>>() {
            @Override
            public Task<Void> then(@NonNull Task<Void> task) {
                task.getResult(); // Propagate exception if there was one

                List<Task<?>> credentialTasks = new ArrayList<>();
                for (Credential credential : credentials) {
                    credentialTasks.add(client.delete(credential));
                }
                return Tasks.whenAll(credentialTasks)
                        .continueWithTask(new Continuation<Void, Task<Void>>() {
                            @Override
                            public Task<Void> then(@NonNull Task<Void> task) {
                                Exception e = task.getException();
                                Throwable t = e == null ? null : e.getCause();
                                if (!(t instanceof ApiException)
                                        || ((ApiException) t).getStatusCode() != CommonStatusCodes.CANCELED) {
                                    // Only propagate the exception if it isn't an invalid account
                                    // one. This can occur if we failed to save the credential or it
                                    // was deleted elsewhere. However, a lack of stored credential
                                    // doesn't mean fully deleting the user failed.
                                    task.getResult();
                                }

                                return Tasks.forResult(null);
                            }
                        });
            }
        });
    }

    private Task<Void> signOutIdps(@NonNull Context context) {
        try {
            LoginManager.getInstance().logOut();
            TwitterProvider.signOut(context);
        } catch (NoClassDefFoundError e) {
            // Do nothing: this is perfectly fine if the dev doesn't include Facebook/Twitter
            // support
        }

        return GoogleSignIn.getClient(context, GoogleSignInOptions.DEFAULT_SIGN_IN).signOut();
    }

    /**
     * Make a list of {@link Credential} from a FirebaseUser. Useful for deleting Credentials, not
     * for saving since we don't have access to the password.
     */
    private static List<Credential> getCredentialsFromFirebaseUser(@NonNull FirebaseUser user) {
        if (TextUtils.isEmpty(user.getEmail()) && TextUtils.isEmpty(user.getPhoneNumber())) {
            return Collections.emptyList();
        }

        List<Credential> credentials = new ArrayList<>();
        for (UserInfo userInfo : user.getProviderData()) {
            if (FirebaseAuthProvider.PROVIDER_ID.equals(userInfo.getProviderId())) {
                continue;
            }

            String type = SaveSmartLock.providerIdToAccountType(userInfo.getProviderId());

            credentials.add(new Credential.Builder(
                    user.getEmail() == null ? user.getPhoneNumber() : user.getEmail())
                    .setAccountType(type)
                    .build());
        }

        return credentials;
    }

    /**
     * Starts the process of creating a sign in intent, with the mandatory application context
     * parameter.
     */
    @NonNull
    public SignInIntentBuilder createSignInIntentBuilder() {
        return new SignInIntentBuilder();
    }

    /**
     * Configuration for an identity provider.
     * <p>
     * In the simplest case, you can supply the provider ID and build the config like this: {@code
     * new IdpConfig.Builder(AuthUI.GOOGLE_PROVIDER).build()}
     */
    public static class IdpConfig implements Parcelable {
        private final String mProviderId;
        private final List<String> mScopes;
        private final Bundle mParams;

        private IdpConfig(
                @SupportedProvider @NonNull String providerId,
                @NonNull List<String> scopes,
                @NonNull Bundle params) {
            mProviderId = providerId;
            mScopes = Collections.unmodifiableList(scopes);
            mParams = params;
        }

        private IdpConfig(Parcel in) {
            mProviderId = in.readString();
            mScopes = Collections.unmodifiableList(in.createStringArrayList());
            mParams = in.readBundle(getClass().getClassLoader());
        }

        @NonNull
        @SupportedProvider
        public String getProviderId() {
            return mProviderId;
        }

        @NonNull
        public List<String> getScopes() {
            return mScopes;
        }

        @NonNull
        public Bundle getParams() {
            return mParams;
        }

        public static final Creator<IdpConfig> CREATOR = new Creator<IdpConfig>() {
            @Override
            public IdpConfig createFromParcel(Parcel in) {
                return new IdpConfig(in);
            }

            @Override
            public IdpConfig[] newArray(int size) {
                return new IdpConfig[size];
            }
        };

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel parcel, int i) {
            parcel.writeString(mProviderId);
            parcel.writeStringList(mScopes);
            parcel.writeBundle(mParams);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            IdpConfig config = (IdpConfig) o;

            return mProviderId.equals(config.mProviderId);
        }

        @Override
        public int hashCode() {
            return mProviderId.hashCode();
        }

        @Override
        public String toString() {
            return "IdpConfig{" +
                    "mProviderId='" + mProviderId + '\'' +
                    ", mScopes=" + mScopes +
                    ", mParams=" + mParams +
                    '}';
        }

        public static class Builder {
            @SupportedProvider private String mProviderId;
            private List<String> mScopes = new ArrayList<>();
            private Bundle mParams = new Bundle();

            /**
             * Builds the configuration parameters for an identity provider.
             *
             * @param providerId An ID of one of the supported identity providers. e.g. {@link
             *                   AuthUI#GOOGLE_PROVIDER}. See {@link AuthUI#SUPPORTED_PROVIDERS} for
             *                   the complete list of supported Identity providers
             */
            public Builder(@SupportedProvider @NonNull String providerId) {
                if (!SUPPORTED_PROVIDERS.contains(providerId)) {
                    throw new IllegalArgumentException("Unknown provider: " + providerId);
                }
                mProviderId = providerId;
            }

            /**
             * Specifies the additional permissions that the application will request for this
             * identity provider.
             * <p>
             * For Facebook permissions see:
             * https://developers.facebook.com/docs/facebook-login/android
             * https://developers.facebook.com/docs/facebook-login/permissions
             * <p>
             * For Google permissions see:
             * https://developers.google.com/identity/protocols/googlescopes
             * <p>
             * Twitter permissions are only configurable through the
             * <a href="https://apps.twitter.com/">Twitter developer console</a>.
             */
            @NonNull
            public Builder setPermissions(@Nullable List<String> permissions) {
                mScopes = permissions;
                return this;
            }

            @NonNull
            public Builder setParams(@Nullable Bundle params) {
                mParams = params;
                return this;
            }

            @NonNull
            public IdpConfig build() {
                mScopes = mScopes == null ? Collections.<String>emptyList() : mScopes;
                mParams = mParams == null ? new Bundle() : mParams;

                return new IdpConfig(mProviderId, mScopes, mParams);
            }
        }
    }

    /**
     * Base builder for both {@link SignInIntentBuilder}.
     */
    @SuppressWarnings(value = "unchecked")
    private abstract class AuthIntentBuilder<T extends AuthIntentBuilder> {
        int mLogo = NO_LOGO;
        int mTheme = getDefaultTheme();
        List<IdpConfig> mProviders = new ArrayList<>();
        String mTosUrl;
        String mPrivacyPolicyUrl;
        boolean mEnableCredentials = true;
        boolean mEnableHints = true;

        private AuthIntentBuilder() {}

        /**
         * Specifies the theme to use for the application flow. If no theme is specified, a default
         * theme will be used.
         */
        @NonNull
        public T setTheme(@StyleRes int theme) {
            Preconditions.checkValidStyle(
                    mApp.getApplicationContext(),
                    theme,
                    "theme identifier is unknown or not a style definition");
            mTheme = theme;
            return (T) this;
        }

        /**
         * Specifies the logo to use for the {@link AuthMethodPickerActivity}. If no logo is
         * specified, none will be used.
         */
        @NonNull
        public T setLogo(@DrawableRes int logo) {
            mLogo = logo;
            return (T) this;
        }

        /**
         * Specifies the terms-of-service URL for the application.
         */
        @NonNull
        public T setTosUrl(@Nullable String tosUrl) {
            mTosUrl = tosUrl;
            return (T) this;
        }

        /**
         * Specifies the privacy policy URL for the application.
         */
        @NonNull
        public T setPrivacyPolicyUrl(@Nullable String privacyPolicyUrl) {
            mPrivacyPolicyUrl = privacyPolicyUrl;
            return (T) this;
        }

        /**
         * Specified the set of supported authentication providers. At least one provider must be
         * specified. There may only be one instance of each provider.
         * <p>
         * <p>If no providers are explicitly specified by calling this method, then the email
         * provider is the default supported provider.
         *
         * @param idpConfigs a list of {@link IdpConfig}s, where each {@link IdpConfig} contains the
         *                   configuration parameters for the IDP.
         * @see IdpConfig
         */
        @NonNull
        public T setAvailableProviders(@NonNull List<IdpConfig> idpConfigs) {
            mProviders.clear();

            for (IdpConfig config : idpConfigs) {
                if (mProviders.contains(config)) {
                    throw new IllegalArgumentException("Each provider can only be set once. "
                            + config.getProviderId()
                            + " was set twice.");
                } else {
                    mProviders.add(config);
                }

                if (config.getProviderId().equals(FACEBOOK_PROVIDER)) {
                    try {
                        Class c = com.facebook.FacebookSdk.class;
                    } catch (NoClassDefFoundError e) {
                        throw new RuntimeException(
                                "Facebook provider cannot be configured " +
                                        "without dependency. Did you forget to add " +
                                        "'com.facebook.android:facebook-android-sdk:VERSION' dependency?");
                    }
                }

                if (config.getProviderId().equals(TWITTER_PROVIDER)) {
                    try {
                        Class c = com.twitter.sdk.android.core.TwitterCore.class;
                    } catch (NoClassDefFoundError e) {
                        throw new RuntimeException(
                                "Twitter provider cannot be configured " +
                                        "without dependency. Did you forget to add " +
                                        "'com.twitter.sdk.android:twitter-core:VERSION' dependency?");
                    }
                }
            }

            return (T) this;
        }

        /**
         * Specified the set of supported authentication providers. At least one provider must be
         * specified. There may only be one instance of each provider.
         * <p>
         * <p>If no providers are explicitly specified by calling this method, then the email
         * provider is the default supported provider.
         *
         * @param idpConfigs a list of {@link IdpConfig}s, where each {@link IdpConfig} contains the
         *                   configuration parameters for the IDP.
         * @see IdpConfig
         * @deprecated because the order in which providers were displayed was the inverse of the
         * order in which they were supplied. Use {@link #setAvailableProviders(List)} to display
         * the providers in the order in which they were supplied.
         */
        @Deprecated
        public T setProviders(@NonNull List<IdpConfig> idpConfigs) {
            setAvailableProviders(idpConfigs);

            // Ensure email provider is at the bottom to keep backwards compatibility
            int emailProviderIndex = mProviders.indexOf(new IdpConfig.Builder(EMAIL_PROVIDER).build());
            if (emailProviderIndex != -1) {
                mProviders.add(0, mProviders.remove(emailProviderIndex));
            }
            Collections.reverse(mProviders);

            return (T) this;
        }

        /**
         * Enables or disables the use of Smart Lock for Passwords in the sign in flow. To
         * (en)disable hint selector and credential selector independently use {@link
         * #setIsSmartLockEnabled(boolean, boolean)}
         * <p>
         * <p>SmartLock is enabled by default.
         *
         * @param enabled enables smartlock's credential selector and hint selector
         */
        @NonNull
        public T setIsSmartLockEnabled(boolean enabled) {
            setIsSmartLockEnabled(enabled, enabled);
            return (T) this;
        }

        /**
         * Enables or disables the use of Smart Lock for Passwords credential selector and hint
         * selector.
         * <p>
         * <p>Both selectors are enabled by default.
         *
         * @param enableCredentials enables credential selector before signup
         * @param enableHints       enable hint selector in respective signup screens
         */
        @NonNull
        public T setIsSmartLockEnabled(boolean enableCredentials, boolean enableHints) {
            mEnableCredentials = enableCredentials;
            mEnableHints = enableHints;
            return (T) this;
        }

        @CallSuper
        @NonNull
        public Intent build() {
            if (mProviders.isEmpty()) {
                mProviders.add(new IdpConfig.Builder(EMAIL_PROVIDER).build());
            }

            return KickoffActivity.createIntent(mApp.getApplicationContext(), getFlowParams());
        }

        protected abstract FlowParameters getFlowParams();
    }

    /**
     * Builder for the intent to start the user authentication flow.
     */
    public final class SignInIntentBuilder extends AuthIntentBuilder<SignInIntentBuilder> {
        private boolean mAllowNewEmailAccounts = true;

        private SignInIntentBuilder() {
            super();
        }

        /**
         * Enables or disables creating new accounts in the email sign in flow.
         * <p>
         * <p>Account creation is enabled by default.
         */
        @NonNull
        public SignInIntentBuilder setAllowNewEmailAccounts(boolean enabled) {
            mAllowNewEmailAccounts = enabled;
            return this;
        }

        @Override
        protected FlowParameters getFlowParams() {
            return new FlowParameters(
                    mApp.getName(),
                    mProviders,
                    mTheme,
                    mLogo,
                    mTosUrl,
                    mPrivacyPolicyUrl,
                    mEnableCredentials,
                    mEnableHints,
                    mAllowNewEmailAccounts);
        }
    }
}
