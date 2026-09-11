package com.firebase.ui.auth.ui.components

import com.firebase.ui.auth.AuthException
import com.firebase.ui.auth.configuration.string_provider.AuthUIStringProvider
import com.google.common.truth.Truth
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.GoogleAuthProvider
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Unit tests for [ErrorRecoveryDialog] logic functions.
 */
@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class ErrorRecoveryDialogLogicTest {

    private val mockStringProvider = Mockito.mock(AuthUIStringProvider::class.java).apply {
        Mockito.`when`(retryAction).thenReturn("Try again")
        Mockito.`when`(continueText).thenReturn("Continue")
        Mockito.`when`(signInDefault).thenReturn("Sign in")
        Mockito.`when`(continueWithGoogle).thenReturn("Continue with Google")
        Mockito.`when`(signInWithEmailLink).thenReturn("Sign in with email link")
        Mockito.`when`(networkErrorRecoveryMessage).thenReturn("Network error, check your internet connection.")
        Mockito.`when`(invalidCredentialsRecoveryMessage).thenReturn("Incorrect password.")
        Mockito.`when`(userNotFoundRecoveryMessage).thenReturn("That email address doesn't match an existing account")
        Mockito.`when`(weakPasswordRecoveryMessage).thenReturn("Password not strong enough. Use at least 6 characters and a mix of letters and numbers")
        Mockito.`when`(emailAlreadyInUseRecoveryMessage).thenReturn("Email account registration unsuccessful")
        Mockito.`when`(tooManyRequestsRecoveryMessage).thenReturn("This phone number has been used too many times")
        Mockito.`when`(mfaRequiredRecoveryMessage).thenReturn("Additional verification required. Please complete multi-factor authentication.")
        Mockito.`when`(accountLinkingRequiredRecoveryMessage).thenReturn("Account needs to be linked. Please try a different sign-in method.")
        Mockito.`when`(authCancelledRecoveryMessage).thenReturn("Authentication was cancelled. Please try again when ready.")
        Mockito.`when`(unknownErrorRecoveryMessage).thenReturn("An unknown error occurred.")
    }

    // =============================================================================================
    // Recovery Message Tests
    // =============================================================================================

    @Test
    fun `getRecoveryMessage prefers the library-owned message for NetworkException`() {
        // Arrange - AuthException.from now puts the configured provider's string on the exception,
        // so discarding it here would throw away the host's own translated copy.
        val error = AuthException.NetworkException("Pas de connexion Internet")

        // Act
        val message = getRecoveryMessage(error, mockStringProvider)

        // Assert
        Truth.assertThat(message).isEqualTo("Pas de connexion Internet")
    }

    @Test
    fun `getRecoveryMessage returns network error message for NetworkException with blank message`() {
        // Arrange
        val error = AuthException.NetworkException("")

        // Act
        val message = getRecoveryMessage(error, mockStringProvider)

        // Assert
        Truth.assertThat(message).isEqualTo("Network error, check your internet connection.")
    }

    @Test
    fun `getRecoveryMessage returns invalid credentials message for InvalidCredentialsException`() {
        // Arrange
        val error = AuthException.InvalidCredentialsException("Invalid credentials")

        // Act
        val message = getRecoveryMessage(error, mockStringProvider)

        // Assert - Should show the actual error message since it's not the generic fallback
        Truth.assertThat(message).isEqualTo("Invalid credentials")
    }

    @Test
    fun `getRecoveryMessage returns actual Firebase error message for InvalidCredentialsException`() {
        // Arrange - Simulate a real Firebase error message
        val error = AuthException.InvalidCredentialsException("The email address is badly formatted.")

        // Act
        val message = getRecoveryMessage(error, mockStringProvider)

        // Assert - Should show the actual Firebase error, not the generic message
        Truth.assertThat(message).isEqualTo("The email address is badly formatted.")
    }

    @Test
    fun `getRecoveryMessage shows the hardcoded fallback text for InvalidCredentialsException`() {
        // Arrange - The old sentinel dropped this exact string on the floor. It could never match
        // real traffic anyway: the SDK formats every message as "<canned English> [ <detail> ]".
        val error = AuthException.InvalidCredentialsException("Invalid credentials provided")

        // Act
        val message = getRecoveryMessage(error, mockStringProvider)

        // Assert
        Truth.assertThat(message).isEqualTo("Invalid credentials provided")
    }

    @Test
    fun `getRecoveryMessage returns generic message for InvalidCredentialsException with blank message`() {
        // Arrange
        val error = AuthException.InvalidCredentialsException("")

        // Act
        val message = getRecoveryMessage(error, mockStringProvider)

        // Assert - Should show the localized generic message
        Truth.assertThat(message).isEqualTo("Incorrect password.")
    }

    @Test
    fun `getRecoveryMessage prefers the library-owned message for UserNotFoundException`() {
        // Arrange
        val error = AuthException.UserNotFoundException("Aucun compte pour cette adresse")

        // Act
        val message = getRecoveryMessage(error, mockStringProvider)

        // Assert
        Truth.assertThat(message).isEqualTo("Aucun compte pour cette adresse")
    }

    @Test
    fun `getRecoveryMessage returns user not found message for UserNotFoundException with blank message`() {
        // Arrange
        val error = AuthException.UserNotFoundException("")

        // Act
        val message = getRecoveryMessage(error, mockStringProvider)

        // Assert
        Truth.assertThat(message).isEqualTo("That email address doesn't match an existing account")
    }

    @Test
    fun `getRecoveryMessage drops the untranslated reason for WeakPasswordException`() {
        // Arrange - the reason is the raw SDK string, English in every locale.
        val error = AuthException.WeakPasswordException(
            "",
            null,
            "Password should be at least 8 characters"
        )

        // Act
        val message = getRecoveryMessage(error, mockStringProvider)

        // Assert - blank message, so the provider string supplies the whole body. The reason is
        // not appended: it is untranslated, and the provider string already states the minimum.
        Truth.assertThat(message).isEqualTo("Password not strong enough. Use at least 6 characters and a mix of letters and numbers")
        Truth.assertThat(message).doesNotContain("Reason:")
        Truth.assertThat(message).doesNotContain("Password should be at least 8 characters")
    }

    @Test
    fun `getRecoveryMessage returns weak password message without reason for WeakPasswordException`() {
        // Arrange
        val error = AuthException.WeakPasswordException("", null, null)

        // Act
        val message = getRecoveryMessage(error, mockStringProvider)

        // Assert
        Truth.assertThat(message).isEqualTo("Password not strong enough. Use at least 6 characters and a mix of letters and numbers")
    }

    @Test
    fun `getRecoveryMessage returns email already in use message with email for EmailAlreadyInUseException`() {
        // Arrange
        val error = AuthException.EmailAlreadyInUseException(
            "",
            null,
            "test@example.com"
        )

        // Act
        val message = getRecoveryMessage(error, mockStringProvider)

        // Assert - blank message, so the provider string supplies the base and the email is kept
        Truth.assertThat(message).isEqualTo("Email account registration unsuccessful (test@example.com)")
    }

    @Test
    fun `getRecoveryMessage returns email already in use message without email for EmailAlreadyInUseException`() {
        // Arrange
        val error = AuthException.EmailAlreadyInUseException("", null, null)

        // Act
        val message = getRecoveryMessage(error, mockStringProvider)

        // Assert
        Truth.assertThat(message).isEqualTo("Email account registration unsuccessful")
    }

    // =============================================================================================
    // Misconfiguration — the one message that is never rendered
    // =============================================================================================

    @Test
    fun `getRecoveryMessage never shows the raw message for MisconfigurationException`() {
        // Arrange - exactly what firebase-auth 24.2.0 puts on a disabled sign-in provider. It is
        // untranslated, it names the Firebase console, and the user can do nothing with it.
        val rawDiagnostic = "This operation is not allowed. This may be because the given sign-in " +
                "provider is disabled for this Firebase project. Enable it in the Firebase " +
                "console, under the sign-in method tab of the Auth section. [ OPERATION_NOT_ALLOWED ]"
        val error = AuthException.MisconfigurationException(rawDiagnostic)

        // Act
        val message = getRecoveryMessage(error, mockStringProvider)

        // Assert
        Truth.assertThat(message).isEqualTo("An unknown error occurred.")
        Truth.assertThat(message).doesNotContain("Firebase")
        Truth.assertThat(message).doesNotContain("OPERATION_NOT_ALLOWED")
    }

    @Test
    fun `MisconfigurationException from() keeps the raw diagnostic on the cause, not the message`() {
        // EmailAuthScreen and PhoneAuthScreen render `exception.message` inline without going
        // through getRecoveryMessage, so the diagnostic has to be off the message entirely.
        val rawDiagnostic = "The supplied auth credential is malformed. [ INVALID_CERT_HASH ]"
        val firebaseException =
            object : FirebaseAuthException("ERROR_INVALID_CERT_HASH", rawDiagnostic) {}

        val error = AuthException.from(firebaseException, mockStringProvider)

        Truth.assertThat(error).isInstanceOf(AuthException.MisconfigurationException::class.java)
        Truth.assertThat(error.message).isEqualTo("An unknown error occurred.")
        Truth.assertThat(error.cause).isEqualTo(firebaseException)
        Truth.assertThat(error.cause?.message).isEqualTo(rawDiagnostic)
    }

    @Test
    fun `isRecoverable returns false for MisconfigurationException`() {
        val error = AuthException.MisconfigurationException("Unauthorized domain")

        Truth.assertThat(isRecoverable(error)).isFalse()
    }

    // =============================================================================================
    // Subtypes whose arms used to discard error.message outright
    // =============================================================================================

    @Test
    fun `getRecoveryMessage prefers the library-owned message for TooManyRequestsException`() {
        val error = AuthException.TooManyRequestsException("Trop de tentatives")

        Truth.assertThat(getRecoveryMessage(error, mockStringProvider))
            .isEqualTo("Trop de tentatives")
    }

    @Test
    fun `getRecoveryMessage returns the recovery string for TooManyRequestsException with blank message`() {
        val error = AuthException.TooManyRequestsException("")

        Truth.assertThat(getRecoveryMessage(error, mockStringProvider))
            .isEqualTo("This phone number has been used too many times")
    }

    @Test
    fun `getRecoveryMessage prefers the library-owned message for MfaRequiredException`() {
        val error = AuthException.MfaRequiredException("Vérification supplémentaire requise")

        Truth.assertThat(getRecoveryMessage(error, mockStringProvider))
            .isEqualTo("Vérification supplémentaire requise")
    }

    @Test
    fun `getRecoveryMessage returns the recovery string for MfaRequiredException with blank message`() {
        val error = AuthException.MfaRequiredException("")

        Truth.assertThat(getRecoveryMessage(error, mockStringProvider))
            .isEqualTo("Additional verification required. Please complete multi-factor authentication.")
    }

    @Test
    fun `getRecoveryMessage prefers the library-owned message for AuthCancelledException`() {
        val error = AuthException.AuthCancelledException("Connexion annulée")

        Truth.assertThat(getRecoveryMessage(error, mockStringProvider))
            .isEqualTo("Connexion annulée")
    }

    @Test
    fun `getRecoveryMessage returns the recovery string for AuthCancelledException with blank message`() {
        val error = AuthException.AuthCancelledException("")

        Truth.assertThat(getRecoveryMessage(error, mockStringProvider))
            .isEqualTo("Authentication was cancelled. Please try again when ready.")
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
        Truth.assertThat(actionText).isEqualTo("Try again")
    }

    @Test
    fun `getRecoveryActionText returns continue for AuthCancelledException, ignoring the raw exception message`() {
        // Arrange - message mirrors the raw GetCredentialCancellationException message from GH #2422;
        // the action label must never surface this raw string to the user
        val error = AuthException.AuthCancelledException("User cancelled the selector")

        // Act
        val actionText = getRecoveryActionText(error, mockStringProvider)

        // Assert
        Truth.assertThat(actionText).isEqualTo("Continue")
    }

    @Test
    fun `getRecoveryActionText returns sign in for EmailAlreadyInUseException`() {
        // Arrange
        val error = AuthException.EmailAlreadyInUseException("Email already in use", null, null)

        // Act
        val actionText = getRecoveryActionText(error, mockStringProvider)

        // Assert
        Truth.assertThat(actionText).isEqualTo("Sign in")
    }

    @Test
    fun `getRecoveryActionText returns sign in for AccountLinkingRequiredException`() {
        // Arrange - user needs to sign in with the existing method to link accounts
        val error = AuthException.AccountLinkingRequiredException("Account linking required")

        // Act
        val actionText = getRecoveryActionText(error, mockStringProvider)

        // Assert
        Truth.assertThat(actionText).isEqualTo("Sign in")
    }

    @Test
    fun `getRecoveryActionText returns provider specific action for DifferentSignInMethodRequiredException`() {
        val error = AuthException.DifferentSignInMethodRequiredException(
            message = "Use a different sign-in method",
            email = "test@example.com",
            signInMethods = listOf(GoogleAuthProvider.PROVIDER_ID),
            suggestedSignInMethod = GoogleAuthProvider.PROVIDER_ID
        )

        val actionText = getRecoveryActionText(error, mockStringProvider)

        Truth.assertThat(actionText).isEqualTo("Continue with Google")
    }

    @Test
    fun `getRecoveryActionText returns email link action for DifferentSignInMethodRequiredException`() {
        val error = AuthException.DifferentSignInMethodRequiredException(
            message = "Use a different sign-in method",
            email = "test@example.com",
            signInMethods = listOf(EmailAuthProvider.EMAIL_LINK_SIGN_IN_METHOD),
            suggestedSignInMethod = EmailAuthProvider.EMAIL_LINK_SIGN_IN_METHOD
        )

        val actionText = getRecoveryActionText(error, mockStringProvider)

        Truth.assertThat(actionText).isEqualTo("Sign in with email link")
    }

    @Test
    fun `getRecoveryActionText returns continue for MfaRequiredException`() {
        // Arrange
        val error = AuthException.MfaRequiredException("MFA required")

        // Act
        val actionText = getRecoveryActionText(error, mockStringProvider)

        // Assert
        Truth.assertThat(actionText).isEqualTo("Continue")
    }

    // =============================================================================================
    // Recoverable Tests
    // =============================================================================================

    @Test
    fun `isRecoverable returns true for NetworkException`() {
        // Arrange
        val error = AuthException.NetworkException("Network error")

        // Act & Assert
        Truth.assertThat(isRecoverable(error)).isTrue()
    }

    @Test
    fun `isRecoverable returns true for InvalidCredentialsException`() {
        // Arrange
        val error = AuthException.InvalidCredentialsException("Invalid credentials")

        // Act & Assert
        Truth.assertThat(isRecoverable(error)).isTrue()
    }

    @Test
    fun `isRecoverable returns false for TooManyRequestsException`() {
        // Arrange
        val error = AuthException.TooManyRequestsException("Too many requests")

        // Act & Assert
        Truth.assertThat(isRecoverable(error)).isFalse()
    }

    @Test
    fun `isRecoverable returns true for MfaRequiredException`() {
        // Arrange
        val error = AuthException.MfaRequiredException("MFA required")

        // Act & Assert
        Truth.assertThat(isRecoverable(error)).isTrue()
    }

    @Test
    fun `isRecoverable returns true for UnknownException`() {
        // Arrange
        val error = AuthException.UnknownException("Unknown error")

        // Act & Assert
        Truth.assertThat(isRecoverable(error)).isTrue()
    }
}
