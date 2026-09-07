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

package com.firebase.ui.auth.configuration

import com.firebase.ui.auth.util.CountryUtils

/**
 * Configuration class for Multi-Factor Authentication (MFA) enrollment and verification behavior.
 *
 * This class controls which MFA factors are available to users and whether enrollment is
 * mandatory.
 *
 * @property allowedFactors List of MFA factors that users are permitted to enroll in.
 *                          Defaults to [MfaFactor.Sms, MfaFactor.Totp].
 * @property requireEnrollment Whether MFA enrollment is mandatory for all users.
 *                             When true, users must enroll in at least one MFA factor.
 *                             Defaults to false.
 * @property allowedCountries ISO 3166-1 alpha-2 country codes the [MfaFactor.Sms] enrollment step
 *                            restricts its country selector to, or `null` for no restriction. Dial
 *                            codes are rejected: the filter behind this matches alpha-2 only, so a
 *                            dial code would silently restrict to nothing. Lives here rather
 *                            than on the phone sign-in provider because a second factor is
 *                            configured independently of the first: Firebase enables SMS second
 *                            factors separately from phone sign-in, and phone sign-in cannot carry
 *                            a second factor at all. Defaults to null.
 */
class MfaConfiguration(
    val allowedFactors: List<MfaFactor> = listOf(MfaFactor.Sms, MfaFactor.Totp),
    val requireEnrollment: Boolean = false,
    val allowedCountries: List<String>? = null
) {
    init {
        require(allowedFactors.isNotEmpty()) {
            "At least one MFA factor must be allowed"
        }
        allowedCountries?.forEach { code ->
            require(CountryUtils.findByCountryCode(code) != null) {
                "Invalid country code: $code. allowedCountries takes ISO 3166-1 alpha-2 codes " +
                        "(e.g. 'us', 'GB'). Dial codes are not accepted."
            }
        }
    }
}
