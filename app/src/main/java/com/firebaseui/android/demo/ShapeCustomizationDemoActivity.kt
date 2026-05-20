package com.firebaseui.android.demo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.firebase.ui.auth.configuration.auth_provider.AuthProvider
import com.firebase.ui.auth.configuration.string_provider.DefaultAuthUIStringProvider
import com.firebase.ui.auth.configuration.theme.AuthUITheme
import com.firebase.ui.auth.configuration.theme.ProviderStyleDefaults
import com.firebase.ui.auth.ui.components.AuthProviderButton

class ShapeCustomizationDemoActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .systemBarsPadding()
                    ) {
                        ShapeCustomizationDemo()
                    }
                }
            }
        }
    }
}

@Composable
fun ShapeCustomizationDemo() {
    val context = LocalContext.current
    val stringProvider = DefaultAuthUIStringProvider(context)
    var selectedPreset by remember { mutableStateOf(ShapePreset.DEFAULT) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Provider Button Shape Customization",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.primary
        )

        Text(
            text = "Showcases the shape customization API for provider buttons. " +
                    "Set a global shape for all buttons or customize individual providers.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        HorizontalDivider()

        Text(text = "Select Shape Preset:", style = MaterialTheme.typography.titleMedium)

        ShapePreset.entries.forEach { preset ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(
                    selected = selectedPreset == preset,
                    onClick = { selectedPreset = preset }
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(text = preset.displayName, style = MaterialTheme.typography.bodyLarge)
                    Text(
                        text = preset.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        HorizontalDivider()

        Text(text = "Preview:", style = MaterialTheme.typography.titleMedium)

        when (selectedPreset) {
            ShapePreset.DEFAULT -> DefaultShapeButtons(stringProvider)
            ShapePreset.DEFAULT_COPY -> DefaultCopyShapeButtons(stringProvider)
            ShapePreset.DARK_COPY -> DarkCopyShapeButtons(stringProvider)
            ShapePreset.FROM_MATERIAL -> FromMaterialThemeButtons(stringProvider)
            ShapePreset.PILL -> PillShapeButtons(stringProvider)
            ShapePreset.MIXED -> MixedShapeButtons(stringProvider)
        }

        HorizontalDivider()

        Text(text = "Code Example:", style = MaterialTheme.typography.titleMedium)

        androidx.compose.material3.Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surfaceVariant,
            shape = RoundedCornerShape(8.dp)
        ) {
            Text(
                text = selectedPreset.codeExample,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                ),
                modifier = Modifier.padding(12.dp)
            )
        }
    }
}

enum class ShapePreset(
    val displayName: String,
    val description: String,
    val codeExample: String
) {
    DEFAULT(
        "Default Shapes",
        "Uses the standard 4dp rounded corners",
        "// No customization needed\nval theme = AuthUITheme.Default"
    ),
    DEFAULT_COPY(
        "Default.copy()",
        "Customize default light theme with .copy()",
        "val theme = AuthUITheme.Default.copy(\n    providerButtonShape = RoundedCornerShape(12.dp)\n)"
    ),
    DARK_COPY(
        "DefaultDark.copy()",
        "Customize default dark theme with .copy()",
        "val theme = AuthUITheme.DefaultDark.copy(\n    providerButtonShape = RoundedCornerShape(16.dp)\n)"
    ),
    FROM_MATERIAL(
        "fromMaterialTheme()",
        "Inherit from Material Theme",
        "val theme = AuthUITheme.fromMaterialTheme(\n    providerButtonShape = RoundedCornerShape(12.dp)\n)"
    ),
    PILL(
        "Pill Shape",
        "Creates pill-shaped buttons (Default.copy)",
        "val theme = AuthUITheme.Default.copy(\n    providerButtonShape = RoundedCornerShape(28.dp)\n)"
    ),
    MIXED(
        "Mixed Shapes",
        "Different shapes per provider (Default.copy)",
        "val customStyles = mapOf(\n    \"google.com\" to ProviderStyleDefaults.Google.copy(\n        shape = RoundedCornerShape(24.dp)\n    ),\n    \"facebook.com\" to ProviderStyleDefaults.Facebook.copy(\n        shape = RoundedCornerShape(8.dp)\n    )\n)\n\nval theme = AuthUITheme.Default.copy(\n    providerButtonShape = RoundedCornerShape(12.dp),\n    providerStyles = customStyles\n)"
    )
}

@Composable
fun DefaultShapeButtons(stringProvider: DefaultAuthUIStringProvider) {
    AuthUITheme { ButtonPreviewColumn(stringProvider) }
}

@Composable
fun DefaultCopyShapeButtons(stringProvider: DefaultAuthUIStringProvider) {
    AuthUITheme(theme = AuthUITheme.Default.copy(providerButtonShape = RoundedCornerShape(12.dp))) {
        ButtonPreviewColumn(stringProvider)
    }
}

@Composable
fun DarkCopyShapeButtons(stringProvider: DefaultAuthUIStringProvider) {
    AuthUITheme(theme = AuthUITheme.DefaultDark.copy(providerButtonShape = RoundedCornerShape(16.dp))) {
        ButtonPreviewColumn(stringProvider)
    }
}

@Composable
fun FromMaterialThemeButtons(stringProvider: DefaultAuthUIStringProvider) {
    AuthUITheme(theme = AuthUITheme.fromMaterialTheme(providerButtonShape = RoundedCornerShape(12.dp))) {
        ButtonPreviewColumn(stringProvider)
    }
}

@Composable
fun PillShapeButtons(stringProvider: DefaultAuthUIStringProvider) {
    AuthUITheme(theme = AuthUITheme.Default.copy(providerButtonShape = RoundedCornerShape(28.dp))) {
        ButtonPreviewColumn(stringProvider)
    }
}

@Composable
fun MixedShapeButtons(stringProvider: DefaultAuthUIStringProvider) {
    val customStyles = mapOf(
        "google.com" to ProviderStyleDefaults.Google.copy(shape = RoundedCornerShape(24.dp)),
        "facebook.com" to ProviderStyleDefaults.Facebook.copy(shape = RoundedCornerShape(8.dp))
    )
    AuthUITheme(
        theme = AuthUITheme.Default.copy(
            providerButtonShape = RoundedCornerShape(12.dp),
            providerStyles = customStyles
        )
    ) {
        ButtonPreviewColumn(stringProvider)
    }
}

@Composable
fun ButtonPreviewColumn(stringProvider: DefaultAuthUIStringProvider) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        AuthProviderButton(
            provider = AuthProvider.Google(scopes = emptyList(), serverClientId = null),
            onClick = { },
            stringProvider = stringProvider,
            modifier = Modifier.fillMaxWidth()
        )
        AuthProviderButton(
            provider = AuthProvider.Facebook(),
            onClick = { },
            stringProvider = stringProvider,
            modifier = Modifier.fillMaxWidth()
        )
        AuthProviderButton(
            provider = AuthProvider.Email(
                emailLinkActionCodeSettings = null,
                passwordValidationRules = emptyList()
            ),
            onClick = { },
            stringProvider = stringProvider,
            modifier = Modifier.fillMaxWidth()
        )
    }
}
