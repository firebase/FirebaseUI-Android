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

package com.firebase.ui.auth.compose.mfa

import com.firebase.ui.auth.compose.configuration.string_provider.AuthUIStringProvider
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthRecentLoginRequiredException
import java.io.IOException

/**
 * Maps Firebase Auth exceptions to localized error messages for MFA enrollment.
 *
 * @param stringProvider Provider for localized strings
 * @return Localized error message appropriate for the exception type
 */
fun Exception.toMfaErrorMessage(stringProvider: AuthUIStringProvider): String {
    return when (this) {
        is FirebaseAuthRecentLoginRequiredException ->
            stringProvider.mfaErrorRecentLoginRequired
        is FirebaseAuthInvalidCredentialsException ->
            stringProvider.mfaErrorInvalidVerificationCode
        is IOException, is FirebaseNetworkException ->
            stringProvider.mfaErrorNetwork
        else -> stringProvider.mfaErrorGeneric
    }
}
