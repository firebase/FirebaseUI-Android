package com.firebaseui.android.demo.auth.fullcustomization

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun HardOffsetShadow(
    shape: Shape,
    modifier: Modifier = Modifier,
    offsetX: Dp = 3.dp,
    offsetY: Dp = 6.dp,
    color: Color = MaterialTheme.colorScheme.primaryContainer,
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
