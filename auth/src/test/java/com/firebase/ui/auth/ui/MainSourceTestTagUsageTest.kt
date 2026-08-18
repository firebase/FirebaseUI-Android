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

package com.firebase.ui.auth.ui

import com.google.common.truth.Truth.assertWithMessage
import java.io.File
import org.junit.Test

/**
 * Closes the one-sided gap left by [FirebaseAuthTestTagsTest].
 *
 * That class reflects over the registry and so only polices tags that already opted in. A
 * contributor writing `Modifier.testTag("Email Field")` inline in a main source file satisfies the
 * compiler, keeps the `:auth` suite green, and still ships a tag that cannot be addressed as an
 * Android resource id — which is the entire reason the registry exists. This class scans the
 * `auth/src/main` Kotlin sources instead of the compiled registry and fails the build when a tag is
 * applied without going through [FirebaseAuthTestTags].
 *
 * A source scan is used rather than a custom lint rule because it needs no new module, no lint API
 * surface, and no UAST plumbing to answer a purely lexical question, and it runs inside the unit
 * test task that already gates every change.
 *
 * It lives in its own class rather than in [FirebaseAuthTestTagsTest] because it tests a different
 * thing by a different mechanism: the registry test asserts properties of compiled constants, while
 * this one reads files off disk and therefore carries a failure mode of its own — a scan that
 * finds nothing and passes. Keeping them apart keeps that vacuity guard from reading as noise
 * inside the registry invariants.
 *
 * @suppress Internal test class
 */
class MainSourceTestTagUsageTest {

    @Test
    fun `every main source test tag is a FirebaseAuthTestTags reference`() {
        val violations = tagCallSites().filterNot { site ->
            site.argument.startsWith(REGISTRY_REFERENCE_PREFIX)
        }

        assertWithMessage(violationMessage(violations)).that(violations).isEmpty()
    }

    /**
     * Guards this class against the failure mode that would make it worthless: resolving no source
     * tree, or the wrong one, scanning nothing, and reporting success. Every step of the scan is
     * pinned, so a moved module or a relocated source set breaks the build loudly instead of
     * quietly disarming the assertion above.
     */
    @Test
    fun `the source scan cannot pass having scanned nothing`() {
        val mainSources = mainSourceRoot()
        val sources = kotlinSources(mainSources)

        assertWithMessage(
            "Resolved the auth main source root to $mainSources but found no Kotlin files under " +
                "it. Compose test tags are applied from Kotlin, so an empty scan means this " +
                "class is no longer checking anything."
        ).that(sources).isNotEmpty()

        val callSites = tagCallSites()

        assertWithMessage(
            "Scanned ${sources.size} Kotlin files under $mainSources and found no testTag call " +
                "sites at all. The auth screens do tag nodes, so zero matches means the scan is " +
                "looking in the wrong place or no longer recognises the call shape — either way " +
                "the registry-reference assertion in this class would pass vacuously."
        ).that(callSites).isNotEmpty()

        assertWithMessage(
            "Expected $EXPECTED_TAG_CALL_SITES testTag call sites in auth main sources but " +
                "found ${callSites.size}:\n" +
                callSites.joinToString("\n") { "  ${it.path}:${it.line} -> ${it.argument}" } +
                "\n\nThis count is pinned on purpose. If you added or removed a tag, update " +
                "EXPECTED_TAG_CALL_SITES in this class to match. If you did not, the scan is " +
                "reaching a different set of files than it should and the check above has " +
                "quietly stopped covering the ones it misses."
        ).that(callSites).hasSize(EXPECTED_TAG_CALL_SITES)
    }

    /**
     * The message a contributor reads immediately after this test broke their build. It is the
     * whole value of the test, so it names every offending site and says what to do instead.
     */
    private fun violationMessage(violations: List<TagCallSite>): String = buildString {
        append(
            "Compose test tags applied in auth main sources must be $REGISTRY_REFERENCE_PREFIX " +
                "references. The call sites below pass something else, which bypasses every " +
                "invariant FirebaseAuthTestTagsTest enforces — valid resource-name shape, " +
                "uniqueness across the registry, and the surface prefix — so the tag can ship " +
                "unaddressable by Firebase Test Lab Robo directives and UiAutomator By.res().\n\n"
        )
        violations.forEach { site ->
            append("  ${site.path}:${site.line} -> testTag argument: ${site.argument}\n")
        }
        append(
            "\nFix: add a constant to the ${FirebaseAuthTestTags::class.simpleName} group that " +
                "owns this screen or surface — adding a new group if none fits — and pass that " +
                "constant here, then update EXPECTED_TAG_CALL_SITES in this class if the number " +
                "of tagged nodes changed.\n\n" +
                "Non-literal arguments are rejected as well, not only string literals: an " +
                "argument assembled from a variable, a parameter, a string template, or a helper " +
                "call is a value this scan cannot check and the registry does not publish, so it " +
                "is not an escape hatch. Only an expression beginning with " +
                "\"$REGISTRY_REFERENCE_PREFIX\" is accepted. Note that a host application tagging " +
                "one of our nodes with its own value does so by passing a modifier into the " +
                "composable from its own sources; that is legitimate and is not affected by this " +
                "check, which reads auth main sources only. Test sources are likewise not " +
                "scanned, so fixture-local tags in the `:auth` unit tests stay free-form."
        )
    }

