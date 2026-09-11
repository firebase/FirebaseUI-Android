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

package com.firebase.ui.auth

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.firebase.ui.auth.configuration.string_provider.AuthUIStringProvider
import com.firebase.ui.auth.configuration.string_provider.DefaultAuthUIStringProvider
import com.firebase.ui.auth.ui.components.getRecoveryMessage
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import java.util.Locale
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Covers the Google Identity Platform password-policy path, which [FirebaseAuthErrorCodeSweepTest]
 * structurally cannot reach.
 *
 * That sweep's synthetic diagnostic never carries the `PASSWORD_DOES_NOT_MEET_REQUIREMENTS`
 * marker, so neither policy branch is ever entered. The fixtures below are the real backend
 * strings, captured from a project with `Require` enforcement and a minimum length of 10.
 *
 * @suppress Internal test class
 */
@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class PasswordPolicyMessageLocalizationTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val strings: AuthUIStringProvider = DefaultAuthUIStringProvider(context)
    private val french: AuthUIStringProvider = DefaultAuthUIStringProvider(context, Locale.FRENCH)

    // =============================================================================================
    // The real backend strings
    // =============================================================================================

    private companion object {
        /** Verbatim single-constraint probe responses. */
        const val MIN_LENGTH = "Password must contain at least 10 characters"
        const val UPPER_CASE = "Password must contain an upper case character"
        const val LOWER_CASE = "Password must contain a lower case character"
        const val NUMERIC = "Password must contain a numeric character"

        /**
         * Unverified: the probe project has special characters disabled. GIdP's wording is
         * "non-alphanumeric", which is where the substring collision with [NUMERIC] comes from.
         */
        const val NON_ALPHANUMERIC = "Password must contain a non-alphanumeric character"

        /** All failing requirements come back together, comma-separated, inside one bracket. */
        const val ALL_THREE =
            "PASSWORD_DOES_NOT_MEET_REQUIREMENTS : Missing password requirements: " +
                    "[$MIN_LENGTH, $UPPER_CASE, $NUMERIC]"

        /**
         * The marker with nothing parseable after it. `parsePasswordPolicyRequirements` returns an
         * empty list, and the whole message is then the generic string.
         */
        const val UNPARSEABLE = "PASSWORD_DOES_NOT_MEET_REQUIREMENTS"

        val ENGLISH_REQUIREMENT_SENTENCES =
            listOf(MIN_LENGTH, UPPER_CASE, LOWER_CASE, NUMERIC)
    }

    /** The two exception shapes that carry a policy rejection into `from()`. */
    private fun shapesFor(sourceText: String): List<Pair<String, Exception>> = listOf(
        // `from()` reads `reason` first for this type.
        "FirebaseAuthWeakPasswordException" to
                FirebaseAuthWeakPasswordException("ERROR_WEAK_PASSWORD", sourceText, sourceText),
        "FirebaseException" to FirebaseException(sourceText),
    )

    // =============================================================================================
    // The leak the sweep could not see
    // =============================================================================================

    @Test
    fun `a parsed policy rejection never renders the backend's English sentences`() {
        for ((shape, exception) in shapesFor(ALL_THREE)) {
            val rendered = getRecoveryMessage(AuthException.from(exception, strings), strings)

            for (sentence in ENGLISH_REQUIREMENT_SENTENCES) {
                assertWithMessage("%s -> dialog body still contains %s", shape, sentence)
                    .that(rendered).doesNotContain(sentence)
            }
            assertWithMessage("%s -> dialog body is blank", shape)
                .that(rendered.isBlank()).isFalse()
        }
    }

    @Test
    fun `an unparseable policy rejection never renders the old hardcoded literal`() {
        for ((shape, exception) in shapesFor(UNPARSEABLE)) {
            val rendered = getRecoveryMessage(AuthException.from(exception, strings), strings)

            // `errorWeakPasswordGeneric` is the host's hook and ships blank.
            assertWithMessage("%s -> dialog body fell back to the hardcoded literal", shape)
                .that(rendered).doesNotContain("Password does not meet policy requirements")
            assertWithMessage("%s -> dialog body", shape)
                .that(rendered).isEqualTo(strings.errorPasswordPolicyGeneric)
        }
    }

    @Test
    fun `both policy paths resolve to French on a French device`() {
        for (sourceText in listOf(ALL_THREE, UNPARSEABLE)) {
            for ((shape, exception) in shapesFor(sourceText)) {
                val rendered = getRecoveryMessage(AuthException.from(exception, french), french)

                for (sentence in ENGLISH_REQUIREMENT_SENTENCES) {
                    assertWithMessage("%s -> French dialog body contains %s", shape, sentence)
                        .that(rendered).doesNotContain(sentence)
                }
                assertWithMessage("%s -> French dialog body is not French", shape)
                    .that(rendered).contains("mot de passe")
            }
        }
    }

    // =============================================================================================
    // What the mapping actually produces
    // =============================================================================================

    @Test
    fun `each requirement maps to the library's own translated copy`() {
        val rendered = getRecoveryMessage(
            AuthException.from(FirebaseException(ALL_THREE), strings), strings
        )

        // The project's own minimum, read back out of the sentence — not the 6 that
        // `weakPasswordRecoveryMessage` hardcodes.
        assertThat(rendered).contains(strings.passwordTooShort(10))
        assertThat(rendered).contains(strings.passwordMissingUppercase)
        assertThat(rendered).contains(strings.passwordMissingDigit)
        // Only the three that failed.
        assertThat(rendered).doesNotContain(strings.passwordMissingLowercase)
        assertThat(rendered.lines()).hasSize(3)
    }

    @Test
    fun `the lower case requirement maps too`() {
        // Not in ALL_THREE, so it needs its own fixture to be covered at all.
        val rendered = getRecoveryMessage(
            AuthException.from(
                FirebaseException("PASSWORD_DOES_NOT_MEET_REQUIREMENTS: [$LOWER_CASE]"), strings
            ),
            strings
        )

        assertThat(rendered).isEqualTo(strings.passwordMissingLowercase)
    }

    @Test
    fun `the non-alphanumeric requirement maps to the special-character copy, not the digit one`() {
        // "numeric" is a substring of "non-alphanumeric", so the digit arm can swallow this.
        val rendered = getRecoveryMessage(
            AuthException.from(
                FirebaseException("PASSWORD_DOES_NOT_MEET_REQUIREMENTS: [$NON_ALPHANUMERIC]"),
                strings
            ),
            strings
        )

        assertThat(rendered).isEqualTo(strings.passwordMissingSpecialCharacter)
        assertThat(rendered).isNotEqualTo(strings.passwordMissingDigit)
    }

    @Test
    fun `the digit and special-character requirements stay distinct when both fail`() {
        // The pair must resolve to two different strings whichever arm is tested first.
        val rendered = getRecoveryMessage(
            AuthException.from(
                FirebaseException(
                    "PASSWORD_DOES_NOT_MEET_REQUIREMENTS: [$NUMERIC, $NON_ALPHANUMERIC]"
                ),
                strings
            ),
            strings
        )

        assertThat(rendered.lines()).containsExactly(
            strings.passwordMissingDigit,
            strings.passwordMissingSpecialCharacter,
        ).inOrder()
    }

    @Test
    fun `the spelled-out special character wording maps too`() {
        val rendered = getRecoveryMessage(
            AuthException.from(
                FirebaseException(
                    "PASSWORD_DOES_NOT_MEET_REQUIREMENTS: " +
                            "[Password must contain a special character]"
                ),
                strings
            ),
            strings
        )

        assertThat(rendered).isEqualTo(strings.passwordMissingSpecialCharacter)
    }

    @Test
    fun `an exclusive maximum-length requirement states the maximum, not the bound`() {
        // "fewer than 4096" permits 4095, and passwordTooLong renders the maximum, not the bound.
        val rendered = getRecoveryMessage(
            AuthException.from(
                FirebaseException(
                    "PASSWORD_DOES_NOT_MEET_REQUIREMENTS: " +
                            "[Password must contain fewer than 4096 characters]"
                ),
                strings
            ),
            strings
        )

        assertThat(rendered).isEqualTo(strings.passwordTooLong(4095))
        assertThat(rendered).isNotEqualTo(strings.passwordTooLong(4096))
    }

    @Test
    fun `an inclusive maximum-length requirement takes the number as written`() {
        // "at most N" and "no more than N" are inclusive, so no adjustment applies.
        for (wording in listOf("at most 64", "no more than 64")) {
            val rendered = getRecoveryMessage(
                AuthException.from(
                    FirebaseException(
                        "PASSWORD_DOES_NOT_MEET_REQUIREMENTS: " +
                                "[Password must contain $wording characters]"
                    ),
                    strings
                ),
                strings
            )

            assertWithMessage(wording).that(rendered).isEqualTo(strings.passwordTooLong(64))
        }
    }

    @Test
    fun `an unrecognised requirement is kept verbatim rather than dropped`() {
        val reworded = "Password must not be one of your last 5 passwords"
        val rendered = getRecoveryMessage(
            AuthException.from(
                FirebaseException(
                    "PASSWORD_DOES_NOT_MEET_REQUIREMENTS: [$UPPER_CASE, $reworded]"
                ),
                strings
            ),
            strings
        )

        // The known one is still translated...
        assertThat(rendered).contains(strings.passwordMissingUppercase)
        // ...and the unknown one is kept verbatim instead of vanishing or blanking the message.
        assertThat(rendered).contains(reworded)
    }

    @Test
    fun `failingRequirements keeps the raw untranslated sentences`() {
        val exception = AuthException.from(FirebaseException(ALL_THREE), french)

        assertThat(exception).isInstanceOf(AuthException.PasswordPolicyViolationException::class.java)
        val policy = exception as AuthException.PasswordPolicyViolationException

        // This list stays raw even when `message` is French.
        assertThat(policy.failingRequirements)
            .containsExactly(MIN_LENGTH, UPPER_CASE, NUMERIC).inOrder()
        assertThat(policy.message).doesNotContain(MIN_LENGTH)
    }

    @Test
    fun `a policy rejection stays a PasswordPolicyViolationException, not a WeakPasswordException`() {
        for ((shape, exception) in shapesFor(ALL_THREE)) {
            assertWithMessage("%s", shape).that(AuthException.from(exception, strings))
                .isInstanceOf(AuthException.PasswordPolicyViolationException::class.java)
        }

        // A plain weak-password rejection must not be pulled into the policy type.
        val plain = FirebaseAuthWeakPasswordException(
            "ERROR_WEAK_PASSWORD", "Password should be at least 6 characters",
            "Password should be at least 6 characters"
        )
        assertThat(AuthException.from(plain, strings))
            .isInstanceOf(AuthException.WeakPasswordException::class.java)
    }

    @Test
    fun `a null string provider still leaves the backend sentences readable`() {
        // Nothing to resolve against, so the raw sentences are the only output.
        val resolved = AuthException.from(FirebaseException(ALL_THREE), null as AuthUIStringProvider?)

        assertThat(resolved.message).contains(MIN_LENGTH)
        assertThat(resolved.message).contains(UPPER_CASE)
    }
}
