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
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithText
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
 * Unit tests for [SignInUI], covering the sign-up button's visibility and email pre-fill.
 *
 * @suppress Internal test class
 */
@Config(sdk = [34])
@RunWith(RobolectricTestRunner::class)
class SignInUITest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var applicationContext: Context
    private lateinit var stringProvider: AuthUIStringProvider

    @Before
    fun setUp() {
        applicationContext = ApplicationProvider.getApplicationContext()
        stringProvider = DefaultAuthUIStringProvider(applicationContext)
    }

    private fun setSignInUIContent(isNewAccountsAllowed: Boolean) {
        val provider = AuthProvider.Email(
            emailLinkActionCodeSettings = null,
            isNewAccountsAllowed = isNewAccountsAllowed,
            passwordValidationRules = emptyList()
        )
        val configuration = authUIConfiguration {
            context = applicationContext
            providers { provider(provider) }
        }

        composeTestRule.setContent {
            CompositionLocalProvider(LocalAuthUIStringProvider provides stringProvider) {
                SignInUI(
                    configuration = configuration,
                    isLoading = false,
                    emailSignInLinkSent = false,
                    email = "",
                    password = "",
                    onEmailChange = { },
                    onPasswordChange = { },
                    onSignInClick = { },
                    onRetrievedCredential = { },
                    onGoToEmailLinkSignIn = { },
                    onGoToSignUp = { },
                    onGoToResetPassword = { },
                )
            }
        }
    }

    @Test
    fun `sign up button is hidden when new accounts are not allowed`() {
        setSignInUIContent(isNewAccountsAllowed = false)

        composeTestRule.onNode(hasText(stringProvider.signupPageTitle.uppercase()) and hasClickAction())
            .assertDoesNotExist()
    }

    @Test
    fun `sign up button is enabled when new accounts are allowed`() {
        setSignInUIContent(isNewAccountsAllowed = true)

        composeTestRule.onNode(hasText(stringProvider.signupPageTitle.uppercase()) and hasClickAction())
            .assertIsEnabled()
    }

    @Test
    fun `email field is pre-filled when initial email value is provided`() {
        val prefillEmail = "user@example.com"
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
                SignInUI(
                    configuration = configuration,
                    isLoading = false,
                    emailSignInLinkSent = false,
                    email = prefillEmail,
                    password = "",
                    onEmailChange = { },
                    onPasswordChange = { },
                    onRetrievedCredential = { },
                    onSignInClick = { },
                    onGoToSignUp = { },
                    onGoToResetPassword = { },
                    onGoToEmailLinkSignIn = { },
                )
            }
        }

        composeTestRule.onNodeWithText(prefillEmail).assertExists()
    }

    @Test
    fun `email field is empty when no initial email value is provided`() {
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
                SignInUI(
                    configuration = configuration,
                    isLoading = false,
                    emailSignInLinkSent = false,
                    email = "",
                    password = "",
                    onEmailChange = { },
                    onPasswordChange = { },
                    onRetrievedCredential = { },
                    onSignInClick = { },
                    onGoToSignUp = { },
                    onGoToResetPassword = { },
                    onGoToEmailLinkSignIn = { },
                )
            }
        }

        composeTestRule.onNodeWithText("user@example.com").assertDoesNotExist()
    }

    // ---- Modifier contract tests ----

    private fun setSignInUIContent(modifier: Modifier) {
        val provider = AuthProvider.Email(
            emailLinkActionCodeSettings = null,
            passwordValidationRules = emptyList()
        )
        val configuration = authUIConfiguration {
            context = applicationContext
            providers { provider(provider) }
        }

        composeTestRule.setContent {
            CompositionLocalProvider(LocalAuthUIStringProvider provides stringProvider) {
                SignInUI(
                    modifier = modifier,
                    configuration = configuration,
                    isLoading = false,
                    emailSignInLinkSent = false,
                    email = "",
                    password = "",
                    onEmailChange = { },
                    onPasswordChange = { },
                    onSignInClick = { },
                    onRetrievedCredential = { },
                    onGoToEmailLinkSignIn = { },
                    onGoToSignUp = { },
                    onGoToResetPassword = { },
                )
            }
        }
    }

    /**
     * The screen's `modifier` used to also land on the "trouble signing in" `Text`; it must reach
     * only the screen root.
     */
    @Test
    fun `caller modifier does not reach the trouble signing in label`() {
        setSignInUIContent(modifier = Modifier.testTag(CALLER_TAG))

        composeTestRule
            .onNode(
                hasTestTag(CALLER_TAG) and hasText(stringProvider.troubleSigningIn),
                useUnmergedTree = true
            )
            .assertDoesNotExist()
    }

    /** The corollary: exactly one node carries what the caller passed. */
    @Test
    fun `caller modifier is applied to exactly one node`() {
        setSignInUIContent(modifier = Modifier.testTag(CALLER_TAG))

        composeTestRule
            .onAllNodesWithTag(CALLER_TAG, useUnmergedTree = true)
            .assertCountEquals(1)
    }

    private companion object {
        const val CALLER_TAG = "caller_supplied_tag"
    }
}
