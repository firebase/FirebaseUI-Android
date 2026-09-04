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
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.CompletableDeferred
import java.util.UUID

/**
 * An outstanding request standing in for one a caller is waiting on, running [operation] if and when the
 * screen resolves it with a retry.
 *
 * The resolver is completed from the screen's own effect, and `invokeOnCompletion` runs on the
 * completing thread, so [operation] lands exactly where the retry used to — which is what lets a
 * test keep asserting on a flag it sets.
 */
internal fun retryingReauth(
    user: FirebaseUser,
    reason: String? = null,
    operation: () -> Unit,
): AuthState.Reauthentication.Required {
    val resolver = CompletableDeferred<Boolean>()
    resolver.invokeOnCompletion { cause ->
        if (cause == null && resolver.getCompleted()) operation()
    }
    return raisedReauth(user, reason, resolver)
}

/** An outstanding request with a caller waiting on [resolver], for asserting the decision itself. */
internal fun raisedReauth(
    user: FirebaseUser,
    reason: String? = null,
    resolver: CompletableDeferred<Boolean>? = null,
): AuthState.Reauthentication.Required =
    AuthState.Reauthentication.Required(
        AuthState.Reauthentication.Request(
            requestId = UUID.randomUUID().toString(),
            user = user,
            reason = reason,
            resolver = resolver,
        )
    )

/**
 * An outstanding request whose caller is already gone — the shape a recreation leaves behind when the
 * scope that launched the operation did not survive it.
 */
internal fun abandonedReauth(user: FirebaseUser): AuthState.Reauthentication.Required {
    val resolver = CompletableDeferred<Boolean>()
    resolver.cancel()
    return raisedReauth(user, resolver = resolver)
}

/**
 * Captures the flow a reauthentication surface composes its content in.
 *
 * The request's own [AuthFlowScope] is what provider code emits into during a credential exchange,
 * and every content slot — `reauthContent`, `emailContent`, `phoneContent` — is composed inside it.
 * So a test standing in for provider code emits here, exactly where the real thing would, instead
 * of writing the process-wide state channel and relying on the host to work out whose state it was.
 */
internal class ReauthScopeProbe {
    var scope: AuthFlowScope? = null
        private set

    /** Call from inside a content slot. */
    @Composable
    fun capture() {
        scope = LocalAuthFlowScope.current
    }

    /** Emits [state] as the exchange's provider code would. */
    fun emit(state: AuthState) {
        requireNotNull(scope) { "No reauthentication surface has been composed yet" }.emit(state)
    }
}
