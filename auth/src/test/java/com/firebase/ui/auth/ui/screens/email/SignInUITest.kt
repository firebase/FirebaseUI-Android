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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTextInput
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialResponse
import androidx.credentials.PasswordCredential
import androidx.test.core.app.ApplicationProvider
import com.firebase.ui.auth.FirebaseAuthUI
import com.firebase.ui.auth.R
import com.firebase.ui.auth.configuration.AuthUIConfiguration
import com.firebase.ui.auth.configuration.authUIConfiguration
import com.firebase.ui.auth.configuration.auth_provider.AuthProvider
import com.firebase.ui.auth.configuration.string_provider.AuthUIStringProvider
import com.firebase.ui.auth.configuration.string_provider.DefaultAuthUIStringProvider
import com.firebase.ui.auth.configuration.string_provider.LocalAuthUIStringProvider
import com.firebase.ui.auth.credentialmanager.CredentialManagerProvider
import com.firebase.ui.auth.credentialmanager.PasswordCredentialHandler
import com.firebase.ui.auth.util.CredentialPersistenceManager
import com.google.common.truth.Truth.assertThat
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.UserInfo
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.any
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Address of the (different) account the fake Credential Manager offers. */
private const val SAVED_CREDENTIAL_USERNAME = "saved-other@example.com"

/**
 * A Credential Manager that always offers a saved password for an account *other* than the one
 * being reauthenticated — the case that used to strand the user on a locked, wrong address.
 */
private object FakeCredentialManagerProvider : CredentialManagerProvider {
    /** Set the moment the screen reaches for a saved credential at all. */
    @Volatile
    var wasQueried: Boolean = false

    override fun getCredentialManager(context: Context): CredentialManager {
        wasQueried = true
        val response = GetCredentialResponse(
            PasswordCredential(SAVED_CREDENTIAL_USERNAME, "saved-password")
        )
        return org.mockito.kotlin.mock {
            onBlocking {
                getCredential(any<Context>(), any<androidx.credentials.GetCredentialRequest>())
            } doReturn response
        }
    }
}

