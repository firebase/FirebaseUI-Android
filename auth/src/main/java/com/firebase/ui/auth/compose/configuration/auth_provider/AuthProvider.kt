/*
 * Copyright 2025 Google Inc. All Rights Reserved.
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

package com.firebase.ui.auth.compose.configuration.auth_provider

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.compose.ui.graphics.Color
import androidx.datastore.preferences.core.stringPreferencesKey
import com.firebase.ui.auth.R
import com.firebase.ui.auth.compose.configuration.AuthUIConfiguration
import com.firebase.ui.auth.compose.configuration.AuthUIConfigurationDsl
import com.firebase.ui.auth.compose.configuration.PasswordRule
import com.firebase.ui.auth.compose.configuration.theme.AuthUIAsset
import com.firebase.ui.auth.util.Preconditions
import com.firebase.ui.auth.util.data.ContinueUrlBuilder
import com.firebase.ui.auth.util.data.PhoneNumberUtils
import com.firebase.ui.auth.util.data.ProviderAvailability
import com.google.firebase.FirebaseException
import com.google.firebase.auth.ActionCodeSettings
import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FacebookAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GithubAuthProvider
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.MultiFactorSession
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import com.google.firebase.auth.TwitterAuthProvider
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.auth.actionCodeSettings
import kotlinx.coroutines.tasks.await
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

@AuthUIConfigurationDsl
class AuthProvidersBuilder {
    private val providers = mutableListOf<AuthProvider>()

    fun provider(provider: AuthProvider) {
        providers.add(provider)
    }

    internal fun build(): List<AuthProvider> = providers.toList()
}

/**
 * Enum class to represent all possible providers.
 */
internal enum class Provider(val id: String, val isSocialProvider: Boolean = false) {
    GOOGLE(GoogleAuthProvider.PROVIDER_ID, isSocialProvider = true),
    FACEBOOK(FacebookAuthProvider.PROVIDER_ID, isSocialProvider = true),
    TWITTER(TwitterAuthProvider.PROVIDER_ID, isSocialProvider = true),
    GITHUB(GithubAuthProvider.PROVIDER_ID, isSocialProvider = true),
    EMAIL(EmailAuthProvider.PROVIDER_ID),
    PHONE(PhoneAuthProvider.PROVIDER_ID),
    ANONYMOUS("anonymous"),
    MICROSOFT("microsoft.com"),
    YAHOO("yahoo.com"),
    APPLE("apple.com");

    companion object {
        fun fromId(id: String): Provider? {
            return entries.find { it.id == id }
        }
    }
}

/**
 * Base abstract class for OAuth authentication providers with common properties.
 */
abstract class OAuthProvider(
    override val providerId: String,
    open val scopes: List<String> = emptyList(),
    open val customParameters: Map<String, String> = emptyMap(),
) : AuthProvider(providerId)

/**
 * Base abstract class for authentication providers.
 */
abstract class AuthProvider(open val providerId: String) {

