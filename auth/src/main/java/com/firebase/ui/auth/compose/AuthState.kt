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

import com.firebase.ui.auth.compose.configuration.auth_provider.AuthProvider
import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.MultiFactorResolver

/**
 * Represents the authentication state in Firebase Auth UI.
 *
 * This class encapsulates all possible authentication states that can occur during
 * the authentication flow, including success, error, and intermediate states.
 *
 * Use the companion object factory methods or specific subclass constructors to create instances.
 *
 * @since 10.0.0
 */
abstract class AuthState private constructor() {

    /**
     * Initial state before any authentication operation has been started.
     */
    class Idle internal constructor() : AuthState() {
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
     */
    class Success(
        val result: AuthResult?,
        val user: FirebaseUser,
        val isNewUser: Boolean = false
    ) : AuthState() {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is Success) return false
            return result == other.result &&
                   user == other.user &&
                   isNewUser == other.isNewUser
        }

        override fun hashCode(): Int {
            var result1 = result?.hashCode() ?: 0
            result1 = 31 * result1 + user.hashCode()
            result1 = 31 * result1 + isNewUser.hashCode()
            return result1
        }

        override fun toString(): String =
            "AuthState.Success(result=$result, user=$user, isNewUser=$isNewUser)"
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
     */
    class Cancelled internal constructor() : AuthState() {
        override fun equals(other: Any?): Boolean = other is Cancelled
        override fun hashCode(): Int = javaClass.hashCode()
        override fun toString(): String = "AuthState.Cancelled"
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
     * The user needs to sign in with a different provider.
     *
     * Emitted when a user tries to sign up with an email that already exists
     * and needs to use the existing provider to sign in instead.
     *
     * @property provider The [AuthProvider] the user should sign in with
     * @property email The email address of the existing account
     */
    class RequiresSignIn(
        val provider: AuthProvider,
        val email: String
    ) : AuthState() {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is RequiresSignIn) return false
            return provider == other.provider &&
                    email == other.email
        }

        override fun hashCode(): Int {
            var result = provider.hashCode()
            result = 31 * result + email.hashCode()
            return result
        }

        override fun toString(): String =
            "AuthState.RequiresSignIn(provider=$provider, email=$email)"
    }

    /**
     * Pending credential for an anonymous upgrade merge conflict.
     *
     * Emitted when an anonymous user attempts to convert to a permanent account but
     * Firebase detects that the target email already belongs to another user. The UI can
     * prompt the user to resolve the conflict by signing in with the existing account and
     * later linking the stored [pendingCredential].
     */
    class MergeConflict(
        val pendingCredential: AuthCredential
    ) : AuthState() {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is MergeConflict) return false
            return pendingCredential == other.pendingCredential
        }

        override fun hashCode(): Int {
            var result = pendingCredential.hashCode()
            result = 31 * result + pendingCredential.hashCode()
            return result
        }

        override fun toString(): String =
            "AuthState.MergeConflict(pendingCredential=$pendingCredential)"
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
    }
}