/**
 * Unit tests for [SignInUI], covering the sign-up button's visibility, email pre-fill, and the
 * reauthentication-mode restrictions on the email field and Credential Manager autofill.
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
        runBlocking { CredentialPersistenceManager.clearSavedCredentialsFlag(applicationContext) }
        FakeCredentialManagerProvider.wasQueried = false
        FirebaseAuthUI.clearInstanceCache()
        FirebaseApp.getApps(applicationContext).forEach { it.delete() }
    }

    @After
    fun tearDown() {
        PasswordCredentialHandler.testCredentialManagerProvider = null
        runBlocking { CredentialPersistenceManager.clearSavedCredentialsFlag(applicationContext) }
        FirebaseAuthUI.clearInstanceCache()
        FirebaseApp.getApps(applicationContext).forEach {
            try {
                it.delete()
            } catch (_: Exception) {
            }
        }
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

    /**
     * The configuration [FirebaseAuthUI.createReauthFlow] actually produces, so these tests
     * exercise the public standalone-reauthentication entry point rather than a hand-rolled copy.
     */
    private fun createReauthFlowConfiguration(): AuthUIConfiguration {
        val providerInfo = mock(UserInfo::class.java)
        `when`(providerInfo.providerId).thenReturn("password")
        val user = mock(FirebaseUser::class.java)
        `when`(user.providerData).thenReturn(listOf(providerInfo))
        val auth = mock(FirebaseAuth::class.java)
        `when`(auth.currentUser).thenReturn(user)

        val app = FirebaseApp.initializeApp(
            applicationContext,
            FirebaseOptions.Builder()
                .setApiKey("fake-api-key")
                .setApplicationId("fake-app-id")
                .setProjectId("fake-project-id")
                .build()
        )
        val configuration = authUIConfiguration {
            context = applicationContext
            providers {
                provider(
                    AuthProvider.Email(
                        emailLinkActionCodeSettings = null,
                        passwordValidationRules = emptyList()
                    )
                )
            }
            isCredentialManagerEnabled = credentialManagerEnabled
        }
        return FirebaseAuthUI.create(app, auth).createReauthFlow(configuration).configuration
    }

    /**
     * Set before [createReauthFlowConfiguration] to build a Credential-Manager-enabled config.
     *
     * Safe as mutable per-instance state only because JUnit4 constructs a *fresh* instance of this
     * class for every `@Test` method, so it cannot leak from one test to the next. It would need
     * resetting in [setUp] under a runner that reuses the instance.
     */
    private var credentialManagerEnabled = false

    private fun setStatefulSignInUIContent(
        configuration: AuthUIConfiguration,
        initialEmail: String,
        isEmailLocked: Boolean = false,
        onSignInClicked: () -> Unit = {},
        onCredentialProbeDone: (() -> Unit)? = null,
    ) {
        composeTestRule.setContent {
            CompositionLocalProvider(LocalAuthUIStringProvider provides stringProvider) {
                var email by remember { mutableStateOf(initialEmail) }
                var password by remember { mutableStateOf("") }
                SignInUI(
                    configuration = configuration,
                    isLoading = false,
                    emailSignInLinkSent = false,
                    email = email,
                    password = password,
                    onEmailChange = { email = it },
                    onPasswordChange = { password = it },
                    onRetrievedCredential = { },
                    onSignInClick = onSignInClicked,
                    onGoToSignUp = { },
                    onGoToResetPassword = { },
                    onGoToEmailLinkSignIn = { },
                    isEmailLocked = isEmailLocked,
                )
                if (onCredentialProbeDone != null) {
                    // Mirrors the suspend read SignInUI's own autofill effect makes first, and is
                    // launched after it, so completing here means that effect already decided.
                    LaunchedEffect(Unit) {
                        PasswordCredentialHandler.hasSavedCredentials(applicationContext)
                        onCredentialProbeDone()
                    }
                }
            }
        }
    }

    /**
     * Reauthentication can only ever re-prove the signed-in user's own account, so when the library
     * says the address is locked the field is read-only: a different one would only produce an
     * opaque credential mismatch. The lock is an explicit input rather than something this screen
     * infers from the current field value — see the round-trip test in
     * [com.firebase.ui.auth.ui.screens.email.EmailAuthScreenReauthEmailLockTest].
     */
    @Test
    fun `email field is read-only when the address is locked`() {
        val prefillEmail = "linked@example.com"

        setStatefulSignInUIContent(
            createReauthFlowConfiguration(),
            initialEmail = prefillEmail,
            isEmailLocked = true,
        )

        composeTestRule.onNodeWithText(prefillEmail)
            .assert(SemanticsMatcher.keyNotDefined(SemanticsActions.SetText))
        composeTestRule.onNodeWithText(prefillEmail).assertExists()
    }

    /**
     * Regression guard: locking on the *mode* rather than on an actual prefill left the standalone
     * `createReauthFlow` path with a blank field the user could not type into, because nothing
     * prefills it unless a "Continue as" chip was tapped. An unlocked field must stay editable — and
     * must not flip to read-only on the first keystroke either.
     */
    @Test
    fun `email field stays editable in reauthentication mode when nothing was prefilled`() {
        setStatefulSignInUIContent(createReauthFlowConfiguration(), initialEmail = "")

        composeTestRule.onNodeWithText(stringProvider.emailHint)
            .performTextInput("typed@example.com")
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("typed@example.com").assertExists()
        composeTestRule.onNodeWithText("typed@example.com")
            .assert(SemanticsMatcher.keyIsDefined(SemanticsActions.SetText))
    }

    /**
     * SIGN UP creates a brand new account, which cannot re-prove an existing session — it replaces
     * it. The button was still offered during reauthentication because it is gated on
     * `AuthProvider.Email.isNewAccountsAllowed` (default `true`), which the reauthentication config
     * never touches.
     */
    @Test
    fun `sign up button is hidden in reauthentication mode`() {
        setStatefulSignInUIContent(
            createReauthFlowConfiguration(),
            initialEmail = "linked@example.com",
            isEmailLocked = true,
        )

        composeTestRule.onNode(hasText(stringProvider.signupPageTitle.uppercase()) and hasClickAction())
            .assertDoesNotExist()
    }

    /** The configuration-level veto has to work on its own, independently of the provider flag. */
    @Test
    fun `sign up button is hidden when new email accounts are not allowed by the configuration`() {
        val provider = AuthProvider.Email(
            emailLinkActionCodeSettings = null,
            isNewAccountsAllowed = true,
            passwordValidationRules = emptyList()
        )
        val configuration = authUIConfiguration {
            context = applicationContext
            providers { provider(provider) }
        }.copy(isNewEmailAccountsAllowed = false)

        setStatefulSignInUIContent(configuration, initialEmail = "")

        composeTestRule.onNode(hasText(stringProvider.signupPageTitle.uppercase()) and hasClickAction())
            .assertDoesNotExist()
    }

    /**
     * `isCredentialManagerEnabled` defaults to true and the reauthentication config preserves it,
     * so this effect used to fire during reauthentication too — writing a saved credential straight
     * into the form and auto-submitting it. A saved password for a *different* account would then
     * silently submit the wrong credential (a read-only field does not stop a programmatic write),
     * stranding the user. The control test below proves the harness really does autofill.
     */
    @Test
    fun `credential manager autofill is skipped in reauthentication mode`() {
        credentialManagerEnabled = true
        runBlocking { CredentialPersistenceManager.setCredentialsSaved(applicationContext) }
        PasswordCredentialHandler.testCredentialManagerProvider = FakeCredentialManagerProvider
        var signInClicks = 0

        var probeDone = false
        setStatefulSignInUIContent(
            createReauthFlowConfiguration(),
            initialEmail = "linked@example.com",
            onSignInClicked = { signInClicks++ },
            onCredentialProbeDone = { probeDone = true },
        )
        awaitOrTimeout { probeDone || FakeCredentialManagerProvider.wasQueried }

        assertThat(FakeCredentialManagerProvider.wasQueried).isFalse()
        composeTestRule.onNodeWithText(SAVED_CREDENTIAL_USERNAME).assertDoesNotExist()
        composeTestRule.onNodeWithText("linked@example.com").assertExists()
        assertThat(signInClicks).isEqualTo(0)
    }

    /**
     * Polls [condition] and returns as soon as it holds, idling composition in between. The two
     * second cap is only a safety net — the caller supplies a condition that really does settle.
     */
    private fun awaitOrTimeout(condition: () -> Boolean) {
        val deadline = System.currentTimeMillis() + 2_000
        while (System.currentTimeMillis() < deadline && !condition()) {
            composeTestRule.waitForIdle()
            Thread.sleep(25)
        }
    }

    /**
     * Firebase reports the provider id `"password"` for passwordless email-link accounts too, so
     * such a user is offered the Email method and lands on a password field they can never fill.
     * Reauthentication mode has also removed the email-link toggle and "trouble signing in?", so
     * without this notice the screen is a silent dead end. The provider cannot be filtered out
     * instead: `providerData` cannot tell a password account from an email-link one.
     */
    @Test
    fun `a password requirement notice is shown while reauthenticating`() {
        setStatefulSignInUIContent(
            createReauthFlowConfiguration(),
            initialEmail = "linked@example.com",
            isEmailLocked = true,
        )

        composeTestRule.onNodeWithTag(REAUTH_PASSWORD_NOTICE_TEST_TAG).assertExists()
        composeTestRule
            .onNodeWithText(applicationContext.getString(R.string.fui_reauth_password_required_notice))
            .assertExists()
    }

    /** The notice is specific to reauthentication and must not appear in a normal sign-in. */
    @Test
    fun `no password requirement notice outside reauthentication`() {
        setSignInUIContent(isNewAccountsAllowed = true)

        composeTestRule.onNodeWithTag(REAUTH_PASSWORD_NOTICE_TEST_TAG).assertDoesNotExist()
    }

    /** Control for the test above: outside reauthentication mode the autofill still happens. */
    @Test
    fun `credential manager autofill still happens outside reauthentication mode`() {
        runBlocking { CredentialPersistenceManager.setCredentialsSaved(applicationContext) }
        PasswordCredentialHandler.testCredentialManagerProvider = FakeCredentialManagerProvider
        var signInClicks = 0
        val configuration = authUIConfiguration {
            context = applicationContext
            providers {
                provider(
                    AuthProvider.Email(
                        emailLinkActionCodeSettings = null,
                        passwordValidationRules = emptyList()
                    )
                )
            }
            isCredentialManagerEnabled = true
        }

        setStatefulSignInUIContent(
            configuration,
            initialEmail = "",
            onSignInClicked = { signInClicks++ },
        )
        composeTestRule.waitUntil(timeoutMillis = 5_000) { signInClicks > 0 }

        composeTestRule.onNodeWithText(SAVED_CREDENTIAL_USERNAME).assertExists()
        assertThat(signInClicks).isEqualTo(1)
    }
}
