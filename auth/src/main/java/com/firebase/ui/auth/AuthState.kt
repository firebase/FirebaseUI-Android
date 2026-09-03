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

import com.firebase.ui.auth.AuthState.Companion.Cancelled
import com.firebase.ui.auth.AuthState.Companion.Idle
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.MultiFactorResolver
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthProvider
import kotlinx.coroutines.CompletableDeferred
import java.util.UUID

/**
 * Represents the authentication state in Firebase Auth UI.
 *
 * This class encapsulates all possible authentication states that can occur during
 * the authentication flow, including success, error, and intermediate states.
 *
 * Instances come from the companion object factory methods or a subclass constructor; states only
 * the library may publish have an `internal` constructor.
 *
 * @since 10.0.0
 */
abstract class AuthState private constructor() {

    /**
     * Whether this is a one-off notification: something a screen shows once (a dialog, a "link
     * sent" message) and must reset back to [Idle] immediately after consuming, so it doesn't
     * leak to a screen/Activity created later. `abstract` so every new state must explicitly
     * decide this rather than silently defaulting one way.
     */
    abstract val isNotification: Boolean

    /**
     * Initial state before any authentication operation has been started.
     */
    class Idle internal constructor() : AuthState() {
        override val isNotification: Boolean = false
        override fun equals(other: Any?): Boolean = other is Idle
        override fun hashCode(): Int = javaClass.hashCode()
        override fun toString(): String = "AuthState.Idle"
    }

