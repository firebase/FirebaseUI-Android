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
import com.firebase.ui.auth.data.remote.TwitterSignInHandler;
import com.firebase.ui.auth.ui.idp.AuthMethodPickerActivity;
import com.firebase.ui.auth.util.CredentialUtils;
import com.firebase.ui.auth.util.ExtraConstants;
import com.firebase.ui.auth.util.GoogleApiUtils;
import com.firebase.ui.auth.util.Preconditions;
import com.firebase.ui.auth.util.data.PhoneNumberUtils;
import com.firebase.ui.auth.util.data.ProviderAvailability;
import com.firebase.ui.auth.util.data.ProviderUtils;
import com.google.android.gms.auth.api.credentials.Credential;
import com.google.android.gms.auth.api.credentials.CredentialRequest;
import com.google.android.gms.auth.api.credentials.CredentialRequestResponse;
import com.google.android.gms.auth.api.credentials.CredentialsClient;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.CommonStatusCodes;
import com.google.android.gms.common.api.Scope;
import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FacebookAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidUserException;
import com.google.firebase.auth.FirebaseAuthProvider;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GithubAuthProvider;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.auth.PhoneAuthProvider;
import com.google.firebase.auth.TwitterAuthProvider;
import com.google.firebase.auth.UserInfo;
import com.twitter.sdk.android.core.TwitterCore;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Locale;
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
public final class AuthUI {

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public static final String TAG = "AuthUI";

    /**
     * Provider for anonymous users.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public static final String ANONYMOUS_PROVIDER = "anonymous";

    @StringDef({
                       GoogleAuthProvider.PROVIDER_ID,
                       FacebookAuthProvider.PROVIDER_ID,
                       TwitterAuthProvider.PROVIDER_ID,
                       GithubAuthProvider.PROVIDER_ID,
                       EmailAuthProvider.PROVIDER_ID,
                       PhoneAuthProvider.PROVIDER_ID,
                       ANONYMOUS_PROVIDER
               })
    @Retention(RetentionPolicy.SOURCE)
    public @interface SupportedProvider {}

    /**
     * Default value for logo resource, omits the logo from the {@link AuthMethodPickerActivity}.
     */
    public static final int NO_LOGO = -1;

    /**
     * The set of authentication providers supported in Firebase Auth UI.
     */
    public static final Set<String> SUPPORTED_PROVIDERS =
            Collections.unmodifiableSet(new HashSet<>(Arrays.asList(
                    GoogleAuthProvider.PROVIDER_ID,
                    FacebookAuthProvider.PROVIDER_ID,
                    TwitterAuthProvider.PROVIDER_ID,
                    GithubAuthProvider.PROVIDER_ID,
                    EmailAuthProvider.PROVIDER_ID,
                    PhoneAuthProvider.PROVIDER_ID,
                    ANONYMOUS_PROVIDER
            )));

