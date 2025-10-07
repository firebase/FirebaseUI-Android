package com.firebase.ui.auth.compose.configuration.validators

import com.firebase.ui.auth.compose.configuration.string_provider.AuthUIStringProvider

internal class GeneralFieldValidator(
    override val stringProvider: AuthUIStringProvider,
    val isValid: ((String) -> Boolean)? = null,
    val customMessage: String? = null,
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
                errorMessage = stringProvider.requiredField
            )
            return false
        }

        if (isValid != null && !isValid(value)) {
            _validationStatus = FieldValidationStatus(
                hasError = true,
                errorMessage = customMessage
            )
            return false
        }

        _validationStatus = FieldValidationStatus(hasError = false, errorMessage = null)
        return true
    }
}