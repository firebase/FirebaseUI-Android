package com.firebaseui.android.demo.auth.fullcustomization

import androidx.navigation3.runtime.NavKey
import com.firebase.ui.auth.ui.screens.AuthRoute
import com.firebase.ui.auth.ui.screens.email.EmailAuthMode

/** The demo's own navigation over its email back stack; not FirebaseUI API. */
class DemoNavigator(private val backStack: MutableList<NavKey>) {

    /**
     * Navigates to [target], replacing any existing entry of the same step *type* rather than stacking
     * a duplicate — matching [com.firebase.ui.auth.ui.screens.email.navigateToEmailStep], which the
     * library keeps `internal` to its own module. Adds before removing, so no single write leaves the
     * stack without the chooser at its base.
     */
    fun navigate(target: AuthRoute.Email.Step) {
        val existing = backStack.indexOfFirst { it is AuthRoute.Email.Step && it::class == target::class }
        backStack.add(target)
        if (existing >= 0) {
            while (backStack.size > existing + 1) backStack.removeAt(existing)
        }
    }

    /** For [com.firebase.ui.auth.ui.screens.email.EmailAuthScreen]'s `onNavigateToMode`. */
    fun navigate(mode: EmailAuthMode, email: String?) = navigate(stepFor(mode, email))
}

private fun stepFor(mode: EmailAuthMode, email: String?): AuthRoute.Email.Step = when (mode) {
    EmailAuthMode.SignIn -> AuthRoute.Email.SignIn(email)
    EmailAuthMode.SignUp -> AuthRoute.Email.SignUp(email)
    EmailAuthMode.ResetPassword -> AuthRoute.Email.ResetPassword(email)
    EmailAuthMode.EmailLinkSignIn -> AuthRoute.Email.EmailLinkSignIn(email)
}

internal fun AuthRoute.Email.Step.toMode(): EmailAuthMode = when (this) {
    is AuthRoute.Email.SignIn -> EmailAuthMode.SignIn
    is AuthRoute.Email.SignUp -> EmailAuthMode.SignUp
    is AuthRoute.Email.ResetPassword -> EmailAuthMode.ResetPassword
    is AuthRoute.Email.EmailLinkSignIn -> EmailAuthMode.EmailLinkSignIn
}
