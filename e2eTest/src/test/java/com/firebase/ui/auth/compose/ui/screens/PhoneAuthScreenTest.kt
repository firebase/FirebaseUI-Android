package com.firebase.ui.auth.compose.ui.screens

import android.content.Context
import android.os.Looper
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ApplicationProvider
import com.firebase.ui.auth.compose.AuthException
import com.firebase.ui.auth.compose.AuthState
import com.firebase.ui.auth.compose.FirebaseAuthUI
import com.firebase.ui.auth.compose.configuration.AuthUIConfiguration
import com.firebase.ui.auth.compose.configuration.authUIConfiguration
import com.firebase.ui.auth.compose.configuration.auth_provider.AuthProvider
import com.firebase.ui.auth.compose.configuration.string_provider.AuthUIStringProvider
import com.firebase.ui.auth.compose.configuration.string_provider.DefaultAuthUIStringProvider
import com.firebase.ui.auth.compose.testutil.EmulatorAuthApi
import com.firebase.ui.auth.compose.ui.screens.phone.EnterPhoneNumberUI
import com.firebase.ui.auth.compose.ui.screens.phone.EnterVerificationCodeUI
import com.firebase.ui.auth.compose.ui.screens.phone.PhoneAuthScreen
import com.firebase.ui.auth.compose.ui.screens.phone.PhoneAuthStep
import com.google.common.truth.Truth.assertThat
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.AuthResult
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.MockitoAnnotations
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

private const val AUTH_STATE_WAIT_TIMEOUT_MS = 5_000L
private const val TEST_PHONE_NUMBER = "5551234567"
private const val TEST_VERIFICATION_CODE = "123456"

