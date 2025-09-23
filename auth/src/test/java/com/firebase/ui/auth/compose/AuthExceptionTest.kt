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

import com.google.common.truth.Truth.assertThat
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuthException
import org.junit.Test
import org.junit.runner.RunWith
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
}