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
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.navigation.NavBackStackEntry

/**
 * Container for screen transition animations used in Firebase Auth UI.
 * 
 * @property enterTransition Transition when entering a new screen
 * @property exitTransition Transition when exiting current screen
 * @property popEnterTransition Transition when returning to previous screen (back navigation)
 * @property popExitTransition Transition when exiting during back navigation
 */
data class AuthUITransitions(
    val enterTransition: (AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition)? = null,
    val exitTransition: (AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition)? = null,
    val popEnterTransition: (AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition)? = null,
    val popExitTransition: (AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition)? = null,
)
