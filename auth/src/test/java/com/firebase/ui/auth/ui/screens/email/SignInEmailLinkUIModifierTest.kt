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
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
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
 * Modifier-contract tests for [SignInEmailLinkUI].
 *
 * This screen carried the same defect as [SignInUI]: the screen-level `modifier` was correctly
 * applied to the `Scaffold` and then applied a second time to the "trouble signing in" label, so a
 * caller's sizing, padding or tag reached a leaf it was never meant to touch.
 *
 * @suppress Internal test class
 */
@Config(manifest = Config.NONE, sdk = [34])
@RunWith(RobolectricTestRunner::class)
class SignInEmailLinkUIModifierTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var applicationContext: Context
    private lateinit var stringProvider: AuthUIStringProvider

    @Before
    fun setUp() {
        applicationContext = ApplicationProvider.getApplicationContext()
        stringProvider = DefaultAuthUIStringProvider(applicationContext)
    }

    private fun setContent(modifier: Modifier) {
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
                SignInEmailLinkUI(
                    modifier = modifier,
                    configuration = configuration,
                    isLoading = false,
                    emailSignInLinkSent = false,
                    email = "",
                    onEmailChange = { },
                    onSignInWithEmailLink = { },
                    onGoToSignIn = { },
                    onGoToResetPassword = { },
                )
            }
        }
    }

    @Test
    fun `caller modifier does not reach the trouble signing in label`() {
        setContent(modifier = Modifier.testTag(CALLER_TAG))

        composeTestRule
            .onNode(
                hasTestTag(CALLER_TAG) and hasText(stringProvider.troubleSigningIn),
                useUnmergedTree = true
            )
            .assertDoesNotExist()
    }

    @Test
    fun `caller modifier is applied to exactly one node`() {
        setContent(modifier = Modifier.testTag(CALLER_TAG))

        composeTestRule
            .onAllNodesWithTag(CALLER_TAG, useUnmergedTree = true)
            .assertCountEquals(1)
    }

    private companion object {
        const val CALLER_TAG = "caller_supplied_tag"
    }
}
