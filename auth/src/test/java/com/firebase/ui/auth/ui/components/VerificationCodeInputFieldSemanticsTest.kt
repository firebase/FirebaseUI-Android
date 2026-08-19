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

package com.firebase.ui.auth.ui.components

import android.content.Context
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.AccessibilityAction
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.SemanticsPropertyKey
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.text.AnnotatedString
import androidx.test.core.app.ApplicationProvider
import com.firebase.ui.auth.configuration.string_provider.DefaultAuthUIStringProvider
import com.firebase.ui.auth.configuration.string_provider.LocalAuthUIStringProvider
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * The text-input contract [VerificationCodeInputField] declares on the group that holds its digit
 * boxes.
 *
 * That contract is the reason the group carries semantics at all. The boxes accept one character
 * each, so nothing could be handed a whole code, and the node a caller can address by test tag had
 * no text-input action — a Firebase Test Lab Robo `inputText` directive naming it resolved a node and
 * typed nothing, which is issue #2050 reproduced on the verification screen.
 * [com.firebase.ui.auth.ui.TestTagsAsResourceIdsTest] proves the code arrives through the resource id
 * a crawler would use; this class pins the behaviour underneath that — how a string is spread across
 * the boxes, and what happens to input the boxes cannot hold.
 *
 * The rejection cases matter as much as the accepting ones. Both actions return a `Boolean`, and
 * silently truncating over-long or non-numeric input would hand a caller a half-entered code and a
 * success. So the actions are invoked directly here rather than through `performTextReplacement`,
 * which discards the result.
 *
 * @suppress Internal test class
 */
