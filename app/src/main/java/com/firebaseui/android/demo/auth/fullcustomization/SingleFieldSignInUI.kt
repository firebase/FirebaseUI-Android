package com.firebaseui.android.demo.auth.fullcustomization

import android.util.Log
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.firebase.ui.auth.configuration.auth_provider.AuthProvider
import com.firebase.ui.auth.ui.screens.email.EmailAuthContentState
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

private enum class FlowStep { EnterEmail, Login, SignUp }

@Composable
fun SingleFieldSignInUI(
    state: EmailAuthContentState,
    otherProviders: List<AuthProvider>,
    onProviderSelected: (AuthProvider) -> Unit,
    tosUrl: String?,
    ppUrl: String?,
) {
    var flowStep by remember { mutableStateOf(FlowStep.EnterEmail) }
    var showOtherMethods by remember { mutableStateOf(false) }
    var isCheckingEmail by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    // Password/confirmPassword are hoisted in EmailAuthContentState, not local to LoginStep/
    // SignUpStep — they survive a round trip back to EnterEmail, so a stale password typed for
    // one email could carry over if a different email also routes to the same step. Clear them
    // whenever the user backs out via "Use a different email".
    val onUseDifferentEmail: () -> Unit = {
        state.onPasswordChange("")
        state.onConfirmPasswordChange("")
        flowStep = FlowStep.EnterEmail
    }

    Box(modifier = Modifier.fillMaxSize()) {
        when (flowStep) {
            FlowStep.EnterEmail -> EmailEntryStep(
                email = state.email,
                onEmailChange = state.onEmailChange,
                isLoading = state.isLoading || isCheckingEmail,
                onContinue = {
                    isCheckingEmail = true
                    coroutineScope.launch {
                        val signInMethods = fetchLegacySignInMethods(state.email)
                        flowStep = if (signInMethods.isEmpty()) FlowStep.SignUp else FlowStep.Login
                        isCheckingEmail = false
                    }
                },
                onShowOtherMethods = { showOtherMethods = true },
            )

            FlowStep.Login -> LoginStep(
                state = state,
                onUseDifferentEmail = onUseDifferentEmail,
            )

            FlowStep.SignUp -> SignUpStep(
                state = state,
                onUseDifferentEmail = onUseDifferentEmail,
            )
        }
    }

    if (showOtherMethods) {
        OtherSignInMethodsSheet(
            otherProviders = otherProviders,
            onProviderSelected = onProviderSelected,
            onDismissRequest = { showOtherMethods = false },
            tosUrl = tosUrl,
            ppUrl = ppUrl,
        )
    }
}

/**
 * Whether [email] is already registered, via Firebase Auth's `fetchSignInMethodsForEmail` —
 * deprecated by Firebase ("legacy") and, depending on the project's Email Enumeration Protection
 * setting, may always return an empty list regardless of whether the email exists.
 */
private suspend fun fetchLegacySignInMethods(email: String): List<String> {
    return try {
        @Suppress("DEPRECATION")
        FirebaseAuth.getInstance()
            .fetchSignInMethodsForEmail(email)
            .await()
            .signInMethods
            ?.filter { it.isNotBlank() }
            ?: emptyList()
    } catch (e: Exception) {
        Log.w("SingleFieldSignInUI", "fetchSignInMethodsForEmail failed for $email", e)
        emptyList()
    }
}
