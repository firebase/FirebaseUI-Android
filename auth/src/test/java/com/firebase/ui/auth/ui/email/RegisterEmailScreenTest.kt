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

package com.firebase.ui.auth.ui.email

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.firebase.ui.auth.data.model.FlowParameters
import com.firebase.ui.auth.data.model.User
import com.google.common.truth.Truth.assertThat
import com.google.firebase.auth.EmailAuthProvider
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

private fun hasAnyError(): SemanticsMatcher =
    SemanticsMatcher("has any error") { it.config.contains(SemanticsProperties.Error) }

private val signUpButton = hasText("Sign up", ignoreCase = true).and(hasClickAction())


@RunWith(AndroidJUnit4::class)
class RegisterEmailScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var flowParameters: FlowParameters

    @Before
    fun setUp() {
        flowParameters = FlowParameters(
            appName                  = "test-app",
            providers                = emptyList(),
            defaultProvider          = null,
            themeId                  = 0,
            logoId                   = 0,
            termsOfServiceUrl        = "https://example.com/terms",
            privacyPolicyUrl         = "https://example.com/privacy",
            enableCredentials        = false,
            enableAnonymousUpgrade   = false,
            alwaysShowProviderChoice = true,
            lockOrientation          = false,
            emailLink                = null,
            passwordResetSettings    = null,
            authMethodPickerLayout   = null
        )
    }

    @Test
    fun initialEmailAndName_areDisplayed() {
        val initialUser = User.Builder(EmailAuthProvider.PROVIDER_ID, "alice@invertase.io")
            .setName("Alice")
            .build()

        composeTestRule.setContent {
            RegisterEmailScreen(
                flowParameters    = flowParameters,
                user              = initialUser,
                onRegisterSuccess = { _, _ -> },
                onRegisterError   = {}
            )
        }

        composeTestRule.onNodeWithText("alice@invertase.io", substring = false, ignoreCase = true)
            .assertIsDisplayed()
        composeTestRule.onNodeWithText("Alice", substring = false, ignoreCase = true)
            .assertIsDisplayed()
    }

    @Test
    fun enteringValidData_andClickingSignUp_invokesCallback() {
        var callbackInvoked = false

        composeTestRule.setContent {
            RegisterEmailScreen(
                flowParameters    = flowParameters,
                user              = User.Builder(EmailAuthProvider.PROVIDER_ID, "").build(),
                onRegisterSuccess = { _, _ -> callbackInvoked = true },
                onRegisterError   = {}
            )
        }

        composeTestRule.onNodeWithText("Email", substring = true)
            .performTextInput("bob@example.com")
        composeTestRule.onNodeWithText("Name", substring = true, ignoreCase = true)
            .performTextInput("Bob")
        composeTestRule.onNodeWithText("Password", substring = true)
            .performTextInput("password123") 

        composeTestRule.onNode(signUpButton).performClick()

        composeTestRule.waitUntil(timeoutMillis = 2_000) { callbackInvoked }
        assertThat(callbackInvoked).isTrue()
    }

    @Test
    fun emptyForm_andClickingSignUp_setsError_andDoesNotInvokeCallback() {
        var callbackInvoked = false

        composeTestRule.setContent {
            RegisterEmailScreen(
                flowParameters    = flowParameters,
                user              = User.Builder(EmailAuthProvider.PROVIDER_ID, "").build(),
                onRegisterSuccess = { _, _ -> callbackInvoked = true },
                onRegisterError   = {}
            )
        }

        composeTestRule.onNode(signUpButton).performClick()

        composeTestRule
            .onNode(hasText("Email", ignoreCase = true).and(hasAnyError()))
            .assertExists()

        assertThat(callbackInvoked).isFalse()
    }
}