@Config(manifest = Config.NONE, sdk = [34])
@RunWith(RobolectricTestRunner::class)
class VerificationCodeInputFieldSemanticsTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val entered = mutableListOf<String>()
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val stringProvider = DefaultAuthUIStringProvider(context)

    private fun setContent(codeLength: Int = 6) {
        composeTestRule.setContent {
            CompositionLocalProvider(
                LocalAuthUIStringProvider provides stringProvider
            ) {
                VerificationCodeInputField(
                    modifier = Modifier.testTag(CODE_FIELD_TAG),
                    codeLength = codeLength,
                    onCodeChange = { entered += it }
                )
            }
        }
    }

    /** The code the group currently holds, as its callback last reported it. */
    private fun currentCode(): String = entered.last()

    /** The code the group publishes on itself, which is what a reader of the node sees. */
    private fun editableText(): String =
        composeTestRule
            .onNodeWithTag(CODE_FIELD_TAG)
            .fetchSemanticsNode()
            .config[SemanticsProperties.EditableText]
            .text

    /**
     * Invokes a text action on the group directly, so its `Boolean` result — which the Compose test
     * APIs throw away — can be asserted.
     */
    private fun invokeSetText(text: String): Boolean = invoke(SemanticsActions.SetText, text)

    private fun invokeInsert(text: String): Boolean =
        invoke(SemanticsActions.InsertTextAtCursor, text)

    private fun invoke(
        key: SemanticsPropertyKey<AccessibilityAction<(AnnotatedString) -> Boolean>>,
        text: String,
    ): Boolean {
        val config = composeTestRule.onNodeWithTag(CODE_FIELD_TAG).fetchSemanticsNode().config

        assertWithMessage(
            "The node tagged $CODE_FIELD_TAG declares no ${key.name} action, so it cannot be typed " +
                "into from outside Compose — which is the whole point of declaring semantics on the " +
                "group. Its config: ${config.joinToString { it.key.name }}."
        ).that(config.contains(key)).isTrue()

        val action = requireNotNull(config[key].action) { "${key.name} has a null handler." }
        val result = composeTestRule.runOnIdle { action(AnnotatedString(text)) }
        composeTestRule.waitForIdle()
        return result
    }

    @Test
    fun `setText spreads a whole code across the boxes`() {
        setContent()

        assertThat(invokeSetText("123456")).isTrue()
        assertThat(currentCode()).isEqualTo("123456")
    }

    @Test
    fun `setText replaces rather than appends`() {
        setContent()

        invokeSetText("123456")
        assertThat(invokeSetText("987")).isTrue()

        assertWithMessage(
            "setText is a replacement, so a shorter code must leave the trailing boxes empty " +
                "rather than keeping digits from the previous one."
        ).that(currentCode()).isEqualTo("987")
    }

    @Test
    fun `setText clears the code when given an empty string`() {
        setContent()

        invokeSetText("123456")
        assertThat(invokeSetText("")).isTrue()
        assertThat(currentCode()).isEmpty()
    }

    @Test
    fun `setText refuses input the boxes cannot hold`() {
        setContent()

        invokeSetText("12")

        assertWithMessage("A letter is not a digit in any script, so no box can show it.")
            .that(invokeSetText("12a456")).isFalse()
        assertWithMessage("Seven digits do not fit in six boxes.")
            .that(invokeSetText("1234567")).isFalse()

        assertWithMessage(
            "A refused action must leave the code untouched; anything else is a partial write " +
                "reported as a failure."
        ).that(currentCode()).isEqualTo("12")
    }

    @Test
    fun `insertTextAtCursor fills forward from the first empty box`() {
        setContent()

        assertThat(invokeInsert("12")).isTrue()
        assertThat(invokeInsert("3456")).isTrue()
        assertThat(currentCode()).isEqualTo("123456")
    }

    @Test
    fun `insertTextAtCursor refuses more digits than there are empty boxes`() {
        setContent()

        invokeInsert("1234")

        assertWithMessage("Three digits do not fit in the two remaining boxes.")
            .that(invokeInsert("567")).isFalse()

        invokeInsert("56")
        assertWithMessage("A full code leaves nowhere to insert.")
            .that(invokeInsert("7")).isFalse()

        assertThat(currentCode()).isEqualTo("123456")
    }

    /**
     * The path a host application takes, as the registry KDoc invites: `performTextInput` against the
     * published tag rather than against any single box.
     */
    @Test
    fun `performTextInput and performTextReplacement both enter a whole code`() {
        setContent()

        composeTestRule.onNodeWithTag(CODE_FIELD_TAG).performTextInput("123456")
        composeTestRule.waitForIdle()
        assertThat(currentCode()).isEqualTo("123456")

        composeTestRule.onNodeWithTag(CODE_FIELD_TAG).performTextReplacement("654321")
        composeTestRule.waitForIdle()
        assertThat(currentCode()).isEqualTo("654321")
    }

    /**
     * Inserting nothing is a no-op, and a no-op reports success — including on a full code, where
     * there is no empty box to insert into. The two guards in `insertTextAtCursor` are ordered for
     * this: the emptiness check runs before the "nowhere to insert" check, so an empty insert cannot
     * be reported as a failed action.
     */
    @Test
    fun `insertTextAtCursor accepts an empty insert whether or not the code is full`() {
        setContent()

        assertWithMessage("Inserting nothing into an empty code changes nothing and fails nothing.")
            .that(invokeInsert("")).isTrue()

        invokeInsert("123456")

        assertWithMessage(
            "Inserting nothing into a full code is still a no-op. Reporting failure here would " +
                "have a caller believe its input was refused when it asked for nothing."
        ).that(invokeInsert("")).isTrue()
        assertThat(currentCode()).isEqualTo("123456")
    }

    /**
     * Pins the digit definition the actions use, which is `Character.isDigit`'s rather than ASCII
     * `0`-`9`: a code in Arabic-Indic or fullwidth digits is accepted and normalised to its ASCII
     * value.
     *
     * This is behaviour worth pinning rather than narrowing. The per-box keyboard path normalises the
     * same way, so restricting only these two actions to ASCII would leave a code that can be typed by
     * hand on a localised keypad refused through `ACTION_SET_TEXT`. Nothing malformed gets in either
     * way — the length and digit checks still hold, so what arrives is a complete, well-formed code.
     */
    @Test
    fun `the actions accept non ASCII digits and normalise them`() {
        setContent()

        assertThat(invokeSetText(ARABIC_INDIC_CODE)).isTrue()
        assertWithMessage(
            "Arabic-Indic digits are digits to Character.isDigit and carry the same values, so the " +
                "code arrives normalised to ASCII rather than refused."
        ).that(currentCode()).isEqualTo("123456")

        assertThat(invokeSetText(FULLWIDTH_CODE)).isTrue()
        assertThat(currentCode()).isEqualTo("123")
    }

    /**
     * The group's `editableText` is the digits before the first empty box, not every digit entered.
     *
     * Boxes can be filled out of order — tapping the third box moves focus straight to it — so
     * compacting the whole list would report `[1, 2, null, 4]` as `"124"` and place a digit at a
     * position that is empty. A prefix understates instead, which is what a text field with a cursor
     * means anyway and what `insertTextAtCursor` appends to. `editableText` is read back by anything
     * holding the node, a crawler included, so it has to be positionally honest.
     */
    @Test
    fun `editableText reports the digits before the first gap rather than a compacted code`() {
        setContent()

        invokeInsert("12")

        assertThat(editableText()).isEqualTo("12")

        // A gap: the fourth box is filled directly, as tapping it and typing would.
        composeTestRule.onAllNodesWithContentDescription(DIGIT_BOX_DESCRIPTION, substring = true)[3]
            .performTextInput("4")
        composeTestRule.waitForIdle()

        assertWithMessage(
            "The third box is empty, so the code the node reports must stop at it. Compacting to " +
                "\"124\" would claim a 4 in the third position."
        ).that(editableText()).isEqualTo("12")

        assertWithMessage(
            "onCodeChange stays compacted — screens on this branch read it as the code so far — so " +
                "the two deliberately disagree while there is a gap."
        ).that(currentCode()).isEqualTo("124")

        // Filling the gap makes the prefix the whole code again.
        composeTestRule.onAllNodesWithContentDescription(DIGIT_BOX_DESCRIPTION, substring = true)[2]
            .performTextInput("3")
        composeTestRule.waitForIdle()

        assertThat(editableText()).isEqualTo("1234")
    }

    /** A code that is not six digits long, to pin that the actions follow `codeLength`. */
    @Test
    fun `the actions follow a non default code length`() {
        setContent(codeLength = 4)

        assertThat(invokeSetText("1234")).isTrue()
        assertThat(currentCode()).isEqualTo("1234")
        assertThat(invokeSetText("12345")).isFalse()
        assertThat(currentCode()).isEqualTo("1234")
    }

    private companion object {
        /**
         * A fixture-local tag. The registry values belong to screens, and this exercises the widget
         * on its own — a host application tagging our node with its own value is supported and this
         * stands in for that too.
         */
        const val CODE_FIELD_TAG = "verification_code_group_under_test"

        /**
         * The shared prefix of every digit box's content description (each box's full
         * description is positional, e.g. "Verification code digit 3 of 6"), used with a
         * substring match to reach one box directly by its index in the row.
         */
        const val DIGIT_BOX_DESCRIPTION = "Verification code digit"

        /** `123456` in Arabic-Indic digits. */
        const val ARABIC_INDIC_CODE = "١٢٣٤٥٦"

        /** `123` in fullwidth digits. */
        const val FULLWIDTH_CODE = "１２３"
    }
}
