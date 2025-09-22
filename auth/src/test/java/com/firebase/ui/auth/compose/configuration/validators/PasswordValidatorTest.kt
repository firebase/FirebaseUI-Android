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

package com.firebase.ui.auth.compose.configuration.validators

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.firebase.ui.auth.R
import com.firebase.ui.auth.compose.configuration.DefaultAuthUIStringProvider
import com.firebase.ui.auth.compose.configuration.PasswordRule
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Integration tests for [PasswordValidator] covering password validation logic,
 * password rule enforcement, error state management, and integration with
 * [DefaultAuthUIStringProvider] using real Android string resources.
 *
 * @suppress Internal test class
 */
@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class PasswordValidatorTest {

    private lateinit var stringProvider: DefaultAuthUIStringProvider
    private lateinit var passwordValidator: PasswordValidator

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        stringProvider = DefaultAuthUIStringProvider(context)
    }

    // =============================================================================================
    // Initial State Tests
    // =============================================================================================

    @Test
    fun `validator initial state has no error`() {
        passwordValidator = PasswordValidator(stringProvider, emptyList())

        assertThat(passwordValidator.hasError).isFalse()
        assertThat(passwordValidator.errorMessage).isEmpty()
    }

    // =============================================================================================
    // Empty Password Validation Tests
    // =============================================================================================

    @Test
    fun `validate returns false and sets error for empty password`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        passwordValidator = PasswordValidator(stringProvider, emptyList())

        val isValid = passwordValidator.validate("")

        assertThat(isValid).isFalse()
        assertThat(passwordValidator.hasError).isTrue()
        assertThat(passwordValidator.errorMessage)
            .isEqualTo(context.getString(R.string.fui_error_invalid_password))
    }

    // =============================================================================================
    // Minimum Length Rule Tests
    // =============================================================================================

    @Test
    fun `validate returns false for password shorter than minimum length`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val rules = listOf(PasswordRule.MinimumLength(8))
        passwordValidator = PasswordValidator(stringProvider, rules)

        val isValid = passwordValidator.validate("short")

        assertThat(isValid).isFalse()
        assertThat(passwordValidator.hasError).isTrue()
        assertThat(passwordValidator.errorMessage)
            .isEqualTo(context.getString(R.string.fui_error_password_too_short, 8))
    }

    @Test
    fun `validate returns true for password meeting minimum length`() {
        val rules = listOf(PasswordRule.MinimumLength(8))
        passwordValidator = PasswordValidator(stringProvider, rules)

        val isValid = passwordValidator.validate("password123")

        assertThat(isValid).isTrue()
        assertThat(passwordValidator.hasError).isFalse()
        assertThat(passwordValidator.errorMessage).isEmpty()
    }

    // =============================================================================================
    // Character Requirement Tests
    // =============================================================================================

    @Test
    fun `validate returns false for password missing uppercase letter`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val rules = listOf(PasswordRule.RequireUppercase)
        passwordValidator = PasswordValidator(stringProvider, rules)

        val isValid = passwordValidator.validate("password123")

        assertThat(isValid).isFalse()
        assertThat(passwordValidator.hasError).isTrue()
        assertThat(passwordValidator.errorMessage)
            .isEqualTo(context.getString(R.string.fui_error_password_missing_uppercase))
    }

    @Test
    fun `validate returns true for password with uppercase letter`() {
        val rules = listOf(PasswordRule.RequireUppercase)
        passwordValidator = PasswordValidator(stringProvider, rules)

        val isValid = passwordValidator.validate("Password123")

        assertThat(isValid).isTrue()
        assertThat(passwordValidator.hasError).isFalse()
        assertThat(passwordValidator.errorMessage).isEmpty()
    }

    @Test
    fun `validate returns false for password missing lowercase letter`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val rules = listOf(PasswordRule.RequireLowercase)
        passwordValidator = PasswordValidator(stringProvider, rules)

        val isValid = passwordValidator.validate("PASSWORD123")

        assertThat(isValid).isFalse()
        assertThat(passwordValidator.hasError).isTrue()
        assertThat(passwordValidator.errorMessage)
            .isEqualTo(context.getString(R.string.fui_error_password_missing_lowercase))
    }

    @Test
    fun `validate returns false for password missing digit`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val rules = listOf(PasswordRule.RequireDigit)
        passwordValidator = PasswordValidator(stringProvider, rules)

        val isValid = passwordValidator.validate("Password")

        assertThat(isValid).isFalse()
        assertThat(passwordValidator.hasError).isTrue()
        assertThat(passwordValidator.errorMessage)
            .isEqualTo(context.getString(R.string.fui_error_password_missing_digit))
    }

    @Test
    fun `validate returns false for password missing special character`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val rules = listOf(PasswordRule.RequireSpecialCharacter)
        passwordValidator = PasswordValidator(stringProvider, rules)

        val isValid = passwordValidator.validate("Password123")

        assertThat(isValid).isFalse()
        assertThat(passwordValidator.hasError).isTrue()
        assertThat(passwordValidator.errorMessage)
            .isEqualTo(context.getString(R.string.fui_error_password_missing_special_character))
    }

    // =============================================================================================
    // Multiple Rules Tests
    // =============================================================================================

    @Test
    fun `validate returns false and shows first failing rule error`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val rules = listOf(
            PasswordRule.MinimumLength(8),
            PasswordRule.RequireUppercase,
            PasswordRule.RequireDigit
        )
        passwordValidator = PasswordValidator(stringProvider, rules)

        val isValid = passwordValidator.validate("short")

        assertThat(isValid).isFalse()
        assertThat(passwordValidator.hasError).isTrue()
        // Should show the first failing rule (MinimumLength)
        assertThat(passwordValidator.errorMessage)
            .isEqualTo(context.getString(R.string.fui_error_password_too_short, 8))
    }

    @Test
    fun `validate returns true for password meeting all rules`() {
        val rules = listOf(
            PasswordRule.MinimumLength(8),
            PasswordRule.RequireUppercase,
            PasswordRule.RequireLowercase,
            PasswordRule.RequireDigit,
            PasswordRule.RequireSpecialCharacter
        )
        passwordValidator = PasswordValidator(stringProvider, rules)

        val isValid = passwordValidator.validate("Password123!")

        assertThat(isValid).isTrue()
        assertThat(passwordValidator.hasError).isFalse()
        assertThat(passwordValidator.errorMessage).isEmpty()
    }

    // =============================================================================================
    // Custom Rule Tests
    // =============================================================================================

    @Test
    fun `validate works with custom regex rule`() {
        val customRule = PasswordRule.Custom(
            // Valid (has upper, lower, digit, 8+ chars, only letters/digits)
            regex = Regex("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)[a-zA-Z\\d]{8,}$"),
            errorMessage = "Custom validation failed"
        )
        val rules = listOf(customRule)
        passwordValidator = PasswordValidator(stringProvider, rules)

        val isValid = passwordValidator.validate("Password123")

        assertThat(isValid).isTrue()
        assertThat(passwordValidator.hasError).isFalse()
        assertThat(passwordValidator.errorMessage).isEmpty()
    }

    @Test
    fun `validate returns custom error message for failing custom rule`() {
        val customRule = PasswordRule.Custom(
            // Valid (has upper, lower, digit, 8+ chars, only letters/digits)
            regex = Regex("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)[a-zA-Z\\d]{8,}$"),
            errorMessage = "Custom validation failed"
        )
        val rules = listOf(customRule)
        passwordValidator = PasswordValidator(stringProvider, rules)

        val isValid = passwordValidator.validate("weak")

        assertThat(isValid).isFalse()
        assertThat(passwordValidator.hasError).isTrue()
        assertThat(passwordValidator.errorMessage).isEqualTo("Custom validation failed")
    }

    // =============================================================================================
    // Error State Management Tests
    // =============================================================================================

    @Test
    fun `validate clears previous error when password becomes valid`() {
        val rules = listOf(PasswordRule.MinimumLength(8))
        passwordValidator = PasswordValidator(stringProvider, rules)

        passwordValidator.validate("short")
        assertThat(passwordValidator.hasError).isTrue()

        val isValid = passwordValidator.validate("longenough")

        assertThat(isValid).isTrue()
        assertThat(passwordValidator.hasError).isFalse()
        assertThat(passwordValidator.errorMessage).isEmpty()
    }
}