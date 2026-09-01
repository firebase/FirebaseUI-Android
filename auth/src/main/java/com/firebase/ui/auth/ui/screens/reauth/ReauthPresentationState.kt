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

// The process-local request and retry callback live in AuthState.Reauthentication. Only the marker
// and presentation route needed for Activity/process restoration are saveable here.
internal val ReauthPresentationStateSaver: Saver<ReauthPresentationState?, List<String?>> = Saver(
    save = { state ->
        state?.let { listOf(it.requestId, it.userUid, it.subRoute?.route) }
    },
    restore = { saved ->
        ReauthPresentationState(
            requestId = requireNotNull(saved[0]),
            userUid = requireNotNull(saved[1]),
            subRoute = when (saved.getOrNull(2)) {
                AuthRoute.Email.route -> AuthRoute.Email
                AuthRoute.Phone.route -> AuthRoute.Phone
                else -> null
            },
        )
    },
)
