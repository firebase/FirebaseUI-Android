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

import android.app.Activity;
import android.content.Intent;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.DrawableRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StyleRes;
import android.support.annotation.VisibleForTesting;
import android.support.v4.app.FragmentActivity;

import com.facebook.login.LoginManager;
import com.firebase.ui.auth.ui.FlowParameters;
import com.firebase.ui.auth.ui.idp.AuthMethodPickerActivity;
import com.firebase.ui.auth.util.CredentialTaskApi;
import com.firebase.ui.auth.util.CredentialsApiHelper;
import com.firebase.ui.auth.util.GoogleApiClientTaskHelper;
import com.firebase.ui.auth.util.GoogleSignInHelper;
import com.firebase.ui.auth.util.Preconditions;
import com.firebase.ui.auth.util.signincontainer.SmartLockBase;
import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.credentials.Credential;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FacebookAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.auth.TwitterAuthProvider;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedHashSet;
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

    /**
     * Provider identifier for email and password credentials, for use with
     * {@link SignInIntentBuilder#setProviders}.
     */
    public static final String EMAIL_PROVIDER = EmailAuthProvider.PROVIDER_ID;

    /**
     * Provider identifier for Google, for use with {@link SignInIntentBuilder#setProviders}.
     */
    public static final String GOOGLE_PROVIDER = GoogleAuthProvider.PROVIDER_ID;

    /**
     * Provider identifier for Facebook, for use with {@link SignInIntentBuilder#setProviders}.
     */
    public static final String FACEBOOK_PROVIDER = FacebookAuthProvider.PROVIDER_ID;

    /**
     * Provider identifier for Twitter, for use with {@link SignInIntentBuilder#setProviders}.
     */
    public static final String TWITTER_PROVIDER = TwitterAuthProvider.PROVIDER_ID;

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
                    TWITTER_PROVIDER
            )));

    private static final IdentityHashMap<FirebaseApp, AuthUI> INSTANCES = new IdentityHashMap<>();

    private final FirebaseApp mApp;
    private final FirebaseAuth mAuth;

    private AuthUI(FirebaseApp app) {
        mApp = app;
        mAuth = FirebaseAuth.getInstance(mApp);
    }

    /**
     * Retrieves the {@link AuthUI} instance associated with the default app, as returned by
     * {@code FirebaseApp.getInstance()}.
     *
     * @throws IllegalStateException if the default app is not initialized.
     */
    public static AuthUI getInstance() {
        return getInstance(FirebaseApp.getInstance());
    }

    /**
     * Retrieves the {@link AuthUI} instance associated the the specified app.
     */
    public static AuthUI getInstance(FirebaseApp app) {
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
     * @param activity The activity requesting the user be signed out.
     * @return a task which, upon completion, signals that the user has been signed out ({@code
     * result.isSuccess()}, or that the sign-out attempt failed unexpectedly ({@code
     * !result.isSuccess()}).
     * @deprecated use {@link #signOut(FragmentActivity)} instead
     */
    @Deprecated
    public Task<Void> signOut(@NonNull Activity activity) {
        // Get helper for Google Sign In and Credentials API
        GoogleApiClientTaskHelper taskHelper = GoogleApiClientTaskHelper.getInstance(activity);
        taskHelper.getBuilder()
                .addApi(Auth.CREDENTIALS_API)
                .addApi(Auth.GOOGLE_SIGN_IN_API, GoogleSignInOptions.DEFAULT_SIGN_IN);

        // Get Credentials Helper
        CredentialTaskApi credentialsHelper = CredentialsApiHelper.getInstance(taskHelper);

        // Firebase Sign out
        mAuth.signOut();

        // Disable credentials auto sign-in
        Task<Status> disableCredentialsTask = credentialsHelper.disableAutoSignIn();

        // Google sign out
        Task<Void> googleSignOutTask = taskHelper.getConnectedGoogleApiClient()
                .continueWith(new Continuation<GoogleApiClient, Void>() {
                    @Override
                    public Void then(@NonNull Task<GoogleApiClient> task) throws Exception {
                        if (task.isSuccessful()) {
                            Auth.GoogleSignInApi.signOut(task.getResult());
                        }
                        return null;
                    }
                });

        // Facebook sign out
        LoginManager.getInstance().logOut();

        // Wait for all tasks to complete
        return Tasks.whenAll(disableCredentialsTask, googleSignOutTask);
    }

    /**
     * Delete the use from FirebaseAuth and delete any associated credentials from the Credentials
     * API. Returns a {@code Task} that succeeds if the Firebase Auth user deletion succeeds and
     * fails if the Firebase Auth deletion fails. Credentials deletion failures are handled
     * silently.
     *
     * @param activity the calling {@link Activity}.
     * @deprecated use {@link #delete(FragmentActivity)} instead
     */
    @Deprecated
    public Task<Void> delete(@NonNull Activity activity) {
        // Initialize SmartLock helper
        GoogleApiClientTaskHelper gacHelper = GoogleApiClientTaskHelper.getInstance(activity);
        gacHelper.getBuilder().addApi(Auth.CREDENTIALS_API);
        CredentialTaskApi credentialHelper = CredentialsApiHelper.getInstance(gacHelper);

        return getDeleteTask(credentialHelper);
    }

    /**
     * Signs the current user out, if one is signed in.
     *
     * @param activity the activity requesting the user be signed out
     * @return A task which, upon completion, signals that the user has been signed out ({@link
     * Task#isSuccessful()}, or that the sign-out attempt failed unexpectedly !{@link
     * Task#isSuccessful()}).
     */
    public Task<Void> signOut(@NonNull FragmentActivity activity) {
        // Get Credentials Helper
        GoogleSignInHelper credentialsHelper = GoogleSignInHelper.getInstance(activity);

        // Firebase Sign out
        mAuth.signOut();

        // Disable credentials auto sign-in
        Task<Status> disableCredentialsTask = credentialsHelper.disableAutoSignIn();

        // Google sign out
        Task<Status> signOutTask = credentialsHelper.signOut();

        // Facebook sign out
        LoginManager.getInstance().logOut();

        // Wait for all tasks to complete
        return Tasks.whenAll(disableCredentialsTask, signOutTask);
    }

    /**
     * Delete the use from FirebaseAuth and delete any associated credentials from the Credentials
     * API. Returns a {@link Task} that succeeds if the Firebase Auth user deletion succeeds and
     * fails if the Firebase Auth deletion fails. Credentials deletion failures are handled
     * silently.
     *
     * @param activity the calling {@link Activity}.
     */
    public Task<Void> delete(@NonNull FragmentActivity activity) {
        // Initialize SmartLock helper
        CredentialTaskApi credentialHelper = GoogleSignInHelper.getInstance(activity);

        return getDeleteTask(credentialHelper);
    }

    private Task<Void> getDeleteTask(CredentialTaskApi credentialHelper) {
        FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        if (firebaseUser == null) {
            // If the current user is null, return a failed task immediately
            return Tasks.forException(new Exception("No currently signed in user."));
        }

        // Delete the Firebase user
        Task<Void> deleteUserTask = firebaseUser.delete();

        // Get all SmartLock credentials associated with the user
        List<Credential> credentials = SmartLockBase.credentialsFromFirebaseUser(firebaseUser);

        // For each Credential in the list, create a task to delete it.
        List<Task<?>> credentialTasks = new ArrayList<>();
        for (Credential credential : credentials) {
            credentialTasks.add(credentialHelper.delete(credential));
        }

        // Create a combined task that will succeed when all credential delete operations
        // have completed (even if they fail).
        final Task<Void> combinedCredentialTask = Tasks.whenAll(credentialTasks);

        // Chain the Firebase Auth delete task with the combined Credentials task
        // and return.
        return deleteUserTask.continueWithTask(new Continuation<Void, Task<Void>>() {
            @Override
            public Task<Void> then(@NonNull Task<Void> task) throws Exception {
                // Call getResult() to propagate failure by throwing an exception
                // if there was one.
                task.getResult(Exception.class);

                // Return the combined credential task
                return combinedCredentialTask;
            }
        });
    }

    /**
     * Starts the process of creating a sign in intent, with the mandatory application
     * context parameter.
     */
    public SignInIntentBuilder createSignInIntentBuilder() {
        return new SignInIntentBuilder();
    }

    /**
     * Configuration for an identity provider.
     * <p>
     * In the simplest case, you can supply the provider ID and build the config like this:
     * {@code new IdpConfig.Builder(AuthUI.GOOGLE_PROVIDER).build()}
     */
    public static class IdpConfig implements Parcelable {
        private final String mProviderId;
        private final List<String> mScopes;

        private IdpConfig(@NonNull String providerId, List<String> scopes) {
            mProviderId = providerId;
            mScopes = Collections.unmodifiableList(scopes);
        }

        private IdpConfig(Parcel in) {
            mProviderId = in.readString();
            mScopes = Collections.unmodifiableList(in.createStringArrayList());
        }

        public String getProviderId() {
            return mProviderId;
        }

        public List<String> getScopes() {
            return mScopes;
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
        }

        public static class Builder {
            private String mProviderId;
            private List<String> mScopes = new ArrayList<>();


            /**
             * Builds the configuration parameters for an identity provider.
             *
             * @param providerId An ID of one of the supported identity providers. e.g. {@link
             *                   AuthUI#GOOGLE_PROVIDER}. See {@link AuthUI#SUPPORTED_PROVIDERS} for
             *                   the complete list of supported Identity providers
             */
            public Builder(@NonNull String providerId) {
                if (!SUPPORTED_PROVIDERS.contains(providerId)) {
                    throw new IllegalArgumentException("Unkown provider: " + providerId);
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
            public Builder setPermissions(List<String> permissions) {
                mScopes = permissions;
                return this;
            }

            public IdpConfig build() {
                return new IdpConfig(mProviderId, mScopes);
            }
        }
    }

    /**
     * Builder for the intent to start the user authentication flow.
     */
    public final class SignInIntentBuilder {
        private int mLogo = NO_LOGO;
        private int mTheme = getDefaultTheme();
        private LinkedHashSet<IdpConfig> mProviders = new LinkedHashSet<>();
        private String mTosUrl;
        private boolean mIsSmartLockEnabled = true;
        private boolean mAllowNewEmailAccounts = true;

        private SignInIntentBuilder() {
            mProviders.add(new IdpConfig.Builder(EMAIL_PROVIDER).build());
        }

        /**
         * Specifies the theme to use for the application flow. If no theme is specified,
         * a default theme will be used.
         */
        public SignInIntentBuilder setTheme(@StyleRes int theme) {
            Preconditions.checkValidStyle(
                    mApp.getApplicationContext(),
                    theme,
                    "theme identifier is unknown or not a style definition");
            mTheme = theme;
            return this;
        }

        /**
         * Specifies the logo to use for the {@link AuthMethodPickerActivity}. If no logo
         * is specified, none will be used.
         */
        public SignInIntentBuilder setLogo(@DrawableRes int logo) {
            mLogo = logo;
            return this;
        }

        /**
         * Specifies the terms-of-service URL for the application.
         */
        public SignInIntentBuilder setTosUrl(@Nullable String tosUrl) {
            mTosUrl = tosUrl;
            return this;
        }

        /**
         * Specified the set of supported authentication providers. At least one provider must
         * be specified. There may only be one instance of each provider.
         * <p>
         * <p>If no providers are explicitly specified by calling this method, then the email
         * provider is the default supported provider.
         *
         * @param idpConfigs a list of {@link IdpConfig}s, where each {@link IdpConfig} contains the
         *                   configuration parameters for the IDP.
         * @see IdpConfig
         */
        public SignInIntentBuilder setProviders(@NonNull List<IdpConfig> idpConfigs) {
            mProviders.clear();
            Set<String> configuredProviders = new HashSet<>();
            for (IdpConfig idpConfig : idpConfigs) {
                if (configuredProviders.contains(idpConfig.getProviderId())) {
                    throw new IllegalArgumentException("Each provider can only be set once. "
                                                               + idpConfig.getProviderId()
                                                               + " was set twice.");
                }
                configuredProviders.add(idpConfig.getProviderId());
                mProviders.add(idpConfig);
            }
            return this;
        }

        /**
         * Specifies the set of supported authentication providers. At least one provider
         * must be specified, and the set of providers must be a subset of
         * {@link #SUPPORTED_PROVIDERS}. There may only be one instance of each provider.
         * <p>
         * <p>If no providers are explicitly specified by calling this method, then
         * {@link #EMAIL_PROVIDER email} is the default supported provider.
         *
         * @see #EMAIL_PROVIDER
         * @see #FACEBOOK_PROVIDER
         * @see #GOOGLE_PROVIDER
         */
        @Deprecated
        public SignInIntentBuilder setProviders(@NonNull String... providers) {
            mProviders.clear(); // clear the default email provider
            for (String provider : providers) {
                if (isIdpAlreadyConfigured(provider)) {
                    throw new IllegalArgumentException("Provider already configured: " + provider);
                }
                mProviders.add(new IdpConfig.Builder(provider).build());
            }
            return this;
        }

        /**
         * Enables or disables the use of Smart Lock for Passwords in the sign in flow.
         * <p>
         * <p>SmartLock is enabled by default.
         */
        public SignInIntentBuilder setIsSmartLockEnabled(boolean enabled) {
            mIsSmartLockEnabled = enabled;
            return this;
        }

        /**
         * Enables or disables creating new accounts in the email sign in flow.
         * <p>
         * <p>Account creation is enabled by default.
         */
        public SignInIntentBuilder setAllowNewEmailAccounts(boolean enabled) {
            mAllowNewEmailAccounts = enabled;
            return this;
        }

        private boolean isIdpAlreadyConfigured(@NonNull String providerId) {
            for (IdpConfig config : mProviders) {
                if (config.getProviderId().equals(providerId)) {
                    return true;
                }
            }
            return false;
        }

        public Intent build() {
            return KickoffActivity.createIntent(mApp.getApplicationContext(), getFlowParams());
        }

        @VisibleForTesting()
        public FlowParameters getFlowParams() {
            return new FlowParameters(mApp.getName(),
                                      new ArrayList<>(mProviders),
                                      mTheme,
                                      mLogo,
                                      mTosUrl,
                                      mIsSmartLockEnabled,
                                      mAllowNewEmailAccounts);
        }
    }
}
