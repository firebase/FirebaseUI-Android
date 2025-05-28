package com.firebase.uidemo.auth.compose

import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.bumptech.glide.integration.compose.GlideImage
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.firebase.ui.auth.AuthUI
import com.firebase.ui.auth.IdpResponse
import com.firebase.uidemo.R
import com.google.firebase.auth.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SignedInScreen(
    idpResponse: IdpResponse?,
    onSignedOut: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }

    val firebaseUser by produceState(initialValue = FirebaseAuth.getInstance().currentUser) {
        val listener = FirebaseAuth.AuthStateListener { auth -> value = auth.currentUser }
        FirebaseAuth.getInstance().addAuthStateListener(listener)
        awaitDispose { FirebaseAuth.getInstance().removeAuthStateListener(listener) }
    }
    if (firebaseUser == null) {
        onSignedOut(); return
    }

    var askDelete by remember { mutableStateOf(false) }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = { CenterAlignedTopAppBar(title = { Text("Profile") }) },
        floatingActionButton = {
            FloatingActionButton(onClick = { askDelete = true }) {
                Icon(Icons.Default.Delete, contentDescription = null)
            }
        }
    ) { padding ->
        ProfileContent(
            modifier = Modifier.padding(padding),
            user = firebaseUser!!,
            isNewUser = idpResponse?.isNewUser ?: false,
            idpToken = idpResponse?.idpToken,
            idpSecret = idpResponse?.idpSecret,
            onSignOut = {
                AuthUI.getInstance().signOut(context).addOnCompleteListener { task ->
                    if (task.isSuccessful) onSignedOut()
                    else scope.launch {
                        snackbar.showSnackbar(
                            context.getString(R.string.sign_out_failed)
                        )
                    }
                }
            }
        )
    }

    if (askDelete) {
        AlertDialog(
            onDismissRequest = { askDelete = false },
            confirmButton = {
                TextButton(onClick = {
                    askDelete = false
                    AuthUI.getInstance().delete(context).addOnCompleteListener { task ->
                        if (task.isSuccessful) onSignedOut()
                        else scope.launch {
                            snackbar.showSnackbar(
                                context.getString(R.string.delete_account_failed)
                            )
                        }
                    }
                }) { Text("Yes, nuke it!") }
            },
            dismissButton = { TextButton(onClick = { askDelete = false }) { Text("No") } },
            title = { Text("Delete account") },
            text  = { Text("Are you sure you want to delete this account?") }
        )
    }
}


@OptIn(ExperimentalGlideComposeApi::class)
@Composable
private fun ProfileContent(
    modifier: Modifier = Modifier,
    user: FirebaseUser,
    isNewUser: Boolean,
    idpToken: String?,
    idpSecret: String?,
    onSignOut: () -> Unit
) {
    val ctx = LocalContext.current
    val providers = remember(user) { user.enabledProviderNames(ctx) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        user.photoUrl?.let { url ->
            GlideImage(
                model = url,
                contentDescription = null,
                modifier = Modifier
                    .size(96.dp)
                    .clip(CircleShape)
            )
            Spacer(Modifier.height(16.dp))
        }

        InfoRow(label = "Email", value = user.email)
        InfoRow(label = "Phone", value = user.phoneNumber)
        InfoRow(label = "Display name", value = user.displayName)

        if (isNewUser) {
            Text(
                "New user",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        Spacer(Modifier.height(12.dp))

        Text(
            text = stringResource(R.string.used_providers, providers),
            style = MaterialTheme.typography.bodySmall
        )

        if (idpToken != null) {
            Spacer(Modifier.height(12.dp))
            TokenBlock(label = "IDP Token", value = idpToken)
        }
        if (idpSecret != null) {
            Spacer(Modifier.height(8.dp))
            TokenBlock(label = "IDP Secret", value = idpSecret)
        }

        Spacer(Modifier.weight(1f))

        Button(
            onClick = onSignOut,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.ExitToApp, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.sign_out))
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String?) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label)
        Text(
            text = value.takeUnless { it.isNullOrBlank() } ?: "—",
            overflow = TextOverflow.Ellipsis,
            maxLines = 1
        )
    }
}

@Composable
private fun TokenBlock(label: String, value: String) {
    Column(Modifier.fillMaxWidth()) {
        Text(label, style = MaterialTheme.typography.labelSmall)
        Text(value, style = MaterialTheme.typography.bodySmall, overflow = TextOverflow.Ellipsis)
    }
}

private fun FirebaseUser.enabledProviderNames(ctx: Context): String {
    if (providerData.isEmpty()) return ctx.getString(R.string.providers_anonymous)

    val names = providerData.mapNotNull { info ->
        when (info.providerId) {
            GoogleAuthProvider.PROVIDER_ID   -> ctx.getString(R.string.providers_google)
            FacebookAuthProvider.PROVIDER_ID -> ctx.getString(R.string.providers_facebook)
            TwitterAuthProvider.PROVIDER_ID  -> ctx.getString(R.string.providers_twitter)
            EmailAuthProvider.PROVIDER_ID    -> ctx.getString(R.string.providers_email)
            PhoneAuthProvider.PROVIDER_ID    -> ctx.getString(R.string.providers_phone)
            AuthUI.EMAIL_LINK_PROVIDER       -> ctx.getString(R.string.providers_email_link)
            FirebaseAuthProvider.PROVIDER_ID -> null   // ignore
            else                             -> info.providerId
        }
    }
    return names.joinToString()
}