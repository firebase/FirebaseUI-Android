package com.firebase.ui.auth.ui.screens

import android.content.Context
import android.os.Looper
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ApplicationProvider
import com.firebase.ui.auth.AuthException
import com.firebase.ui.auth.AuthState
import com.firebase.ui.auth.FirebaseAuthUI
import com.firebase.ui.auth.configuration.AuthUIConfiguration
import com.firebase.ui.auth.configuration.authUIConfiguration
import com.firebase.ui.auth.configuration.auth_provider.AuthProvider
import com.firebase.ui.auth.configuration.string_provider.AuthUIStringProvider
import com.firebase.ui.auth.configuration.string_provider.DefaultAuthUIStringProvider
import com.firebase.ui.auth.configuration.string_provider.LocalAuthUIStringProvider
import com.firebase.ui.auth.testutil.AUTH_STATE_WAIT_TIMEOUT_MS
import com.firebase.ui.auth.testutil.EmulatorAuthApi
import com.firebase.ui.auth.testutil.awaitWithLooper
import com.firebase.ui.auth.testutil.ensureFreshUser
import com.firebase.ui.auth.testutil.ensureTestFirebaseApp
import com.firebase.ui.auth.testutil.verifyEmailInEmulator
import com.firebase.ui.auth.ui.FirebaseAuthTestTags
import com.firebase.ui.auth.ui.screens.phone.EnterPhoneNumberUI
import com.firebase.ui.auth.ui.screens.phone.EnterVerificationCodeUI
import com.firebase.ui.auth.ui.screens.phone.PhoneAuthScreen
import com.firebase.ui.auth.ui.screens.phone.PhoneAuthStep
import com.firebase.ui.auth.ui.screens.phone.rememberPhoneAuthFlowState
import com.firebase.ui.auth.util.CountryUtils
import com.google.common.truth.Truth.assertThat
import com.google.firebase.auth.AuthResult
import org.junit.After
import org.junit.Assume
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

        val firebaseApp = ensureTestFirebaseApp(applicationContext)
        authUI = FirebaseAuthUI.getInstance()

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
        // Clean up after each test to prevent test pollution. The FirebaseApp itself is
        // shared across test classes (see ensureTestFirebaseApp), so the client-side
        // session must be reset explicitly here rather than relying on app re-creation.
        authUI.auth.signOut()
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
            TestAuthScreen(configuration = configuration)
        }

        composeTestRule.onNodeWithText(stringProvider.enterPhoneNumberTitle)
            .assertIsDisplayed()
    }

    /**
     * Was `@Ignore`d as simply "flaky test". Three races, all the test's own: it typed the code
     * without waiting for the verification step to arrive, it hunted for the code field before it
     * existed, and it waited for the emulator to mint a code with `Thread.sleep` — which, under
     * `LooperMode.PAUSED`, sleeps the one thread that would have driven the work it is waiting for.
     * Each is now an explicit wait on the thing itself.
     */
    @Test
    fun `sign-in and verify SMS emits Success auth state`() {
        val country = CountryUtils.findByCountryCode("DE")!!
        val phone = "151${System.currentTimeMillis() % 100000000}"

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
        var step: PhoneAuthStep? = null

        composeTestRule.setContent {
            TestAuthScreen(configuration = configuration, onStepChange = { step = it })
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
        composeTestRule.onNodeWithTag(FirebaseAuthTestTags.CountrySelector.COUNTRY_LIST)
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

        // The screen's own step, not a guess at how long the emulator round-trip takes.
        composeTestRule.waitUntil(timeoutMillis = AUTH_STATE_WAIT_TIMEOUT_MS) {
            shadowOf(Looper.getMainLooper()).idle()
            step == PhoneAuthStep.EnterVerificationCode
        }

        // Poll the emulator for the code it minted, pumping the looper between attempts rather
        // than sleeping the thread that has to run the work.
        // NOTE: requires the Firebase Auth Emulator on localhost:9099
        // (`./scripts/start-firebase-emulator.sh`).
        var phoneCode: String? = null
        composeTestRule.waitUntil(timeoutMillis = AUTH_STATE_WAIT_TIMEOUT_MS) {
            shadowOf(Looper.getMainLooper()).idle()
            phoneCode = try {
                emulatorApi.fetchVerifyPhoneCode(phone)
            } catch (_: Exception) {
                null
            }
            phoneCode != null
        }
        // Absent after the full timeout means the emulator has no verification-codes endpoint,
        // which is an environment problem rather than a failure of what is under test.
        Assume.assumeTrue(
            "Skipping test: Firebase Auth Emulator verification codes endpoint not available. " +
                    "Ensure the emulator is running on localhost:9099.",
            phoneCode != null
        )

        // The whole code goes in via the published tag in one call, rather than selecting boxes
        // positionally out of onAllNodes(hasSetTextAction()).
        composeTestRule.waitUntil(timeoutMillis = AUTH_STATE_WAIT_TIMEOUT_MS) {
            shadowOf(Looper.getMainLooper()).idle()
            composeTestRule.onAllNodesWithTag(FirebaseAuthTestTags.VerificationCode.CODE_FIELD)
                .fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule
            .onNodeWithTag(FirebaseAuthTestTags.VerificationCode.CODE_FIELD)
            .performTextInput(requireNotNull(phoneCode))
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

    /**
     * Was `@Ignore`d as "flaky in CI due to timing/scrolling issues". The flakiness was the test's,
     * not the product's: sending a code is an emulator round-trip, and this waited for it by
     * pumping the looper a fixed number of times and hoping. Both transitions are now waited on by
     * the step the screen actually reports, so there is nothing left to lose a race with — and the
     * scroll no longer runs against a node that may not exist yet.
     */
    @Test
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

        var step: PhoneAuthStep? = null
        composeTestRule.setContent {
            TestAuthScreen(configuration = configuration, onStepChange = { step = it })
        }

        composeTestRule.onNodeWithText(stringProvider.sendVerificationCode.uppercase())
            .performScrollTo()
            .performClick()

        composeTestRule.waitUntil(timeoutMillis = AUTH_STATE_WAIT_TIMEOUT_MS) {
            shadowOf(Looper.getMainLooper()).idle()
            step == PhoneAuthStep.EnterVerificationCode
        }

        composeTestRule.onNodeWithText(stringProvider.changePhoneNumber)
            .performScrollTo()
            .performClick()

        composeTestRule.waitUntil(timeoutMillis = AUTH_STATE_WAIT_TIMEOUT_MS) {
            shadowOf(Looper.getMainLooper()).idle()
            step == PhoneAuthStep.EnterPhoneNumber
        }

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
            TestAuthScreen(configuration = configuration)
        }

        // The country selector should show the default country's dial code (GB = +44)
        composeTestRule.onNodeWithContentDescription("Country selector")
            .assertTextContains(country.dialCode, substring = true)
            .assertIsDisplayed()
    }

    /**
     * Was `@Ignore`d as "flaky in CI due to timing issues with countdown timer". The countdown was
     * never the problem — with `LooperMode.PAUSED` the clock does not advance, so the timer holds
     * at the configured value. What raced was getting to the screen showing it: the assertion ran
     * after a fixed amount of looper pumping rather than after the step actually changed.
     */
    @Test
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

        var step: PhoneAuthStep? = null
        composeTestRule.setContent {
            TestAuthScreen(configuration = configuration, onStepChange = { step = it })
        }

        composeTestRule.onNodeWithText(stringProvider.sendVerificationCode.uppercase())
            .performScrollTo()
            .performClick()

        composeTestRule.waitUntil(timeoutMillis = AUTH_STATE_WAIT_TIMEOUT_MS) {
            shadowOf(Looper.getMainLooper()).idle()
            step == PhoneAuthStep.EnterVerificationCode
        }

        // The clock is frozen, so the timer reads the configured timeout: 120s as "2:00".
        val expectedTimerText = stringProvider.resendCodeTimer("2:00")
        composeTestRule.waitUntil(timeoutMillis = AUTH_STATE_WAIT_TIMEOUT_MS) {
            shadowOf(Looper.getMainLooper()).idle()
            composeTestRule.onAllNodesWithText(expectedTimerText, substring = true)
                .fetchSemanticsNodes().isNotEmpty()
        }
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
            TestAuthScreen(configuration = configuration)
        }

        // The send verification code button should be enabled since phone number is pre-filled
        composeTestRule.onNodeWithText(stringProvider.sendVerificationCode.uppercase())
            .performScrollTo()
            .assertIsEnabled()
    }

    @Test
    fun `onSuccess fires when a returning signed-in user links a phone number`() {
        val email = "returning-user-2352@example.com"
        val password = "Test@123"
        val phone = "2025550199"
        val country = CountryUtils.findByCountryCode("US")!!

        // Step 1: establish a pre-existing session - the "returning user".
        println("TEST: Creating email/password user...")
        val createdUser = ensureFreshUser(authUI, email, password)
        requireNotNull(createdUser) { "Failed to create user" }

        println("TEST: Verifying email in emulator...")
        verifyEmailInEmulator(authUI, emulatorApi, createdUser)

        authUI.auth.signInWithEmailAndPassword(email, password).awaitWithLooper()
        assertThat(authUI.auth.currentUser).isNotNull()

        // Step 2: mount PhoneAuthScreen standalone, exactly like the reporter's app -
        // success is only observable via the onSuccess callback.
        val configuration = authUIConfiguration {
            context = applicationContext
            providers {
                provider(
                    AuthProvider.Phone(
                        defaultNumber = null,
                        defaultCountryCode = country.countryCode,
                        allowedCountries = null,
                        timeout = 60L,
                    )
                )
            }
            isCredentialLinkingEnabled = true
        }

        var successResult: AuthResult? = null

        composeTestRule.setContent {
            TestAuthScreen(
                configuration = configuration,
                onSuccess = { result -> successResult = result },
            )
        }

        composeTestRule.waitForIdle()
        shadowOf(Looper.getMainLooper()).idle()

        // Step 3: enter phone number and request a verification code.
        println("TEST: Entering phone number...")
        composeTestRule.onNodeWithText(stringProvider.phoneNumberHint)
            .performTextInput(phone)

        composeTestRule.onNodeWithText(stringProvider.sendVerificationCode.uppercase())
            .performScrollTo()
            .assertIsEnabled()
            .performClick()

        composeTestRule.waitForIdle()
        shadowOf(Looper.getMainLooper()).idle()

        // Step 4: fetch verification code from emulator
        println("TEST: Fetching phone verification code...")
        var phoneCode: String? = null
        var retries = 0
        val maxRetries = 5
        while (phoneCode == null && retries < maxRetries) {
            Thread.sleep(if (retries == 0) 200L else 500L * retries)
            shadowOf(Looper.getMainLooper()).idle()
            try {
                phoneCode = emulatorApi.fetchVerifyPhoneCode(phone)
                println("TEST: Found phone code after ${retries + 1} attempts")
            } catch (e: Exception) {
                retries++
                if (retries >= maxRetries) {
                    Assume.assumeTrue(
                        "Skipping test: Firebase Auth Emulator not available. Error: ${e.message}",
                        false
                    )
                }
                println("TEST: Phone code not found yet, retrying... (attempt $retries/$maxRetries)")
            }
        }
        requireNotNull(phoneCode) { "Phone code should not be null at this point" }

        // Step 5: enter verification code
        println("TEST: Entering verification code: $phoneCode")
        // The whole code goes in via the published tag in one call, rather than selecting boxes
        // positionally out of onAllNodes(hasSetTextAction()).
        composeTestRule.waitForIdle()
        composeTestRule
            .onNodeWithTag(FirebaseAuthTestTags.VerificationCode.CODE_FIELD)
            .performTextInput(phoneCode)
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText(stringProvider.verifyPhoneNumber.uppercase())
            .performScrollTo()
            .assertIsEnabled()
            .performClick()

        composeTestRule.waitForIdle()
        shadowOf(Looper.getMainLooper()).idle()

        // Step 6: onSuccess must fire - this is what issue #2352 reports as broken.
        println("TEST: Waiting for onSuccess callback...")
        composeTestRule.waitUntil(timeoutMillis = AUTH_STATE_WAIT_TIMEOUT_MS) {
            shadowOf(Looper.getMainLooper()).idle()
            successResult != null
        }

        assertThat(successResult).isNotNull()
        assertThat(successResult?.user?.phoneNumber).isEqualTo(
            CountryUtils.formatPhoneNumber(country.dialCode, phone)
        )
    }

    @Composable
    private fun TestAuthScreen(
        configuration: AuthUIConfiguration,
        onSuccess: ((AuthResult) -> Unit) = {},
        onError: ((AuthException) -> Unit) = {},
        onCancel: (() -> Unit) = {},
        onStepChange: ((PhoneAuthStep) -> Unit) = {},
    ) {
        CompositionLocalProvider(
            LocalAuthUIStringProvider provides DefaultAuthUIStringProvider(applicationContext)
        ) {
            // Hosted the way production is: a stack of steps, and a `key` on its top, so a step
            // switch composes fresh rather than inheriting what the previous step held.
            val stack = remember { mutableStateListOf(PhoneAuthStep.EnterPhoneNumber) }
            val flowState = rememberPhoneAuthFlowState(configuration)
            key(stack.last()) {
            PhoneAuthScreen(
                context = applicationContext,
                configuration = configuration,
                authUI = authUI,
                onSuccess = onSuccess,
                onError = onError,
                onCancel = onCancel,
                step = stack.last(),
                onNavigateToStep = { stack.add(it) },
                onNavigateBack = { if (stack.size > 1) stack.removeAt(stack.lastIndex) },
                flowState = flowState,
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
    }
}
