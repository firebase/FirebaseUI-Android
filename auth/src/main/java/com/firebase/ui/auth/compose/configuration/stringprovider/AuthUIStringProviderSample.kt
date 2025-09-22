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

package com.firebase.ui.auth.compose.configuration.stringprovider

import android.content.Context
import com.firebase.ui.auth.compose.configuration.AuthProvider
import com.firebase.ui.auth.compose.configuration.AuthUIConfiguration
import com.firebase.ui.auth.compose.configuration.authUIConfiguration

class AuthUIStringProviderSample {
    /**
     * Override specific strings while delegating others to default provider
     */
    class CustomAuthUIStringProvider(
        private val defaultProvider: AuthUIStringProvider
    ) : AuthUIStringProvider by defaultProvider {

        // Override only the strings you want to customize
        override val signInWithGoogle: String = "Continue with Google • MyApp"
        override val signInWithFacebook: String = "Continue with Facebook • MyApp"

        // Add custom branding to common actions
        override val continueText: String = "Continue to MyApp"
        override val signInDefault: String = "Sign in to MyApp"

        // Custom MFA messaging
        override val enterTOTPCode: String =
            "Enter the 6-digit code from your authenticator app to secure your MyApp account"
    }

    fun createCustomConfiguration(applicationContext: Context): AuthUIConfiguration {
        val customStringProvider =
            CustomAuthUIStringProvider(DefaultAuthUIStringProvider(applicationContext))
        return authUIConfiguration {
            context = applicationContext
            providers {
                provider(
                    AuthProvider.Google(
                        scopes = listOf(),
                        serverClientId = ""
                    )
                )
            }
            stringProvider = customStringProvider
        }
    }
}