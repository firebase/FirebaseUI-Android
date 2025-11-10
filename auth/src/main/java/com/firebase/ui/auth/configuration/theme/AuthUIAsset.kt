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

package com.firebase.ui.auth.compose.configuration.theme

import androidx.annotation.DrawableRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.res.painterResource

/**
 * Represents a visual asset used in the authentication UI.
 *
 * This sealed class allows specifying icons and images from either Android drawable
 * resources ([Resource]) or Jetpack Compose [ImageVector]s ([Vector]). The [painter]
 * property provides a unified way to get a [Painter] for the asset within a composable.
 *
 * **Example usage:**
 * ```kotlin
 * // To use a drawable resource:
 * val asset = AuthUIAsset.Resource(R.drawable.my_logo)
 *
 * // To use a vector asset:
 * val vectorAsset = AuthUIAsset.Vector(Icons.Default.Info)
 * ```
 */
sealed class AuthUIAsset {
    /**
     * An asset loaded from a drawable resource.
     *
     * @param resId The resource ID of the drawable (e.g., `R.drawable.my_icon`).
     */
    class Resource(@param:DrawableRes val resId: Int) : AuthUIAsset()

    /**
     * An asset represented by an [ImageVector].
     *
     * @param image The [ImageVector] to be displayed.
     */
    class Vector(val image: ImageVector) : AuthUIAsset()

    /**
     * A [Painter] that can be used to draw this asset in a composable.
     *
     * This property automatically resolves the asset type and returns the appropriate
     * [Painter] for rendering.
     */
    @get:Composable
    internal val painter: Painter
        get() = when (this) {
            is Resource -> painterResource(resId)
            is Vector -> rememberVectorPainter(image)
        }
}