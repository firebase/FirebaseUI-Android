package com.firebase.ui.auth.compose.configuration.auth_provider

import android.app.Activity
import com.firebase.ui.auth.compose.AuthException
import com.firebase.ui.auth.compose.AuthState
import com.firebase.ui.auth.compose.FirebaseAuthUI
import com.firebase.ui.auth.compose.configuration.AuthUIConfiguration
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.MultiFactorSession
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthProvider
import kotlinx.coroutines.CancellationException

/**
 * Initiates phone number verification with Firebase Phone Authentication.
 *
 * This method starts the phone verification flow, which can complete in two ways:
 * 1. **Instant verification** (auto): Firebase SDK automatically retrieves and verifies
 *    the SMS code without user interaction. This happens when Google Play services can
 *    detect the incoming SMS automatically.
 * 2. **Manual verification**: SMS code is sent to the user's device, and the user must
 *    manually enter the code via [submitVerificationCode].
 *
 * **Flow:**
 * - Call this method with the phone number
 * - Firebase SDK attempts instant verification
 * - If instant verification succeeds:
 *   - Emits [AuthState.SMSAutoVerified] with the credential
 *   - UI should observe this state and call [signInWithPhoneAuthCredential]
 * - If instant verification fails:
 *   - Emits [AuthState.PhoneNumberVerificationRequired] with verification details
 *   - UI should show code entry screen
 *   - User enters code → call [submitVerificationCode]
 *
 * **Resending codes:**
 * To resend a verification code, call this method again with:
 * - `forceResendingToken` = the token from [AuthState.PhoneNumberVerificationRequired]
 *
 * **Example: Basic phone verification**
 * ```kotlin
 * // Step 1: Start verification
 * firebaseAuthUI.verifyPhoneNumber(
 *     provider = phoneProvider,
 *     phoneNumber = "+1234567890",
 * )
 *
 * // Step 2: Observe AuthState
 * authUI.authStateFlow().collect { state ->
 *     when (state) {
 *         is AuthState.SMSAutoVerified -> {
 *             // Instant verification succeeded!
 *             showToast("Phone number verified automatically")
 *             // Now sign in with the credential
 *             firebaseAuthUI.signInWithPhoneAuthCredential(
 *                 config = authUIConfig,
 *                 credential = state.credential
 *             )
 *         }
 *         is AuthState.PhoneNumberVerificationRequired -> {
 *             // Show code entry screen
 *             showCodeEntryScreen(
 *                 verificationId = state.verificationId,
 *                 forceResendingToken = state.forceResendingToken
 *             )
 *         }
 *         is AuthState.Error -> {
 *             // Handle error
 *             showError(state.exception.message)
 *         }
 *     }
 * }
 *
 * // Step 3: When user enters code
 * firebaseAuthUI.submitVerificationCode(
 *     config = authUIConfig,
 *     verificationId = verificationId,
 *     code = userEnteredCode
 * )
 * ```
 *
 * **Example: Resending verification code**
 * ```kotlin
 * // User didn't receive the code, wants to resend
 * firebaseAuthUI.verifyPhoneNumber(
 *     provider = phoneProvider,
 *     phoneNumber = "+1234567890",
 *     forceResendingToken = savedToken  // From PhoneNumberVerificationRequired state
 * )
 * ```
 *
 * @param provider The [AuthProvider.Phone] configuration containing timeout and other settings
 * @param phoneNumber The phone number to verify in E.164 format (e.g., "+1234567890")
 * @param multiFactorSession Optional [MultiFactorSession] for MFA enrollment. When provided,
 * this initiates phone verification for enrolling a second factor rather than primary sign-in.
 * Obtain this from `FirebaseUser.multiFactor.session` when enrolling MFA.
 * @param forceResendingToken Optional token from previous verification for resending SMS
 *
 * @throws AuthException.InvalidCredentialsException if the phone number is invalid
 * @throws AuthException.TooManyRequestsException if SMS quota is exceeded
 * @throws AuthException.AuthCancelledException if the operation is cancelled
 * @throws AuthException.NetworkException if a network error occurs
 */
