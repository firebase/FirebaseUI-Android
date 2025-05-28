package com.firebase.ui.auth.ui.email

import android.app.Application
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.firebase.ui.auth.IdpResponse
import com.firebase.ui.auth.data.model.FlowParameters
import com.firebase.ui.auth.data.model.User
import com.firebase.ui.auth.viewmodel.email.WelcomeBackPasswordViewModel
import com.google.common.truth.Truth.assertThat
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.EmailAuthProvider
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith


private fun hasAnyError(): SemanticsMatcher =
    SemanticsMatcher("has any error") { it.config.contains(SemanticsProperties.Error) }

private val signInButton =
    hasText("Sign in", ignoreCase = true).and(hasClickAction())

@RunWith(AndroidJUnit4::class)
class WelcomeBackPasswordPromptTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var flowParameters: FlowParameters
    private lateinit var idpResponse: IdpResponse
    private lateinit var appContext: Application

    @Before
    fun setUp() {
        appContext = ApplicationProvider.getApplicationContext()

        if (FirebaseApp.getApps(appContext).none { it.name == "test-app" }) {
            val opts = FirebaseOptions.Builder()
                .setApplicationId("1:123:android:dummy")   // minimal stub values
                .setApiKey("dummy")
                .setProjectId("dummy")
                .build()
            FirebaseApp.initializeApp(appContext, opts, "test-app")
        }

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

        val user = User.Builder(EmailAuthProvider.PROVIDER_ID, "jane@invertase.io").build()
        idpResponse = IdpResponse.Builder(user).build()
    }

    @Test
    fun blankPassword_showsError_andDoesNotInvokeSuccess() {
        var successCalled = false

        composeTestRule.setContent {
            WelcomeBackPasswordPrompt(
                flowParameters   = flowParameters,
                email            = "jane@invertase.io",
                idpResponse      = idpResponse,
                onSignInSuccess  = { successCalled = true },
                onSignInError    = {},
                onForgotPassword = {},
                viewModel        = WelcomeBackPasswordViewModel(appContext)
            )
        }

        composeTestRule.onNode(signInButton).performClick()

        composeTestRule
            .onNode(hasText("Password", ignoreCase = true).and(hasAnyError()))
            .assertExists()

        assertThat(successCalled).isFalse()
    }
}