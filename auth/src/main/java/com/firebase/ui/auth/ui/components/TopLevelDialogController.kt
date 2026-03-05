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

package com.firebase.ui.auth.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.firebase.ui.auth.AuthException
import com.firebase.ui.auth.AuthState
import com.firebase.ui.auth.configuration.string_provider.AuthUIStringProvider

/**
 * CompositionLocal for accessing the top-level dialog controller from any composable.
 */
val LocalTopLevelDialogController = compositionLocalOf<TopLevelDialogController?> {
    null
}

/**
 * A top-level dialog controller that allows any child composable to show error recovery dialogs.
 * 
 * It provides a single point of control for showing dialogs from anywhere in the composition tree,
 * preventing duplicate dialogs when multiple screens observe the same error state.
 *
 * **Usage:**
 * ```kotlin
 * // At the root of your auth flow (FirebaseAuthScreen):
 * val dialogController = rememberTopLevelDialogController(stringProvider)
 * 
 * CompositionLocalProvider(LocalTopLevelDialogController provides dialogController) {
 *     // Your auth screens...
 *     
 *     // Show dialog at root level (only one instance)
 *     dialogController.CurrentDialog()
 * }
 * 
 * // In any child screen (EmailAuthScreen, PhoneAuthScreen, etc.):
 * val dialogController = LocalTopLevelDialogController.current
 * 
 * LaunchedEffect(error) {
 *     error?.let { exception ->
 *         dialogController?.showErrorDialog(
 *             exception = exception,
 *             onRetry = { ... },
 *             onRecover = { ... },
 *             onDismiss = { ... }
 *         )
 *     }
 * }
 * ```
 *
 * @since 10.0.0
 */
class TopLevelDialogController(
    private val stringProvider: AuthUIStringProvider,
    private val authState: AuthState
) {
    private var dialogState by mutableStateOf<DialogState?>(null)
    private val shownErrorStates = mutableSetOf<AuthState.Error>()

    /**
     * Shows an error recovery dialog at the top level using [ErrorRecoveryDialog].
     * Automatically prevents duplicate dialogs for the same AuthState.Error instance.
     *
     * @param exception The auth exception to display
     * @param onRetry Callback when user clicks retry button
     * @param onRecover Callback when user clicks recover button (e.g., navigate to different screen)
     * @param onDismiss Callback when dialog is dismissed
     */
    fun showErrorDialog(
        exception: AuthException,
        onRetry: (AuthException) -> Unit = {},
        onRecover: (AuthException) -> Unit = {},
        onDismiss: () -> Unit = {}
    ) {
        // Get current error state
        val currentErrorState = authState as? AuthState.Error

        // If this exact error state has already been shown, skip
        if (currentErrorState != null && currentErrorState in shownErrorStates) {
            return
        }

        // Mark this error state as shown
        currentErrorState?.let { shownErrorStates.add(it) }

        dialogState = DialogState.ErrorDialog(
            exception = exception,
            onRetry = onRetry,
            onRecover = onRecover,
            onDismiss = {
                dialogState = null
                onDismiss()
            }
        )
    }

    /**
     * Dismisses the currently shown dialog.
     */
    fun dismissDialog() {
        dialogState = null
    }

    /**
     * Composable that renders the current dialog, if any.
     * This should be called once at the root level of your auth flow.
     * 
     * Uses the existing [ErrorRecoveryDialog] component.
     */
    @Composable
    fun CurrentDialog() {
        val state = dialogState
        when (state) {
            is DialogState.ErrorDialog -> {
                ErrorRecoveryDialog(
                    error = state.exception,
                    stringProvider = stringProvider,
                    onRetry = { exception ->
                        state.onRetry(exception)
                        state.onDismiss()
                    },
                    onRecover = { exception ->
                        state.onRecover(exception)
                        state.onDismiss()
                    },
                    onDismiss = state.onDismiss
                )
            }
            null -> {
                // No dialog to show
            }
        }
    }

    private sealed class DialogState {
        data class ErrorDialog(
            val exception: AuthException,
            val onRetry: (AuthException) -> Unit,
            val onRecover: (AuthException) -> Unit,
            val onDismiss: () -> Unit
        ) : DialogState()
    }
}

/**
 * Creates and remembers a [TopLevelDialogController].
 */
@Composable
fun rememberTopLevelDialogController(
    stringProvider: AuthUIStringProvider,
    authState: AuthState
): TopLevelDialogController {
    return remember(stringProvider, authState) {
        TopLevelDialogController(stringProvider, authState)
    }
}
