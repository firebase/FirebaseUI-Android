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
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import androidx.test.core.app.ApplicationProvider
import com.firebase.ui.auth.configuration.AuthUIConfiguration
import com.firebase.ui.auth.configuration.PasswordRule
import com.firebase.ui.auth.configuration.authUIConfiguration
import com.firebase.ui.auth.configuration.auth_provider.AuthProvider
import com.firebase.ui.auth.configuration.string_provider.AuthUIStringProvider
import com.firebase.ui.auth.configuration.string_provider.DefaultAuthUIStringProvider
import com.firebase.ui.auth.configuration.string_provider.LocalAuthUIStringProvider
import com.firebase.ui.auth.ui.FirebaseAuthTestTags
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
        composeTestRule.onNode(hasText(stringProvider.signupPageTitle.uppercase()) and hasClickAction())
            .assertIsEnabled()
    }

    @Test
    fun `eager form validation does not surface an error on the untouched email field`() {
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
                    onSignUpClick = { }
                )
            }
        }

        composeTestRule.waitForIdle()

        // isFormValid validates every field on each recomposition, so emailValidator.hasError
        // flips to true while email is still empty. That stays invisible because FieldValidator
        // backs its state with a plain var, not a Compose MutableState, so nothing recomposes.
        // Guards a regression: making isFormValid a plain Boolean moves validation ahead of the
        // text fields in the same composition pass, and the error does then show up.
        composeTestRule.onNodeWithText(stringProvider.missingEmailAddress).assertDoesNotExist()

        // Touch ONLY the password field; email is never focused or edited.
        composeTestRule.onNodeWithText(stringProvider.passwordHint)
            .performTextInput("Password123")

        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText(stringProvider.missingEmailAddress).assertDoesNotExist()
    }

    @Test
    fun `typing an invalid email into the email field shows its error`() {
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
                    onSignUpClick = { }
                )
            }
        }

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText(stringProvider.invalidEmailAddress).assertDoesNotExist()

        composeTestRule.onNodeWithText(stringProvider.emailHint)
            .performTextInput("notanemail")

        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText(stringProvider.invalidEmailAddress).assertExists()
    }

    private fun setSignUpContent(provider: AuthProvider.Email) {
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
                    onSignUpClick = { }
                )
            }
        }
        composeTestRule.onNodeWithText(stringProvider.emailHint)
            .performTextInput("test@example.com")
    }

    private fun enterPasswords(password: String) {
        composeTestRule.onNodeWithText(stringProvider.passwordHint)
            .performTextReplacement(password)
        composeTestRule.onNodeWithText(stringProvider.confirmPasswordHint)
            .performTextReplacement(password)
        composeTestRule.waitForIdle()
    }

    @Test
    fun `live password validation honours minimumPasswordLength`() {
        setSignUpContent(
            AuthProvider.Email(
                isDisplayNameRequired = false,
                emailLinkActionCodeSettings = null,
                minimumPasswordLength = 10,
                passwordValidationRules = emptyList()
            )
        )

        // The submit path rejects this password, so the live validator must too.
        enterPasswords("short123")
        composeTestRule.onNodeWithTag(FirebaseAuthTestTags.SignUp.SIGN_UP_BUTTON)
            .assertIsNotEnabled()
        composeTestRule.onNodeWithText(stringProvider.passwordTooShort(10)).assertExists()

        enterPasswords("long123456")
        composeTestRule.onNodeWithTag(FirebaseAuthTestTags.SignUp.SIGN_UP_BUTTON)
            .assertIsEnabled()
        composeTestRule.onNodeWithText(stringProvider.passwordTooShort(10)).assertDoesNotExist()
    }

    @Test
    fun `minimumPasswordLength and a custom length rule surface a single message`() {
        setSignUpContent(
            AuthProvider.Email(
                isDisplayNameRequired = false,
                emailLinkActionCodeSettings = null,
                minimumPasswordLength = 10,
                passwordValidationRules = listOf(PasswordRule.MinimumLength(12))
            )
        )

        // Checked in the submit path's order, so minimumPasswordLength reports first and alone.
        enterPasswords("short123")
        composeTestRule.onAllNodesWithText(stringProvider.passwordTooShort(10))
            .assertCountEquals(1)
        composeTestRule.onNodeWithText(stringProvider.passwordTooShort(12)).assertDoesNotExist()

        enterPasswords("long123456")
        composeTestRule.onNodeWithText(stringProvider.passwordTooShort(10)).assertDoesNotExist()
        composeTestRule.onAllNodesWithText(stringProvider.passwordTooShort(12))
            .assertCountEquals(1)
    }

    // A fresh MinimumLength instance has no equals, so each call re-keys the password validator.
    private fun equivalentConfiguration(): AuthUIConfiguration = authUIConfiguration {
        context = applicationContext
        providers {
            provider(
                AuthProvider.Email(
                    isDisplayNameRequired = false,
                    emailLinkActionCodeSettings = null,
                    minimumPasswordLength = 6,
                    passwordValidationRules = listOf(PasswordRule.MinimumLength(12))
                )
            )
        }
    }

    private fun setSwappableContent(): (AuthUIConfiguration) -> Unit {
        var configuration by mutableStateOf(equivalentConfiguration())
        composeTestRule.setContent {
            CompositionLocalProvider(LocalAuthUIStringProvider provides stringProvider) {
                var password by remember { mutableStateOf("") }
                SignUpUI(
                    configuration = configuration,
                    isLoading = false,
                    displayName = "",
                    email = "",
                    password = password,
                    confirmPassword = "",
                    onDisplayNameChange = { },
                    onEmailChange = { },
                    onPasswordChange = { password = it },
                    onConfirmPasswordChange = { },
                    onSignUpClick = { }
                )
            }
        }
        return { configuration = it }
    }

    private val isInError = SemanticsMatcher.keyIsDefined(SemanticsProperties.Error)

    @Test
    fun `password error survives an equivalent configuration swap`() {
        val swap = setSwappableContent()
        composeTestRule.onNodeWithText(stringProvider.passwordHint).performTextInput("short1")
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag(FirebaseAuthTestTags.SignUp.PASSWORD_FIELD).assert(isInError)
        composeTestRule.onNodeWithText(stringProvider.passwordTooShort(12)).assertExists()

        swap(equivalentConfiguration())
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag(FirebaseAuthTestTags.SignUp.PASSWORD_FIELD).assert(isInError)
        composeTestRule.onNodeWithText(stringProvider.passwordTooShort(12)).assertExists()
    }

    @Test
    fun `cleared password error survives an equivalent configuration swap`() {
        val swap = setSwappableContent()
        composeTestRule.onNodeWithText(stringProvider.passwordHint).performTextInput("short1")
        composeTestRule.onNodeWithTag(FirebaseAuthTestTags.SignUp.PASSWORD_FIELD)
            .performTextClearance()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag(FirebaseAuthTestTags.SignUp.PASSWORD_FIELD).assert(isInError)
        composeTestRule.onNodeWithText(stringProvider.invalidPassword).assertExists()

        // The field is empty again, so only the edit history tells it apart from an untouched one.
        swap(equivalentConfiguration())
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag(FirebaseAuthTestTags.SignUp.PASSWORD_FIELD).assert(isInError)
        composeTestRule.onNodeWithText(stringProvider.invalidPassword).assertExists()
    }

    @Test
    fun `untouched password field gains no error from a configuration swap`() {
        val swap = setSwappableContent()
        composeTestRule.waitForIdle()

        swap(equivalentConfiguration())
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag(FirebaseAuthTestTags.SignUp.PASSWORD_FIELD)
            .assert(!isInError)
        composeTestRule.onNodeWithText(stringProvider.invalidPassword).assertDoesNotExist()
    }
}
