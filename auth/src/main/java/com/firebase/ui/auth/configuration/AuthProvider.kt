package com.firebase.ui.auth.configuration

import android.graphics.Color
import com.firebase.ui.auth.configuration.PasswordRule
import com.google.firebase.auth.ActionCodeSettings

/**
 * Base sealed class for authentication providers.
 */
sealed class AuthProvider() {
    /**
     * Email/Password authentication provider configuration.
     */
    data class EmailAuthProvider(
        /**
         * Requires the user to provide a display name. Defaults to true.
         */
        val requireDisplayName: Boolean = true,

        /**
         * Enables email link sign-in, Defaults to false.
         */
        val enableEmailLinkSignIn: Boolean = false,

        /**
         * Settings for email link actions.
         */
        val actionCodeSettings: ActionCodeSettings?,

        /**
         * Allows new accounts to be created. Defaults to true.
         */
        val allowNewAccounts: Boolean = true,

        /**
         * The minimum length for a password. Defaults to 6.
         */
        val minimumPasswordLength: Int = 6,

        /**
         * A list of custom password validation rules.
         */
        val passwordValidationRules: List<PasswordRule>
    ) : AuthProvider()

    /**
     * Phone number authentication provider configuration.
     */
    data class PhoneAuthProvider(
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
        val enableInstantVerification: Boolean = true,

        /**
         * Enables automatic retrieval of the SMS code. Defaults to true.
         */
        val enableAutoRetrieval: Boolean = true
    ) : AuthProvider()

    /**
     * Google Sign-In provider configuration.
     */
    data class GoogleAuthProvider(
        /**
         * The list of scopes to request.
         */
        val scopes: List<String>,

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
        val requestEmail: Boolean = true
    ) : AuthProvider()

    /**
     * Facebook Login provider configuration.
     */
    data class FacebookAuthProvider(
        /**
         * The list of scopes (permissions) to request. Defaults to email and public_profile.
         */
        val scopes: List<String> = listOf("email", "public_profile"),

        /**
         * if true, enable limited login mode. Defaults to false.
         */
        val limitedLogin: Boolean = false
    ) : AuthProvider()

    /**
     * Twitter/X authentication provider configuration.
     */
    data class TwitterAuthProvider(
        /**
         * A map of custom OAuth parameters.
         */
        val customParameters: Map<String, String>
    ) : AuthProvider()

    /**
     * Github authentication provider configuration.
     */
    data class GithubAuthProvider(
        /**
         * The list of scopes to request. Defaults to user:email.
         */
        val scopes: List<String> = listOf("user:email"),

        /**
         * A map of custom OAuth parameters.
         */
        val customParameters: Map<String, String>
    ) : AuthProvider()

    /**
     * Microsoft authentication provider configuration.
     */
    data class MicrosoftAuthProvider(
        /**
         * The list of scopes to request. Defaults to openid, profile, email.
         */
        val scopes: List<String> = listOf("openid", "profile", "email"),

        /**
         * The tenant ID for Azure Active Directory.
         */
        val tenant: String?,

        /**
         * A map of custom OAuth parameters.
         */
        val customParameters: Map<String, String>
    ) : AuthProvider()

    /**
     * Yahoo authentication provider configuration.
     */
    data class YahooAuthProvider(
        /**
         * The list of scopes to request. Defaults to openid, profile, email.
         */
        val scopes: List<String> = listOf("openid", "profile", "email"),

        /**
         * A map of custom OAuth parameters.
         */
        val customParameters: Map<String, String>
    ) : AuthProvider()

    /**
     * Apple Sign-In provider configuration.
     */
    data class AppleAuthProvider(
        /**
         * The list of scopes to request. Defaults to name and email.
         */
        val scopes: List<String> = listOf("name", "email"),

        /**
         * The locale for the sign-in page.
         */
        val locale: String?,

        /**
         * A map of custom OAuth parameters.
         */
        val customParameters: Map<String, String>
    ) : AuthProvider()

    /**
     * Anonymous authentication provider. It has no configurable properties.
     */
    object AnonymousAuthProvider : AuthProvider()

    /**
     * A generic OAuth provider for any unsupported provider.
     */
    data class GenericOAuthProvider(
        /**
         * The provider ID as configured in the Firebase console.
         */
        val providerId: String,

        /**
         * The list of scopes to request.
         */
        val scopes: List<String>,

        /**
         * A map of custom OAuth parameters.
         */
        val customParameters: Map<String, String>,

        /**
         * The text to display on the provider button.
         */
        val buttonLabel: String,

        /**
         * An optional icon for the provider button.
         */
        val buttonIcon: Any?,

        /**
         * An optional background color for the provider button.
         */
        val buttonColor: Color?
    ) : AuthProvider()
}