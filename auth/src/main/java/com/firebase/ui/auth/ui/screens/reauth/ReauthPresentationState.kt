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

import androidx.compose.runtime.saveable.Saver
import com.firebase.ui.auth.ui.screens.AuthRoute

internal data class ReauthPresentationState(
    val requestId: String,
    val userUid: String,
    val subRoute: AuthRoute? = null,
)

/**
 * Explicit ids for the sub-routes this marker round-trips. Independent of [AuthRoute.route], so
 * renaming a destination cannot change what an already-saved marker restores to.
 */
private const val SUB_ROUTE_EMAIL = "email"
private const val SUB_ROUTE_PHONE = "phone"

/**
 * The id to save for a sub-route, or null for one that must not be restored.
 *
 * [AuthRoute.MfaChallenge] is absent: its resolver lives only in the process-local
 * [com.firebase.ui.auth.AuthState], so that sub-route is derived on restore rather than saved.
 */
internal fun AuthRoute?.reauthSubRouteId(): String? = when (this) {
    AuthRoute.Email -> SUB_ROUTE_EMAIL
    AuthRoute.Phone -> SUB_ROUTE_PHONE
    else -> null
}

internal fun reauthSubRouteForId(id: String?): AuthRoute? = when (id) {
    SUB_ROUTE_EMAIL -> AuthRoute.Email
    SUB_ROUTE_PHONE -> AuthRoute.Phone
    else -> null
}

// Only the marker and presentation route are saveable; the rest of the request is process-local.
internal val ReauthPresentationStateSaver: Saver<ReauthPresentationState?, List<String?>> = Saver(
    save = { state ->
        state?.let { listOf(it.requestId, it.userUid, it.subRoute.reauthSubRouteId()) }
    },
    restore = { saved ->
        ReauthPresentationState(
            requestId = requireNotNull(saved[0]),
            userUid = requireNotNull(saved[1]),
            subRoute = reauthSubRouteForId(saved.getOrNull(2)),
        )
    },
)
