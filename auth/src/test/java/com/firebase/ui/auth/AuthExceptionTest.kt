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

import com.firebase.ui.auth.configuration.string_provider.AuthUIStringProvider
import com.google.common.truth.Truth.assertThat
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mock
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Unit tests for [AuthException] covering exception mapping from Firebase exceptions
 * to the unified AuthException hierarchy.
 *
 * @suppress Internal test class
 */
@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class AuthExceptionTest {

    @Test
    fun `from() maps FirebaseException to NetworkException`() {
        // Arrange
        val firebaseException = object : FirebaseException("Network error occurred") {}

        // Act
        val authException = AuthException.from(firebaseException)

        // Assert
        assertThat(authException).isInstanceOf(AuthException.NetworkException::class.java)
        assertThat(authException.message).isEqualTo("Network error occurred")
        assertThat(authException.cause).isEqualTo(firebaseException)
    }

    @Test
    fun `from() maps FirebaseAuthException with ERROR_TOO_MANY_REQUESTS to TooManyRequestsException`() {
        // Arrange
        val firebaseException = object : FirebaseAuthException("ERROR_TOO_MANY_REQUESTS", "Too many requests") {}

        // Act
        val authException = AuthException.from(firebaseException)

        // Assert
        assertThat(authException).isInstanceOf(AuthException.TooManyRequestsException::class.java)
        assertThat(authException.message).isEqualTo("Too many requests")
        assertThat(authException.cause).isEqualTo(firebaseException)
    }

    @Test
    fun `from() maps FirebaseAuthException with unknown error code to UnknownException`() {
        // Arrange
        val firebaseException = object : FirebaseAuthException("ERROR_UNKNOWN", "Unknown auth error") {}

        // Act
        val authException = AuthException.from(firebaseException)

        // Assert
        assertThat(authException).isInstanceOf(AuthException.UnknownException::class.java)
        assertThat(authException.message).isEqualTo("Unknown auth error")
        assertThat(authException.cause).isEqualTo(firebaseException)
    }

    @Test
    fun `from() maps exception with cancelled message to AuthCancelledException`() {
        // Arrange
        val firebaseException = RuntimeException("Operation was cancelled by user")

        // Act
        val authException = AuthException.from(firebaseException)

        // Assert
        assertThat(authException).isInstanceOf(AuthException.AuthCancelledException::class.java)
        assertThat(authException.message).isEqualTo("Operation was cancelled by user")
        assertThat(authException.cause).isEqualTo(firebaseException)
    }

    @Test
    fun `from() maps unknown exception to UnknownException`() {
        // Arrange
        val firebaseException = RuntimeException("Unknown error occurred")

        // Act
        val authException = AuthException.from(firebaseException)

        // Assert
        assertThat(authException).isInstanceOf(AuthException.UnknownException::class.java)
        assertThat(authException.message).isEqualTo("Unknown error occurred")
        assertThat(authException.cause).isEqualTo(firebaseException)
    }

    @Test
    fun `all AuthException subclasses extend AuthException`() {
        // Arrange & Assert
        assertThat(AuthException.NetworkException("Test")).isInstanceOf(AuthException::class.java)
        assertThat(AuthException.InvalidCredentialsException("Test")).isInstanceOf(AuthException::class.java)
        assertThat(AuthException.UserNotFoundException("Test")).isInstanceOf(AuthException::class.java)
        assertThat(AuthException.WeakPasswordException("Test")).isInstanceOf(AuthException::class.java)
        assertThat(AuthException.PasswordPolicyViolationException("Test", emptyList())).isInstanceOf(AuthException::class.java)
        assertThat(AuthException.EmailAlreadyInUseException("Test")).isInstanceOf(AuthException::class.java)
        assertThat(AuthException.TooManyRequestsException("Test")).isInstanceOf(AuthException::class.java)
        assertThat(AuthException.MfaRequiredException("Test")).isInstanceOf(AuthException::class.java)
        assertThat(AuthException.AccountLinkingRequiredException("Test")).isInstanceOf(AuthException::class.java)
        assertThat(AuthException.AuthCancelledException("Test")).isInstanceOf(AuthException::class.java)
        assertThat(AuthException.UnknownException("Test")).isInstanceOf(AuthException::class.java)
    }

    @Test
    fun `WeakPasswordException stores reason property correctly`() {
        // Arrange
        val reason = "Password must contain at least one number"
        val exception = AuthException.WeakPasswordException("Weak password", null, reason)

        // Assert
        assertThat(exception.reason).isEqualTo(reason)
    }

    @Test
    fun `EmailAlreadyInUseException stores email property correctly`() {
        // Arrange
        val email = "test@example.com"
        val exception = AuthException.EmailAlreadyInUseException("Email in use", null, email)

        // Assert
        assertThat(exception.email).isEqualTo(email)
    }

    // =============================================================================================
    // AuthUIStringProvider message customisation
    // =============================================================================================

    @Test
    fun `from() uses string provider message when non-empty`() {
        val firebaseException = mock(FirebaseAuthInvalidUserException::class.java)
        whenever(firebaseException.errorCode).thenReturn("ERROR_USER_DISABLED")
        whenever(firebaseException.message).thenReturn("Firebase: user disabled")

        val stringProvider = mock(AuthUIStringProvider::class.java)
        whenever(stringProvider.errorUserDisabled).thenReturn("Custom: account disabled")

        val result = AuthException.from(firebaseException, stringProvider)

        assertThat(result.message).isEqualTo("Custom: account disabled")
    }

    @Test
    fun `from() falls back to Firebase message when string provider returns empty`() {
        val firebaseException = mock(FirebaseAuthInvalidUserException::class.java)
        whenever(firebaseException.errorCode).thenReturn("ERROR_USER_DISABLED")
        whenever(firebaseException.message).thenReturn("Firebase: user disabled")

        val stringProvider = mock(AuthUIStringProvider::class.java)
        whenever(stringProvider.errorUserDisabled).thenReturn("")

        val result = AuthException.from(firebaseException, stringProvider)

        assertThat(result.message).isEqualTo("Firebase: user disabled")
    }

    @Test
    fun `from() falls back to Firebase message when no string provider given`() {
        val firebaseException = mock(FirebaseAuthInvalidUserException::class.java)
        whenever(firebaseException.errorCode).thenReturn("ERROR_USER_DISABLED")
        whenever(firebaseException.message).thenReturn("Firebase: user disabled")

        val result = AuthException.from(firebaseException)

        assertThat(result.message).isEqualTo("Firebase: user disabled")
    }

    // =============================================================================================
    // GIdP password policy
    // =============================================================================================

    @Test
    fun `from() maps GIdP policy violation FirebaseException to PasswordPolicyViolationException`() {
        val msg = "An internal error has occurred. [ PASSWORD_DOES_NOT_MEET_REQUIREMENTS:" +
                "Missing password requirements: [Password must contain at least 10 characters] ]"
        val firebaseException = object : com.google.firebase.FirebaseException(msg) {}

        val result = AuthException.from(firebaseException)

        assertThat(result).isInstanceOf(AuthException.PasswordPolicyViolationException::class.java)
        val policyEx = result as AuthException.PasswordPolicyViolationException
        assertThat(policyEx.failingRequirements).containsExactly(
            "Password must contain at least 10 characters"
        )
        assertThat(policyEx.message).isEqualTo("Password must contain at least 10 characters")
        assertThat(policyEx.cause).isEqualTo(firebaseException)
    }

    @Test
    fun `from() maps GIdP policy violation with multiple requirements`() {
        val msg = "An internal error has occurred. [ PASSWORD_DOES_NOT_MEET_REQUIREMENTS:" +
                "Missing password requirements: [Password must contain at least 10 characters, " +
                "Password must contain at least one uppercase letter] ]"
        val firebaseException = object : com.google.firebase.FirebaseException(msg) {}

        val result = AuthException.from(firebaseException)

        assertThat(result).isInstanceOf(AuthException.PasswordPolicyViolationException::class.java)
        val policyEx = result as AuthException.PasswordPolicyViolationException
        assertThat(policyEx.failingRequirements).containsExactly(
            "Password must contain at least 10 characters",
            "Password must contain at least one uppercase letter"
        ).inOrder()
        assertThat(policyEx.message).isEqualTo(
            "Password must contain at least 10 characters\nPassword must contain at least one uppercase letter"
        )
    }

    @Test
    fun `from() maps GIdP policy violation in FirebaseAuthWeakPasswordException reason`() {
        val firebaseException = FirebaseAuthWeakPasswordException(
            "ERROR_WEAK_PASSWORD",
            "weak",
            "PASSWORD_DOES_NOT_MEET_REQUIREMENTS : [Password must contain uppercase, Password must contain a number]"
        )

        val result = AuthException.from(firebaseException)

        assertThat(result).isInstanceOf(AuthException.PasswordPolicyViolationException::class.java)
        val policyEx = result as AuthException.PasswordPolicyViolationException
        assertThat(policyEx.failingRequirements).containsExactly(
            "Password must contain uppercase",
            "Password must contain a number"
        ).inOrder()
        assertThat(policyEx.message).isEqualTo("Password must contain uppercase\nPassword must contain a number")
    }

    @Test
    fun `from() passes through unknown requirement strings as-is`() {
        val msg = "An internal error has occurred. [ PASSWORD_DOES_NOT_MEET_REQUIREMENTS:" +
                "Missing password requirements: [Some future requirement] ]"
        val firebaseException = object : com.google.firebase.FirebaseException(msg) {}

        val result = AuthException.from(firebaseException)

        assertThat(result).isInstanceOf(AuthException.PasswordPolicyViolationException::class.java)
        val policyEx = result as AuthException.PasswordPolicyViolationException
        assertThat(policyEx.failingRequirements).containsExactly("Some future requirement")
        assertThat(policyEx.message).isEqualTo("Some future requirement")
    }

    @Test
    fun `from() maps plain weak password (no policy) to WeakPasswordException`() {
        val firebaseException = FirebaseAuthWeakPasswordException(
            "ERROR_WEAK_PASSWORD",
            "The given password is invalid.",
            "Password should be at least 6 characters"
        )

        val result = AuthException.from(firebaseException)

        assertThat(result).isInstanceOf(AuthException.WeakPasswordException::class.java)
    }

    @Test
    fun `from() maps plain FirebaseException without policy to NetworkException`() {
        val firebaseException = object : com.google.firebase.FirebaseException("Network timeout") {}

        val result = AuthException.from(firebaseException)

        assertThat(result).isInstanceOf(AuthException.NetworkException::class.java)
    }

    @Test
    fun `PasswordPolicyViolationException stores failingRequirements correctly`() {
        val requirements = listOf("MISSING_UPPERCASE_CHARACTER", "MISSING_NUMERIC_CHARACTER")
        val exception = AuthException.PasswordPolicyViolationException("msg", requirements)

        assertThat(exception.failingRequirements).isEqualTo(requirements)
    }
}