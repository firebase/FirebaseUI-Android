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

/**
 * Represents the result of a validation operation, containing both the validation state
 * and any associated error message.
 *
 * This class is used throughout the authentication validation system to communicate
 * validation results between validators and UI components.
 */
internal class ValidationStatus(
    /**
     * Returns true if the last validation failed.
     */
    val hasError: Boolean,

    /**
     * The error message for the current state.
     */
    val errorMessage: String? = null,
)
