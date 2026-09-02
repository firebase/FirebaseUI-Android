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

package com.firebase.ui.auth.configuration

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ContentTransform
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.scene.Scene

/**
 * Container for screen transition animations used in Firebase Auth UI.
 *
 * Each spec is an [AnimatedContentTransitionScope] receiver on [Scene]`<`[NavKey]`>` returning a
 * single [ContentTransform], pairing the enter and exit halves with `togetherWith`. To vary the
 * animation per destination, read [com.firebase.ui.auth.ui.screens.authRoute] off `initialState`
 * / `targetState`.
 *
 * @property transitionSpec Forward navigation.
 * @property popTransitionSpec Back navigation.
 * @property predictivePopTransitionSpec Predictive-back gesture. Its `Int` is the swipe edge:
 * [androidx.navigationevent.NavigationEvent.EDGE_LEFT] (`0`),
 * [androidx.navigationevent.NavigationEvent.EDGE_RIGHT] (`1`) or
 * [androidx.navigationevent.NavigationEvent.EDGE_NONE] (`2`) for a back from no edge at all. Left
 * null it falls back to the library's default cross-fade, not to [popTransitionSpec]. It runs when
 * the gesture **starts**, not when a back navigation completes, so any side effect placed in it
 * (analytics, logging a screen change) also fires for gestures the user goes on to cancel.
 *
 * @since 10.0.0
 */
data class AuthUITransitions(
    val transitionSpec:
    (AnimatedContentTransitionScope<Scene<NavKey>>.() -> ContentTransform)? = null,
    val popTransitionSpec:
    (AnimatedContentTransitionScope<Scene<NavKey>>.() -> ContentTransform)? = null,
    val predictivePopTransitionSpec:
    (AnimatedContentTransitionScope<Scene<NavKey>>.(Int) -> ContentTransform)? = null,
)