    companion object {
        internal fun canUpgradeAnonymous(config: AuthUIConfiguration, auth: FirebaseAuth): Boolean {
            val currentUser = auth.currentUser
            return config.isAnonymousUpgradeEnabled
                    && currentUser != null
                    && currentUser.isAnonymous
        }

        /**
         * Merges profile information (display name and photo URL) with the current user's profile.
         *
         * This method updates the user's profile only if the current profile is incomplete
         * (missing display name or photo URL). This prevents overwriting existing profile data.
         *
         * **Use case:**
         * After creating a new user account or linking credentials, update the profile with
         * information from the sign-up form or social provider.
         *
         * @param auth The [FirebaseAuth] instance
         * @param displayName The display name to set (if current is empty)
         * @param photoUri The photo URL to set (if current is null)
         *
         * **Note:** This operation always succeeds to minimize login interruptions.
         * Failures are logged but don't prevent sign-in completion.
         */
        internal suspend fun mergeProfile(
            auth: FirebaseAuth,
            displayName: String?,
            photoUri: Uri?,
        ) {
            try {
                val currentUser = auth.currentUser ?: return

                // Only update if current profile is incomplete
                val currentDisplayName = currentUser.displayName
                val currentPhotoUrl = currentUser.photoUrl

                if (!currentDisplayName.isNullOrEmpty() && currentPhotoUrl != null) {
                    // Profile is complete, no need to update
                    return
                }

                // Build profile update with provided values
                val nameToSet =
                    if (currentDisplayName.isNullOrEmpty()) displayName else currentDisplayName
                val photoToSet = currentPhotoUrl ?: photoUri

                if (nameToSet != null || photoToSet != null) {
                    val profileUpdates = UserProfileChangeRequest.Builder()
                        .setDisplayName(nameToSet)
                        .setPhotoUri(photoToSet)
                        .build()

                    currentUser.updateProfile(profileUpdates).await()
                }
            } catch (e: Exception) {
                // Log error but don't throw - profile update failure shouldn't prevent sign-in
                Log.e("AuthProvider.Email", "Error updating profile", e)
            }
        }
    }

    /**
     * Email/Password authentication provider configuration.
     */
    class Email(
        /**
         * Requires the user to provide a display name. Defaults to true.
         */
        val isDisplayNameRequired: Boolean = true,

        /**
         * Enables email link sign-in, Defaults to false.
         */
        val isEmailLinkSignInEnabled: Boolean = false,

        /**
         * Forces email link sign-in to complete on the same device that initiated it.
         *
         * When enabled, prevents email links from being opened on different devices,
         * which is required for security when upgrading anonymous users. Defaults to true.
         */
        val isEmailLinkForceSameDeviceEnabled: Boolean = true,

        /**
         * Settings for email link actions.
         */
        val emailLinkActionCodeSettings: ActionCodeSettings?,

        /**
         * Allows new accounts to be created. Defaults to true.
         */
        val isNewAccountsAllowed: Boolean = true,

        /**
         * The minimum length for a password. Defaults to 6.
         */
        val minimumPasswordLength: Int = 6,

        /**
         * A list of custom password validation rules.
         */
        val passwordValidationRules: List<PasswordRule>,
    ) : AuthProvider(providerId = Provider.EMAIL.id) {
        companion object {
            const val SESSION_ID_LENGTH = 10
            val KEY_EMAIL = stringPreferencesKey("com.firebase.ui.auth.data.client.email")
            val KEY_PROVIDER = stringPreferencesKey("com.firebase.ui.auth.data.client.provider")
            val KEY_ANONYMOUS_USER_ID =
                stringPreferencesKey("com.firebase.ui.auth.data.client.auid")
            val KEY_SESSION_ID = stringPreferencesKey("com.firebase.ui.auth.data.client.sid")
            val KEY_IDP_TOKEN = stringPreferencesKey("com.firebase.ui.auth.data.client.idpToken")
            val KEY_IDP_SECRET = stringPreferencesKey("com.firebase.ui.auth.data.client.idpSecret")
        }

        internal fun validate(isAnonymousUpgradeEnabled: Boolean = false) {
            if (isEmailLinkSignInEnabled) {
                val actionCodeSettings = requireNotNull(emailLinkActionCodeSettings) {
                    "ActionCodeSettings cannot be null when using " +
                            "email link sign in."
                }

                check(actionCodeSettings.canHandleCodeInApp()) {
                    "You must set canHandleCodeInApp in your " +
                            "ActionCodeSettings to true for Email-Link Sign-in."
                }

                if (isAnonymousUpgradeEnabled) {
                    check(isEmailLinkForceSameDeviceEnabled) {
                        "You must force the same device flow when using email link sign in " +
                                "with anonymous user upgrade"
                    }
                }
            }
        }

        // For Send Email Link
        internal fun addSessionInfoToActionCodeSettings(
            sessionId: String,
            anonymousUserId: String,
        ): ActionCodeSettings {
            requireNotNull(emailLinkActionCodeSettings) {
                "ActionCodeSettings is required for email link sign in"
            }

            val continueUrl = continueUrl(emailLinkActionCodeSettings.url) {
                appendSessionId(sessionId)
                appendAnonymousUserId(anonymousUserId)
                appendForceSameDeviceBit(isEmailLinkForceSameDeviceEnabled)
                appendProviderId(providerId)
            }

            return actionCodeSettings {
                url = continueUrl
                handleCodeInApp = emailLinkActionCodeSettings.canHandleCodeInApp()
                iosBundleId = emailLinkActionCodeSettings.iosBundle
                setAndroidPackageName(
                    emailLinkActionCodeSettings.androidPackageName ?: "",
                    emailLinkActionCodeSettings.androidInstallApp,
                    emailLinkActionCodeSettings.androidMinimumVersion
                )
            }
        }

        // For Sign In With Email Link
        internal fun isDifferentDevice(
            sessionIdFromLocal: String?,
            sessionIdFromLink: String,
        ): Boolean {
            return sessionIdFromLocal == null || sessionIdFromLocal.isEmpty()
                    || sessionIdFromLink.isEmpty()
                    || (sessionIdFromLink != sessionIdFromLocal)
        }

        private fun continueUrl(continueUrl: String, block: ContinueUrlBuilder.() -> Unit) =
            ContinueUrlBuilder(continueUrl).apply(block).build()

        /**
         * An interface to wrap the static `EmailAuthProvider.getCredential` method to make it testable.
         * @suppress
         */
        internal interface CredentialProvider {
            fun getCredential(email: String, password: String): AuthCredential
        }

        /**
         * The default implementation of [CredentialProvider] that calls the static method.
         * @suppress
         */
        internal class DefaultCredentialProvider : CredentialProvider {
            override fun getCredential(email: String, password: String): AuthCredential {
                return EmailAuthProvider.getCredential(email, password)
            }
        }
    }

