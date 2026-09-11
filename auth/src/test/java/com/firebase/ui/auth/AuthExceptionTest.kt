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
import com.google.common.truth.Truth.assertWithMessage
import com.google.firebase.FirebaseException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.FirebaseAuthActionCodeException
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
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
    fun `from() maps FirebaseTooManyRequestsException to TooManyRequestsException`() {
        // Arrange — rate limiting arrives as this type, not as a FirebaseAuthException. It carries
        // no error code, so the only thing that can select the arm is the exception class itself.
        val firebaseException = FirebaseTooManyRequestsException(
            "We have blocked all requests from this device due to unusual activity. Try again later."
        )

        // Act
        val authException = AuthException.from(firebaseException)

        // Assert — without a dedicated arm this falls through to `is FirebaseException` and a
        // throttled user is told they have no internet connection.
        assertThat(authException).isInstanceOf(AuthException.TooManyRequestsException::class.java)
        assertThat(authException.cause).isEqualTo(firebaseException)
    }

    @Test
    fun `from() takes the too-many-requests message from the string provider`() {
        val firebaseException = FirebaseTooManyRequestsException("Blocked due to unusual activity.")
        val stringProvider = mock(AuthUIStringProvider::class.java)
        whenever(stringProvider.errorTooManyRequests).thenReturn("Zu viele Versuche")

        val result = AuthException.from(firebaseException, stringProvider)

        assertThat(result).isInstanceOf(AuthException.TooManyRequestsException::class.java)
        assertThat(result.message).isEqualTo("Zu viele Versuche")
    }

    @Test
    fun `from() no longer treats ERROR_TOO_MANY_REQUESTS as a real Firebase Auth code`() {
        // firebase-auth 24.2.0 has no such code: every status that means "rate limited" (17010,
        // 17052) is turned into a FirebaseTooManyRequestsException instead. Anything still
        // carrying this string is not from the SDK, so it is just an unknown code.
        val firebaseException = object : FirebaseAuthException("ERROR_TOO_MANY_REQUESTS", "x") {}

        val result = AuthException.from(firebaseException)

        assertThat(result).isInstanceOf(AuthException.UnknownException::class.java)
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

    // =============================================================================================
    // Per-error-code message selection
    //
    // Every code below is one the resolved firebase-auth 24.2.0 maps onto the exception type the
    // arm matches, so each branch is reachable. The provider member is stubbed to a sentinel that
    // exists nowhere else: collapsing two codes onto one branch, or dropping a branch back to the
    // arm's generic `else`, changes the message and fails the test.
    // =============================================================================================

    /** The raw text the SDK would put on the exception, which must lose to the provider string. */
    private val sdkText = "The Firebase SDK's own untranslated English."

    private fun invalidCredentials(errorCode: String) =
        FirebaseAuthInvalidCredentialsException(errorCode, sdkText)

    @Test
    fun `from() routes each invalid-credentials error code to its own string`() {
        val stringProvider = mock(AuthUIStringProvider::class.java)
        whenever(stringProvider.errorIncorrectEmailOrPassword).thenReturn("s:incorrectEmailOrPassword")
        whenever(stringProvider.invalidPassword).thenReturn("s:invalidPassword")
        whenever(stringProvider.invalidEmailAddress).thenReturn("s:invalidEmailAddress")
        whenever(stringProvider.missingEmailAddress).thenReturn("s:missingEmailAddress")
        whenever(stringProvider.requiredField).thenReturn("s:requiredField")
        whenever(stringProvider.invalidPhoneNumber).thenReturn("s:invalidPhoneNumber")
        whenever(stringProvider.missingPhoneNumber).thenReturn("s:missingPhoneNumber")
        whenever(stringProvider.invalidVerificationCode).thenReturn("s:invalidVerificationCode")
        whenever(stringProvider.errorSessionExpired).thenReturn("s:sessionExpired")
        whenever(stringProvider.errorInvalidVerificationId).thenReturn("s:invalidVerificationId")
        whenever(stringProvider.errorRetryPhoneAuth).thenReturn("s:retryPhoneAuth")
        whenever(stringProvider.errorUserMismatch).thenReturn("s:userMismatch")
        whenever(stringProvider.errorPhoneNumberNotEnrolled).thenReturn("s:phoneNumberNotEnrolled")
        whenever(stringProvider.errorMultiFactorSessionExpired).thenReturn("s:multiFactorSessionExpired")

        val expected = mapOf(
            "ERROR_INVALID_CREDENTIAL" to "s:incorrectEmailOrPassword",
            "ERROR_WRONG_PASSWORD" to "s:invalidPassword",
            "ERROR_INVALID_EMAIL" to "s:invalidEmailAddress",
            "ERROR_MISSING_EMAIL" to "s:missingEmailAddress",
            "ERROR_MISSING_PASSWORD" to "s:requiredField",
            "ERROR_MISSING_VERIFICATION_CODE" to "s:requiredField",
            "ERROR_INVALID_PHONE_NUMBER" to "s:invalidPhoneNumber",
            "ERROR_MISSING_PHONE_NUMBER" to "s:missingPhoneNumber",
            "ERROR_INVALID_VERIFICATION_CODE" to "s:invalidVerificationCode",
            "ERROR_SESSION_EXPIRED" to "s:sessionExpired",
            "ERROR_INVALID_VERIFICATION_ID" to "s:invalidVerificationId",
            "ERROR_MISSING_VERIFICATION_ID" to "s:invalidVerificationId",
            "ERROR_RETRY_PHONE_AUTH" to "s:retryPhoneAuth",
            "ERROR_USER_MISMATCH" to "s:userMismatch",
            "ERROR_PHONE_NUMBER_NOT_FOUND" to "s:phoneNumberNotEnrolled",
            "ERROR_MULTI_FACTOR_INFO_NOT_FOUND" to "s:phoneNumberNotEnrolled",
            "ERROR_INVALID_MULTI_FACTOR_SESSION" to "s:multiFactorSessionExpired",
            "ERROR_MISSING_MULTI_FACTOR_SESSION" to "s:multiFactorSessionExpired",
        )

        val actual = expected.keys.associateWith { code ->
            val result = AuthException.from(invalidCredentials(code), stringProvider)
            assertThat(result).isInstanceOf(AuthException.InvalidCredentialsException::class.java)
            result.message
        }

        assertThat(actual).containsExactlyEntriesIn(expected)
    }

    @Test
    fun `from() uses ERROR_INVALID_CREDENTIAL copy that does not blame the password`() {
        // Under email enumeration protection this single code covers wrong password AND no such
        // account, so reusing the wrong-password string would state something false.
        val stringProvider = mock(AuthUIStringProvider::class.java)
        whenever(stringProvider.errorIncorrectEmailOrPassword).thenReturn("Email or password wrong")
        whenever(stringProvider.invalidPassword).thenReturn("Incorrect password.")

        val result = AuthException.from(invalidCredentials("ERROR_INVALID_CREDENTIAL"), stringProvider)

        assertThat(result.message).isEqualTo("Email or password wrong")
    }

    @Test
    fun `from() prefers the blank-able type-level hook over the per-code string`() {
        val stringProvider = mock(AuthUIStringProvider::class.java)
        whenever(stringProvider.errorInvalidCredentials).thenReturn("Host-wide override")
        whenever(stringProvider.invalidPassword).thenReturn("Incorrect password.")

        val result = AuthException.from(invalidCredentials("ERROR_WRONG_PASSWORD"), stringProvider)

        assertThat(result.message).isEqualTo("Host-wide override")
    }

    @Test
    fun `from() falls back to the Firebase message when the per-code string is blank`() {
        val stringProvider = mock(AuthUIStringProvider::class.java)
        whenever(stringProvider.errorInvalidCredentials).thenReturn("")
        whenever(stringProvider.invalidPassword).thenReturn("")

        val result = AuthException.from(invalidCredentials("ERROR_WRONG_PASSWORD"), stringProvider)

        assertThat(result.message).isEqualTo(sdkText)
    }

    @Test
    fun `from() routes custom token codes to MisconfigurationException`() {
        val stringProvider = mock(AuthUIStringProvider::class.java)
        whenever(stringProvider.unknownErrorRecoveryMessage).thenReturn("Une erreur est survenue")

        for (code in listOf("ERROR_INVALID_CUSTOM_TOKEN", "ERROR_CUSTOM_TOKEN_MISMATCH")) {
            val firebaseException = invalidCredentials(code)
            val result = AuthException.from(firebaseException, stringProvider)
            assertThat(result).isInstanceOf(AuthException.MisconfigurationException::class.java)
            // The diagnostic names the developer's own token backend; it is renderable nowhere.
            assertWithMessage(code).that(result.message).isEqualTo("Une erreur est survenue")
            assertWithMessage(code).that(result.cause).isEqualTo(firebaseException)
            assertWithMessage(code).that(result.cause?.message).isEqualTo(sdkText)
        }
    }

    @Test
    fun `from() routes expired user tokens to the session-expired copy`() {
        val stringProvider = mock(AuthUIStringProvider::class.java)
        whenever(stringProvider.errorMultiFactorSessionExpired).thenReturn("Session gone")

        for (code in listOf("ERROR_INVALID_USER_TOKEN", "ERROR_USER_TOKEN_EXPIRED")) {
            val firebaseException = FirebaseAuthInvalidUserException(code, sdkText)
            val result = AuthException.from(firebaseException, stringProvider)
            assertThat(result).isInstanceOf(AuthException.InvalidCredentialsException::class.java)
            assertThat(result.message).isEqualTo("Session gone")
        }
    }

    @Test
    fun `from() routes action code failures to the action-code copy`() {
        val stringProvider = mock(AuthUIStringProvider::class.java)
        whenever(stringProvider.errorActionCodeInvalid).thenReturn("Link no longer valid")

        for (code in listOf("ERROR_EXPIRED_ACTION_CODE", "ERROR_INVALID_ACTION_CODE")) {
            val firebaseException = FirebaseAuthActionCodeException(code, sdkText)
            val result = AuthException.from(firebaseException, stringProvider)
            assertThat(result).isInstanceOf(AuthException.InvalidCredentialsException::class.java)
            assertThat(result.message).isEqualTo("Link no longer valid")
        }
    }

    @Test
    fun `from() routes the user-facing plain auth codes to their own strings`() {
        val stringProvider = mock(AuthUIStringProvider::class.java)
        whenever(stringProvider.errorUnverifiedEmail).thenReturn("s:unverifiedEmail")
        whenever(stringProvider.errorSecondFactorAlreadyEnrolled).thenReturn("s:alreadyEnrolled")
        whenever(stringProvider.errorMaximumSecondFactorCountExceeded).thenReturn("s:maxFactors")

        val expected = mapOf(
            "ERROR_UNVERIFIED_EMAIL" to "s:unverifiedEmail",
            "ERROR_SECOND_FACTOR_ALREADY_ENROLLED" to "s:alreadyEnrolled",
            "ERROR_MAXIMUM_SECOND_FACTOR_COUNT_EXCEEDED" to "s:maxFactors",
        )

        val actual = expected.keys.associateWith { code ->
            val result = AuthException.from(object : FirebaseAuthException(code, sdkText) {}, stringProvider)
            assertThat(result).isInstanceOf(AuthException.InvalidCredentialsException::class.java)
            result.message
        }

        assertThat(actual).containsExactlyEntriesIn(expected)
    }

    @Test
    fun `from() routes developer setup codes to MisconfigurationException, diagnostic on the cause`() {
        val configurationCodes = listOf(
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
            "ERROR_INVALID_TENANT_ID",
            "ERROR_TENANT_ID_MISMATCH",
            "ERROR_UNSUPPORTED_TENANT_OPERATION",
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
            // Email-template settings in the Firebase console.
            "ERROR_INVALID_MESSAGE_PAYLOAD",
            "ERROR_INVALID_SENDER",
            "ERROR_INVALID_RECIPIENT_EMAIL",
            // Host integration and project quota.
            "ERROR_MISSING_ACTIVITY",
            "ERROR_WEB_STORAGE_UNSUPPORTED",
            "ERROR_QUOTA_EXCEEDED",
        )
        // EmailAuthScreen and PhoneAuthScreen render `exception.message` inline without going
        // through getRecoveryMessage, so the diagnostic must not be on the message at all. It
        // stays on the cause, where logs and `exception.cause?.message` still reach it.
        val stringProvider = mock(AuthUIStringProvider::class.java)
        whenever(stringProvider.errorUnknownAuth).thenReturn("Generic unknown error")
        whenever(stringProvider.unknownErrorRecoveryMessage).thenReturn("Une erreur est survenue")

        for (code in configurationCodes) {
            val firebaseException = object : FirebaseAuthException(code, sdkText) {}
            val result = AuthException.from(firebaseException, stringProvider)
            assertWithMessage(code).that(result)
                .isInstanceOf(AuthException.MisconfigurationException::class.java)
            assertWithMessage(code).that(result.message).isEqualTo("Une erreur est survenue")
            assertWithMessage(code).that(result.cause).isEqualTo(firebaseException)
            assertWithMessage(code).that(result.cause?.message).isEqualTo(sdkText)
        }
    }

    @Test
    fun `from() keeps internal errors out of MisconfigurationException`() {
        // INTERNAL_ERROR is what the SDK falls back to for a status it does not recognise, and
        // ERROR_WEB_INTERNAL_ERROR is a backend fault. Neither is a setup problem.
        for (code in listOf("INTERNAL_ERROR", "ERROR_WEB_INTERNAL_ERROR")) {
            val result = AuthException.from(object : FirebaseAuthException(code, sdkText) {})
            assertWithMessage(code).that(result)
                .isInstanceOf(AuthException.UnknownException::class.java)
        }
    }

    @Test
    fun `from() maps ERROR_USER_CANCELLED to AuthCancelledException`() {
        val stringProvider = mock(AuthUIStringProvider::class.java)
        whenever(stringProvider.errorAuthCancelled).thenReturn("You cancelled")

        for (code in listOf("ERROR_USER_CANCELLED", "ERROR_WEB_CONTEXT_CANCELED")) {
            val result = AuthException.from(object : FirebaseAuthException(code, sdkText) {}, stringProvider)
            assertWithMessage(code).that(result)
                .isInstanceOf(AuthException.AuthCancelledException::class.java)
            assertWithMessage(code).that(result.message).isEqualTo("You cancelled")
        }
    }

    @Test
    fun `new AuthUIStringProvider members compile to real JVM default methods`() {
        // Decision: every new member must be source- AND binary-compatible, so a host that
        // implemented the interface before this change still compiles and still links. An
        // abstract JVM method here would break every existing implementor at runtime.
        val newMembers = listOf(
            "getErrorIncorrectEmailOrPassword",
            "getErrorInvalidVerificationId",
            "getErrorRetryPhoneAuth",
            "getErrorUserMismatch",
            "getErrorPhoneNumberNotEnrolled",
            "getErrorSessionExpired",
            "getErrorMultiFactorSessionExpired",
            "getErrorActionCodeInvalid",
            "getErrorUnverifiedEmail",
            "getErrorSecondFactorAlreadyEnrolled",
            "getErrorMaximumSecondFactorCountExceeded",
        )

        for (name in newMembers) {
            val method = AuthUIStringProvider::class.java.getMethod(name)
            assertWithMessage(name).that(method.isDefault).isTrue()
        }
    }
}