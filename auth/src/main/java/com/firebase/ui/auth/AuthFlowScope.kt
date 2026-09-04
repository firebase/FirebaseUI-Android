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
 * One auth flow's collaborators, and where its states go. Provider code is written against this,
 * not [FirebaseAuthUI], so it reaches the public state channel only through [sink].
 *
 * @since 10.0.0
 */
internal class AuthFlowScope(
    val auth: FirebaseAuth,
    val config: AuthUIConfiguration,
    val credentialManagerProvider: AuthProvider.Google.CredentialManagerProvider? = null,
    val loginManagerProvider: AuthProvider.Facebook.LoginManagerProvider? = null,
    /**
     * What this flow is currently doing, for the screens rendering it. Under a reauthentication
     * request's scope this is that request's phase rather than the host's state.
     */
    val state: State<AuthState>,
    private val sink: AuthStateSink,
) {
    fun emit(state: AuthState) = sink.emit(state)

    /**
     * Publishes what [result] means for this flow: a password user who still owes email
     * verification is not signed in yet, however successful the credential exchange was.
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
 */
internal fun authUserState(user: FirebaseUser, result: AuthResult?, isNewUser: Boolean): AuthState {
    val email = user.email
    return if (!user.isEmailVerified &&
        email != null &&
        user.providerData.any { it.providerId == "password" }
    ) {
        AuthState.RequiresEmailVerification(user = user, email = email)
    } else {
        AuthState.Success(result = result, user = user, isNewUser = isNewUser)
    }
}

/** The auth flow the current composition belongs to, or null outside one. */
internal val LocalAuthFlowScope = staticCompositionLocalOf<AuthFlowScope?> { null }

/**
 * The ambient flow when composed inside one, otherwise a fresh flow over [authUI]'s public state —
 * which is what a consumer composing `EmailAuthScreen` or `PhoneAuthScreen` on its own gets.
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
