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

package com.firebase.ui.auth.ui.screens

import android.content.Context
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.hasAnyDescendant
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.test.core.app.ApplicationProvider
import com.firebase.ui.auth.FirebaseAuthUI
import com.firebase.ui.auth.configuration.AuthUIConfiguration
import com.firebase.ui.auth.configuration.authUIConfiguration
import com.firebase.ui.auth.configuration.auth_provider.AuthProvider
import com.firebase.ui.auth.configuration.string_provider.DefaultAuthUIStringProvider
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
 * Tests that [FirebaseAuthScreen] honours the Compose modifier contract: the caller's `modifier`
 * is applied once, to the composable's own outermost node.
 *
 * The screen used to ignore its `modifier` at the root — the hosting `Surface` hardcoded
 * `Modifier.fillMaxSize()` — and forward the caller's instance into individual `NavHost`
 * destinations instead. That made the parameter mean "decorate whichever screen happens to be
 * showing", which is both surprising and incomplete: destinations other than the method picker
 * never received it at all, so a caller could not decorate the flow as a whole. In particular a
 * host application could not attach `semantics { testTagsAsResourceId = true }` through the public
 * API, because no destination-level modifier reaches the root.
 *
 * @suppress Internal test class
 */
@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, sdk = [34])
class FirebaseAuthScreenModifierTest {

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

    /** A single email provider makes the email screen — not the method picker — the start route. */
    private fun emailOnlyConfiguration(): AuthUIConfiguration = authUIConfiguration {
        context = this@FirebaseAuthScreenModifierTest.context
        providers {
            provider(
                AuthProvider.Email(
                    emailLinkActionCodeSettings = null,
                    passwordValidationRules = emptyList()
                )
            )
        }
    }

    private fun methodPickerConfiguration(): AuthUIConfiguration = authUIConfiguration {
        context = this@FirebaseAuthScreenModifierTest.context
        providers {
            provider(
                AuthProvider.Email(
                    emailLinkActionCodeSettings = null,
                    passwordValidationRules = emptyList()
                )
            )
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
        configuration: AuthUIConfiguration,
        modifier: Modifier,
        customMethodPickerLayout: (@Composable () -> Unit)? = null,
    ) {
        composeTestRule.setContent {
            FirebaseAuthScreen(
                modifier = modifier,
                configuration = configuration,
                authUI = authUI,
                onSignInSuccess = {},
                onSignInFailure = {},
                onSignInCancelled = {},
                customMethodPickerLayout = customMethodPickerLayout?.let { slot ->
                    { _, _ -> slot() }
                }
            )
        }
    }

    /**
     * The decisive case. When the flow starts somewhere other than the method picker, the old code
     * dropped the caller's modifier entirely — there was no `modifier` forwarding on any route
     * except `MethodPicker`, and the root `Surface` used a fresh `Modifier`. So this found zero
     * nodes before the fix and finds exactly one after it.
     */
    @Test
    fun `caller modifier reaches the root on a route that never received it`() {
        setContent(emailOnlyConfiguration(), Modifier.testTag(CALLER_TAG))

        composeTestRule
            .onAllNodesWithTag(CALLER_TAG, useUnmergedTree = true)
            .assertCountEquals(1)
    }

    /**
     * And the node it reaches really is the root, not a leaf that happens to exist once: the
     * destination's content sits underneath it.
     */
    @Test
    fun `caller modifier node hosts the destination content`() {
        setContent(emailOnlyConfiguration(), Modifier.testTag(CALLER_TAG))

        val screenTitle = DefaultAuthUIStringProvider(context).signInDefault

        composeTestRule
            .onAllNodes(
                hasTestTag(CALLER_TAG) and hasAnyDescendant(hasText(screenTitle)),
                useUnmergedTree = true
            )
            .assertCountEquals(1)
    }

    /**
     * Regression guard for the method-picker route, which is the one route that did receive the
     * caller's modifier. It must now be tagged once, at the root, rather than on the picker's own
     * `Column`. This asserts the count only, so it held before the fix as well; the routing case
     * above is what pins the change.
     */
    @Test
    fun `caller modifier is applied once on the method picker route`() {
        setContent(methodPickerConfiguration(), Modifier.testTag(CALLER_TAG))

        composeTestRule
            .onAllNodesWithTag(CALLER_TAG, useUnmergedTree = true)
            .assertCountEquals(1)
    }

    /**
     * The custom method-picker slot used to take the caller's modifier on its wrapping `Box`; it
     * now sits under the tagged root instead. Both arrangements satisfy these assertions, so this
     * is a guard against the modifier being duplicated or dropped on this path rather than a pin
     * on the change itself.
     */
    @Test
    fun `caller modifier is applied once when a custom method picker is supplied`() {
        setContent(
            configuration = methodPickerConfiguration(),
            modifier = Modifier.testTag(CALLER_TAG),
            customMethodPickerLayout = {
                Text(text = "Custom Picker", modifier = Modifier.testTag(SENTINEL_TAG))
            }
        )

        composeTestRule
            .onAllNodesWithTag(CALLER_TAG, useUnmergedTree = true)
            .assertCountEquals(1)

        composeTestRule
            .onAllNodes(
                hasTestTag(CALLER_TAG) and hasAnyDescendant(hasTestTag(SENTINEL_TAG)),
                useUnmergedTree = true
            )
            .assertCountEquals(1)
    }

    private companion object {
        const val CALLER_TAG = "caller_supplied_tag"

        const val SENTINEL_TAG = "destination_content_sentinel"
    }
}
