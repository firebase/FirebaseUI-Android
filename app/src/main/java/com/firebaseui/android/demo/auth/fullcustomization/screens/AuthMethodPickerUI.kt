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
import com.firebaseui.android.demo.auth.fullcustomization.common.OtherSignInMethodsSheet
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

private fun AuthRoute.Email.Step.toMode(): EmailAuthMode = when (this) {
    is AuthRoute.Email.SignIn -> EmailAuthMode.SignIn
    is AuthRoute.Email.SignUp -> EmailAuthMode.SignUp
    is AuthRoute.Email.ResetPassword -> EmailAuthMode.ResetPassword
    is AuthRoute.Email.EmailLinkSignIn -> EmailAuthMode.EmailLinkSignIn
}

private fun stepFor(mode: EmailAuthMode, email: String?): AuthRoute.Email.Step = when (mode) {
    EmailAuthMode.SignIn -> AuthRoute.Email.SignIn(email)
    EmailAuthMode.SignUp -> AuthRoute.Email.SignUp(email)
    EmailAuthMode.ResetPassword -> AuthRoute.Email.ResetPassword(email)
    EmailAuthMode.EmailLinkSignIn -> AuthRoute.Email.EmailLinkSignIn(email)
}

/**
 * Navigates to [target], replacing any existing entry of the same step *type* rather than stacking
 * a duplicate — matching [com.firebase.ui.auth.ui.screens.email.navigateToEmailStep], which the
 * library keeps `internal` to its own module. Adds before removing, so no single write leaves the
 * stack without the chooser at its base.
 */
private fun MutableList<NavKey>.navigateToStep(target: AuthRoute.Email.Step) {
    val existing = indexOfFirst { it is AuthRoute.Email.Step && it::class == target::class }
    add(target)
    if (existing >= 0) {
        while (size > existing + 1) removeAt(existing)
    }
}

/** Matches the 700ms cross-fade [FirebaseAuthScreen][com.firebase.ui.auth.ui.screens.FirebaseAuthScreen]
 * itself falls back to, so a step switch here looks the same as one at the top level. */
private val EmailStepTransform: AnimatedContentTransitionScope<Scene<NavKey>>.() -> ContentTransform = {
    fadeIn(animationSpec = tween(700)) togetherWith fadeOut(animationSpec = tween(700))
}

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

    Box(modifier = Modifier.fillMaxSize()) {
        NavDisplay(
            backStack = backStack,
            transitionSpec = EmailStepTransform,
            popTransitionSpec = EmailStepTransform,
            entryProvider = entryProvider {
                entry<EmailEntryKey> {
                    // Local and disposable: this step performs no auth operation of its own, so
                    // there is nothing here for EmailAuthContentState to own.
                    var email by rememberSaveable { mutableStateOf("") }
                    EmailEntryStep(
                        email = email,
                        onEmailChange = { email = it },
                        isLoading = false,
                        onSignIn = dropUnlessResumed {
                            backStack.navigateToStep(AuthRoute.Email.SignIn(email))
                        },
                        onCreateAccount = dropUnlessResumed {
                            backStack.navigateToStep(AuthRoute.Email.SignUp(email))
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
            otherProviders = otherProviders,
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
    // Resets to the chooser rather than popping one entry: from ResetPassword (reached via
    // LoginStep's "forgot password" link) the stack is [chooser, SignIn, ResetPassword], and this
    // is meant to leave the whole in-progress mode, not step back into it.
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
        onNavigateToMode = { mode, email -> backStack.navigateToStep(stepFor(mode, email)) },
        onSuccess = onSuccess,
        onError = onError,
        onCancel = onCancel,
    ) { state ->
        when (state.mode) {
            EmailAuthMode.SignUp -> SignUpStep(state, onUseDifferentEmail)
            // Reset-password and email-link are offered inline on the login form, which also
            // reports their "sent" states, so every mode has a screen and none can blank out.
            EmailAuthMode.SignIn,
            EmailAuthMode.ResetPassword,
            EmailAuthMode.EmailLinkSignIn -> LoginStep(state, onUseDifferentEmail)
        }
    }
}
