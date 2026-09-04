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

package com.firebase.ui.auth.ui.screens.reauth

import com.firebase.ui.auth.configuration.auth_provider.AuthProvider
import com.firebase.ui.auth.ui.screens.FirebaseAuthScreen
import com.google.firebase.auth.FirebaseUser

/**
 * State class containing all the necessary information to render a custom UI for the
 * reauthentication flow triggered by a sensitive operation (account deletion, password change,
 * email change).
 *
 * This class is passed to the `reauthContent` slot of [FirebaseAuthScreen]. The caller renders a
 * provider chooser; the library owns the credential exchange. [AuthProvider.Email] and
 * [AuthProvider.Phone] hand off to the library's own sub-flow, which replaces this slot while
 * active, so keep the slot stateless. On success the library resumes the pending operation.
 *
 * Render the slot so it blocks interaction with the content behind it (a dialog or modal sheet):
 * that content stays composed, and the library only makes its own affordances inert.
 *
 * ```kotlin
 * FirebaseAuthScreen(
 *     configuration = configuration,
 *     onSignInSuccess = { },
 *     onSignInFailure = { },
 *     onSignInCancelled = { },
 *     reauthContent = { state ->
 *         AlertDialog(
 *             onDismissRequest = state.onDismiss,
 *             title = { Text(state.reason ?: "Verify your identity") },
 *             text = {
 *                 Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
 *                     state.error?.let { Text(it) }
 *                     if (state.isLoading) CircularProgressIndicator()
 *                     state.providers.forEach { provider ->
 *                         Button(
 *                             onClick = { state.onProviderSelected(provider) },
 *                             enabled = !state.isLoading,
 *                         ) { Text("Continue with ${provider.providerName}") }
 *                     }
 *                 }
 *             },
 *             confirmButton = {},
 *             dismissButton = { TextButton(onClick = state.onDismiss) { Text("Cancel") } },
 *         )
 *     },
 * )
 * ```
 *
 * @property user The [FirebaseUser] that needs to reauthenticate.
 * @property reason An optional human-readable reason to show the user, as supplied by the caller of the sensitive operation. Will be `null` when no reason was given.
 * @property providers The providers the user may reauthenticate with, already filtered by the library to those both configured and linked to [user].
 * @property onProviderSelected Callback invoked with the provider the user chose. Receives the selected [AuthProvider]; the library owns what happens next.
 * @property isLoading `true` while a credential attempt, or the sensitive operation it unblocked, is in progress. Use this to show loading indicators and disable the provider buttons. The library's own loading dialog is suppressed while this slot is shown.
 * @property error A localized error message for the last failed attempt, or `null` if it did not fail. Persists until the next credential attempt starts, so it can be rendered inline. Backing out of an attempt is not a failure and leaves this unchanged. Survives Activity recreation.
 * @property onDismiss Callback to abandon reauthentication and drop the pending operation. This is the only way to abandon it — backing out of a single provider attempt returns to this slot with the operation still pending.
 * @property exception The exception behind [error], or `null` if the last attempt did not fail.
 * Branch on its type when a message alone is not enough. Survives Activity recreation with the
 * active reauthentication request.
 *
 * @since 10.0.0
 */
data class ReauthContentState(
    /** The [FirebaseUser] that needs to reauthenticate. */
    val user: FirebaseUser,

    /** Optional human-readable reason to show the user. `null` when none was given. */
    val reason: String? = null,

    /** Configured providers linked to [user]. Already filtered by the library. */
    val providers: List<AuthProvider> = emptyList(),

    /** Callback invoked with the provider the user chose. The library owns the credential path. */
    val onProviderSelected: (AuthProvider) -> Unit = {},

    /** `true` while a credential attempt, or the operation it unblocked, is in progress. */
    val isLoading: Boolean = false,

    /** Localized error message for the last failed attempt. `null` if it did not fail. */
    val error: String? = null,

    /** Callback to abandon reauthentication and drop the pending operation. */
    val onDismiss: () -> Unit = {},

    /** The exception behind [error], if the last attempt failed. */
    val exception: Exception? = null,
)
