package com.firebaseui.android.demo.auth.fullcustomization.common

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.firebaseui.android.demo.auth.fullcustomization.theme.LocalFullCustomizationStyle

/** The theme's main call-to-action colours. */
@Composable
fun primaryCtaColors(): ButtonColors = LocalFullCustomizationStyle.current.primaryButton.let {
    ButtonDefaults.buttonColors(containerColor = it.container, contentColor = it.content)
}

/** The theme's colours for a call to action that reads as an alternative to the main one. */
@Composable
fun secondaryCtaColors(): ButtonColors = LocalFullCustomizationStyle.current.secondaryButton.let {
    ButtonDefaults.buttonColors(containerColor = it.container, contentColor = it.content)
}

@Composable
fun CtaButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isLoading: Boolean = false,
    colors: ButtonColors = primaryCtaColors(),
) {
    val style = LocalFullCustomizationStyle.current
    val shadow = style.buttonShadow
    HardOffsetShadow(
        shape = style.buttonShape,
        offsetX = shadow?.offset?.x ?: 0.dp,
        offsetY = shadow?.offset?.y ?: 0.dp,
        color = if (enabled && shadow != null) shadow.color else Color.Transparent,
        modifier = modifier.fillMaxWidth(),
    ) {
        Button(
            onClick = onClick,
            enabled = enabled,
            shape = style.buttonShape,
            colors = colors,
            border = style.buttonBorder,
            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 10.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(style.buttonHeight),
        ) {
            if (isLoading) {
                // M3 default (primary) reads on the disabled fill; LocalContentColor would wash it out.
                CircularProgressIndicator(modifier = Modifier.size(20.dp))
            } else {
                Text(text = text, style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}
