package com.firebaseui.android.demo.auth.fullcustomization.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.firebaseui.android.demo.auth.fullcustomization.theme.LocalFullCustomizationStyle

@Composable
fun HardOffsetShadow(
    shape: Shape,
    modifier: Modifier = Modifier,
    // Defaults to the theme's card shadow, which Grid and Coffee Spark don't draw.
    offsetX: Dp = LocalFullCustomizationStyle.current.cardShadow?.offset?.x ?: 0.dp,
    offsetY: Dp = LocalFullCustomizationStyle.current.cardShadow?.offset?.y ?: 0.dp,
    color: Color = LocalFullCustomizationStyle.current.cardShadow?.color ?: Color.Transparent,
    content: @Composable () -> Unit,
) {
    Box(modifier = modifier) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .offset(x = offsetX, y = offsetY)
                .background(color = color, shape = shape),
        )
        content()
    }
}
