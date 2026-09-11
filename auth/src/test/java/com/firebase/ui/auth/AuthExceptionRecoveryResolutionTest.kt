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
import androidx.test.core.app.ApplicationProvider
import com.firebase.ui.auth.configuration.string_provider.AuthUIStringProvider
import com.firebase.ui.auth.configuration.string_provider.DefaultAuthUIStringProvider
import com.firebase.ui.auth.ui.components.getRecoveryActionText
import com.firebase.ui.auth.ui.components.getRecoveryMessage
import com.firebase.ui.auth.ui.components.isRecoverable
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import com.google.firebase.FirebaseException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthMissingActivityForRecaptchaException
import com.google.firebase.auth.FirebaseAuthMultiFactorException
import com.google.firebase.auth.FirebaseAuthRecentLoginRequiredException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import java.util.Locale
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mock
import org.mockito.kotlin.doCallRealMethod
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * End-to-end resolution tests: a real Firebase exception goes through [AuthException.from] with a
 * real [DefaultAuthUIStringProvider], and the result is rendered through
 * [getRecoveryMessage] exactly as the error dialog would render it.
 *
 * Why the real provider and not a mock: the `fui_error_*` type-level hooks are deliberately blank
 * so hosts can override them, so a mocked provider that stubs one proves nothing about what ships.
 * Every assertion here fails if a branch of `from()` falls through a blank hook to the raw,
 * untranslated Firebase SDK diagnostic.
 *
 * @suppress Internal test class
 */
