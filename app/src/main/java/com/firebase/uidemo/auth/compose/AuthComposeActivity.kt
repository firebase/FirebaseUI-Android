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

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.core.view.WindowCompat
import com.firebase.ui.auth.data.model.FirebaseAuthUIAuthenticationResult
import com.firebase.uidemo.auth.SignedInActivity
import com.firebase.uidemo.ui.theme.FirebaseUIDemoTheme

class AuthComposeActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Configure system UI
        window.apply {
            statusBarColor = Color.White.toArgb()
            navigationBarColor = Color.White.toArgb()

            WindowCompat.getInsetsController(this, decorView).apply {
                isAppearanceLightStatusBars = true
                isAppearanceLightNavigationBars = true
            }
        }

        setContent {
            FirebaseUIDemoTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = Color.White) {
                    AuthScreen { result -> handleSignInResponse(result) }
                }
            }
        }
    }

    private fun handleSignInResponse(result: FirebaseAuthUIAuthenticationResult) {
        when (result.resultCode) {
            RESULT_OK -> {
                val response = result.idpResponse
                startActivity(SignedInActivity.createIntent(this, response))
                finish()
            }
            else -> {
                val response = result.idpResponse
                if (response == null) {
                    finish()
                    return
                }
            }
        }
    }

    companion object {
        fun createIntent(context: Context): Intent {
            return Intent(context, AuthComposeActivity::class.java)
        }
    }
}
