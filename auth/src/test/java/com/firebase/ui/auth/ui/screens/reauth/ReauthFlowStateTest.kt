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

import androidx.compose.runtime.mutableStateOf
import com.firebase.ui.auth.AuthException
import com.firebase.ui.auth.AuthState
import com.google.common.truth.Truth.assertThat
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.CompletableDeferred
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import java.util.UUID

/**
 * The phase machine on its own, which is what `FirebaseAuthUI.contextualizeReauthenticationState`
 * and `updateReauthentication` used to be. Nothing here needs a composition or a flow: the holder
 * reads the phase it owns rather than read-modify-writing the flow it is written to.
 */
class ReauthFlowStateTest {

    private val user: FirebaseUser = mock(FirebaseUser::class.java).also {
        `when`(it.uid).thenReturn("uid-reauth")
    }

    private fun holder() = ReauthFlowState(mutableStateOf(null))

    private fun request(resolver: CompletableDeferred<Boolean>? = null) =
        AuthState.Reauthentication.Request(
            requestId = UUID.randomUUID().toString(),
            user = user,
            reason = null,
            resolver = resolver,
        )

    private fun ReauthFlowState.accepted(
        resolver: CompletableDeferred<Boolean>? = null,
    ): AuthState.Reauthentication.Request {
        val request = request(resolver)
        accept(AuthState.Reauthentication.Required(request))
        return request
    }

    /**
     * The counter `addReauthenticationDrainer` kept is gone because this is the same question:
     * with no request outstanding there is no conversation for a provider state to belong to, so it stays
     * the host's.
     */
    @Test
    fun `nothing is folded while nothing is outstanding`() {
        val holder = holder()

        assertThat(holder.fold(AuthState.Loading("Signing in"))).isNull()
        assertThat(holder.phase).isNull()
    }

    @Test
    fun `provider states are folded onto the same request`() {
        val holder = holder()
        val request = holder.accepted()

        val authenticating = holder.fold(AuthState.Loading("Signing in"))
        assertThat(authenticating)
            .isInstanceOf(AuthState.Reauthentication.Authenticating::class.java)
        assertThat(authenticating!!.requestId).isEqualTo(request.requestId)

        val failed = holder.fold(AuthState.Error(IllegalArgumentException("wrong password")))
        assertThat(failed).isInstanceOf(AuthState.Reauthentication.AttemptFailed::class.java)
        assertThat(failed!!.requestId).isEqualTo(request.requestId)

        val resumed = holder.fold(AuthState.Cancelled)
        assertThat(resumed).isInstanceOf(AuthState.Reauthentication.Required::class.java)
        assertThat(resumed!!.requestId).isEqualTo(request.requestId)
    }

    @Test
    fun `email notifications keep the request until they are consumed`() {
        val holder = holder()
        val request = holder.accepted()

        val notification = holder.fold(AuthState.PasswordResetLinkSent())
        assertThat(notification)
            .isInstanceOf(AuthState.Reauthentication.PasswordResetLinkSent::class.java)
        assertThat(notification!!.requestId).isEqualTo(request.requestId)

        holder.update(request.requestId) { it.returnedToProviderSelection() }

        assertThat(holder.phase).isInstanceOf(AuthState.Reauthentication.Required::class.java)
        assertThat(holder.phase!!.requestId).isEqualTo(request.requestId)
    }

    @Test
    fun `update ignores a stale requestId`() {
        val holder = holder()
        val request = holder.accepted()

        holder.update("stale-request-id") { it.attemptStarted() }

        assertThat(holder.phase).isInstanceOf(AuthState.Reauthentication.Required::class.java)
        assertThat(holder.phase!!.requestId).isEqualTo(request.requestId)
    }

    @Test
    fun `attemptCancelled does not rewind a surfaced attempt failure`() {
        val holder = holder()
        val request = holder.accepted()
        holder.fold(AuthState.Error(IllegalArgumentException("wrong password")))

        holder.update(request.requestId) { it.attemptCancelled() }

        assertThat(holder.phase).isInstanceOf(AuthState.Reauthentication.AttemptFailed::class.java)
    }

    /**
     * The phone sub-flow's "Change number" returns to provider selection, and by then a wrong SMS
     * code has latched a failure. Clearing it there would erase the only report the user gets.
     */
    @Test
    fun `returnedToProviderSelection does not wipe a surfaced attempt failure`() {
        val holder = holder()
        val request = holder.accepted()
        holder.fold(AuthState.Error(IllegalArgumentException("wrong sms code")))

        holder.update(request.requestId) { it.returnedToProviderSelection() }

        assertThat(holder.phase).isInstanceOf(AuthState.Reauthentication.AttemptFailed::class.java)
    }

    /** Credentials were already accepted, so a stray attempt must not rewind the proof. */
    @Test
    fun `attemptStarted does not rewind an accepted proof`() {
        val holder = holder()
        val request = holder.accepted()
        `when`(user.uid).thenReturn("uid-reauth")
        holder.fold(
            AuthState.Success(result = null, user = user, reauthenticatedUid = "uid-reauth")
        )
        assertThat(holder.phase).isInstanceOf(AuthState.Reauthentication.Succeeded::class.java)

        holder.update(request.requestId) { it.attemptStarted() }

        assertThat(holder.phase).isInstanceOf(AuthState.Reauthentication.Succeeded::class.java)
    }

    /** Only a stamped Success proves this user was re-verified. */
    @Test
    fun `an unstamped Success leaves the phase alone`() {
        val holder = holder()
        holder.accepted()

        val folded = holder.fold(AuthState.Success(result = null, user = user))

        assertThat(folded).isInstanceOf(AuthState.Reauthentication.Required::class.java)
        assertThat(holder.phase).isInstanceOf(AuthState.Reauthentication.Required::class.java)
    }