internal suspend fun FirebaseAuthUI.verifyPhoneNumber(
    provider: AuthProvider.Phone,
    activity: Activity?,
    phoneNumber: String,
    multiFactorSession: MultiFactorSession? = null,
    forceResendingToken: PhoneAuthProvider.ForceResendingToken? = null,
    verifier: AuthProvider.Phone.Verifier = AuthProvider.Phone.DefaultVerifier(),
) {
    try {
        updateAuthState(AuthState.Loading("Verifying phone number..."))
        val result = provider.verifyPhoneNumberAwait(
            auth = auth,
            activity = activity,
            phoneNumber = phoneNumber,
            multiFactorSession = multiFactorSession,
            forceResendingToken = forceResendingToken,
            verifier = verifier
        )
        when (result) {
            is AuthProvider.Phone.VerifyPhoneNumberResult.AutoVerified -> {
                updateAuthState(AuthState.SMSAutoVerified(credential = result.credential))
            }

            is AuthProvider.Phone.VerifyPhoneNumberResult.NeedsManualVerification -> {
                updateAuthState(
                    AuthState.PhoneNumberVerificationRequired(
                        verificationId = result.verificationId,
                        forceResendingToken = result.token,
                    )
                )
            }
        }
    } catch (e: CancellationException) {
        val cancelledException = AuthException.AuthCancelledException(
            message = "Verify phone number was cancelled",
            cause = e
        )
        updateAuthState(AuthState.Error(cancelledException))
        throw cancelledException
    } catch (e: AuthException) {
        updateAuthState(AuthState.Error(e))
        throw e
    } catch (e: Exception) {
        val authException = AuthException.from(e)
        updateAuthState(AuthState.Error(authException))
        throw authException
    }
}

/**
 * Submits a verification code entered by the user and signs them in.
 *
 * This method is called after [verifyPhoneNumber] emits [AuthState.PhoneNumberVerificationRequired],
 * indicating that manual code entry is needed. It creates a [PhoneAuthCredential] from the
 * verification ID and user-entered code, then signs in the user by calling
 * [signInWithPhoneAuthCredential].
 *
 * **Flow:**
 * 1. User receives SMS with 6-digit code
 * 2. User enters code in UI
 * 3. UI calls this method with the code
 * 4. Credential is created and used to sign in
 * 5. Returns [AuthResult] with signed-in user
 *
 * This method handles both normal sign-in and anonymous account upgrade scenarios based
 * on the [AuthUIConfiguration] settings.
 *
 * **Example: Manual code entry flow*
 * ```
 * val userEnteredCode = "123456"
 * try {
 *     val result = firebaseAuthUI.submitVerificationCode(
 *         config = authUIConfig,
 *         verificationId = savedVerificationId!!,
 *         code = userEnteredCode
 *     )
 *     // User is now signed in
 * } catch (e: AuthException.InvalidCredentialsException) {
 *     // Wrong code entered
 *     showError("Invalid verification code")
 * } catch (e: AuthException.SessionExpiredException) {
 *     // Code expired
 *     showError("Verification code expired. Please request a new one.")
 * }
 * ```
 *
 * @param config The [AuthUIConfiguration] containing authentication settings
 * @param verificationId The verification ID from [AuthState.PhoneNumberVerificationRequired]
 * @param code The 6-digit verification code entered by the user
 *
 * @return [AuthResult] containing the signed-in user
 *
 * @throws AuthException.InvalidCredentialsException if the code is incorrect or expired
 * @throws AuthException.AuthCancelledException if the operation is cancelled
 * @throws AuthException.NetworkException if a network error occurs
 */
