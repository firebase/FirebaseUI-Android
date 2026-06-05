package com.firebaseui.android.demo

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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

class CustomSlotsThemingDemoActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    CustomSlotsDemoChooser(
                        onEmailAuthSlotClick = {
                            startActivity(Intent(this, EmailAuthSlotDemoActivity::class.java))
                        },
                        onPhoneAuthSlotClick = {
                            startActivity(Intent(this, PhoneAuthSlotDemoActivity::class.java))
                        },
                        onShapeCustomizationClick = {
                            startActivity(Intent(this, ShapeCustomizationDemoActivity::class.java))
                        },
                        onCustomMethodPickerClick = {
                            startActivity(Intent(this, CustomMethodPickerDemoActivity::class.java))
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun CustomSlotsDemoChooser(
    onEmailAuthSlotClick: () -> Unit,
    onPhoneAuthSlotClick: () -> Unit,
    onShapeCustomizationClick: () -> Unit,
    onCustomMethodPickerClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .systemBarsPadding()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Custom Slots & Theming",
            style = MaterialTheme.typography.headlineMedium
        )
        Text(
            text = "Select a demo to explore slot APIs and theme customization",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(8.dp))

        DemoCard(
            title = "Email Auth — Custom Slot",
            description = "Replace the default email sign-in UI with a fully custom composable using the content slot.",
            onClick = onEmailAuthSlotClick
        )

        DemoCard(
            title = "Phone Auth — Custom Slot",
            description = "Replace the default phone auth UI with a fully custom composable using the content slot.",
            onClick = onPhoneAuthSlotClick
        )

        DemoCard(
            title = "Shape Customization",
            description = "Preview provider button shapes using global and per-provider overrides via AuthUITheme.",
            onClick = onShapeCustomizationClick
        )

        DemoCard(
            title = "Custom Method Picker Layout & Terms",
            description = "Replace the default provider list with a custom layout, and swap the 'By continuing...' footer with a checkbox using customMethodPickerLayout and customMethodPickerTermsConfiguration on FirebaseAuthScreen.",
            onClick = onCustomMethodPickerClick
        )
    }
}

@Composable
private fun DemoCard(
    title: String,
    description: String,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(text = title, style = MaterialTheme.typography.titleMedium)
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
