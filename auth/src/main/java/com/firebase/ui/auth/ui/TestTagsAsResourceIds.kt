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

import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId

/**
 * Publishes the [FirebaseAuthTestTags] applied beneath this node as Android resource ids, so that
 * they appear as `viewIdResourceName` on the accessibility node.
 *
 * This is what makes the tags addressable from outside a Compose test: Firebase Test Lab Robo
 * directives take resource names, Google Play pre-launch reports drive the same Robo crawler, and
 * UiAutomator's `By.res()` matches on the same field. Without it a tag is visible only to Compose's
 * own test APIs, which is no help to a crawler — the gap that made the 9.x `@id/email` and
 * `@id/password` targets unreachable after the Compose rewrite.
 *
 * Apply it once per **semantics owner**, not once per screen. The flag is read by walking a node's
 * semantics ancestors, and that walk stops at the root of the window the node lives in. Every dialog
 * and bottom sheet Compose shows is hosted in its own window with its own semantics root, so it
 * inherits nothing from the composable that opened it and has to carry the flag itself. Sibling
 * branches within one window are a quieter version of the same trap: a `Scaffold` per step of a
 * wizard inherits nothing from the `Scaffold` of the step beside it.
 *
 * ## The rule: flag the owner, tags or not
 *
 * **Every semantics owner the library creates carries this flag, whether or not anything inside it
 * is tagged today.** Flagging is applied to owners rather than to tags on purpose, and the reason is
 * asymmetry of failure. Flagging an owner that holds no tags costs nothing — the property is
 * `isImportantForAccessibility = false`, so it changes no accessibility behaviour and is invisible
 * until a tag appears beneath it. Adding a tag inside an owner that was skipped costs a silent
 * regression: `onNodeWithTag(...).assertExists()` still passes, the suite stays green, and only a
 * Robo directive or `By.res()` — neither of which runs in CI — can tell that the tag never became a
 * resource id.
 *
 * So the condition for applying it is "the library creates a semantics owner here", never "there is
 * a tag under here worth exposing". Owners currently flagged with nothing tagged inside them are
 * deliberate and should not be pruned as dead code: the loading dialog, the default
 * re-authentication bottom sheet, the manage-MFA tooltip, and the TOTP enrollment steps are all in
 * that state, and each of them is one added tag away from needing it.
 */
internal fun Modifier.exposeTestTagsAsResourceIds(): Modifier =
    semantics { testTagsAsResourceId = true }
