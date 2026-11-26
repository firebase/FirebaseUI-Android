package com.firebaseui.android.demo.auth.fullcustomization.screens

import android.content.Context
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.dropUnlessResumed
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.scene.Scene
import androidx.navigation3.ui.NavDisplay
import com.firebase.ui.auth.AuthException
import com.firebase.ui.auth.FirebaseAuthUI
import com.firebase.ui.auth.configuration.AuthUIConfiguration
import com.firebase.ui.auth.configuration.auth_provider.AuthProvider
import com.firebase.ui.auth.ui.screens.AuthRoute
import com.firebase.ui.auth.ui.screens.email.EmailAuthMode
import com.firebase.ui.auth.ui.screens.email.EmailAuthScreen
import com.firebaseui.android.demo.auth.fullcustomization.DemoNavigator
import com.firebaseui.android.demo.auth.fullcustomization.common.OtherSignInMethodsSheet
import com.firebaseui.android.demo.auth.fullcustomization.toMode
import com.firebaseui.android.demo.auth.fullcustomization.professional.ProSignIn
import com.firebaseui.android.demo.auth.fullcustomization.professional.isProfessional
import com.firebaseui.android.demo.auth.fullcustomization.professional.proInlineProviders
import com.firebaseui.android.demo.auth.fullcustomization.screens.email.pages.EmailEntryStep
import com.firebaseui.android.demo.auth.fullcustomization.screens.email.pages.LoginStep
import com.firebaseui.android.demo.auth.fullcustomization.screens.email.pages.SignUpStep
import com.google.firebase.auth.AuthResult
import kotlinx.serialization.Serializable

/**
 * The demo's own pre-step, entered before any [AuthRoute.Email.Step]: type an address, then choose
 * to sign in or create an account. Not part of the library's [AuthRoute] — that sealed hierarchy is
 * closed outside the auth module — so this is a plain [NavKey] of the demo's own, sharing the same
 * back stack as the library's per-mode destinations.
 */
@Serializable
private data object EmailEntryKey : NavKey

/** Matches the 700ms cross-fade [FirebaseAuthScreen][com.firebase.ui.auth.ui.screens.FirebaseAuthScreen]
 * itself falls back to, so a step switch here looks the same as one at the top level. */
private val EmailStepTransform: AnimatedContentTransitionScope<Scene<NavKey>>.() -> ContentTransform = {
    fadeIn(animationSpec = tween(700)) togetherWith fadeOut(animationSpec = tween(700))
}

/**
 * [NavDisplay]'s predictive-back default scales the outgoing step down to 70% while the incoming one
 * springs in at full size, so a swipe back drew both steps superimposed at different scales. Reuse
 * the cross-fade above so a gesture back looks like a tapped one.
 */
private val EmailStepPredictivePopTransform:
    AnimatedContentTransitionScope<Scene<NavKey>>.(Int) -> ContentTransform =
    { EmailStepTransform(this) }

/**
 * Custom UI for `customMethodPickerLayout`'s email path.
 *
 * Hosts its own [NavDisplay] over [AuthRoute.Email]'s public per-mode destinations, the same
 * mechanism [com.firebase.ui.auth.ui.screens.FirebaseAuthScreen] uses for its own hosted
 * destinations — so switching between sign-in, sign-up, password reset and email-link sign-in
 * animates, gets a real back-stack entry, and never loses the address the user already typed,
 * because the address travels as a field on the step's own key rather than in state a switch could
 * clear.
 */