    /**
     * The audit rule: whatever `fold` declines has to be ended by a handler that clears the phase
     * *and* resolves the caller. `Aborted` is the one such state, and `FirebaseAuthScreen`'s own
     * branch is what covers it — so this pins the decline itself, which is the half that a new
     * `AuthState` member would silently inherit.
     */
    @Test
    fun `fold declines Aborted, leaving the phase for the screen to end`() {
        val holder = holder()
        holder.accepted()

        assertThat(holder.fold(AuthState.Aborted)).isNull()
        assertThat(holder.phase).isInstanceOf(AuthState.Reauthentication.Required::class.java)
    }

    /** Arming states are the caller's own signal, never something to fold back onto themselves. */
    @Test
    fun `fold declines a reauthentication state`() {
        val holder = holder()
        val request = holder.accepted()

        assertThat(holder.fold(AuthState.Reauthentication.Required(request))).isNull()
    }

    @Test
    fun `finish resolves the waiting caller with the retry decision`() {
        val holder = holder()
        val resolver = CompletableDeferred<Boolean>()
        holder.accepted(resolver)

        holder.finish(true)

        assertThat(holder.phase).isNull()
        assertThat(resolver.isCompleted).isTrue()
        assertThat(resolver.getCompleted()).isTrue()
    }

    /**
     * A decline resumes the caller by throwing. "You backed out" and "your operation ran" are
     * different outcomes, and a caller that cannot tell them apart has to guess whether its work
     * happened.
     */
    @Test
    fun `finish without a retry fails the caller rather than returning quietly`() {
        val holder = holder()
        val resolver = CompletableDeferred<Boolean>()
        holder.accepted(resolver)

        holder.finish(false)

        assertThat(resolver.isCompleted).isTrue()
        assertThat(resolver.getCompleted()).isFalse()
    }

    /** A standalone flow has no operation behind it, so there is nothing to resolve. */
    @Test
    fun `finish is a no-op for a request with no caller`() {
        val holder = holder()
        holder.accepted()

        holder.finish(true)

        assertThat(holder.phase).isNull()
    }

    @Test
    fun `a resolved request ignores being resolved again`() {
        val resolver = CompletableDeferred<Boolean>()
        val request = request(resolver)

        request.resolve()
        request.decline()

        assertThat(resolver.getCompleted()).isTrue()
    }

    @Test
    fun `a request whose caller was cancelled is not resumable`() {
        val resolver = CompletableDeferred<Boolean>()
        val request = request(resolver)
        assertThat(request.isResumable).isTrue()

        resolver.cancel()

        assertThat(request.isResumable).isFalse()
        // No crash, and nothing to hand back to.
        request.resolve()
    }

    /** No caller means nothing to lose, so a standalone request is always presentable. */
    @Test
    fun `a request with no caller is always resumable`() {
        assertThat(request().isResumable).isTrue()
        assertThat(request().hasPendingOperation).isFalse()
    }

    @Test
    fun `an attempt failure carries the exception it folded`() {
        val holder = holder()
        holder.accepted()
        val cause = AuthException.UnknownException("nope")

        val folded = holder.fold(AuthState.Error(cause))

        assertThat((folded as AuthState.Reauthentication.AttemptFailed).exception).isEqualTo(cause)
    }

    // =============================================================================================
    // Sink isolation
    // =============================================================================================

    /**
     * The point of giving the request its own sink: provider code driving a credential exchange
     * publishes into the phase, and an app collecting `authStateFlow()` never sees a failure that
     * belongs to a conversation it is not part of.
     */
    @Test
    fun `the request's sink keeps the exchange off the host flow`() {
        val holder = holder()
        holder.accepted()
        val host = mutableListOf<AuthState>()
        val sink = holder.sink(hostFallback = { host += it })

        sink.emit(AuthState.Loading("Signing in"))
        sink.emit(AuthState.Error(AuthException.UnknownException("wrong password")))

        assertThat(host).isEmpty()
        assertThat(holder.phase).isInstanceOf(AuthState.Reauthentication.AttemptFailed::class.java)
    }

    /** What the exchange does not own is still the host's, so the sink forwards it rather than eating it. */
    @Test
    fun `the request's sink forwards what the exchange does not own`() {
        val holder = holder()
        holder.accepted()
        val host = mutableListOf<AuthState>()
        val sink = holder.sink(hostFallback = { host += it })

        sink.emit(AuthState.Aborted)

        assertThat(host).hasSize(1)
        assertThat(host.single()).isInstanceOf(AuthState.Aborted::class.java)
    }

    /** With no request outstanding there is no exchange to absorb into, so everything is the host's. */
    @Test
    fun `the sink forwards everything while nothing is outstanding`() {
        val holder = holder()
        val host = mutableListOf<AuthState>()
        val sink = holder.sink(hostFallback = { host += it })

        sink.emit(AuthState.Loading("Signing in"))

        assertThat(host).hasSize(1)
        assertThat(host.single()).isInstanceOf(AuthState.Loading::class.java)
    }

    /** A cancellation is the user backing out of a sub-flow, not a failure to report. */
    @Test
    fun `a cancelled attempt returns to provider selection rather than surfacing`() {
        val holder = holder()
        holder.accepted()

        val folded = holder.fold(
            AuthState.Error(AuthException.AuthCancelledException(message = "cancelled"))
        )

        assertThat(folded).isInstanceOf(AuthState.Reauthentication.Required::class.java)
    }
}
