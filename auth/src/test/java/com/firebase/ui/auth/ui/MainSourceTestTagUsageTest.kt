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
 * test task that already gates every change. The cost of that choice is that the scan has to do its
 * own tokenising: matching `testTag` with a bare regex over raw file text reports commented-out
 * code, `val testTag = "…"` bindings, and call shapes quoted inside string literals, all of which
 * are correct code. So [maskCommentsAndLiterals] runs first and blanks comments and literal
 * contents — length-preservingly, so offsets and line numbers still line up with the original text
 * — and matches are then required to be a member access or an assignment rather than any token
 * spelled `testTag`. Arguments are still read from the original text, so violations are reported
 * with the literal a contributor actually wrote.
 *
 * What a lexical scan cannot see, it is required to refuse rather than wave through. An aliased
 * import (`import …testTag as composeTestTag`) renames the call out of this scan's reach for one
 * line of effort, so a separate test in this class fails on the alias itself instead of pretending
 * the file is clean. Indirection through a local wrapper function remains out of reach and is
 * accepted: it costs more to write than to review, and the reviewer sees the wrapper.
 *
 * The same scan answers two adjacent questions that nothing else could. Whether every semantics
 * owner the library creates carries [exposeTestTagsAsResourceIds] — the rule that file declares,
 * stated as owners rather than tags — and, as a cheaper backstop for owner shapes this scan does not
 * recognise, whether a file applying a tag flags an owner anywhere in itself. All of these failures
 * are invisible to every Compose assertion in the suite, which is why a lexical check is worth its
 * limitations here.
 *
 * ## What the owner check cannot see
 *
 * The owner check is lexical, so four gaps are known and accepted rather than papered over:
 *
 * * It recognises the owner-creating shapes in [SEMANTICS_OWNER_SHAPES] by name. A `Dialog`
 *   subclassed or wrapped under a different name, or a future Compose owner shape, is invisible to
 *   it. That is what the weaker per-file check below still covers.
 * * Lexical presence cannot tell an *applied* modifier from one that is built and discarded.
 *   `Scaffold(modifier = Modifier.exposeTestTagsAsResourceIds().let { Modifier })` reads as flagged.
 *   Requiring the call inside the owner's own argument list rather than merely nearby is as close as
 *   a scan gets; the remaining shapes are not ones anybody writes by accident.
 * * The converse is a false positive: a flag reached through a local, as in
 *   `val m = Modifier.exposeTestTagsAsResourceIds()` followed by `Scaffold(modifier = m)`, is not in
 *   the argument list and reads as unflagged. The fix — inline it, or flag in the argument list too
 *   — is harmless, because the property is `isImportantForAccessibility = false` and setting it
 *   twice changes nothing.
 * * `@Preview` composables are skipped. They are private, are never composed in a shipped
 *   application, and Android Studio's preview host is not a crawler, so an owner inside one has
 *   nothing to expose. This is the only exemption, and it is a rule rather than an allowlist so that
 *   adding a preview cannot redden an unrelated build.
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
     * Renaming `testTag` on import puts every call through it beyond a lexical scan, so the alias
     * is rejected outright. The check is deliberately blunt: there is no legitimate reason for auth
     * main sources to import `testTag` under another name, and "the guard cannot analyse this" must
     * not resolve to "the guard passes".
     */
    @Test
    fun `no main source file aliases the testTag import`() {
        val root = mainSourceRoot()
        val aliases = kotlinSources(root).flatMap { file ->
            val relativePath = file.toRelativeString(root).replace(File.separatorChar, '/')
            ALIASED_IMPORT_PATTERN.findAll(file.readText()).map { match ->
                "  $relativePath -> ${match.value.trim()}"
            }
        }

        assertWithMessage(
            "Auth main sources must import `testTag` under its own name so that this class can see " +
                "the calls that use it. The imports below rename it, which hides every call " +
                "through the alias from the registry check in this class:\n" +
                aliases.joinToString("\n") +
                "\n\nFix: drop the `as` clause and call `testTag` directly, passing a " +
                "$REGISTRY_REFERENCE_PREFIX constant."
        ).that(aliases).isEmpty()
    }

    /**
     * The rule [exposeTestTagsAsResourceIds] declares, checked as declared: **every semantics owner
     * the library creates carries the flag, whether or not anything inside it is tagged today.**
     *
     * Stating the rule over owners rather than over tags is the whole point of it. A flag on an owner
     * that holds no tags costs nothing, while a tag added inside an owner that was skipped costs a
     * silent regression — `onNodeWithTag(...).assertExists()` still passes and only a Robo directive
     * or `By.res()`, neither of which runs in CI, can tell. So a check keyed on "this file applies a
     * tag" cannot enforce it: the four owners that had to be fixed by hand while this branch was
     * written — the manage-MFA tooltip and the three sibling `Scaffold`s in the MFA enrollment steps
     * — hold no tags at all, and no tag-keyed check would ever have named them.
     *
     * An owner is recognised by construction shape ([SEMANTICS_OWNER_SHAPES]) and is required to carry
     * the flag *in its own argument list*, which ties the flag to that owner rather than to its
     * neighbourhood. A shape invoked with a trailing lambda and no argument list at all — `Scaffold {
     * … }` — cannot be passed a modifier and so cannot be flagged; it is reported for the same
     * reason, and the fix is to give it one. The gaps this cannot see are listed on the class.
     */
    @Test
    fun `every semantics owner the library creates flags itself`() {
        val unflagged = semanticsOwnerSites().filterNot { site -> site.flagged }

        assertWithMessage(
            "The semantics owners below are created by the library but do not call " +
                "$FLAG_FUNCTION_NAME() in their own argument list, so a test tag applied inside " +
                "one of them is not guaranteed to become an Android resource id. " +
                "TestTagsAsResourceIds.kt states the rule as owners rather than " +
                "tags on purpose: flagging an owner that holds no tag is free, because the property " +
                "is not relevant to accessibility, while adding a tag inside an owner that was " +
                "skipped is a silent regression that leaves every Compose assertion in this suite " +
                "green and only Firebase Test Lab Robo or UiAutomator By.res() — neither of which " +
                "runs in CI — able to see it. That is issue #2050:\n" +
                unflagged.joinToString("\n") { "  ${it.path}:${it.line} -> ${it.shape}" } +
                "\n\nFix: pass Modifier.$FLAG_FUNCTION_NAME() to the owner, combining it with any " +
                "modifier already there. An owner invoked with a trailing lambda and no argument " +
                "list has nowhere to put it, so give it one. If the owner genuinely must not be " +
                "flagged, it does not belong in SEMANTICS_OWNER_SHAPES — change the list and say " +
                "why, rather than leaving a named exception here."
        ).that(unflagged).isEmpty()
    }

    /**
     * The cheaper backstop for the owner check above: a file that applies a tag must flag an owner
     * somewhere in itself, whatever shape that owner has.
     *
     * Nothing in the type system ties a `testTag` call to an enclosing
     * [exposeTestTagsAsResourceIds]. The check above covers the owner shapes it knows by name; this
     * one covers the case where a tag is applied under something it does not recognise, at the cost
     * of being coarser — it requires the flag *somewhere in the file*, not that it encloses that
     * particular tag.
     *
     * Three things it therefore cannot see: a file that flags one owner and tags a node under a
     * second, unflagged one; a tag placed in a file with no owner of its own that relies on a flag
     * applied by its caller; and, as everywhere in this class, a flag that is built and never
     * applied. The first is what the owner check and [TestTagsAsResourceIdsTest] are for. The second
     * would be a false positive, and the fix for it — apply the flag in the file too — is harmless
     * rather than wrong: the property is `isImportantForAccessibility = false`, so setting it again
     * where it is already set changes nothing. A coarse check that costs a redundant one-liner is
     * worth more than no check.
     */
    @Test
    fun `every main source file that applies a test tag also flags a semantics owner`() {
        val root = mainSourceRoot()
        val unflagged = kotlinSources(root)
            .filter { file -> tagCallSitesIn(file, root).isNotEmpty() }
            // Masked, so a KDoc paragraph or a commented-out line that merely names the function
            // does not count as applying it.
            .filterNot { file ->
                FLAG_APPLICATION_PATTERN.containsMatchIn(maskCommentsAndLiterals(file.readText()))
            }
            .map { file -> file.toRelativeString(root).replace(File.separatorChar, '/') }

        assertWithMessage(
            "The files below apply a Compose test tag but never call " +
                "$FLAG_FUNCTION_NAME(), so nothing in them exposes a tag as an Android resource " +
                "id. Compose reads that flag by walking a node's semantics ancestors and the walk " +
                "stops at the root of the node's own window, so a tag with no flagged owner above " +
                "it is visible to Compose tests and invisible to Firebase Test Lab Robo and " +
                "UiAutomator By.res() — the exact failure of issue #2050, and one that leaves " +
                "every assertion in the suite green:\n" +
                unflagged.joinToString("\n") { "  $it" } +
                "\n\nFix: call Modifier.$FLAG_FUNCTION_NAME() on the semantics owner that " +
                "encloses the tagged node — the screen's root, or the dialog, bottom sheet, or " +
                "popup it lives in, each of which is its own semantics owner and inherits nothing " +
                "from the composable that opened it. If the owner is genuinely in another file " +
                "because this file only contributes content to a flagged screen, applying it here " +
                "as well is safe and satisfies this check: the property is not relevant to " +
                "accessibility, so setting it twice is a no-op."
        ).that(unflagged).isEmpty()
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
            "Expected at least $MINIMUM_TAG_CALL_SITES testTag call sites in auth main sources " +
                "but found ${callSites.size}:\n" +
                callSites.joinToString("\n") { "  ${it.path}:${it.line} -> ${it.argument}" } +
                "\n\nThis is a floor, not a pin: adding tags is expected and needs no change here, " +
                "but the count dropping below the tags that already exist means the scan has " +
                "stopped reaching files it used to read, and the registry-reference assertion has " +
                "quietly stopped covering them. If a tagged node was genuinely removed, lower " +
                "MINIMUM_TAG_CALL_SITES to match."
        ).that(callSites.size).isAtLeast(MINIMUM_TAG_CALL_SITES)

        val ownerSites = semanticsOwnerSites()

        assertWithMessage(
            "Scanned ${sources.size} Kotlin files under $mainSources and recognised no semantics " +
                "owner construction at all. The auth screens are built out of Scaffolds, dialogs " +
                "and bottom sheets, so zero matches means the owner check in this class would pass " +
                "vacuously."
        ).that(ownerSites).isNotEmpty()

        assertWithMessage(
            "Expected at least $MINIMUM_SEMANTICS_OWNER_SITES semantics owner constructions in " +
                "auth main sources but found ${ownerSites.size}:\n" +
                ownerSites.joinToString("\n") { "  ${it.path}:${it.line} -> ${it.shape}" } +
                "\n\nA floor, not a pin, for the same reason as MINIMUM_TAG_CALL_SITES: new " +
                "screens are expected. Dropping below the owners that already exist means the scan " +
                "has stopped recognising a call shape it used to, and the owner assertion has " +
                "quietly stopped covering it. If an owner was genuinely removed, lower " +
                "MINIMUM_SEMANTICS_OWNER_SITES to match."
        ).that(ownerSites.size).isAtLeast(MINIMUM_SEMANTICS_OWNER_SITES)
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
                "constant here.\n\n" +
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

    /** Every semantics owner construction found in the auth main sources, in file order. */
    private fun semanticsOwnerSites(): List<OwnerSite> {
        val mainSources = mainSourceRoot()
        return kotlinSources(mainSources).flatMap { file -> semanticsOwnerSitesIn(file, mainSources) }
    }

    /**
     * Locates `auth/src/main` without trusting the working directory.
     *
     * Gradle runs unit tests with the module directory as the working directory, but that is a
     * default rather than a guarantee, and the same test may be launched from an IDE or from the
     * repository root. So the module-relative and repository-relative paths are both tried at the
     * working directory and at each of its ancestors, and a candidate only counts once
     * [FirebaseAuthTestTags] itself is found inside it. Using the registry file as the sentinel
     * means resolution cannot silently land on some unrelated `src/main`.
     *
     * The whole of `src/main` is resolved rather than `src/main/java` specifically, because a Kotlin
     * file is not required to live under `java/`: an `src/main/kotlin` directory is a source root
     * Gradle compiles and would have been invisible to this scan. Walking `src/main` covers every
     * source directory in it, present and future, and only `.kt` files are read from it. The
     * sentinel is looked for under either directory for the same reason.
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
            REGISTRY_RELATIVE_PATHS.any { relativePath -> File(candidate, relativePath).isFile }
        }

        assertWithMessage(
            "Could not locate the auth main source tree, so this class cannot check anything and " +
                "fails rather than passing on an empty scan. Looked for " +
                "${REGISTRY_RELATIVE_PATHS.joinToString(" or ")} under " +
                "\"$MODULE_RELATIVE_MAIN_SOURCES\" and " +
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
     * Matching runs over [maskCommentsAndLiterals] output, so nothing inside a comment — of either
     * form, including a trailing comment on a line of live code — and nothing inside a string
     * literal is treated as source. Arguments are then sliced out of the original text at the same
     * offsets, because the argument is what the contributor needs to see in the failure message.
     */
    private fun tagCallSitesIn(file: File, root: File): List<TagCallSite> {
        val relativePath = file.toRelativeString(root).replace(File.separatorChar, '/')
        val source = file.readText()
        val masked = maskCommentsAndLiterals(source)

        return TAG_APPLICATION_PATTERN.findAll(masked)
            .filter { match -> isTagApplication(masked, match) }
            .map { match ->
                val delimiterIndex = match.range.last
                val argument = if (masked[delimiterIndex] == '(') {
                    balancedArgument(masked, source, delimiterIndex)
                } else {
                    assignedExpression(masked, source, delimiterIndex)
                }

                TagCallSite(
                    path = relativePath,
                    line = masked.take(match.range.first).count { it == '\n' } + 1,
                    argument = argument.replace(WHITESPACE_RUN, " ").trim()
                )
            }.toList()
    }

    /**
     * Finds the semantics owner constructions in [file] and says which of them flag themselves.
     *
     * Matching runs over [maskCommentsAndLiterals] output for the same reason the tag scan does: the
     * KDoc in these files names `Scaffold`, `ModalBottomSheet` and the rest constantly, and a
     * documented shape is not a constructed one. The flag is then looked for inside the owner's own
     * argument list — balanced over the masked text, so a paren in a string cannot unbalance it —
     * rather than within some number of surrounding lines, so the flag is tied to that owner and not
     * to a sibling that happens to sit above it. An owner invoked with a trailing lambda and no
     * argument list has no argument list to search and is reported: it cannot be handed a modifier at
     * all.
     */
    private fun semanticsOwnerSitesIn(file: File, root: File): List<OwnerSite> {
        val relativePath = file.toRelativeString(root).replace(File.separatorChar, '/')
        val source = file.readText()
        val masked = maskCommentsAndLiterals(source)

        return OWNER_CONSTRUCTION_PATTERN.findAll(masked)
            .filterNot { match -> precedingWord(masked, match.range.first) in CALL_DISQUALIFYING_KEYWORDS }
            .filterNot { match -> isInsidePreview(masked, match.range.first) }
            .map { match ->
                val delimiterIndex = match.range.last
                val arguments = if (masked[delimiterIndex] == '(') {
                    // Masked on both sides: a flag name quoted in a string is not an application.
                    balancedArgument(masked, masked, delimiterIndex)
                } else {
                    ""
                }

                OwnerSite(
                    path = relativePath,
                    line = masked.take(match.range.first).count { it == '\n' } + 1,
                    shape = match.groupValues[1],
                    flagged = FLAG_APPLICATION_PATTERN.containsMatchIn(arguments)
                )
            }.toList()
    }

    /**
     * Whether the code at [index] sits in a `@Preview` composable.
     *
     * Resolution is by nearest preceding function declaration and the annotation block directly above
     * it, which is exact for the top-level composables these sources are written as. A preview is
     * private, is never composed in a shipped application, and is rendered by Android Studio rather
     * than by a crawler, so an owner inside one has nothing to expose as a resource id.
     */
    private fun isInsidePreview(masked: String, index: Int): Boolean {
        val declaration = FUNCTION_DECLARATION_PATTERN
            .findAll(masked)
            .lastOrNull { match -> match.range.first < index }
            ?: return false

        for (line in masked.take(declaration.range.first).lines().asReversed()) {
            val trimmed = line.trim()
            when {
                trimmed.isEmpty() -> continue
                trimmed.startsWith(PREVIEW_ANNOTATION_PREFIX) -> return true
                trimmed.startsWith("@") -> continue
                else -> return false
            }
        }
        return false
    }

    /**
     * Rejects the tokens that merely spell `testTag` without applying one.
     *
     * A call only counts when it is reached through a receiver (`Modifier.testTag(`) or is a bare
     * call rather than a declaration of a function by that name; an assignment only counts when it
     * is an assignment (`semantics { testTag = … }`) rather than a `val`/`var` binding or an
     * equality comparison. Both were live false positives: a `val testTag = "…"` local was
     * reported as a registry violation, with fix advice that made no sense for it.
     */
    private fun isTagApplication(masked: String, match: MatchResult): Boolean {
        val identifierStart = match.range.first
        val delimiterIndex = match.range.last

        return if (masked[delimiterIndex] == '(') {
            precedingSymbol(masked, identifierStart) == '.' ||
                precedingWord(masked, identifierStart) !in CALL_DISQUALIFYING_KEYWORDS
        } else {
            val next = masked.getOrNull(delimiterIndex + 1)
            next != '=' && precedingWord(masked, identifierStart) !in BINDING_KEYWORDS
        }
    }

    /** The first non-whitespace character before [index], or `null` at the start of the file. */
    private fun precedingSymbol(masked: String, index: Int): Char? {
        var cursor = index - 1
        while (cursor >= 0 && masked[cursor].isWhitespace()) cursor--
        return masked.getOrNull(cursor)
    }

    /** The identifier immediately before [index], or the empty string if a symbol sits there. */
    private fun precedingWord(masked: String, index: Int): String {
        var end = index - 1
        while (end >= 0 && masked[end].isWhitespace()) end--
        var start = end
        while (start >= 0 && (masked[start].isLetterOrDigit() || masked[start] == '_')) start--
        return masked.substring(start + 1, end + 1)
    }

    /**
     * Reads the [original] text between the paren at [openIndex] in [masked] and its match.
     *
     * Balancing runs over the masked text, where a paren inside a string literal has already been
     * blanked, so a tag value such as `"(unused)"` cannot unbalance the scan.
     */
    private fun balancedArgument(masked: String, original: String, openIndex: Int): String {
        var depth = 0
        var index = openIndex
        while (index < masked.length) {
            when (masked[index]) {
                '(' -> depth++
                ')' -> {
                    depth--
                    if (depth == 0) return original.substring(openIndex + 1, index)
                }
            }
            index++
        }
        // Unbalanced source would not compile; report what is there so the site is still named.
        return original.substring(openIndex + 1)
    }

    /**
     * Reads the right-hand side of a `testTag =` assignment whose `=` sits at [equalsIndex].
     *
     * The expression may start on the following line, so leading whitespace is skipped before
     * reading, and it ends at the first newline or closing bracket reached at nesting depth zero.
     * Taking only the remainder of the `=` line — as this did previously — yielded an empty
     * argument for a wrapped assignment, and an empty argument fails the registry-prefix check.
     */
    private fun assignedExpression(masked: String, original: String, equalsIndex: Int): String {
        var index = equalsIndex + 1
        while (index < masked.length && masked[index].isWhitespace()) index++

        val start = index
        var depth = 0
        while (index < masked.length) {
            val character = masked[index]
            when {
                character in OPENING_BRACKETS -> depth++
                character in CLOSING_BRACKETS -> {
                    if (depth == 0) break
                    depth--
                }
                character == '\n' && depth == 0 -> break
            }
            index++
        }
        return original.substring(start, index)
    }

    /**
     * Replaces the contents of comments and of string and character literals with spaces, keeping
     * the result the same length as [source] and its newlines in place so offsets and line numbers
     * carry over unchanged. Literal delimiters are kept so a blanked literal still reads as one.
     *
     * Block comments nest, as they do in Kotlin. A string template containing a nested string
     * literal (`"${'$'}{f("x")}"`) is the one shape this mis-tokenises; it garbles the region
     * rather than failing, and no auth source writes one.
     */
    private fun maskCommentsAndLiterals(source: String): String {
        val masked = StringBuilder(source.length)
        var index = 0

        while (index < source.length) {
            when {
                source.startsWith(BLOCK_COMMENT_OPEN, index) -> {
                    var depth = 0
                    while (index < source.length) {
                        when {
                            source.startsWith(BLOCK_COMMENT_OPEN, index) -> {
                                depth++
                                masked.append("  ")
                                index += 2
                            }
                            source.startsWith(BLOCK_COMMENT_CLOSE, index) -> {
                                depth--
                                masked.append("  ")
                                index += 2
                                if (depth == 0) break
                            }
                            else -> masked.append(blanked(source[index++]))
                        }
                    }
                }

                source.startsWith(LINE_COMMENT, index) ->
                    while (index < source.length && source[index] != '\n') {
                        masked.append(blanked(source[index++]))
                    }

                source.startsWith(RAW_STRING_QUOTE, index) -> {
                    masked.append(RAW_STRING_QUOTE)
                    index += RAW_STRING_QUOTE.length
                    while (index < source.length && !source.startsWith(RAW_STRING_QUOTE, index)) {
                        masked.append(blanked(source[index++]))
                    }
                    if (index < source.length) {
                        masked.append(RAW_STRING_QUOTE)
                        index += RAW_STRING_QUOTE.length
                    }
                }

                source[index] == '"' || source[index] == '\'' -> {
                    val quote = source[index]
                    masked.append(quote)
                    index++
                    while (index < source.length && source[index] != quote && source[index] != '\n') {
                        if (source[index] == '\\') masked.append(blanked(source[index++]))
                        if (index < source.length) masked.append(blanked(source[index++]))
                    }
                    if (index < source.length && source[index] == quote) {
                        masked.append(quote)
                        index++
                    }
                }

                else -> masked.append(source[index++])
            }
        }

        return masked.toString()
    }

    private fun blanked(character: Char): Char = if (character == '\n') '\n' else ' '

    /** One `testTag` application in a main source file. */
    private data class TagCallSite(val path: String, val line: Int, val argument: String)

    /** One semantics owner construction in a main source file, and whether it flags itself. */
    private data class OwnerSite(
        val path: String,
        val line: Int,
        val shape: String,
        val flagged: Boolean,
    )

    private companion object {
        /**
         * Lower bound on the number of `testTag` applications in `auth/src/main`, so a scan that
         * stops reaching files announces itself. A floor rather than an exact count: tagging more
         * nodes is the expected direction of travel and should not redden an unrelated build.
         */
        const val MINIMUM_TAG_CALL_SITES = 35

        /**
         * Lower bound on the number of recognised semantics owner constructions outside `@Preview`
         * functions, so a scan that stops recognising a call shape announces itself rather than
         * reporting a clean tree. A floor for the same reason as [MINIMUM_TAG_CALL_SITES].
         */
        const val MINIMUM_SEMANTICS_OWNER_SITES = 21

        const val REGISTRY_REFERENCE_PREFIX = "FirebaseAuthTestTags."

        /** The modifier that exposes tags beneath a semantics owner as Android resource ids. */
        const val FLAG_FUNCTION_NAME = "exposeTestTagsAsResourceIds"

        /**
         * `exposeTestTagsAsResourceIds(` as a call. The declaration in `TestTagsAsResourceIds.kt`
         * matches too, but that file applies no tags and so is never examined.
         */
        val FLAG_APPLICATION_PATTERN = Regex("""\b$FLAG_FUNCTION_NAME\s*\(""")

        /**
         * The Compose call shapes that create a semantics owner the library is responsible for
         * flagging. Dialogs, popups and bottom sheets are each hosted in their own window with their
         * own semantics root; a `Scaffold` is a subtree root that a sibling `Scaffold`'s flag does not
         * reach. Longest-first is not needed — `\b` already stops `Dialog` from matching inside
         * `AlertDialog` — but alphabetical order keeps `AlertDialog` ahead of it anyway.
         */
        val SEMANTICS_OWNER_SHAPES = listOf(
            "AlertDialog",
            "Dialog",
            "ModalBottomSheet",
            "PlainTooltip",
            "Popup",
            "Scaffold",
        )

        /**
         * One of [SEMANTICS_OWNER_SHAPES] being constructed. `{` is accepted as the delimiter as well
         * as `(` so that a trailing-lambda-only call, which can carry no modifier and therefore no
         * flag, is caught rather than missed.
         */
        val OWNER_CONSTRUCTION_PATTERN =
            Regex("""\b(${SEMANTICS_OWNER_SHAPES.joinToString("|")})\s*[({]""")

        /**
         * A function declaration at any indentation, used to find the declaration enclosing a call
         * site so its annotations can be read.
         */
        val FUNCTION_DECLARATION_PATTERN = Regex(
            """(?m)^[ \t]*(?:(?:private|internal|public|protected|expect|actual|override|""" +
                """suspend|inline|operator|infix)\s+)*fun\s"""
        )

        /** Covers `@Preview` and its multipreview variants, such as `@PreviewLightDark`. */
        const val PREVIEW_ANNOTATION_PREFIX = "@Preview"

        /**
         * The registry, used as the sentinel that a candidate directory really is the auth main
         * source tree. Both conventional Kotlin source directories are tried, so moving the registry
         * from `java/` to `kotlin/` does not disarm the scan.
         */
        val REGISTRY_RELATIVE_PATHS = listOf(
            "java/com/firebase/ui/auth/ui/FirebaseAuthTestTags.kt",
            "kotlin/com/firebase/ui/auth/ui/FirebaseAuthTestTags.kt",
        )

        /** Working directory is the module directory under Gradle's default. */
        const val MODULE_RELATIVE_MAIN_SOURCES = "src/main"

        /** Working directory is the repository root, as when launched from an IDE run config. */
        const val REPOSITORY_RELATIVE_MAIN_SOURCES = "auth/src/main"

        const val MAX_ANCESTOR_WALK = 6

        const val KOTLIN_EXTENSION = "kt"

        const val BLOCK_COMMENT_OPEN = "/*"

        const val BLOCK_COMMENT_CLOSE = "*/"

        const val LINE_COMMENT = "//"

        const val RAW_STRING_QUOTE = "\"\"\""

        val OPENING_BRACKETS = setOf('(', '[', '{')

        val CLOSING_BRACKETS = setOf(')', ']', '}')

        /**
         * `fun testTag(` declares one rather than calling it, and `fun Scaffold(` would declare a
         * composable rather than construct one.
         */
        val CALL_DISQUALIFYING_KEYWORDS = setOf("fun")

        /** `val testTag =` binds a name; it does not assign a semantics property. */
        val BINDING_KEYWORDS = setOf("val", "var")

        /**
         * `testTag(` as a call, or `testTag =` as a `SemanticsPropertyReceiver` assignment. Both
         * apply a tag, so both are checked; the trailing delimiter says which form matched.
         * [isTagApplication] then discards the matches that are neither.
         */
        val TAG_APPLICATION_PATTERN = Regex("""\btestTag\s*[(=]""")

        /** `import …testTag as somethingElse`, in any of the packages that declare a `testTag`. */
        val ALIASED_IMPORT_PATTERN = Regex("""(?m)^\s*import\s+[\w.]*\btestTag\s+as\s+\w+.*$""")

        val WHITESPACE_RUN = Regex("""\s+""")
    }
}
