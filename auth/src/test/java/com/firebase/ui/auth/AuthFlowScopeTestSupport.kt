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

import com.firebase.ui.auth.configuration.AuthUIConfiguration

/**
 * This instance's own flow, for driving provider code from a test.
 *
 * Deliberately the *host* sink, so `authStateFlow()` still carries what provider code publishes and
 * every assertion written against it keeps the meaning it had before provider code moved off the
 * [FirebaseAuthUI] receiver. Converting those assertions to a recording sink instead would have
 * quietly dropped the ones that assert [FirebaseAuthUI]'s own combine and staleness behaviour
 * rather than a provider's — and they would still have passed.
 *
 * Use [recordingScope] where the point is isolation: that a state does *not* reach the host flow.
 */
internal fun FirebaseAuthUI.flowScope(config: AuthUIConfiguration): AuthFlowScope =
    hostAuthFlowScope(this, config)

/** A scope whose states are collected in [recorded] and go nowhere else. */
internal fun FirebaseAuthUI.recordingScope(
    config: AuthUIConfiguration,
    recorded: MutableList<AuthState>,
): AuthFlowScope = AuthFlowScope(
    auth = auth,
    config = config,
    credentialManagerProvider = testCredentialManagerProvider,
    loginManagerProvider = testLoginManagerProvider,
    sink = { recorded += it },
)
