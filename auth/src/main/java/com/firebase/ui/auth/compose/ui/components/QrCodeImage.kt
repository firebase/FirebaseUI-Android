/*
 * Copyright 2025 Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.firebase.ui.auth.compose.ui.components

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.WriterException
import com.google.zxing.qrcode.QRCodeWriter

/**
 * Renders a QR code from the provided content string.
 *
 * This component is typically used to display TOTP enrollment URIs. The QR code is generated on the
 * fly and memoized for the given [content].
 *
 * @param content The string content to encode into the QR code (for example the TOTP URI).
 * @param modifier Optional [Modifier] applied to the QR container.
 * @param size The size of the QR code square in density-independent pixels.
 * @param foregroundColor Color used to render the QR pixels (defaults to black).
 * @param backgroundColor Background color for the QR code (defaults to white).
 */
@Composable
fun QrCodeImage(
    content: String,
    modifier: Modifier = Modifier,
    size: Dp = 250.dp,
    foregroundColor: Color = Color.Black,
    backgroundColor: Color = Color.White
) {
    val bitmap = remember(content, size, foregroundColor, backgroundColor) {
        generateQrCodeBitmap(
            content = content,
            sizePx = (size.value * 2).toInt(), // Render at 2x for better scaling quality.
            foregroundColor = foregroundColor,
            backgroundColor = backgroundColor
        )
    }

    Box(
        modifier = modifier
            .size(size)
            .background(backgroundColor),
        contentAlignment = Alignment.Center
    ) {
        bitmap?.let {
            Image(
                bitmap = it.asImageBitmap(),
                contentDescription = "QR code for authenticator app setup",
                modifier = Modifier.size(size)
            )
        }
    }
}

private fun generateQrCodeBitmap(
    content: String,
    sizePx: Int,
    foregroundColor: Color,
    backgroundColor: Color
): Bitmap? {
    return try {
        val qrCodeWriter = QRCodeWriter()
        val hints = mapOf(
            EncodeHintType.MARGIN to 1 // Small margin keeps QR code compact while remaining scannable.
        )

        val bitMatrix = qrCodeWriter.encode(
            content,
            BarcodeFormat.QR_CODE,
            sizePx,
            sizePx,
            hints
        )

        val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)

        val foregroundArgb = android.graphics.Color.argb(
            (foregroundColor.alpha * 255).toInt(),
            (foregroundColor.red * 255).toInt(),
            (foregroundColor.green * 255).toInt(),
            (foregroundColor.blue * 255).toInt()
        )

        val backgroundArgb = android.graphics.Color.argb(
            (backgroundColor.alpha * 255).toInt(),
            (backgroundColor.red * 255).toInt(),
            (backgroundColor.green * 255).toInt(),
            (backgroundColor.blue * 255).toInt()
        )

        for (x in 0 until sizePx) {
            for (y in 0 until sizePx) {
                bitmap.setPixel(
                    x,
                    y,
                    if (bitMatrix[x, y]) foregroundArgb else backgroundArgb
                )
            }
        }

        bitmap
    } catch (e: WriterException) {
        null
    }
}
