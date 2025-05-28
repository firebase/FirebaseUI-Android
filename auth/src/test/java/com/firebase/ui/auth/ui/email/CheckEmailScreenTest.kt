package com.firebase.ui.auth.ui.email

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.firebase.ui.auth.data.model.FlowParameters
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Matches any node that exposes an `Error` semantics property. */
private fun hasAnyError(): SemanticsMatcher =
    SemanticsMatcher("has any error") { node ->
        node.config.contains(SemanticsProperties.Error)
    }

/**
 * UI tests for [CheckEmailScreen] – no Mockito required.
 */
@RunWith(AndroidJUnit4::class)
class CheckEmailScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var flowParameters: FlowParameters

    @Before
    fun setUp() {
        flowParameters = FlowParameters(
            appName                  = "test-app",
            providers                = emptyList(),
            defaultProvider          = null,
            themeId                  = 0,
            logoId                   = 0,
            termsOfServiceUrl        = "https://example.com/terms",
            privacyPolicyUrl         = "https://example.com/privacy",
            enableCredentials        = false,
            enableAnonymousUpgrade   = false,
            alwaysShowProviderChoice = true,
            lockOrientation          = false,
            emailLink                = null,
            passwordResetSettings    = null,
            authMethodPickerLayout   = null
        )
    }

    @Test
    fun initialEmail_isDisplayed() {
        val initial = "jane@invertase.io"

        composeTestRule.setContent {
            CheckEmailScreen(
                flowParameters       = flowParameters,
                initialEmail         = initial,
                onExistingEmailUser  = {},
                onExistingIdpUser    = {},
                onNewUser            = {},
                onDeveloperFailure   = {}
            )
        }

        composeTestRule
            .onNodeWithText(initial, substring = false, ignoreCase = true)
            .assertIsDisplayed()
    }

    @Test
    fun enteringValidEmail_andClickingSignIn_invokesCallback() {
        var callbackInvoked = false

        composeTestRule.setContent {
            CheckEmailScreen(
                flowParameters       = flowParameters,
                onExistingEmailUser  = { callbackInvoked = true },
                onExistingIdpUser    = {},
                onNewUser            = {},
                onDeveloperFailure   = {}
            )
        }

        composeTestRule.onNodeWithText("Email", substring = true)
            .performTextInput("test@example.com")

        composeTestRule.onNodeWithText("Sign in", substring = true)
            .performClick()

        composeTestRule.waitUntil(timeoutMillis = 2_000) { callbackInvoked }
        assertThat(callbackInvoked).isTrue()
    }

    @Test
    fun emptyEmail_andClickingSignUp_setsTextFieldError_andDoesNotInvokeCallback() {
        var callbackInvoked = false

        composeTestRule.setContent {
            CheckEmailScreen(
                flowParameters       = flowParameters,
                onExistingEmailUser  = {},
                onExistingIdpUser    = {},
                onNewUser            = { callbackInvoked = true },
                onDeveloperFailure   = {}
            )
        }

        composeTestRule
            .onNodeWithText("Sign up", substring = true, ignoreCase = true)
            .performClick()

        composeTestRule.onNode(hasAnyError()).assertExists()

        assertThat(callbackInvoked).isFalse()
    }
}