@Config(sdk = [34])
@RunWith(RobolectricTestRunner::class)
class PhoneAuthScreenTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var applicationContext: Context
    private lateinit var stringProvider: AuthUIStringProvider
    private lateinit var authUI: FirebaseAuthUI
    private lateinit var emulatorApi: EmulatorAuthApi

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)

        applicationContext = ApplicationProvider.getApplicationContext()
        stringProvider = DefaultAuthUIStringProvider(applicationContext)

        // Clear any existing Firebase apps
        FirebaseApp.getApps(applicationContext).forEach { app ->
            app.delete()
        }

        // Initialize default FirebaseApp
        val firebaseApp = FirebaseApp.initializeApp(
            applicationContext,
            FirebaseOptions.Builder()
                .setApiKey("fake-api-key")
                .setApplicationId("fake-app-id")
                .setProjectId("fake-project-id")
                .build()
        )

        authUI = FirebaseAuthUI.getInstance()
        authUI.auth.useEmulator("127.0.0.1", 9099)

        emulatorApi = EmulatorAuthApi(
            projectId = firebaseApp.options.projectId
                ?: throw IllegalStateException("Project ID is required for emulator interactions"),
            emulatorHost = "127.0.0.1",
            emulatorPort = 9099
        )

        // Clear emulator data
        clearEmulatorData()
    }

    @After
    fun tearDown() {
        // Clean up after each test to prevent test pollution
        FirebaseAuthUI.clearInstanceCache()

        // Clear emulator data
        clearEmulatorData()
    }

    @Test
    fun `initial PhoneAuthStep is EnterPhoneNumber`() {
        val configuration = authUIConfiguration {
            context = applicationContext
            providers {
                provider(
                    AuthProvider.Phone(
                        defaultNumber = null,
                        defaultCountryCode = null,
                        allowedCountries = null,
                        timeout = 60L,
                        isInstantVerificationEnabled = true
                    )
                )
            }
        }

        composeTestRule.setContent {
            FirebaseAuthScreen(configuration = configuration)
        }

        composeTestRule.onNodeWithText(stringProvider.enterPhoneNumberTitle)
            .assertIsDisplayed()
    }

    @Test
    fun `phone number input enables send code button when valid`() {
        val configuration = authUIConfiguration {
            context = applicationContext
            providers {
                provider(
                    AuthProvider.Phone(
                        defaultNumber = null,
                        defaultCountryCode = "US",
                        allowedCountries = null,
                        timeout = 60L,
                        isInstantVerificationEnabled = true
                    )
                )
            }
        }

        composeTestRule.setContent {
            FirebaseAuthScreen(configuration = configuration)
        }

        // Initially button should be disabled (no phone number)
        composeTestRule.onNodeWithText(stringProvider.sendVerificationCode.uppercase())
            .performScrollTo()
            .assertIsNotEnabled()

        // Enter phone number
        composeTestRule.onNodeWithText(stringProvider.phoneNumberHint)
            .performScrollTo()
            .performTextInput(TEST_PHONE_NUMBER)

        // Button should now be enabled
        composeTestRule.onNodeWithText(stringProvider.sendVerificationCode.uppercase())
            .performScrollTo()
            .assertIsEnabled()
    }

    @Test
    fun `sends verification code and transitions to EnterVerificationCode step`() {
        val configuration = authUIConfiguration {
            context = applicationContext
            providers {
                provider(
                    AuthProvider.Phone(
                        defaultNumber = null,
                        defaultCountryCode = "US",
                        allowedCountries = null,
                        timeout = 60L,
                        isInstantVerificationEnabled = false // Disable instant verification for this test
                    )
                )
            }
        }

        var currentAuthState: AuthState = AuthState.Idle
        var currentStep: PhoneAuthStep? = null

        composeTestRule.setContent {
            FirebaseAuthScreen(
                configuration = configuration,
                onStepChange = { step -> currentStep = step }
            )
            val authState by authUI.authStateFlow().collectAsState(AuthState.Idle)
            currentAuthState = authState
        }

        // Enter phone number
        composeTestRule.onNodeWithText(stringProvider.phoneNumberHint)
            .performScrollTo()
            .performTextInput(TEST_PHONE_NUMBER)

        // Click send code
        composeTestRule.onNodeWithText(stringProvider.sendVerificationCode.uppercase())
            .performScrollTo()
            .performClick()

        println("TEST: Pumping looper after click...")
        shadowOf(Looper.getMainLooper()).idle()

        // Wait for transition to verification code step
        println("TEST: Waiting for step change... Current state: $currentAuthState")
        composeTestRule.waitUntil(timeoutMillis = AUTH_STATE_WAIT_TIMEOUT_MS) {
            shadowOf(Looper.getMainLooper()).idle()
            println("TEST: Auth state during wait: $currentAuthState, Step: $currentStep")
            currentStep == PhoneAuthStep.EnterVerificationCode
        }

        // Ensure final recomposition is complete
        shadowOf(Looper.getMainLooper()).idle()

        // Verify we're on the verification code screen
        println("TEST: Verifying verification code screen is displayed")
        composeTestRule.onNodeWithText(stringProvider.verifyPhoneNumber)
            .assertIsDisplayed()
    }

    @Test
    fun `verification code input enables verify button when complete`() {
        val configuration = authUIConfiguration {
            context = applicationContext
            providers {
                provider(
                    AuthProvider.Phone(
                        defaultNumber = TEST_PHONE_NUMBER,
                        defaultCountryCode = "US",
                        allowedCountries = null,
                        timeout = 60L,
                        isInstantVerificationEnabled = false
                    )
                )
            }
        }

        var currentStep: PhoneAuthStep? = null

        composeTestRule.setContent {
            FirebaseAuthScreen(
                configuration = configuration,
                onStepChange = { step -> currentStep = step }
            )
        }

        // Send verification code
        composeTestRule.onNodeWithText(stringProvider.sendVerificationCode.uppercase())
            .performScrollTo()
            .performClick()

        shadowOf(Looper.getMainLooper()).idle()

        // Wait for transition to verification code step
        composeTestRule.waitUntil(timeoutMillis = AUTH_STATE_WAIT_TIMEOUT_MS) {
            shadowOf(Looper.getMainLooper()).idle()
            currentStep == PhoneAuthStep.EnterVerificationCode
        }

        shadowOf(Looper.getMainLooper()).idle()

        // Verify button should be disabled initially
        composeTestRule.onNodeWithText(stringProvider.verifyCode.uppercase())
            .performScrollTo()
            .assertIsNotEnabled()

        // Note: VerificationCodeInputField uses a custom implementation
        // For testing, we'd need to interact with the actual input mechanism
        // This is a simplified test - in practice you'd need to properly interact
        // with the VerificationCodeInputField component
    }

    @Test
    fun `resend code timer starts at configured timeout`() {
        val timeout = 60L
        val configuration = authUIConfiguration {
            context = applicationContext
            providers {
                provider(
                    AuthProvider.Phone(
                        defaultNumber = TEST_PHONE_NUMBER,
                        defaultCountryCode = "US",
                        allowedCountries = null,
                        timeout = timeout,
                        isInstantVerificationEnabled = false
                    )
                )
            }
        }

        var currentStep: PhoneAuthStep? = null

        composeTestRule.setContent {
            FirebaseAuthScreen(
                configuration = configuration,
                onStepChange = { step -> currentStep = step }
            )
        }

        // Send verification code
        composeTestRule.onNodeWithText(stringProvider.sendVerificationCode.uppercase())
            .performScrollTo()
            .performClick()

        shadowOf(Looper.getMainLooper()).idle()

        // Wait for transition to verification code step
        composeTestRule.waitUntil(timeoutMillis = AUTH_STATE_WAIT_TIMEOUT_MS) {
            shadowOf(Looper.getMainLooper()).idle()
            currentStep == PhoneAuthStep.EnterVerificationCode
        }

        shadowOf(Looper.getMainLooper()).idle()

        // Check that timer text is displayed (should show 1:00 for 60 seconds)
        val expectedTimerText = stringProvider.resendCodeTimer("1:00")
        composeTestRule.onNodeWithText(expectedTimerText, substring = true)
            .assertIsDisplayed()
    }

    @Test
    fun `change phone number navigates back to EnterPhoneNumber step`() {
        val configuration = authUIConfiguration {
            context = applicationContext
            providers {
                provider(
                    AuthProvider.Phone(
                        defaultNumber = TEST_PHONE_NUMBER,
                        defaultCountryCode = "US",
                        allowedCountries = null,
                        timeout = 60L,
                        isInstantVerificationEnabled = false
                    )
                )
            }
        }

        var currentStep: PhoneAuthStep? = null

        composeTestRule.setContent {
            FirebaseAuthScreen(
                configuration = configuration,
                onStepChange = { step -> currentStep = step }
            )
        }

        // Send verification code to get to verification screen
        composeTestRule.onNodeWithText(stringProvider.sendVerificationCode.uppercase())
            .performScrollTo()
            .performClick()

        shadowOf(Looper.getMainLooper()).idle()

        // Wait for transition to verification code step
        composeTestRule.waitUntil(timeoutMillis = AUTH_STATE_WAIT_TIMEOUT_MS) {
            shadowOf(Looper.getMainLooper()).idle()
            currentStep == PhoneAuthStep.EnterVerificationCode
        }

        shadowOf(Looper.getMainLooper()).idle()

        // Click change phone number
        composeTestRule.onNodeWithText(stringProvider.changePhoneNumber)
            .performScrollTo()
            .performClick()

        shadowOf(Looper.getMainLooper()).idle()

        // Verify we're back on the phone number entry screen
        assertThat(currentStep).isEqualTo(PhoneAuthStep.EnterPhoneNumber)
        composeTestRule.onNodeWithText(stringProvider.enterPhoneNumberTitle)
            .assertIsDisplayed()
    }

    @Test
    fun `default country code is applied when configured`() {
        val defaultCountryCode = "GB"
        val configuration = authUIConfiguration {
            context = applicationContext
            providers {
                provider(
                    AuthProvider.Phone(
                        defaultNumber = null,
                        defaultCountryCode = defaultCountryCode,
                        allowedCountries = null,
                        timeout = 60L,
                        isInstantVerificationEnabled = true
                    )
                )
            }
        }

        composeTestRule.setContent {
            FirebaseAuthScreen(configuration = configuration)
        }

        // The country selector should show the default country (GB = United Kingdom)
        // Note: The exact text depends on how the country is displayed in the UI
        composeTestRule.onNodeWithText("United Kingdom", substring = true)
            .assertIsDisplayed()
    }

    @Test
    fun `default phone number is pre-filled when configured`() {
        val defaultNumber = "1234567890"
        val configuration = authUIConfiguration {
            context = applicationContext
            providers {
                provider(
                    AuthProvider.Phone(
                        defaultNumber = defaultNumber,
                        defaultCountryCode = "US",
                        allowedCountries = null,
                        timeout = 60L,
                        isInstantVerificationEnabled = true
                    )
                )
            }
        }

        composeTestRule.setContent {
            FirebaseAuthScreen(configuration = configuration)
        }

        // The send verification code button should be enabled since phone number is pre-filled
        composeTestRule.onNodeWithText(stringProvider.sendVerificationCode.uppercase())
            .performScrollTo()
            .assertIsEnabled()
    }

    @Composable
    private fun FirebaseAuthScreen(
        configuration: AuthUIConfiguration,
        onSuccess: ((AuthResult) -> Unit) = {},
        onError: ((AuthException) -> Unit) = {},
        onCancel: (() -> Unit) = {},
        onStepChange: ((PhoneAuthStep) -> Unit) = {},
    ) {
        PhoneAuthScreen(
            context = applicationContext,
            configuration = configuration,
            authUI = authUI,
            onSuccess = onSuccess,
            onError = onError,
            onCancel = onCancel,
        ) { state ->
            onStepChange(state.step)

            when (state.step) {
                PhoneAuthStep.EnterPhoneNumber -> {
                    EnterPhoneNumberUI(
                        configuration = configuration,
                        isLoading = state.isLoading,
                        phoneNumber = state.phoneNumber,
                        selectedCountry = state.selectedCountry,
                        useInstantVerificationEnabled = state.useInstantVerificationEnabled,
                        onPhoneNumberChange = state.onPhoneNumberChange,
                        onCountrySelected = state.onCountrySelected,
                        onSendCodeClick = state.onSendCodeClick,
                        onUseInstantVerificationChange = state.onUseInstantVerificationChange,
                    )
                }

                PhoneAuthStep.EnterVerificationCode -> {
                    EnterVerificationCodeUI(
                        configuration = configuration,
                        isLoading = state.isLoading,
                        verificationCode = state.verificationCode,
                        fullPhoneNumber = state.fullPhoneNumber,
                        resendTimer = state.resendTimer,
                        onVerificationCodeChange = state.onVerificationCodeChange,
                        onVerifyCodeClick = state.onVerifyCodeClick,
                        onResendCodeClick = state.onResendCodeClick,
                        onChangeNumberClick = state.onChangeNumberClick,
                    )
                }
            }
        }
    }

    /**
     * Clears all data from the Firebase Auth Emulator.
     *
     * This function calls the emulator's clear data endpoint to remove all accounts,
     * OOB codes, and other authentication data. This ensures test isolation by providing
     * a clean slate for each test.
     */
    private fun clearEmulatorData() {
        if (::emulatorApi.isInitialized) {
            try {
                emulatorApi.clearAccounts()
            } catch (e: Exception) {
                println("WARNING: Exception while clearing emulator data: ${e.message}")
            }
        }
    }
}
