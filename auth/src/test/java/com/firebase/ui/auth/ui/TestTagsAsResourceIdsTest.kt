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
import com.firebase.ui.auth.ui.screens.mfa.DefaultMfaChallengeContent
import com.firebase.ui.auth.ui.screens.mfa.DefaultMfaEnrollmentContent
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
 * End-to-end check that each published tag reaches a real node as an Android **resource id**, not
 * only a Compose test tag — `assertExists()` on a `testTag` matcher can't tell the two apart.
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

    // ---- The assertion this class is built around ----

    /**
     * Asserts exactly one node carries [tag], is the kind of node [expected] describes, and is
     * exposed to the platform as a resource id — not just that a tag exists somewhere.
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
     * Scrolls the node tagged [tag] into the window when scrollable, since a node outside the
     * root's bounds has no accessibility node to read a resource id from.
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
     * Enters [text] via `ACTION_SET_TEXT` into the node published under resource id [resourceName],
     * the way a crawler would — deliberately not located by its Compose test tag.
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
     * The accessibility node whose `viewIdResourceName` is [resourceName], found by scanning the
     * whole tree the way `By.res()` does — not via the matching Compose test tag.
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

    // ---- Fixtures ----

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

    // ---- In-window screen roots ----

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
     * Pins [AuthTextField]'s `visibilityToggleModifier` reaching a real resource id on the sign-in
     * screen's toggle, since the shared component needs a distinct tag per call site.
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

        // The tag names the digit group, which is the node that takes text — hence `field()`, not
        // `hasAnyDescendant(field())`.
        assertExposedAsResourceId(FirebaseAuthTestTags.VerificationCode.CODE_FIELD, field())
        assertExposedAsResourceId(FirebaseAuthTestTags.VerificationCode.VERIFY_BUTTON, button())
        assertExposedAsResourceId(FirebaseAuthTestTags.VerificationCode.RESEND_CODE_BUTTON, button())
        assertExposedAsResourceId(
            FirebaseAuthTestTags.VerificationCode.CHANGE_PHONE_NUMBER_BUTTON,
            button()
        )
        assertExposedAsResourceId(FirebaseAuthTestTags.VerificationCode.BACK_BUTTON, button())
    }

    // ---- The code fields, which have to accept a code and not merely carry a tag ----

    /**
     * Regression pin: `fui_verification_code_code_field` used to name a bare container whose
     * `ACTION_SET_TEXT` typed nothing into the real digit boxes, which had no resource id of their own.
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
     * The MFA challenge screen shares the code input with the phone screen and had no tags at all,
     * so it gets the same assertions rather than a weaker one.
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
     * [FirebaseAuthTestTags.MfaChallenge.CANCEL_BUTTON] is shared by two mutually exclusive
     * controls in [DefaultMfaChallengeContent]; this pins the TOTP side, distinct from SMS above.
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
     * The "Continue as …" button only exists when a sign-in preference is stored, so it needs its
     * own fixture — and it's the first control a returning user's crawl reaches.
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

    // ---- Separate semantics owners: dialogs ----

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
     * side: a host app's tag passed via `modifier` becomes a resource id via the dialog's own flag.
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

    // ---- Separate semantics owners: bottom sheets ----

    /**
     * The country selector sheet: [FirebaseAuthScreenModifierTest] shows a modifier passed to the
     * flow's root never reaches it, so the sheet must set the flag itself.
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

    // ---- MFA enrollment: the factor-selection step, and the collision it is built to avoid ----

    /**
     * [DefaultMfaEnrollmentContent] renders both enroll buttons at once via `forEach`, so this
     * proves both are addressable simultaneously — a one-at-a-time fixture would miss a shared tag.
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
     * Mirror case on `EnrolledFactorItem`: a user enrolled in both factors composes one remove
     * button per factor in the same `forEach`, so both need to be addressable at once too.
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

    // ---- MFA enrollment: TOTP setup and verification ----

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
     * The TOTP verification code field is the one enrollment node a Robo directive must type a
     * real value into, so it gets the same `ACTION_SET_TEXT` treatment as the phone code field.
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

    // ---- The flow root ----

    /**
     * The flow root's `Surface` in [FirebaseAuthScreen] carries the flag too, covering content
     * hosted directly beneath it — the custom method-picker slot bypasses [AuthMethodPicker]'s own flag.
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

    // ---- FirebaseAuthScreen needs a live FirebaseApp ----

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
         * The class a crawler requires before typing into a node; Robo's `inputText` directives are
         * documented for `EditText` only.
         */
        const val EDIT_TEXT_CLASS_NAME = "android.widget.EditText"

        /**
         * Matches every semantics node, so [accessibilityNodePublishing] can search the whole tree
         * for a resource id rather than being handed the node by its test tag.
         */
        val ANY_NODE = SemanticsMatcher("any semantics node") { true }
    }
}
