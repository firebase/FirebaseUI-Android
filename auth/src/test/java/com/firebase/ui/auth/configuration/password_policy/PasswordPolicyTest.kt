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

package com.firebase.ui.auth.configuration.password_policy

import com.firebase.ui.auth.configuration.password_policy.PasswordPolicy.EnforcementState
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Unit tests for [PasswordPolicy] parsing and evaluation.
 *
 * @suppress Internal test class
 */
@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class PasswordPolicyTest {

    // =============================================================================================
    // Parsing
    // =============================================================================================

    @Test
    fun `parse reads the sample response`() {
        val policy = PasswordPolicy.parse(SAMPLE_RESPONSE)!!

        assertThat(policy.minPasswordLength).isEqualTo(10)
        assertThat(policy.maxPasswordLength).isEqualTo(4096)
        assertThat(policy.containsLowercaseCharacter).isTrue()
        assertThat(policy.containsUppercaseCharacter).isTrue()
        assertThat(policy.containsNumericCharacter).isTrue()
        assertThat(policy.containsNonAlphanumericCharacter).isFalse()
        assertThat(policy.allowedNonAlphanumericCharacters).hasSize(30)
        assertThat(policy.allowedNonAlphanumericCharacters).containsAtLeast("^", "\"", "\\", "`")
        assertThat(policy.enforcementState).isEqualTo(EnforcementState.ENFORCE)
    }

    @Test
    fun `parse rejects schemaVersion 2`() {
        assertThat(PasswordPolicy.parse("""{"schemaVersion":2,"enforcementState":"ENFORCE"}"""))
            .isNull()
    }

    @Test
    fun `parse rejects a string or fractional schemaVersion`() {
        assertThat(PasswordPolicy.parse("""{"schemaVersion":"1"}""")).isNull()
        assertThat(PasswordPolicy.parse("""{"schemaVersion":1.9}""")).isNull()
        assertThat(PasswordPolicy.parse("""{"schemaVersion":true}""")).isNull()
    }

    @Test
    fun `parse accepts schemaVersion 1 point 0`() {
        assertThat(PasswordPolicy.parse("""{"schemaVersion":1.0}""")).isNotNull()
    }

    @Test
    fun `explicit null minPasswordLength falls back to the default`() {
        val body = """{"schemaVersion":1,"customStrengthOptions":{"minPasswordLength":null}}"""
        val policy = PasswordPolicy.parse(body)!!

        assertThat(policy.minPasswordLength).isEqualTo(PasswordPolicy.DEFAULT_MIN_PASSWORD_LENGTH)
    }

    @Test
    fun `explicit null maxPasswordLength is treated as absent`() {
        val body = """{"schemaVersion":1,"customStrengthOptions":{"maxPasswordLength":null}}"""
        val policy = PasswordPolicy.parse(body)!!

        assertThat(policy.maxPasswordLength).isNull()
    }

    @Test
    fun `maxPasswordLength of 0 is treated as absent`() {
        val body = """{"schemaVersion":1,"customStrengthOptions":{"maxPasswordLength":0}}"""
        val policy = PasswordPolicy.parse(body)!!

        assertThat(policy.maxPasswordLength).isNull()
        assertThat(policy.validate("abcdefg").meetsMaxPasswordLength).isNull()
    }

    @Test
    fun `parse rejects missing schemaVersion`() {
        assertThat(PasswordPolicy.parse("""{"enforcementState":"ENFORCE"}""")).isNull()
    }

    @Test
    fun `parse rejects malformed JSON`() {
        assertThat(PasswordPolicy.parse("{not json")).isNull()
        assertThat(PasswordPolicy.parse("[]")).isNull()
        assertThat(PasswordPolicy.parse("")).isNull()
    }

    @Test
    fun `parse rejects a non-numeric length`() {
        val body = """{"schemaVersion":1,"customStrengthOptions":{"minPasswordLength":"x"}}"""
        assertThat(PasswordPolicy.parse(body)).isNull()
    }

    @Test
    fun `enforcementState OFF, UNSPECIFIED, unknown and missing all parse as OFF`() {
        listOf(
            """{"schemaVersion":1,"enforcementState":"OFF"}""",
            """{"schemaVersion":1,"enforcementState":"ENFORCEMENT_STATE_UNSPECIFIED"}""",
            """{"schemaVersion":1,"enforcementState":"SOMETHING_NEW"}""",
            """{"schemaVersion":1}""",
        ).forEach { body ->
            assertThat(PasswordPolicy.parse(body)!!.enforcementState).isEqualTo(EnforcementState.OFF)
        }
    }

    @Test
    fun `missing customStrengthOptions defaults min length to 6 and configures nothing else`() {
        val policy = PasswordPolicy.parse("""{"schemaVersion":1}""")!!

        assertThat(policy.minPasswordLength).isEqualTo(PasswordPolicy.DEFAULT_MIN_PASSWORD_LENGTH)
        assertThat(policy.maxPasswordLength).isNull()
        assertThat(policy.allowedNonAlphanumericCharacters).isEmpty()
    }

    // =============================================================================================
    // Evaluation
    // =============================================================================================

    @Test
    fun `min length defaults to 6 and is the only requirement when nothing is configured`() {
        val policy = PasswordPolicy.parse("""{"schemaVersion":1}""")!!

        val short = policy.validate("abcde")
        assertThat(short.meetsMinPasswordLength).isFalse()
        assertThat(short.isValid).isFalse()
        assertThat(short.meetsMaxPasswordLength).isNull()
        assertThat(short.containsLowercaseLetter).isNull()
        assertThat(short.containsUppercaseLetter).isNull()
        assertThat(short.containsNumericCharacter).isNull()
        assertThat(short.containsNonAlphanumericCharacter).isNull()

        assertThat(policy.validate("abcdef").isValid).isTrue()
    }

    @Test
    fun `max length is checked only when present`() {
        val capped = policy(min = 1, max = 3)
        assertThat(capped.validate("abcd").meetsMaxPasswordLength).isFalse()
        assertThat(capped.validate("abcd").isValid).isFalse()
        assertThat(capped.validate("abc").meetsMaxPasswordLength).isTrue()

        val uncapped = policy(min = 1)
        assertThat(uncapped.validate("a".repeat(10_000)).meetsMaxPasswordLength).isNull()
        assertThat(uncapped.validate("a".repeat(10_000)).isValid).isTrue()
    }

    @Test
    fun `length counts UTF-16 code units`() {
        // U+1F600 is one code point and two UTF-16 code units, like JS password.length.
        val status = policy(min = 2, max = 2).validate("😀")
        assertThat(status.meetsMinPasswordLength).isTrue()
        assertThat(status.meetsMaxPasswordLength).isTrue()
    }

    @Test
    fun `character classes are ASCII only`() {
        val p = policy(min = 1, lower = true, upper = true, numeric = true)

        val accented = p.validate("Éé٣")
        assertThat(accented.containsUppercaseLetter).isFalse()
        assertThat(accented.containsLowercaseLetter).isFalse()
        assertThat(accented.containsNumericCharacter).isFalse()

        val ascii = p.validate("aZ9")
        assertThat(ascii.containsLowercaseLetter).isTrue()
        assertThat(ascii.containsUppercaseLetter).isTrue()
        assertThat(ascii.containsNumericCharacter).isTrue()
        assertThat(ascii.isValid).isTrue()
    }

    @Test
    fun `symbol counts only when allowed and a disallowed symbol is not rejected`() {
        val p = policy(min = 1, symbol = true, allowed = listOf("!", "#"))

        assertThat(p.validate("a!").containsNonAlphanumericCharacter).isTrue()

        val disallowed = p.validate("a+")
        assertThat(disallowed.containsNonAlphanumericCharacter).isFalse()
        assertThat(disallowed.meetsMinPasswordLength).isTrue()

        // A disallowed character never invalidates an otherwise valid password.
        assertThat(p.validate("a+#é").isValid).isTrue()
    }

    @Test
    fun `character classes are independent, so an allowed alphanumeric also counts as a symbol`() {
        val p = policy(min = 1, lower = true, symbol = true, allowed = listOf("a"))

        val status = p.validate("a")
        assertThat(status.containsLowercaseLetter).isTrue()
        assertThat(status.containsNonAlphanumericCharacter).isTrue()
    }

    @Test
    fun `unconfigured requirements are absent even when the password would fail them`() {
        val p = policy(min = 1, lower = true)
        val status = p.validate("a")

        assertThat(status.containsUppercaseLetter).isNull()
        assertThat(status.containsNumericCharacter).isNull()
        assertThat(status.containsNonAlphanumericCharacter).isNull()
        assertThat(status.isValid).isTrue()
    }

    @Test
    fun `all unmet requirements are reported together`() {
        val p = PasswordPolicy.parse(
            SAMPLE_RESPONSE.replace(
                "\"containsNonAlphanumericCharacter\":false",
                "\"containsNonAlphanumericCharacter\":true",
            )
        )!!

        val status = p.validate("é")
        assertThat(status.meetsMinPasswordLength).isFalse()
        assertThat(status.meetsMaxPasswordLength).isTrue()
        assertThat(status.containsLowercaseLetter).isFalse()
        assertThat(status.containsUppercaseLetter).isFalse()
        assertThat(status.containsNumericCharacter).isFalse()
        assertThat(status.containsNonAlphanumericCharacter).isFalse()
        assertThat(status.isValid).isFalse()

        assertThat(p.validate("Abcdefgh1!").isValid).isTrue()
    }

    private fun policy(
        min: Int,
        max: Int? = null,
        lower: Boolean = false,
        upper: Boolean = false,
        numeric: Boolean = false,
        symbol: Boolean = false,
        allowed: List<String> = emptyList(),
    ) = PasswordPolicy(
        minPasswordLength = min,
        maxPasswordLength = max,
        containsLowercaseCharacter = lower,
        containsUppercaseCharacter = upper,
        containsNumericCharacter = numeric,
        containsNonAlphanumericCharacter = symbol,
        allowedNonAlphanumericCharacters = allowed,
        enforcementState = EnforcementState.ENFORCE,
    )

    internal companion object {
        const val SAMPLE_RESPONSE = """{"customStrengthOptions":{"minPasswordLength":10,"maxPasswordLength":4096,"containsLowercaseCharacter":true,"containsUppercaseCharacter":true,"containsNumericCharacter":true,"containsNonAlphanumericCharacter":false},"schemaVersion":1,"allowedNonAlphanumericCharacters":["^","$","*",".","[","]","{","}","(",")","?","\"","!","@","#","%","&","/","\\",",",">","<","'",":",";","|","_","~","`","-"],"enforcementState":"ENFORCE","forceUpgradeOnSignin":true}"""
    }
}
