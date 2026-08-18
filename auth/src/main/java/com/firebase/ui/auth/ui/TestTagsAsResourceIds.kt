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
 * inherits nothing from the composable that opened it and has to carry the flag itself.
 */
internal fun Modifier.exposeTestTagsAsResourceIds(): Modifier =
    semantics { testTagsAsResourceId = true }