@Composable
fun AuthMethodPickerUI(
    context: Context,
    configuration: AuthUIConfiguration,
    authUI: FirebaseAuthUI,
    otherProviders: List<AuthProvider>,
    onProviderSelected: (AuthProvider) -> Unit,
    onSuccess: (AuthResult) -> Unit,
    onError: (AuthException) -> Unit,
    onCancel: () -> Unit,
) {
    var showOtherMethods by remember { mutableStateOf(false) }
    val backStack = rememberNavBackStack(EmailEntryKey)
    val navigator = remember(backStack) { DemoNavigator(backStack) }
    // Professional shows the common providers on its sign-in screen; the sheet keeps the rest.
    val inlineProviders = if (isProfessional()) proInlineProviders(otherProviders) else emptyList()

    Box(modifier = Modifier.fillMaxSize()) {
        NavDisplay(
            backStack = backStack,
            transitionSpec = EmailStepTransform,
            popTransitionSpec = EmailStepTransform,
            predictivePopTransitionSpec = EmailStepPredictivePopTransform,
            entryProvider = entryProvider {
                entry<EmailEntryKey> {
                    // Professional opens on sign-in itself, email and password together, as the
                    // iOS demo does; the other modes push from it like the shared steps below.
                    if (isProfessional()) {
                        EmailAuthScreen(
                            context = context,
                            configuration = configuration,
                            authUI = authUI,
                            mode = EmailAuthMode.SignIn,
                            onNavigateToMode = { mode, email -> navigator.navigate(mode, email) },
                            onSuccess = onSuccess,
                            onError = onError,
                            onCancel = onCancel,
                        ) { state ->
                            ProSignIn(
                                state = state,
                                isEmailLinkOffered = configuration.providers
                                    .filterIsInstance<AuthProvider.Email>()
                                    .firstOrNull()?.isEmailLinkSignInEnabled == true,
                                isSignUpOffered = configuration.isNewEmailAccountsAllowed &&
                                    configuration.providers.filterIsInstance<AuthProvider.Email>()
                                        .firstOrNull()?.isNewAccountsAllowed == true,
                                inlineProviders = inlineProviders,
                                onProviderSelected = onProviderSelected,
                                onShowOtherMethods = { showOtherMethods = true }
                                    .takeIf { otherProviders.size > inlineProviders.size },
                            )
                        }
                        return@entry
                    }
                    // No auth operation here, so there is nothing for EmailAuthContentState to own.
                    var email by rememberSaveable { mutableStateOf("") }
                    EmailEntryStep(
                        email = email,
                        onEmailChange = { email = it },
                        isLoading = false,
                        onSignIn = dropUnlessResumed {
                            navigator.navigate(AuthRoute.Email.SignIn(email))
                        },
                        onCreateAccount = dropUnlessResumed {
                            navigator.navigate(AuthRoute.Email.SignUp(email))
                        },
                        onShowOtherMethods = { showOtherMethods = true },
                    )
                }

                entry<AuthRoute.Email.SignIn> { step ->
                    EmailStep(step, backStack, context, configuration, authUI, onSuccess, onError, onCancel)
                }
                entry<AuthRoute.Email.SignUp> { step ->
                    EmailStep(step, backStack, context, configuration, authUI, onSuccess, onError, onCancel)
                }
                entry<AuthRoute.Email.ResetPassword> { step ->
                    EmailStep(step, backStack, context, configuration, authUI, onSuccess, onError, onCancel)
                }
                entry<AuthRoute.Email.EmailLinkSignIn> { step ->
                    EmailStep(step, backStack, context, configuration, authUI, onSuccess, onError, onCancel)
                }
            },
        )
    }

    if (showOtherMethods) {
        OtherSignInMethodsSheet(
            otherProviders = otherProviders - inlineProviders.toSet(),
            onProviderSelected = onProviderSelected,
            onDismissRequest = { showOtherMethods = false },
            tosUrl = configuration.tosUrl,
            ppUrl = configuration.privacyPolicyUrl,
        )
    }
}

/**
 * One [AuthRoute.Email.Step] destination: a single [EmailAuthScreen] instance pinned to [step]'s
 * mode, seeded with the address [step] carries. [EmailAuthScreen.onNavigateToMode] is what makes
 * a mode switch push (or replace) an entry on [backStack] instead of mutating local state.
 */
@Composable
private fun EmailStep(
    step: AuthRoute.Email.Step,
    backStack: MutableList<NavKey>,
    context: Context,
    configuration: AuthUIConfiguration,
    authUI: FirebaseAuthUI,
    onSuccess: (AuthResult) -> Unit,
    onError: (AuthException) -> Unit,
    onCancel: () -> Unit,
) {
    val navigator = remember(backStack) { DemoNavigator(backStack) }
    // Resets rather than pops: from ResetPassword the stack is [chooser, SignIn, ResetPassword].
    val onUseDifferentEmail: () -> Unit = dropUnlessResumed {
        backStack.clear()
        backStack.add(EmailEntryKey)
    }

    EmailAuthScreen(
        context = context,
        configuration = configuration,
        authUI = authUI,
        prefillEmail = step.email,
        mode = step.toMode(),
        onNavigateToMode = { mode, email -> navigator.navigate(mode, email) },
        onSuccess = onSuccess,
        onError = onError,
        onCancel = onCancel,
    ) { state ->
        when (state.mode) {
            EmailAuthMode.SignUp -> SignUpStep(state, onUseDifferentEmail)
            // Reset-password and email-link are inline on the login form, so every mode has a screen.
            EmailAuthMode.SignIn,
            EmailAuthMode.ResetPassword,
            EmailAuthMode.EmailLinkSignIn -> LoginStep(state, onUseDifferentEmail)
        }
    }
}
