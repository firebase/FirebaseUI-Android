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

package com.firebase.ui.auth.compose.configuration

import android.content.Context
import android.graphics.Color
import android.util.Log
import androidx.compose.ui.graphics.vector.ImageVector
import com.firebase.ui.auth.AuthUI
import com.firebase.ui.auth.R
import com.firebase.ui.auth.util.Preconditions
import com.firebase.ui.auth.util.data.PhoneNumberUtils
import com.firebase.ui.auth.util.data.ProviderAvailability
import com.google.firebase.auth.ActionCodeSettings
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FacebookAuthProvider
import com.google.firebase.auth.GithubAuthProvider
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.PhoneAuthProvider
import com.google.firebase.auth.TwitterAuthProvider

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
internal enum class Provider(val id: String) {
    GOOGLE(GoogleAuthProvider.PROVIDER_ID),
    FACEBOOK(FacebookAuthProvider.PROVIDER_ID),
    TWITTER(TwitterAuthProvider.PROVIDER_ID),
    GITHUB(GithubAuthProvider.PROVIDER_ID),
    EMAIL(EmailAuthProvider.PROVIDER_ID),
    PHONE(PhoneAuthProvider.PROVIDER_ID),
    ANONYMOUS("anonymous"),
    MICROSOFT("microsoft.com"),
    YAHOO("yahoo.com"),
    APPLE("apple.com"),
}

/**
 * Base abstract class for OAuth authentication providers with common properties.
 */
abstract class OAuthProvider(
    override val providerId: String,
    open val scopes: List<String> = emptyList(),
    open val customParameters: Map<String, String> = emptyMap()
) : AuthProvider(providerId)

/**
 * Base abstract class for authentication providers.
 */
abstract class AuthProvider(open val providerId: String) {
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
        val actionCodeSettings: ActionCodeSettings?,

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
        val passwordValidationRules: List<PasswordRule>
    ) : AuthProvider(providerId = Provider.EMAIL.id) {
        fun validate() {
            if (isEmailLinkSignInEnabled) {
                val actionCodeSettings = requireNotNull(actionCodeSettings) {
                    "ActionCodeSettings cannot be null when using " +
                            "email link sign in."
                }

                check(actionCodeSettings.canHandleCodeInApp()) {
                    "You must set canHandleCodeInApp in your " +
                            "ActionCodeSettings to true for Email-Link Sign-in."
                }
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

        /**
         * Enables automatic retrieval of the SMS code. Defaults to true.
         */
        val isAutoRetrievalEnabled: Boolean = true
    ) : AuthProvider(providerId = Provider.PHONE.id) {
        fun validate() {
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
        override val customParameters: Map<String, String> = emptyMap()
    ) : OAuthProvider(
        providerId = Provider.GOOGLE.id,
        scopes = scopes,
        customParameters = customParameters
    ) {
        fun validate(context: Context) {
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
        override val customParameters: Map<String, String> = emptyMap()
    ) : OAuthProvider(
        providerId = Provider.FACEBOOK.id,
        scopes = scopes,
        customParameters = customParameters
    ) {
        fun validate(context: Context) {
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
        override val customParameters: Map<String, String>
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
        override val customParameters: Map<String, String>
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
        override val customParameters: Map<String, String>
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
        override val customParameters: Map<String, String>
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
        override val customParameters: Map<String, String>
    ) : OAuthProvider(
        providerId = Provider.APPLE.id,
        scopes = scopes,
        customParameters = customParameters
    )

    /**
     * Anonymous authentication provider. It has no configurable properties.
     */
    object Anonymous : AuthProvider(providerId = Provider.ANONYMOUS.id) {
        fun validate(providers: List<AuthProvider>) {
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
        val buttonIcon: ImageVector?,

        /**
         * An optional background color for the provider button.
         */
        val buttonColor: Color?
    ) : OAuthProvider(
        providerId = providerId,
        scopes = scopes,
        customParameters = customParameters
    ) {
        fun validate() {
            require(providerId.isNotBlank()) {
                "Provider ID cannot be null or empty"
            }

            require(buttonLabel.isNotBlank()) {
                "Button label cannot be null or empty"
            }
        }
    }
}