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
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Matches any node that exposes an `Error` semantics property. */
private fun hasAnyError(): SemanticsMatcher =
    SemanticsMatcher("has any error") { node ->
        node.config.contains(SemanticsProperties.Error)
    }

/**
 * UI tests for [CheckEmailScreen] – no Mockito required.
 */
@RunWith(AndroidJUnit4::class)
class CheckEmailScreenTest {

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
    fun initialEmail_isDisplayed() {
        val initial = "jane@invertase.io"

        composeTestRule.setContent {
            CheckEmailScreen(
                flowParameters       = flowParameters,
                initialEmail         = initial,
                onExistingEmailUser  = {},
                onExistingIdpUser    = {},
                onNewUser            = {},
                onDeveloperFailure   = {}
            )
        }

        composeTestRule
            .onNodeWithText(initial, substring = false, ignoreCase = true)
            .assertIsDisplayed()
    }

    @Test
    fun enteringValidEmail_andClickingSignIn_invokesCallback() {
        var callbackInvoked = false

        composeTestRule.setContent {
            CheckEmailScreen(
                flowParameters       = flowParameters,
                onExistingEmailUser  = { callbackInvoked = true },
                onExistingIdpUser    = {},
                onNewUser            = {},
                onDeveloperFailure   = {}
            )
        }

        composeTestRule.onNodeWithText("Email", substring = true)
            .performTextInput("test@example.com")

        composeTestRule.onNodeWithText("Sign in", substring = true)
            .performClick()

        composeTestRule.waitUntil(timeoutMillis = 2_000) { callbackInvoked }
        assertThat(callbackInvoked).isTrue()
    }

    @Test
    fun emptyEmail_andClickingSignUp_setsTextFieldError_andDoesNotInvokeCallback() {
        var callbackInvoked = false

        composeTestRule.setContent {
            CheckEmailScreen(
                flowParameters       = flowParameters,
                onExistingEmailUser  = {},
                onExistingIdpUser    = {},
                onNewUser            = { callbackInvoked = true },
                onDeveloperFailure   = {}
            )
        }

        composeTestRule
            .onNodeWithText("Sign up", substring = true, ignoreCase = true)
            .performClick()

        composeTestRule.onNode(hasAnyError()).assertExists()

        assertThat(callbackInvoked).isFalse()
    }
}