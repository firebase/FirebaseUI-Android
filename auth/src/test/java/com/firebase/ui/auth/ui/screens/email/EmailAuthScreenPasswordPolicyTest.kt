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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ApplicationProvider
import com.firebase.ui.auth.AuthException
import com.firebase.ui.auth.FirebaseAuthUI
import com.firebase.ui.auth.configuration.authUIConfiguration
import com.firebase.ui.auth.configuration.auth_provider.AuthProvider
import com.firebase.ui.auth.configuration.password_policy.CertificateHashProvider
import com.firebase.ui.auth.configuration.password_policy.PasswordPolicyHttpClient
import com.firebase.ui.auth.configuration.password_policy.PasswordPolicyRepository
import com.firebase.ui.auth.configuration.string_provider.AuthUIStringProvider
import com.firebase.ui.auth.configuration.string_provider.DefaultAuthUIStringProvider
import com.firebase.ui.auth.configuration.string_provider.LocalAuthUIStringProvider
import com.firebase.ui.auth.ui.FirebaseAuthTestTags
import com.google.android.gms.tasks.Tasks
import com.google.common.truth.Truth.assertThat
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * [AuthProvider.Email.isProjectPasswordPolicyEnabled] wiring in [EmailAuthScreen]: the policy
 * source it provides, the repository it shares, and the refetch after a server rejection.
 *
 * @suppress Internal test class
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class EmailAuthScreenPasswordPolicyTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var applicationContext: Context
    private lateinit var stringProvider: AuthUIStringProvider
    private lateinit var app: FirebaseApp
    private lateinit var auth: FirebaseAuth
    private lateinit var authUI: FirebaseAuthUI

    /** Served in order; the last one repeats. */
    private var responses = listOf(PasswordPolicyHttpClient.HttpResponse(200, policyJson(10)))
    /** Untenanted requests only; tenant-routed ones count in [tenantRequests]. */
    private var requests = 0

    /** Per-tenant responses, served in order to requests carrying that tenant; the last repeats. */
    private var tenantResponses = mapOf<String, List<PasswordPolicyHttpClient.HttpResponse>>()
    private var tenantRequests = 0
    private val errors = mutableListOf<AuthException>()

    @Before
    fun setUp() {
        applicationContext = ApplicationProvider.getApplicationContext()
        stringProvider = DefaultAuthUIStringProvider(applicationContext)
        FirebaseAuthUI.clearInstanceCache()
        FirebaseApp.getApps(applicationContext).forEach { it.delete() }
        app = FirebaseApp.initializeApp(
            applicationContext,
            FirebaseOptions.Builder()
                .setApiKey("fake-api-key")
                .setApplicationId("fake-app-id")
                .setProjectId("fake-project-id")
                .build()
        )
        auth = mock(FirebaseAuth::class.java)
        `when`(auth.app).thenReturn(app)
        `when`(auth.tenantId).thenReturn(null)
        authUI = FirebaseAuthUI.create(app, auth)
        authUI.testPasswordPolicyRepository = fakeRepository()
    }

    @After
    fun tearDown() {
        FirebaseAuthUI.clearInstanceCache()
        FirebaseApp.getApps(applicationContext).forEach {
            try {
                it.delete()
            } catch (_: Exception) {
            }
        }
    }

    private fun fakeRepository() = PasswordPolicyRepository(
        httpClient = PasswordPolicyHttpClient { url, _ ->
            val tenantResponses = tenantResponses.entries
                .firstOrNull { (tenant, _) -> url.contains("tenantId=$tenant") }?.value
            if (tenantResponses != null) {
                tenantResponses[minOf(tenantRequests++, tenantResponses.lastIndex)]
            } else {
                responses[minOf(requests++, responses.lastIndex)]
            }
        },
        certificateHash = CertificateHashProvider { _, _ -> null },
        ioDispatcher = Dispatchers.Unconfined,
        log = {},
    )

    private fun provider(enabled: Boolean) = AuthProvider.Email(
        isDisplayNameRequired = false,
        emailLinkActionCodeSettings = null,
        passwordValidationRules = emptyList(),
        isProjectPasswordPolicyEnabled = enabled,
    )

    private fun setScreen(
        enabled: Boolean,
        content: (@Composable (EmailAuthContentState) -> Unit)? = null,
    ) {
        val configuration = authUIConfiguration {
            context = applicationContext
            providers { provider(provider(enabled)) }
            isCredentialManagerEnabled = false
        }
        composeTestRule.setContent {
            CompositionLocalProvider(LocalAuthUIStringProvider provides stringProvider) {
                EmailAuthScreen(
                    context = applicationContext,
                    configuration = configuration,
                    authUI = authUI,
                    mode = EmailAuthMode.SignUp,
                    onNavigateToMode = { _, _ -> },
                    onSuccess = {},
                    onError = { errors += it },
                    onCancel = {},
                    content = content,
                )
            }
        }
        composeTestRule.waitForIdle()
    }

    private val isChecklistLine = SemanticsMatcher.keyIsDefined(SemanticsProperties.ToggleableState)

    private fun checklistLine(label: String) =
        composeTestRule.onNode(hasText(label) and isChecklistLine)

    private fun submit(password: String) {
        composeTestRule.onNodeWithText(stringProvider.emailHint)
            .performTextInput("test@example.com")
        composeTestRule.onNodeWithText(stringProvider.passwordHint).performTextInput(password)
        composeTestRule.onNodeWithText(stringProvider.confirmPasswordHint)
            .performTextInput(password)
        composeTestRule.onNodeWithTag(FirebaseAuthTestTags.SignUp.SIGN_UP_BUTTON)
            .performScrollTo()
            .performClick()
        composeTestRule.waitForIdle()
    }

    private fun rejectSignUp(exception: Exception) {
        `when`(auth.createUserWithEmailAndPassword(anyString(), anyString()))
            .thenReturn(Tasks.forException(exception))
    }

    private fun policyRejection(minLength: Int) = FirebaseAuthWeakPasswordException(
        "ERROR_WEAK_PASSWORD",
        "The given password is invalid.",
        "PASSWORD_DOES_NOT_MEET_REQUIREMENTS:Missing password requirements: " +
            "[Password must contain at least $minLength characters]",
    )

    // =============================================================================================
    // Source wiring
    // =============================================================================================

    @Test
    fun `flag off provides no source and fetches nothing`() {
        var source: PasswordPolicySource? = NOT_READ
        setScreen(enabled = false) { source = LocalPasswordPolicySource.current }

        assertThat(source).isNull()
        assertThat(requests).isEqualTo(0)
    }

    @Test
    fun `flag off shows no checklist on the default sign-up screen`() {
        setScreen(enabled = false)

        composeTestRule.onAllNodes(isChecklistLine).assertCountEquals(0)
        assertThat(requests).isEqualTo(0)
    }

    @Test
    fun `flag on fetches once and shows the policy checklist`() {
        setScreen(enabled = true)

        assertThat(requests).isEqualTo(1)
        checklistLine(stringProvider.passwordTooShort(10)).assertExists()
    }

    @Test
    fun `flag on provides one source across recompositions, backed by the shared repository`() {
        val seen = mutableListOf<PasswordPolicySource?>()
        var tick by mutableIntStateOf(0)
        setScreen(enabled = true) {
            // Read so a tick recomposes this slot.
            check(tick >= 0)
            seen += LocalPasswordPolicySource.current
        }
        tick++
        composeTestRule.waitForIdle()

        assertThat(seen.size).isAtLeast(2)
        assertThat(seen.toSet()).hasSize(1)
        assertThat(seen.first()).isNotNull()
        // Reads through the instance's repository: served from its cache, no second request.
        val policy = runBlocking { seen.first()!!.getPolicy(applicationContext) }
        assertThat(policy?.minPasswordLength).isEqualTo(10)
        assertThat(requests).isEqualTo(1)
    }

    @Test
    fun `one repository per FirebaseAuthUI instance`() {
        val other = FirebaseAuthUI.create(app, auth)

        assertThat(authUI.passwordPolicyRepository).isSameInstanceAs(authUI.passwordPolicyRepository)
        assertThat(other.passwordPolicyRepository).isNotSameInstanceAs(authUI.passwordPolicyRepository)
    }

    @Test
    fun `a recreated sign-up screen reuses the cached policy`() {
        var visible by mutableStateOf(true)
        val configuration = authUIConfiguration {
            context = applicationContext
            providers { provider(provider(true)) }
            isCredentialManagerEnabled = false
        }
        composeTestRule.setContent {
            CompositionLocalProvider(LocalAuthUIStringProvider provides stringProvider) {
                if (visible) {
                    EmailAuthScreen(
                        context = applicationContext,
                        configuration = configuration,
                        authUI = authUI,
                        mode = EmailAuthMode.SignUp,
                        onNavigateToMode = { _, _ -> },
                        onSuccess = {},
                        onError = {},
                        onCancel = {},
                    )
                }
            }
        }
        composeTestRule.waitForIdle()
        visible = false
        composeTestRule.waitForIdle()
        visible = true
        composeTestRule.waitForIdle()

        assertThat(requests).isEqualTo(1)
        checklistLine(stringProvider.passwordTooShort(10)).assertExists()
    }

    // =============================================================================================
    // Refetch after PASSWORD_DOES_NOT_MEET_REQUIREMENTS
    // =============================================================================================

    @Test
    fun `policy rejection refetches a cached policy and the checklist shows the new one`() {
        responses = listOf(
            PasswordPolicyHttpClient.HttpResponse(200, policyJson(10)),
            PasswordPolicyHttpClient.HttpResponse(200, policyJson(12)),
        )
        rejectSignUp(policyRejection(12))
        setScreen(enabled = true)

        submit("abcdefghij")

        assertThat(requests).isEqualTo(2)
        checklistLine(stringProvider.passwordTooShort(12)).assertExists()
        checklistLine(stringProvider.passwordTooShort(10)).assertDoesNotExist()
    }

    @Test
    fun `a tenant change while sign-up is shown shows and refetches that tenant's policy`() {
        tenantResponses = mapOf(
            "tenant-b" to listOf(
                PasswordPolicyHttpClient.HttpResponse(200, policyJson(14)),
                PasswordPolicyHttpClient.HttpResponse(200, policyJson(16)),
            )
        )
        setScreen(enabled = true)
        checklistLine(stringProvider.passwordTooShort(10)).assertExists()

        `when`(auth.tenantId).thenReturn("tenant-b")
        // Not snapshot state: typing recomposes the screen so it reads the new tenant.
        composeTestRule.onNodeWithText(stringProvider.emailHint).performTextInput("x")
        composeTestRule.waitForIdle()

        assertThat(tenantRequests).isEqualTo(1)
        checklistLine(stringProvider.passwordTooShort(14)).assertExists()
        checklistLine(stringProvider.passwordTooShort(10)).assertDoesNotExist()

        rejectSignUp(policyRejection(16))
        submit("abcdefghijklmn")

        assertThat(tenantRequests).isEqualTo(2)
        checklistLine(stringProvider.passwordTooShort(16)).assertExists()
        checklistLine(stringProvider.passwordTooShort(14)).assertDoesNotExist()
    }

    @Test
    fun `other sign-up errors do not refetch`() {
        rejectSignUp(FirebaseNetworkException("offline"))
        setScreen(enabled = true)

        submit("abcdefghij")

        assertThat(errors).isNotEmpty()
        assertThat(requests).isEqualTo(1)
    }

    @Test
    fun `policy rejection does not refetch when no policy is cached`() {
        responses = listOf(PasswordPolicyHttpClient.HttpResponse(501, null))
        rejectSignUp(policyRejection(12))
        setScreen(enabled = true)

        submit("abcdefghij")

        assertThat(requests).isEqualTo(1)
        composeTestRule.onAllNodes(isChecklistLine).assertCountEquals(0)
    }

    @Test
    fun `flag off never refetches after a policy rejection`() {
        rejectSignUp(policyRejection(12))
        setScreen(enabled = false)

        submit("abcdefghij")

        assertThat(requests).isEqualTo(0)
    }

    // =============================================================================================
    // Backstop: the server message matcher (CPRN-443) still applies when the fetch is unavailable
    // =============================================================================================

    @Test
    fun `rejection maps to translated requirement messages when the policy fetch fails`() {
        responses = listOf(PasswordPolicyHttpClient.HttpResponse(501, null))
        rejectSignUp(policyRejection(12))
        setScreen(enabled = true)

        submit("abcdefghij")

        val error = errors.first()
        assertThat(error).isInstanceOf(AuthException.PasswordPolicyViolationException::class.java)
        assertThat(error.message).isEqualTo(stringProvider.passwordTooShort(12))
    }

    private companion object {
        val NOT_READ = PasswordPolicySource { null }

        fun policyJson(minLength: Int) = """
            {
              "customStrengthOptions": {"minPasswordLength": $minLength},
              "enforcementState": "ENFORCE",
              "schemaVersion": 1
            }
        """
    }
}
