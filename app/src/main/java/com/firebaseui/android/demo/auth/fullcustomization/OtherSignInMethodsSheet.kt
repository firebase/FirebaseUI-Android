package com.firebaseui.android.demo.auth.fullcustomization

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.firebase.ui.auth.configuration.auth_provider.AuthProvider
import com.firebase.ui.auth.ui.components.TermsAndPrivacyForm

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OtherSignInMethodsSheet(
    otherProviders: List<AuthProvider>,
    onProviderSelected: (AuthProvider) -> Unit,
    onDismissRequest: () -> Unit,
    tosUrl: String?,
    ppUrl: String?,
) {
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        containerColor = MaterialTheme.colorScheme.primaryContainer,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 64.dp),
        ) {
            Text(
                text = "Other sign in methods",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
                    .semantics { contentDescription = "Other sign-in methods sheet title" },
            )
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                otherProviders.forEach { provider ->
                    SheetProviderButton(
                        provider = provider,
                        onClick = {
                            onDismissRequest()
                            onProviderSelected(provider)
                        },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            TermsAndPrivacyForm(tosUrl = tosUrl, ppUrl = ppUrl)
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
