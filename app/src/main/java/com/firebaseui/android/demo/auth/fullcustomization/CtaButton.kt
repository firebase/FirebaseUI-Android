package com.firebaseui.android.demo.auth.fullcustomization

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

private val CtaShadowColor = Color(0xFF5D0B47)

@Composable
fun CtaButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isLoading: Boolean = false,
    colors: ButtonColors = ButtonDefaults.buttonColors(),
) {
    HardOffsetShadow(
        shape = ButtonShape,
        offsetX = 2.dp,
        offsetY = 4.dp,
        color = if (enabled) CtaShadowColor else Color.Transparent,
        modifier = modifier.fillMaxWidth(),
    ) {
        Button(
            onClick = onClick,
            enabled = enabled,
            shape = ButtonShape,
            colors = colors,
            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 10.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(80.dp),
        ) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp))
            } else {
                Text(text = text, style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}
