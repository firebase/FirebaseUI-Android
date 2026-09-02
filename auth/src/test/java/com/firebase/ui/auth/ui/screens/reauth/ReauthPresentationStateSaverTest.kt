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
 * The reauthentication marker is the only thing that survives Activity recreation for a request
 * whose retry callback is process-local, so what it round-trips has to be exact.
 *
 * The saver maps each presentable sub-route to an explicit id rather than to anything derived from
 * the type, so restoring a marker cannot depend on the order candidates are tested in. These tests
 * pin that mapping.
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

    /**
     * Every sub-route the presentation can be restored into. Written out per value rather than
     * looped, because the point is that each one comes back as *itself*.
     */
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
     * A flow and its start step are distinct values, and only the flow — the presentable sub-route
     * — carries an id, so a step can never be restored in place of the flow it belongs to.
     *
     * The explicit ids are independent of class names, so renaming a destination cannot change
     * what an already-saved marker restores to.
     */
    @Test
    fun `a flow and its start step are distinct, and only the flow has an id`() {
        assertThat(AuthRoute.Email as Any).isNotEqualTo(AuthRoute.Email.SignIn() as Any)
        assertThat(AuthRoute.Phone as Any).isNotEqualTo(AuthRoute.Phone.EnterPhoneNumber as Any)

        assertThat(AuthRoute.Email.reauthSubRouteId())
            .isNotEqualTo(AuthRoute.Phone.reauthSubRouteId())
        // The step is not a presentable sub-route at all, so it has no id and cannot be mistaken
        // for its flow on the way back in.
        assertThat(AuthRoute.Email.SignIn().reauthSubRouteId()).isNull()
        assertThat(AuthRoute.Phone.EnterPhoneNumber.reauthSubRouteId()).isNull()
    }

    /**
     * The MFA challenge is deliberately not restorable: its resolver lives only in the
     * process-local `AuthState`, so the sub-route is derived from that state on the way back in.
     * Saving it would restore a challenge screen with nothing to challenge.
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
