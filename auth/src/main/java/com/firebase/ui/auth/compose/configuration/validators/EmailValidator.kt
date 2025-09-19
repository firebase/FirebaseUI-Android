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

import com.firebase.ui.auth.compose.configuration.AuthUIStringProvider

internal class EmailValidator(override val stringProvider: AuthUIStringProvider) : FieldValidator {
    override var validationStatus: ValidationStatus = ValidationStatus(hasError = false)
        private set

    override fun validate(value: String): ValidationStatus {
        val result = when {
            value.isEmpty() -> {
                ValidationStatus(
                    hasError = true,
                    errorMessage = stringProvider.missingEmailAddress
                )
            }

            !android.util.Patterns.EMAIL_ADDRESS.matcher(value).matches() -> {
                ValidationStatus(
                    hasError = true,
                    errorMessage = stringProvider.invalidEmailAddress
                )
            }

            else -> ValidationStatus(hasError = false)
        }

        validationStatus = result
        return result
    }
}
