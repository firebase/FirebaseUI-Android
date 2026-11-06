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

package com.firebase.ui.auth.configuration.auth_provider

import com.firebase.ui.auth.AuthException
import com.firebase.ui.auth.AuthFlowScope
import com.google.firebase.auth.AuthResult
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.tasks.await

/**
 * Finishes an account-linking recovery: links [AuthFlowScope.pendingAccountLink] to the account
 * [result] just signed in to, before the flow reports success.
 *
 * Linking happens here, inside the provider's sign-in, rather than once success is observed: by
 * then the host has already been handed the unlinked result, and a failed link has nowhere to go.
 *
 * Either the sign-in and the link both stand, or neither does:
 * - The signed-in account must have the address the collision named. Credential Manager and the
 *   OAuth flows let the user pick any account, and linking to a different one would attach the
 *   credential to someone else. A mismatch signs back out and raises the account-linking error
 *   again, with the credential still pending, so the user can pick the right account.
 * - A link that fails signs back out and raises the failure, so the host is not left signed in to
 *   an account that silently lacks the credential it was asked to link.
 *
 * @return The link's result, or [result] unchanged when nothing is pending
 */
internal suspend fun AuthFlowScope.linkPendingAccountCredential(result: AuthResult): AuthResult {
    val pending = pendingAccountLink.value ?: return result
    if (config.isReauthenticationMode) return result
    val user = result.user ?: return result

    val expectedEmail = pending.email
    if (expectedEmail != null && !expectedEmail.equals(user.email, ignoreCase = true)) {
        auth.signOut()
        throw AuthException.AccountLinkingRequiredException(
            message = config.stringProvider.errorUserMismatch,
            email = expectedEmail,
            credential = pending.credential,
        )
    }

    val linked = try {
        user.linkWithCredential(pending.credential).await()
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        pendingAccountLink.value = null
        auth.signOut()
        throw when (val mapped = AuthException.from(e, config.stringProvider)) {
            // Firebase reports some link failures as a collision, but there is no further account
            // to recover to: offering the linking recovery again would loop.
            is AuthException.AccountLinkingRequiredException -> AuthException.UnknownException(
                message = config.stringProvider.unknownErrorRecoveryMessage,
                cause = e,
            )

            else -> mapped
        }
    }
    pendingAccountLink.value = null
    return linked
}

/**
 * Whether [e], raised by a provider's own sign-in, is [linkPendingAccountCredential] rejecting the
 * account the user picked rather than that provider's credential colliding. Only a collision of
 * the provider's own credential is worth persisting for a later email-link sign-in.
 */
internal fun AuthFlowScope.isPendingLinkMismatch(e: AuthException.AccountLinkingRequiredException) =
    e.credential != null && e.credential === pendingAccountLink.value?.credential
