package com.firebase.uidemo.auth.compose

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.firebase.ui.auth.AuthUI
import com.firebase.ui.auth.data.model.FirebaseAuthUIAuthenticationResult
import com.firebase.uidemo.R
import com.firebase.uidemo.auth.SignedInActivity
import com.firebase.uidemo.ui.theme.FirebaseUIDemoTheme

class AuthComposeActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            FirebaseUIDemoTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AuthScreen { result ->
                        handleSignInResponse(result)
                    }
                }
            }
        }
    }

    private fun handleSignInResponse(result: FirebaseAuthUIAuthenticationResult) {
        when (result.resultCode) {
            RESULT_OK -> {
                // Successfully signed in
                val response = result.idpResponse
                startActivity(SignedInActivity.createIntent(this, response))
                finish()
            }
            else -> {
                // Sign in failed
                val response = result.idpResponse
                if (response == null) {
                    // User pressed back button
                    finish()
                    return
                }
                // Handle other error cases
            }
        }
    }

    companion object {
        fun createIntent(context: Context): Intent {
            return Intent(context, AuthComposeActivity::class.java)
        }
    }
} 