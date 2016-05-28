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
import android.content.Context;
import android.content.Intent;
import android.support.annotation.DrawableRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StyleRes;

import com.firebase.ui.auth.provider.IDPProviderParcel;
import com.firebase.ui.auth.ui.FlowParameters;
import com.firebase.ui.auth.ui.ChooseAccountActivity;
import com.firebase.ui.auth.ui.idp.AuthMethodPickerActivity;
import com.firebase.ui.auth.util.CredentialsApiHelper;
import com.firebase.ui.auth.util.Preconditions;
import com.firebase.ui.auth.util.ProviderHelper;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;

/**
 * The entry point to the AuthUI authentication flow, and related utility methods.
 * If your application uses the default {@link FirebaseApp} instance, an AuthUI instance can
 * be retrieved simply by calling {@link AuthUI#getInstance() AuthUI.getInstance()}.
 * If an alternative app instance is in use, call
 * {@link AuthUI#getInstance(FirebaseApp) AuthUI.getInstance(app} instead, passing the
 * appropriate app instance.
 *
 * <h2>Sign-in</h2>
 *
 * If a user is not currently signed in (as can be determined by checking
 * {@code auth.getCurrentUser() != null}, where {@code auth} is the {@link FirebaseAuth}
 * associated with your {@link FirebaseApp}) then the sign-in process can be started by creating
 * a sign-in intent using {@link SignInIntentBuilder}. A builder instance can be retrieved by
 * calling {@link AuthUI#createSignInIntentBuilder()}.
 *
 * <p>The builder provides the following customization options for the authentication flow
 * implemented by this library:
 *
 * <ul>
 *     <li>The set of authentication methods desired can be specified.</li>
 *     <li>The terms of service URL for your app can be specified, which is included as a link
 *         in the small-print of the account creation step for new users. If no terms of service
 *         URL is provided, the associated small-print is omitted.
 *     </li>
 *     <li>A custom theme can specified for the flow, which is applied to all the activities in
 *         the flow for consistent customization of colors and typography.
 *     </li>
 * </ul>
 *
 *
 * <h3>Sign-in examples</h3>
 *
 * If no customization is required, and only email authentication is required, the sign-in flow
 * can be started as follows:
 *
 * <pre>
 * {@code
 * startActivityForResult(
 *     AuthUI.getInstance().createSignInIntentBuilder().build(),
 *     RC_SIGN_IN);
 * }
 * </pre>
 *
 * If Google Sign-in and Facebook Sign-in are also required, then this can be replaced with:
 *
 * <pre>
 * {@code
 * startActivityForResult(
 *     AuthUI.getInstance()
 *         .createSignInIntentBuilder()
 *         .setProviders(AuthUI.EMAIL_PROVIDER, AuthUI.GOOGLE_PROVIDER, AuthUI.FACEBOOK_PROVIDER)
 *         .build(),
 *     RC_SIGN_IN);
 * }
 * </pre>
 *
 * Finally, if a terms of service URL and a custom theme are required:
 *
 * <pre>
 * {@code
 * startActivityForResult(
 *     AuthUI.getInstance()
 *         .createSignInIntentBuilder()
 *         .setProviders(...)
 *         .setTosUrl("https://superapp.example.com/terms-of-service.html")
 *         .setTheme(R.style.SuperAppTheme)
 *         .build(),
 *     RC_SIGN_IN);
 * }
 * </pre>
 *
 * <h3>Handling the Sign-in response</h3>
 *
 * The authentication flow provides only two response codes: {@link Activity#RESULT_OK RESULT_OK}
 * if a user is signed in, and {@link Activity#RESULT_CANCELED RESULT_CANCELLED} if sign in
 * failed. No further information on failure is provided as it is not typically useful; the only
 * recourse for most apps if sign in fails is to ask the user to sign in again later, or proceed
 * with an anonymous account if supported.
 *
 * <pre>
 * {@code
 * @Override
 * protected void onActivityResult(int requestCode, int resultCode, Intent data) {
 *   super.onActivityResult(requestCode, resultCode, data);
 *   if (requestCode == RC_SIGN_IN) {
 *     if (resultCode == RESULT_OK) {
 *       // user is signed in!
 *       startActivity(new Intent(this, WelcomeBackActivity.class));
 *       finish();
 *     } else {
 *       // user is not signed in :(
 *       // Maybe just wait for the user to press "sign in" again, or show a message
 *       showSnackbar("Sign in is required to use this app.");
 *     }
 *   }
 * }
 * </pre>
 *
 * <h2>Sign-out</h2>
 *
 * With the integrations provided by AuthUI, signing out a user is a multi-stage process:
 *
 * <ol>
 *     <li>The user must be signed out of the {@link FirebaseAuth} instance.</li>
 *     <li>Smart Lock for Passwords must be instructed to disable automatic sign-in, in
 *         order to prevent an automatic sign-in loop that prevents the user from switching
 *         accounts.
 *     </li>
 *     <li>If the current user signed in using either Google or Facebook, the user must also be
 *         signed out using the associated API for that authentication method. This typically
 *         ensures that the user will not be automatically signed-in using the current account
 *         when using that authentication method again from the authentication method picker, which
 *         would also prevent the user from switching between accounts on the same provider.
 *     </li>
 * </ol>
 *
 * In order to make this process easier, AuthUI provides a simple
 * {@link AuthUI#signOut(Activity) signOut} method to encapsulate this behavior. The method returns
 * a {@link Task} which is marked completed once all necessary sign-out operations are completed:
 *
 * <pre>
 * {@code
 * public void onClick(View v) {
 *   if (v.getId() == R.id.sign_out) {
 *       AuthUI.getInstance()
 *           .signOut(this)
 *           .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
 *             public void onComplete(@NonNull Task<AuthResult> task) {
 *               // user is now signed out
 *               startActivity(new Intent(MyActivity.this, SignInActivity.class));
 *               finish();
 *             });
 *   }
 * }
 * </pre>
 *
 * <h2>IDP Provider configuration</h2>
 *
 * Interacting with identity providers typically requires some additional client configuration.
 * AuthUI currently supports Google Sign-in and Facebook Sign-in, and currently requires the
 * basic configuration for these providers to be specified via string properties:
 *
 * <ul>
 *
 * <li>Google Sign-in: If your app build uses the
 * <a href="https://developers.google.com/android/guides/google-services-plugin">Google
 * Services Gradle Plugin</a>, no additional configuration is required. If not, please override
 * {@code R.string.default_web_client_id} to provide your
 * <a href="https://developers.google.com/identity/sign-in/web/devconsole-project">Google OAuth
 * web client id.</a>
 * </li>
 *
 * <li>Facebook Sign-in: Please override the string resource
 * {@code facebook_application_id} to provide the
 * <a href="https://developers.facebook.com/docs/apps/register">App ID</a> for your app as
 * registered on the
 * <a href="https://developers.facebook.com/apps">Facebook Developer Dashboard</a>.
 * </li>
 *
 * </ul>
 */