    /** Every `testTag` application found in the auth main sources, in file order. */
    private fun tagCallSites(): List<TagCallSite> {
        val mainSources = mainSourceRoot()
        return kotlinSources(mainSources).flatMap { file -> tagCallSitesIn(file, mainSources) }
    }

    /**
     * Locates `auth/src/main/java` without trusting the working directory.
     *
     * Gradle runs unit tests with the module directory as the working directory, but that is a
     * default rather than a guarantee, and the same test may be launched from an IDE or from the
     * repository root. So the module-relative and repository-relative paths are both tried at the
     * working directory and at each of its ancestors, and a candidate only counts once
     * [FirebaseAuthTestTags] itself is found inside it. Using the registry file as the sentinel
     * means resolution cannot silently land on some unrelated `src/main/java`.
     */
    private fun mainSourceRoot(): File {
        val workingDirectory = File(System.getProperty("user.dir") ?: ".").absoluteFile

        val candidates = mutableListOf<File>()
        var directory: File? = workingDirectory
        var depth = 0
        while (directory != null && depth <= MAX_ANCESTOR_WALK) {
            candidates += File(directory, MODULE_RELATIVE_MAIN_SOURCES)
            candidates += File(directory, REPOSITORY_RELATIVE_MAIN_SOURCES)
            directory = directory.parentFile
            depth++
        }

        val resolved = candidates.firstOrNull { candidate ->
            File(candidate, REGISTRY_RELATIVE_PATH).isFile
        }

        assertWithMessage(
            "Could not locate the auth main source tree, so this class cannot check anything and " +
                "fails rather than passing on an empty scan. Looked for " +
                "$REGISTRY_RELATIVE_PATH under \"$MODULE_RELATIVE_MAIN_SOURCES\" and " +
                "\"$REPOSITORY_RELATIVE_MAIN_SOURCES\", relative to the working directory " +
                "$workingDirectory and up to $MAX_ANCESTOR_WALK of its ancestors. If the :auth " +
                "module or its source set moved, update the constants in this class."
        ).that(resolved).isNotNull()

        return resolved!!
    }

    private fun kotlinSources(root: File): List<File> =
        root.walkTopDown()
            .filter { it.isFile && it.extension == KOTLIN_EXTENSION }
            .sortedBy { it.invariantSeparatorsPath }
            .toList()

    /**
     * Finds `testTag(...)` calls and `testTag = ...` semantics assignments in [file].
     *
     * Whole-line comments are blanked before matching, so a call shown in KDoc is not reported,
     * while line numbering stays intact. Arguments are read with a paren-balancing scan that
     * ignores delimiters inside string literals, so a multi-line call site is captured whole.
     */
    private fun tagCallSitesIn(file: File, root: File): List<TagCallSite> {
        val relativePath = file.toRelativeString(root).replace(File.separatorChar, '/')
        val source = file.readLines().joinToString("\n") { line ->
            if (COMMENT_PREFIXES.any { line.trimStart().startsWith(it) }) "" else line
        }

        return TAG_APPLICATION_PATTERN.findAll(source).map { match ->
            val delimiterIndex = match.range.last
            val argument = if (source[delimiterIndex] == '(') {
                balancedArgument(source, delimiterIndex)
            } else {
                source.substring(delimiterIndex + 1).substringBefore('\n')
            }

            TagCallSite(
                path = relativePath,
                line = source.take(match.range.first).count { it == '\n' } + 1,
                argument = argument.replace(WHITESPACE_RUN, " ").trim()
            )
        }.toList()
    }

    /** Reads the text between the paren at [openIndex] and its match, string literals included. */
    private fun balancedArgument(source: String, openIndex: Int): String {
        var depth = 0
        var index = openIndex
        var inString = false
        while (index < source.length) {
            val character = source[index]
            when {
                inString && character == '\\' -> index++
                character == '"' -> inString = !inString
                inString -> Unit
                character == '(' -> depth++
                character == ')' -> {
                    depth--
                    if (depth == 0) return source.substring(openIndex + 1, index)
                }
            }
            index++
        }
        // Unbalanced source would not compile; report what is there so the site is still named.
        return source.substring(openIndex + 1)
    }

    /** One `testTag` application in a main source file. */
    private data class TagCallSite(val path: String, val line: Int, val argument: String)

    private companion object {
        /**
         * Number of `testTag` applications in `auth/src/main`. Pinned so that a scan which stops
         * reaching files announces itself. Update it deliberately when tagging a new node.
         */
        const val EXPECTED_TAG_CALL_SITES = 3

        const val REGISTRY_REFERENCE_PREFIX = "FirebaseAuthTestTags."

        const val REGISTRY_RELATIVE_PATH = "com/firebase/ui/auth/ui/FirebaseAuthTestTags.kt"

        /** Working directory is the module directory under Gradle's default. */
        const val MODULE_RELATIVE_MAIN_SOURCES = "src/main/java"

        /** Working directory is the repository root, as when launched from an IDE run config. */
        const val REPOSITORY_RELATIVE_MAIN_SOURCES = "auth/src/main/java"

        const val MAX_ANCESTOR_WALK = 6

        const val KOTLIN_EXTENSION = "kt"

        val COMMENT_PREFIXES = listOf("//", "*", "/*")

        /**
         * `testTag(` as a call, or `testTag =` as a `SemanticsPropertyReceiver` assignment. Both
         * apply a tag, so both are checked; the trailing delimiter says which form matched.
         */
        val TAG_APPLICATION_PATTERN = Regex("""\btestTag\s*[(=]""")

        val WHITESPACE_RUN = Regex("""\s+""")
    }
}
