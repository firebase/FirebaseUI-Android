package com.firebase.ui.appcompose

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.firebase.ui.appcompose.ui.theme.FirebaseUIAndroidTheme
import com.firebase.ui.authcompose.AuthUI
import com.firebase.core.IdpConfig
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser

@Composable
fun MainView() {
    var currentUser by remember { mutableStateOf<FirebaseUser?>(null) }

    val providers = listOf(
        IdpConfig.GoogleBuilder()
            .setServerClientId("771411398215-o39fujhds88bs4mb5ai7u6o73g86fspp.apps.googleusercontent.com")
            .build(),
        IdpConfig.EmailBuilder().build(),
        IdpConfig.PhoneBuilder().build(),
    )

    Scaffold(
        modifier = Modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            when (currentUser) {
                null -> {
                    AuthUI.initialize(
                        providers = providers,
                        onAuthSuccess = { user ->
                            currentUser = user
                            println("✅Sign-in successful: ${user.displayName}")
                        },
                        onAuthFailure = { error ->
                            println("❌ Sign-in failed: ${error.message}")
                        },
                        onAuthCancelled = {
                            println("🚫 Sign-in cancelled")
                        }
                    )
                }

                else -> {
                    Column {
                        Text("Welcome, ${currentUser?.displayName}")
                        Button(
                            onClick = {
                                FirebaseAuth.getInstance().signOut()
                                currentUser = null
                            }
                        ) {
                            Text("Sign Out")
                        }
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewMainView() {
    FirebaseUIAndroidTheme {
        MainView()
    }
}
