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

import android.content.Context
import android.os.Bundle
import android.view.accessibility.AccessibilityNodeInfo
import android.view.accessibility.AccessibilityNodeProvider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ViewRootForTest
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.hasAnyDescendant
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ApplicationProvider
import com.firebase.ui.auth.AuthException
import com.firebase.ui.auth.FirebaseAuthUI
import com.firebase.ui.auth.configuration.AuthUIConfiguration
import com.firebase.ui.auth.configuration.authUIConfiguration
import com.firebase.ui.auth.configuration.auth_provider.AuthProvider
import com.firebase.ui.auth.configuration.string_provider.AuthUIStringProvider
import com.firebase.ui.auth.configuration.string_provider.DefaultAuthUIStringProvider
import com.firebase.ui.auth.configuration.MfaFactor
import com.firebase.ui.auth.configuration.string_provider.LocalAuthUIStringProvider
import com.firebase.ui.auth.mfa.MfaChallengeContentState
import com.firebase.ui.auth.mfa.MfaEnrollmentContentState
import com.firebase.ui.auth.mfa.MfaEnrollmentStep
import com.firebase.ui.auth.ui.components.CountrySelector
import com.firebase.ui.auth.ui.components.ErrorRecoveryDialog
import com.firebase.ui.auth.ui.components.ReauthenticationDialog
import com.firebase.ui.auth.ui.method_picker.AuthMethodPicker
import com.firebase.ui.auth.ui.screens.DefaultMfaChallengeContent
import com.firebase.ui.auth.ui.screens.DefaultMfaEnrollmentContent
import com.firebase.ui.auth.ui.screens.FirebaseAuthScreen
import com.firebase.ui.auth.ui.screens.email.ResetPasswordUI
import com.firebase.ui.auth.ui.screens.email.SignInEmailLinkUI
import com.firebase.ui.auth.ui.screens.email.SignInUI
import com.firebase.ui.auth.ui.screens.email.SignUpUI
import com.firebase.ui.auth.ui.screens.phone.EnterPhoneNumberUI
import com.firebase.ui.auth.ui.screens.phone.EnterVerificationCodeUI
import com.firebase.ui.auth.util.CountryUtils
import com.firebase.ui.auth.util.SignInPreferenceManager
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.MultiFactorInfo
import com.google.firebase.auth.PhoneMultiFactorInfo
import com.google.firebase.auth.TotpMultiFactorInfo
import com.google.firebase.auth.actionCodeSettings
import com.google.common.truth.Truth.assertWithMessage
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * The end-to-end check for the reason [FirebaseAuthTestTags] exists: that each published tag is
 * applied to a real node on its screen, and that the node carries the tag as an Android **resource
 * id** rather than only as a Compose test tag.
 *
 * That distinction is the whole point, and it is why these tests read `viewIdResourceName` off the
 * accessibility node instead of calling `assertExists()` on a `testTag` matcher. A `testTag` is
 * visible only to Compose's own test APIs. Firebase Test Lab Robo directives, the Play pre-launch
 * report's crawler, and UiAutomator's `By.res()` all match on the resource name, and they see it
 * only when [exposeTestTagsAsResourceIds] has been applied at the enclosing semantics owner. An
 * `onNodeWithTag(...).assertExists()` passes identically with and without that modifier, so it
 * cannot tell the fixed state from the bug — issue #2050, where Robo could type a username but
 * never reach the password field.
 *
 * Two harness details are load-bearing:
 *
 * * `@GraphicsMode(NATIVE)`. Compose builds its accessibility node tree by subtracting each node's
 *   bounds from an `android.graphics.Region` of unaccounted space. Under Robolectric's legacy
 *   graphics that `Region` is inert, every node below the root is treated as covered, and
 *   `createAccessibilityNodeInfo` returns an empty node for which `viewIdResourceName` is always
 *   `null` — the assertions here would fail for a reason that has nothing to do with the library.
 * * Scrolling a node into the window before reading it. Nodes lying outside the root's bounds are
 *   culled from that same tree, and these screens are taller than the test window, so a tag near
 *   the bottom of a screen has no accessibility node until it is scrolled to. Enlarging the window
 *   is the obvious alternative and is deliberately not used: at `h2400dp` an `AlertDialog`
 *   containing a text field — the shape [com.firebase.ui.auth.ui.components.ReauthenticationDialog]
 *   has — sends Robolectric's text measurement into a runaway allocation and the suite dies with an
 *   `OutOfMemoryError` that has nothing to do with test tags. It reproduces with stock Material 3
 *   components and no test tags involved, so it is a harness limit, not a library one.
 *
 * Dialogs and bottom sheets are covered explicitly rather than incidentally. Each is hosted in its
 * own window with its own semantics root, so it inherits nothing from the composable that opened it
 * — the case with no coverage before this class existed, and the one where forgetting the modifier
 * is invisible in a Compose-only assertion.
 *
 * ## What is deliberately not covered here
 *
 * [exposeTestTagsAsResourceIds] is applied to every semantics owner the library creates, including
 * the ones that hold no tag today, and those flags have no test of their own. That is intentional
 * twice over. There is nothing to assert — the property is only observable through a tag beneath it,
 * so a test would have to plant its own tag and would then be testing Compose. And the flags exist
 * precisely because no test can see them missing: the whole reason for flagging owners rather than
 * tags is that a tag added inside an unflagged owner keeps every Compose assertion green. The loading
 * dialog, the default re-authentication sheet, the manage-MFA tooltip, and the TOTP enrollment steps
 * are all in that state.
 *
 * The one case where a flag-without-tags *is* asserted is `error recovery dialog exposes a caller
 * supplied tag`, which stands in for all of them: it plants a caller tag in an owner the library
 * flags but does not tag, and so proves the mechanism works the moment a tag arrives.
 *
 * @suppress Internal test class
 */
