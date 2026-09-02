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

import androidx.compose.runtime.saveable.SaverScope
import com.firebase.ui.auth.ui.screens.AuthRoute
import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * The reauthentication marker is all that survives Activity recreation for a request whose retry
 * callback is process-local, so what it round-trips has to be exact.
 *
 * [AuthRoute.route] is not an identity: a flow entry point shares its start step's route string.
 * These tests pin that the saver uses explicit ids instead, and pin the collision itself.
 *
 * @suppress Internal test class
 */
class ReauthPresentationStateSaverTest {

    private val scope = SaverScope { true }

    @Test
    fun `the marker round-trips with no sub-route`() {
        val restored = roundTrip(
            ReauthPresentationState(requestId = "request-1", userUid = "uid-1")
        )

        assertThat(restored).isEqualTo(
            ReauthPresentationState(requestId = "request-1", userUid = "uid-1", subRoute = null)
        )
    }

    /** Every sub-route the presentation can be restored into comes back as itself. */
    @Test
    fun `each presentable sub-route round-trips as itself`() {
        listOf(AuthRoute.Email, AuthRoute.Phone).forEach { subRoute ->
            val restored = roundTrip(
                ReauthPresentationState(
                    requestId = "request-1",
                    userUid = "uid-1",
                    subRoute = subRoute,
                )
            )

            assertThat(restored?.subRoute).isSameInstanceAs(subRoute)
            assertThat(restored?.requestId).isEqualTo("request-1")
            assertThat(restored?.userUid).isEqualTo("uid-1")
        }
    }

    /**
     * The collision the ids exist to survive: a flow and its start step report the same [route],
     * so a saver keyed on that string cannot tell the two apart.
     */
    @Test
    fun `a flow and its start step share a route string but not an id`() {
        assertThat(AuthRoute.Email.route).isEqualTo(AuthRoute.Email.SignIn.route)
        assertThat(AuthRoute.Phone.route).isEqualTo(AuthRoute.Phone.EnterPhoneNumber.route)

        assertThat(AuthRoute.Email.reauthSubRouteId())
            .isNotEqualTo(AuthRoute.Phone.reauthSubRouteId())
        // A step is not a presentable sub-route at all, so it has no id.
        assertThat(AuthRoute.Email.SignIn.reauthSubRouteId()).isNull()
        assertThat(AuthRoute.Phone.EnterPhoneNumber.reauthSubRouteId()).isNull()
    }

    /**
     * The MFA challenge is not restorable: its resolver lives only in the process-local
     * `AuthState`, so saving it would restore a challenge screen with nothing to challenge.
     */
    @Test
    fun `the mfa challenge sub-route is not restored`() {
        assertThat(AuthRoute.MfaChallenge.reauthSubRouteId()).isNull()

        val restored = roundTrip(
            ReauthPresentationState(
                requestId = "request-1",
                userUid = "uid-1",
                subRoute = AuthRoute.MfaChallenge,
            )
        )

        assertThat(restored?.subRoute).isNull()
        // The request itself still survives — only the sub-route is dropped.
        assertThat(restored?.requestId).isEqualTo("request-1")
    }

    /** An unknown id restores as no sub-route rather than throwing on a stale saved bundle. */
    @Test
    fun `an unrecognised sub-route id restores as none`() {
        assertThat(reauthSubRouteForId("something_else")).isNull()
        assertThat(reauthSubRouteForId(null)).isNull()
    }

    @Test
    fun `a null marker round-trips as null`() {
        with(ReauthPresentationStateSaver) {
            assertThat(scope.save(null)).isNull()
        }
    }

    private fun roundTrip(state: ReauthPresentationState): ReauthPresentationState? =
        with(ReauthPresentationStateSaver) {
            val saved = requireNotNull(scope.save(state))
            restore(saved)
        }
}
