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

package com.firebase.ui.auth.ui.screens.mfa

import com.firebase.ui.auth.util.CountryUtils
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.Locale

/**
 * The country the SMS enrollment step opens on. Asserts on the resolved **dial code**, not just
 * membership of the selector's list: the picker being filtered is not what a restriction is for
 * if the number the step sends to still carries an unpermitted prefix.
 *
 * @suppress Internal test class
 */
@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class InitialEnrollmentCountryTest {

    @Test
    fun `keeps the device country when the restriction permits it`() {
        withLocaleCountry("GB") {
            val country = initialEnrollmentCountry(listOf("GB", "IE"))

            assertThat(country.countryCode).isEqualTo("GB")
            assertThat(country.dialCode).isEqualTo("+44")
        }
    }

    /** The mismatch this exists for: a permitted list that does not contain the device country. */
    @Test
    fun `falls back to a permitted country when the device country is not allowed`() {
        withLocaleCountry("US") {
            val country = initialEnrollmentCountry(listOf("GB"))

            assertThat(country.countryCode).isEqualTo("GB")
            assertThat(country.dialCode).isEqualTo("+44")
            assertThat(country.dialCode).isNotEqualTo(CountryUtils.findByCountryCode("US")!!.dialCode)
        }
    }

    @Test
    fun `resolved dial code always belongs to a permitted country`() {
        val permitted = listOf("GB", "IE", "FR")
        withLocaleCountry("US") {
            val country = initialEnrollmentCountry(permitted)

            val permittedDialCodes = permitted.map { CountryUtils.findByCountryCode(it)!!.dialCode }
            assertThat(permittedDialCodes).contains(country.dialCode)
        }
    }

    @Test
    fun `a null restriction leaves the device country alone`() {
        withLocaleCountry("US") {
            assertThat(initialEnrollmentCountry(null).countryCode).isEqualTo("US")
        }
    }

    /** `filterByAllowedCountries` treats empty as "no restriction", so this must not narrow. */
    @Test
    fun `an empty restriction leaves the device country alone`() {
        withLocaleCountry("US") {
            assertThat(initialEnrollmentCountry(emptyList()).countryCode).isEqualTo("US")
        }
    }

    /**
     * `MfaConfiguration` rejects codes that resolve to nothing, but this helper is also reachable
     * from a host passing its own list, so it must not return a country outside the restriction
     * and must not throw.
     */
    @Test
    fun `an unresolvable restriction falls back to the device country rather than throwing`() {
        withLocaleCountry("US") {
            assertThat(initialEnrollmentCountry(listOf("ZZ")).countryCode).isEqualTo("US")
        }
    }

    private fun withLocaleCountry(countryCode: String, block: () -> Unit) {
        val original = Locale.getDefault()
        Locale.setDefault(Locale.Builder().setLanguage("en").setRegion(countryCode).build())
        try {
            block()
        } finally {
            Locale.setDefault(original)
        }
    }
}
