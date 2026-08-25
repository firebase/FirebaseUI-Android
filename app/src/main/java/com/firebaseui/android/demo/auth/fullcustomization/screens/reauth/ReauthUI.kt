package com.firebaseui.android.demo.auth.fullcustomization.screens.reauth

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.firebase.ui.auth.ui.screens.reauth.ReauthContentState
import com.firebaseui.android.demo.R
import com.firebaseui.android.demo.auth.fullcustomization.common.AuthPage
import com.firebaseui.android.demo.auth.fullcustomization.common.SheetProviderButton

/**
 * Custom UI for `FirebaseAuthScreen.reauthContent`.
 *
 * [ReauthContentState.providers] arrives already filtered to the providers linked to this user, and
 * [ReauthContentState.onProviderSelected] performs the credential exchange, so this is purely a
 * chooser: the library owns the reauthentication itself and the dismiss/retry sequencing that
 * follows it. Picking email or phone hands off to the library's own sub-flow.
 */
@Composable
fun ReauthUI(state: ReauthContentState) {
    // The slot renders as an overlay outside the NavHost, so nothing else consumes the system back
    // press — without this it would fall through and finish the Activity mid-reauthentication.
    BackHandler(enabled = !state.isLoading) { state.onDismiss() }

    AuthPage(
        mascot = R.drawable.full_customization_mascot,
        mascotDescription = "doggo - cute welcome mascot",
        title = "Is that you?",
        cardContentDescription = "reauth - provider chooser card",
        card = {
            Text(
                text = state.reason
                    ?: "Confirm it's you to continue with ${state.user.email ?: "this account"}.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            state.error?.let { error ->
                Text(
                    text = error,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.error,
                )
            }

            if (state.isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .semantics { contentDescription = "reauth - in progress" },
                )
            }
        },
        actions = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                state.providers.forEach { provider ->
                    SheetProviderButton(
                        provider = provider,
                        onClick = { state.onProviderSelected(provider) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .semantics {
                                contentDescription = "button - reauth with ${provider.providerName}"
                            },
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            TextButton(
                onClick = state.onDismiss,
                enabled = !state.isLoading,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Cancel")
            }
        },
    )
}
