package com.firebase.ui.auth.ui.screens

import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.core.app.ApplicationProvider
import com.firebase.ui.auth.AuthState
import com.firebase.ui.auth.FirebaseAuthUI
import com.firebase.ui.auth.configuration.authUIConfiguration
import com.firebase.ui.auth.configuration.auth_provider.AuthProvider
import com.firebase.ui.auth.mfa.MfaChallengeContentState
import com.google.android.gms.tasks.Tasks
import com.google.common.truth.Truth.assertThat
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.MultiFactorInfo
import com.google.firebase.auth.MultiFactorResolver
import com.google.firebase.auth.MultiFactorSession
import com.google.firebase.auth.TotpMultiFactorInfo
import com.google.firebase.auth.UserInfo
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.any
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Regression tests for the MFA challenge success path in [FirebaseAuthScreen].
 *
 * A successful challenge used to clear `pendingResolver` while the route was still composed,
 * which emptied the back stack and left a blank screen, and it reset the auth state to
 * [AuthState.Idle], which discarded the resolved [AuthResult] so `onSignInSuccess` never fired.
 *
 * The emulator cannot perform a real MFA resolve, so the resolver is mocked - the same approach
 * the `:e2eTest` MFA tests take.
 */
@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, sdk = [34])
class FirebaseAuthScreenMfaSuccessTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Mock
    private lateinit var mockFirebaseAuth: FirebaseAuth

    @Mock
    private lateinit var mockResolver: MultiFactorResolver

    @Mock
    private lateinit var mockSession: MultiFactorSession

    @Mock
    private lateinit var mockTotpHint: TotpMultiFactorInfo

    @Mock
    private lateinit var mockAuthResult: AuthResult

    @Mock
    private lateinit var mockUser: FirebaseUser

    @Mock
    private lateinit var mockPasswordProvider: UserInfo

    private lateinit var authUI: FirebaseAuthUI

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)

        FirebaseAuthUI.clearInstanceCache()

        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        FirebaseApp.getApps(context).forEach { app ->
            app.delete()
        }

        val defaultApp = FirebaseApp.initializeApp(
            context,
            FirebaseOptions.Builder()
                .setApiKey("fake-api-key")
                .setApplicationId("fake-app-id")
                .setProjectId("fake-project-id")
                .build()
        )!!

        `when`(mockFirebaseAuth.app).thenReturn(defaultApp)

        authUI = FirebaseAuthUI.create(defaultApp, mockFirebaseAuth)

        `when`(mockResolver.session).thenReturn(mockSession)
        `when`(mockResolver.hints).thenReturn(listOf<MultiFactorInfo>(mockTotpHint))
        `when`(mockTotpHint.factorId).thenReturn("totp")
        `when`(mockTotpHint.uid).thenReturn("totp-factor-uid")
        `when`(mockResolver.resolveSignIn(any())).thenReturn(Tasks.forResult(mockAuthResult))

        `when`(mockAuthResult.user).thenReturn(mockUser)
        `when`(mockUser.uid).thenReturn("mfa-user-uid")
    }

    @After
    fun tearDown() {
        FirebaseAuthUI.clearInstanceCache()

        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        FirebaseApp.getApps(context).forEach { app ->
            app.delete()
        }
    }

    @Test
    fun `successful mfa challenge renders the authenticated destination`() {
        `when`(mockUser.isEmailVerified).thenReturn(true)

        resolveChallenge()

        composeTestRule.onNodeWithTag(AUTHENTICATED_TAG).assertIsDisplayed()
    }

    @Test
    fun `successful mfa challenge invokes onSignInSuccess with the resolved result`() {
        `when`(mockUser.isEmailVerified).thenReturn(true)

        val results = mutableListOf<AuthResult>()
        resolveChallenge(onSignInSuccess = { results.add(it) })

        assertThat(results).containsExactly(mockAuthResult)
    }

    @Test
    fun `mfa challenge for an unverified password user does not invoke onSignInSuccess`() {
        `when`(mockUser.isEmailVerified).thenReturn(false)
        `when`(mockUser.email).thenReturn("user@example.com")
        `when`(mockPasswordProvider.providerId).thenReturn("password")
        `when`(mockUser.providerData).thenReturn(listOf(mockPasswordProvider))

        val results = mutableListOf<AuthResult>()
        resolveChallenge(onSignInSuccess = { results.add(it) })

        assertThat(results).isEmpty()
    }

    /**
     * Drives [FirebaseAuthScreen] into the MFA challenge route and completes the challenge by
     * invoking the captured [MfaChallengeContentState.onVerifyClick], which resolves against the
     * mocked [MultiFactorResolver].
     */
    private fun resolveChallenge(onSignInSuccess: (AuthResult) -> Unit = {}) {
        val configuration = authUIConfiguration {
            context = ApplicationProvider.getApplicationContext()
            providers {
                provider(
                    AuthProvider.Email(
                        emailLinkActionCodeSettings = null,
                        passwordValidationRules = emptyList()
                    )
                )
            }
        }
        var challengeState: MfaChallengeContentState? = null

        composeTestRule.setContent {
            FirebaseAuthScreen(
                configuration = configuration,
                authUI = authUI,
                onSignInSuccess = onSignInSuccess,
                onSignInFailure = {},
                onSignInCancelled = {},
                mfaChallengeContent = { state -> challengeState = state },
                authenticatedContent = { _, _ ->
                    Text(text = "authenticated", modifier = Modifier.testTag(AUTHENTICATED_TAG))
                }
            )
        }

        composeTestRule.runOnIdle {
            authUI.updateAuthState(AuthState.RequiresMfa(mockResolver))
        }
        composeTestRule.waitForIdle()

        composeTestRule.runOnIdle {
            requireNotNull(challengeState) { "MFA challenge route was never composed" }
                .onVerificationCodeChange(VERIFICATION_CODE)
        }
        composeTestRule.waitForIdle()

        composeTestRule.runOnIdle {
            requireNotNull(challengeState).onVerifyClick()
        }
        composeTestRule.waitForIdle()
    }

    private companion object {
        const val AUTHENTICATED_TAG = "authenticated-destination"
        const val VERIFICATION_CODE = "123456"
    }
}
