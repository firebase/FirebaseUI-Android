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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import androidx.test.core.app.ApplicationProvider
import com.firebase.ui.auth.configuration.PasswordRule
import com.firebase.ui.auth.configuration.authUIConfiguration
import com.firebase.ui.auth.configuration.auth_provider.AuthProvider
import com.firebase.ui.auth.configuration.password_policy.CertificateHashProvider
import com.firebase.ui.auth.configuration.password_policy.PasswordPolicy
import com.firebase.ui.auth.configuration.password_policy.PasswordPolicy.EnforcementState
import com.firebase.ui.auth.configuration.password_policy.PasswordPolicyHttpClient
import com.firebase.ui.auth.configuration.password_policy.PasswordPolicyRepository
import com.firebase.ui.auth.configuration.string_provider.AuthUIStringProvider
import com.firebase.ui.auth.configuration.string_provider.DefaultAuthUIStringProvider
import com.firebase.ui.auth.configuration.string_provider.LocalAuthUIStringProvider
import com.firebase.ui.auth.ui.FirebaseAuthTestTags
import com.google.common.truth.Truth.assertThat
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Dispatchers
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Unit tests for the password requirements checklist [SignUpUI] shows from the project password
 * policy, and that it never blocks submission.
 *
 * @suppress Internal test class
 */
