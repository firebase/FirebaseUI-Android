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

package com.firebase.ui.auth.compose.ui.screens

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import com.firebase.ui.auth.compose.FirebaseAuthUI

/**
 * Activity that handles email link deep links for passwordless authentication.
 *
 * ## Setup (Required)
 *
 * Add this activity to your app's `AndroidManifest.xml`:
 * ```xml
 * <activity
 *     android:name="com.firebase.ui.auth.compose.ui.screens.EmailSignInLinkHandlerActivity"
 *     android:exported="true"
 *     tools:replace="android:exported">
 *     <intent-filter>
 *         <action android:name="android.intent.action.VIEW" />
 *         <category android:name="android.intent.category.DEFAULT" />
 *         <category android:name="android.intent.category.BROWSABLE" />
 *         <data
 *             android:scheme="https"
 *             android:host="yourapp.com"
 *             android:pathPattern="/__/auth/.*" />
 *     </intent-filter>
 * </activity>
 * ```
 *
 * Configure matching ActionCodeSettings:
 * ```kotlin
 * val provider = AuthProvider.Email(
 *     emailLinkActionCodeSettings = actionCodeSettings {
 *         url = "https://yourapp.com"  // Must match android:host above
 *         handleCodeInApp = true
 *         setAndroidPackageName("com.yourapp.package", true, null)
 *     },
 *     isEmailLinkSignInEnabled = true
 * )
 * ```
 *
 * By default, users see a dialog "Open with Browser or App?" on first click.
 * For auto-opening without dialog, set up App Links verification:
 * https://developer.android.com/training/app-links/verify-android-applinks
 *
 * @see FirebaseAuthUI.sendSignInLinkToEmail
 */
class EmailSignInLinkHandlerActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Extract email link from deep link intent
        val emailLink = intent.data?.toString()

        if (emailLink.isNullOrEmpty()) {
            // No valid email link, just finish
            finish()
            return
        }

        // Redirect to app's launch activity with the email link
        // The app should check for this extra in onCreate and handle email link sign-in
        val launchIntent = packageManager.getLaunchIntentForPackage(packageName)

        if (launchIntent != null) {
            launchIntent.apply {
                // Clear the back stack and start fresh
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                // Pass the email link to the launch activity
                putExtra(EXTRA_EMAIL_LINK, emailLink)
            }
            startActivity(launchIntent)
        }

        finish()
    }

    companion object {
        /**
         * Intent extra key for the email link.
         *
         * Check for this extra in your MainActivity's onCreate to detect email link sign-in:
         * ```kotlin
         * val emailLink = intent.getStringExtra(EmailSignInLinkHandlerActivity.EXTRA_EMAIL_LINK)
         * if (emailLink != null) {
         *     // Handle email link sign-in
         *     firebaseAuthUI.signInWithEmailLink(...)
         * }
         * ```
         */
        const val EXTRA_EMAIL_LINK = "com.firebase.ui.auth.EXTRA_EMAIL_LINK"
    }
}