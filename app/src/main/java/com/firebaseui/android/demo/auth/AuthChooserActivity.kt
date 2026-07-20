package com.firebaseui.android.demo.auth

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.firebaseui.android.demo.CredentialLinkingDemoActivity

class AuthChooserActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AuthChooserScreen(
                        onHighLevelApiClick = {
                            startActivity(Intent(this, HighLevelApiDemoActivity::class.java))
                        },
                        onLowLevelApiClick = {
                            startActivity(Intent(this, AuthFlowControllerDemoActivity::class.java))
                        },
                        onCustomSlotsClick = {
                            startActivity(Intent(this, CustomSlotsThemingDemoActivity::class.java))
                        },
                        onCredentialLinkingClick = {
                            startActivity(Intent(this, CredentialLinkingDemoActivity::class.java))
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun AuthChooserScreen(
    onHighLevelApiClick: () -> Unit,
    onLowLevelApiClick: () -> Unit,
    onCustomSlotsClick: () -> Unit,
    onCredentialLinkingClick: () -> Unit = {},
    isEmulatorMode: Boolean = false
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .systemBarsPadding()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        Spacer(modifier = Modifier.height(16.dp))
        // Header
        Text(
            text = "Firebase Auth UI Compose",
            style = MaterialTheme.typography.headlineLarge,
            textAlign = TextAlign.Center
        )

        Text(
            text = "Choose a demo to explore different authentication APIs",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        // Emulator Mode Warning
        if (isEmulatorMode) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "⚠️ Emulator Mode",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                    Text(
                        text = "Running with Firebase Auth Emulator. Some features like third-party" +
                                " OAuth providers (Facebook, Twitter, LINE etc.) may not work correctly." +
                                " Disable Firebase Auth Emulator using" +
                                " MainActivity.USE_AUTH_EMULATOR = false",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
        }

        // High-Level API Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            onClick = onHighLevelApiClick
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "🎨 High-Level API",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "FirebaseAuthScreen Composable",
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = "Best for: Pure Compose applications that want a complete, ready-to-use authentication UI with minimal setup.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Features:",
                    style = MaterialTheme.typography.labelLarge
                )
                Text(
                    text = "• Drop-in Composable\n• Automatic navigation\n• State management included\n• Customizable content",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Low-Level API Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            onClick = onLowLevelApiClick
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "⚙️ Low-Level API",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "AuthFlowController",
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = "Best for: Applications that need fine-grained control over the authentication flow with ActivityResultLauncher integration.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Features:",
                    style = MaterialTheme.typography.labelLarge
                )
                Text(
                    text = "• Lifecycle-safe controller\n• ActivityResultLauncher\n• Observable state with Flow\n• Manual flow control",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Custom Slots & Theming Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            onClick = onCustomSlotsClick
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "🎨 Custom Slots & Theming",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Slot APIs & Theme Customization",
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = "Best for: Applications that need fully custom UI while leveraging the authentication logic and state management.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Features:",
                    style = MaterialTheme.typography.labelLarge
                )
                Text(
                    text = "• Custom email auth UI via slots\n• Custom phone auth UI via slots\n• AuthUITheme.fromMaterialTheme()\n• Custom ProviderStyle examples",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Credential Linking Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            onClick = onCredentialLinkingClick
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "🔗 Credential Linking",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "isCredentialLinkingEnabled",
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = "Sign in with one provider, then add another to the same account without losing your UID.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Info card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "💡 Tip",
                    style = MaterialTheme.typography.labelLarge
                )
                Text(
                    text = "Both APIs provide the same authentication capabilities. Choose based on your app's architecture and control requirements.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}
