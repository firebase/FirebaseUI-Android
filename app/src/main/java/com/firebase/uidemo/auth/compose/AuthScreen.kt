/*
 * Copyright 2025 Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.firebase.uidemo.auth.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.firebase.ui.auth.AuthUI.IdpConfig
import com.firebase.ui.auth.compose.FirebaseAuthUI
import com.firebase.ui.auth.data.model.FirebaseAuthUIAuthenticationResult
import com.firebase.uidemo.R

@Composable
fun AuthScreen(onSignInResult: (FirebaseAuthUIAuthenticationResult) -> Unit) {
    val providers =
            listOf(
                    IdpConfig.GoogleBuilder().build(),
                    IdpConfig.EmailBuilder().build(),
            )

    Box(modifier = Modifier.fillMaxSize().background(Color.White)) {
        FirebaseAuthUI(
                providers = providers,
                onSignInResult = { result -> /* optional logging */ },
                signedInContent = { SignedInScreen(idpResponse = null) {} },
                theme = R.style.AppTheme,
                logo = R.drawable.firebase_auth_120dp,
                tosUrl = "https://www.google.com/policies/terms/",
                privacyPolicyUrl = "https://www.google.com/policies/privacy/"
        )
    }
}
