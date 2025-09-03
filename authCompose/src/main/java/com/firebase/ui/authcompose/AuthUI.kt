package com.firebase.ui.authcompose

import com.google.firebase.FirebaseApp
import android.content.Context
import android.content.Intent
import androidx.annotation.StringDef
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.firebase.core.IdpConfig
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FacebookAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.PhoneAuthProvider

class AuthUI(app: FirebaseApp) {
    private val firebaseAuth: FirebaseAuth = FirebaseAuth.getInstance(app)

    companion object {
        @Composable
        fun initialize(
            providers: List<IdpConfig>,
            firebaseApp: FirebaseApp = FirebaseApp.getInstance(),
            onAuthSuccess: (FirebaseUser) -> Unit,
            onAuthFailure: (Exception) -> Unit,
            onAuthCancelled: () -> Unit = {},
        ) {
            Column {
                providers.forEach { provider ->
                    when (provider.providerId) {
                        GoogleAuthProvider.PROVIDER_ID -> {
                            GoogleSignInButton(
                                config = provider,
                                firebaseAuth = remember {
                                    FirebaseAuth.getInstance(firebaseApp)
                                },
                                onSuccess = onAuthSuccess,
                                onFailure = onAuthFailure,
                                onCancelled = onAuthCancelled,
                            )
                        }

                        EmailAuthProvider.PROVIDER_ID -> {

                        }
                    }
                }
            }
        }
    }
}

class AuthUISDK(app: FirebaseApp) {
    private val firebaseAuth: FirebaseAuth = FirebaseAuth.getInstance(app)

    val isUseEmulator: Boolean = false

    companion object {
        /**
         * The set of authentication providers supported in Firebase Auth UI.
         */
        val SUPPORTED_PROVIDERS = listOf<String>(
            GoogleAuthProvider.PROVIDER_ID,
            EmailAuthProvider.PROVIDER_ID,
            PhoneAuthProvider.PROVIDER_ID,
        )

        /**
         * The set of OAuth2.0 providers supported in Firebase Auth UI through Generic
         * IDP (web flow).
         */
        val SUPPORTED_OAUTH_PROVIDERS = listOf<String>(
            "microsoft.com",
            "yahoo.com",
            "apple.com",
        )

        /**
         * The set of social authentication providers supported in Firebase Auth UI using their SDK.
         */
        val SOCIAL_PROVIDERS = listOf<String>(
            GoogleAuthProvider.PROVIDER_ID,
            FacebookAuthProvider.PROVIDER_ID,
        )

        /**
         * Retrieves the {@link AuthUI} instance associated with the default app, as returned by {@code
         * FirebaseApp.getInstance()}.
         *
         * @throws IllegalStateException if the default app is not initialized.
         */
        fun getInstance(): AuthUI {
            val defaultApp = FirebaseApp.getInstance()
            return Singleton.getInstance(defaultApp)
        }

        /**
         * Retrieves the {@link AuthUI} instance associated the the specified app name.
         *
         * @throws IllegalStateException if the app is not initialized.
         */
        fun getInstance(appName: String): AuthUI {
            val app = FirebaseApp.getInstance(appName)
            return Singleton.getInstance(app)
        }

        /**
         * Retrieves the {@link AuthUI} instance associated the the specified app.
         */
        fun getInstance(app: FirebaseApp): AuthUI {
            return Singleton.getInstance(app)
        }

        private object Singleton {
            private val instances = mutableMapOf<FirebaseApp, AuthUI>()

            @Synchronized
            fun getInstance(app: FirebaseApp): AuthUI {
                return instances.getOrPut(app) {
                    AuthUI(app)
                }
            }
        }
    }

    fun getApp(): FirebaseApp {
        TODO("Not yet implemented, getApp(): FirebaseApp")
    }

    fun getAuth(): FirebaseAuth {
        TODO("Not yet implemented, getAuth(): FirebaseAuth")
    }

    /**
     * Returns true if AuthUI can handle the intent.
     * <p>
     * AuthUI handle the intent when the embedded data is an email link. If it is, you can then
     * specify the link in {@link SignInIntentBuilder#setEmailLink(String)} before starting AuthUI
     * and it will be handled immediately.
     */
    fun canHandleIntent(intent: Intent): Boolean {
        TODO("Not yet implemented, canHandleIntent(intent: Intent): Boolean")
    }

    /**
     * Default theme used by {@link SignInIntentBuilder#setTheme(int)} if no theme customization is
     * required.
     */
    fun getDefaultTheme(): Int {
        TODO("Not yet implemented, getDefaultTheme(): Int")
    }

    /**
     * Signs the current user out, if one is signed in.
     *
     * @param context the context requesting the user be signed out
     * @return A task which, upon completion, signals that the user has been signed out ({@link
     * Task#isSuccessful()}, or that the sign-out attempt failed unexpectedly !{@link
     * Task#isSuccessful()}).
     */
    fun signOut(context: Context): Task<Void> {
        TODO("Not yet implemented, signOut(context: Context): Task<Void>")
    }

    /**
     * Delete the user from FirebaseAuth.
     *
     * <p>Any associated saved credentials are not explicitly deleted with the new APIs.
     *
     * @param context the calling {@link Context}.
     */
    fun delete(context: Context): Task<Void> {
        TODO("Not yet implemented, delete(context: Context): Task<Void>")
    }

    /**
     * Connect to the Firebase Authentication emulator.
     * @see FirebaseAuth#useEmulator(String, int)
     */
    fun useEmulator(): Unit {
        TODO("Not yet implemented, useEmulator(): Unit")
    }

    fun getEmulatorHost() {
        TODO("Not yet implemented, getEmulatorHost()")
    }

    fun getEmulatorPort() {
        TODO("Not yet implemented, getEmulatorPort()")
    }

    fun signOutIdps(context: Context) {
        TODO("Not yet implemented, signOutIdps(context: Context)")
    }

    /**
     * A Task to clear the credential state in Credential Manager.
     * @param context
     * @param executor
     * @return
     */
    fun clearCredentialState(context: Context): Task<Void> {
        TODO("Not yet implemented, clearCredentialState(context: Context): Task<Void>")
    }

    /**
     * Starts the process of creating a sign in intent, with the mandatory application context
     * parameter.
     */
    fun createSignInIntentBuilder(): SignInIntentBuilder {
        TODO("Not yet implemented, createSignInIntentBuilder(): SignInIntentBuilder")
    }

    @StringDef(
        GoogleAuthProvider.PROVIDER_ID,
        EmailAuthProvider.PROVIDER_ID,
        PhoneAuthProvider.PROVIDER_ID,
    )
    @Retention(AnnotationRetention.SOURCE)
    annotation class SupportedProvider
}

abstract class AuthIntentBuilder<T : AuthIntentBuilder<T>> {

}

class SignInIntentBuilder : AuthIntentBuilder<SignInIntentBuilder>() {

}