public class AuthUI {

    /**
     * Provider identifier for email and password credentials, for use with
     * {@link SignInIntentBuilder#setProviders}.
     */
    public static final String EMAIL_PROVIDER = "email";

    /**
     * Provider identifier for Google, for use with {@link SignInIntentBuilder#setProviders}.
     */
    public static final String GOOGLE_PROVIDER = "google";

    /**
     * Provider identifier for Facebook, for use with {@link SignInIntentBuilder#setProviders}.
     */
    public static final String FACEBOOK_PROVIDER = "facebook";

    /**
     * Default value for logo resource, omits the logo from the
     * {@link AuthMethodPickerActivity}
     */
    public static final int NO_LOGO = -1;

    /**
     * The set of authentication providers supported in Firebase Auth UI.
     */
    public static final Set<String> SUPPORTED_PROVIDERS =
            Collections.unmodifiableSet(new HashSet<>(Arrays.asList(
                    EMAIL_PROVIDER,
                    GOOGLE_PROVIDER,
                    FACEBOOK_PROVIDER
            )));

    private static final IdentityHashMap<FirebaseApp, AuthUI> INSTANCES = new IdentityHashMap<>();

    private final FirebaseApp mApp;
    private final FirebaseAuth mAuth;

    private AuthUI(FirebaseApp app) {
        mApp = app;
        mAuth = FirebaseAuth.getInstance(mApp);
    }

    /**
     * Signs the current user out, if one is signed in.
     *
     * @param activity The activity requesting the user be signed out.
     * @return a task which, upon completion, signals that the user has been signed out
     * ({@code result.isSuccess()}, or that the sign-out attempt failed unexpectedly
     * ({@code !result.isSuccess()}).
     */
    public Task<Void> signOut(@NonNull Activity activity) {
        mAuth.signOut();
        return CredentialsApiHelper.getInstance(activity)
                .disableAutoSignIn()
                .continueWith(new Continuation<Status, Void>() {
                    @Override
                    public Void then(@NonNull Task<Status> task) throws Exception {
                        return null;
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
     * Retrieves the {@link AuthUI} instance associated with the default app, as returned by
     * {@code FirebaseApp.getInstance()}.
     * @throws IllegalStateException if the default app is not initialized.
     */
    public static AuthUI getInstance() {
        return getInstance(FirebaseApp.getInstance());
    }

    /**
     * Retrieves the {@link AuthUI} instance associated  the the specified app.
     */
    public static AuthUI getInstance(FirebaseApp app) {
        AuthUI authUi = null;
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
     * Default theme used by {@link SignInIntentBuilder#setTheme(int)} if no theme
     * customization is required.
     */
    public static @StyleRes int getDefaultTheme() {
        // TODO(iainmgin): figure out why this works as a static method but not as a static
        //                 final variable.
        return R.style.FirebaseUI;
    }

    /**
     * Builder for the intent to start the user authentication flow.
     */
    public final class SignInIntentBuilder {
        private int mLogo = NO_LOGO;
        private int mTheme = getDefaultTheme();
        private List<String> mProviders = Collections.singletonList(EMAIL_PROVIDER);
        private String mTosUrl;

        private SignInIntentBuilder() {}

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
         * Specifies the set of supported authentication providers. At least one provider
         * must be specified, and the set of providers must be a subset of
         * {@link #SUPPORTED_PROVIDERS}.
         *
         * <p>If no providers are explicitly specified by calling this method, then
         * {@link #EMAIL_PROVIDER email} is the default supported provider.
         *
         * @see #EMAIL_PROVIDER
         * @see #FACEBOOK_PROVIDER
         * @see #GOOGLE_PROVIDER
         */
        public SignInIntentBuilder setProviders(@NonNull String... providers) {
            mProviders = Arrays.asList(providers);
            for (String provider : mProviders) {
                if (!SUPPORTED_PROVIDERS.contains(provider)) {
                    throw new IllegalArgumentException("Unknown provider: " + provider);
                }
            }
            return this;
        }

        public Intent build() {
            Context context = mApp.getApplicationContext();
            List<IDPProviderParcel> providerInfo =
                    ProviderHelper.getProviderParcels(context, mProviders);
            return ChooseAccountActivity.createIntent(
                    context,
                    new FlowParameters(
                            mApp.getName(),
                            providerInfo,
                            mTheme,
                            mLogo,
                            mTosUrl));
        }
    }
}