internal suspend fun FirebaseAuthUI.submitVerificationCode(
    config: AuthUIConfiguration,
    verificationId: String,
    code: String,
    credentialProvider: AuthProvider.Phone.CredentialProvider = AuthProvider.Phone.DefaultCredentialProvider(),
): AuthResult? {
    try {
        updateAuthState(AuthState.Loading("Submitting verification code..."))
        val credential = credentialProvider.getCredential(verificationId, code)
        return signInWithPhoneAuthCredential(
            config = config,
            credential = credential
        )
    } catch (e: CancellationException) {
        val cancelledException = AuthException.AuthCancelledException(
            message = "Submit verification code was cancelled",
            cause = e
        )
        updateAuthState(AuthState.Error(cancelledException))
        throw cancelledException
    } catch (e: AuthException) {
        updateAuthState(AuthState.Error(e))
        throw e
    } catch (e: Exception) {
        val authException = AuthException.from(e)
        updateAuthState(AuthState.Error(authException))
        throw authException
    }
}

/**
 * Signs in a user with a phone authentication credential.
 *
 * This method is the final step in the phone authentication flow. It takes a
 * [PhoneAuthCredential] (either from instant verification or manual code entry) and
 * signs in the user. The method handles both normal sign-in and anonymous account
 * upgrade scenarios by delegating to [signInAndLinkWithCredential].
 *
 * **When to call this:**
 * - After [verifyPhoneNumber] emits [AuthState.SMSAutoVerified] (instant verification)
 * - Called internally by [submitVerificationCode] (manual verification)
 *
 * The method automatically handles:
 * - Normal sign-in for new or returning users
 * - Linking phone credential to anonymous accounts (if enabled in config)
 * - Throwing [AuthException.AccountLinkingRequiredException] if phone number already exists on another account
 *
 * **Example: Sign in after instant verification**
 * ```kotlin
 * authUI.authStateFlow().collect { state ->
 *     when (state) {
 *         is AuthState.SMSAutoVerified -> {
 *             // Phone was instantly verified
 *             showToast("Phone verified automatically!")
 *
 *             // Now sign in with the credential
 *             val result = firebaseAuthUI.signInWithPhoneAuthCredential(
 *                 config = authUIConfig,
 *                 credential = state.credential
 *             )
 *             // User is now signed in
 *         }
 *     }
 * }
 * ```
 *
 * **Example: Anonymous upgrade with collision**
 * ```kotlin
 * // User is currently anonymous
 * try {
 *     firebaseAuthUI.signInWithPhoneAuthCredential(
 *         config = authUIConfig,
 *         credential = phoneCredential
 *     )
 * } catch (e: AuthException.AccountLinkingRequiredException) {
 *     // Phone number already exists on another account
 *     // Account linking required - show account linking screen
 *     // User needs to sign in with existing account to link
 * }
 * ```
 *
 * @param config The [AuthUIConfiguration] containing authentication settings
 * @param credential The [PhoneAuthCredential] to use for signing in
 *
 * @return [AuthResult] containing the signed-in user, or null if anonymous upgrade collision occurred
 *
 * @throws AuthException.InvalidCredentialsException if the credential is invalid or expired
 * @throws AuthException.EmailAlreadyInUseException if phone number is linked to another account
 * @throws AuthException.AuthCancelledException if the operation is cancelled
 * @throws AuthException.NetworkException if a network error occurs
 */
internal suspend fun FirebaseAuthUI.signInWithPhoneAuthCredential(
    config: AuthUIConfiguration,
    credential: PhoneAuthCredential,
): AuthResult? {
    try {
        updateAuthState(AuthState.Loading("Signing in with phone..."))
        return signInAndLinkWithCredential(
            config = config,
            credential = credential,
        )
    } catch (e: CancellationException) {
        val cancelledException = AuthException.AuthCancelledException(
            message = "Sign in with phone was cancelled",
            cause = e
        )
        updateAuthState(AuthState.Error(cancelledException))
        throw cancelledException
    } catch (e: AuthException) {
        updateAuthState(AuthState.Error(e))
        throw e
    } catch (e: Exception) {
        val authException = AuthException.from(e)
        updateAuthState(AuthState.Error(authException))
        throw authException
    }
}
