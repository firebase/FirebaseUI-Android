package com.firebase.ui.auth.compose.ui.screens

import android.content.Context
import android.os.Looper
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
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
import com.firebase.ui.auth.compose.data.CountryUtils
import com.firebase.ui.auth.compose.testutil.AUTH_STATE_WAIT_TIMEOUT_MS
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
import org.robolectric.annotation.LooperMode

@Config(sdk = [34])
@RunWith(RobolectricTestRunner::class)
@LooperMode(LooperMode.Mode.PAUSED)
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
        emulatorApi.clearEmulatorData()
    }

    @After
    fun tearDown() {
        // Clean up after each test to prevent test pollution
        FirebaseAuthUI.clearInstanceCache()

        // Clear emulator data
        emulatorApi.clearEmulatorData()
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
    fun `sign-in and verify SMS emits Success auth state`() {
        val country = CountryUtils.findByCountryCode("DE")!!
        val phone = "15123456789"

        val configuration = authUIConfiguration {
            context = applicationContext
            providers {
                provider(
                    AuthProvider.Phone(
                        defaultNumber = null,
                        defaultCountryCode = null,
                        allowedCountries = null,
                    )
                )
            }
        }

        // Track auth state changes
        var currentAuthState: AuthState = AuthState.Idle

        composeTestRule.setContent {
            FirebaseAuthScreen(configuration = configuration)
            val authState by authUI.authStateFlow().collectAsState(AuthState.Idle)
            currentAuthState = authState
        }

        // Show country selector modal
        composeTestRule.onNodeWithText(stringProvider.signInWithPhone)
            .assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Country selector")
            .assertIsDisplayed()
            .performClick()
        composeTestRule.waitForIdle()
        // Select country from list
        composeTestRule.onNodeWithTag("CountrySelector LazyColumn")
            .assertIsDisplayed()
            .performScrollToNode(hasText(country.name))
        composeTestRule.onNodeWithText(country.name)
            .assertIsDisplayed()
            .performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithContentDescription("Country selector")
            .assertTextContains(country.dialCode)
            .assertIsDisplayed()
        // Enter phone number
        composeTestRule.onNodeWithText(stringProvider.phoneNumberHint)
            .assertIsDisplayed()
            .performTextInput(phone)
        // Submit
        composeTestRule.onNodeWithText(stringProvider.sendVerificationCode.uppercase())
            .assertIsDisplayed()
            .assertIsEnabled()
            .performClick()
        composeTestRule.waitForIdle()

        val phoneCode = emulatorApi.fetchVerifyPhoneCode(phone)

        // Check current page is Verify Phone Number & Enter verification code
        composeTestRule.onNodeWithText(stringProvider.verifyPhoneNumber)
        val textFields = composeTestRule.onAllNodes(hasSetTextAction())
        // Enter each digit into its corresponding field
        phoneCode.forEachIndexed { index, digit ->
            composeTestRule.waitForIdle()
            textFields[index].performTextInput(digit.toString())
        }
        composeTestRule.waitForIdle()
        // Submit verification code
        composeTestRule.onNodeWithText(stringProvider.verifyPhoneNumber.uppercase())
            .performScrollTo()
            .assertIsEnabled()
            .performClick()
        composeTestRule.waitForIdle()

        shadowOf(Looper.getMainLooper()).idle()

        // Wait for authentication to complete
        println("TEST: Waiting for auth state change after verification...")
        composeTestRule.waitUntil(timeoutMillis = AUTH_STATE_WAIT_TIMEOUT_MS) {
            shadowOf(Looper.getMainLooper()).idle()
            println("TEST: Auth state during verification: $currentAuthState")
            currentAuthState is AuthState.Success
        }
        shadowOf(Looper.getMainLooper()).idle()

        // Verify authentication succeeded or failed appropriately
        // Note: In emulator, this might fail with invalid code, which is expected
        println("TEST: Final auth state: $currentAuthState")
        assertThat(currentAuthState)
            .isInstanceOf(AuthState.Success::class.java)
        val user = (currentAuthState as AuthState.Success).user
        println("TEST: User phone: ${user.phoneNumber}")
        assertThat(authUI.auth.currentUser).isEqualTo(user)
        assertThat(authUI.auth.currentUser!!.phoneNumber).isEqualTo(
            CountryUtils.formatPhoneNumber(
                country.dialCode,
                phone
            )
        )
    }

    @Test
    @org.junit.Ignore("Flaky in CI due to timing/scrolling issues - works locally")
    fun `change phone number navigates back to EnterPhoneNumber step`() {
        val defaultNumber = "+12025550123"
        val country = CountryUtils.findByCountryCode("US")!!
        val configuration = authUIConfiguration {
            context = applicationContext
            providers {
                provider(
                    AuthProvider.Phone(
                        defaultNumber = defaultNumber,
                        defaultCountryCode = country.countryCode,
                        allowedCountries = null,
                        timeout = 60L,
                    )
                )
            }
        }

        composeTestRule.setContent {
            FirebaseAuthScreen(configuration = configuration)
        }

        // Send verification code to get to verification screen
        composeTestRule.onNodeWithText(stringProvider.sendVerificationCode.uppercase())
            .performScrollTo()
            .performClick()
        composeTestRule.waitForIdle()

        // Wait for the verification screen to appear and pump looper (CI timing)
        shadowOf(Looper.getMainLooper()).idle()
        composeTestRule.waitForIdle()

        // Click change phone number
        composeTestRule.onNodeWithText(stringProvider.changePhoneNumber)
            .performScrollTo()
            .performClick()
        composeTestRule.waitForIdle()

        // Pump looper after navigation
        shadowOf(Looper.getMainLooper()).idle()
        composeTestRule.waitForIdle()

        // Verify we are back to sign in with phone screen
        composeTestRule.onNodeWithText(stringProvider.signInWithPhone)
            .assertIsDisplayed()
    }

    @Test
    fun `default country code is applied when configured`() {
        val country = CountryUtils.findByCountryCode("GB")!!
        val configuration = authUIConfiguration {
            context = applicationContext
            providers {
                provider(
                    AuthProvider.Phone(
                        defaultNumber = null,
                        defaultCountryCode = country.countryCode,
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

        // The country selector should show the default country's dial code (GB = +44)
        composeTestRule.onNodeWithContentDescription("Country selector")
            .assertTextContains(country.dialCode, substring = true)
            .assertIsDisplayed()
    }

    @Test
    @org.junit.Ignore("Flaky in CI due to timing issues with countdown timer")
    fun `resend code timer starts at configured timeout`() {
        val phone = "+12025550123"
        val timeout = 120L
        val configuration = authUIConfiguration {
            context = applicationContext
            providers {
                provider(
                    AuthProvider.Phone(
                        defaultNumber = phone,
                        defaultCountryCode = "US",
                        allowedCountries = null,
                        timeout = timeout,
                    )
                )
            }
        }

        composeTestRule.setContent {
            FirebaseAuthScreen(configuration = configuration)
        }

        // Send verification code
        composeTestRule.onNodeWithText(stringProvider.sendVerificationCode.uppercase())
            .performScrollTo()
            .performClick()
        composeTestRule.waitForIdle()

        // Process pending tasks to render the verification screen
        shadowOf(Looper.getMainLooper()).idle()

        // With LooperMode.PAUSED, time doesn't advance automatically,
        // so the timer will stay frozen at "2:00" (the configured timeout)
        val expectedTimerText = stringProvider.resendCodeTimer("2:00")
        composeTestRule.onNodeWithText(expectedTimerText, substring = true)
            .assertIsDisplayed()
    }

    @Test
    fun `default phone number is pre-filled when configured`() {
        val defaultNumber = "+12025550123"
        val country = CountryUtils.findByCountryCode("US")!!
        val configuration = authUIConfiguration {
            context = applicationContext
            providers {
                provider(
                    AuthProvider.Phone(
                        defaultNumber = defaultNumber,
                        defaultCountryCode = country.countryCode,
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
                        onPhoneNumberChange = state.onPhoneNumberChange,
                        onCountrySelected = state.onCountrySelected,
                        onSendCodeClick = state.onSendCodeClick,
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
}
