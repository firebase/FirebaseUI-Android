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

@Composable
fun CtaButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isLoading: Boolean = false,
    colors: ButtonColors = ButtonDefaults.buttonColors(
        containerColor = LocalFullCustomizationStyle.current.primaryButton.container,
        contentColor = LocalFullCustomizationStyle.current.primaryButton.content,
    ),
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
                .height(80.dp),
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
