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
 * Configuration class for Multi-Factor Authentication (MFA) enrollment and verification behavior.
 *
 * This class controls which MFA factors are available to users, whether enrollment is mandatory,
 * and whether recovery codes are generated.
 *
 * @property allowedFactors List of MFA factors that users are permitted to enroll in.
 *                          Defaults to [MfaFactor.Sms, MfaFactor.Totp].
 * @property requireEnrollment Whether MFA enrollment is mandatory for all users.
 *                             When true, users must enroll in at least one MFA factor.
 *                             Defaults to false.
 * @property enableRecoveryCodes Whether to generate and provide recovery codes to users
 *                               after successful MFA enrollment. These codes can be used
 *                               as a backup authentication method. Defaults to true.
 */
class MfaConfiguration(
    val allowedFactors: List<MfaFactor> = listOf(MfaFactor.Sms, MfaFactor.Totp),
    val requireEnrollment: Boolean = false,
    val enableRecoveryCodes: Boolean = true
) {
    init {
        require(allowedFactors.isNotEmpty()) {
            "At least one MFA factor must be allowed"
        }
    }
}
