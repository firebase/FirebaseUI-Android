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
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Integration tests for [EmailValidator] covering email validation logic,
 * error state management, and integration with [DefaultAuthUIStringProvider]
 * using real Android string resources.
 *
 * @suppress Internal test class
 */
@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class EmailValidatorTest {

    @Mock
    private lateinit var stringProvider: DefaultAuthUIStringProvider

    private lateinit var emailValidator: EmailValidator

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        stringProvider = DefaultAuthUIStringProvider(context)
        emailValidator = EmailValidator(stringProvider)
    }

    // =============================================================================================
    // Initial State Tests
    // =============================================================================================

    @Test
    fun `validator initial state has no error`() {
        assertThat(emailValidator.hasError).isFalse()
        assertThat(emailValidator.errorMessage).isEmpty()
    }

    // =============================================================================================
    // Validation Logic Tests
    // =============================================================================================

    @Test
    fun `validate returns false and sets error for empty email`() {
        val context = ApplicationProvider.getApplicationContext<Context>()

        val isValid = emailValidator.validate("")

        assertThat(isValid).isFalse()
        assertThat(emailValidator.hasError).isTrue()
        assertThat(emailValidator.errorMessage)
            .isEqualTo(context.getString(R.string.fui_missing_email_address))
    }

    @Test
    fun `validate returns false and sets error for invalid email format`() {
        val context = ApplicationProvider.getApplicationContext<Context>()

        val isValid = emailValidator.validate("invalid-email")

        assertThat(isValid).isFalse()
        assertThat(emailValidator.hasError).isTrue()
        assertThat(emailValidator.errorMessage)
            .isEqualTo(context.getString(R.string.fui_invalid_email_address))
    }

    @Test
    fun `validate returns true and clears error for valid email`() {
        val isValid = emailValidator.validate("test@example.com")

        assertThat(isValid).isTrue()
        assertThat(emailValidator.hasError).isFalse()
        assertThat(emailValidator.errorMessage).isEmpty()
    }

    @Test
    fun `validate clears previous error when valid email provided`() {
        emailValidator.validate("invalid")
        assertThat(emailValidator.hasError).isTrue()

        val isValid = emailValidator.validate("valid@example.com")

        assertThat(isValid).isTrue()
        assertThat(emailValidator.hasError).isFalse()
        assertThat(emailValidator.errorMessage).isEmpty()
    }
}