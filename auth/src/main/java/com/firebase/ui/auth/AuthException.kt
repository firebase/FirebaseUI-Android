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

package com.firebase.ui.auth

import android.content.Context
import com.firebase.ui.auth.AuthException.Companion.from
import com.firebase.ui.auth.configuration.string_provider.AuthUIStringProvider
import com.firebase.ui.auth.configuration.string_provider.DefaultAuthUIStringProvider
import com.google.firebase.FirebaseException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.FirebaseAuthActionCodeException
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthMultiFactorException
import com.google.firebase.auth.FirebaseAuthRecentLoginRequiredException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import java.util.Locale

/**
 * Abstract base class representing all possible authentication exceptions in Firebase Auth UI.
 *
 * This class provides a unified exception hierarchy for authentication operations, allowing
 * for consistent error handling across the entire Auth UI system.
 *
 * Use the companion object [from] method to create specific exception instances from
 * Firebase authentication exceptions.
 *
 * **Example usage:**
 * ```kotlin
 * try {
 *     // Perform authentication operation
 * } catch (firebaseException: Exception) {
 *     val authException = AuthException.from(firebaseException)
 *     when (authException) {
 *         is AuthException.NetworkException -> {
 *             // Handle network error
 *         }
 *         is AuthException.InvalidCredentialsException -> {
 *             // Handle invalid credentials
 *         }
 *         // ... handle other exception types
 *     }
 * }
 * ```
 *
 * @property message The detailed error message
 * @property cause The underlying [Throwable] that caused this exception
 *
 * @since 10.0.0
 */
