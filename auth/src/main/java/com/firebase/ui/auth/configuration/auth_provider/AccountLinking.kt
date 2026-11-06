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
import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.AuthResult
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

/**
 * Finishes an account-linking recovery: links [AuthFlowScope.pendingAccountLink] to the account
 * [result] just signed in to, before the flow reports success.
 *
 * Linking happens here, inside the provider's sign-in, rather than once success is observed: by
 * then the host has already been handed the unlinked result, and a failed link has nowhere to go.
 *
 * Only a sign-in through one of the providers the recovery offered can finish it
 * ([com.firebase.ui.auth.PendingAccountLink.ownerProviderIds]). Any other sign-in, such as phone,
 * means the user has gone another way: the pending link is dropped and the sign-in stands as it is.
 *
 * Otherwise the sign-in and the link both stand, or neither does:
 * - The signed-in account must have the address the collision named. Credential Manager and the
 *   OAuth flows let the user pick any account, and linking to a different one would attach the
 *   credential to someone else. A mismatch is undone, and the account-linking error raised again
 *   with the credential still pending, so the user can pick the right account. An account the
 *   sign-in only just created is deleted rather than left behind.
 * - A link that fails, or is cancelled, signs back out, so the host is not left signed in to an
 *   account that silently lacks the credential it was asked to link.
 *
 * @param providerId The provider [result] signed in with
 * @return The link's result, or [result] unchanged when nothing is pending for [providerId]
 */
internal suspend fun AuthFlowScope.linkPendingAccountCredential(
    result: AuthResult,
    providerId: String?,
): AuthResult {
    val pending = pendingAccountLink.value ?: return result
    if (config.isReauthenticationMode) return result
    if (providerId == null || providerId !in pending.ownerProviderIds) {
        pendingAccountLink.value = null
        return result
    }
    val user = result.user ?: return result

    val expectedEmail = pending.email
    if (expectedEmail != null && !expectedEmail.equals(user.email, ignoreCase = true)) {
        if (result.additionalUserInfo?.isNewUser == true) {
            try {
                user.delete().await()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // Signing out below still undoes the sign-in; only the empty account remains.
            }
        }
        auth.signOut()
        throw pendingLinkMismatch()
    }

    val linked = try {
        user.linkWithCredential(pending.credential).await()
    } catch (e: CancellationException) {
        withContext(NonCancellable) { auth.signOut() }
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
 * Refuses an email step about to sign in, or send a sign-in link, to an address other than the
 * one the account-linking recovery is finishing. The step is prefilled with that address but the
 * field stays editable, and the credential must not end up on someone else's account.
 *
 * Applies only to the recovery's own credential: a host passing a credential of its own to the
 * public `EmailAuthScreen` has no recovery address to be held to.
 */
internal fun AuthFlowScope.requirePendingLinkAddress(
    credentialForLinking: AuthCredential?,
    email: String,
) {
    val pending = pendingAccountLink.value ?: return
    if (credentialForLinking == null || credentialForLinking !== pending.credential) return
    val expectedEmail = pending.email ?: return
    if (!expectedEmail.equals(email, ignoreCase = true)) throw pendingLinkMismatch()
}

/** The account-linking error again, for the account the recovery still wants. */
private fun AuthFlowScope.pendingLinkMismatch(): AuthException.AccountLinkingRequiredException {
    val pending = checkNotNull(pendingAccountLink.value)
    return AuthException.AccountLinkingRequiredException(
        message = config.stringProvider.errorUserMismatch,
        email = pending.email,
        credential = pending.credential,
    )
}

/**
 * Whether [e], raised by a provider's own sign-in, is [linkPendingAccountCredential] rejecting the
 * account the user picked rather than that provider's credential colliding. Only a collision of
 * the provider's own credential is worth persisting for a later email-link sign-in.
 */
internal fun AuthFlowScope.isPendingLinkMismatch(e: AuthException.AccountLinkingRequiredException) =
    e.credential != null && e.credential === pendingAccountLink.value?.credential
