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
import com.google.firebase.auth.FirebaseAuthActionCodeException
import com.google.firebase.auth.FirebaseAuthEmailException
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthRecentLoginRequiredException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.auth.FirebaseAuthWebException
import java.io.DataInputStream
import java.io.File
import java.io.InputStream
import java.net.URLDecoder
import java.util.Locale
import java.util.jar.JarFile
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Exhaustive sweep of the invariant `AuthException.from` is built on: given a non-null
 * [AuthUIStringProvider], the resulting `message` — and the dialog body rendered from it — is
 * always library-owned copy, never the Firebase SDK's untranslated English diagnostic.
 *
 * Unlike the hand-written allowlists in the other suites here, this one does not let the author
 * choose the inputs: it reads every `ERROR_*` literal out of the firebase-auth artifact on the
 * classpath and drives all of them through every constructible `FirebaseAuthException` subtype.
 * A firebase-auth upgrade that adds error codes therefore widens the suite automatically.
 *
 * @suppress Internal test class
 */
@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class FirebaseAuthErrorCodeSweepTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val strings: AuthUIStringProvider = DefaultAuthUIStringProvider(context)

    /**
     * A diagnostic that cannot collide with any library string, so `doesNotContain` is a sound
     * test for "the SDK text leaked into what the user sees".
     */
    private val diagnostic =
        "ZZ_RAW_SDK_DIAGNOSTIC_ZZ the Firebase SDK's own untranslated English."

    /** Every shape a Firebase error code can arrive in that `from()` dispatches on by type. */
    private fun shapesFor(code: String): List<Pair<String, Exception>> = listOf(
        "FirebaseAuthException" to object : FirebaseAuthException(code, diagnostic) {},
        "FirebaseAuthInvalidCredentialsException" to
                FirebaseAuthInvalidCredentialsException(code, diagnostic),
        "FirebaseAuthWeakPasswordException" to
                FirebaseAuthWeakPasswordException(code, diagnostic, diagnostic),
        "FirebaseAuthInvalidUserException" to FirebaseAuthInvalidUserException(code, diagnostic),
        "FirebaseAuthUserCollisionException" to
                FirebaseAuthUserCollisionException(code, diagnostic),
        "FirebaseAuthRecentLoginRequiredException" to
                FirebaseAuthRecentLoginRequiredException(code, diagnostic),
        "FirebaseAuthActionCodeException" to FirebaseAuthActionCodeException(code, diagnostic),
        "FirebaseAuthEmailException" to FirebaseAuthEmailException(code, diagnostic),
        "FirebaseAuthWebException" to FirebaseAuthWebException(code, diagnostic),
    )

    // =============================================================================================
    // The extraction itself — if this is wrong, the sweep below proves nothing
    // =============================================================================================

    @Test
    fun `the error codes are read from the firebase-auth artifact on the classpath`() {
        val codes = FIREBASE_AUTH_ERROR_CODES

        // An empty read makes every other assertion in this file vacuous; this bound catches it.
        assertWithMessage(
            "Expected ~81 ERROR_* literals across the artifacts holding " +
                    "$ARTIFACT_PROBE_CLASSES, found ${codes.size}. Either the artifact layout " +
                    "changed, or a probe class was renamed by an SDK upgrade. Fix the " +
                    "extraction — do not lower this bound, and do not delete this test."
        ).that(codes.size).isAtLeast(79)

        // Anchors across unrelated families, so a partial read cannot pass.
        assertThat(codes).containsAtLeast(
            "ERROR_INVALID_CREDENTIAL",
            "ERROR_USER_NOT_FOUND",
            "ERROR_EMAIL_ALREADY_IN_USE",
            "ERROR_REQUIRES_RECENT_LOGIN",
            "ERROR_OPERATION_NOT_ALLOWED",
            "ERROR_SESSION_EXPIRED",
        )

        // The invalid-credential-family codes; ERROR_REJECTED_CREDENTIAL reaches the `else` arm.
        assertThat(codes).containsAtLeast(
            "ERROR_MISSING_OR_INVALID_NONCE",
            "ERROR_REJECTED_CREDENTIAL",
            "ERROR_INVALID_AUTHENTICATOR_RESPONSE",
            "ERROR_PASSKEY_ENROLLMENT_NOT_FOUND",
        )
    }

    @Test
    fun `the extraction reads the whole artifact, not just the internal error-code table`() {
        // Not redundant with the size bound above: a read narrowed back to the internal table
        // still yields 80 of 81 codes and passes that bound, but fails here.
        assertThat(FIREBASE_AUTH_ERROR_CODES).contains("ERROR_MISSING_ACTIVITY")
        assertThat(ERROR_CODES_IN_INTERNAL_TABLE).doesNotContain("ERROR_MISSING_ACTIVITY")

        assertWithMessage(
            "Every code the internal table declares must still be in the artifact-wide read; " +
                    "if it is not, the widened extraction is dropping classes."
        ).that(FIREBASE_AUTH_ERROR_CODES).containsAtLeastElementsIn(ERROR_CODES_IN_INTERNAL_TABLE)

        assertWithMessage(
            "The artifact-wide read must be strictly wider than the internal table — that is the " +
                    "entire point of widening it."
        ).that(FIREBASE_AUTH_ERROR_CODES.size)
            .isGreaterThan(ERROR_CODES_IN_INTERNAL_TABLE.size)
    }

    @Test
    fun `from() names no error code that the firebase-auth artifact does not ship`() {
        // The sweep only drives codes that exist, so an arm on a code the SDK never produces is
        // invisible to it; this runs the check the other way round.
        assertWithMessage(
            "No ERROR_* literals found in $FROM_HOLDER_CLASS. `from()` was refactored so its " +
                    "`when` arms no longer compile into that class, or the class was renamed. " +
                    "Fix the extraction — do not delete this test."
        ).that(NAMED_BY_FROM.size).isAtLeast(60)

        val shipped = FIREBASE_AUTH_ERROR_CODES.toSet()
        val named = NAMED_BY_FROM.filterNot { it in shipped }

        assertWithMessage(
            "AuthException.from() dispatches on these error codes, none of which exist in the " +
                    "firebase-auth artifact on the classpath. Each is an unreachable branch: " +
                    "either the SDK retired the code, or it was never spelled correctly."
        ).that(named).isEmpty()
    }

    // =============================================================================================
    // The sweep
    // =============================================================================================

    @Test
    fun `no shipped error code leaks the SDK diagnostic into the message`() {
        for (code in FIREBASE_AUTH_ERROR_CODES) {
            for ((shape, firebaseException) in shapesFor(code)) {
                val resolved = AuthException.from(firebaseException, strings).message
                assertWithMessage("%s as %s -> message", code, shape)
                    .that(resolved.orEmpty()).doesNotContain(diagnostic)
            }
        }
    }

    @Test
    fun `no shipped error code leaks the SDK diagnostic into the error dialog`() {
        for (code in FIREBASE_AUTH_ERROR_CODES) {
            for ((shape, firebaseException) in shapesFor(code)) {
                val authException = AuthException.from(firebaseException, strings)
                val rendered = getRecoveryMessage(authException, strings)

                assertWithMessage("%s as %s -> dialog body", code, shape)
                    .that(rendered).doesNotContain(diagnostic)
                // The dialog must still say something; an empty body is its own defect.
                assertWithMessage("%s as %s -> dialog body is blank", code, shape)
                    .that(rendered.isBlank()).isFalse()
                // The developer still needs the diagnostic, so it has to survive on the cause.
                assertWithMessage("%s as %s -> cause", code, shape)
                    .that(authException.cause?.message).isEqualTo(diagnostic)
            }
        }
    }

    @Test
    fun `no shipped error code leaks the SDK diagnostic on a French device`() {
        val french = DefaultAuthUIStringProvider(context, Locale.FRENCH)

        for (code in FIREBASE_AUTH_ERROR_CODES) {
            for ((shape, firebaseException) in shapesFor(code)) {
                val rendered = getRecoveryMessage(AuthException.from(firebaseException, french), french)
                assertWithMessage("%s as %s -> French dialog body", code, shape)
                    .that(rendered).doesNotContain(diagnostic)
            }
        }
    }

    companion object {
        /**
         * Landmarks used to locate the artifacts to read. Whichever jar or class directory holds
         * each of these is read in full; duplicate containers are read once.
         *
         * A landmark must be a class only firebase-auth itself ships, because
         * `com.google.firebase.auth` is a split package. Do not add
         * `com.google.firebase.auth.FirebaseAuthException`: it resolves to firebase-auth-interop,
         * whose jar holds zero `ERROR_*` literals, so it reads as "the artifact ships no codes".
         */
        val ARTIFACT_PROBE_CLASSES = listOf(
            "com.google.firebase.auth.FirebaseAuthMissingActivityForRecaptchaException",
        )

        /**
         * Most of firebase-auth's error-code literals live in this one internal class, as
         * constant-pool entries rather than declared fields, so they are read out of the class
         * file rather than by reflection.
         *
         * It is named here only so the assertions can say how the whole-artifact total splits.
         * **The sweep must not scope itself to this class**: `ERROR_MISSING_ACTIVITY` is declared
         * on [com.google.firebase.auth.FirebaseAuthMissingActivityForRecaptchaException]'s
         * constructor, not in this table, so a narrower scan reports it unshipped.
         */
        const val ERROR_CODE_HOLDER_CLASS = "com.google.android.gms.internal.firebase-auth-api.zzaew"

        /**
         * Every `ERROR_*` literal in **every class of** the firebase-auth artifact resolved onto
         * this test's classpath, extracted at runtime. Nothing here is hardcoded, so an SDK upgrade
         * that adds codes — in the internal table or on a public exception class — widens the sweep
         * without anyone remembering to update a list.
         */
        val FIREBASE_AUTH_ERROR_CODES: List<String> by lazy {
            ARTIFACT_PROBE_CLASSES
                .mapNotNull { containerOf(it) }
                .distinct()
                .flatMap { readUtf8ConstantPools(it) }
                .filter { it.matches(Regex("ERROR_[A-Z0-9_]+")) }
                .distinct()
                .sorted()
        }

        /** The subset of [FIREBASE_AUTH_ERROR_CODES] that the old single-class scope could see. */
        val ERROR_CODES_IN_INTERNAL_TABLE: List<String> by lazy {
            readUtf8ConstantPool(ERROR_CODE_HOLDER_CLASS)
                .filter { it.matches(Regex("ERROR_[A-Z0-9_]+")) }
                .distinct()
                .sorted()
        }

        /**
         * Where `AuthException.from` compiles to. Its `when` arms dispatch on string literals,
         * which land in this class's constant pool exactly like the SDK's own do.
         */
        const val FROM_HOLDER_CLASS = "com.firebase.ui.auth.AuthException\$Companion"

        /**
         * Every `ERROR_*` literal `from()` dispatches on, read back out of its own bytecode for
         * the same reason the SDK's list is: a hand-maintained copy here would drift from the
         * `when` it is supposed to describe, which is the drift this whole suite exists to catch.
         */
        val NAMED_BY_FROM: List<String> by lazy {
            readUtf8ConstantPool(FROM_HOLDER_CLASS)
                .filter { it.matches(Regex("ERROR_[A-Z0-9_]+")) }
                .distinct()
                .sorted()
        }

        /**
         * The jar or class directory that [probeClassName] was loaded from, or `null` when the
         * class does not resolve or arrives in a shape this does not understand. Gradle resolves
         * an AAR's `classes.jar` either as a jar on the classpath or as a directory of extracted
         * `.class` files depending on the transform, so both are handled; anything else yields
         * `null`, which ends as an empty code list and trips the size guard in the first test
         * rather than passing the suite vacuously.
         */
        private fun containerOf(probeClassName: String): File? {
            val url = FirebaseAuthErrorCodeSweepTest::class.java.classLoader
                ?.getResource(probeClassName.replace('.', '/') + ".class")
                ?: return null

            return when (url.protocol) {
                // "jar:file:/path/to/classes.jar!/com/google/firebase/auth/…"
                "jar" -> File(
                    URLDecoder.decode(url.path.substringBefore("!/").removePrefix("file:"), "UTF-8")
                )

                // Walk the package path back off the file to reach the classpath root: one
                // directory per package segment, which is one per '.' in the class name.
                "file" -> generateSequence(
                    File(URLDecoder.decode(url.path, "UTF-8")).parentFile
                ) { it.parentFile }
                    .drop(probeClassName.count { it == '.' })
                    .firstOrNull()

                else -> null
            }
        }

        /** Every CONSTANT_Utf8 entry in every class of [container]. */
        private fun readUtf8ConstantPools(container: File): List<String> =
            if (container.isDirectory) {
                container.walkTopDown()
                    .filter { it.isFile && it.extension == "class" }
                    .flatMap { file ->
                        file.inputStream().use { readUtf8ConstantPool(it, file.path) }.asSequence()
                    }
                    .toList()
            } else {
                JarFile(container).use { jar ->
                    jar.entries().asSequence()
                        .filter { it.name.endsWith(".class") }
                        .flatMap { entry ->
                            jar.getInputStream(entry)
                                .use { readUtf8ConstantPool(it, entry.name) }
                                .asSequence()
                        }
                        .toList()
                }
            }

        /**
         * Returns every CONSTANT_Utf8 entry in [className]'s constant pool. Walking the pool is
         * the only way to see string literals that exist solely inside a method body.
         */
        private fun readUtf8ConstantPool(className: String): List<String> {
            val resource = className.replace('.', '/') + ".class"
            val stream = FirebaseAuthErrorCodeSweepTest::class.java.classLoader
                ?.getResourceAsStream(resource)
                ?: return emptyList()

            return stream.use { readUtf8ConstantPool(it, resource) }
        }

        /** Reads one class file's CONSTANT_Utf8 entries. [resource] only names it in failures. */
        private fun readUtf8ConstantPool(source: InputStream, resource: String): List<String> {
            return DataInputStream(source.buffered()).let { input ->
                require(input.readInt() == -0x35014542) { "$resource is not a class file" }
                input.readUnsignedShort() // minor version
                input.readUnsignedShort() // major version

                val entryCount = input.readUnsignedShort()
                val strings = mutableListOf<String>()
                var index = 1
                while (index < entryCount) {
                    when (val tag = input.readUnsignedByte()) {
                        1 -> strings += input.readUTF()
                        7, 8, 16, 19, 20 -> input.skipBytes(2)
                        15 -> input.skipBytes(3)
                        3, 4, 9, 10, 11, 12, 17, 18 -> input.skipBytes(4)
                        // Long and Double occupy two constant-pool slots each.
                        5, 6 -> {
                            input.skipBytes(8)
                            index++
                        }
                        else -> error("Unknown constant pool tag $tag at $index in $resource")
                    }
                    index++
                }
                strings
            }
        }
    }
}
