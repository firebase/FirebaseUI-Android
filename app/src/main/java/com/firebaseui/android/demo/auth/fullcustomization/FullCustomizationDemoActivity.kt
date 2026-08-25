package com.firebaseui.android.demo.auth.fullcustomization

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import com.firebase.ui.auth.AuthException
import com.firebase.ui.auth.FirebaseAuthUI
import com.firebase.ui.auth.configuration.AuthUIConfiguration
import com.firebase.ui.auth.configuration.MfaConfiguration
import com.firebase.ui.auth.configuration.MfaFactor
import com.firebase.ui.auth.configuration.authUIConfiguration
import com.firebase.ui.auth.configuration.auth_provider.AuthProvider
import com.firebase.ui.auth.configuration.theme.AuthUIAsset
import com.firebase.ui.auth.ui.screens.FirebaseAuthScreen
import com.firebase.ui.auth.ui.screens.email.EmailAuthScreen
import com.firebaseui.android.demo.R
import com.firebaseui.android.demo.auth.fullcustomization.screens.AuthMethodPickerUI
import com.firebaseui.android.demo.auth.fullcustomization.screens.AuthenticatedUI
import com.firebaseui.android.demo.auth.fullcustomization.screens.mfa.MfaChallengeUI
import com.firebaseui.android.demo.auth.fullcustomization.screens.mfa.MfaEnrollmentUI
import com.firebaseui.android.demo.auth.fullcustomization.screens.phone.PhoneSignInUI
import com.firebaseui.android.demo.auth.fullcustomization.screens.reauth.ReauthUI
import com.firebaseui.android.demo.auth.fullcustomization.theme.FullCustomizationTheme

class FullCustomizationDemoActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val authUI = FirebaseAuthUI.getInstance()
        val configuration = authUIConfiguration {
            context = applicationContext
            logo = AuthUIAsset.Resource(R.drawable.firebase_auth)
            tosUrl = "https://policies.google.com/terms"
            privacyPolicyUrl = "https://policies.google.com/privacy"
            providers {
                provider(
                    AuthProvider.Google(
                        scopes = listOf("email"),
                        serverClientId = "406099696497-a12gakvts4epfk5pkio7dphc1anjiggc.apps.googleusercontent.com",
                    )
                )
                provider(AuthProvider.Apple(customParameters = emptyMap(), locale = null))
                provider(AuthProvider.Facebook())
                provider(AuthProvider.Twitter(customParameters = emptyMap()))
                provider(AuthProvider.Github(customParameters = emptyMap()))
                provider(AuthProvider.Microsoft(tenant = null, customParameters = emptyMap()))
                provider(AuthProvider.Yahoo(customParameters = emptyMap()))
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
                provider(AuthProvider.Anonymous)
            }
        }

        setContent {
            FullCustomizationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    FirebaseAuthScreen(
                        configuration = configuration,
                        authUI = authUI,
                        onSignInSuccess = { result ->
                            Log.d("FullCustomizationDemo", "Auth success: ${result.user?.uid}")
                        },
                        onSignInFailure = { exception: AuthException ->
                            Log.e("FullCustomizationDemo", "Auth failed", exception)
                        },
                        onSignInCancelled = {
                            Log.d("FullCustomizationDemo", "Auth cancelled")
                        },
                        mfaConfiguration = MfaConfiguration(
                            allowedFactors = listOf(MfaFactor.Sms, MfaFactor.Totp),
                            requireEnrollment = false,
                        ),
                        customMethodPickerLayout = { providers, onProviderSelected ->
                            MainUI(
                                authUI = authUI,
                                configuration = configuration,
                                providers = providers,
                                onProviderSelected = onProviderSelected,
                            )
                        },
                        phoneContent = { state -> PhoneSignInUI(state) },
                        mfaEnrollmentContent = { state -> MfaEnrollmentUI(state) },
                        mfaChallengeContent = { state -> MfaChallengeUI(state) },
                        reauthContent = { state -> ReauthUI(state) },
                        authenticatedContent = { state, uiContext ->
                            AuthenticatedUI(state = state, uiContext = uiContext)
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun MainUI(
    authUI: FirebaseAuthUI,
    configuration: AuthUIConfiguration,
    providers: List<AuthProvider>,
    onProviderSelected: (AuthProvider) -> Unit,
) {
    val context = LocalContext.current
    Box(modifier = Modifier.fillMaxSize()) {
        Image(
            painter = painterResource(id = R.drawable.custom_background),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        Column(modifier = Modifier.fillMaxSize()) {
            EmailAuthScreen(
                context = context,
                configuration = configuration,
                authUI = authUI,
                onSuccess = { result ->
                    Log.d("FullCustomizationDemo", "Auth success: ${result.user?.uid}")
                },
                onError = { exception ->
                    Log.e("FullCustomizationDemo", "Auth failed", exception)
                },
                onCancel = {
                    Log.d("FullCustomizationDemo", "Auth cancelled")
                },
            ) { state ->
                AuthMethodPickerUI(
                    state = state,
                    auth = authUI.auth,
                    otherProviders = providers.filterNot { it is AuthProvider.Email },
                    onProviderSelected = onProviderSelected,
                    tosUrl = configuration.tosUrl,
                    ppUrl = configuration.privacyPolicyUrl,
                )
            }
        }
    }
}
