package com.firebaseui.android.demo.auth.fullcustomization.screens.mfa

import androidx.compose.runtime.Composable
import com.firebase.ui.auth.mfa.MfaEnrollmentContentState
import com.firebase.ui.auth.mfa.MfaEnrollmentStep
import com.firebaseui.android.demo.auth.fullcustomization.screens.mfa.pages.ConfigureSmsStep
import com.firebaseui.android.demo.auth.fullcustomization.screens.mfa.pages.ConfigureTotpStep
import com.firebaseui.android.demo.auth.fullcustomization.screens.mfa.pages.RecoveryCodesStep
import com.firebaseui.android.demo.auth.fullcustomization.screens.mfa.pages.SelectFactorStep
import com.firebaseui.android.demo.auth.fullcustomization.screens.mfa.pages.VerifyFactorStep

/**
 * Custom UI for `FirebaseAuthScreen.mfaEnrollmentContent`.
 *
 * A single state object drives all five enrollment steps, so this only dispatches on
 * [MfaEnrollmentContentState.step] — the library owns the step transitions.
 */
@Composable
fun MfaEnrollmentUI(state: MfaEnrollmentContentState) {
    when (state.step) {
        MfaEnrollmentStep.SelectFactor -> SelectFactorStep(state)
        MfaEnrollmentStep.ConfigureSms -> ConfigureSmsStep(state)
        MfaEnrollmentStep.ConfigureTotp -> ConfigureTotpStep(state)
        MfaEnrollmentStep.VerifyFactor -> VerifyFactorStep(state)
        MfaEnrollmentStep.ShowRecoveryCodes -> RecoveryCodesStep(state)
    }
}