    /**
     * Phone number authentication provider configuration.
     */
    class Phone(
        /**
         * The phone number in international format.
         */
        val defaultNumber: String?,

        /**
         * The default country code to pre-select.
         */
        val defaultCountryCode: String?,

        /**
         * A list of allowed country codes.
         */
        val allowedCountries: List<String>?,

        /**
         * The expected length of the SMS verification code. Defaults to 6.
         */
        val smsCodeLength: Int = 6,

        /**
         * The timeout in seconds for receiving the SMS. Defaults to 60L.
         */
        val timeout: Long = 60L,

        /**
         * Enables instant verification of the phone number. Defaults to true.
         */
        val isInstantVerificationEnabled: Boolean = true,
    ) : AuthProvider(providerId = Provider.PHONE.id) {
        /**
         * Sealed class representing the result of phone number verification.
         *
         * Phone verification can complete in two ways:
         * - [AutoVerified]: SMS was instantly retrieved and verified by the Firebase SDK
         * - [NeedsManualVerification]: SMS code was sent, user must manually enter it
         */
        internal sealed class VerifyPhoneNumberResult {
            /**
             * Instant verification succeeded via SMS auto-retrieval.
             *
             * @property credential The [PhoneAuthCredential] that can be used to sign in
             */
            class AutoVerified(val credential: PhoneAuthCredential) : VerifyPhoneNumberResult()

            /**
             * Instant verification failed, manual code entry required.
             *
             * @property verificationId The verification ID to use when submitting the code
             * @property token Token for resending the verification code
             */
            class NeedsManualVerification(
                val verificationId: String,
                val token: PhoneAuthProvider.ForceResendingToken,
            ) : VerifyPhoneNumberResult()
        }

        internal fun validate() {
            defaultNumber?.let {
                check(PhoneNumberUtils.isValid(it)) {
                    "Invalid phone number: $it"
                }
            }

            defaultCountryCode?.let {
                check(PhoneNumberUtils.isValidIso(it)) {
                    "Invalid country iso: $it"
                }
            }

            allowedCountries?.forEach { code ->
                check(
                    PhoneNumberUtils.isValidIso(code) ||
                            PhoneNumberUtils.isValid(code)
                ) {
                    "Invalid input: You must provide a valid country iso (alpha-2) " +
                            "or code (e-164). e.g. 'us' or '+1'. Invalid code: $code"
                }
            }
        }

        /**
         * Internal coroutine-based wrapper for Firebase Phone Authentication verification.
         *
         * This method wraps the callback-based Firebase Phone Auth API into a suspending function
         * using Kotlin coroutines. It handles the Firebase [PhoneAuthProvider.OnVerificationStateChangedCallbacks]
         * and converts them into a [VerifyPhoneNumberResult].
         *
         * **Callback mapping:**
         * - `onVerificationCompleted` → [VerifyPhoneNumberResult.AutoVerified]
         * - `onCodeSent` → [VerifyPhoneNumberResult.NeedsManualVerification]
         * - `onVerificationFailed` → throws the exception
         *
         * This is a private helper method used by [verifyPhoneNumber]. Callers should use
         * [verifyPhoneNumber] instead as it handles state management and error handling.
         *
         * @param auth The [FirebaseAuth] instance to use for verification
         * @param phoneNumber The phone number to verify in E.164 format
         * @param multiFactorSession Optional [MultiFactorSession] for MFA enrollment. When provided,
         * Firebase verifies the phone number for enrolling as a second authentication factor
         * instead of primary sign-in. Pass null for standard phone authentication.
         * @param forceResendingToken Optional token from previous verification for resending
         *
         * @return [VerifyPhoneNumberResult] indicating auto-verified or manual verification needed
         * @throws FirebaseException if verification fails
         */
        internal suspend fun verifyPhoneNumberAwait(
            auth: FirebaseAuth,
            phoneNumber: String,
            multiFactorSession: MultiFactorSession? = null,
            forceResendingToken: PhoneAuthProvider.ForceResendingToken?,
            verifier: Verifier = DefaultVerifier(),
        ): VerifyPhoneNumberResult {
            return verifier.verifyPhoneNumber(
                auth,
                phoneNumber,
                timeout,
                forceResendingToken,
                multiFactorSession,
                isInstantVerificationEnabled
            )
        }

        /**
         * @suppress
         */
        internal interface Verifier {
            suspend fun verifyPhoneNumber(
                auth: FirebaseAuth,
                phoneNumber: String,
                timeout: Long,
                forceResendingToken: PhoneAuthProvider.ForceResendingToken?,
                multiFactorSession: MultiFactorSession?,
                isInstantVerificationEnabled: Boolean
            ): VerifyPhoneNumberResult
        }

        /**
         * @suppress
         */
        internal class DefaultVerifier : Verifier {
            override suspend fun verifyPhoneNumber(
                auth: FirebaseAuth,
                phoneNumber: String,
                timeout: Long,
                forceResendingToken: PhoneAuthProvider.ForceResendingToken?,
                multiFactorSession: MultiFactorSession?,
                isInstantVerificationEnabled: Boolean
            ): VerifyPhoneNumberResult {
                return suspendCoroutine { continuation ->
                    val options = PhoneAuthOptions.newBuilder(auth)
                        .setPhoneNumber(phoneNumber)
                        .requireSmsValidation(!isInstantVerificationEnabled)
                        .setTimeout(timeout, TimeUnit.SECONDS)
                        .setCallbacks(object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                            override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                                continuation.resume(VerifyPhoneNumberResult.AutoVerified(credential))
                            }

                            override fun onVerificationFailed(e: FirebaseException) {
                                continuation.resumeWithException(e)
                            }

                            override fun onCodeSent(
                                verificationId: String,
                                token: PhoneAuthProvider.ForceResendingToken,
                            ) {
                                continuation.resume(
                                    VerifyPhoneNumberResult.NeedsManualVerification(
                                        verificationId,
                                        token
                                    )
                                )
                            }
                        })
                    if (forceResendingToken != null) {
                        options.setForceResendingToken(forceResendingToken)
                    }
                    if (multiFactorSession != null) {
                        options.setMultiFactorSession(multiFactorSession)
                    }
                    PhoneAuthProvider.verifyPhoneNumber(options.build())
                }
            }
        }

        /**
         * An interface to wrap the static `PhoneAuthProvider.getCredential` method to make it testable.
         * @suppress
         */
        internal interface CredentialProvider {
            fun getCredential(verificationId: String, smsCode: String): PhoneAuthCredential
        }

        /**
         * The default implementation of [CredentialProvider] that calls the static method.
         * @suppress
         */
        internal class DefaultCredentialProvider : CredentialProvider {
            override fun getCredential(verificationId: String, smsCode: String): PhoneAuthCredential {
                return PhoneAuthProvider.getCredential(verificationId, smsCode)
            }
        }

    }

    /**
     * Google Sign-In provider configuration.
     */
    class Google(
        /**
         * The list of scopes to request.
         */
        override val scopes: List<String>,

        /**
         * The OAuth 2.0 client ID for your server.
         */
        val serverClientId: String?,

        /**
         * Requests an ID token. Default to true.
         */
        val requestIdToken: Boolean = true,

        /**
         * Requests the user's profile information. Defaults to true.
         */
        val requestProfile: Boolean = true,

        /**
         * Requests the user's email address. Defaults to true.
         */
        val requestEmail: Boolean = true,

        /**
         * A map of custom OAuth parameters.
         */
        override val customParameters: Map<String, String> = emptyMap(),
    ) : OAuthProvider(
        providerId = Provider.GOOGLE.id,
        scopes = scopes,
        customParameters = customParameters
    ) {
        internal fun validate(context: Context) {
            if (serverClientId == null) {
                Preconditions.checkConfigured(
                    context,
                    "Check your google-services plugin configuration, the" +
                            " default_web_client_id string wasn't populated.",
                    R.string.default_web_client_id
                )
            } else {
                require(serverClientId.isNotBlank()) {
                    "Server client ID cannot be blank."
                }
            }

            val hasEmailScope = scopes.contains("email")
            if (!hasEmailScope) {
                Log.w(
                    "AuthProvider.Google",
                    "The scopes do not include 'email'. In most cases this is a mistake!"
                )
            }
        }
    }

    /**
     * Facebook Login provider configuration.
     */
    class Facebook(
        /**
         * The Facebook application ID.
         */
        val applicationId: String? = null,

        /**
         * The list of scopes (permissions) to request. Defaults to email and public_profile.
         */
        override val scopes: List<String> = listOf("email", "public_profile"),

        /**
         * if true, enable limited login mode. Defaults to false.
         */
        val limitedLogin: Boolean = false,

        /**
         * A map of custom OAuth parameters.
         */
        override val customParameters: Map<String, String> = emptyMap(),
    ) : OAuthProvider(
        providerId = Provider.FACEBOOK.id,
        scopes = scopes,
        customParameters = customParameters
    ) {
        internal fun validate(context: Context) {
            if (!ProviderAvailability.IS_FACEBOOK_AVAILABLE) {
                throw RuntimeException(
                    "Facebook provider cannot be configured " +
                            "without dependency. Did you forget to add " +
                            "'com.facebook.android:facebook-login:VERSION' dependency?"
                )
            }

            if (applicationId == null) {
                Preconditions.checkConfigured(
                    context,
                    "Facebook provider unconfigured. Make sure to " +
                            "add a `facebook_application_id` string or provide applicationId parameter.",
                    R.string.facebook_application_id
                )
            } else {
                require(applicationId.isNotBlank()) {
                    "Facebook application ID cannot be blank"
                }
            }
        }
    }

    /**
     * Twitter/X authentication provider configuration.
     */
    class Twitter(
        /**
         * A map of custom OAuth parameters.
         */
        override val customParameters: Map<String, String>,
    ) : OAuthProvider(
        providerId = Provider.TWITTER.id,
        customParameters = customParameters
    )

    /**
     * Github authentication provider configuration.
     */
    class Github(
        /**
         * The list of scopes to request. Defaults to user:email.
         */
        override val scopes: List<String> = listOf("user:email"),

        /**
         * A map of custom OAuth parameters.
         */
        override val customParameters: Map<String, String>,
    ) : OAuthProvider(
        providerId = Provider.GITHUB.id,
        scopes = scopes,
        customParameters = customParameters
    )

    /**
     * Microsoft authentication provider configuration.
     */
    class Microsoft(
        /**
         * The list of scopes to request. Defaults to openid, profile, email.
         */
        override val scopes: List<String> = listOf("openid", "profile", "email"),

        /**
         * The tenant ID for Azure Active Directory.
         */
        val tenant: String?,

        /**
         * A map of custom OAuth parameters.
         */
        override val customParameters: Map<String, String>,
    ) : OAuthProvider(
        providerId = Provider.MICROSOFT.id,
        scopes = scopes,
        customParameters = customParameters
    )

    /**
     * Yahoo authentication provider configuration.
     */
    class Yahoo(
        /**
         * The list of scopes to request. Defaults to openid, profile, email.
         */
        override val scopes: List<String> = listOf("openid", "profile", "email"),

        /**
         * A map of custom OAuth parameters.
         */
        override val customParameters: Map<String, String>,
    ) : OAuthProvider(
        providerId = Provider.YAHOO.id,
        scopes = scopes,
        customParameters = customParameters
    )

    /**
     * Apple Sign-In provider configuration.
     */
    class Apple(
        /**
         * The list of scopes to request. Defaults to name and email.
         */
        override val scopes: List<String> = listOf("name", "email"),

        /**
         * The locale for the sign-in page.
         */
        val locale: String?,

        /**
         * A map of custom OAuth parameters.
         */
        override val customParameters: Map<String, String>,
    ) : OAuthProvider(
        providerId = Provider.APPLE.id,
        scopes = scopes,
        customParameters = customParameters
    )

    /**
     * Anonymous authentication provider. It has no configurable properties.
     */
    object Anonymous : AuthProvider(providerId = Provider.ANONYMOUS.id) {
        internal fun validate(providers: List<AuthProvider>) {
            if (providers.size == 1 && providers.first() is Anonymous) {
                throw IllegalStateException(
                    "Sign in as guest cannot be the only sign in method. " +
                            "In this case, sign the user in anonymously your self; no UI is needed."
                )
            }
        }
    }

    /**
     * A generic OAuth provider for any unsupported provider.
     */
    class GenericOAuth(
        /**
         * The provider ID as configured in the Firebase console.
         */
        override val providerId: String,

        /**
         * The list of scopes to request.
         */
        override val scopes: List<String>,

        /**
         * A map of custom OAuth parameters.
         */
        override val customParameters: Map<String, String>,

        /**
         * The text to display on the provider button.
         */
        val buttonLabel: String,

        /**
         * An optional icon for the provider button.
         */
        val buttonIcon: AuthUIAsset?,

        /**
         * An optional background color for the provider button.
         */
        val buttonColor: Color?,

        /**
         * An optional content color for the provider button.
         */
        val contentColor: Color?,
    ) : OAuthProvider(
        providerId = providerId,
        scopes = scopes,
        customParameters = customParameters
    ) {
        internal fun validate() {
            require(providerId.isNotBlank()) {
                "Provider ID cannot be null or empty"
            }

            require(buttonLabel.isNotBlank()) {
                "Button label cannot be null or empty"
            }
        }
    }
}
