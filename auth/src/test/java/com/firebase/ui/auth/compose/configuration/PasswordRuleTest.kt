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

package com.firebase.ui.auth.compose.configuration

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.firebase.ui.auth.R
import com.firebase.ui.auth.compose.configuration.stringprovider.AuthUIStringProvider
import com.firebase.ui.auth.compose.configuration.stringprovider.DefaultAuthUIStringProvider
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Unit tests for [PasswordRule] implementations covering validation logic
 * and error message generation for each password rule type.
 *
 * @suppress Internal test class
 */
@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class PasswordRuleTest {

    private lateinit var stringProvider: AuthUIStringProvider

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        stringProvider = DefaultAuthUIStringProvider(context)
    }

    // =============================================================================================
    // MinimumLength Rule Tests
    // =============================================================================================

    @Test
    fun `MinimumLength isValid returns true for password meeting length requirement`() {
        val rule = PasswordRule.MinimumLength(8)

        val isValid = rule.isValid("password123")

        assertThat(isValid).isTrue()
    }

    @Test
    fun `MinimumLength isValid returns false for password shorter than requirement`() {
        val rule = PasswordRule.MinimumLength(8)

        val isValid = rule.isValid("short")

        assertThat(isValid).isFalse()
    }

    @Test
    fun `MinimumLength getErrorMessage returns formatted message`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val rule = PasswordRule.MinimumLength(10)

        val message = rule.getErrorMessage(stringProvider)

        assertThat(message).isEqualTo(context.getString(R.string.fui_error_password_too_short, 10))
    }

    // =============================================================================================
    // RequireUppercase Rule Tests
    // =============================================================================================

    @Test
    fun `RequireUppercase isValid returns true for password with uppercase letter`() {
        val rule = PasswordRule.RequireUppercase

        val isValid = rule.isValid("Password123")

        assertThat(isValid).isTrue()
    }

    @Test
    fun `RequireUppercase isValid returns false for password without uppercase letter`() {
        val rule = PasswordRule.RequireUppercase

        val isValid = rule.isValid("password123")

        assertThat(isValid).isFalse()
    }

    @Test
    fun `RequireUppercase getErrorMessage returns correct message`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val rule = PasswordRule.RequireUppercase

        val message = rule.getErrorMessage(stringProvider)

        assertThat(message).isEqualTo(context.getString(R.string.fui_error_password_missing_uppercase))
    }

    // =============================================================================================
    // RequireLowercase Rule Tests
    // =============================================================================================

    @Test
    fun `RequireLowercase isValid returns true for password with lowercase letter`() {
        val rule = PasswordRule.RequireLowercase

        val isValid = rule.isValid("Password123")

        assertThat(isValid).isTrue()
    }

    @Test
    fun `RequireLowercase isValid returns false for password without lowercase letter`() {
        val rule = PasswordRule.RequireLowercase

        val isValid = rule.isValid("PASSWORD123")

        assertThat(isValid).isFalse()
    }

    @Test
    fun `RequireLowercase getErrorMessage returns correct message`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val rule = PasswordRule.RequireLowercase

        val message = rule.getErrorMessage(stringProvider)

        assertThat(message).isEqualTo(context.getString(R.string.fui_error_password_missing_lowercase))
    }

    // =============================================================================================
    // RequireDigit Rule Tests
    // =============================================================================================

    @Test
    fun `RequireDigit isValid returns true for password with digit`() {
        val rule = PasswordRule.RequireDigit

        val isValid = rule.isValid("Password123")

        assertThat(isValid).isTrue()
    }

    @Test
    fun `RequireDigit isValid returns false for password without digit`() {
        val rule = PasswordRule.RequireDigit

        val isValid = rule.isValid("Password")

        assertThat(isValid).isFalse()
    }

    @Test
    fun `RequireDigit getErrorMessage returns correct message`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val rule = PasswordRule.RequireDigit

        val message = rule.getErrorMessage(stringProvider)

        assertThat(message).isEqualTo(context.getString(R.string.fui_error_password_missing_digit))
    }

    // =============================================================================================
    // RequireSpecialCharacter Rule Tests
    // =============================================================================================

    @Test
    fun `RequireSpecialCharacter isValid returns true for password with special character`() {
        val rule = PasswordRule.RequireSpecialCharacter

        val isValid = rule.isValid("Password123!")

        assertThat(isValid).isTrue()
    }

    @Test
    fun `RequireSpecialCharacter isValid returns false for password without special character`() {
        val rule = PasswordRule.RequireSpecialCharacter

        val isValid = rule.isValid("Password123")

        assertThat(isValid).isFalse()
    }

    @Test
    fun `RequireSpecialCharacter getErrorMessage returns correct message`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val rule = PasswordRule.RequireSpecialCharacter

        val message = rule.getErrorMessage(stringProvider)

        assertThat(message).isEqualTo(context.getString(R.string.fui_error_password_missing_special_character))
    }

    @Test
    fun `RequireSpecialCharacter validates various special characters`() {
        val rule = PasswordRule.RequireSpecialCharacter
        val specialChars = "!@#$%^&*()_+-=[]{}|;:,.<>?"

        specialChars.forEach { char ->
            val isValid = rule.isValid("Password123$char")
            assertThat(isValid).isTrue()
        }
    }

    // =============================================================================================
    // Custom Rule Tests
    // =============================================================================================

    @Test
    fun `Custom rule isValid works with provided regex`() {
        val rule = PasswordRule.Custom(
            regex = Regex("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)[a-zA-Z\\d]{8,}$"),
            errorMessage = "Custom validation failed"
        )

        val validPassword = rule.isValid("Password123")
        val invalidPassword = rule.isValid("weak")

        assertThat(validPassword).isTrue()
        assertThat(invalidPassword).isFalse()
    }

    @Test
    fun `Custom rule getErrorMessage returns custom message`() {
        val customMessage = "Custom validation failed"
        val rule = PasswordRule.Custom(
            regex = Regex(".*"),
            errorMessage = customMessage
        )

        val message = rule.getErrorMessage(stringProvider)

        assertThat(message).isEqualTo(customMessage)
    }

    @Test
    fun `Custom rule with complex regex works correctly`() {
        // Must contain at least one letter, one number, and be 6+ characters
        val rule = PasswordRule.Custom(
            regex = Regex("^(?=.*[a-zA-Z])(?=.*\\d).{6,}$"),
            errorMessage = "Must contain letter and number, min 6 chars"
        )

        assertThat(rule.isValid("abc123")).isTrue()
        assertThat(rule.isValid("password1")).isTrue()
        assertThat(rule.isValid("123456")).isFalse()  // No letter
        assertThat(rule.isValid("abcdef")).isFalse()  // No number
        assertThat(rule.isValid("abc12")).isFalse()   // Too short
    }

    // =============================================================================================
    // Rule Extensibility Tests
    // =============================================================================================

    @Test
    fun `custom password rule by extending PasswordRule works`() {
        val customRule = object : PasswordRule() {
            override fun isValid(password: String): Boolean {
                return password.contains("test")
            }

            override fun getErrorMessage(stringProvider: AuthUIStringProvider): String {
                return "Password must contain 'test'"
            }
        }

        val validResult = customRule.isValid("testing123")
        val invalidResult = customRule.isValid("invalid")
        val errorMessage = customRule.getErrorMessage(stringProvider)

        assertThat(validResult).isTrue()
        assertThat(invalidResult).isFalse()
        assertThat(errorMessage).isEqualTo("Password must contain 'test'")
    }

    @Test
    fun `multiple custom rules can be created independently`() {
        val rule1 = object : PasswordRule() {
            override fun isValid(password: String): Boolean = password.startsWith("prefix")
            override fun getErrorMessage(stringProvider: AuthUIStringProvider): String = "Must start with 'prefix'"
        }

        val rule2 = object : PasswordRule() {
            override fun isValid(password: String): Boolean = password.endsWith("suffix")
            override fun getErrorMessage(stringProvider: AuthUIStringProvider): String = "Must end with 'suffix'"
        }

        assertThat(rule1.isValid("prefixPassword")).isTrue()
        assertThat(rule1.isValid("passwordsuffix")).isFalse()

        assertThat(rule2.isValid("passwordsuffix")).isTrue()
        assertThat(rule2.isValid("prefixPassword")).isFalse()

        assertThat(rule1.getErrorMessage(stringProvider)).isEqualTo("Must start with 'prefix'")
        assertThat(rule2.getErrorMessage(stringProvider)).isEqualTo("Must end with 'suffix'")
    }

    // =============================================================================================
    // Edge Case Tests
    // =============================================================================================

    @Test
    fun `all rules handle empty password correctly`() {
        val rules = listOf(
            PasswordRule.MinimumLength(1),
            PasswordRule.RequireUppercase,
            PasswordRule.RequireLowercase,
            PasswordRule.RequireDigit,
            PasswordRule.RequireSpecialCharacter
        )

        rules.forEach { rule ->
            val isValid = rule.isValid("")
            assertThat(isValid).isFalse()
        }
    }

    @Test
    fun `MinimumLength with zero length allows any password`() {
        val rule = PasswordRule.MinimumLength(0)

        assertThat(rule.isValid("")).isTrue()
        assertThat(rule.isValid("any")).isTrue()
    }
}