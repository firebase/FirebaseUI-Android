package com.firebaseui.android.demo.auth.fullcustomization.screens.phone

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.firebase.ui.auth.ui.screens.phone.PhoneAuthContentState
import com.firebase.ui.auth.ui.screens.phone.PhoneAuthStep
import com.firebaseui.android.demo.auth.fullcustomization.common.ThemeBackground
import com.firebaseui.android.demo.auth.fullcustomization.screens.phone.pages.PhoneEntryStep
import com.firebaseui.android.demo.auth.fullcustomization.screens.phone.pages.PhoneVerificationStep

@Composable
fun PhoneSignInUI(state: PhoneAuthContentState) {
    Box(modifier = Modifier.fillMaxSize()) {
        ThemeBackground()
        when (state.step) {
            PhoneAuthStep.EnterPhoneNumber -> PhoneEntryStep(state)
            PhoneAuthStep.EnterVerificationCode -> PhoneVerificationStep(state)
        }
    }
}
