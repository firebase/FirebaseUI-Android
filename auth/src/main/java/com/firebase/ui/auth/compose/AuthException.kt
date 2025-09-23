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

package com.firebase.ui.auth.compose

import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthMultiFactorException
import com.google.firebase.auth.FirebaseAuthRecentLoginRequiredException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException

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
     * that needs to be linked to an existing account.
     *
     * @property message The detailed error message
     * @property cause The underlying [Throwable] that caused this exception
     */
    class AccountLinkingRequiredException(
        message: String,
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

    companion object {
        /**
         * Creates an appropriate [AuthException] instance from a Firebase authentication exception.
         *
         * This method maps known Firebase exception types to their corresponding [AuthException]
         * subtypes, providing a consistent exception hierarchy for error handling.
         *
         * **Mapping:**
         * - [FirebaseException] → [NetworkException] (for network-related errors)
         * - [FirebaseAuthInvalidCredentialsException] → [InvalidCredentialsException]
         * - [FirebaseAuthInvalidUserException] → [UserNotFoundException]
         * - [FirebaseAuthWeakPasswordException] → [WeakPasswordException]
         * - [FirebaseAuthUserCollisionException] → [EmailAlreadyInUseException]
         * - [FirebaseAuthException] with ERROR_TOO_MANY_REQUESTS → [TooManyRequestsException]
         * - [FirebaseAuthMultiFactorException] → [MfaRequiredException]
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
         * @param firebaseException The Firebase exception to convert
         * @return An appropriate [AuthException] subtype
         */
        @JvmStatic
        fun from(firebaseException: Exception): AuthException {
            return when (firebaseException) {
                // Handle specific Firebase Auth exceptions first (before general FirebaseException)
                is FirebaseAuthInvalidCredentialsException -> {
                    InvalidCredentialsException(
                        message = firebaseException.message ?: "Invalid credentials provided",
                        cause = firebaseException
                    )
                }
                is FirebaseAuthInvalidUserException -> {
                    when (firebaseException.errorCode) {
                        "ERROR_USER_NOT_FOUND" -> UserNotFoundException(
                            message = firebaseException.message ?: "User not found",
                            cause = firebaseException
                        )
                        "ERROR_USER_DISABLED" -> InvalidCredentialsException(
                            message = firebaseException.message ?: "User account has been disabled",
                            cause = firebaseException
                        )
                        else -> UserNotFoundException(
                            message = firebaseException.message ?: "User account error",
                            cause = firebaseException
                        )
                    }
                }
                is FirebaseAuthWeakPasswordException -> {
                    WeakPasswordException(
                        message = firebaseException.message ?: "Password is too weak",
                        cause = firebaseException,
                        reason = firebaseException.reason
                    )
                }
                is FirebaseAuthUserCollisionException -> {
                    when (firebaseException.errorCode) {
                        "ERROR_EMAIL_ALREADY_IN_USE" -> EmailAlreadyInUseException(
                            message = firebaseException.message ?: "Email address is already in use",
                            cause = firebaseException,
                            email = firebaseException.email
                        )
                        "ERROR_ACCOUNT_EXISTS_WITH_DIFFERENT_CREDENTIAL" -> AccountLinkingRequiredException(
                            message = firebaseException.message ?: "Account already exists with different credentials",
                            cause = firebaseException
                        )
                        "ERROR_CREDENTIAL_ALREADY_IN_USE" -> AccountLinkingRequiredException(
                            message = firebaseException.message ?: "Credential is already associated with a different user account",
                            cause = firebaseException
                        )
                        else -> AccountLinkingRequiredException(
                            message = firebaseException.message ?: "Account collision error",
                            cause = firebaseException
                        )
                    }
                }
                is FirebaseAuthMultiFactorException -> {
                    MfaRequiredException(
                        message = firebaseException.message ?: "Multi-factor authentication required",
                        cause = firebaseException
                    )
                }
                is FirebaseAuthRecentLoginRequiredException -> {
                    InvalidCredentialsException(
                        message = firebaseException.message ?: "Recent login required for this operation",
                        cause = firebaseException
                    )
                }
                is FirebaseAuthException -> {
                    // Handle FirebaseAuthException and check for specific error codes
                    when (firebaseException.errorCode) {
                        "ERROR_TOO_MANY_REQUESTS" -> TooManyRequestsException(
                            message = firebaseException.message ?: "Too many requests. Please try again later",
                            cause = firebaseException
                        )
                        else -> UnknownException(
                            message = firebaseException.message ?: "An unknown authentication error occurred",
                            cause = firebaseException
                        )
                    }
                }
                is FirebaseException -> {
                    // Handle general Firebase exceptions, which include network errors
                    NetworkException(
                        message = firebaseException.message ?: "Network error occurred",
                        cause = firebaseException
                    )
                }
                else -> {
                    // Check for common cancellation patterns
                    if (firebaseException.message?.contains("cancelled", ignoreCase = true) == true ||
                        firebaseException.message?.contains("canceled", ignoreCase = true) == true) {
                        AuthCancelledException(
                            message = firebaseException.message ?: "Authentication was cancelled",
                            cause = firebaseException
                        )
                    } else {
                        UnknownException(
                            message = firebaseException.message ?: "An unknown error occurred",
                            cause = firebaseException
                        )
                    }
                }
            }
        }
    }
}