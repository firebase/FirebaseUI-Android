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

package com.firebase.ui.auth.ui.screens.phone

import android.content.Context
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.test.core.app.ApplicationProvider
import com.firebase.ui.auth.FirebaseAuthUI
import com.firebase.ui.auth.configuration.AuthUIConfiguration
import com.firebase.ui.auth.configuration.authUIConfiguration
import com.firebase.ui.auth.configuration.auth_provider.AuthProvider
import com.firebase.ui.auth.configuration.string_provider.DefaultAuthUIStringProvider
import com.firebase.ui.auth.configuration.string_provider.LocalAuthUIStringProvider
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Tests that [PhoneAuthScreen] honours its `modifier` contract, which used to be dead: declared
 * but never applied on either the content-slot or default-UI rendering path.
 *
 * @suppress Internal test class
 */
@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, sdk = [34])
class PhoneAuthScreenModifierTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var context: Context
    private lateinit var authUI: FirebaseAuthUI

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        FirebaseAuthUI.clearInstanceCache()
        FirebaseApp.getApps(context).forEach { it.delete() }
        FirebaseApp.initializeApp(
            context,
            FirebaseOptions.Builder()
                .setApiKey("fake-api-key")
                .setApplicationId("fake-app-id")
                .setProjectId("fake-project-id")
                .build()
        )
        authUI = FirebaseAuthUI.getInstance()
    }

    @After
    fun tearDown() {
        FirebaseAuthUI.clearInstanceCache()
        FirebaseApp.getApps(context).forEach {
            try { it.delete() } catch (_: Exception) {}
        }
    }

    private fun phoneOnlyConfiguration(): AuthUIConfiguration = authUIConfiguration {
        context = this@PhoneAuthScreenModifierTest.context
        providers {
            provider(
                AuthProvider.Phone(
                    defaultNumber = null,
                    defaultCountryCode = null,
                    allowedCountries = null
                )
            )
        }
    }

    private fun setContent(
        modifier: Modifier,
        content: @Composable ((PhoneAuthContentState) -> Unit)? = null,
    ) {
        val configuration = phoneOnlyConfiguration()
        composeTestRule.setContent {
            CompositionLocalProvider(
                LocalAuthUIStringProvider provides DefaultAuthUIStringProvider(context)
            ) {
                PhoneAuthScreen(
                    context = context,
                    configuration = configuration,
                    authUI = authUI,
                    onSuccess = { },
                    onError = { },
                    onCancel = { },
                    modifier = modifier,
                    step = PhoneAuthStep.EnterPhoneNumber,
                    onNavigateToStep = { },
                    onNavigateBack = { },
                    flowState = rememberPhoneAuthFlowState(configuration),
                    content = content,
                )
            }
        }
    }

    @Test
    fun `caller modifier reaches the rendered tree on the default content path`() {
        setContent(modifier = Modifier.testTag(CALLER_TAG))

        composeTestRule
            .onAllNodesWithTag(CALLER_TAG, useUnmergedTree = true)
            .assertCountEquals(1)
    }

    @Test
    fun `caller modifier reaches the rendered tree on the content slot path`() {
        setContent(modifier = Modifier.testTag(CALLER_TAG)) { state ->
            Text(text = "step: ${state.step}")
        }

        composeTestRule
            .onAllNodesWithTag(CALLER_TAG, useUnmergedTree = true)
            .assertCountEquals(1)
    }

    private companion object {
        const val CALLER_TAG = "caller_supplied_tag"
    }
}
