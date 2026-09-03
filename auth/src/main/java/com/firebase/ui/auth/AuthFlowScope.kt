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

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import com.firebase.ui.auth.configuration.AuthUIConfiguration
import com.firebase.ui.auth.configuration.auth_provider.AuthProvider
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser

/** Where one auth flow's states go. */
internal fun interface AuthStateSink {
    fun emit(state: AuthState)
}

/**
 * One auth flow's collaborators, and where its states go.
 *
 * Provider code is written against this rather than against [FirebaseAuthUI], which is what makes
 * "provider implementations do not write to the process-wide state channel" a rule the compiler
 * holds instead of one a reviewer has to hold across ninety-odd hand edits: there is no way to
 * reach `_authStateFlow` from here. Two sinks exist — the host's, which writes the public flow, and
 * a reauthentication request's, which writes its own phase and nothing else.
 *
 * It also carries [config], which used to be an explicit parameter on nearly every provider
 * function, so those signatures got shorter rather than longer.
 *
 * @since 10.0.0
 */
internal class AuthFlowScope(
    val auth: FirebaseAuth,
    val config: AuthUIConfiguration,
    val credentialManagerProvider: AuthProvider.Google.CredentialManagerProvider? = null,
    val loginManagerProvider: AuthProvider.Facebook.LoginManagerProvider? = null,
    /**
     * What this flow is currently doing, for the screens rendering it.
     *
     * The read side of [sink], and the reason a reauthentication phase no longer has to be
     * published to the public channel for the sub-screens to see it: under a request's scope this
     * *is* the phase, so `EmailAuthScreen` and `PhoneAuthScreen` read their spinner and their
     * inline error from the conversation they are actually part of.
     */
    val state: State<AuthState>,
    private val sink: AuthStateSink,
) {
    fun emit(state: AuthState) = sink.emit(state)

    /**
     * Publishes what [result] means for this flow: a password user who still owes email
     * verification is not signed in yet, however successful the credential exchange was.
     *
     * Moved off [FirebaseAuthUI] with the rest of provider publishing. The decision itself is
     * [authUserState], which the host also needs when it observes FirebaseAuth directly.
     */
    fun emitResult(result: AuthResult?, defaultIsNewUser: Boolean = false) {
        val user = result?.user
        if (user != null) {
            val isNewUser = result.additionalUserInfo?.isNewUser ?: defaultIsNewUser
            emit(authUserState(user, result, isNewUser))
        } else {
            emit(AuthState.Idle)
        }
    }
}

/**
 * What a signed-in [user] means as an [AuthState]: the single source of truth for whether they
 * still owe email verification. Callers must not re-derive it — only password users with an email
 * can satisfy that screen.
 *
 * Top-level rather than a member of either [AuthFlowScope] or [FirebaseAuthUI], because both need
 * it: provider code reaches it through [AuthFlowScope.emitResult], and [FirebaseAuthUI] calls it
 * from the `callbackFlow` that observes FirebaseAuth directly, which has no flow and therefore no
 * scope. Its body reads only its three parameters.
 */
internal fun authUserState(user: FirebaseUser, result: AuthResult?, isNewUser: Boolean): AuthState =
    if (!user.isEmailVerified &&
        user.email != null &&
        user.providerData.any { it.providerId == "password" }
    ) {
        AuthState.RequiresEmailVerification(user = user, email = user.email!!)
    } else {
        AuthState.Success(result = result, user = user, isNewUser = isNewUser)
    }

/**
 * The auth flow the current composition belongs to, or null outside one.
 *
 * Ambient rather than a parameter because the sub-screens that need it — `EmailAuthScreen`,
 * `PhoneAuthScreen` — are public composables, and "which conversation am I part of" is a property
 * of where they are composed, not of what their caller knows to pass. `FirebaseAuthScreen`
 * provides the host's flow; `reauthDestinations` provides the request's, so a credential exchange's
 * states go to that request and are never seen by anything collecting the public flow.
 */
internal val LocalAuthFlowScope = staticCompositionLocalOf<AuthFlowScope?> { null }

/**
 * The flow this composition belongs to: the ambient one when composed inside a flow that provides
 * it, and otherwise a fresh one over the host's public state channel — which is what a consumer
 * composing `EmailAuthScreen` or `PhoneAuthScreen` on its own gets.
 */
@Composable
internal fun rememberAuthFlowScope(
    authUI: FirebaseAuthUI,
    configuration: AuthUIConfiguration,
): AuthFlowScope {
    val ambient = LocalAuthFlowScope.current
    val hostState = remember(authUI) { authUI.authStateFlow() }
        .collectAsState(AuthState.Idle)
    return remember(ambient, authUI, configuration, hostState) {
        ambient ?: hostAuthFlowScope(authUI, configuration, hostState)
    }
}

/** An [AuthFlowScope] over [authUI]'s public flow, in both directions. */
internal fun hostAuthFlowScope(
    authUI: FirebaseAuthUI,
    configuration: AuthUIConfiguration,
    state: State<AuthState>,
): AuthFlowScope = AuthFlowScope(
    auth = authUI.auth,
    config = configuration,
    credentialManagerProvider = authUI.testCredentialManagerProvider,
    loginManagerProvider = authUI.testLoginManagerProvider,
    state = state,
    sink = { authUI.updateAuthState(it) },
)
