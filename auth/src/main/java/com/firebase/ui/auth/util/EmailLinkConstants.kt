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

/**
 * Constants for email link authentication.
 *
 * ## Usage Example:
 *
 * Check for email link in your MainActivity:
 * ```kotlin
 * override fun onCreate(savedInstanceState: Bundle?) {
 *     super.onCreate(savedInstanceState)
 *     
 *     val authUI = FirebaseAuthUI.getInstance()
 *     
 *     // Check if intent contains email link (from deep link)
 *     var emailLink: String? = null
 *     
 *     if (authUI.canHandleIntent(intent)) {
 *         emailLink = intent.data?.toString()
 *     }
 *     
 *     if (emailLink != null) {
 *         // Handle email link sign-in
 *         // Pass to FirebaseAuthScreen or handle manually
 *     }
 * }
 * ```
 *
 * @since 10.0.0
 */
object EmailLinkConstants {
    
    /**
     * Intent extra key for the email link.
     *
     * Use this constant when passing email links between activities via Intent extras.
     *
     * **Example:**
     * ```kotlin
     * // Sending activity
     * val intent = Intent(this, MainActivity::class.java)
     * intent.putExtra(EmailLinkConstants.EXTRA_EMAIL_LINK, emailLink)
     * startActivity(intent)
     * 
     * // Receiving activity
     * val emailLink = intent.getStringExtra(EmailLinkConstants.EXTRA_EMAIL_LINK)
     * ```
     */
    const val EXTRA_EMAIL_LINK = "com.firebase.ui.auth.EXTRA_EMAIL_LINK"
}
