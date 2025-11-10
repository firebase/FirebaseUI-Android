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

/**
 * Represents the different Multi-Factor Authentication (MFA) factors that can be used
 * for enrollment and verification.
 */
enum class MfaFactor {
    /**
     * SMS-based authentication factor.
     * Users receive a verification code via text message to their registered phone number.
     */
    Sms,

    /**
     * Time-based One-Time Password (TOTP) authentication factor.
     * Users generate verification codes using an authenticator app.
     */
    Totp
}
