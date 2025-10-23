package com.firebase.composeapp

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Surface
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.firebase.ui.auth.compose.AuthException
import com.firebase.ui.auth.compose.AuthState
import com.firebase.ui.auth.compose.FirebaseAuthUI
import com.firebase.ui.auth.compose.configuration.PasswordRule
import com.firebase.ui.auth.compose.configuration.authUIConfiguration
import com.firebase.ui.auth.compose.configuration.auth_provider.AuthProvider
import com.firebase.ui.auth.compose.configuration.theme.AuthUIAsset
import com.firebase.ui.auth.compose.configuration.theme.AuthUITheme
import com.firebase.ui.auth.compose.ui.screens.AuthSuccessUiContext
import com.firebase.ui.auth.compose.ui.screens.EmailSignInLinkHandlerActivity
import com.firebase.ui.auth.compose.ui.screens.FirebaseAuthScreen
import com.google.firebase.FirebaseApp

/**
 * Main launcher activity that allows users to choose between different
 * authentication API demonstrations.
 */
class MainActivity : ComponentActivity() {
    companion object {
        private const val USE_AUTH_EMULATOR = false
        private const val AUTH_EMULATOR_HOST = "10.0.2.2"
        private const val AUTH_EMULATOR_PORT = 9099
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Initialize Firebase and configure emulator if needed
        FirebaseApp.initializeApp(applicationContext)
        val authUI = FirebaseAuthUI.getInstance()

        if (USE_AUTH_EMULATOR) {
            authUI.auth.useEmulator(AUTH_EMULATOR_HOST, AUTH_EMULATOR_PORT)
        }

        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    ChooserScreen(
                        onHighLevelApiClick = {
                            startActivity(Intent(this, HighLevelApiDemoActivity::class.java))
                        },
                        onLowLevelApiClick = {
                            startActivity(Intent(this, AuthFlowControllerDemoActivity::class.java))
                        },
                        onCustomSlotsClick = {
                            startActivity(Intent(this, CustomSlotsThemingDemoActivity::class.java))
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun ChooserScreen(
    onHighLevelApiClick: () -> Unit,
    onLowLevelApiClick: () -> Unit,
    onCustomSlotsClick: () -> Unit
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

        Spacer(modifier = Modifier.height(32.dp))

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
