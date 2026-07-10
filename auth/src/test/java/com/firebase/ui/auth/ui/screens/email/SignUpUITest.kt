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

import android.content.Context
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ApplicationProvider
import com.firebase.ui.auth.configuration.authUIConfiguration
import com.firebase.ui.auth.configuration.auth_provider.AuthProvider
import com.firebase.ui.auth.configuration.string_provider.AuthUIStringProvider
import com.firebase.ui.auth.configuration.string_provider.DefaultAuthUIStringProvider
import com.firebase.ui.auth.configuration.string_provider.LocalAuthUIStringProvider
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Unit tests for [SignUpUI], covering form validity logic.
 *
 * @suppress Internal test class
 */
@Config(sdk = [34])
@RunWith(RobolectricTestRunner::class)
class SignUpUITest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var applicationContext: Context
    private lateinit var stringProvider: AuthUIStringProvider

    @Before
    fun setUp() {
        applicationContext = ApplicationProvider.getApplicationContext()
        stringProvider = DefaultAuthUIStringProvider(applicationContext)
    }

    @Test
    fun `sign up button becomes enabled when display name is not required and hidden`() {
        val provider = AuthProvider.Email(
            isDisplayNameRequired = false,
            emailLinkActionCodeSettings = null,
            passwordValidationRules = emptyList()
        )
        val configuration = authUIConfiguration {
            context = applicationContext
            providers { provider(provider) }
        }

        composeTestRule.setContent {
            CompositionLocalProvider(LocalAuthUIStringProvider provides stringProvider) {
                var email by remember { mutableStateOf("") }
                var password by remember { mutableStateOf("") }
                var confirmPassword by remember { mutableStateOf("") }

                SignUpUI(
                    configuration = configuration,
                    isLoading = false,
                    displayName = "",
                    email = email,
                    password = password,
                    confirmPassword = confirmPassword,
                    onDisplayNameChange = { },
                    onEmailChange = { email = it },
                    onPasswordChange = { password = it },
                    onConfirmPasswordChange = { confirmPassword = it },
                    onGoToSignIn = { },
                    onSignUpClick = { }
                )
            }
        }

        // Name field should not be rendered since it isn't required.
        composeTestRule.onNodeWithText(stringProvider.nameHint).assertDoesNotExist()

        composeTestRule.onNodeWithText(stringProvider.emailHint)
            .performTextInput("test@example.com")
        composeTestRule.onNodeWithText(stringProvider.passwordHint)
            .performTextInput("Password123")
        composeTestRule.onNodeWithText(stringProvider.confirmPasswordHint)
            .performTextInput("Password123")

        composeTestRule.waitForIdle()

        // With email/password/confirmPassword all valid and no display name required,
        // the sign up button should be enabled even though displayName is still "".
        composeTestRule.onNodeWithText(stringProvider.signupPageTitle.uppercase())
            .assertIsEnabled()
    }
}
