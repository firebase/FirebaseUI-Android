package com.firebase.ui.auth.ui.email

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.firebase.ui.auth.data.model.FlowParameters
import com.firebase.ui.auth.data.model.User
import com.google.common.truth.Truth.assertThat
import com.google.firebase.auth.EmailAuthProvider
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

private fun hasAnyError(): SemanticsMatcher =
    SemanticsMatcher("has any error") { it.config.contains(SemanticsProperties.Error) }

private val signUpButton = hasText("Sign up", ignoreCase = true).and(hasClickAction())


@RunWith(AndroidJUnit4::class)
class RegisterEmailScreenTest {

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
    fun initialEmailAndName_areDisplayed() {
        val initialUser = User.Builder(EmailAuthProvider.PROVIDER_ID, "alice@invertase.io")
            .setName("Alice")
            .build()

        composeTestRule.setContent {
            RegisterEmailScreen(
                flowParameters    = flowParameters,
                user              = initialUser,
                onRegisterSuccess = { _, _ -> },
                onRegisterError   = {}
            )
        }

        composeTestRule.onNodeWithText("alice@invertase.io", substring = false, ignoreCase = true)
            .assertIsDisplayed()
        composeTestRule.onNodeWithText("Alice", substring = false, ignoreCase = true)
            .assertIsDisplayed()
    }

    @Test
    fun enteringValidData_andClickingSignUp_invokesCallback() {
        var callbackInvoked = false

        composeTestRule.setContent {
            RegisterEmailScreen(
                flowParameters    = flowParameters,
                user              = User.Builder(EmailAuthProvider.PROVIDER_ID, "").build(),
                onRegisterSuccess = { _, _ -> callbackInvoked = true },
                onRegisterError   = {}
            )
        }

        composeTestRule.onNodeWithText("Email", substring = true)
            .performTextInput("bob@example.com")
        composeTestRule.onNodeWithText("Name", substring = true, ignoreCase = true)
            .performTextInput("Bob")
        composeTestRule.onNodeWithText("Password", substring = true)
            .performTextInput("password123") 

        composeTestRule.onNode(signUpButton).performClick()

        composeTestRule.waitUntil(timeoutMillis = 2_000) { callbackInvoked }
        assertThat(callbackInvoked).isTrue()
    }

    @Test
    fun emptyForm_andClickingSignUp_setsError_andDoesNotInvokeCallback() {
        var callbackInvoked = false

        composeTestRule.setContent {
            RegisterEmailScreen(
                flowParameters    = flowParameters,
                user              = User.Builder(EmailAuthProvider.PROVIDER_ID, "").build(),
                onRegisterSuccess = { _, _ -> callbackInvoked = true },
                onRegisterError   = {}
            )
        }

        composeTestRule.onNode(signUpButton).performClick()

        composeTestRule
            .onNode(hasText("Email", ignoreCase = true).and(hasAnyError()))
            .assertExists()

        assertThat(callbackInvoked).isFalse()
    }
}