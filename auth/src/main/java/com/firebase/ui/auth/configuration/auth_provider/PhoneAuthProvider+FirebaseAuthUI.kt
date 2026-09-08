package com.firebase.ui.auth.configuration.auth_provider

import android.app.Activity
import android.content.Context
import com.firebase.ui.auth.AuthFlowScope
import com.firebase.ui.auth.AuthException
import com.firebase.ui.auth.AuthState
import com.firebase.ui.auth.util.SignInPreferenceManager
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.MultiFactorSession
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthProvider
import kotlinx.coroutines.CancellationException

/**
 * Starts phone verification for [phoneNumber].
 *
 * Firebase may verify instantly, emitting [AuthState.SMSAutoVerified] with a ready credential,
 * or fall back to SMS and emit [AuthState.PhoneNumberVerificationRequired] for code entry.
 * Passing [forceResendingToken] from that state resends the code.
 *
 * Firebase reports progress as a stream, so this call does not return when the code is sent. On
 * the SMS path it keeps collecting until the auto-retrieval window expires, verification fails,
 * or the caller is cancelled — so a credential auto-retrieved after
 * [AuthState.PhoneNumberVerificationRequired] still arrives, and a host already on code entry
 * must handle the late [AuthState.SMSAutoVerified]. On the instant path Firebase reports no
 * terminal callback at all, so only cancellation ends the call: cancel a superseded attempt
 * before starting a new one.
 */
internal suspend fun AuthFlowScope.verifyPhoneNumber(
    provider: AuthProvider.Phone,
    activity: Activity?,
    phoneNumber: String,
    multiFactorSession: MultiFactorSession? = null,
    forceResendingToken: PhoneAuthProvider.ForceResendingToken? = null,
    verifier: AuthProvider.Phone.Verifier = AuthProvider.Phone.DefaultVerifier(),
) {
    try {
        emit(AuthState.Loading(config.stringProvider.loadingVerifyingPhoneNumber))
        provider.verifyPhoneNumberFlow(
            auth = auth,
            activity = activity,
            phoneNumber = phoneNumber,
            multiFactorSession = multiFactorSession,
            forceResendingToken = forceResendingToken,
            verifier = verifier
        ).collect { result ->
            when (result) {
                is AuthProvider.Phone.VerifyPhoneNumberResult.AutoVerified -> {
                    emit(AuthState.SMSAutoVerified(credential = result.credential))
                }

                is AuthProvider.Phone.VerifyPhoneNumberResult.NeedsManualVerification -> {
                    emit(
                        AuthState.PhoneNumberVerificationRequired(
                            verificationId = result.verificationId,
                            forceResendingToken = result.token,
                        )
                    )
                }
            }
        }
    } catch (e: CancellationException) {
        // Writes nothing: the caller cancelling this attempt owns whatever state replaces it, and
        // a retraction from here would race the replacement's own Loading.
        throw e
    } catch (e: AuthException) {
        emit(AuthState.Error(e))
        throw e
    } catch (e: Exception) {
        val authException = AuthException.from(e)
        emit(AuthState.Error(authException))
        throw authException
    }
}

/**
 * Builds a credential from [verificationId] and the [code] the user typed, then signs in.
 *
 * Follows [AuthState.PhoneNumberVerificationRequired], which carries the verification id.
 * Signing in goes through [signInWithPhoneAuthCredential], so anonymous upgrade is handled
 * there rather than here.
 *
 * @throws AuthException.InvalidCredentialsException when the code is wrong or has expired.
 */
internal suspend fun AuthFlowScope.submitVerificationCode(
    context: Context,
    verificationId: String,
    code: String,
    credentialProvider: AuthProvider.Phone.CredentialProvider = AuthProvider.Phone.DefaultCredentialProvider(),
): AuthResult? {
    try {
        emit(AuthState.Loading(config.stringProvider.loadingSubmittingVerificationCode))
        val credential = credentialProvider.getCredential(verificationId, code)
        return signInWithPhoneAuthCredential(
            context = context,
            credential = credential
        )
    } catch (e: CancellationException) {
        val cancelledException = AuthException.AuthCancelledException(
            message = "Submit verification code was cancelled",
            cause = e
        )
        emit(AuthState.Error(cancelledException))
        throw cancelledException
    } catch (e: AuthException) {
        emit(AuthState.Error(e))
        throw e
    } catch (e: Exception) {
        val authException = AuthException.from(e, context)
        emit(AuthState.Error(authException))
        throw authException
    }
}

/**
 * Signs in with a verified [PhoneAuthCredential], from either verification path.
 *
 * Delegates to [signInAndLinkWithCredential], so anonymous upgrade and the
 * [AuthException.AccountLinkingRequiredException] raised when the number already belongs to
 * another account behave as they do for every other provider.
 */
internal suspend fun AuthFlowScope.signInWithPhoneAuthCredential(
    context: Context,
    credential: PhoneAuthCredential,
): AuthResult? {
    try {
        emit(AuthState.Loading(config.stringProvider.loadingSigningInWithPhone))
        val result = signInAndLinkWithCredential(
            credential = credential,
        )

        // Save sign-in preference for "Continue as..." feature
        if (result != null) {
            try {
                val user = auth.currentUser
                val identifier = user?.phoneNumber
                if (identifier != null) {
                    SignInPreferenceManager.saveLastSignIn(
                        context = context,
                        providerId = "phone",
                        identifier = identifier
                    )
                    android.util.Log.d("PhoneAuthProvider", "Sign-in preference saved for: $identifier")
                }
            } catch (e: Exception) {
                // Failed to save preference - log but don't break auth flow
                android.util.Log.w("PhoneAuthProvider", "Failed to save sign-in preference", e)
            }
        }

        return result
    } catch (e: CancellationException) {
        val cancelledException = AuthException.AuthCancelledException(
            message = "Sign in with phone was cancelled",
            cause = e
        )
        emit(AuthState.Error(cancelledException))
        throw cancelledException
    } catch (e: AuthException) {
        emit(AuthState.Error(e))
        throw e
    } catch (e: Exception) {
        val authException = AuthException.from(e, context)
        emit(AuthState.Error(authException))
        throw authException
    }
}
