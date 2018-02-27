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
import android.util.Log;

import com.facebook.login.LoginManager;
import com.firebase.ui.auth.data.model.FlowParameters;
import com.firebase.ui.auth.provider.TwitterProvider;
import com.firebase.ui.auth.ui.idp.AuthMethodPickerActivity;
import com.firebase.ui.auth.util.ExtraConstants;
import com.firebase.ui.auth.util.GoogleApiUtils;
import com.firebase.ui.auth.util.Preconditions;
import com.firebase.ui.auth.util.data.PhoneNumberUtils;
import com.firebase.ui.auth.util.data.ProviderUtils;
import com.google.android.gms.auth.api.credentials.Credential;
import com.google.android.gms.auth.api.credentials.CredentialsClient;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.CommonStatusCodes;
import com.google.android.gms.common.api.Scope;
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

    private static final String TAG = "AuthUI";

    @StringDef({
                       EmailAuthProvider.PROVIDER_ID,
                       PhoneAuthProvider.PROVIDER_ID,
                       GoogleAuthProvider.PROVIDER_ID,
                       FacebookAuthProvider.PROVIDER_ID,
                       TwitterAuthProvider.PROVIDER_ID
               })
    @Retention(RetentionPolicy.SOURCE)
    public @interface SupportedProvider {}

    /**
     * Provider identifier for email and password credentials, for use with {@link
     * SignInIntentBuilder#setAvailableProviders(List)}.
     *
     * @deprecated this constant is no longer needed, use the {@link IdpConfig.EmailBuilder}
     * directly or {@link EmailAuthProvider#PROVIDER_ID} if needed.
     */
    @Deprecated
    public static final String EMAIL_PROVIDER = EmailAuthProvider.PROVIDER_ID;

    /**
     * Provider identifier for Google, for use with {@link SignInIntentBuilder#setAvailableProviders(List)}.
     *
     * @deprecated this constant is no longer needed, use the {@link IdpConfig.GoogleBuilder}
     * directly or {@link GoogleAuthProvider#PROVIDER_ID} if needed.
     */
    @Deprecated
    public static final String GOOGLE_PROVIDER = GoogleAuthProvider.PROVIDER_ID;

    /**
     * Provider identifier for Facebook, for use with {@link SignInIntentBuilder#setAvailableProviders(List)}.
     *
     * @deprecated this constant is no longer needed, use the {@link IdpConfig.FacebookBuilder}
     * directly or {@link FacebookAuthProvider#PROVIDER_ID} if needed.
     */
    @Deprecated
    public static final String FACEBOOK_PROVIDER = FacebookAuthProvider.PROVIDER_ID;

    /**
     * Provider identifier for Twitter, for use with {@link SignInIntentBuilder#setAvailableProviders(List)}.
     *
     * @deprecated this constant is no longer needed, use the {@link IdpConfig.TwitterBuilder}
     * directly or {@link TwitterAuthProvider#PROVIDER_ID} if needed.
     */
    @Deprecated
    public static final String TWITTER_PROVIDER = TwitterAuthProvider.PROVIDER_ID;

    /**
     * Provider identifier for Phone, for use with {@link SignInIntentBuilder#setAvailableProviders(List)}.
     *
     * @deprecated this constant is no longer needed, use the {@link IdpConfig.PhoneBuilder}
     * directly or {@link PhoneAuthProvider#PROVIDER_ID} if needed.
     */
    @Deprecated
    public static final String PHONE_VERIFICATION_PROVIDER = PhoneAuthProvider.PROVIDER_ID;

    /**
     * Bundle key for the default full phone number parameter.
     *
     * @deprecated this constant is no longer needed, use {@link IdpConfig.PhoneBuilder#setDefaultNumber(String)}
     * instead.
     */
    @Deprecated
    public static final String EXTRA_DEFAULT_PHONE_NUMBER = ExtraConstants.EXTRA_PHONE;

    /**
     * Bundle key for the default phone country code parameter.
     *
     * @deprecated this constant is no longer needed, use {@link IdpConfig.PhoneBuilder#setDefaultNumber(String,
     * String)} instead.
     */
    @Deprecated
    public static final String EXTRA_DEFAULT_COUNTRY_CODE = ExtraConstants.EXTRA_COUNTRY_ISO;

    /**
     * Bundle key for the default national phone number parameter.
     *
     * @deprecated this constant is no longer needed, use {@link IdpConfig.PhoneBuilder#setDefaultNumber(String,
     * String)} instead.
     */
    @Deprecated
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
                    EmailAuthProvider.PROVIDER_ID,
                    GoogleAuthProvider.PROVIDER_ID,
                    FacebookAuthProvider.PROVIDER_ID,
                    TwitterAuthProvider.PROVIDER_ID,
                    PhoneAuthProvider.PROVIDER_ID
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

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public static final String UNCONFIGURED_CONFIG_VALUE = "CHANGE-ME";

    private static final IdentityHashMap<FirebaseApp, AuthUI> INSTANCES = new IdentityHashMap<>();

    private static Context sApplicationContext;

    private final FirebaseApp mApp;
    private final FirebaseAuth mAuth;

    private AuthUI(FirebaseApp app) {
        mApp = app;
        mAuth = FirebaseAuth.getInstance(mApp);

        mAuth.setFirebaseUIVersion(BuildConfig.VERSION_NAME);
        mAuth.useAppLanguage();
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public static void setApplicationContext(@NonNull Context context) {
        sApplicationContext = Preconditions.checkNotNull(context, "App context cannot be null.")
                .getApplicationContext();
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    @NonNull
    public static Context getApplicationContext() {
        return sApplicationContext;
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

        Task<Void> maybeDisableAutoSignIn = GoogleApiUtils.getCredentialsClient(context)
                .disableAutoSignIn()
                .continueWithTask(new Continuation<Void, Task<Void>>() {
                    @Override
                    public Task<Void> then(@NonNull Task<Void> task) throws Exception {
                        // We want to ignore a specific exception, since it's not a good reason
                        // to fail (see Issue 1156).
                        if (!task.isSuccessful() && (task.getException() instanceof ApiException)) {
                            ApiException ae = (ApiException) task.getException();
                            if (ae.getStatusCode() == CommonStatusCodes.CANCELED) {
                                Log.w(TAG, "Could not disable auto-sign in, maybe there are no " +
                                    "SmartLock accounts available?", ae);

                                return Tasks.forResult(null);
                            }
                        }

                        return task;
                    }
                });

        return Tasks.whenAll(
                signOutIdps(context),
                maybeDisableAutoSignIn);
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
        final FirebaseUser currentUser = mAuth.getCurrentUser();
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
        } catch (NoClassDefFoundError e) {
            // Do nothing: this is perfectly fine if the dev doesn't include Facebook/Twitter
            // support
        }

        try {
            TwitterProvider.signOut(context);
        } catch (NoClassDefFoundError e) {
            // See comment above
            // Note: we need to have separate try/catch statements since devs can include
            // _either_ one of the providers. If one crashes, we still need to sign out of
            // the other one.
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

            String type = ProviderUtils.providerIdToAccountType(userInfo.getProviderId());

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
     */
    public static class IdpConfig implements Parcelable {
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

        private final String mProviderId;
        private final Bundle mParams;

        private IdpConfig(
                @SupportedProvider @NonNull String providerId,
                @NonNull Bundle params) {
            mProviderId = providerId;
            mParams = new Bundle(params);
        }

        private IdpConfig(Parcel in) {
            mProviderId = in.readString();
            mParams = in.readBundle(getClass().getClassLoader());
        }

        @NonNull
        @SupportedProvider
        public String getProviderId() {
            return mProviderId;
        }

        /**
         * @deprecated use the lists of scopes you passed in directly, or get a provider-specific
         * implementation from {@link #getParams()}.
         */
        @Deprecated
        @NonNull
        public List<String> getScopes() {
            List<String> permissions;
            if (mProviderId.equals(GoogleAuthProvider.PROVIDER_ID)) {
                Scope[] array = ((GoogleSignInOptions)
                        mParams.getParcelable(ExtraConstants.EXTRA_GOOGLE_SIGN_IN_OPTIONS))
                        .getScopeArray();

                List<String> scopes = new ArrayList<>();
                for (Scope scope : array) {
                    scopes.add(scope.toString());
                }
                permissions = scopes;
            } else if (mProviderId.equals(FacebookAuthProvider.PROVIDER_ID)) {
                permissions = mParams.getStringArrayList(ExtraConstants.EXTRA_FACEBOOK_PERMISSIONS);
            } else {
                permissions = null;
            }
            return permissions == null ? Collections.<String>emptyList() : permissions;
        }

        /**
         * @return provider-specific options
         */
        @NonNull
        public Bundle getParams() {
            return new Bundle(mParams);
        }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel parcel, int i) {
            parcel.writeString(mProviderId);
            parcel.writeBundle(mParams);
        }

        @Override
        public final boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            IdpConfig config = (IdpConfig) o;

            return mProviderId.equals(config.mProviderId);
        }

        @Override
        public final int hashCode() {
            return mProviderId.hashCode();
        }

        @Override
        public String toString() {
            return "IdpConfig{" +
                    "mProviderId='" + mProviderId + '\'' +
                    ", mParams=" + mParams +
                    '}';
        }

        /**
         * Base builder for all authentication providers.
         *
         * @see SignInIntentBuilder#setAvailableProviders(List)
         */
        public static class Builder {
            @SupportedProvider private final String mProviderId;
            private final Bundle mParams = new Bundle();

            /**
             * Builds the configuration parameters for an identity provider.
             *
             * @param providerId An ID of one of the supported identity providers. e.g. {@link
             *                   AuthUI#GOOGLE_PROVIDER}. See {@link AuthUI#SUPPORTED_PROVIDERS} for
             *                   the complete list of supported Identity providers
             * @deprecated use the provider's specific builder, for example, {@link GoogleBuilder}
             */
            @Deprecated
            public Builder(@SupportedProvider @NonNull String providerId) {
                if (!SUPPORTED_PROVIDERS.contains(providerId)) {
                    throw new IllegalArgumentException("Unknown provider: " + providerId);
                }
                mProviderId = providerId;
            }

            @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
            @NonNull
            protected Bundle getParams() {
                return mParams;
            }

            /**
             * @deprecated additional phone verification options are now available on the phone
             * builder: {@link PhoneBuilder#setDefaultNumber(String, String)}.
             */
            @NonNull
            @Deprecated
            public Builder setParams(@Nullable Bundle params) {
                mParams.clear();
                mParams.putAll(params == null ? new Bundle() : params);
                return this;
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
             *
             * @deprecated use the provider's specific builder. For Google, use {@link
             * GoogleBuilder#setScopes(List)}. For Facebook, use {@link FacebookBuilder#setPermissions(List)}.
             */
            @NonNull
            @Deprecated
            public Builder setPermissions(@Nullable List<String> permissions) {
                if (permissions == null) {
                    mParams.clear();
                    return this;
                }

                Bundle params;
                if (mProviderId.equals(GOOGLE_PROVIDER)) {
                    params = new GoogleBuilder().setScopes(permissions).build().getParams();
                } else if (mProviderId.equals(FACEBOOK_PROVIDER)) {
                    params = new FacebookBuilder().setPermissions(permissions).build().getParams();
                } else {
                    params = new Bundle();
                }
                setParams(params);
                return this;
            }

            @CallSuper
            @NonNull
            public IdpConfig build() {
                // Ensures deprecated Google provider builder backcompat
                if (mProviderId.equals(GoogleAuthProvider.PROVIDER_ID)
                        && getClass() == Builder.class
                        && mParams.isEmpty()) {
                    return new GoogleBuilder().build();
                }

                return new IdpConfig(mProviderId, mParams);
            }
        }

        /**
         * {@link IdpConfig} builder for the email provider.
         */
        public static final class EmailBuilder extends Builder {
            public EmailBuilder() {
                //noinspection deprecation taking a hit for the backcompat team
                super(EmailAuthProvider.PROVIDER_ID);
            }

            /**
             * Enables or disables creating new accounts in the email sign in flow.
             * <p>
             * Account creation is enabled by default.
             */
            @NonNull
            public EmailBuilder setAllowNewAccounts(boolean allow) {
                getParams().putBoolean(ExtraConstants.EXTRA_ALLOW_NEW_EMAILS, allow);
                return this;
            }

            /**
             * Configures the requirement for the user to enter first and last name
             * in the email sign up flow.
             * <p>
             * Name is required by default.
             */
            @NonNull
            public EmailBuilder setRequireName(boolean requireName) {
                getParams().putBoolean(ExtraConstants.EXTRA_REQUIRE_NAME, requireName);
                return this;
            }
        }

        /**
         * {@link IdpConfig} builder for the phone provider.
         */
        public static final class PhoneBuilder extends Builder {
            public PhoneBuilder() {
                //noinspection deprecation taking a hit for the backcompat team
                super(PhoneAuthProvider.PROVIDER_ID);
            }

            /**
             * @param number the phone number in international format
             * @see #setDefaultNumber(String, String)
             */
            @NonNull
            public PhoneBuilder setDefaultNumber(@NonNull String number) {
                Preconditions.checkUnset(getParams(),
                        "Cannot overwrite previously set phone number",
                        ExtraConstants.EXTRA_COUNTRY_ISO,
                        ExtraConstants.EXTRA_NATIONAL_NUMBER);
                if (!PhoneNumberUtils.isValid(number)) {
                    throw new IllegalStateException("Invalid phone number: " + number);
                }

                getParams().putString(ExtraConstants.EXTRA_PHONE, number);

                return this;
            }

            /**
             * Set the default phone number that will be used to populate the phone verification
             * sign-in flow.
             *
             * @param iso    the phone number's country code
             * @param number the phone number in local format
             */
            @NonNull
            public PhoneBuilder setDefaultNumber(@NonNull String iso, @NonNull String number) {
                Preconditions.checkUnset(getParams(),
                        "Cannot overwrite previously set phone number",
                        ExtraConstants.EXTRA_PHONE);
                if (!PhoneNumberUtils.isValidIso(iso)) {
                    throw new IllegalStateException("Invalid country iso: " + iso);
                }

                getParams().putString(ExtraConstants.EXTRA_COUNTRY_ISO, iso);
                getParams().putString(ExtraConstants.EXTRA_NATIONAL_NUMBER, number);

                return this;
            }

            /**
             * Set the default country code that will be used in the phone verification sign-in
             * flow.
             *
             * @param iso country iso
             */
            @NonNull
            public PhoneBuilder setDefaultCountryIso(@NonNull String iso) {
                Preconditions.checkUnset(getParams(),
                        "Cannot overwrite previously set phone number",
                        ExtraConstants.EXTRA_PHONE,
                        ExtraConstants.EXTRA_COUNTRY_ISO,
                        ExtraConstants.EXTRA_NATIONAL_NUMBER);
                if (!PhoneNumberUtils.isValidIso(iso)) {
                    throw new IllegalStateException("Invalid country iso: " + iso);
                }

                getParams().putString(ExtraConstants.EXTRA_COUNTRY_ISO, iso);

                return this;
            }
        }

        /**
         * {@link IdpConfig} builder for the Google provider.
         */
        public static final class GoogleBuilder extends Builder {
            public GoogleBuilder() {
                //noinspection deprecation taking a hit for the backcompat team
                super(GoogleAuthProvider.PROVIDER_ID);
                Preconditions.checkConfigured(getApplicationContext(),
                        "Check your google-services plugin configuration, the" +
                                " default_web_client_id string wasn't populated.",
                        R.string.default_web_client_id);
            }

            /**
             * Set the scopes that your app will request when using Google sign-in. See all <a
             * href="https://developers.google.com/identity/protocols/googlescopes">available
             * scopes</a>.
             *
             * @param scopes additional scopes to be requested
             */
            @NonNull
            public GoogleBuilder setScopes(@NonNull List<String> scopes) {
                GoogleSignInOptions.Builder builder =
                        new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN);
                for (String scope : scopes) {
                    builder.requestScopes(new Scope(scope));
                }
                return setSignInOptions(builder.build());
            }

            /**
             * Set the {@link GoogleSignInOptions} to be used for Google sign-in. Standard options
             * like requesting the user's email will automatically be added.
             *
             * @param options sign-in options
             */
            @NonNull
            public GoogleBuilder setSignInOptions(@NonNull GoogleSignInOptions options) {
                Preconditions.checkUnset(getParams(),
                        "Cannot overwrite previously set sign-in options.",
                        ExtraConstants.EXTRA_GOOGLE_SIGN_IN_OPTIONS);

                GoogleSignInOptions.Builder builder = new GoogleSignInOptions.Builder(options);
                builder.requestEmail().requestIdToken(getApplicationContext()
                        .getString(R.string.default_web_client_id));
                getParams().putParcelable(
                        ExtraConstants.EXTRA_GOOGLE_SIGN_IN_OPTIONS, builder.build());

                return this;
            }

            @NonNull
            @Override
            public IdpConfig build() {
                if (!getParams().containsKey(ExtraConstants.EXTRA_GOOGLE_SIGN_IN_OPTIONS)) {
                    setScopes(Collections.<String>emptyList());
                }

                return super.build();
            }
        }

        /**
         * {@link IdpConfig} builder for the Facebook provider.
         */
        public static final class FacebookBuilder extends Builder {
            private static final String TAG = "FacebookBuilder";

            public FacebookBuilder() {
                //noinspection deprecation taking a hit for the backcompat team
                super(FacebookAuthProvider.PROVIDER_ID);

                try {
                    //noinspection unused to possibly throw
                    Class c = com.facebook.FacebookSdk.class;
                } catch (NoClassDefFoundError e) {
                    throw new RuntimeException(
                            "Facebook provider cannot be configured " +
                                    "without dependency. Did you forget to add " +
                                    "'com.facebook.android:facebook-login:VERSION' dependency?");
                }

                Preconditions.checkConfigured(getApplicationContext(),
                        "Facebook provider unconfigured. Make sure to add a" +
                                " `facebook_application_id` string. See the docs for more info:" +
                                " https://github.com/firebase/FirebaseUI-Android/blob/master/auth/README.md#facebook",
                        R.string.facebook_application_id);
                if (getApplicationContext().getString(R.string.facebook_login_protocol_scheme)
                        .equals("fbYOUR_APP_ID")) {
                    Log.w(TAG, "Facebook provider unconfigured for Chrome Custom Tabs.");
                }
            }

            /**
             * Specifies the additional permissions that the application will request in the
             * Facebook Login SDK. Available permissions can be found <a
             * href="https://developers.facebook.com/docs/facebook-login/permissions">here</a>.
             */
            @SuppressWarnings({"deprecation", "NullableProblems"}) // For backcompat
            @NonNull
            public FacebookBuilder setPermissions(@NonNull List<String> permissions) {
                getParams().putStringArrayList(
                        ExtraConstants.EXTRA_FACEBOOK_PERMISSIONS, new ArrayList<>(permissions));
                return this;
            }
        }

        /**
         * {@link IdpConfig} builder for the Twitter provider.
         */
        public static final class TwitterBuilder extends Builder {
            public TwitterBuilder() {
                //noinspection deprecation taking a hit for the backcompat team
                super(TwitterAuthProvider.PROVIDER_ID);

                try {
                    //noinspection unused to possibly throw
                    Class c = com.twitter.sdk.android.core.TwitterCore.class;
                } catch (NoClassDefFoundError e) {
                    throw new RuntimeException(
                            "Twitter provider cannot be configured " +
                                    "without dependency. Did you forget to add " +
                                    "'com.twitter.sdk.android:twitter-core:VERSION' dependency?");
                }

                Preconditions.checkConfigured(getApplicationContext(),
                        "Twitter provider unconfigured. Make sure to add your key and secret." +
                                " See the docs for more info:" +
                                " https://github.com/firebase/FirebaseUI-Android/blob/master/auth/README.md#twitter",
                        R.string.twitter_consumer_key,
                        R.string.twitter_consumer_secret);
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
        final List<IdpConfig> mProviders = new ArrayList<>();
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
            mTheme = Preconditions.checkValidStyle(
                    mApp.getApplicationContext(),
                    theme,
                    "theme identifier is unknown or not a style definition");
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
            return setIsSmartLockEnabled(enabled, enabled);
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
                mProviders.add(new IdpConfig.EmailBuilder().build());
            }

            return KickoffActivity.createIntent(mApp.getApplicationContext(), getFlowParams());
        }

        protected abstract FlowParameters getFlowParams();
    }

    /**
     * Builder for the intent to start the user authentication flow.
     */
    public final class SignInIntentBuilder extends AuthIntentBuilder<SignInIntentBuilder> {
        private Boolean mAllowNewEmailAccounts;

        private SignInIntentBuilder() {
            super();
        }

        /**
         * Enables or disables creating new accounts in the email sign in flow.
         * <p>
         * <p>Account creation is enabled by default.
         *
         * @deprecated set this option directly on the email builder: {@link
         * IdpConfig.EmailBuilder#setAllowNewAccounts(boolean)}.
         */
        @NonNull
        @Deprecated
        public SignInIntentBuilder setAllowNewEmailAccounts(boolean enabled) {
            mAllowNewEmailAccounts = enabled;
            return this;
        }

        @NonNull
        @Override
        public Intent build() {
            if (mAllowNewEmailAccounts != null) {
                // To ensure setAllowNewEmailAccounts backcompat
                for (int i = 0; i < mProviders.size(); i++) {
                    if (mProviders.get(i).getProviderId().equals(EmailAuthProvider.PROVIDER_ID)) {
                        mProviders.set(i, new IdpConfig.EmailBuilder()
                                .setAllowNewAccounts(mAllowNewEmailAccounts)
                                .build());
                        break;
                    }
                }
            }

            return super.build();
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
                    mEnableHints);
        }
    }
}
