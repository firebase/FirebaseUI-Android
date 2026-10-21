package com.firebaseui.android.demo.auth.fullcustomization.screens.phone

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.firebase.ui.auth.ui.screens.phone.PhoneAuthContentState
import com.firebase.ui.auth.ui.screens.phone.PhoneAuthStep
import com.firebaseui.android.demo.R
import com.firebaseui.android.demo.auth.fullcustomization.screens.phone.pages.PhoneEntryStep
import com.firebaseui.android.demo.auth.fullcustomization.screens.phone.pages.PhoneVerificationStep

@Composable
fun PhoneSignInUI(state: PhoneAuthContentState) {
    Box(modifier = Modifier.fillMaxSize()) {
        Image(
            painter = painterResource(id = R.drawable.custom_background),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
        when (state.step) {
            PhoneAuthStep.EnterPhoneNumber -> PhoneEntryStep(state)
            PhoneAuthStep.EnterVerificationCode -> PhoneVerificationStep(state)
        }
    }
}
