package com.firebase.ui.auth.configuration

/**
 * A sealed class representing a set of validation rules that can be applied to a password field,
 * typically within the [AuthProvider.EmailAuthProvider] configuration.
 */
sealed class PasswordRule {
    /**
     * Requires the password to have at least a certain number of characters.
     */
    data class MinimumLength(val value: Int) : PasswordRule()

    /**
     * Requires the password to contain at least one uppercase letter (A-Z).
     */
    object RequireUppercase : PasswordRule()

    /**
     * Requires the password to contain at least one lowercase letter (a-z).
     */
    object RequireLowercase: PasswordRule()

    /**
     * Requires the password to contain at least one numeric digit (0-9).
     */
    object RequireDigit: PasswordRule()

    /**
     * Requires the password to contain at least one special character (e.g., !@#$%^&*).
     */
    object RequireSpecialCharacter: PasswordRule()

    /**
     * Defines a custom validation rule using a regular expression and provides a specific error
     * message on failure.
     */
    data class Custom(val regex: Regex, val errorMessage: String)
}