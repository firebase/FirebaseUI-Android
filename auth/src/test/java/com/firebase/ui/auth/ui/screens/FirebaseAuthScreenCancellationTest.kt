package com.firebase.ui.auth.ui.screens

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.core.app.ApplicationProvider
import com.firebase.ui.auth.AuthState
import com.firebase.ui.auth.FirebaseAuthUI
import com.firebase.ui.auth.configuration.authUIConfiguration
import com.firebase.ui.auth.configuration.auth_provider.AuthProvider
import com.google.common.truth.Truth.assertThat
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.FirebaseAuth
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, sdk = [34])
class FirebaseAuthScreenCancellationTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Mock
    private lateinit var mockFirebaseAuth: FirebaseAuth

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
    fun `single email provider cancellation invokes callback once`() {
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
        var cancelCount = 0

        composeTestRule.setContent {
            FirebaseAuthScreen(
                configuration = configuration,
                authUI = authUI,
                onSignInSuccess = {},
                onSignInFailure = {},
                onSignInCancelled = { cancelCount++ }
            )
        }

        composeTestRule.runOnIdle {
            authUI.updateAuthState(AuthState.Cancelled)
        }
        composeTestRule.waitForIdle()

        assertThat(cancelCount).isEqualTo(1)
    }

    @Test
    fun `single phone provider cancellation invokes callback once`() {
        val configuration = authUIConfiguration {
            context = ApplicationProvider.getApplicationContext()
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
        var cancelCount = 0

        composeTestRule.setContent {
            FirebaseAuthScreen(
                configuration = configuration,
                authUI = authUI,
                onSignInSuccess = {},
                onSignInFailure = {},
                onSignInCancelled = { cancelCount++ }
            )
        }

        composeTestRule.runOnIdle {
            authUI.updateAuthState(AuthState.Cancelled)
        }
        composeTestRule.waitForIdle()

        assertThat(cancelCount).isEqualTo(1)
    }
}