@Config(sdk = [34])
@RunWith(RobolectricTestRunner::class)
class SignUpUIPasswordPolicyTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var applicationContext: Context
    private lateinit var stringProvider: AuthUIStringProvider
    private var signUpClicks = 0

    @Before
    fun setUp() {
        applicationContext = ApplicationProvider.getApplicationContext()
        stringProvider = DefaultAuthUIStringProvider(applicationContext)
    }

    private fun policy(
        minPasswordLength: Int = 8,
        maxPasswordLength: Int? = null,
        lowercase: Boolean = false,
        uppercase: Boolean = false,
        numeric: Boolean = false,
        symbol: Boolean = false,
        symbols: List<String> = emptyList(),
        enforcementState: EnforcementState = EnforcementState.ENFORCE,
    ) = PasswordPolicy(
        minPasswordLength = minPasswordLength,
        maxPasswordLength = maxPasswordLength,
        containsLowercaseCharacter = lowercase,
        containsUppercaseCharacter = uppercase,
        containsNumericCharacter = numeric,
        containsNonAlphanumericCharacter = symbol,
        allowedNonAlphanumericCharacters = symbols,
        enforcementState = enforcementState,
    )

    private fun setContent(
        source: PasswordPolicySource?,
        rules: List<PasswordRule> = emptyList(),
        minimumPasswordLength: Int = 6,
    ) = setContent(rules, minimumPasswordLength) { content ->
        CompositionLocalProvider(LocalPasswordPolicySource provides source) { content() }
    }

    /** Composes [SignUpUI] inside [wrap], which decides whether a policy source is provided. */
    private fun setContent(
        rules: List<PasswordRule> = emptyList(),
        minimumPasswordLength: Int = 6,
        wrap: @Composable (content: @Composable () -> Unit) -> Unit,
    ) {
        val provider = AuthProvider.Email(
            isDisplayNameRequired = false,
            emailLinkActionCodeSettings = null,
            minimumPasswordLength = minimumPasswordLength,
            passwordValidationRules = rules,
        )
        val configuration = authUIConfiguration {
            context = applicationContext
            providers { provider(provider) }
        }
        composeTestRule.setContent {
            CompositionLocalProvider(LocalAuthUIStringProvider provides stringProvider) {
                wrap {
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
                        onSignUpClick = { signUpClicks++ },
                    )
                }
            }
        }
        composeTestRule.waitForIdle()
    }

    // Checklist lines are the only toggleable nodes on the screen, which also tells them apart
    // from the password field's own error text. Main sources may only tag with FirebaseAuthTestTags.
    private val isChecklistLine = SemanticsMatcher.keyIsDefined(SemanticsProperties.ToggleableState)

    private fun checklistLine(label: String) =
        composeTestRule.onNode(hasText(label) and isChecklistLine)

    private fun assertNoChecklist() {
        composeTestRule.onAllNodes(isChecklistLine).assertCountEquals(0)
    }

    private fun fillForm(password: String) {
        composeTestRule.onNodeWithText(stringProvider.emailHint)
            .performTextInput("test@example.com")
        composeTestRule.onNodeWithText(stringProvider.passwordHint).performTextInput(password)
        composeTestRule.onNodeWithText(stringProvider.confirmPasswordHint)
            .performTextInput(password)
        composeTestRule.waitForIdle()
    }

    private val signUpButton
        get() = composeTestRule.onNodeWithTag(FirebaseAuthTestTags.SignUp.SIGN_UP_BUTTON)

    private fun typePassword(password: String) {
        composeTestRule.onNodeWithText(stringProvider.passwordHint).performTextInput(password)
        composeTestRule.waitForIdle()
    }

    private fun assertPasswordFieldIsError() {
        composeTestRule.onNodeWithTag(FirebaseAuthTestTags.SignUp.PASSWORD_FIELD)
            .assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.Error))
    }

    // =============================================================================================
    // Hidden
    // =============================================================================================

    @Test
    fun `checklist is hidden when the switch is off`() {
        setContent(source = null)
        assertNoChecklist()
    }

    @Test
    fun `checklist is off by default when no source is provided`() {
        // Sentinel, so the assertion fails if the local is never read.
        var current: PasswordPolicySource? = PasswordPolicySource { null }
        setContent { content ->
            current = LocalPasswordPolicySource.current
            content()
        }
        assertNoChecklist()
        assertThat(current).isNull()
    }

    @Test
    fun `checklist is hidden when no policy is available`() {
        var calls = 0
        setContent(source = PasswordPolicySource { calls++; null })
        assertThat(calls).isEqualTo(1)
        assertNoChecklist()
    }

    @Test
    fun `checklist is hidden when the policy is OFF`() {
        val off = policy(uppercase = true, enforcementState = EnforcementState.OFF)
        setContent(source = PasswordPolicySource { off })
        assertNoChecklist()
    }

    // =============================================================================================
    // Rendered lines
    // =============================================================================================

    @Test
    fun `ENFORCE policy shows only its configured requirements with the allowed symbols`() {
        val enforced = policy(
            minPasswordLength = 8,
            maxPasswordLength = 20,
            uppercase = true,
            symbol = true,
            symbols = listOf("^$", "*", "!"),
        )
        setContent(source = PasswordPolicySource { enforced })

        checklistLine(stringProvider.passwordTooShort(8)).assertExists()
        checklistLine(stringProvider.passwordTooLong(20)).assertDoesNotExist()
        checklistLine(stringProvider.passwordMissingUppercase).assertExists()
        checklistLine(stringProvider.passwordMissingSpecialCharacter).assertExists()
        checklistLine(stringProvider.passwordMissingLowercase).assertDoesNotExist()
        checklistLine(stringProvider.passwordMissingDigit).assertDoesNotExist()
        composeTestRule.onNodeWithText("^ $ * !", useUnmergedTree = true).assertExists()
        checklistLine("^ $ * !").assertExists()
    }

    @Test
    fun `allowed symbols are split by code point and de-duplicated`() {
        val grinning = "\uD83D\uDE00"
        val enforced = policy(symbol = true, symbols = listOf("$grinning!", grinning, "~"))
        val symbolLine = passwordRequirements(enforced, "", stringProvider).single {
            it.label == stringProvider.passwordMissingSpecialCharacter
        }
        assertThat(symbolLine.symbols).isEqualTo("$grinning ! ~")
    }

    @Test
    fun `max length line is not shown even when the policy sets one`() {
        val enforced = policy(
            minPasswordLength = 8,
            maxPasswordLength = 4096,
            lowercase = true,
            numeric = true,
        )
        setContent(source = PasswordPolicySource { enforced })

        checklistLine(stringProvider.passwordTooShort(8)).assertExists()
        checklistLine(stringProvider.passwordMissingLowercase).assertExists()
        checklistLine(stringProvider.passwordMissingDigit).assertExists()
        composeTestRule.onAllNodesWithText(stringProvider.passwordTooLong(4096))
            .assertCountEquals(0)
        checklistLine(stringProvider.passwordMissingUppercase).assertDoesNotExist()
        checklistLine(stringProvider.passwordMissingSpecialCharacter).assertDoesNotExist()
        composeTestRule.onAllNodes(isChecklistLine).assertCountEquals(3)
    }

    @Test
    fun `lines report met and unmet live as the password changes`() {
        val enforced = policy(
            minPasswordLength = 8,
            uppercase = true,
            symbol = true,
            symbols = listOf("!"),
        )
        setContent(source = PasswordPolicySource { enforced })
        val minLine = checklistLine(stringProvider.passwordTooShort(8))
        val upperLine = checklistLine(stringProvider.passwordMissingUppercase)
        val symbolLine = checklistLine(stringProvider.passwordMissingSpecialCharacter)

        minLine.assertIsOff()
        upperLine.assertIsOff()
        symbolLine.assertIsOff()

        val passwordField = composeTestRule.onNodeWithText(stringProvider.passwordHint)
        passwordField.performTextInput("Abcdefgh!")
        composeTestRule.waitForIdle()
        minLine.assertIsOn()
        upperLine.assertIsOn()
        symbolLine.assertIsOn()

        passwordField.performTextReplacement("abcdefghijk")
        composeTestRule.waitForIdle()
        minLine.assertIsOn()
        upperLine.assertIsOff()
        symbolLine.assertIsOff()
    }

    // =============================================================================================
    // Never blocking
    // =============================================================================================

    @Test
    fun `unmet ENFORCE requirements do not block sign-up when local rules pass`() {
        val enforced = policy(minPasswordLength = 12, uppercase = true, symbol = true)
        setContent(source = PasswordPolicySource { enforced })

        fillForm("password1")
        checklistLine(stringProvider.passwordTooShort(12)).assertIsOff()
        checklistLine(stringProvider.passwordMissingUppercase).assertIsOff()

        signUpButton.assertIsEnabled()
        signUpButton.performScrollTo().performClick()
        assertThat(signUpClicks).isEqualTo(1)
    }

    @Test
    fun `local rules still block sign-up while the checklist is shown`() {
        val enforced = policy(minPasswordLength = 6)
        setContent(
            source = PasswordPolicySource { enforced },
            rules = listOf(PasswordRule.RequireDigit),
        )

        fillForm("password")
        checklistLine(stringProvider.passwordTooShort(6)).assertIsOn()
        signUpButton.assertIsNotEnabled()
        composeTestRule.onNodeWithText(stringProvider.passwordMissingDigit).assertExists()
    }

    // =============================================================================================
    // Field message vs checklist
    // =============================================================================================

    @Test
    fun `field message matching a checklist line is not repeated but the field stays in error`() {
        val enforced = policy(minPasswordLength = 6, uppercase = true)
        setContent(
            source = PasswordPolicySource { enforced },
            rules = listOf(PasswordRule.RequireUppercase),
            minimumPasswordLength = 6,
        )

        typePassword("abc")
        composeTestRule.onAllNodesWithText(stringProvider.passwordTooShort(6))
            .assertCountEquals(1)
        checklistLine(stringProvider.passwordTooShort(6)).assertIsOff()
        assertPasswordFieldIsError()

        composeTestRule.onNodeWithText(stringProvider.passwordHint).performTextReplacement("abcdef")
        composeTestRule.waitForIdle()
        composeTestRule.onAllNodesWithText(stringProvider.passwordMissingUppercase)
            .assertCountEquals(1)
        checklistLine(stringProvider.passwordMissingUppercase).assertIsOff()
        assertPasswordFieldIsError()
        signUpButton.assertIsNotEnabled()
    }

    @Test
    fun `field message matching a met checklist line is still shown`() {
        // The policy accepts `~`, the local rule does not, so only the local rule fails.
        val enforced = policy(minPasswordLength = 6, symbol = true, symbols = listOf("~"))
        setContent(
            source = PasswordPolicySource { enforced },
            rules = listOf(PasswordRule.RequireSpecialCharacter),
        )

        fillForm("Abcdef1~")
        checklistLine(stringProvider.passwordMissingSpecialCharacter).assertIsOn()
        composeTestRule.onNode(
            hasText(stringProvider.passwordMissingSpecialCharacter) and !isChecklistLine
        ).assertExists()
        assertPasswordFieldIsError()
        signUpButton.assertIsNotEnabled()
    }

    @Test
    fun `special character message is not blanked beside an unmet symbol line with a local rule`() {
        // The policy may accept a symbol the local rule rejects, so the local message stays.
        val enforced = policy(minPasswordLength = 6, symbol = true, symbols = listOf("~", "!"))
        setContent(
            source = PasswordPolicySource { enforced },
            rules = listOf(PasswordRule.RequireSpecialCharacter),
        )

        fillForm("Abcdef1")
        checklistLine(stringProvider.passwordMissingSpecialCharacter).assertIsOff()
        composeTestRule.onNode(
            hasText(stringProvider.passwordMissingSpecialCharacter) and !isChecklistLine
        ).assertExists()
        assertPasswordFieldIsError()
        signUpButton.assertIsNotEnabled()
    }

    @Test
    fun `field message absent from the checklist is still shown`() {
        val enforced = policy(minPasswordLength = 8)
        setContent(source = PasswordPolicySource { enforced }, minimumPasswordLength = 6)

        typePassword("abc")
        checklistLine(stringProvider.passwordTooShort(8)).assertIsOff()
        composeTestRule.onNode(hasText(stringProvider.passwordTooShort(6)) and !isChecklistLine)
            .assertExists()
        assertPasswordFieldIsError()
    }

    @Test
    fun `field message shows as before when the switch is off`() {
        setContent(source = null, minimumPasswordLength = 6)

        typePassword("abc")
        assertNoChecklist()
        composeTestRule.onAllNodesWithText(stringProvider.passwordTooShort(6))
            .assertCountEquals(1)
        assertPasswordFieldIsError()
    }

    // =============================================================================================
    // Repository-backed source
    // =============================================================================================

    @Test
    fun `repository source fetches once and shows the fetched policy`() {
        val requests = mutableListOf<String>()
        val repository = PasswordPolicyRepository(
            httpClient = PasswordPolicyHttpClient { url, _ ->
                requests += url
                PasswordPolicyHttpClient.HttpResponse(200, ENFORCE_RESPONSE)
            },
            certificateHash = CertificateHashProvider { _, _ -> null },
            ioDispatcher = Dispatchers.Unconfined,
        )
        setContent(source = repository.sourceFor(auth()))

        assertThat(requests).hasSize(1)
        checklistLine(stringProvider.passwordTooShort(10)).assertExists()
        checklistLine(stringProvider.passwordMissingDigit).assertExists()
    }

    private fun auth(): FirebaseAuth {
        val options = FirebaseOptions.Builder()
            .setApiKey("fake-api-key")
            .setApplicationId("1:123:android:abc")
            .setProjectId("test-project")
            .build()
        val app = mock<FirebaseApp> {
            on { name } doReturn "[DEFAULT]"
            on { this.options } doReturn options
        }
        return mock {
            on { this.app } doReturn app
            on { tenantId } doReturn null
        }
    }

    private companion object {
        const val ENFORCE_RESPONSE = """
            {
              "customStrengthOptions": {"minPasswordLength": 10, "containsNumericCharacter": true},
              "enforcementState": "ENFORCE",
              "schemaVersion": 1
            }
        """
    }
}
