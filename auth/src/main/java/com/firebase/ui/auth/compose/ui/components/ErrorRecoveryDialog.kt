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

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.window.DialogProperties
import com.firebase.ui.auth.compose.AuthException
import com.firebase.ui.auth.compose.configuration.string_provider.AuthUIStringProvider

/**
 * A composable dialog for displaying authentication errors with recovery options.
 *
 * This dialog provides friendly error messages and actionable recovery suggestions
 * based on the specific [AuthException] type. It integrates with [AuthUIStringProvider]
 * for localization support.
 *
 * **Example usage:**
 * ```kotlin
 * var showError by remember { mutableStateOf<AuthException?>(null) }
 *
 * if (showError != null) {
 *     ErrorRecoveryDialog(
 *         error = showError!!,
 *         stringProvider = stringProvider,
 *         onRetry = {
 *             showError = null
 *             // Retry authentication operation
 *         },
 *         onDismiss = {
 *             showError = null
 *         }
 *     )
 * }
 * ```
 *
 * @param error The [AuthException] to display recovery information for
 * @param stringProvider The [AuthUIStringProvider] for localized strings
 * @param onRetry Callback invoked when the user taps the retry action
 * @param onDismiss Callback invoked when the user dismisses the dialog
 * @param modifier Optional [Modifier] for the dialog
 * @param onRecover Optional callback for custom recovery actions based on the exception type
 * @param properties Optional [DialogProperties] for dialog configuration
 *
 * @since 10.0.0
 */
@Composable
fun ErrorRecoveryDialog(
    error: AuthException,
    stringProvider: AuthUIStringProvider,
    onRetry: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    onRecover: ((AuthException) -> Unit)? = null,
    properties: DialogProperties = DialogProperties()
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringProvider.errorDialogTitle,
                style = MaterialTheme.typography.headlineSmall
            )
        },
        text = {
            Text(
                text = getRecoveryMessage(error, stringProvider),
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Start
            )
        },
        confirmButton = {
            if (isRecoverable(error)) {
                TextButton(
                    onClick = {
                        onRecover?.invoke(error) ?: onRetry()
                    }
                ) {
                    Text(
                        text = getRecoveryActionText(error, stringProvider),
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = stringProvider.dismissAction,
                    style = MaterialTheme.typography.labelLarge
                )
            }
        },
        modifier = modifier,
        properties = properties
    )
}

/**
 * Gets the appropriate recovery message for the given [AuthException].
 *
 * @param error The [AuthException] to get the message for
 * @param stringProvider The [AuthUIStringProvider] for localized strings
 * @return The localized recovery message
 */
private fun getRecoveryMessage(
    error: AuthException,
    stringProvider: AuthUIStringProvider
): String {
    return when (error) {
        is AuthException.NetworkException -> stringProvider.networkErrorRecoveryMessage
        is AuthException.InvalidCredentialsException -> stringProvider.invalidCredentialsRecoveryMessage
        is AuthException.UserNotFoundException -> stringProvider.userNotFoundRecoveryMessage
        is AuthException.WeakPasswordException -> {
            // Include specific reason if available
            val baseMessage = stringProvider.weakPasswordRecoveryMessage
            error.reason?.let { reason ->
                "$baseMessage\n\nReason: $reason"
            } ?: baseMessage
        }
        is AuthException.EmailAlreadyInUseException -> {
            // Include email if available
            val baseMessage = stringProvider.emailAlreadyInUseRecoveryMessage
            error.email?.let { email ->
                "$baseMessage ($email)"
            } ?: baseMessage
        }
        is AuthException.TooManyRequestsException -> stringProvider.tooManyRequestsRecoveryMessage
        is AuthException.MfaRequiredException -> stringProvider.mfaRequiredRecoveryMessage
        is AuthException.AccountLinkingRequiredException -> stringProvider.accountLinkingRequiredRecoveryMessage
        is AuthException.AuthCancelledException -> stringProvider.authCancelledRecoveryMessage
        is AuthException.UnknownException -> stringProvider.unknownErrorRecoveryMessage
        else -> stringProvider.unknownErrorRecoveryMessage
    }
}

/**
 * Gets the appropriate recovery action text for the given [AuthException].
 *
 * @param error The [AuthException] to get the action text for
 * @param stringProvider The [AuthUIStringProvider] for localized strings
 * @return The localized action text
 */
private fun getRecoveryActionText(
    error: AuthException,
    stringProvider: AuthUIStringProvider
): String {
    return when (error) {
        is AuthException.AuthCancelledException -> stringProvider.continueText
        is AuthException.EmailAlreadyInUseException -> stringProvider.signInDefault // Use existing "Sign in" text
        is AuthException.AccountLinkingRequiredException -> stringProvider.continueText // Use "Continue" for linking
        is AuthException.MfaRequiredException -> stringProvider.continueText // Use "Continue" for MFA
        is AuthException.NetworkException,
        is AuthException.InvalidCredentialsException,
        is AuthException.UserNotFoundException,
        is AuthException.WeakPasswordException,
        is AuthException.TooManyRequestsException,
        is AuthException.UnknownException -> stringProvider.retryAction
        else -> stringProvider.retryAction
    }
}

/**
 * Determines if the given [AuthException] is recoverable through user action.
 *
 * @param error The [AuthException] to check
 * @return `true` if the error is recoverable, `false` otherwise
 */
private fun isRecoverable(error: AuthException): Boolean {
    return when (error) {
        is AuthException.NetworkException -> true
        is AuthException.InvalidCredentialsException -> true
        is AuthException.UserNotFoundException -> true
        is AuthException.WeakPasswordException -> true
        is AuthException.EmailAlreadyInUseException -> true
        is AuthException.TooManyRequestsException -> false // User must wait
        is AuthException.MfaRequiredException -> true
        is AuthException.AccountLinkingRequiredException -> true
        is AuthException.AuthCancelledException -> true
        is AuthException.UnknownException -> true
        else -> true
    }
}