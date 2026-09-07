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

package com.firebase.ui.auth.ui.screens.email

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import kotlinx.serialization.Serializable

/**
 * One email mode, as a caller's own back-stack key — the same shape the slot demos define.
 *
 * The library's own keys are internal, so a host outside
 * [com.firebase.ui.auth.ui.screens.FirebaseAuthScreen] builds its own from the public
 * [EmailAuthMode] and the address the screen hands back.
 *
 * @suppress Internal test support
 */
@Serializable
internal data class EmailModeKey(val mode: EmailAuthMode, val email: String = "") : NavKey

/**
 * Moves to [mode], carrying the address so a switch keeps what the user typed.
 *
 * Adds before trimming, so no single write empties the stack: a mode already on the stack is
 * replaced by the fresh key rather than revisited with a stale address; one that is not is pushed,
 * leaving the mode below reachable by back.
 */
internal fun NavBackStack<NavKey>.goToEmailMode(mode: EmailAuthMode, email: String) {
    val existing = indexOfFirst { it is EmailModeKey && it.mode == mode }
    add(EmailModeKey(mode, email))
    if (existing >= 0) {
        while (size > existing + 1) removeAt(existing)
    }
}

/**
 * Hosts an email flow the way a caller outside `FirebaseAuthScreen` has to: every mode is a real
 * destination on the host's own stack, so a switch is navigation and system back pops one mode.
 *
 * Transitions are off. Two entries composed at once during a crossfade would put two copies of the
 * form on screen, which is about the animation rather than anything a test here asserts.
 *
 * @suppress Internal test support
 */
@Composable
internal fun EmailModeBackStackHost(
    startMode: EmailAuthMode,
    startEmail: String? = null,
    content: @Composable (key: EmailModeKey, goToMode: (EmailAuthMode, String) -> Unit) -> Unit,
) {
    val backStack = rememberNavBackStack(EmailModeKey(startMode, startEmail.orEmpty()))
    NavDisplay(
        backStack = backStack,
        onBack = { backStack.removeLastOrNull() },
        transitionSpec = { EnterTransition.None togetherWith ExitTransition.None },
        popTransitionSpec = { EnterTransition.None togetherWith ExitTransition.None },
        predictivePopTransitionSpec = { EnterTransition.None togetherWith ExitTransition.None },
        entryProvider = entryProvider {
            entry<EmailModeKey> { key ->
                content(key) { mode, email -> backStack.goToEmailMode(mode, email) }
            }
        },
    )
}
