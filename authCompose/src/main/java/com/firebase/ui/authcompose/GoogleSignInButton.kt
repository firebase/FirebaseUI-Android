package com.firebase.ui.authcompose

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import com.firebase.core.IdpConfig
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@Composable
fun GoogleSignInButton(
    config: IdpConfig,
    firebaseAuth: FirebaseAuth,
    onSuccess: (FirebaseUser) -> Unit,
    onFailure: (Exception) -> Unit,
    onCancelled: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isLoading by remember { mutableStateOf(false) }

    Button(
        onClick = {
            isLoading = true
            scope.launch {
                // Modern Google Sign-In flow using Credential Manager API:
                // 1. Extract server client ID from config
                // 2. Build Google ID credential request
                // 3. Get credential from system via Credential Manager
                // 4. Extract Google ID token from credential
                // 5. Convert to Firebase credential and sign in
                // 6. Handle success/failure with callbacks
                try {
                    val serverClientId = config.params["serverClientId"] as? String
                        ?: throw IllegalStateException("Server client ID not found")

                    val googleIdOption = GetGoogleIdOption.Builder()
                        .setServerClientId(serverClientId)
                        .setFilterByAuthorizedAccounts(false)
                        .setAutoSelectEnabled(true)
                        .build()

                    val request = GetCredentialRequest.Builder()
                        .addCredentialOption(googleIdOption)
                        .build()

                    val credentialManager = CredentialManager.create(context)
                    val result = credentialManager.getCredential(context, request)

                    val credential = result.credential
                    val googleIdTokenCredential =
                        GoogleIdTokenCredential.createFrom(credential.data)
                    val idToken = googleIdTokenCredential.idToken

                    val firebaseCredential = GoogleAuthProvider.getCredential(idToken, null)
                    val authResult = firebaseAuth.signInWithCredential((firebaseCredential)).await()

                    authResult.user?.let { user ->
                        onSuccess(user)
                    } ?: run {
                        onFailure(Exception("Sign in failed: no user returned"))
                    }
                } catch (e: GetCredentialCancellationException) {
                    onCancelled()
                } catch (e: GetCredentialException) {
                    onFailure(e)
                } catch (e: Exception) {
                    onFailure(e)
                } finally {
                    isLoading = false
                }
            }
        },
        enabled = !isLoading,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp)
                )
            } else {
                Icon(
                    painter = painterResource(R.drawable.fui_ic_googleg_color_24dp),
                    contentDescription = "google sign in logo",
                    modifier = Modifier
                        .size(18.dp)
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text("Sign in with Google")
        }
    }
}