    /**
     * The set of social authentication providers supported in Firebase Auth UI.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public static final Set<String> SOCIAL_PROVIDERS =
            Collections.unmodifiableSet(new HashSet<>(Arrays.asList(
                    GoogleAuthProvider.PROVIDER_ID,
                    FacebookAuthProvider.PROVIDER_ID,
                    TwitterAuthProvider.PROVIDER_ID,
                    GithubAuthProvider.PROVIDER_ID)));

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public static final String UNCONFIGURED_CONFIG_VALUE = "CHANGE-ME";

    private static final IdentityHashMap<FirebaseApp, AuthUI> INSTANCES = new IdentityHashMap<>();

    private static Context sApplicationContext;

    private final FirebaseApp mApp;
    private final FirebaseAuth mAuth;

    private AuthUI(FirebaseApp app) {
        mApp = app;
        mAuth = FirebaseAuth.getInstance(mApp);

        try {
            mAuth.setFirebaseUIVersion(BuildConfig.VERSION_NAME);
        } catch (Exception e) {
            Log.e(TAG, "Couldn't set the FUI version.", e);
        }
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
     * Signs the user in without any UI if possible. If this operation fails, you can safely start a
     * UI-based sign-in flow knowing it is required.
     *
     * @param context requesting the user be signed in
     * @param configs to use for silent sign in. Only Google and email are currently supported, the
     *                rest will be ignored.
     * @return a task which indicates whether or not the user was successfully signed in.
     */
    @NonNull
    public Task<AuthResult> silentSignIn(@NonNull Context context,
                                         @NonNull List<IdpConfig> configs) {
        if (mAuth.getCurrentUser() != null) {
            throw new IllegalArgumentException("User already signed in!");
        }

        final Context appContext = context.getApplicationContext();
        final IdpConfig google =
                ProviderUtils.getConfigFromIdps(configs, GoogleAuthProvider.PROVIDER_ID);
        final IdpConfig email =
                ProviderUtils.getConfigFromIdps(configs, EmailAuthProvider.PROVIDER_ID);

        if (google == null && email == null) {
            throw new IllegalArgumentException("No supported providers were supplied. " +
                    "Add either Google or email support.");
        }

        final GoogleSignInOptions googleOptions;
        if (google == null) {
            googleOptions = null;
        } else {
            GoogleSignInAccount last = GoogleSignIn.getLastSignedInAccount(appContext);
            if (last != null && last.getIdToken() != null) {
                return mAuth.signInWithCredential(GoogleAuthProvider.getCredential(
                        last.getIdToken(), null));
            }

            googleOptions = google.getParams()
                    .getParcelable(ExtraConstants.GOOGLE_SIGN_IN_OPTIONS);
        }

        return GoogleApiUtils.getCredentialsClient(context)
                .request(new CredentialRequest.Builder()
                        // We can support both email and Google at the same time here because they
                        // are mutually exclusive. If a user signs in with Google, their email
                        // account will automatically be upgraded (a.k.a. replaced) with the Google
                        // one, meaning Smart Lock won't have to show the picker UI.
                        .setPasswordLoginSupported(email != null)
                        .setAccountTypes(google == null ? null :
                                ProviderUtils.providerIdToAccountType(GoogleAuthProvider.PROVIDER_ID))
                        .build())
                .continueWithTask(new Continuation<CredentialRequestResponse, Task<AuthResult>>() {
                    @Override
                    public Task<AuthResult> then(@NonNull Task<CredentialRequestResponse> task) {
                        Credential credential = task.getResult().getCredential();
                        String email = credential.getId();
                        String password = credential.getPassword();

                        if (TextUtils.isEmpty(password)) {
                            return GoogleSignIn.getClient(appContext,
                                    new GoogleSignInOptions.Builder(googleOptions)
                                            .setAccountName(email)
                                            .build())
                                    .silentSignIn()
                                    .continueWithTask(new Continuation<GoogleSignInAccount, Task<AuthResult>>() {
                                        @Override
                                        public Task<AuthResult> then(@NonNull Task<GoogleSignInAccount> task) {
                                            AuthCredential authCredential = GoogleAuthProvider.getCredential(
                                                    task.getResult().getIdToken(), null);
                                            return mAuth.signInWithCredential(authCredential);
                                        }
                                    });
                        } else {
                            return mAuth.signInWithEmailAndPassword(email, password);
                        }
                    }
                });
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
        Task<Void> maybeDisableAutoSignIn = GoogleApiUtils.getCredentialsClient(context)
                .disableAutoSignIn()
                .continueWith(new Continuation<Void, Void>() {
                    @Override
                    public Void then(@NonNull Task<Void> task) {
                        // We want to ignore a specific exception, since it's not a good reason
                        // to fail (see Issue 1156).
                        Exception e = task.getException();
                        if (e instanceof ApiException
                                && ((ApiException) e).getStatusCode() == CommonStatusCodes.CANCELED) {
                            Log.w(TAG, "Could not disable auto-sign in, maybe there are no " +
                                    "SmartLock accounts available?", e);
                            return null;
                        }

                        return task.getResult();
                    }
                });

        return Tasks.whenAll(
                signOutIdps(context),
                maybeDisableAutoSignIn
        ).continueWith(new Continuation<Void, Void>() {
            @Override
            public Void then(@NonNull Task<Void> task) {
                task.getResult(); // Propagate exceptions
                mAuth.signOut();
                return null;
            }
        });
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

                List<Task<?>> credentialTasks = new ArrayList<>();
                for (Credential credential : credentials) {
                    credentialTasks.add(client.delete(credential));
                }
                return Tasks.whenAll(credentialTasks)
                        .continueWith(new Continuation<Void, Void>() {
                            @Override
                            public Void then(@NonNull Task<Void> task) {
                                Exception e = task.getException();
                                Throwable t = e == null ? null : e.getCause();
                                if (!(t instanceof ApiException)
                                        || ((ApiException) t).getStatusCode() != CommonStatusCodes.CANCELED) {
                                    // Only propagate the exception if it isn't an invalid account
                                    // one. This can occur if we failed to save the credential or it
                                    // was deleted elsewhere. However, a lack of stored credential
                                    // doesn't mean fully deleting the user failed.
                                    return task.getResult();
                                }

                                return null;
                            }
                        });
            }
        }).continueWithTask(new Continuation<Void, Task<Void>>() {
            @Override
            public Task<Void> then(@NonNull Task<Void> task) {
                task.getResult(); // Propagate exception if there was one
                return currentUser.delete();
            }
        });
    }

    private Task<Void> signOutIdps(@NonNull Context context) {
        if (ProviderAvailability.IS_FACEBOOK_AVAILABLE) {
            LoginManager.getInstance().logOut();
        }
        if (ProviderAvailability.IS_TWITTER_AVAILABLE) {
            TwitterSignInHandler.initializeTwitter();
            TwitterCore.getInstance().getSessionManager().clearActiveSession();
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
            if (type == null) {
                // Since the account type is null, we've got an email credential. Adding a fake
                // password is the only way to tell Smart Lock that this is an email credential.
                credentials.add(CredentialUtils.buildCredentialOrThrow(user, "pass", null));
            } else {
                credentials.add(CredentialUtils.buildCredentialOrThrow(user, null, type));
            }
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
    public static final class IdpConfig implements Parcelable {
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

            protected Builder(@SupportedProvider @NonNull String providerId) {
                if (!SUPPORTED_PROVIDERS.contains(providerId)) {
                    throw new IllegalArgumentException("Unknown provider: " + providerId);
                }
                mProviderId = providerId;
            }

            @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
            @NonNull
            protected final Bundle getParams() {
                return mParams;
            }

            @CallSuper
            @NonNull
            public IdpConfig build() {
                return new IdpConfig(mProviderId, mParams);
            }
        }

        /**
         * {@link IdpConfig} builder for the email provider.
         */
        public static final class EmailBuilder extends Builder {
            public EmailBuilder() {
                super(EmailAuthProvider.PROVIDER_ID);
            }

            /**
             * Enables or disables creating new accounts in the email sign in flow.
             * <p>
             * Account creation is enabled by default.
             */
            @NonNull
            public EmailBuilder setAllowNewAccounts(boolean allow) {
                getParams().putBoolean(ExtraConstants.ALLOW_NEW_EMAILS, allow);
                return this;
            }

            /**
             * Configures the requirement for the user to enter first and last name in the email
             * sign up flow.
             * <p>
             * Name is required by default.
             */
            @NonNull
            public EmailBuilder setRequireName(boolean requireName) {
                getParams().putBoolean(ExtraConstants.REQUIRE_NAME, requireName);
                return this;
            }
        }

        /**
         * {@link IdpConfig} builder for the phone provider.
         */
        public static final class PhoneBuilder extends Builder {
            public PhoneBuilder() {
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
                        ExtraConstants.PHONE,
                        ExtraConstants.COUNTRY_ISO,
                        ExtraConstants.NATIONAL_NUMBER);
                if (!PhoneNumberUtils.isValid(number)) {
                    throw new IllegalStateException("Invalid phone number: " + number);
                }

                getParams().putString(ExtraConstants.PHONE, number);

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
                        ExtraConstants.PHONE,
                        ExtraConstants.COUNTRY_ISO,
                        ExtraConstants.NATIONAL_NUMBER);
                if (!PhoneNumberUtils.isValidIso(iso)) {
                    throw new IllegalStateException("Invalid country iso: " + iso);
                }

                getParams().putString(ExtraConstants.COUNTRY_ISO, iso);
                getParams().putString(ExtraConstants.NATIONAL_NUMBER, number);

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
                        ExtraConstants.PHONE,
                        ExtraConstants.COUNTRY_ISO,
                        ExtraConstants.NATIONAL_NUMBER);
                if (!PhoneNumberUtils.isValidIso(iso)) {
                    throw new IllegalStateException("Invalid country iso: " + iso);
                }

                getParams().putString(ExtraConstants.COUNTRY_ISO,
                        iso.toUpperCase(Locale.getDefault()));

                return this;
            }


            /**
             * Sets the country codes available in the country code selector for phone
             * authentication. Takes as input a List of both country isos and codes.
             * This is not to be called with
             * {@link #setBlacklistedCountries(List)}.
             * If both are called, an exception will be thrown.
             * <p>
             * Inputting an e-164 country code (e.g. '+1') will include all countries with
             * +1 as its code.
             * Example input: {'+52', 'us'}
             * For a list of country iso or codes, see Alpha-2 isos here:
             * https://en.wikipedia.org/wiki/ISO_3166-1
             * and e-164 codes here: https://en.wikipedia.org/wiki/List_of_country_calling_codes
             *
             * @param whitelistedCountries a non empty case insensitive list of country codes
             *                             and/or isos to be whitelisted
             * @throws IllegalArgumentException if an empty whitelist is provided.
             * @throws NullPointerException if a null whitelist is provided.
             */
            public PhoneBuilder setWhitelistedCountries(
                    @NonNull List<String> whitelistedCountries) {
                if (getParams().containsKey(ExtraConstants.BLACKLISTED_COUNTRIES)) {
                    throw new IllegalStateException(
                            "You can either whitelist or blacklist country codes for phone " +
                                    "authentication.");
                }

                String message = "Invalid argument: Only non-%s whitelists are valid. " +
                        "To specify no whitelist, do not call this method.";
                Preconditions.checkNotNull(whitelistedCountries, String.format(message, "null"));
                Preconditions.checkArgument(!whitelistedCountries.isEmpty(), String.format(message, "empty"));

                addCountriesToBundle(whitelistedCountries, ExtraConstants.WHITELISTED_COUNTRIES);
                return this;
            }

            /**
             * Sets the countries to be removed from the country code selector for phone
             * authentication. Takes as input a List of both country isos and codes.
             * This is not to be called with
             * {@link #setWhitelistedCountries(List)}.
             * If both are called, an exception will be thrown.
             * <p>
             * Inputting an e-164 country code (e.g. '+1') will include all countries with
             * +1 as its code.
             * Example input: {'+52', 'us'}
             * For a list of country iso or codes, see Alpha-2 codes here:
             * https://en.wikipedia.org/wiki/ISO_3166-1
             * and e-164 codes here: https://en.wikipedia.org/wiki/List_of_country_calling_codes
             *
             * @param blacklistedCountries a non empty case insensitive list of country codes
             *                             and/or isos to be blacklisted
             * @throws IllegalArgumentException if an empty blacklist is provided.
             * @throws NullPointerException if a null blacklist is provided.
             */
            public PhoneBuilder setBlacklistedCountries(
                    @NonNull List<String> blacklistedCountries) {
                if (getParams().containsKey(ExtraConstants.WHITELISTED_COUNTRIES)) {
                    throw new IllegalStateException(
                            "You can either whitelist or blacklist country codes for phone " +
                                    "authentication.");
                }

                String message = "Invalid argument: Only non-%s blacklists are valid. " +
                        "To specify no blacklist, do not call this method.";
                Preconditions.checkNotNull(blacklistedCountries, String.format(message, "null"));
                Preconditions.checkArgument(!blacklistedCountries.isEmpty(), String.format(message, "empty"));

                addCountriesToBundle(blacklistedCountries, ExtraConstants.BLACKLISTED_COUNTRIES);
                return this;
            }

            @Override
            public IdpConfig build() {
                validateInputs();
                return super.build();
            }

            private void addCountriesToBundle(List<String> CountryIsos, String CountryIsoType) {
                ArrayList<String> uppercaseCodes = new ArrayList<>();
                for (String code : CountryIsos) {
                    uppercaseCodes.add(code.toUpperCase(Locale.getDefault()));
                }

                getParams().putStringArrayList(CountryIsoType, uppercaseCodes);
            }

            private void validateInputs() {
                List<String> whitelistedCountries = getParams().getStringArrayList(
                        ExtraConstants.WHITELISTED_COUNTRIES);
                List<String> blacklistedCountries = getParams().getStringArrayList(
                        ExtraConstants.BLACKLISTED_COUNTRIES);

                if (whitelistedCountries != null &&
                        blacklistedCountries != null) {
                    throw new IllegalStateException(
                            "You can either whitelist or blacklist country codes for phone " +
                                    "authentication.");
                } else if (whitelistedCountries != null) {
                    validateInputs(whitelistedCountries, true);

                } else if (blacklistedCountries != null) {
                    validateInputs(blacklistedCountries, false);
                }
            }

            private void validateInputs(List<String> countries, boolean whitelisted) {
                validateCountryInput(countries);
                validateDefaultCountryInput(countries, whitelisted);
            }

            private void validateCountryInput(List<String> codes) {
                for (String code : codes) {
                    if (!PhoneNumberUtils.isValidIso(code) && !PhoneNumberUtils.isValid(code)) {
                        throw new IllegalArgumentException("Invalid input: You must provide a " +
                                "valid country iso (alpha-2) or code (e-164). e.g. 'us' or '+1'.");
                    }
                }
            }

            private void validateDefaultCountryInput(List<String> codes, boolean whitelisted) {
                // A default iso/code can be set via #setDefaultCountryIso() or #setDefaultNumber()
                if (getParams().containsKey(ExtraConstants.COUNTRY_ISO) ||
                        getParams().containsKey(ExtraConstants.PHONE)) {

                    if (!validateDefaultCountryIso(codes, whitelisted)
                            || !validateDefaultPhoneIsos(codes, whitelisted)) {
                        throw new IllegalArgumentException("Invalid default country iso. Make " +
                                "sure it is either part of the whitelisted list or that you "
                                + "haven't blacklisted it.");
                    }
                }

            }

            private boolean validateDefaultCountryIso(List<String> codes, boolean whitelisted) {
                String defaultIso = getDefaultIso();
                return isValidDefaultIso(codes, defaultIso, whitelisted);
            }

            private boolean validateDefaultPhoneIsos(List<String> codes, boolean whitelisted) {
                List<String> phoneIsos = getPhoneIsosFromCode();
                for (String iso : phoneIsos) {
                    if (isValidDefaultIso(codes, iso, whitelisted)) {
                        return true;
                    }
                }
                return phoneIsos.isEmpty();
            }

            private boolean isValidDefaultIso(List<String> codes, String iso, boolean whitelisted) {
                if (iso == null) return true;
                boolean containsIso = containsCountryIso(codes, iso);
                return containsIso && whitelisted || !containsIso && !whitelisted;

            }

            private boolean containsCountryIso(List<String> codes, String iso) {
                iso = iso.toUpperCase(Locale.getDefault());
                for (String code : codes) {
                    if (PhoneNumberUtils.isValidIso(code)) {
                        if (code.equals(iso)) {
                            return true;
                        }
                    } else {
                        List<String> isos = PhoneNumberUtils.getCountryIsosFromCountryCode(code);
                        if (isos.contains(iso)) {
                            return true;
                        }
                    }
                }
                return false;
            }

            private List<String> getPhoneIsosFromCode() {
                List<String> isos = new ArrayList<>();
                String phone = getParams().getString(ExtraConstants.PHONE);
                if (phone != null && phone.startsWith("+")) {
                    String countryCode = "+" + PhoneNumberUtils.getPhoneNumber(phone)
                            .getCountryCode();
                    List<String> isosToAdd = PhoneNumberUtils.
                            getCountryIsosFromCountryCode(countryCode);
                    if (isosToAdd != null) {
                        isos.addAll(isosToAdd);
                    }
                }
                return isos;
            }

            private String getDefaultIso() {
                return getParams().containsKey(ExtraConstants.COUNTRY_ISO) ?
                        getParams().getString(ExtraConstants.COUNTRY_ISO) : null;
            }
        }

        /**
         * {@link IdpConfig} builder for the Google provider.
         */
        public static final class GoogleBuilder extends Builder {
            public GoogleBuilder() {
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
             * Set the {@link GoogleSignInOptions} to be used for Google sign-in. Standard
             * options like requesting the user's email will automatically be added.
             *
             * @param options sign-in options
             */
            @NonNull
            public GoogleBuilder setSignInOptions(@NonNull GoogleSignInOptions options) {
                Preconditions.checkUnset(getParams(),
                        "Cannot overwrite previously set sign-in options.",
                        ExtraConstants.GOOGLE_SIGN_IN_OPTIONS);

                GoogleSignInOptions.Builder builder = new GoogleSignInOptions.Builder(options);
                builder.requestEmail().requestIdToken(getApplicationContext()
                        .getString(R.string.default_web_client_id));
                getParams().putParcelable(
                        ExtraConstants.GOOGLE_SIGN_IN_OPTIONS, builder.build());

                return this;
            }

            @NonNull
            @Override
            public IdpConfig build() {
                if (!getParams().containsKey(ExtraConstants.GOOGLE_SIGN_IN_OPTIONS)) {
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
                super(FacebookAuthProvider.PROVIDER_ID);
                if (!ProviderAvailability.IS_FACEBOOK_AVAILABLE) {
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
            @NonNull
            public FacebookBuilder setPermissions(@NonNull List<String> permissions) {
                getParams().putStringArrayList(
                        ExtraConstants.FACEBOOK_PERMISSIONS, new ArrayList<>(permissions));
                return this;
            }
        }

        /**
         * {@link IdpConfig} builder for the Twitter provider.
         */
        public static final class TwitterBuilder extends Builder {
            public TwitterBuilder() {
                super(TwitterAuthProvider.PROVIDER_ID);
                if (!ProviderAvailability.IS_TWITTER_AVAILABLE) {
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

        /**
         * {@link IdpConfig} builder for the GitHub provider.
         */
        public static final class GitHubBuilder extends Builder {
            public GitHubBuilder() {
                //noinspection deprecation taking a hit for the backcompat team
                super(GithubAuthProvider.PROVIDER_ID);
                if (!ProviderAvailability.IS_GITHUB_AVAILABLE) {
                    throw new RuntimeException(
                            "GitHub provider cannot be configured " +
                                    "without dependency. Did you forget to add " +
                                    "'com.firebaseui:firebase-ui-auth-github:VERSION' dependency?");
                }
                Preconditions.checkConfigured(getApplicationContext(),
                        "GitHub provider unconfigured. Make sure to add your client id and secret." +
                                " See the docs for more info:" +
                                " https://github.com/firebase/FirebaseUI-Android/blob/master/auth/README.md#github",
                        R.string.firebase_web_host,
                        R.string.github_client_id,
                        R.string.github_client_secret);
            }

            /**
             * Specifies the additional permissions to be requested. Available permissions can be
             * found <ahref="https://developer.github.com/apps/building-oauth-apps/scopes-for-oauth-apps/#available-scopes">here</a>.
             */
            @NonNull
            public GitHubBuilder setPermissions(@NonNull List<String> permissions) {
                getParams().putStringArrayList(
                        ExtraConstants.GITHUB_PERMISSIONS, new ArrayList<>(permissions));
                return this;
            }
        }

        /**
         * {@link IdpConfig} builder for the Anonymous provider.
         */
        public static final class AnonymousBuilder extends Builder {
            public AnonymousBuilder() {
                super(ANONYMOUS_PROVIDER);
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
        boolean mAlwaysShowProviderChoice = false;
        boolean mEnableCredentials = true;
        boolean mEnableHints = true;

        /**
         * Specifies the theme to use for the application flow. If no theme is specified, a
         * default theme will be used.
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
         *
         * @deprecated Please use {@link #setTosAndPrivacyPolicyUrls(String, String)} For the Tos
         * link to be displayed a Privacy Policy url must also be provided.
         */
        @NonNull
        @Deprecated
        public T setTosUrl(@Nullable String tosUrl) {
            mTosUrl = tosUrl;
            return (T) this;
        }

        /**
         * Specifies the privacy policy URL for the application.
         *
         * @deprecated Please use {@link #setTosAndPrivacyPolicyUrls(String, String)} For the
         * Privacy Policy link to be displayed a Tos url must also be provided.
         */
        @NonNull
        @Deprecated
        public T setPrivacyPolicyUrl(@Nullable String privacyPolicyUrl) {
            mPrivacyPolicyUrl = privacyPolicyUrl;
            return (T) this;
        }

        @NonNull
        public T setTosAndPrivacyPolicyUrls(@NonNull String tosUrl,
                                            @NonNull String privacyPolicyUrl) {
            Preconditions.checkNotNull(tosUrl, "tosUrl cannot be null");
            Preconditions.checkNotNull(privacyPolicyUrl, "privacyPolicyUrl cannot be null");
            mTosUrl = tosUrl;
            mPrivacyPolicyUrl = privacyPolicyUrl;
            return (T) this;
        }

        /**
         * Specified the set of supported authentication providers. At least one provider must
         * be specified. There may only be one instance of each provider. Anonymous provider cannot
         * be the only provider specified.
         * <p>
         * <p>If no providers are explicitly specified by calling this method, then the email
         * provider is the default supported provider.
         *
         * @param idpConfigs a list of {@link IdpConfig}s, where each {@link IdpConfig} contains the
         *                   configuration parameters for the IDP.
         * @see IdpConfig
         *
         * @throws IllegalStateException if anonymous provider is the only specified provider.
         */
        @NonNull
        public T setAvailableProviders(@NonNull List<IdpConfig> idpConfigs) {
            Preconditions.checkNotNull(idpConfigs, "idpConfigs cannot be null");
            if (idpConfigs.size() == 1 &&
                    idpConfigs.get(0).getProviderId().equals(ANONYMOUS_PROVIDER)) {
                throw new IllegalStateException("Sign in as guest cannot be the only sign in " +
                        "method. In this case, sign the user in anonymously your self; " +
                        "no UI is needed.");
            }

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

        /**
         * Forces the sign-in method choice screen to always show, even if there is only
         * a single provider configured.
         * <p>
         * <p>This is false by default.
         * @param alwaysShow if true, force the sign-in choice screen to show.
         */
        @NonNull
        public T setAlwaysShowSignInMethodScreen(boolean alwaysShow) {
            mAlwaysShowProviderChoice = alwaysShow;
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

        private boolean mEnableAnonymousUpgrade;

        private SignInIntentBuilder() {
            super();
        }

        /**
         * Enables upgrading anonymous accounts to full accounts during the sign-in flow.
         * This is disabled by default.
         */
        @NonNull
        public SignInIntentBuilder enableAnonymousUsersAutoUpgrade() {
            mEnableAnonymousUpgrade = true;
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
                    mEnableAnonymousUpgrade,
                    mAlwaysShowProviderChoice);
        }
    }
}
