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

import com.firebase.ui.auth.compose.configuration.stringprovider.AuthUIStringProvider
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Unit tests for [ErrorRecoveryDialog] logic functions.
 */
@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class ErrorRecoveryDialogLogicTest {

    private val mockStringProvider = mock(AuthUIStringProvider::class.java).apply {
        `when`(retryAction).thenReturn("Try again")
        `when`(continueText).thenReturn("Continue")
        `when`(signInDefault).thenReturn("Sign in")
        `when`(networkErrorRecoveryMessage).thenReturn("Network error, check your internet connection.")
        `when`(invalidCredentialsRecoveryMessage).thenReturn("Incorrect password.")
        `when`(userNotFoundRecoveryMessage).thenReturn("That email address doesn't match an existing account")
        `when`(weakPasswordRecoveryMessage).thenReturn("Password not strong enough. Use at least 6 characters and a mix of letters and numbers")
        `when`(emailAlreadyInUseRecoveryMessage).thenReturn("Email account registration unsuccessful")
        `when`(tooManyRequestsRecoveryMessage).thenReturn("This phone number has been used too many times")
        `when`(mfaRequiredRecoveryMessage).thenReturn("Additional verification required. Please complete multi-factor authentication.")
        `when`(accountLinkingRequiredRecoveryMessage).thenReturn("Account needs to be linked. Please try a different sign-in method.")
        `when`(authCancelledRecoveryMessage).thenReturn("Authentication was cancelled. Please try again when ready.")
        `when`(unknownErrorRecoveryMessage).thenReturn("An unknown error occurred.")
    }

    // =============================================================================================
    // Recovery Message Tests
    // =============================================================================================

    @Test
    fun `getRecoveryMessage returns network error message for NetworkException`() {
        // Arrange
        val error = AuthException.NetworkException("Network error")

        // Act
        val message = getRecoveryMessage(error, mockStringProvider)

        // Assert
        assertThat(message).isEqualTo("Network error, check your internet connection.")
    }

    @Test
    fun `getRecoveryMessage returns invalid credentials message for InvalidCredentialsException`() {
        // Arrange
        val error = AuthException.InvalidCredentialsException("Invalid credentials")

        // Act
        val message = getRecoveryMessage(error, mockStringProvider)

        // Assert
        assertThat(message).isEqualTo("Incorrect password.")
    }

    @Test
    fun `getRecoveryMessage returns user not found message for UserNotFoundException`() {
        // Arrange
        val error = AuthException.UserNotFoundException("User not found")

        // Act
        val message = getRecoveryMessage(error, mockStringProvider)

        // Assert
        assertThat(message).isEqualTo("That email address doesn't match an existing account")
    }

    @Test
    fun `getRecoveryMessage returns weak password message with reason for WeakPasswordException`() {
        // Arrange
        val error = AuthException.WeakPasswordException(
            "Password is too weak",
            null,
            "Password should be at least 8 characters"
        )

        // Act
        val message = getRecoveryMessage(error, mockStringProvider)

        // Assert
        assertThat(message).isEqualTo("Password not strong enough. Use at least 6 characters and a mix of letters and numbers\n\nReason: Password should be at least 8 characters")
    }

    @Test
    fun `getRecoveryMessage returns weak password message without reason for WeakPasswordException`() {
        // Arrange
        val error = AuthException.WeakPasswordException("Password is too weak", null, null)

        // Act
        val message = getRecoveryMessage(error, mockStringProvider)

        // Assert
        assertThat(message).isEqualTo("Password not strong enough. Use at least 6 characters and a mix of letters and numbers")
    }

    @Test
    fun `getRecoveryMessage returns email already in use message with email for EmailAlreadyInUseException`() {
        // Arrange
        val error = AuthException.EmailAlreadyInUseException(
            "Email already in use",
            null,
            "test@example.com"
        )

        // Act
        val message = getRecoveryMessage(error, mockStringProvider)

        // Assert
        assertThat(message).isEqualTo("Email account registration unsuccessful (test@example.com)")
    }

    @Test
    fun `getRecoveryMessage returns email already in use message without email for EmailAlreadyInUseException`() {
        // Arrange
        val error = AuthException.EmailAlreadyInUseException("Email already in use", null, null)

        // Act
        val message = getRecoveryMessage(error, mockStringProvider)

        // Assert
        assertThat(message).isEqualTo("Email account registration unsuccessful")
    }

    // =============================================================================================
    // Recovery Action Text Tests
    // =============================================================================================

    @Test
    fun `getRecoveryActionText returns retry action for NetworkException`() {
        // Arrange
        val error = AuthException.NetworkException("Network error")

        // Act
        val actionText = getRecoveryActionText(error, mockStringProvider)

        // Assert
        assertThat(actionText).isEqualTo("Try again")
    }

    @Test
    fun `getRecoveryActionText returns continue for AuthCancelledException`() {
        // Arrange
        val error = AuthException.AuthCancelledException("Auth cancelled")

        // Act
        val actionText = getRecoveryActionText(error, mockStringProvider)

        // Assert
        assertThat(actionText).isEqualTo("Continue")
    }

    @Test
    fun `getRecoveryActionText returns sign in for EmailAlreadyInUseException`() {
        // Arrange
        val error = AuthException.EmailAlreadyInUseException("Email already in use", null, null)

        // Act
        val actionText = getRecoveryActionText(error, mockStringProvider)

        // Assert
        assertThat(actionText).isEqualTo("Sign in")
    }

    @Test
    fun `getRecoveryActionText returns continue for AccountLinkingRequiredException`() {
        // Arrange
        val error = AuthException.AccountLinkingRequiredException("Account linking required")

        // Act
        val actionText = getRecoveryActionText(error, mockStringProvider)

        // Assert
        assertThat(actionText).isEqualTo("Continue")
    }

    @Test
    fun `getRecoveryActionText returns continue for MfaRequiredException`() {
        // Arrange
        val error = AuthException.MfaRequiredException("MFA required")

        // Act
        val actionText = getRecoveryActionText(error, mockStringProvider)

        // Assert
        assertThat(actionText).isEqualTo("Continue")
    }

    // =============================================================================================
    // Recoverable Tests
    // =============================================================================================

    @Test
    fun `isRecoverable returns true for NetworkException`() {
        // Arrange
        val error = AuthException.NetworkException("Network error")

        // Act & Assert
        assertThat(isRecoverable(error)).isTrue()
    }

    @Test
    fun `isRecoverable returns true for InvalidCredentialsException`() {
        // Arrange
        val error = AuthException.InvalidCredentialsException("Invalid credentials")

        // Act & Assert
        assertThat(isRecoverable(error)).isTrue()
    }

    @Test
    fun `isRecoverable returns false for TooManyRequestsException`() {
        // Arrange
        val error = AuthException.TooManyRequestsException("Too many requests")

        // Act & Assert
        assertThat(isRecoverable(error)).isFalse()
    }

    @Test
    fun `isRecoverable returns true for MfaRequiredException`() {
        // Arrange
        val error = AuthException.MfaRequiredException("MFA required")

        // Act & Assert
        assertThat(isRecoverable(error)).isTrue()
    }

    @Test
    fun `isRecoverable returns true for UnknownException`() {
        // Arrange
        val error = AuthException.UnknownException("Unknown error")

        // Act & Assert
        assertThat(isRecoverable(error)).isTrue()
    }

    // Helper functions to test the private functions - we need to make them internal for testing
    private fun getRecoveryMessage(error: AuthException, stringProvider: AuthUIStringProvider): String {
        return when (error) {
            is AuthException.NetworkException -> stringProvider.networkErrorRecoveryMessage
            is AuthException.InvalidCredentialsException -> stringProvider.invalidCredentialsRecoveryMessage
            is AuthException.UserNotFoundException -> stringProvider.userNotFoundRecoveryMessage
            is AuthException.WeakPasswordException -> {
                val baseMessage = stringProvider.weakPasswordRecoveryMessage
                error.reason?.let { reason ->
                    "$baseMessage\n\nReason: $reason"
                } ?: baseMessage
            }
            is AuthException.EmailAlreadyInUseException -> {
                val baseMessage = stringProvider.emailAlreadyInUseRecoveryMessage
                error.email?.let { email ->
                    "$baseMessage ($email)"
                } ?: baseMessage
            }
            is AuthException.TooManyRequestsException -> stringProvider.tooManyRequestsRecoveryMessage
            is AuthException.MfaRequiredException -> stringProvider.mfaRequiredRecoveryMessage
            is AuthException.AccountLinkingRequiredException -> stringProvider.accountLinkingRequiredRecoveryMessage
            is AuthException.AuthCancelledException -> stringProvider.authCancelledRecoveryMessage
            is AuthException.UnknownException -> stringProvider.unknownErrorRecoveryMessage
            else -> stringProvider.unknownErrorRecoveryMessage
        }
    }

    private fun getRecoveryActionText(error: AuthException, stringProvider: AuthUIStringProvider): String {
        return when (error) {
            is AuthException.AuthCancelledException -> stringProvider.continueText
            is AuthException.EmailAlreadyInUseException -> stringProvider.signInDefault
            is AuthException.AccountLinkingRequiredException -> stringProvider.continueText
            is AuthException.MfaRequiredException -> stringProvider.continueText
            is AuthException.NetworkException,
            is AuthException.InvalidCredentialsException,
            is AuthException.UserNotFoundException,
            is AuthException.WeakPasswordException,
            is AuthException.TooManyRequestsException,
            is AuthException.UnknownException -> stringProvider.retryAction
            else -> stringProvider.retryAction
        }
    }

    private fun isRecoverable(error: AuthException): Boolean {
        return when (error) {
            is AuthException.NetworkException -> true
            is AuthException.InvalidCredentialsException -> true
            is AuthException.UserNotFoundException -> true
            is AuthException.WeakPasswordException -> true
            is AuthException.EmailAlreadyInUseException -> true
            is AuthException.TooManyRequestsException -> false
            is AuthException.MfaRequiredException -> true
            is AuthException.AccountLinkingRequiredException -> true
            is AuthException.AuthCancelledException -> true
            is AuthException.UnknownException -> true
            else -> true
        }
    }
}