abstract class AuthException(
    message: String,
    cause: Throwable? = null
) : Exception(message, cause) {

    /**
     * A network error occurred during the authentication operation.
     *
     * This exception is thrown when there are connectivity issues, timeouts,
     * or other network-related problems.
     *
     * @property message The detailed error message
     * @property cause The underlying [Throwable] that caused this exception
     */
    class NetworkException(
        message: String,
        cause: Throwable? = null
    ) : AuthException(message, cause)

    /**
     * The provided credentials are not valid.
     *
     * This exception is thrown when the user provides incorrect login information,
     * such as wrong email/password combinations or malformed credentials.
     *
     * @property message The detailed error message
     * @property cause The underlying [Throwable] that caused this exception
     */
    class InvalidCredentialsException(
        message: String,
        cause: Throwable? = null
    ) : AuthException(message, cause)

    /**
     * The account exists, but the sign-in method the user just attempted is not available on it.
     *
     * The attempt was well-formed and the backend answered definitively that this method cannot
     * be used for this account, so the error is not recoverable: the error dialog offers no retry
     * and the copy points the user at a different way to sign in.
     *
     * The type is general, but the copy is not. `ERROR_PASSKEY_ENROLLMENT_NOT_FOUND` is the only
     * code routed here today and the dialog's fallback for a blank message is `errorPasskeyNotFound`.
     * Routing a second code here means giving it its own string and making that fallback a
     * per-method choice.
     *
     * @property message The detailed error message
     * @property cause The underlying [Throwable] that caused this exception
     */
    class SignInMethodUnavailableException(
        message: String,
        cause: Throwable? = null
    ) : AuthException(message, cause)

    /**
     * The user account does not exist.
     *
     * This exception is thrown when attempting to sign in with credentials
     * for a user that doesn't exist in the Firebase Auth system.
     *
     * @property message The detailed error message
     * @property cause The underlying [Throwable] that caused this exception
     */
    class UserNotFoundException(
        message: String,
        cause: Throwable? = null
    ) : AuthException(message, cause)

    /**
     * The password provided is not strong enough.
     *
     * This exception is thrown when creating an account or updating a password
     * with a password that doesn't meet the security requirements.
     *
     * @property message The detailed error message
     * @property cause The underlying [Throwable] that caused this exception
     * @property reason The specific reason why the password is considered weak
     */
    class WeakPasswordException(
        message: String,
        cause: Throwable? = null,
        val reason: String? = null
    ) : AuthException(message, cause)

    /**
     * The password violates one or more Google Identity Platform password policy requirements.
     *
     * This exception is thrown when GIdP password policy enforcement is enabled and the supplied
     * password fails one or more configured constraints (e.g. minimum length, missing uppercase).
     *
     * [message] is a newline-separated, human-readable description of each failing constraint,
     * suitable for direct display in the UI. Built by [from], each constraint is translated copy
     * where the library recognises the server's sentence, and the server's own English where it
     * does not.
     *
     * [failingRequirements] keeps the **raw** server sentences, untranslated, for hosts that
     * render the constraints themselves rather than showing [message].
     *
     * @property message Translated description of the failing constraints
     * @property failingRequirements The raw, untranslated constraint strings from the server
     * @property cause The underlying [Throwable] that caused this exception
     */
    class PasswordPolicyViolationException(
        message: String,
        val failingRequirements: List<String>,
        cause: Throwable? = null
    ) : AuthException(message, cause)

    /**
     * An account with the given email already exists.
     *
     * This exception is thrown when attempting to create a new account with
     * an email address that is already registered.
     *
     * @property message The detailed error message
     * @property cause The underlying [Throwable] that caused this exception
     * @property email The email address that already exists
     */
    class EmailAlreadyInUseException(
        message: String,
        cause: Throwable? = null,
        val email: String? = null
    ) : AuthException(message, cause)

    /**
     * Too many requests have been made to the server.
     *
     * This exception is thrown when the client has made too many requests
     * in a short period and needs to wait before making additional requests.
     *
     * @property message The detailed error message
     * @property cause The underlying [Throwable] that caused this exception
     */
    class TooManyRequestsException(
        message: String,
        cause: Throwable? = null
    ) : AuthException(message, cause)

    /**
     * Phone verification is in cooldown period for the same phone number.
     *
     * This exception is thrown when attempting to verify the same phone number
     * again before the cooldown period (timeout) has expired.
     *
     * @property message The detailed error message
     * @property cooldownSeconds The number of seconds remaining in the cooldown period
     * @property cause The underlying [Throwable] that caused this exception
     */
    class PhoneVerificationCooldownException(
        message: String,
        val cooldownSeconds: Long,
        cause: Throwable? = null
    ) : AuthException(message, cause)

    /**
     * Multi-Factor Authentication is required to proceed.
     *
     * This exception is thrown when a user has MFA enabled and needs to
     * complete additional authentication steps.
     *
     * @property message The detailed error message
     * @property cause The underlying [Throwable] that caused this exception
     */
    class MfaRequiredException(
        message: String,
        cause: Throwable? = null
    ) : AuthException(message, cause)

    /**
     * Account linking is required to complete sign-in.
     *
     * This exception is thrown when a user tries to sign in with a provider
     * that needs to be linked to an existing account. For example, when a user
     * tries to sign in with Facebook but an account already exists with that
     * email using a different provider (like email/password).
     *
     * @property message The detailed error message
     * @property email The email address that already has an account (optional)
     * @property credential The credential that should be linked after signing in (optional)
     * @property cause The underlying [Throwable] that caused this exception
     */
    class AccountLinkingRequiredException(
        message: String,
        val email: String? = null,
        val credential: AuthCredential? = null,
        cause: Throwable? = null
    ) : AuthException(message, cause)

    /**
     * A different sign-in method should be used for this email address.
     *
     * This exception is used for the opt-in legacy recovery path backed by
     * `fetchSignInMethodsForEmail`, allowing the UI to guide users toward a previously
     * used provider when email enumeration protection has been disabled.
     *
     * @property email The email address being recovered
     * @property signInMethods The sign-in methods returned by Firebase Auth
     * @property suggestedSignInMethod The preferred method the UI should direct the user toward
     * @property cause The underlying authentication failure that triggered the lookup
     */
    class DifferentSignInMethodRequiredException(
        message: String,
        val email: String,
        val signInMethods: List<String>,
        val suggestedSignInMethod: String,
        cause: Throwable? = null
    ) : AuthException(message, cause)

    /**
     * Authentication was cancelled by the user.
     *
     * This exception is thrown when the user cancels an authentication flow,
     * such as dismissing a sign-in dialog or backing out of the process.
     *
     * @property message The detailed error message
     * @property cause The underlying [Throwable] that caused this exception
     */
    class AuthCancelledException(
        message: String,
        cause: Throwable? = null
    ) : AuthException(message, cause)

    /**
     * The Firebase project or the app is not set up for the operation that was attempted.
     *
     * Examples are a sign-in provider left disabled in the Firebase console, an unauthorized
     * continue-URL domain, a missing SHA-1 certificate hash, and the reCAPTCHA and tenant
     * families. None of these are anything the user can act on.
     *
     * [message] is generic translated copy, safe to render anywhere — the error dialog, an inline
     * error on a screen, or a host's own `onSignInFailure`. The raw Firebase SDK diagnostic is
     * untranslated but names the actual misconfiguration, so [from] keeps it on [cause] (the
     * original [com.google.firebase.auth.FirebaseAuthException]): it stays in the stack trace and
     * is reachable as `exception.cause?.message`.
     *
     * @property message Generic translated copy, safe to display
     * @property cause The original Firebase exception, carrying the raw diagnostic for logs
     */
    class MisconfigurationException(
        message: String,
        cause: Throwable? = null
    ) : AuthException(message, cause)

    /**
     * An unknown or unhandled error occurred.
     *
     * This exception is thrown for errors that don't match any of the specific
     * exception types or for unexpected system errors.
     *
     * @property message The detailed error message
     * @property cause The underlying [Throwable] that caused this exception
     */
    class UnknownException(
        message: String,
        cause: Throwable? = null
    ) : AuthException(message, cause)

    /**
     * The email link used for sign-in is invalid or malformed.
     *
     * This exception is thrown when the link is not a valid Firebase email link,
     * has incorrect format, or is missing required parameters.
     *
     * @property cause The underlying [Throwable] that caused this exception
     */
    class InvalidEmailLinkException(
        cause: Throwable? = null
    ) : AuthException("You are are attempting to sign in with an invalid email link", cause)

    /**
     * The email link is being used on a different device than where it was requested.
     *
     * This exception is thrown when `forceSameDevice = true` and the user opens
     * the link on a different device than the one used to request it.
     *
     * @property cause The underlying [Throwable] that caused this exception
     */
    class EmailLinkWrongDeviceException(
        cause: Throwable? = null
    ) : AuthException("You must open the email link on the same device.", cause)

    /**
     * Cross-device account linking is required to complete email link sign-in.
     *
     * This exception is thrown when the email link matches an existing account with
     * a social provider (Google/Facebook), and the user needs to sign in with that
     * provider to link accounts.
     *
     * @property providerName The name of the social provider that needs to be linked
     * @property emailLink The email link being processed
     * @property cause The underlying [Throwable] that caused this exception
     */
    class EmailLinkCrossDeviceLinkingException(
        val providerName: String? = null,
        val emailLink: String? = null,
        cause: Throwable? = null
    ) : AuthException("You must determine if you want to continue linking or " +
            "complete the sign in", cause)

    /**
     * User needs to provide their email address to complete email link sign-in.
     *
     * This exception is thrown when the email link is opened on a different device
     * and the email address cannot be determined from stored session data.
     *
     * @property emailLink The email link to be used after email is provided
     * @property cause The underlying [Throwable] that caused this exception
     */
    class EmailLinkPromptForEmailException(
        cause: Throwable? = null,
        val emailLink: String? = null,
    ) : AuthException("Please enter your email to continue signing in", cause)

    /**
     * Email link sign-in attempted with a different anonymous user than expected.
     *
     * This exception is thrown when an email link for anonymous account upgrade is
     * opened on a device with a different anonymous user session.
     *
     * @property cause The underlying [Throwable] that caused this exception
     */
    class EmailLinkDifferentAnonymousUserException(
        cause: Throwable? = null
    ) : AuthException("The session associated with this sign-in request has either " +
            "expired or was cleared", cause)

    /**
     * The email address provided does not match the email link.
     *
     * This exception is thrown when the user enters an email address that doesn't
     * match the email to which the sign-in link was sent.
     *
     * @property cause The underlying [Throwable] that caused this exception
     */
    class EmailMismatchException(
        cause: Throwable? = null
    ) : AuthException("You are are attempting to sign in a different email " +
            "than previously provided", cause)

    companion object {
        /**
         * Creates an appropriate [AuthException] instance from a Firebase authentication exception.
         *
         * This method maps known Firebase exception types to their corresponding [AuthException]
         * subtypes, providing a consistent exception hierarchy for error handling.
         *
         * **Mapping**, in dispatch order. Several of these types extend one another, so the order
         * is load-bearing rather than cosmetic:
         * - [FirebaseAuthWeakPasswordException] → [WeakPasswordException], or
         *   [PasswordPolicyViolationException] when the diagnostic carries a GIdP password-policy
         *   rejection
         * - [FirebaseAuthInvalidCredentialsException] → [InvalidCredentialsException], with the
         *   message selected by `errorCode`; the `errorCode`s in that family that are developer
         *   setup faults rather than user error (custom token, OIDC nonce, authenticator
         *   response) → [MisconfigurationException]
         * - [FirebaseAuthInvalidUserException] → [UserNotFoundException]
         * - [FirebaseAuthActionCodeException] → [InvalidCredentialsException]
         * - [FirebaseAuthUserCollisionException] → [EmailAlreadyInUseException]
         * - [FirebaseAuthMultiFactorException] → [MfaRequiredException]
         * - [FirebaseAuthRecentLoginRequiredException] → [InvalidCredentialsException]
         * - [FirebaseAuthException] with a developer-setup `errorCode` → [MisconfigurationException]
         * - [FirebaseTooManyRequestsException] → [TooManyRequestsException]
         * - [FirebaseException] → [NetworkException] (for network-related errors), or
         *   [PasswordPolicyViolationException] when the message carries a policy rejection
         * - Other exceptions → [UnknownException]
         *
         * **Example:**
         * ```kotlin
         * try {
         *     // Firebase auth operation
         * } catch (firebaseException: Exception) {
         *     val authException = AuthException.from(firebaseException)
         *     handleAuthError(authException)
         * }
         * ```
         *
         * Messages are resolved against [context]'s own configuration, so this overload honours
         * neither a custom [AuthUIStringProvider] nor the `locale` a host configured. Prefer the
         * [AuthUIStringProvider] overload wherever one is reachable, which inside an auth flow it
         * always is, as `config.stringProvider`. This overload exists for the entry points that
         * genuinely have no configuration to draw on, such as [FirebaseAuthUI.signOut],
         * [FirebaseAuthUI.withReauth] and [FirebaseAuthUI.delete].
         *
         * @param firebaseException The Firebase exception to convert
         * @param context Used to build a [DefaultAuthUIStringProvider] for the error messages
         * @return An appropriate [AuthException] subtype
         */
        @JvmStatic
        fun from(firebaseException: Exception, context: Context): AuthException =
            from(firebaseException, DefaultAuthUIStringProvider(context))

        /**
         * Creates an [AuthException] from [firebaseException], taking message text from
         * [stringProvider] so it honours the host's configured strings and locale.
         *
         * This is the preferred overload; see the [Context] one above for the exception mapping
         * table and an example.
         *
         * Given a non-null [stringProvider], the `message` on an exception returned by **this
         * method** is library-owned translated copy, so it is safe to render directly. Each branch
         * resolves in this order: the blank-able hook scoped to the exception type, then the
         * string for the specific Firebase `errorCode`, then the corresponding generic recovery
         * message, and only then the Firebase SDK's own untranslated message.
         * [MisconfigurationException] never uses the SDK message at all — the raw diagnostic lives
         * on `cause`. A `null` [stringProvider] has nothing to resolve against and falls back to
         * the SDK message everywhere except [MisconfigurationException].
         * [PasswordPolicyViolationException] is partial by design: each requirement sentence the
         * backend returns is translated when it is recognised and kept verbatim when it is not.
         *
         * The guarantee covers `from()` only. Subtypes constructed directly carry whatever
         * `message` their caller passed, and the email-link subtypes
         * ([InvalidEmailLinkException], [EmailLinkWrongDeviceException],
         * [EmailLinkCrossDeviceLinkingException], [EmailLinkPromptForEmailException],
         * [EmailLinkDifferentAnonymousUserException], [EmailMismatchException]) bake English into
         * their own constructors. `getRecoveryMessage` keeps that out of the error dialog by
         * resolving those types through [AuthUIStringProvider] instead of reading `message`.
         *
         * @param firebaseException The Firebase exception to convert
         * @param stringProvider Supplies localized message text; pass `config.stringProvider`
         * @return An appropriate [AuthException] subtype
         */
        @JvmStatic
        @JvmOverloads
        fun from(firebaseException: Exception, stringProvider: AuthUIStringProvider? = null): AuthException {
            return when (firebaseException) {
                // If already an AuthException, return it directly
                is AuthException -> firebaseException

                // Handle specific Firebase Auth exceptions first (before general FirebaseException).
                // FirebaseAuthWeakPasswordException extends FirebaseAuthInvalidCredentialsException,
                // so it must be checked before the parent type.
                is FirebaseAuthWeakPasswordException -> {
                    val sourceText = firebaseException.reason ?: firebaseException.message ?: ""
                    if (sourceText.contains("PASSWORD_DOES_NOT_MEET_REQUIREMENTS", ignoreCase = true)) {
                        passwordPolicyViolation(sourceText, firebaseException, stringProvider)
                    } else {
                        WeakPasswordException(
                            message = stringProvider?.errorWeakPasswordGeneric.nonEmpty()
                                ?: stringProvider?.weakPasswordRecoveryMessage.nonEmpty()
                                ?: firebaseException.message
                                ?: "Password is too weak",
                            cause = firebaseException,
                            reason = firebaseException.reason
                        )
                    }
                }

                is FirebaseAuthInvalidCredentialsException -> {
                    // `errorInvalidCredentials` is the blank-able hook for the whole exception
                    // type, so it stays ahead of the per-code string in every branch.
                    val typeLevel = stringProvider?.errorInvalidCredentials.nonEmpty()
                    when (firebaseException.errorCode) {
                        // Under email enumeration protection the backend merges "wrong password"
                        // and "no such account" into this one code, so the copy cannot claim the
                        // password specifically.
                        "ERROR_INVALID_CREDENTIAL" -> InvalidCredentialsException(
                            message = typeLevel
                                ?: stringProvider?.errorIncorrectEmailOrPassword.nonEmpty()
                                ?: firebaseException.message
                                ?: "That email or password isn't correct",
                            cause = firebaseException
                        )

                        "ERROR_WRONG_PASSWORD" -> InvalidCredentialsException(
                            message = typeLevel
                                ?: stringProvider?.invalidPassword.nonEmpty()
                                ?: firebaseException.message
                                ?: "Incorrect password.",
                            cause = firebaseException
                        )

                        "ERROR_INVALID_EMAIL" -> InvalidCredentialsException(
                            message = typeLevel
                                ?: stringProvider?.invalidEmailAddress.nonEmpty()
                                ?: firebaseException.message
                                ?: "That email address isn't correct",
                            cause = firebaseException
                        )

                        "ERROR_MISSING_EMAIL" -> InvalidCredentialsException(
                            message = typeLevel
                                ?: stringProvider?.missingEmailAddress.nonEmpty()
                                ?: firebaseException.message
                                ?: "Enter your email address to continue",
                            cause = firebaseException
                        )

                        "ERROR_MISSING_PASSWORD",
                        "ERROR_MISSING_VERIFICATION_CODE" -> InvalidCredentialsException(
                            message = typeLevel
                                ?: stringProvider?.requiredField.nonEmpty()
                                ?: firebaseException.message
                                ?: "You can't leave this empty.",
                            cause = firebaseException
                        )

                        "ERROR_INVALID_PHONE_NUMBER" -> InvalidCredentialsException(
                            message = typeLevel
                                ?: stringProvider?.invalidPhoneNumber.nonEmpty()
                                ?: firebaseException.message
                                ?: "Enter a valid phone number",
                            cause = firebaseException
                        )

                        "ERROR_MISSING_PHONE_NUMBER" -> InvalidCredentialsException(
                            message = typeLevel
                                ?: stringProvider?.missingPhoneNumber.nonEmpty()
                                ?: firebaseException.message
                                ?: "You can't leave this empty.",
                            cause = firebaseException
                        )

                        "ERROR_INVALID_VERIFICATION_CODE" -> InvalidCredentialsException(
                            message = typeLevel
                                ?: stringProvider?.invalidVerificationCode.nonEmpty()
                                ?: firebaseException.message
                                ?: "Wrong code. Try again.",
                            cause = firebaseException
                        )

                        "ERROR_SESSION_EXPIRED" -> InvalidCredentialsException(
                            message = typeLevel
                                ?: stringProvider?.errorSessionExpired.nonEmpty()
                                ?: firebaseException.message
                                ?: "This code is no longer valid",
                            cause = firebaseException
                        )

                        "ERROR_INVALID_VERIFICATION_ID",
                        "ERROR_MISSING_VERIFICATION_ID" -> InvalidCredentialsException(
                            message = typeLevel
                                ?: stringProvider?.errorInvalidVerificationId.nonEmpty()
                                ?: firebaseException.message
                                ?: "That verification session is no longer valid. Request a new code.",
                            cause = firebaseException
                        )

                        "ERROR_RETRY_PHONE_AUTH" -> InvalidCredentialsException(
                            message = typeLevel
                                ?: stringProvider?.errorRetryPhoneAuth.nonEmpty()
                                ?: firebaseException.message
                                ?: "Phone verification didn't complete. Try again.",
                            cause = firebaseException
                        )

                        "ERROR_USER_MISMATCH" -> InvalidCredentialsException(
                            message = typeLevel
                                ?: stringProvider?.errorUserMismatch.nonEmpty()
                                ?: firebaseException.message
                                ?: "Those credentials belong to a different account.",
                            cause = firebaseException
                        )

                        "ERROR_PHONE_NUMBER_NOT_FOUND",
                        "ERROR_MULTI_FACTOR_INFO_NOT_FOUND",
                        "ERROR_MISSING_MULTI_FACTOR_INFO" -> InvalidCredentialsException(
                            message = typeLevel
                                ?: stringProvider?.errorPhoneNumberNotEnrolled.nonEmpty()
                                ?: firebaseException.message
                                ?: "That phone number isn't set up for verification on this account.",
                            cause = firebaseException
                        )

                        "ERROR_INVALID_MULTI_FACTOR_SESSION",
                        "ERROR_MISSING_MULTI_FACTOR_SESSION" -> InvalidCredentialsException(
                            message = typeLevel
                                ?: stringProvider?.errorMultiFactorSessionExpired.nonEmpty()
                                ?: firebaseException.message
                                ?: "Your sign-in session expired. Sign in again to continue.",
                            cause = firebaseException
                        )

                        // Custom tokens are minted by the developer's own backend; the SDK's
                        // diagnostic names the setup problem and means nothing to the user, so it
                        // stays on `cause` while `message` carries renderable generic copy.
                        "ERROR_INVALID_CUSTOM_TOKEN",
                        "ERROR_CUSTOM_TOKEN_MISMATCH",
                        // Same shape: the app built the federated request wrong.
                        "ERROR_MISSING_OR_INVALID_NONCE",
                        "ERROR_INVALID_AUTHENTICATOR_RESPONSE" -> MisconfigurationException(
                            message = stringProvider?.unknownErrorRecoveryMessage.nonEmpty()
                                ?: "An unknown error occurred.",
                            cause = firebaseException
                        )

                        // Not InvalidCredentialsException, so its `typeLevel` hook is skipped too.
                        "ERROR_PASSKEY_ENROLLMENT_NOT_FOUND" -> SignInMethodUnavailableException(
                            message = stringProvider?.errorPasskeyNotFound.nonEmpty()
                                ?: firebaseException.message
                                ?: "We couldn't find a passkey for this account. " +
                                "Sign in another way.",
                            cause = firebaseException
                        )

                        // Unrecognised codes stay recoverable, but the copy must stay generic.
                        else -> InvalidCredentialsException(
                            message = typeLevel
                                ?: stringProvider?.unknownErrorRecoveryMessage.nonEmpty()
                                ?: firebaseException.message
                                ?: "Invalid credentials provided",
                            cause = firebaseException
                        )
                    }
                }

                is FirebaseAuthInvalidUserException -> {
                    when (firebaseException.errorCode) {
                        "ERROR_USER_NOT_FOUND" -> UserNotFoundException(
                            message = stringProvider?.errorUserNotFound.nonEmpty()
                                ?: stringProvider?.userNotFoundRecoveryMessage.nonEmpty()
                                ?: firebaseException.message
                                ?: "User not found",
                            cause = firebaseException
                        )

                        "ERROR_USER_DISABLED" -> InvalidCredentialsException(
                            message = stringProvider?.errorUserDisabled.nonEmpty()
                                ?: firebaseException.message
                                ?: "User account has been disabled",
                            cause = firebaseException
                        )

                        "ERROR_INVALID_USER_TOKEN",
                        "ERROR_USER_TOKEN_EXPIRED" -> InvalidCredentialsException(
                            message = stringProvider?.errorUserAccountGeneric.nonEmpty()
                                ?: stringProvider?.errorMultiFactorSessionExpired.nonEmpty()
                                ?: firebaseException.message
                                ?: "Your sign-in session expired. Sign in again to continue.",
                            cause = firebaseException
                        )

                        else -> UserNotFoundException(
                            message = stringProvider?.errorUserAccountGeneric.nonEmpty()
                                ?: stringProvider?.userNotFoundRecoveryMessage.nonEmpty()
                                ?: firebaseException.message
                                ?: "User account error",
                            cause = firebaseException
                        )
                    }
                }

                // Must precede the plain FirebaseAuthException arm, which it extends.
                is FirebaseAuthActionCodeException -> {
                    when (firebaseException.errorCode) {
                        // The type-level hook is the one scoped to the exception this produces —
                        // `errorInvalidCredentials`. Using `errorUnknownAuth` here would let a
                        // host customising the unknown-error copy silently lose this string.
                        "ERROR_EXPIRED_ACTION_CODE",
                        "ERROR_INVALID_ACTION_CODE" -> InvalidCredentialsException(
                            message = stringProvider?.errorInvalidCredentials.nonEmpty()
                                ?: stringProvider?.errorActionCodeInvalid.nonEmpty()
                                ?: firebaseException.message
                                ?: "That link is no longer valid. Request a new one.",
                            cause = firebaseException
                        )

                        else -> UnknownException(
                            message = stringProvider?.errorUnknownAuth.nonEmpty()
                                ?: stringProvider?.unknownErrorRecoveryMessage.nonEmpty()
                                ?: firebaseException.message
                                ?: "An unknown authentication error occurred",
                            cause = firebaseException
                        )
                    }
                }

                is FirebaseAuthUserCollisionException -> {
                    when (firebaseException.errorCode) {
                        "ERROR_EMAIL_ALREADY_IN_USE" -> EmailAlreadyInUseException(
                            message = stringProvider?.errorEmailAlreadyInUse.nonEmpty()
                                ?: stringProvider?.emailAlreadyInUseRecoveryMessage.nonEmpty()
                                ?: firebaseException.message
                                ?: "Email address is already in use",
                            cause = firebaseException,
                            email = firebaseException.email
                        )

                        "ERROR_ACCOUNT_EXISTS_WITH_DIFFERENT_CREDENTIAL" -> AccountLinkingRequiredException(
                            message = stringProvider?.errorAccountExistsDifferentCredential.nonEmpty()
                                ?: stringProvider?.accountLinkingRequiredRecoveryMessage.nonEmpty()
                                ?: firebaseException.message
                                ?: "Account already exists with different credentials",
                            cause = firebaseException
                        )

                        "ERROR_CREDENTIAL_ALREADY_IN_USE" -> AccountLinkingRequiredException(
                            message = stringProvider?.errorCredentialAlreadyInUse.nonEmpty()
                                ?: stringProvider?.accountLinkingRequiredRecoveryMessage.nonEmpty()
                                ?: firebaseException.message
                                ?: "Credential is already associated with a different user account",
                            cause = firebaseException
                        )

                        else -> AccountLinkingRequiredException(
                            message = stringProvider?.errorAccountCollisionGeneric.nonEmpty()
                                ?: stringProvider?.accountLinkingRequiredRecoveryMessage.nonEmpty()
                                ?: firebaseException.message
                                ?: "Account collision error",
                            cause = firebaseException
                        )
                    }
                }

                is FirebaseAuthMultiFactorException -> {
                    MfaRequiredException(
                        message = stringProvider?.errorMfaRequiredFallback.nonEmpty()
                            ?: stringProvider?.mfaRequiredRecoveryMessage.nonEmpty()
                            ?: firebaseException.message
                            ?: "Multi-factor authentication required",
                        cause = firebaseException
                    )
                }

                is FirebaseAuthRecentLoginRequiredException -> {
                    // `errorRecentLoginRequired` ships blank; the MFA string says the same thing.
                    InvalidCredentialsException(
                        message = stringProvider?.errorRecentLoginRequired.nonEmpty()
                            ?: stringProvider?.mfaErrorRecentLoginRequired.nonEmpty()
                            ?: firebaseException.message
                            ?: "Recent login required for this operation",
                        cause = firebaseException
                    )
                }

                is FirebaseAuthException -> {
                    when (firebaseException.errorCode) {
                        // FirebaseAuthWebException code for backing out of the OAuth custom tab,
                        // and the Credential Manager / Play services equivalent.
                        "ERROR_WEB_CONTEXT_CANCELED",
                        "ERROR_USER_CANCELLED" -> AuthCancelledException(
                            message = stringProvider?.errorAuthCancelled.nonEmpty()
                                ?: stringProvider?.authCancelledRecoveryMessage.nonEmpty()
                                ?: firebaseException.message
                                ?: "Authentication was cancelled",
                            cause = firebaseException
                        )

                        // These three produce InvalidCredentialsException, so the type-level hook
                        // is `errorInvalidCredentials`. `errorUnknownAuth` would let a host that
                        // customises only the unknown-error copy lose all three specific strings.
                        "ERROR_UNVERIFIED_EMAIL" -> InvalidCredentialsException(
                            message = stringProvider?.errorInvalidCredentials.nonEmpty()
                                ?: stringProvider?.errorUnverifiedEmail.nonEmpty()
                                ?: firebaseException.message
                                ?: "Verify your email address before you continue.",
                            cause = firebaseException
                        )

                        "ERROR_SECOND_FACTOR_ALREADY_ENROLLED" -> InvalidCredentialsException(
                            message = stringProvider?.errorInvalidCredentials.nonEmpty()
                                ?: stringProvider?.errorSecondFactorAlreadyEnrolled.nonEmpty()
                                ?: firebaseException.message
                                ?: "That verification method is already set up on this account.",
                            cause = firebaseException
                        )

                        "ERROR_MAXIMUM_SECOND_FACTOR_COUNT_EXCEEDED" -> InvalidCredentialsException(
                            message = stringProvider?.errorInvalidCredentials.nonEmpty()
                                ?: stringProvider?.errorMaximumSecondFactorCountExceeded.nonEmpty()
                                ?: firebaseException.message
                                ?: "You've reached the limit for verification methods on this account.",
                            cause = firebaseException
                        )

                        // Developer setup problems. The user can do nothing about any of them, so
                        // `message` carries generic translated copy and the raw Firebase
                        // diagnostic is kept on `cause` for logs. INTERNAL_ERROR and
                        // ERROR_WEB_INTERNAL_ERROR are deliberately absent — they are backend
                        // faults, not configuration.
                        "ERROR_OPERATION_NOT_ALLOWED",
                        "ERROR_APP_NOT_AUTHORIZED",
                        "ERROR_UNAUTHORIZED_DOMAIN",
                        "ERROR_MISSING_CONTINUE_URI",
                        "ERROR_INVALID_CERT_HASH",
                        "ERROR_DYNAMIC_LINK_NOT_ACTIVATED",
                        "ERROR_INVALID_DYNAMIC_LINK_DOMAIN",
                        "ERROR_INVALID_HOSTING_LINK_DOMAIN",
                        "ERROR_INVALID_PROVIDER_ID",
                        "ERROR_ADMIN_RESTRICTED_OPERATION",
                        "ERROR_UNSUPPORTED_FIRST_FACTOR",
                        "ERROR_UNSUPPORTED_PASSTHROUGH_OPERATION",
                        "ERROR_INVALID_REQ_TYPE",
                        "ERROR_WEB_CONTEXT_ALREADY_PRESENTED",
                        // Tenant family
                        "ERROR_INVALID_TENANT_ID",
                        "ERROR_TENANT_ID_MISMATCH",
                        "ERROR_UNSUPPORTED_TENANT_OPERATION",
                        // reCAPTCHA / app verification family
                        "ERROR_RECAPTCHA_NOT_ENABLED",
                        "ERROR_CAPTCHA_CHECK_FAILED",
                        "ERROR_MISSING_RECAPTCHA_TOKEN",
                        "ERROR_INVALID_RECAPTCHA_TOKEN",
                        "ERROR_INVALID_RECAPTCHA_ACTION",
                        "ERROR_MISSING_RECAPTCHA_VERSION",
                        "ERROR_INVALID_RECAPTCHA_VERSION",
                        "ERROR_MISSING_CLIENT_TYPE",
                        "ERROR_MISSING_CLIENT_IDENTIFIER",
                        "ERROR_ALTERNATE_CLIENT_IDENTIFIER_REQUIRED",
                        // Email-template settings in the Firebase console. These arrive as
                        // FirebaseAuthEmailException, which extends FirebaseAuthException
                        // directly and so lands in this arm.
                        "ERROR_INVALID_MESSAGE_PAYLOAD",
                        "ERROR_INVALID_SENDER",
                        "ERROR_INVALID_RECIPIENT_EMAIL",
                        // Host integration and project quota. ERROR_MISSING_ACTIVITY ships: it is
                        // declared on the Recaptcha-activity exception, not in the SDK code table.
                        "ERROR_MISSING_ACTIVITY",
                        "ERROR_WEB_STORAGE_UNSUPPORTED",
                        "ERROR_QUOTA_EXCEEDED" -> MisconfigurationException(
                            message = stringProvider?.unknownErrorRecoveryMessage.nonEmpty()
                                ?: "An unknown error occurred.",
                            cause = firebaseException
                        )

                        else -> UnknownException(
                            message = stringProvider?.errorUnknownAuth.nonEmpty()
                                ?: stringProvider?.unknownErrorRecoveryMessage.nonEmpty()
                                ?: firebaseException.message
                                ?: "An unknown authentication error occurred",
                            cause = firebaseException
                        )
                    }
                }

                // Rate limiting arrives as a plain FirebaseTooManyRequestsException, which is NOT a
                // FirebaseAuthException and carries no error code. Without this arm it falls to the
                // FirebaseException branch below and a throttled user is told they are offline.
                is FirebaseTooManyRequestsException -> {
                    TooManyRequestsException(
                        message = stringProvider?.errorTooManyRequests.nonEmpty()
                            ?: stringProvider?.tooManyRequestsRecoveryMessage.nonEmpty()
                            ?: firebaseException.message
                            ?: "Too many requests. Please try again later",
                        cause = firebaseException
                    )
                }

                is FirebaseException -> {
                    val msg = firebaseException.message ?: ""
                    if (msg.contains("PASSWORD_DOES_NOT_MEET_REQUIREMENTS", ignoreCase = true)) {
                        passwordPolicyViolation(msg, firebaseException, stringProvider)
                    } else {
                        NetworkException(
                            message = stringProvider?.errorNetworkGeneric.nonEmpty()
                                ?: stringProvider?.networkErrorRecoveryMessage.nonEmpty()
                                ?: msg.ifEmpty { "Network error occurred" },
                            cause = firebaseException
                        )
                    }
                }

                else -> {
                    if (firebaseException.message?.contains("cancelled", ignoreCase = true) == true ||
                        firebaseException.message?.contains("canceled", ignoreCase = true) == true
                    ) {
                        AuthCancelledException(
                            message = stringProvider?.errorAuthCancelled.nonEmpty()
                                ?: stringProvider?.authCancelledRecoveryMessage.nonEmpty()
                                ?: firebaseException.message
                                ?: "Authentication was cancelled",
                            cause = firebaseException
                        )
                    } else {
                        UnknownException(
                            message = stringProvider?.errorUnknownAuth.nonEmpty()
                                ?: stringProvider?.unknownErrorRecoveryMessage.nonEmpty()
                                ?: firebaseException.message
                                ?: "An unknown error occurred",
                            cause = firebaseException
                        )
                    }
                }
            }
        }

        private fun String?.nonEmpty(): String? = this?.ifEmpty { null }

        /**
         * Builds the [PasswordPolicyViolationException] for a GIdP password-policy rejection:
         * `message` is localized one requirement sentence at a time, while `failingRequirements`
         * keeps the backend's raw sentences.
         */
        private fun passwordPolicyViolation(
            sourceText: String,
            cause: Exception,
            stringProvider: AuthUIStringProvider?
        ): PasswordPolicyViolationException {
            val requirements = parsePasswordPolicyRequirements(sourceText)
            return PasswordPolicyViolationException(
                message = requirements
                    .joinToString("\n") { localizePasswordRequirement(it, stringProvider) ?: it }
                    .ifEmpty {
                        // `errorWeakPasswordGeneric` is the host's hook and ships blank.
                        stringProvider?.errorWeakPasswordGeneric.nonEmpty()
                            ?: stringProvider?.errorPasswordPolicyGeneric.nonEmpty()
                            ?: "Password does not meet policy requirements"
                    },
                failingRequirements = requirements,
                cause = cause
            )
        }

        /**
         * Translates one GIdP password-policy requirement sentence, or returns `null` when the
         * sentence is not recognised.
         *
         * The sentences are the backend's own English, e.g. "Password must contain at least 10
         * characters". On `null` the caller keeps that sentence verbatim, so a reworded or newly
         * added requirement degrades to untranslated English rather than to a wrong message.
         */
        private fun localizePasswordRequirement(
            requirement: String,
            stringProvider: AuthUIStringProvider?
        ): String? {
            if (stringProvider == null) return null
            // GIdP writes "upper case" and "lower case" as two words; one word is accepted too.
            val text = requirement.lowercase(Locale.ROOT)
            return when {
                text.contains("upper case") || text.contains("uppercase") ->
                    stringProvider.passwordMissingUppercase.nonEmpty()

                text.contains("lower case") || text.contains("lowercase") ->
                    stringProvider.passwordMissingLowercase.nonEmpty()

                // "numeric" is a substring of "non-alphanumeric": the guard keeps the two arms
                // disjoint regardless of the order they are tested in.
                text.contains("numeric") && !text.contains("non-alphanumeric") ->
                    stringProvider.passwordMissingDigit.nonEmpty()

                // Unverified wording: the probe project had special characters disabled.
                text.contains("non-alphanumeric") || text.contains("special character") ->
                    stringProvider.passwordMissingSpecialCharacter.nonEmpty()

                // The number is the project's own configured minimum, so it is read out of the
                // sentence rather than assumed.
                text.contains("at least") ->
                    firstNumberIn(requirement)?.let {
                        stringProvider.passwordTooShort(it).nonEmpty()
                    }

                // "fewer than N" is exclusive, so the maximum passwordTooLong states is N - 1.
                // Unverified wording: the probe project left the maximum at its 4096 default.
                text.contains("fewer than") ->
                    firstNumberIn(requirement)?.let {
                        stringProvider.passwordTooLong(it - 1).nonEmpty()
                    }

                // "at most N" and "no more than N" are inclusive, so N is the maximum as written.
                text.contains("at most") || text.contains("no more than") ->
                    firstNumberIn(requirement)?.let {
                        stringProvider.passwordTooLong(it).nonEmpty()
                    }

                else -> null
            }
        }

        private fun firstNumberIn(text: String): Int? =
            Regex("\\d+").find(text)?.value?.toIntOrNull()

        // Finds the [...] content that immediately follows PASSWORD_DOES_NOT_MEET_REQUIREMENTS
        // in both FirebaseException and FirebaseAuthWeakPasswordException messages.
        // GIdP returns human-readable requirement strings inside those brackets, e.g.
        // "...PASSWORD_DOES_NOT_MEET_REQUIREMENTS:Missing password requirements: [Password must contain at least 10 characters]"
        private fun parsePasswordPolicyRequirements(message: String): List<String> {
            val policyIndex = message.indexOf("PASSWORD_DOES_NOT_MEET_REQUIREMENTS", ignoreCase = true)
            if (policyIndex == -1) return emptyList()
            val start = message.indexOf('[', policyIndex)
            val end = message.indexOf(']', policyIndex)
            if (start == -1 || end == -1 || end <= start) return emptyList()
            return message.substring(start + 1, end)
                .split(',')
                .map { it.trim() }
                .filter { it.isNotEmpty() }
        }
    }
}