    /**
     * Authentication operation is in progress.
     *
     * @property message Optional message describing what is being loaded
     */
    class Loading(val message: String? = null) : AuthState() {
        override val isNotification: Boolean = false
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is Loading) return false
            return message == other.message
        }

        override fun hashCode(): Int = message?.hashCode() ?: 0

        override fun toString(): String = "AuthState.Loading(message=$message)"
    }

    /**
     * Authentication completed successfully.
     *
     * @property result The [AuthResult] containing the authenticated user, may be null if not available
     * @property user The authenticated [FirebaseUser]
     * @property isNewUser Whether this is a newly created user account
     * @property reauthenticatedUid The uid this success re-proved, or `null` if it is not a
     * reauthentication. Settable only from within the library.
     */
    class Success internal constructor(
        val result: AuthResult?,
        val user: FirebaseUser,
        val isNewUser: Boolean = false,
        val reauthenticatedUid: String? = null
    ) : AuthState() {
        override val isNotification: Boolean = false
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is Success) return false
            return result == other.result &&
                    user == other.user &&
                    isNewUser == other.isNewUser &&
                    reauthenticatedUid == other.reauthenticatedUid
        }

        override fun hashCode(): Int {
            var result1 = result?.hashCode() ?: 0
            result1 = 31 * result1 + user.hashCode()
            result1 = 31 * result1 + isNewUser.hashCode()
            result1 = 31 * result1 + (reauthenticatedUid?.hashCode() ?: 0)
            return result1
        }

        override fun toString(): String =
            "AuthState.Success(result=$result, user=$user, isNewUser=$isNewUser, " +
                    "reauthenticatedUid=$reauthenticatedUid)"
    }

    /**
     * An error occurred during authentication.
     *
     * @property exception The [Exception] that occurred
     * @property isRecoverable Whether the error can be recovered from
     */
    class Error(
        val exception: Exception,
        val isRecoverable: Boolean = true
    ) : AuthState() {
        override val isNotification: Boolean = true
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is Error) return false
            return exception == other.exception &&
                    isRecoverable == other.isRecoverable
        }

        override fun hashCode(): Int {
            var result = exception.hashCode()
            result = 31 * result + isRecoverable.hashCode()
            return result
        }

        override fun toString(): String =
            "AuthState.Error(exception=$exception, isRecoverable=$isRecoverable)"
    }

    /**
     * Authentication was cancelled by the user.
     *
     * This is an operation-level cancellation: the user backed out of a single sign-in
     * attempt (e.g. dismissed the Google Credential Manager sheet, backed out of an MFA
     * challenge). The flow stays open and the screen returns to the method picker.
     *
     * @see Aborted for the state that ends the whole flow instead
     */
    class Cancelled internal constructor() : AuthState() {
        override val isNotification: Boolean = true
        override fun equals(other: Any?): Boolean = other is Cancelled
        override fun hashCode(): Int = javaClass.hashCode()
        override fun toString(): String = "AuthState.Cancelled"
    }

    /**
     * The entire authentication flow was aborted.
     *
     * This state is emitted only by [AuthFlowController.cancel]. Unlike [Cancelled], which is
     * a normal in-flow outcome that leaves the flow open, [Aborted] ends the whole flow —
     * for example, [FirebaseAuthActivity] finishes with `RESULT_CANCELED` when it observes
     * this state.
     *
     * @see Cancelled for the operation-level state that leaves the flow open
     */
    class Aborted internal constructor() : AuthState() {
        override val isNotification: Boolean = true
        override fun equals(other: Any?): Boolean = other is Aborted
        override fun hashCode(): Int = javaClass.hashCode()
        override fun toString(): String = "AuthState.Aborted"
    }

    /**
     * Multi-factor authentication is required to complete sign-in.
     *
     * @property resolver The [MultiFactorResolver] to complete MFA
     * @property hint Optional hint about which factor to use
     */
    class RequiresMfa(
        val resolver: MultiFactorResolver,
        val hint: String? = null
    ) : AuthState() {
        override val isNotification: Boolean = false
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is RequiresMfa) return false
            return resolver == other.resolver &&
                    hint == other.hint
        }

        override fun hashCode(): Int {
            var result = resolver.hashCode()
            result = 31 * result + (hint?.hashCode() ?: 0)
            return result
        }

        override fun toString(): String =
            "AuthState.RequiresMfa(resolver=$resolver, hint=$hint)"
    }

    /**
     * Email verification is required before the user can access the app.
     *
     * @property user The [FirebaseUser] who needs to verify their email
     * @property email The email address that needs verification
     */
    class RequiresEmailVerification(
        val user: FirebaseUser,
        val email: String
    ) : AuthState() {
        override val isNotification: Boolean = false
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is RequiresEmailVerification) return false
            return user == other.user &&
                    email == other.email
        }

        override fun hashCode(): Int {
            var result = user.hashCode()
            result = 31 * result + email.hashCode()
            return result
        }

        override fun toString(): String =
            "AuthState.RequiresEmailVerification(user=$user, email=$email)"
    }

    /**
     * The user needs to complete their profile information.
     *
     * @property user The [FirebaseUser] who needs to complete their profile
     * @property missingFields List of profile fields that need to be completed
     */
    class RequiresProfileCompletion(
        val user: FirebaseUser,
        val missingFields: List<String> = emptyList()
    ) : AuthState() {
        override val isNotification: Boolean = false
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is RequiresProfileCompletion) return false
            return user == other.user &&
                    missingFields == other.missingFields
        }

        override fun hashCode(): Int {
            var result = user.hashCode()
            result = 31 * result + missingFields.hashCode()
            return result
        }

        override fun toString(): String =
            "AuthState.RequiresProfileCompletion(user=$user, missingFields=$missingFields)"
    }

    /**
     * A state in the lifecycle of one reauthentication request.
     *
     * Every state carries a stable [requestId], so Activity recreation can distinguish a
     * continuation of the same sensitive operation from a new operation for the same user. The
     * request itself is process-local because the caller it resolves to cannot be serialized.
     */
    sealed class Reauthentication : AuthState() {
        abstract val requestId: String
        abstract val userUid: String
        internal abstract val request: Request?
        override val isNotification: Boolean = false

        /** Process-local data shared by every resumable state of one reauthentication request. */
        internal class Request(
            val requestId: String,
            val user: FirebaseUser,
            val reason: String?,
            /**
             * Where the caller awaiting this request is parked, or null when nobody is: a
             * standalone flow from [FirebaseAuthUI.createReauthFlow] has no operation behind it.
             * Completing it runs the retry in the caller's own coroutine, which is why nothing
             * retains the caller's closure here.
             */
            val resolver: CompletableDeferred<Boolean>? = null,
        ) {
            /** Whether a caller is waiting on this request to decide a pending operation. */
            val hasPendingOperation: Boolean get() = resolver != null

            /**
             * Whether the awaiting caller is still there to resume. False once its coroutine died
             * with the scope that launched it, which is a request that can no longer complete
             * however well the credential exchange goes.
             */
            val isResumable: Boolean get() = resolver?.isActive != false

            /**
             * Credentials were accepted: the awaiting caller resumes and retries its operation.
             * Idempotent, and a no-op once the caller is gone, so every terminal path can call it
             * without checking first.
             */
            fun resolve() {
                resolver?.complete(true)
            }

            /**
             * The request ended without proof — the user backed out, or the surface was torn down.
             *
             * Completed with a value rather than an exception on purpose. This resolver is
             * parented to the caller's job so that a dead caller is detectable, and completing a
             * parented Deferred *exceptionally* propagates the failure to that parent — declining
             * would cancel the caller's whole scope and take its sibling jobs with it.
             * [FirebaseAuthUI.withReauth] turns this into a throw in its own frame instead, which
             * is an ordinary exception the caller can catch.
             */
            fun decline() {
                resolver?.complete(false)
            }
        }

        /**
         * Reauthentication is required before a sensitive operation (e.g. delete account, change
         * email) can proceed. Use [FirebaseAuthUI.createReauthFlow] to launch a standalone
         * reauthentication flow.
         *
         * @property requestId Stable identifier for this sensitive operation
         * @property user The [FirebaseUser] that needs to reauthenticate
         * @property reason Optional human-readable reason to show the user
         */
        class Required internal constructor(
            override val request: Request,
        ) : Reauthentication() {
            constructor(
                user: FirebaseUser,
                reason: String? = null,
            ) : this(
                Request(
                    requestId = UUID.randomUUID().toString(),
                    user = user,
                    reason = reason,
                )
            )

            override val requestId: String get() = request.requestId
            override val userUid: String get() = request.user.uid
            val user: FirebaseUser get() = request.user
            val reason: String? get() = request.reason

            override fun equals(other: Any?): Boolean =
                other is Required && requestId == other.requestId

            override fun hashCode(): Int = requestId.hashCode()

            override fun toString(): String =
                "AuthState.Reauthentication.Required(requestId=$requestId, " +
                        "user=$user, reason=$reason)"
        }

        /** The user has selected a provider and the library is exchanging credentials. */
        internal class Authenticating(
            override val request: Request,
            val message: String? = null,
        ) : Reauthentication() {
            override val requestId: String get() = request.requestId
            override val userUid: String get() = request.user.uid
        }

        /** The most recent credential attempt failed, but the request remains armed. */
        internal class AttemptFailed(
            override val request: Request,
            val exception: Exception,
        ) : Reauthentication() {
            override val requestId: String get() = request.requestId
            override val userUid: String get() = request.user.uid
        }

        /** A credential attempt requires MFA, which reauthentication UI does not yet support. */
        internal class RequiresMfa(
            override val request: Request,
            val resolver: MultiFactorResolver,
            val hint: String? = null,
        ) : Reauthentication() {
            override val requestId: String get() = request.requestId
            override val userUid: String get() = request.user.uid
        }

        /** Phone verification sent a code and is waiting for the user to enter it. */
        internal class PhoneNumberVerificationRequired(
            override val request: Request,
            val verificationId: String,
            val forceResendingToken: PhoneAuthProvider.ForceResendingToken,
        ) : Reauthentication() {
            override val requestId: String get() = request.requestId
            override val userUid: String get() = request.user.uid
        }

        /** Phone verification obtained a credential automatically. */
        internal class SmsAutoVerified(
            override val request: Request,
            val credential: PhoneAuthCredential,
        ) : Reauthentication() {
            override val requestId: String get() = request.requestId
            override val userUid: String get() = request.user.uid
        }

        /** A password-reset email was sent from the reauthentication email sub-flow. */
        internal class PasswordResetLinkSent(
            override val request: Request,
        ) : Reauthentication() {
            override val requestId: String get() = request.requestId
            override val userUid: String get() = request.user.uid
        }

        /** A sign-in link was sent from the reauthentication email sub-flow. */
        internal class EmailSignInLinkSent(
            override val request: Request,
        ) : Reauthentication() {
            override val requestId: String get() = request.requestId
            override val userUid: String get() = request.user.uid
        }

        /**
         * Credentials were accepted for the request's user. Terminal for the credential exchange:
         * the screen validates the proof, resolves the awaiting caller and ends the request, and
         * the caller's own retry publishes ordinary states from there.
         */
        internal class Succeeded(
            override val request: Request,
            val success: Success,
        ) : Reauthentication() {
            override val requestId: String get() = request.requestId
            override val userUid: String get() = request.user.uid
        }

        /**
         * A provider attempt is about to run, clearing any previously surfaced failure. Null once
         * credentials were accepted, so a late attempt cannot rewind a finished request.
         */
        internal fun attemptStarted(): AuthState? = when (this) {
            is Required,
            is Authenticating,
            is AttemptFailed,
            is RequiresMfa,
            is PhoneNumberVerificationRequired,
            is SmsAutoVerified,
            is PasswordResetLinkSent,
            is EmailSignInLinkSent,
                -> request?.let { Authenticating(it) }

            else -> null
        }

        /**
         * The active sub-flow was consumed, so the request returns to provider selection. Null from
         * a surfaced failure: only [attemptStarted] clears one, when a real attempt replaces it.
         */
        internal fun returnedToProviderSelection(): AuthState? = when (this) {
            is Authenticating,
            is PhoneNumberVerificationRequired,
            is SmsAutoVerified,
            is PasswordResetLinkSent,
            is EmailSignInLinkSent,
                -> request?.let { Required(it) }

            else -> null
        }

        /**
         * The user backed out of an in-flight provider sub-flow. Null in every other phase, so a
         * surfaced failure or a finished request is never rewound to provider selection.
         */
        internal fun attemptCancelled(): AuthState? = when (this) {
            is Authenticating,
            is RequiresMfa,
            is PhoneNumberVerificationRequired,
            is SmsAutoVerified,
                -> request?.let { Required(it) }

            else -> null
        }
    }

    /**
     * Password reset link has been sent to the user's email.
     */
    class PasswordResetLinkSent : AuthState() {
        override val isNotification: Boolean = true
        override fun equals(other: Any?): Boolean = other is PasswordResetLinkSent
        override fun hashCode(): Int = javaClass.hashCode()
        override fun toString(): String = "AuthState.PasswordResetLinkSent"
    }

    /**
     * Email sign in link has been sent to the user's email.
     */
    class EmailSignInLinkSent : AuthState() {
        override val isNotification: Boolean = true
        override fun equals(other: Any?): Boolean = other is EmailSignInLinkSent
        override fun hashCode(): Int = javaClass.hashCode()
        override fun toString(): String = "AuthState.EmailSignInLinkSent"
    }

    /**
     * Phone number was automatically verified via SMS instant verification.
     *
     * This state is emitted when Firebase Phone Authentication successfully retrieves
     * and verifies the SMS code automatically without user interaction. This happens
     * when Google Play services can detect the incoming SMS message.
     *
     * @property credential The [PhoneAuthCredential] that can be used to sign in the user
     *
     * @see PhoneNumberVerificationRequired for the manual verification flow
     */
    class SMSAutoVerified(val credential: PhoneAuthCredential) : AuthState() {
        override val isNotification: Boolean = true
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is SMSAutoVerified) return false
            return credential == other.credential
        }

        override fun hashCode(): Int {
            var result = credential.hashCode()
            result = 31 * result + credential.hashCode()
            return result
        }

        override fun toString(): String =
            "AuthState.SMSAutoVerified(credential=$credential)"
    }

    /**
     * Phone number verification requires manual code entry.
     *
     * This state is emitted when Firebase Phone Authentication cannot instantly verify
     * the phone number and sends an SMS code that the user must manually enter. This is
     * the normal flow when automatic SMS retrieval is not available or fails.
     *
     * **Resending codes:**
     * To allow users to resend the verification code (if they didn't receive it),
     * call [FirebaseAuthUI.verifyPhoneNumber] again with:
     * - `isForceResendingTokenEnabled = true`
     * - `forceResendingToken` from this state
     *
     * @property verificationId The verification ID to use when submitting the code.
     *                          This must be passed to [FirebaseAuthUI.submitVerificationCode].
     * @property forceResendingToken Token that can be used to resend the SMS code if needed
     *
     */
    class PhoneNumberVerificationRequired(
        val verificationId: String,
        val forceResendingToken: PhoneAuthProvider.ForceResendingToken,
    ) : AuthState() {
        override val isNotification: Boolean = false
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is PhoneNumberVerificationRequired) return false
            return verificationId == other.verificationId &&
                    forceResendingToken == other.forceResendingToken
        }

        override fun hashCode(): Int {
            var result = verificationId.hashCode()
            result = 31 * result + forceResendingToken.hashCode()
            return result
        }

        override fun toString(): String =
            "AuthState.PhoneNumberVerificationRequired(verificationId=$verificationId, " +
                    "forceResendingToken=$forceResendingToken)"
    }

    companion object {
        /**
         * Creates an Idle state instance.
         * @return A new [Idle] state
         */
        @JvmStatic
        val Idle: Idle = Idle()

        /**
         * Creates a Cancelled state instance.
         * @return A new [Cancelled] state
         */
        @JvmStatic
        val Cancelled: Cancelled = Cancelled()

        /**
         * Creates an Aborted state instance.
         * @return A new [Aborted] state
         */
        @JvmStatic
        val Aborted: Aborted = Aborted()
    }
}
