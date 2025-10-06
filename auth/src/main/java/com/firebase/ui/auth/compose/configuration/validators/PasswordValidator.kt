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

import com.firebase.ui.auth.compose.configuration.string_provider.AuthUIStringProvider
import com.firebase.ui.auth.compose.configuration.PasswordRule

internal class PasswordValidator(
    override val stringProvider: AuthUIStringProvider,
    private val rules: List<PasswordRule>
) : FieldValidator {
    private var _validationStatus = FieldValidationStatus(hasError = false, errorMessage = null)

    override val hasError: Boolean
        get() = _validationStatus.hasError

    override val errorMessage: String
        get() = _validationStatus.errorMessage ?: ""

    override fun validate(value: String): Boolean {
        if (value.isEmpty()) {
            _validationStatus = FieldValidationStatus(
                hasError = true,
                errorMessage = stringProvider.invalidPassword
            )
            return false
        }

        for (rule in rules) {
            if (!rule.isValid(value)) {
                _validationStatus = FieldValidationStatus(
                    hasError = true,
                    errorMessage = rule.getErrorMessage(stringProvider)
                )
                return false
            }
        }

        _validationStatus = FieldValidationStatus(hasError = false, errorMessage = null)
        return true
    }
}
