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

package com.firebase.ui.auth.ui

/**
 * Stable Compose test tags applied by the FirebaseUI Auth screens.
 *
 * These values are **public API**. Host applications reference them from their own UI tests, and
 * they are intended to be surfaced as Android resource ids so that Firebase Test Lab Robo
 * directives and Play pre-launch reports can target the auth surfaces. Renaming or removing a
 * constant — or changing the string a constant resolves to — is a **breaking change**, not an
 * internal refactor.
 *
 * Constants are grouped by the screen or surface that owns them, and every value repeats that
 * surface as a segment so a `By.res` prefix match selects exactly one surface. Within a group the
 * element type is the **last** token of both the constant name and the value, so sibling nodes sort
 * and complete together.
 *
 * Values are lowercase `snake_case` with a `fui_` prefix. The prefix is not decoration: once these
 * tags are exposed as resource ids they land in the host application's `id` namespace, so it
 * namespaces our tags away from the host app's own resource ids and keeps `By.res()` lookups
 * unambiguous.
 *
 * ## Usage Example:
 *
 * ```kotlin
 * composeTestRule
 *     .onNodeWithTag(FirebaseAuthTestTags.MethodPicker.PROVIDER_LIST)
 *     .performScrollToNode(hasText("Sign in with Google"))
 * ```
 *
 * @since 10.0.0
 */
object FirebaseAuthTestTags {

    /** Tags on the auth method picker screen. */
    object MethodPicker {

        /** The scrollable list of provider buttons. */
        const val PROVIDER_LIST = "fui_method_picker_provider_list"

        /**
         * The "Continue as ..." button, shown when a previous sign-in preference is available.
         */
        const val CONTINUE_AS_BUTTON = "fui_method_picker_continue_as_button"
    }

    /** Tags on the phone number country selector bottom sheet. */
    object CountrySelector {

        /** The scrollable country list. */
        const val COUNTRY_LIST = "fui_country_selector_country_list"
    }
}