@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class AuthExceptionRecoveryResolutionTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val strings: AuthUIStringProvider = DefaultAuthUIStringProvider(context)

    /** Renders [firebaseException] the way the error dialog does, end to end. */
    private fun resolve(
        firebaseException: Exception,
        provider: AuthUIStringProvider = strings
    ): String = getRecoveryMessage(AuthException.from(firebaseException, provider), provider)

    // Verbatim firebase-auth 24.2.0 diagnostics. All are English regardless of device locale.
    private val networkDiagnostic = "A network error (such as timeout, interrupted connection or " +
            "unreachable host) has occurred."
    private val userNotFoundDiagnostic = "There is no user record corresponding to this " +
            "identifier. The user may have been deleted."
    private val weakPasswordDiagnostic = "The given password is invalid. [ Password should be at " +
            "least 6 characters ]"
    private val emailInUseDiagnostic = "The email address is already in use by another account."
    private val mfaDiagnostic = "Please complete a second factor challenge."
    private val recentLoginDiagnostic = "This operation is sensitive and requires recent " +
            "authentication. Log in again before retrying this request."
    private val cancelledDiagnostic = "User cancelled the sign-in flow."
    private val operationNotAllowedDiagnostic = "This operation is not allowed. This may be " +
            "because the given sign-in provider is disabled for this Firebase project. Enable it " +
            "in the Firebase console, under the sign-in method tab of the Auth section. " +
            "[ OPERATION_NOT_ALLOWED ]"

    private fun userCollision(code: String, email: String? = null): FirebaseAuthUserCollisionException {
        val exception = mock(FirebaseAuthUserCollisionException::class.java)
        whenever(exception.errorCode).thenReturn(code)
        whenever(exception.message).thenReturn(emailInUseDiagnostic)
        whenever(exception.email).thenReturn(email)
        return exception
    }

    private fun multiFactor(): FirebaseAuthMultiFactorException {
        val exception = mock(FirebaseAuthMultiFactorException::class.java)
        whenever(exception.message).thenReturn(mfaDiagnostic)
        return exception
    }

    // =============================================================================================
    // The six types whose type-level hook is deliberately blank
    // =============================================================================================

    @Test
    fun `network failure resolves to the library's own copy, not the SDK diagnostic`() {
        val firebaseException = object : FirebaseException(networkDiagnostic) {}

        val resolved = resolve(firebaseException)

        assertThat(resolved).isEqualTo(strings.networkErrorRecoveryMessage)
        assertThat(resolved).isNotEqualTo(networkDiagnostic)
    }

    @Test
    fun `user not found resolves to the library's own copy, not the SDK diagnostic`() {
        val firebaseException =
            FirebaseAuthInvalidUserException("ERROR_USER_NOT_FOUND", userNotFoundDiagnostic)

        val resolved = resolve(firebaseException)

        assertThat(resolved).isEqualTo(strings.userNotFoundRecoveryMessage)
        assertThat(resolved).isNotEqualTo(userNotFoundDiagnostic)
    }

    @Test
    fun `an unmapped user code resolves to the library's own copy, not the SDK diagnostic`() {
        val firebaseException =
            FirebaseAuthInvalidUserException("ERROR_SOMETHING_NEW", userNotFoundDiagnostic)

        val resolved = resolve(firebaseException)

        assertThat(resolved).isEqualTo(strings.userNotFoundRecoveryMessage)
        assertThat(resolved).isNotEqualTo(userNotFoundDiagnostic)
    }

    @Test
    fun `weak password resolves to the library's own copy, not the SDK diagnostic`() {
        val firebaseException = FirebaseAuthWeakPasswordException(
            "ERROR_WEAK_PASSWORD",
            weakPasswordDiagnostic,
            "Password should be at least 6 characters"
        )

        val authException = AuthException.from(firebaseException, strings)

        assertThat(authException.message).isEqualTo(strings.weakPasswordRecoveryMessage)
        assertThat(authException.message).isNotEqualTo(weakPasswordDiagnostic)
        assertThat(resolve(firebaseException)).startsWith(strings.weakPasswordRecoveryMessage)
    }

    @Test
    fun `email already in use resolves to the library's own copy, not the SDK diagnostic`() {
        val firebaseException = userCollision("ERROR_EMAIL_ALREADY_IN_USE")

        val resolved = resolve(firebaseException)

        assertThat(resolved).isEqualTo(strings.emailAlreadyInUseRecoveryMessage)
        assertThat(resolved).isNotEqualTo(emailInUseDiagnostic)
    }

    @Test
    fun `mfa required resolves to the library's own copy, not the SDK diagnostic`() {
        val firebaseException = multiFactor()

        val resolved = resolve(firebaseException)

        assertThat(resolved).isEqualTo(strings.mfaRequiredRecoveryMessage)
        assertThat(resolved).isNotEqualTo(mfaDiagnostic)
    }

    @Test
    fun `auth cancelled resolves to the library's own copy, not the SDK diagnostic`() {
        for (code in listOf("ERROR_USER_CANCELLED", "ERROR_WEB_CONTEXT_CANCELED")) {
            val firebaseException = object : FirebaseAuthException(code, cancelledDiagnostic) {}

            val resolved = resolve(firebaseException)

            assertWithMessage(code).that(resolved).isEqualTo(strings.authCancelledRecoveryMessage)
            assertWithMessage(code).that(resolved).isNotEqualTo(cancelledDiagnostic)
        }
    }

    @Test
    fun `too many requests resolves to the library's own copy, not the SDK diagnostic`() {
        val diagnostic = "We have blocked all requests from this device due to unusual activity."
        val firebaseException = FirebaseTooManyRequestsException(diagnostic)

        val resolved = resolve(firebaseException)

        assertThat(resolved).isEqualTo(strings.tooManyRequestsRecoveryMessage)
        assertThat(resolved).isNotEqualTo(diagnostic)
    }

    @Test
    fun `account collision resolves to the library's own copy, not the SDK diagnostic`() {
        val codes = listOf(
            "ERROR_ACCOUNT_EXISTS_WITH_DIFFERENT_CREDENTIAL",
            "ERROR_CREDENTIAL_ALREADY_IN_USE",
            "ERROR_SOME_FUTURE_COLLISION_CODE",
        )

        for (code in codes) {
            val resolved = resolve(userCollision(code))

            assertWithMessage(code).that(resolved)
                .isEqualTo(strings.accountLinkingRequiredRecoveryMessage)
            assertWithMessage(code).that(resolved).isNotEqualTo(emailInUseDiagnostic)
        }
    }

    // =============================================================================================
    // Misconfiguration — the diagnostic must not be on the message at all
    // =============================================================================================

    @Test
    fun `a disabled sign-in provider never puts the console diagnostic on the message`() {
        val firebaseException =
            object : FirebaseAuthException("ERROR_OPERATION_NOT_ALLOWED", operationNotAllowedDiagnostic) {}

        val authException = AuthException.from(firebaseException, strings)

        assertThat(authException).isInstanceOf(AuthException.MisconfigurationException::class.java)
        // EmailAuthScreen and PhoneAuthScreen render this inline, bypassing getRecoveryMessage.
        assertThat(authException.message).isEqualTo(strings.unknownErrorRecoveryMessage)
        assertThat(authException.message).doesNotContain("Firebase")
        assertThat(authException.message).doesNotContain("OPERATION_NOT_ALLOWED")
        // The diagnostic is still there for logs and for hosts.
        assertThat(authException.cause).isEqualTo(firebaseException)
        assertThat(authException.cause?.message).isEqualTo(operationNotAllowedDiagnostic)
        assertThat(resolve(firebaseException)).isEqualTo(strings.unknownErrorRecoveryMessage)
    }

    @Test
    fun `the email-template and quota codes are misconfiguration, not unknown errors`() {
        val codes = listOf(
            "ERROR_INVALID_MESSAGE_PAYLOAD",
            "ERROR_INVALID_SENDER",
            "ERROR_INVALID_RECIPIENT_EMAIL",
            // Declared on FirebaseAuthMissingActivityForRecaptchaException's own constructor.
            "ERROR_MISSING_ACTIVITY",
            "ERROR_WEB_STORAGE_UNSUPPORTED",
            "ERROR_QUOTA_EXCEEDED",
        )

        for (code in codes) {
            val diagnostic = "Raw SDK English naming $code."
            val firebaseException = object : FirebaseAuthException(code, diagnostic) {}
            val authException = AuthException.from(firebaseException, strings)

            assertWithMessage(code).that(authException)
                .isInstanceOf(AuthException.MisconfigurationException::class.java)
            assertWithMessage(code).that(authException.message).isNotEqualTo(diagnostic)
            assertWithMessage(code).that(authException.cause?.message).isEqualTo(diagnostic)
        }
    }

    @Test
    fun `the SDK's own missing-activity exception type reaches the misconfiguration arm`() {
        // The synthetic assertions elsewhere pin the `when` arm; this pins that the SDK's own
        // type still reaches it, which an SDK release reparenting it would break silently.
        val firebaseException = FirebaseAuthMissingActivityForRecaptchaException()

        val authException = AuthException.from(firebaseException, strings)

        assertThat(firebaseException.errorCode).isEqualTo("ERROR_MISSING_ACTIVITY")
        assertThat(authException)
            .isInstanceOf(AuthException.MisconfigurationException::class.java)
        // Retrying cannot conjure the Activity the host never supplied.
        assertThat(isRecoverable(authException)).isFalse()
        assertThat(authException.message).isEqualTo(strings.unknownErrorRecoveryMessage)
        assertThat(authException.message).doesNotContain("Recaptcha")
        // The SDK's own English stays on the cause, where logs still reach it.
        assertThat(authException.cause).isEqualTo(firebaseException)
        assertThat(authException.cause?.message).contains("valid Activity is required")
    }

    // =============================================================================================
    // Blanket invariant
    // =============================================================================================

    @Test
    fun `no Firebase error code resolves to the raw SDK diagnostic`() {
        // One representative code per named arm of the FirebaseAuthInvalidCredentialsException
        // branch of from(), then, below the blank line, the codes that fall through to its `else`.
        val codes = listOf(
            "ERROR_INVALID_CREDENTIAL",
            "ERROR_WRONG_PASSWORD",
            "ERROR_INVALID_EMAIL",
            "ERROR_MISSING_EMAIL",
            "ERROR_MISSING_PASSWORD",
            "ERROR_INVALID_PHONE_NUMBER",
            "ERROR_MISSING_PHONE_NUMBER",
            "ERROR_INVALID_VERIFICATION_CODE",
            "ERROR_SESSION_EXPIRED",
            "ERROR_INVALID_VERIFICATION_ID",
            "ERROR_RETRY_PHONE_AUTH",
            "ERROR_USER_MISMATCH",
            "ERROR_PHONE_NUMBER_NOT_FOUND",
            "ERROR_MULTI_FACTOR_INFO_NOT_FOUND",
            "ERROR_MISSING_MULTI_FACTOR_INFO",
            "ERROR_INVALID_MULTI_FACTOR_SESSION",
            "ERROR_INVALID_CUSTOM_TOKEN",
            "ERROR_MISSING_OR_INVALID_NONCE",
            "ERROR_INVALID_AUTHENTICATOR_RESPONSE",
            "ERROR_PASSKEY_ENROLLMENT_NOT_FOUND",

            // The first ships in firebase-auth 24.2.0; the second stands in for a future code.
            "ERROR_REJECTED_CREDENTIAL",
            "ERROR_SOME_FUTURE_CREDENTIAL_CODE",
        )
        val diagnostic = "The Firebase SDK's own untranslated English."

        for (code in codes) {
            val firebaseException =
                com.google.firebase.auth.FirebaseAuthInvalidCredentialsException(code, diagnostic)
            assertWithMessage(code).that(resolve(firebaseException)).isNotEqualTo(diagnostic)
            assertWithMessage(code).that(AuthException.from(firebaseException, strings).message)
                .isNotEqualTo(diagnostic)
        }
    }

    @Test
    fun `no plain auth error code resolves to the raw SDK diagnostic`() {
        val codes = listOf(
            "ERROR_UNVERIFIED_EMAIL",
            "ERROR_SECOND_FACTOR_ALREADY_ENROLLED",
            "ERROR_MAXIMUM_SECOND_FACTOR_COUNT_EXCEEDED",
            "ERROR_OPERATION_NOT_ALLOWED",
            "ERROR_UNAUTHORIZED_DOMAIN",
            "ERROR_INVALID_CERT_HASH",
            "ERROR_RECAPTCHA_NOT_ENABLED",
            "ERROR_QUOTA_EXCEEDED",
            // The else branch: neither user-facing nor a known setup problem.
            "INTERNAL_ERROR",
            "ERROR_WEB_INTERNAL_ERROR",
            "ERROR_SOME_FUTURE_AUTH_CODE",
        )
        val diagnostic = "The Firebase SDK's own untranslated English."

        for (code in codes) {
            val firebaseException = object : FirebaseAuthException(code, diagnostic) {}
            assertWithMessage(code).that(resolve(firebaseException)).isNotEqualTo(diagnostic)
            assertWithMessage(code).that(AuthException.from(firebaseException, strings).message)
                .isNotEqualTo(diagnostic)
        }
    }

    // =============================================================================================
    // The copy is actually translated, not just library-owned
    // =============================================================================================

    @Test
    fun `a French device offline sees French, not the English network diagnostic`() {
        val french = DefaultAuthUIStringProvider(context, Locale.FRENCH)
        val firebaseException = object : FirebaseException(networkDiagnostic) {}

        val resolved = resolve(firebaseException, french)

        assertThat(resolved).isEqualTo(french.networkErrorRecoveryMessage)
        assertThat(resolved).isNotEqualTo(networkDiagnostic)
        // Guards against the French resource silently falling back to values/.
        assertThat(resolved).isNotEqualTo(strings.networkErrorRecoveryMessage)
    }

    @Test
    fun `a French device with no such account sees French, not the English diagnostic`() {
        val french = DefaultAuthUIStringProvider(context, Locale.FRENCH)
        val firebaseException =
            FirebaseAuthInvalidUserException("ERROR_USER_NOT_FOUND", userNotFoundDiagnostic)

        val resolved = resolve(firebaseException, french)

        assertThat(resolved).isEqualTo(french.userNotFoundRecoveryMessage)
        assertThat(resolved).isNotEqualTo(userNotFoundDiagnostic)
        assertThat(resolved).isNotEqualTo(strings.userNotFoundRecoveryMessage)
    }

    @Test
    fun `a French device changing its email sees French, not the English reauth diagnostic`() {
        val french = DefaultAuthUIStringProvider(context, Locale.FRENCH)
        val firebaseException = FirebaseAuthRecentLoginRequiredException(
            "ERROR_REQUIRES_RECENT_LOGIN",
            recentLoginDiagnostic
        )

        val resolved = resolve(firebaseException, french)

        // `errorRecentLoginRequired` ships blank, so the arm falls to the MFA string.
        assertThat(resolved).isEqualTo(french.mfaErrorRecentLoginRequired)
        assertThat(resolved).isNotEqualTo(recentLoginDiagnostic)
        assertThat(resolved).isNotEqualTo(strings.mfaErrorRecentLoginRequired)
    }

    @Test
    fun `reauthentication required resolves to library copy, not the SDK diagnostic`() {
        val firebaseException = FirebaseAuthRecentLoginRequiredException(
            "ERROR_REQUIRES_RECENT_LOGIN",
            recentLoginDiagnostic
        )

        val result = AuthException.from(firebaseException, strings)

        assertThat(result).isInstanceOf(AuthException.InvalidCredentialsException::class.java)
        assertThat(result.message).isEqualTo(strings.mfaErrorRecentLoginRequired)
        assertThat(result.cause?.message).isEqualTo(recentLoginDiagnostic)
        assertThat(resolve(firebaseException)).isEqualTo(strings.mfaErrorRecentLoginRequired)
    }

    // =============================================================================================
    // Developer-setup faults in the invalid-credential family
    // =============================================================================================

    @Test
    fun `a bad Sign in with Apple nonce is reported as a misconfiguration, not a bad password`() {
        val diagnostic = "The supplied auth credential is malformed, has expired or is " +
                "currently unsupported. [ MISSING_OR_INVALID_NONCE ]"

        for (code in listOf("ERROR_MISSING_OR_INVALID_NONCE", "ERROR_INVALID_AUTHENTICATOR_RESPONSE")) {
            val firebaseException =
                com.google.firebase.auth.FirebaseAuthInvalidCredentialsException(code, diagnostic)
            val result = AuthException.from(firebaseException, strings)

            // The host built the federated request wrong, so this must not be recoverable.
            assertWithMessage(code).that(result)
                .isInstanceOf(AuthException.MisconfigurationException::class.java)
            assertWithMessage(code).that(result.message)
                .isEqualTo(strings.unknownErrorRecoveryMessage)
            assertWithMessage(code).that(result.cause?.message).isEqualTo(diagnostic)
        }
    }

    @Test
    fun `unnamed invalid-credential codes stay recoverable but never show the SDK diagnostic`() {
        val diagnostic = "The Firebase SDK's own untranslated English."

        for (code in listOf("ERROR_REJECTED_CREDENTIAL", "ERROR_SOME_FUTURE_CREDENTIAL_CODE")) {
            val firebaseException =
                com.google.firebase.auth.FirebaseAuthInvalidCredentialsException(code, diagnostic)
            val result = AuthException.from(firebaseException, strings)

            // "Mismatching credentials" also covers the wrong account, which signing in fixes.
            assertWithMessage(code).that(result)
                .isInstanceOf(AuthException.InvalidCredentialsException::class.java)
            // But the copy has to be generic — we do not know what the code means.
            assertWithMessage(code).that(result.message)
                .isEqualTo(strings.unknownErrorRecoveryMessage)
            assertWithMessage(code).that(result.message).isNotEqualTo(diagnostic)
            assertWithMessage(code).that(result.cause?.message).isEqualTo(diagnostic)
        }
    }

    @Test
    fun `a missing passkey enrolment points at another sign-in method, not a futile retry`() {
        val diagnostic = "Cannot find the passkey linked to the current account."
        val firebaseException = com.google.firebase.auth.FirebaseAuthInvalidCredentialsException(
            "ERROR_PASSKEY_ENROLLMENT_NOT_FOUND", diagnostic
        )

        val result = AuthException.from(firebaseException, strings)

        // Not InvalidCredentialsException: that is recoverable, so the dialog would offer a retry.
        assertThat(result).isInstanceOf(AuthException.SignInMethodUnavailableException::class.java)
        assertThat(result).isNotInstanceOf(AuthException.InvalidCredentialsException::class.java)
        // Specific copy, not the generic unknown-error string.
        assertThat(result.message).isEqualTo(strings.errorPasskeyNotFound)
        assertThat(result.message).isNotEqualTo(strings.unknownErrorRecoveryMessage)
        assertThat(result.message).isNotEqualTo(diagnostic)
        assertThat(result.cause?.message).isEqualTo(diagnostic)
    }

    @Test
    fun `the dialog offers no retry action for a missing passkey enrolment`() {
        val firebaseException = com.google.firebase.auth.FirebaseAuthInvalidCredentialsException(
            "ERROR_PASSKEY_ENROLLMENT_NOT_FOUND",
            "Cannot find the passkey linked to the current account."
        )

        val result = AuthException.from(firebaseException, strings)

        // The dialog renders the action button only when isRecoverable is true.
        assertThat(isRecoverable(result)).isFalse()
        // And the text itself is not a retry invitation, for any caller reading it directly.
        assertThat(getRecoveryActionText(result, strings)).isNotEqualTo(strings.retryAction)
        assertThat(getRecoveryActionText(result, strings)).isEqualTo(strings.dismissAction)
        // The body still says the useful thing.
        assertThat(getRecoveryMessage(result, strings)).isEqualTo(strings.errorPasskeyNotFound)
    }

    // =============================================================================================
    // A host's own hook still wins
    // =============================================================================================

    @Test
    fun `a host that fills the type-level hook still overrides the generic recovery copy`() {
        val hostStrings = mock(AuthUIStringProvider::class.java)
        whenever(hostStrings.errorNetworkGeneric).thenReturn("Host network copy")
        whenever(hostStrings.networkErrorRecoveryMessage).thenReturn("Generic network copy")

        val result = AuthException.from(object : FirebaseException(networkDiagnostic) {}, hostStrings)

        assertThat(result.message).isEqualTo("Host network copy")
    }

    @Test
    fun `a host's credential copy does not hijack the missing-passkey message`() {
        // `errorInvalidCredentials` is the hook for a type this arm deliberately does not return,
        // so a host overriding both must see its passkey copy, not its password copy.
        val hostStrings = mock(AuthUIStringProvider::class.java)
        whenever(hostStrings.errorInvalidCredentials)
            .thenReturn("Check your password and try again.")
        whenever(hostStrings.errorPasskeyNotFound).thenReturn("Use another way to sign in.")

        val result = AuthException.from(
            com.google.firebase.auth.FirebaseAuthInvalidCredentialsException(
                "ERROR_PASSKEY_ENROLLMENT_NOT_FOUND",
                "Cannot find the passkey linked to the current account."
            ),
            hostStrings
        )

        assertThat(result).isInstanceOf(AuthException.SignInMethodUnavailableException::class.java)
        assertThat(result.message).isEqualTo("Use another way to sign in.")
        assertThat(result.message).isNotEqualTo("Check your password and try again.")
    }

    @Test
    fun `a host that leaves the passkey hook unset gets the generic string, not credential copy`() {
        // The interface default is what a host implementing AuthUIStringProvider directly sees;
        // credential copy there would sit in a dialog with no retry button.
        val hostStrings = mock(AuthUIStringProvider::class.java)
        doCallRealMethod().whenever(hostStrings).errorPasskeyNotFound
        whenever(hostStrings.errorInvalidCredentials).thenReturn("Check your password and try again.")
        whenever(hostStrings.errorUnknownAuth).thenReturn("Something went wrong. Please try later.")

        assertThat(hostStrings.errorPasskeyNotFound)
            .isEqualTo("Something went wrong. Please try later.")
        assertThat(hostStrings.errorPasskeyNotFound)
            .isNotEqualTo("Check your password and try again.")
    }
}
