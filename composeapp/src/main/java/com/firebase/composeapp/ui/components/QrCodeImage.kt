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

package com.firebase.composeapp.ui.components

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
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
 * Composable that displays a QR code image generated from the provided content.
 *
 * @param content The string content to encode in the QR code (e.g., TOTP URI)
 * @param modifier Modifier to be applied to the QR code image
 * @param size The size (width and height) of the QR code image
 * @param foregroundColor The color of the QR code pixels (default: black)
 * @param backgroundColor The background color of the QR code (default: white)
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
            sizePx = (size.value * 2).toInt(), // 2x for better resolution
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
                contentDescription = "QR Code for $content",
                modifier = Modifier.size(size)
            )
        }
    }
}

/**
 * Generates a QR code bitmap from the provided content.
 *
 * @param content The string to encode
 * @param sizePx The size of the bitmap in pixels
 * @param foregroundColor The color for the QR code pixels
 * @param backgroundColor The background color
 * @return A Bitmap containing the QR code, or null if generation fails
 */
private fun generateQrCodeBitmap(
    content: String,
    sizePx: Int,
    foregroundColor: Color,
    backgroundColor: Color
): Bitmap? {
    return try {
        val qrCodeWriter = QRCodeWriter()
        val hints = mapOf(
            EncodeHintType.MARGIN to 1 // Minimal margin
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
        e.printStackTrace()
        null
    }
}
