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

import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import java.lang.reflect.Modifier
import org.junit.Test

/**
 * Enforces the invariants [FirebaseAuthTestTags] documents, by reflecting over the whole registry
 * — the root object and every nested grouping object — rather than over a hand-maintained list.
 *
 * The registry is a plain Kotlin object of [String] constants, so these tests need no Android
 * runtime and run on plain JUnit.
 *
 * @suppress Internal test class
 */
class FirebaseAuthTestTagsTest {

    /**
     * Valid Android resource names: a `fui_` prefix followed by lowercase `snake_case` segments.
     */
    private val resourceNamePattern = Regex("^fui_[a-z0-9]+(_[a-z0-9]+)*$")

    @Test
    fun `reflective traversal reaches every nested tag group`() {
        val tags = registeredTags()

        assertWithMessage(
            "Reflective traversal of FirebaseAuthTestTags found no tags. The traversal must walk " +
                "the nested grouping objects, not just the registry root — otherwise the " +
                "resource-name and uniqueness assertions in this class pass vacuously."
        ).that(tags).isNotEmpty()

        assertThat(tags.keys).containsAtLeast(
            "FirebaseAuthTestTags.MethodPicker.PROVIDER_LIST",
            "FirebaseAuthTestTags.MethodPicker.CONTINUE_AS_BUTTON",
            "FirebaseAuthTestTags.CountrySelector.COUNTRY_LIST"
        )
    }

    @Test
    fun `every tag value is a valid resource name`() {
        registeredTags().forEach { (path, value) ->
            assertWithMessage(
                "Test tag value \"$value\" declared at $path is not a valid Android resource " +
                    "name. Tag values are surfaced as resource ids, so each must be lowercase " +
                    "snake_case with a fui_ prefix and match " +
                    "${resourceNamePattern.pattern} — no spaces, capitals, or leading, " +
                    "trailing, or doubled underscores."
            ).that(value).matches(resourceNamePattern.pattern)
        }
    }

    @Test
    fun `every tag value is distinct across the registry`() {
        val duplicates = registeredTags().entries
            .groupBy({ it.value }) { it.key }
            .filterValues { paths -> paths.size > 1 }

        assertWithMessage(
            "Tag values must be distinct across the whole registry. Two constants resolving to " +
                "the same value make onNodeWithTag and By.res() match more than one node, so " +
                "neither a Compose assertion nor a Robo directive can address either node. " +
                "Duplicated values: $duplicates"
        ).that(duplicates).isEmpty()
    }

    @Test
    fun `every tag lives in a grouping object and repeats its surface`() {
        registeredTags().forEach { (path, value) ->
            val segments = path.split('.')

            // Exactly one grouping level, in both directions: the surface prefix asserted below
            // is read from segments[1], so a second nesting level would leave it ambiguous which
            // of the two enclosing groups a value has to repeat.
            assertWithMessage(
                if (segments.size < EXPECTED_PATH_SEGMENTS) {
                    "Tag $path is declared directly on the registry root. Every tag belongs to a " +
                        "grouping object named after the screen or surface that owns it, so that " +
                        "sibling tags stay unambiguous as the registry grows."
                } else {
                    "Tag $path is nested more than one grouping object deep. A group names a " +
                        "single screen or surface and does not nest further, because the tag " +
                        "value repeats exactly one group as its prefix. Flatten it to " +
                        "FirebaseAuthTestTags.<Surface>.<TAG>."
                }
            ).that(segments).hasSize(EXPECTED_PATH_SEGMENTS)

            val surface = segments[1].toSnakeCase()
            assertWithMessage(
                "Test tag value \"$value\" declared at $path does not carry its surface. Values " +
                    "repeat the grouping object as a segment — expected the fui_${surface}_ " +
                    "prefix — so that a By.res prefix match selects exactly one surface."
            ).that(value).startsWith("fui_${surface}_")
        }
    }

    @Test
    fun `surface names keep acronym runs in one segment`() {
        assertThat("MethodPicker".toSnakeCase()).isEqualTo("method_picker")
        assertThat("CountrySelector".toSnakeCase()).isEqualTo("country_selector")
        // Splitting inside an acronym would force a group named after one of the OAuth providers
        // to name its values fui_o_auth_provider_* to satisfy the surface-prefix assertion.
        assertThat("OAuthProvider".toSnakeCase()).isEqualTo("oauth_provider")
    }

    /**
     * Maps every tag constant in the registry to its value, keyed by the constant's fully nested
     * path (for example `FirebaseAuthTestTags.MethodPicker.PROVIDER_LIST`).
     */
    private fun registeredTags(): Map<String, String> {
        val tags = linkedMapOf<String, String>()
        collectTags(FirebaseAuthTestTags::class.java, "FirebaseAuthTestTags", tags)
        return tags
    }

    private fun collectTags(group: Class<*>, path: String, into: MutableMap<String, String>) {
        group.declaredFields
            .filter { field ->
                !field.isSynthetic &&
                        Modifier.isStatic(field.modifiers) &&
                        field.type == String::class.java
            }
            .forEach { field ->
                field.isAccessible = true
                into["$path.${field.name}"] = field.get(null) as String
            }

        // A computed tag (`val TAG: String get() = …`) has no backing field, so the scan above
        // would miss it and it would escape every invariant in this class. Read it through its
        // getter instead, skipping the getters that back the fields already collected.
        val singleton = group.declaredFields
            .firstOrNull { Modifier.isStatic(it.modifiers) && it.type == group }
            ?.also { it.isAccessible = true }
            ?.get(null)

        group.declaredMethods
            .filter { method ->
                !method.isSynthetic &&
                        method.parameterCount == 0 &&
                        method.returnType == String::class.java &&
                        method.name.startsWith(GETTER_PREFIX) &&
                        method.name.length > GETTER_PREFIX.length
            }
            .forEach { method ->
                val name = method.name.removePrefix(GETTER_PREFIX)
                if (into.containsKey("$path.$name")) return@forEach

                val receiver = if (Modifier.isStatic(method.modifiers)) {
                    null
                } else {
                    checkNotNull(singleton) {
                        "Tag group ${group.name} exposes $name through a getter but has no " +
                            "singleton instance to read it from; tag groups must be objects."
                    }
                }
                method.isAccessible = true
                into["$path.$name"] = method.invoke(receiver) as String
            }

        group.declaredClasses.forEach { nested ->
            collectTags(nested, "$path.${nested.simpleName}", into)
        }
    }

    /**
     * Converts a grouping object name to the `snake_case` surface segment its tag values repeat.
     * A run of capitals stays one segment, so `OAuthProvider` becomes `oauth_provider` rather than
     * `o_auth_provider`.
     */
    private fun String.toSnakeCase(): String =
        replace(Regex("(?<=[a-z0-9])(?=\\p{Upper})"), "_").lowercase()

    private companion object {
        /** `FirebaseAuthTestTags` + one grouping object + the constant itself. */
        const val EXPECTED_PATH_SEGMENTS = 3

        const val GETTER_PREFIX = "get"
    }
}