@Config(sdk = [34])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@RunWith(RobolectricTestRunner::class)
class TestTagsAsResourceIdsTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var applicationContext: Context
    private lateinit var stringProvider: AuthUIStringProvider

    @Before
    fun setUp() {
        applicationContext = ApplicationProvider.getApplicationContext()
        stringProvider = DefaultAuthUIStringProvider(applicationContext)
    }

    // =============================================================================================
    // The assertion this class is built around
    // =============================================================================================

    /**
     * Asserts that exactly one node carries [tag], that it is the kind of node [expected] describes,
     * and that the platform sees the tag as a resource id.
     *
     * The node kind is asserted alongside the tag because "the tag exists somewhere" is not the
     * claim worth making — a tag parked on a `Spacer` next to the password field would satisfy it
     * while leaving Robo with nothing to type into. So a field is required to accept text and a
     * button to be clickable.
     *
     * `node.root` is read rather than assumed, so the same helper works for a dialog or bottom
     * sheet: those live in a different window, and this resolves whichever window actually hosts
     * the node.
     */
    private fun assertExposedAsResourceId(tag: String, expected: SemanticsMatcher) {
        val interaction: SemanticsNodeInteraction = composeTestRule.onNode(hasTestTag(tag))

        interaction.assertExists(
            "No node carries the test tag \"$tag\". Either the tag was not applied on this screen, " +
                "or the node it belongs to is not composed in this fixture."
        )
        interaction.assert(expected)
        scrollIntoWindow(tag)

        val node = interaction.fetchSemanticsNode()
        val view = (node.root as ViewRootForTest).view
        val provider = requireNotNull(view.accessibilityNodeProvider) {
            "The Compose view hosting \"$tag\" exposes no AccessibilityNodeProvider, so this test " +
                "cannot read viewIdResourceName from it."
        }
        val info = requireNotNull(provider.createAccessibilityNodeInfo(node.id)) {
            "No accessibility node exists for the node tagged \"$tag\"."
        }

        assertWithMessage(
            "The node tagged \"$tag\" is not exposed as an Android resource id: its accessibility " +
                "node reports viewIdResourceName=${info.viewIdResourceName}. Firebase Test Lab " +
                "Robo directives and UiAutomator By.res() match on that field, so without it the " +
                "tag is reachable from Compose tests only and issue #2050 is not fixed. Fix: apply " +
                "Modifier.exposeTestTagsAsResourceIds() at the semantics owner enclosing this node " +
                "— and note that every dialog and bottom sheet is its own semantics owner, which " +
                "inherits nothing from the composable that opened it."
        ).that(info.viewIdResourceName).isEqualTo(tag)
    }

    /**
     * Scrolls the node tagged [tag] into the window when it sits inside a scrollable, because a node
     * outside the root's bounds has no accessibility node to read a resource id from.
     *
     * The scrollable ancestor is looked for rather than the scroll being attempted unconditionally,
     * so that a genuine `performScrollTo` failure still fails the test instead of being swallowed.
     */
    private fun scrollIntoWindow(tag: String) {
        val hasScrollableAncestor = composeTestRule
            .onAllNodes(hasScrollAction() and hasAnyDescendant(hasTestTag(tag)))
            .fetchSemanticsNodes()
            .isNotEmpty()

        if (hasScrollableAncestor) {
            composeTestRule.onNode(hasTestTag(tag)).performScrollTo()
        }
    }

    /**
     * Enters [text] into whatever node the platform publishes under the resource id [resourceName],
     * the way something outside Compose would: the node is found by scanning the accessibility tree
     * for that resource id, and the text is delivered through `ACTION_SET_TEXT` — the action behind
     * a Robo `inputText` directive and UiAutomator's `setText`.
     *
     * The Compose test tag is deliberately not used to locate the node, because that is the claim
     * under test. `performTextInput` on a tag shows that a Compose test can type; it says nothing
     * about whether a crawler holding only a resource name can, which is the gap issue #2050 is
     * about.
     *
     * Two properties are asserted before the text is sent, because a crawler reads both and either
     * one alone can be satisfied by a node it will never type into. `className` has to be
     * [EDIT_TEXT_CLASS_NAME] — the field a crawler consults to decide a node accepts text at all,
     * and the only class Robo's `inputText` directives are documented for — and `ACTION_SET_TEXT`
     * has to be offered. They fail independently: a plain container can advertise the action, and a
     * node with the action can lose the class to an unrelated semantics property.
     */
    private fun setTextByResourceId(resourceName: String, text: String) {
        val (provider, virtualViewId) = accessibilityNodePublishing(resourceName)

        val info = requireNotNull(provider.createAccessibilityNodeInfo(virtualViewId)) {
            "The accessibility node published as \"$resourceName\" disappeared between being " +
                "found and being read."
        }

        assertWithMessage(
            "The node published as \"$resourceName\" reports className=${info.className} rather " +
                "than $EDIT_TEXT_CLASS_NAME, so a crawler will not treat it as typeable however " +
                "many text actions it offers. Robo decides a node accepts text from its class, and " +
                "its documented directive support is EditText-only. Compose derives the class from " +
                "the node's semantics: `AndroidComposeViewAccessibilityDelegateCompat` maps " +
                "EditableText to EditText, but prefers Text over it — so adding a Text or Role " +
                "property anywhere in this node's modifier chain, including through a modifier a " +
                "caller passes in, silently turns this into a TextView while ACTION_SET_TEXT stays " +
                "on offer and every other assertion in this class keeps passing. Fix: keep the " +
                "group's semantics to isEditable/editableText/maxTextLength and the text actions, " +
                "and put any label on a child rather than on the node itself."
        ).that(info.className?.toString()).isEqualTo(EDIT_TEXT_CLASS_NAME)

        assertWithMessage(
            "The node published as \"$resourceName\" offers no ACTION_SET_TEXT, so a Robo " +
                "inputText directive naming it would resolve a node and type nothing. Compose " +
                "offers that action only where SemanticsActions.SetText is declared, so the node " +
                "needs Modifier.semantics { setText { … } } (or to be a real text field). Actions " +
                "offered: ${info.actionList.map { it.id }}."
        ).that(info.actionList.map { it.id }).contains(AccessibilityNodeInfo.ACTION_SET_TEXT)

        val arguments = Bundle().apply {
            putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, text)
        }

        assertWithMessage(
            "ACTION_SET_TEXT on \"$resourceName\" with \"$text\" was refused. The node advertises " +
                "the action, so its SetText handler rejected the value."
        ).that(
            provider.performAction(
                virtualViewId,
                AccessibilityNodeInfo.ACTION_SET_TEXT,
                arguments
            )
        ).isTrue()

        composeTestRule.waitForIdle()
    }

    /**
     * The accessibility node whose `viewIdResourceName` is [resourceName], together with the
     * provider that owns it, asserting that exactly one node claims the id.
     *
     * Every semantics node in the tree is examined rather than the one carrying a matching test tag,
     * so this resolves the id the same way `By.res()` does — and so a tag that never reached the
     * accessibility tree fails here instead of being found by the back door.
     */
    private fun accessibilityNodePublishing(
        resourceName: String
    ): Pair<AccessibilityNodeProvider, Int> {
        val matches = composeTestRule
            .onAllNodes(ANY_NODE, useUnmergedTree = true)
            .fetchSemanticsNodes()
            .mapNotNull { node: SemanticsNode ->
                val provider = (node.root as? ViewRootForTest)
                    ?.view
                    ?.accessibilityNodeProvider
                    ?: return@mapNotNull null
                val info = provider.createAccessibilityNodeInfo(node.id)
                if (info?.viewIdResourceName == resourceName) provider to node.id else null
            }

        assertWithMessage(
            "Expected exactly one accessibility node to publish viewIdResourceName " +
                "\"$resourceName\". Zero means the tag is not exposed as a resource id here — " +
                "either the tag is not applied, its node is scrolled out of the window, or the " +
                "enclosing semantics owner is missing " +
                "Modifier.exposeTestTagsAsResourceIds(). More than one means By.res() cannot " +
                "address either of them. Found ${matches.size}."
        ).that(matches).hasSize(1)

        return matches.single()
    }

    private fun field(): SemanticsMatcher = hasSetTextAction()

    private fun button(): SemanticsMatcher = hasClickAction()

    // =============================================================================================
    // Fixtures
    // =============================================================================================

    private fun setContent(content: @Composable () -> Unit) {
        composeTestRule.setContent {
            CompositionLocalProvider(LocalAuthUIStringProvider provides stringProvider) {
                content()
            }
        }
    }

    private fun emailConfiguration(
        isDisplayNameRequired: Boolean = true,
        isEmailLinkSignInEnabled: Boolean = true,
        isNewAccountsAllowed: Boolean = true,
    ): AuthUIConfiguration = authUIConfiguration {
        context = applicationContext
        providers {
            provider(
                AuthProvider.Email(
                    isDisplayNameRequired = isDisplayNameRequired,
                    isEmailLinkSignInEnabled = isEmailLinkSignInEnabled,
                    isNewAccountsAllowed = isNewAccountsAllowed,
                    emailLinkActionCodeSettings = if (isEmailLinkSignInEnabled) {
                        actionCodeSettings {
                            url = "https://example.com"
                            handleCodeInApp = true
                            setAndroidPackageName("com.example", true, null)
                        }
                    } else {
                        null
                    },
                    passwordValidationRules = emptyList()
                )
            )
        }
    }

    private fun phoneConfiguration(): AuthUIConfiguration = authUIConfiguration {
        context = applicationContext
        providers {
            provider(
                AuthProvider.Phone(
                    defaultNumber = null,
                    defaultCountryCode = null,
                    allowedCountries = null
                )
            )
        }
    }

    // =============================================================================================
    // In-window screen roots
    // =============================================================================================

    @Test
    fun `sign in screen exposes its credential fields and actions`() {
        setContent {
            SignInUI(
                configuration = emailConfiguration(),
                isLoading = false,
                emailSignInLinkSent = false,
                email = "",
                password = "password123",
                onEmailChange = { },
                onPasswordChange = { },
                onRetrievedCredential = { },
                onSignInClick = { },
                onGoToSignUp = { },
                onGoToResetPassword = { },
                onGoToEmailLinkSignIn = { },
                onNavigateBack = { },
            )
        }

        assertExposedAsResourceId(FirebaseAuthTestTags.SignIn.EMAIL_FIELD, field())
        assertExposedAsResourceId(FirebaseAuthTestTags.SignIn.PASSWORD_FIELD, field())
        assertExposedAsResourceId(FirebaseAuthTestTags.SignIn.SIGN_IN_BUTTON, button())
        assertExposedAsResourceId(FirebaseAuthTestTags.SignIn.SIGN_UP_BUTTON, button())
        assertExposedAsResourceId(FirebaseAuthTestTags.SignIn.FORGOT_PASSWORD_BUTTON, button())
        assertExposedAsResourceId(FirebaseAuthTestTags.SignIn.EMAIL_LINK_BUTTON, button())
        assertExposedAsResourceId(FirebaseAuthTestTags.SignIn.BACK_BUTTON, button())
        assertExposedAsResourceId(FirebaseAuthTestTags.TermsAndPrivacy.TOS_LINK, button())
        assertExposedAsResourceId(FirebaseAuthTestTags.TermsAndPrivacy.PRIVACY_LINK, button())
    }

    /**
     * The password visibility toggle on [AuthTextField] is a shared, low-level component reused by
     * every password-holding screen, so it needs a distinct tag per call site — this pins the
     * end-to-end path for the sign-in screen's toggle: [AuthTextField]'s new
     * `visibilityToggleModifier` parameter reaching a real Android resource id.
     */
    @Test
    fun `sign in screen exposes its password visibility toggle`() {
        setContent {
            SignInUI(
                configuration = emailConfiguration(),
                isLoading = false,
                emailSignInLinkSent = false,
                email = "",
                password = "password123",
                onEmailChange = { },
                onPasswordChange = { },
                onRetrievedCredential = { },
                onSignInClick = { },
                onGoToSignUp = { },
                onGoToResetPassword = { },
                onGoToEmailLinkSignIn = { },
            )
        }

        assertExposedAsResourceId(FirebaseAuthTestTags.SignIn.PASSWORD_VISIBILITY_TOGGLE, button())
    }

    @Test
    fun `sign up screen exposes its fields and actions`() {
        setContent {
            SignUpUI(
                configuration = emailConfiguration(),
                isLoading = false,
                displayName = "",
                email = "",
                password = "password123",
                confirmPassword = "password123",
                onDisplayNameChange = { },
                onEmailChange = { },
                onPasswordChange = { },
                onConfirmPasswordChange = { },
                onGoToSignIn = { },
                onSignUpClick = { },
                onNavigateBack = { },
            )
        }

        assertExposedAsResourceId(FirebaseAuthTestTags.SignUp.NAME_FIELD, field())
        assertExposedAsResourceId(FirebaseAuthTestTags.SignUp.EMAIL_FIELD, field())
        assertExposedAsResourceId(FirebaseAuthTestTags.SignUp.PASSWORD_FIELD, field())
        assertExposedAsResourceId(FirebaseAuthTestTags.SignUp.CONFIRM_PASSWORD_FIELD, field())
        assertExposedAsResourceId(FirebaseAuthTestTags.SignUp.SIGN_UP_BUTTON, button())
        assertExposedAsResourceId(FirebaseAuthTestTags.SignUp.SIGN_IN_BUTTON, button())
        assertExposedAsResourceId(FirebaseAuthTestTags.SignUp.BACK_BUTTON, button())
        assertExposedAsResourceId(FirebaseAuthTestTags.SignUp.PASSWORD_VISIBILITY_TOGGLE, button())
        assertExposedAsResourceId(
            FirebaseAuthTestTags.SignUp.CONFIRM_PASSWORD_VISIBILITY_TOGGLE,
            button()
        )
    }

    @Test
    fun `reset password screen exposes its field and actions`() {
        setContent {
            ResetPasswordUI(
                configuration = emailConfiguration(),
                isLoading = false,
                email = "",
                resetLinkSent = false,
                onEmailChange = { },
                onSendResetLink = { },
                onGoToSignIn = { },
                onNavigateBack = { },
            )
        }

        assertExposedAsResourceId(FirebaseAuthTestTags.ResetPassword.EMAIL_FIELD, field())
        assertExposedAsResourceId(FirebaseAuthTestTags.ResetPassword.SEND_BUTTON, button())
        assertExposedAsResourceId(FirebaseAuthTestTags.ResetPassword.SIGN_IN_BUTTON, button())
        assertExposedAsResourceId(FirebaseAuthTestTags.ResetPassword.BACK_BUTTON, button())
    }

    @Test
    fun `email link screen exposes its field and actions`() {
        setContent {
            SignInEmailLinkUI(
                configuration = emailConfiguration(),
                isLoading = false,
                emailSignInLinkSent = false,
                email = "",
                onEmailChange = { },
                onSignInWithEmailLink = { },
                onGoToSignIn = { },
                onGoToResetPassword = { },
                onNavigateBack = { },
            )
        }

        assertExposedAsResourceId(FirebaseAuthTestTags.EmailLink.EMAIL_FIELD, field())
        assertExposedAsResourceId(FirebaseAuthTestTags.EmailLink.SEND_LINK_BUTTON, button())
        assertExposedAsResourceId(FirebaseAuthTestTags.EmailLink.PASSWORD_SIGN_IN_BUTTON, button())
        assertExposedAsResourceId(FirebaseAuthTestTags.EmailLink.FORGOT_PASSWORD_BUTTON, button())
        assertExposedAsResourceId(FirebaseAuthTestTags.EmailLink.BACK_BUTTON, button())
    }

    @Test
    fun `phone number screen exposes its field and actions`() {
        setContent {
            EnterPhoneNumberUI(
                configuration = phoneConfiguration(),
                isLoading = false,
                phoneNumber = "",
                selectedCountry = CountryUtils.getDefaultCountry(),
                onPhoneNumberChange = { },
                onCountrySelected = { },
                onSendCodeClick = { },
                onNavigateBack = { },
            )
        }

        assertExposedAsResourceId(FirebaseAuthTestTags.PhoneNumber.PHONE_NUMBER_FIELD, field())
        assertExposedAsResourceId(FirebaseAuthTestTags.PhoneNumber.COUNTRY_SELECTOR_BUTTON, button())
        assertExposedAsResourceId(FirebaseAuthTestTags.PhoneNumber.SEND_CODE_BUTTON, button())
        assertExposedAsResourceId(FirebaseAuthTestTags.PhoneNumber.BACK_BUTTON, button())
    }

    @Test
    fun `verification code screen exposes its field and actions`() {
        setContent {
            EnterVerificationCodeUI(
                configuration = phoneConfiguration(),
                isLoading = false,
                verificationCode = "",
                fullPhoneNumber = "+1 555 555 5555",
                resendTimer = 0,
                onVerificationCodeChange = { },
                onVerifyCodeClick = { },
                onResendCodeClick = { },
                onChangeNumberClick = { },
                onNavigateBack = { },
            )
        }

        // The code is drawn as one box per digit, but the tag names the group and the group is the
        // node that takes text — so `field()` here, not `hasAnyDescendant(field())`. See the typing
        // tests below for why that distinction is the whole point.
        assertExposedAsResourceId(FirebaseAuthTestTags.VerificationCode.CODE_FIELD, field())
        assertExposedAsResourceId(FirebaseAuthTestTags.VerificationCode.VERIFY_BUTTON, button())
        assertExposedAsResourceId(FirebaseAuthTestTags.VerificationCode.RESEND_CODE_BUTTON, button())
        assertExposedAsResourceId(
            FirebaseAuthTestTags.VerificationCode.CHANGE_PHONE_NUMBER_BUTTON,
            button()
        )
        assertExposedAsResourceId(FirebaseAuthTestTags.VerificationCode.BACK_BUTTON, button())
    }

    // =============================================================================================
    // The code fields, which have to accept a code and not merely carry a tag
    // =============================================================================================

    /**
     * The finding this section exists for: `fui_verification_code_code_field` used to name a bare
     * container whose entire semantics config was its test tag. A Robo directive
     * `{"resourceName": "fui_verification_code_code_field", "inputText": "123456"}` resolved that
     * node and typed nothing, and the six real digit boxes were unaddressable — no resource id of
     * their own, and six identical content descriptions between them. The constant promised a code
     * input and delivered a `Column`.
     *
     * So this drives the code in the way that failed: locate the node by resource id, issue
     * `ACTION_SET_TEXT`, and then require the code to have arrived where the screen keeps it. The
     * verify button being enabled afterwards is the part that matters — it is the screen agreeing
     * that it holds a complete, valid code, which is as far as a crawler needs to get.
     */
    @Test
    fun `a robo directive can type a whole code into the verification code field`() {
        val entered = mutableStateOf("")

        setContent {
            EnterVerificationCodeUI(
                configuration = phoneConfiguration(),
                isLoading = false,
                verificationCode = entered.value,
                fullPhoneNumber = "+1 555 555 5555",
                resendTimer = 0,
                onVerificationCodeChange = { entered.value = it },
                onVerifyCodeClick = { },
                onResendCodeClick = { },
                onChangeNumberClick = { },
            )
        }

        // Scrolling is a harness necessity, not part of the addressing: a node outside the window
        // has no accessibility node at all, so there would be no resource id to find.
        scrollIntoWindow(FirebaseAuthTestTags.VerificationCode.CODE_FIELD)

        setTextByResourceId(FirebaseAuthTestTags.VerificationCode.CODE_FIELD, VERIFICATION_CODE)

        assertWithMessage(
            "ACTION_SET_TEXT on fui_verification_code_code_field reported success but the screen " +
                "did not receive the code, so the tagged node accepted text without passing it to " +
                "the digit boxes."
        ).that(entered.value).isEqualTo(VERIFICATION_CODE)

        composeTestRule
            .onNodeWithTag(FirebaseAuthTestTags.VerificationCode.VERIFY_BUTTON)
            .assertIsEnabled()
    }

    /**
     * The same claim for a host application's own Compose tests, which the registry KDoc invites:
     * `performTextInput` against the published tag enters the whole code. It exercises a different
     * semantics action from the test above — `InsertTextAtCursor` rather than `SetText` — so both
     * are covered.
     */
    @Test
    fun `performTextInput on the verification code tag enters the whole code`() {
        val entered = mutableStateOf("")

        setContent {
            EnterVerificationCodeUI(
                configuration = phoneConfiguration(),
                isLoading = false,
                verificationCode = entered.value,
                fullPhoneNumber = "+1 555 555 5555",
                resendTimer = 0,
                onVerificationCodeChange = { entered.value = it },
                onVerifyCodeClick = { },
                onResendCodeClick = { },
                onChangeNumberClick = { },
            )
        }

        composeTestRule
            .onNodeWithTag(FirebaseAuthTestTags.VerificationCode.CODE_FIELD)
            .performTextInput(VERIFICATION_CODE)

        // The code reaches the caller from a LaunchedEffect keyed on the digits, so the recomposition
        // the action triggers has to settle before the callback has run.
        composeTestRule.waitForIdle()

        assertWithMessage(
            "performTextInput on the published verification code tag did not enter the code."
        ).that(entered.value).isEqualTo(VERIFICATION_CODE)
    }

    /**
     * The multi-factor challenge screen shares the code input with the phone screen and had no tags
     * at all, so it had the same hole in a place a user reaches during an ordinary sign-in rather
     * than during enrollment. It is covered by the same assertions rather than by a weaker one,
     * because "reachable by a crawler" means the same thing on both screens.
     */
    @Test
    fun `a robo directive can type a whole code into the mfa challenge field`() {
        val entered = mutableStateOf("")

        setContent {
            DefaultMfaChallengeContent(
                state = MfaChallengeContentState(
                    factorType = MfaFactor.Sms,
                    maskedPhoneNumber = "+1••••••890",
                    verificationCode = entered.value,
                    onVerificationCodeChange = { entered.value = it },
                )
            )
        }

        scrollIntoWindow(FirebaseAuthTestTags.MfaChallenge.CODE_FIELD)

        assertExposedAsResourceId(FirebaseAuthTestTags.MfaChallenge.CODE_FIELD, field())
        setTextByResourceId(FirebaseAuthTestTags.MfaChallenge.CODE_FIELD, VERIFICATION_CODE)

        assertWithMessage(
            "ACTION_SET_TEXT on fui_mfa_challenge_code_field reported success but the challenge " +
                "screen did not receive the code."
        ).that(entered.value).isEqualTo(VERIFICATION_CODE)

        assertExposedAsResourceId(FirebaseAuthTestTags.MfaChallenge.VERIFY_BUTTON, button())
        composeTestRule
            .onNodeWithTag(FirebaseAuthTestTags.MfaChallenge.VERIFY_BUTTON)
            .assertIsEnabled()

        assertExposedAsResourceId(FirebaseAuthTestTags.MfaChallenge.RESEND_CODE_BUTTON, button())
        assertExposedAsResourceId(FirebaseAuthTestTags.MfaChallenge.CANCEL_BUTTON, button())
    }

    /**
     * [FirebaseAuthTestTags.MfaChallenge.CANCEL_BUTTON] is shared by two controls that render in
     * mutually exclusive branches of [DefaultMfaChallengeContent] — the SMS "use a different
     * method" button and the TOTP "dismiss" button. This pins the TOTP side of that sharing: the
     * same tag resolves to exactly one node here too, distinct from the SMS fixture above.
     */
    @Test
    fun `mfa challenge cancel button is exposed for the totp factor as well`() {
        setContent {
            DefaultMfaChallengeContent(
                state = MfaChallengeContentState(
                    factorType = MfaFactor.Totp,
                )
            )
        }

        assertExposedAsResourceId(FirebaseAuthTestTags.MfaChallenge.CANCEL_BUTTON, button())
        composeTestRule
            .onNodeWithTag(FirebaseAuthTestTags.MfaChallenge.RESEND_CODE_BUTTON)
            .assertDoesNotExist()
    }

    @Test
    fun `method picker exposes its provider list`() {
        setContent {
            AuthMethodPicker(
                providers = listOf(
                    AuthProvider.Google(scopes = emptyList(), serverClientId = null)
                ),
                onProviderSelected = { },
            )
        }

        assertExposedAsResourceId(
            FirebaseAuthTestTags.MethodPicker.PROVIDER_LIST,
            hasScrollAction()
        )
    }

    /**
     * The "Continue as …" button only exists when a previous sign-in preference is stored, so it
     * needs a fixture of its own and had no coverage without one. It is worth having: for a
     * returning user it is the first control on the flow's first screen, so it is what a crawl
     * reaches before anything else.
     */
    @Test
    fun `method picker exposes its continue as button`() {
        val provider = AuthProvider.Email(
            emailLinkActionCodeSettings = null,
            passwordValidationRules = emptyList()
        )

        setContent {
            AuthMethodPicker(
                providers = listOf(provider),
                lastSignInPreference = SignInPreferenceManager.SignInPreference(
                    providerId = provider.providerId,
                    identifier = "user@example.com",
                    timestamp = 0L
                ),
                onProviderSelected = { },
            )
        }

        assertExposedAsResourceId(FirebaseAuthTestTags.MethodPicker.CONTINUE_AS_BUTTON, button())
    }

    // =============================================================================================
    // Separate semantics owners: dialogs
    // =============================================================================================

    /**
     * The "reset link sent" dialog. Its dismiss button is the only way forward for a crawler that
     * has just submitted the form, and it is in a different window from the screen behind it.
     */
    @Test
    fun `reset password sent dialog exposes its dismiss button`() {
        setContent {
            ResetPasswordUI(
                configuration = emailConfiguration(),
                isLoading = false,
                email = "user@example.com",
                resetLinkSent = true,
                onEmailChange = { },
                onSendResetLink = { },
                onGoToSignIn = { },
            )
        }

        assertExposedAsResourceId(FirebaseAuthTestTags.ResetPassword.DISMISS_BUTTON, button())
    }

    @Test
    fun `email link sent dialog exposes its dismiss button`() {
        setContent {
            SignInEmailLinkUI(
                configuration = emailConfiguration(),
                isLoading = false,
                emailSignInLinkSent = true,
                email = "user@example.com",
                onEmailChange = { },
                onSignInWithEmailLink = { },
                onGoToSignIn = { },
                onGoToResetPassword = { },
            )
        }

        assertExposedAsResourceId(FirebaseAuthTestTags.EmailLink.DISMISS_BUTTON, button())
    }

    @Test
    fun `reauthentication dialog exposes its password field and actions`() {
        val user = mock<FirebaseUser>()
        whenever(user.email).thenReturn("user@example.com")

        setContent {
            ReauthenticationDialog(
                user = user,
                onDismiss = { },
                onSuccess = { },
                onError = { },
            )
        }

        assertExposedAsResourceId(FirebaseAuthTestTags.Reauth.PASSWORD_FIELD, field())
        assertExposedAsResourceId(FirebaseAuthTestTags.Reauth.VERIFY_BUTTON, button())
        assertExposedAsResourceId(FirebaseAuthTestTags.Reauth.DISMISS_BUTTON, button())
    }

    /**
     * [ErrorRecoveryDialog] publishes no tag of its own, so this pins the flag from the caller's
     * side: a host application's tag passed in through the public `modifier` becomes a resource id.
     * Without the library's own flag inside the dialog it would not, because the dialog's window is
     * a fresh semantics root and the flag is read by walking ancestors.
     */
    @Test
    fun `error recovery dialog exposes a caller supplied tag`() {
        setContent {
            ErrorRecoveryDialog(
                error = AuthException.NetworkException(message = "offline"),
                stringProvider = stringProvider,
                onRetry = { },
                onDismiss = { },
                modifier = Modifier.testTag(CALLER_TAG),
            )
        }

        assertExposedAsResourceId(CALLER_TAG, hasAnyDescendant(hasClickAction()))
    }

    /**
     * The dialog's own retry and dismiss actions, as opposed to the caller-supplied tag above:
     * these are the library's tags, always present regardless of what modifier a caller passes in.
     */
    @Test
    fun `error recovery dialog exposes its retry and dismiss buttons`() {
        setContent {
            ErrorRecoveryDialog(
                error = AuthException.NetworkException(message = "offline"),
                stringProvider = stringProvider,
                onRetry = { },
                onDismiss = { },
            )
        }

        assertExposedAsResourceId(FirebaseAuthTestTags.ErrorRecovery.RETRY_BUTTON, button())
        assertExposedAsResourceId(FirebaseAuthTestTags.ErrorRecovery.DISMISS_BUTTON, button())
    }

    // =============================================================================================
    // Separate semantics owners: bottom sheets
    // =============================================================================================

    /**
     * The country selector sheet, and the case that made this work necessary rather than merely
     * tidy: [FirebaseAuthScreenModifierTest] shows that a modifier passed to the flow's root does
     * not reach this sheet at all, so before the library set the flag here itself,
     * `By.res("fui_country_selector_country_list")` could not resolve however the caller was
     * configured.
     */
    @Test
    fun `country selector bottom sheet exposes its country list`() {
        setContent {
            CountrySelector(
                selectedCountry = CountryUtils.getDefaultCountry(),
                onCountrySelected = { },
            )
        }

        composeTestRule.onNodeWithContentDescription(COUNTRY_SELECTOR_DESCRIPTION).performClick()
        composeTestRule.waitForIdle()

        assertExposedAsResourceId(
            FirebaseAuthTestTags.CountrySelector.COUNTRY_LIST,
            hasScrollAction()
        )
    }

    // =============================================================================================
    // MFA enrollment: the factor-selection step, and the collision it is built to avoid
    // =============================================================================================

    /**
     * [DefaultMfaEnrollmentContent] renders one enroll button per not-yet-enrolled factor in a
     * `forEach`, so a user enrolled in neither factor composes both buttons at once — the exact
     * shape of collision this whole tag registry exists to prevent (see
     * [com.firebase.ui.auth.ui.FirebaseAuthTestTags.MfaEnrollment]). This proves both are
     * addressable *at the same time*, not merely that each works in isolation: a fixture that
     * rendered them one at a time would not catch a shared tag.
     */
    @Test
    fun `mfa enrollment exposes distinct buttons for each available factor at once`() {
        setContent {
            DefaultMfaEnrollmentContent(
                state = MfaEnrollmentContentState(
                    step = MfaEnrollmentStep.SelectFactor,
                    availableFactors = listOf(MfaFactor.Sms, MfaFactor.Totp),
                    enrolledFactors = emptyList(),
                    onSkipClick = { },
                ),
                authConfiguration = phoneConfiguration(),
                user = mock(),
            )
        }

        assertExposedAsResourceId(FirebaseAuthTestTags.MfaEnrollment.ENROLL_SMS_BUTTON, button())
        assertExposedAsResourceId(FirebaseAuthTestTags.MfaEnrollment.ENROLL_TOTP_BUTTON, button())
        assertExposedAsResourceId(FirebaseAuthTestTags.MfaEnrollment.SKIP_BUTTON, button())
    }

    /**
     * The mirror case on [com.firebase.ui.auth.ui.screens.MfaEnrollmentDefaults]'s
     * `EnrolledFactorItem`: a user enrolled in both SMS and TOTP composes one remove button per
     * factor in the same `forEach`, so the two need to be addressable at once as well.
     */
    @Test
    fun `mfa enrollment exposes distinct remove buttons for each enrolled factor at once`() {
        val phoneFactor = mock<PhoneMultiFactorInfo>()
        whenever(phoneFactor.phoneNumber).thenReturn("+1234567890")
        val totpFactor = mock<TotpMultiFactorInfo>()

        setContent {
            DefaultMfaEnrollmentContent(
                state = MfaEnrollmentContentState(
                    step = MfaEnrollmentStep.SelectFactor,
                    availableFactors = listOf(MfaFactor.Sms, MfaFactor.Totp),
                    enrolledFactors = listOf<MultiFactorInfo>(phoneFactor, totpFactor),
                ),
                authConfiguration = phoneConfiguration(),
                user = mock(),
            )
        }

        assertExposedAsResourceId(FirebaseAuthTestTags.MfaEnrollment.REMOVE_SMS_BUTTON, button())
        assertExposedAsResourceId(FirebaseAuthTestTags.MfaEnrollment.REMOVE_TOTP_BUTTON, button())
    }

    // =============================================================================================
    // MFA enrollment: TOTP setup and verification
    // =============================================================================================

    @Test
    fun `mfa enrollment totp setup step exposes its back and continue buttons`() {
        setContent {
            DefaultMfaEnrollmentContent(
                state = MfaEnrollmentContentState(
                    step = MfaEnrollmentStep.ConfigureTotp,
                ),
                authConfiguration = phoneConfiguration(),
                user = mock(),
            )
        }

        assertExposedAsResourceId(
            FirebaseAuthTestTags.MfaEnrollment.CONFIGURE_TOTP_BACK_BUTTON,
            button()
        )
        assertExposedAsResourceId(
            FirebaseAuthTestTags.MfaEnrollment.CONFIGURE_TOTP_CONTINUE_BUTTON,
            button()
        )
    }

    /**
     * The TOTP verification step's code field is the highest-value new node in the enrollment
     * flow — it is the one place a Robo directive has to type a real value rather than merely tap
     * a button — so it gets the same `ACTION_SET_TEXT` treatment as the phone verification code
     * field above, rather than only an existence check.
     */
    @Test
    fun `a robo directive can type a code into the mfa enrollment totp verification field`() {
        val entered = mutableStateOf("")

        setContent {
            DefaultMfaEnrollmentContent(
                state = MfaEnrollmentContentState(
                    step = MfaEnrollmentStep.VerifyFactor,
                    selectedFactor = MfaFactor.Totp,
                    verificationCode = entered.value,
                    onVerificationCodeChange = { entered.value = it },
                ),
                authConfiguration = phoneConfiguration(),
                user = mock(),
            )
        }

        assertExposedAsResourceId(FirebaseAuthTestTags.MfaEnrollment.VERIFY_TOTP_CODE_FIELD, field())
        setTextByResourceId(FirebaseAuthTestTags.MfaEnrollment.VERIFY_TOTP_CODE_FIELD, VERIFICATION_CODE)

        assertWithMessage(
            "ACTION_SET_TEXT on fui_mfa_enrollment_verify_totp_code_field reported success but " +
                "the enrollment screen did not receive the code."
        ).that(entered.value).isEqualTo(VERIFICATION_CODE)

        assertExposedAsResourceId(FirebaseAuthTestTags.MfaEnrollment.VERIFY_TOTP_BACK_BUTTON, button())
        assertExposedAsResourceId(FirebaseAuthTestTags.MfaEnrollment.VERIFY_TOTP_BUTTON, button())
    }

    @Test
    fun `mfa enrollment recovery codes step exposes its saved confirmation button`() {
        setContent {
            DefaultMfaEnrollmentContent(
                state = MfaEnrollmentContentState(
                    step = MfaEnrollmentStep.ShowRecoveryCodes,
                    recoveryCodes = listOf("1111-1111", "2222-2222"),
                ),
                authConfiguration = phoneConfiguration(),
                user = mock(),
            )
        }

        assertExposedAsResourceId(
            FirebaseAuthTestTags.MfaEnrollment.RECOVERY_CODES_SAVED_BUTTON,
            button()
        )
    }

    // =============================================================================================
    // The flow root
    // =============================================================================================

    /**
     * The root [androidx.compose.material3.Surface] inside [FirebaseAuthScreen] carries the flag as
     * well, so content the flow hosts directly — rather than through one of the screen composables
     * that flags itself — is covered too. The custom method-picker slot is used because it renders
     * under that `Surface` without going through [AuthMethodPicker], which would supply a flag of
     * its own and make this pass either way.
     */
    @Test
    fun `flow root exposes tags on content hosted directly beneath it`() {
        // Two providers, so the method-picker route — the one the custom slot replaces — is where
        // the flow starts.
        val configuration = authUIConfiguration {
            context = applicationContext
            providers {
                provider(
                    AuthProvider.Email(
                        emailLinkActionCodeSettings = null,
                        passwordValidationRules = emptyList()
                    )
                )
                provider(
                    AuthProvider.Phone(
                        defaultNumber = null,
                        defaultCountryCode = null,
                        allowedCountries = null
                    )
                )
            }
        }

        composeTestRule.setContent {
            FirebaseAuthScreen(
                configuration = configuration,
                authUI = authUI,
                onSignInSuccess = { },
                onSignInFailure = { },
                onSignInCancelled = { },
                customMethodPickerLayout = { _, _ ->
                    Text(text = "Custom Picker", modifier = Modifier.testTag(CALLER_TAG))
                }
            )
        }

        assertExposedAsResourceId(CALLER_TAG, hasText("Custom Picker"))
    }

    // =============================================================================================
    // FirebaseAuthScreen needs a live FirebaseApp
    // =============================================================================================

    private val authUI: FirebaseAuthUI
        get() {
            FirebaseAuthUI.clearInstanceCache()
            FirebaseApp.getApps(applicationContext).forEach { it.delete() }
            FirebaseApp.initializeApp(
                applicationContext,
                FirebaseOptions.Builder()
                    .setApiKey("fake-api-key")
                    .setApplicationId("fake-app-id")
                    .setProjectId("fake-project-id")
                    .build()
            )
            return FirebaseAuthUI.getInstance()
        }

    @After
    fun tearDown() {
        FirebaseAuthUI.clearInstanceCache()
        FirebaseApp.getApps(applicationContext).forEach {
            try {
                it.delete()
            } catch (_: Exception) {
            }
        }
    }

    private companion object {
        /** A host application's own tag, which the library must not require to be registered. */
        const val CALLER_TAG = "host_app_supplied_tag"

        /** Opens the country selector sheet. */
        const val COUNTRY_SELECTOR_DESCRIPTION = "Country selector"

        /** Six digits, the length both code screens expect. */
        const val VERIFICATION_CODE = "123456"

        /**
         * The class a crawler requires before it will type into a node. Robo's `inputText` directives
         * are documented for `EditText` only, and the class is what a crawler reads to decide a node
         * accepts text — not the action list, which a plain container can also carry.
         */
        const val EDIT_TEXT_CLASS_NAME = "android.widget.EditText"

        /**
         * Matches every semantics node, so [accessibilityNodePublishing] can search the whole tree
         * for a resource id rather than being handed the node by its test tag.
         */
        val ANY_NODE = SemanticsMatcher("any semantics node") { true }
    }
}
