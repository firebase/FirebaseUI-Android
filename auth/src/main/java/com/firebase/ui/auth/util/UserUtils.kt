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

package com.firebase.ui.auth.util

import com.google.firebase.auth.FirebaseUser

/**
 * Returns the best available display identifier for the user, trying each field in order:
 * email → phoneNumber → displayName → uid.
 *
 * Each field is checked for blank (not just null) so that an empty string returned by the
 * Firebase SDK falls through to the next candidate rather than being displayed as-is.
 * Returns an empty string if the user is null.
 */
fun FirebaseUser?.displayIdentifier(): String =
    this?.email?.takeIf { it.isNotBlank() }
        ?: this?.phoneNumber?.takeIf { it.isNotBlank() }
        ?: this?.displayName?.takeIf { it.isNotBlank() }
        ?: this?.uid
        ?: ""

/**
 * Returns the user's email if it is non-blank, otherwise returns the provided [fallback].
 */
fun FirebaseUser?.getDisplayEmail(fallback: String): String =
    this?.email?.takeIf { it.isNotBlank() } ?: fallback
