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

package com.firebase.ui.auth.compose.ui.components

import android.content.Context
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ApplicationProvider
import com.firebase.ui.auth.compose.configuration.PasswordRule
import com.firebase.ui.auth.compose.configuration.string_provider.AuthUIStringProvider
import com.firebase.ui.auth.compose.configuration.string_provider.DefaultAuthUIStringProvider
import com.firebase.ui.auth.compose.configuration.validators.EmailValidator
import com.firebase.ui.auth.compose.configuration.validators.PasswordValidator
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Unit tests for [AuthTextField] covering UI interactions, validation,
 * password visibility toggle, and error states.
 *
 * @suppress Internal test class
 */
@Config(sdk = [34])
@RunWith(RobolectricTestRunner::class)
class AuthTextFieldTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var context: Context
    private lateinit var stringProvider: AuthUIStringProvider

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        stringProvider = DefaultAuthUIStringProvider(context)
    }

    // =============================================================================================
    // Basic UI Tests
    // =============================================================================================

    @Test
    fun `AuthTextField displays correctly with basic configuration`() {
        composeTestRule.setContent {
            AuthTextField(
                value = "",
                onValueChange = { },
                label = { Text("Name") }
            )
        }

        composeTestRule
            .onNodeWithText("Name")
            .assertIsDisplayed()
    }

    @Test
    fun `AuthTextField displays initial value`() {
        composeTestRule.setContent {
            AuthTextField(
                value = "test@example.com",
                onValueChange = { },
                label = { Text("Email") }
            )
        }

        composeTestRule
            .onNodeWithText("Email")
            .assertIsDisplayed()

        composeTestRule
            .onNodeWithText("test@example.com")
            .assertIsDisplayed()
    }

    @Test
    fun `AuthTextField updates value on text input`() {
        composeTestRule.setContent {
            val textValue = remember { mutableStateOf("") }
            AuthTextField(
                value = textValue.value,
                onValueChange = { textValue.value = it },
                label = { Text("Email") }
            )
        }

        composeTestRule
            .onNodeWithText("Email")
            .performTextInput("test@example.com")

        composeTestRule
            .onNodeWithText("Email")
            .assertIsDisplayed()

        composeTestRule
            .onNodeWithText("test@example.com")
            .assertIsDisplayed()
    }

    @Test
    fun `AuthTextField respects enabled state`() {
        composeTestRule.setContent {
            AuthTextField(
                value = "",
                onValueChange = { },
                label = { Text("Email") },
                enabled = false
            )
        }

        composeTestRule
            .onNodeWithText("Email")
            .assertIsNotEnabled()
    }

    @Test
    fun `AuthTextField is enabled by default`() {
        composeTestRule.setContent {
            AuthTextField(
                value = "",
                onValueChange = { },
                label = { Text("Email") }
            )
        }

        composeTestRule
            .onNodeWithText("Email")
            .assertIsEnabled()
    }

    @Test
    fun `AuthTextField displays leading icon when provided`() {
        composeTestRule.setContent {
            AuthTextField(
                value = "",
                onValueChange = { },
                label = { Text("Email") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Email,
                        contentDescription = "Email Icon"
                    )
                }
            )
        }

        composeTestRule
            .onNodeWithContentDescription("Email Icon")
            .assertIsDisplayed()
    }

    @Test
    fun `AuthTextField displays custom trailing icon when provided`() {
        composeTestRule.setContent {
            AuthTextField(
                value = "",
                onValueChange = { },
                label = { Text("Email") },
                trailingIcon = {
                    Icon(
                        imageVector = Icons.Default.Email,
                        contentDescription = "Custom Trailing Icon"
                    )
                }
            )
        }

        composeTestRule
            .onNodeWithContentDescription("Custom Trailing Icon")
            .assertIsDisplayed()
    }

    // =============================================================================================
    // Validation Tests
    // =============================================================================================

    @Test
    fun `AuthTextField validates email correctly with EmailValidator`() {
        composeTestRule.setContent {
            val emailValidator = remember {
                EmailValidator(stringProvider = stringProvider)
            }
            val textValue = remember { mutableStateOf("") }
            AuthTextField(
                value = textValue.value,
                onValueChange = { textValue.value = it },
                label = { Text("Email") },
                validator = emailValidator
            )
        }

        composeTestRule
            .onNodeWithText("Email")
            .performTextInput("invalid-email")

        composeTestRule.waitForIdle()

        composeTestRule
            .onNodeWithText(stringProvider.invalidEmailAddress)
            .assertIsDisplayed()

        composeTestRule
            .onNodeWithText("Email")
            .performTextClearance()

        composeTestRule.waitForIdle()

        composeTestRule
            .onNodeWithText(stringProvider.missingEmailAddress)
            .assertIsDisplayed()

        composeTestRule
            .onNodeWithText("Email")
            .performTextInput("valid@example.com")

        composeTestRule.waitForIdle()

        composeTestRule
            .onNodeWithText(stringProvider.missingEmailAddress)
            .assertIsNotDisplayed()
        composeTestRule
            .onNodeWithText(stringProvider.invalidEmailAddress)
            .assertIsNotDisplayed()
    }

    @Test
    fun `AuthTextField displays custom error message when provided`() {
        composeTestRule.setContent {
            val emailValidator = remember {
                EmailValidator(stringProvider = stringProvider)
            }
            val textValue = remember { mutableStateOf("") }
            AuthTextField(
                value = textValue.value,
                onValueChange = { textValue.value = it },
                label = { Text("Email") },
                validator = emailValidator,
                errorMessage = "Custom error message"
            )
        }

        composeTestRule
            .onNodeWithText("Email")
            .performTextInput("invalid")

        composeTestRule.waitForIdle()

        composeTestRule
            .onNodeWithText("Custom error message")
            .assertIsDisplayed()
    }

    @Test
    fun `AuthTextField validates password with PasswordValidator`() {
        composeTestRule.setContent {
            val passwordValidator = remember {
                PasswordValidator(
                    stringProvider = stringProvider,
                    rules = listOf(
                        PasswordRule.MinimumLength(8),
                        PasswordRule.RequireUppercase,
                        PasswordRule.RequireLowercase
                    )
                )
            }
            val textValue = remember { mutableStateOf("") }
            AuthTextField(
                value = textValue.value,
                onValueChange = { textValue.value = it },
                label = { Text("Password") },
                validator = passwordValidator
            )
        }

        composeTestRule
            .onNodeWithText("Password")
            .performTextInput("short")

        composeTestRule.waitForIdle()

        composeTestRule
            .onNodeWithText(stringProvider.passwordTooShort.format(8))
            .assertIsDisplayed()

        composeTestRule
            .onNodeWithText("Password")
            .performTextClearance()

        composeTestRule.waitForIdle()

        composeTestRule
            .onNodeWithText(stringProvider.invalidPassword)
            .assertIsDisplayed()

        composeTestRule
            .onNodeWithText("Password")
            .performTextInput("pass@1234")

        composeTestRule.waitForIdle()

        composeTestRule
            .onNodeWithText(stringProvider.passwordMissingUppercase)
            .assertIsDisplayed()

        composeTestRule
            .onNodeWithText("Password")
            .performTextInput("ValidPass123")

        composeTestRule.waitForIdle()

        composeTestRule
            .onNodeWithText(stringProvider.passwordTooShort.format(8))
            .assertIsNotDisplayed()
        composeTestRule
            .onNodeWithText(stringProvider.passwordMissingLowercase)
            .assertIsNotDisplayed()
        composeTestRule
            .onNodeWithText(stringProvider.passwordMissingUppercase)
            .assertIsNotDisplayed()
    }

    // =============================================================================================
    // Password Visibility Toggle Tests
    // =============================================================================================

    @Test
    fun `AuthTextField shows password visibility toggle when isSecureTextField`() {
        composeTestRule.setContent {
            AuthTextField(
                value = "password123",
                onValueChange = { },
                label = { Text("Password") },
                isSecureTextField = true,
                validator = PasswordValidator(
                    stringProvider = stringProvider,
                    rules = emptyList()
                )
            )
        }

        composeTestRule
            .onNodeWithContentDescription("Show password")
            .assertIsDisplayed()
    }

    @Test
    fun `AuthTextField toggles password visibility when icon is clicked`() {
        composeTestRule.setContent {
            AuthTextField(
                value = "password123",
                onValueChange = { },
                label = { Text("Password") },
                isSecureTextField = true,
                validator = PasswordValidator(
                    stringProvider = stringProvider,
                    rules = emptyList()
                )
            )
        }

        composeTestRule
            .onNodeWithContentDescription("Show password")
            .assertIsDisplayed()
            .performClick()

        composeTestRule.waitForIdle()

        composeTestRule
            .onNodeWithContentDescription("Hide password")
            .assertIsDisplayed()
    }

    @Test
    fun `AuthTextField hides password visibility toggle for non-password fields`() {
        composeTestRule.setContent {
            AuthTextField(
                value = "test@example.com",
                onValueChange = { },
                label = { Text("Email") },
            )
        }

        composeTestRule
            .onNodeWithContentDescription("Show password")
            .assertDoesNotExist()

        composeTestRule
            .onNodeWithContentDescription("Hide password")
            .assertDoesNotExist()
    }

    @Test
    fun `AuthTextField respects custom trailing icon over password toggle`() {
        composeTestRule.setContent {
            AuthTextField(
                value = "password123",
                onValueChange = { },
                label = { Text("Password") },
                validator = PasswordValidator(
                    stringProvider = stringProvider,
                    rules = emptyList()
                ),
                trailingIcon = {
                    Icon(
                        imageVector = Icons.Default.Email,
                        contentDescription = "Custom Icon"
                    )
                }
            )
        }

        composeTestRule
            .onNodeWithContentDescription("Custom Icon")
            .assertIsDisplayed()

        composeTestRule
            .onNodeWithContentDescription("Show password")
            .assertDoesNotExist()
    }
}
