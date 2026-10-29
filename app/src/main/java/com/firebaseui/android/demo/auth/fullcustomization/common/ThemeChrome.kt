package com.firebaseui.android.demo.auth.fullcustomization.common

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Coffee
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.firebaseui.android.demo.R
import com.firebaseui.android.demo.auth.fullcustomization.theme.FullCustomizationThemeId
import com.firebaseui.android.demo.auth.fullcustomization.theme.gridPattern
import com.firebaseui.android.demo.auth.fullcustomization.theme.JetBrainsMono
import com.firebaseui.android.demo.auth.fullcustomization.theme.LocalFullCustomizationStyle

/**
 * The theme's page background, edge to edge. Grid and Coffee Spark place their art at the iOS
 * frames' offsets from the top-left corner: "Frame 5" in 20:5467 and "avatar spark" in 20:5769.
 */
@Composable
fun ThemeBackground() {
    val style = LocalFullCustomizationStyle.current
    val page = MaterialTheme.colorScheme.background
    when (style.id) {
        FullCustomizationThemeId.Doggo -> Image(
            painter = painterResource(id = R.drawable.custom_background),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
        FullCustomizationThemeId.Grid -> PlacedArt(
            art = rememberVectorPainter(remember(page) { gridPattern(page) }),
            x = (-269).dp, y = (-75).dp, width = 997.dp, height = 997.dp,
        )
        // The export includes the layer's texture and noise effects, so it is larger than the
        // 583x568 layer and centred on it.
        FullCustomizationThemeId.Spark -> PlacedArt(
            art = painterResource(id = R.drawable.full_customization_spark_blob),
            x = (-295.8).dp, y = (-317.7).dp, width = 751.dp, height = 735.3.dp,
        )
        FullCustomizationThemeId.Professional ->
            Box(modifier = Modifier.fillMaxSize().background(page))
    }
    // The art is drawn for a light page; dark mode washes it back toward the dark one.
    Box(modifier = Modifier.fillMaxSize().background(style.artScrim))
}

@Composable
private fun PlacedArt(art: Painter, x: Dp, y: Dp, width: Dp, height: Dp) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .clipToBounds(),
    ) {
        Image(
            painter = art,
            contentDescription = null,
            contentScale = ContentScale.FillBounds,
            // requiredSize so art larger than the screen keeps its size instead of being squeezed.
            modifier = Modifier
                .wrapContentSize(align = Alignment.TopStart, unbounded = true)
                .offset(x = x, y = y)
                .requiredSize(width, height),
        )
    }
}

/**
 * A screen's heading: Doggo's [mascot] above the title, Coffee Spark's headline pill above it, and
 * the title alone on Grid.
 */
@Composable
fun ThemeHeader(
    title: String,
    @DrawableRes mascot: Int,
    mascotDescription: String,
    mascotSize: Dp = 72.dp,
) {
    val style = LocalFullCustomizationStyle.current
    when (style.id) {
        FullCustomizationThemeId.Doggo -> {
            Image(
                painter = painterResource(id = mascot),
                contentDescription = mascotDescription,
                modifier = Modifier.size(mascotSize),
            )
            Spacer(modifier = Modifier.height(8.dp))
        }
        FullCustomizationThemeId.Spark -> {
            SparkHeadline()
            Spacer(modifier = Modifier.height(24.dp))
        }
        FullCustomizationThemeId.Grid, FullCustomizationThemeId.Professional -> Unit
    }
    Text(
        text = title,
        style = MaterialTheme.typography.headlineSmall,
        color = style.titleColor,
    )
}

/** Figma "headline" (20:5927), Coffee Spark only. */
@Composable
fun SparkHeadline(modifier: Modifier = Modifier) {
    val pill = LocalFullCustomizationStyle.current.pill
    Surface(
        color = pill.container,
        contentColor = pill.content,
        shape = RoundedCornerShape(24.dp),
        modifier = modifier
            .height(48.dp)
            .semantics(mergeDescendants = true) { contentDescription = "headline - Coffee Spark" },
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 16.dp),
        ) {
            Icon(imageVector = Icons.Outlined.Coffee, contentDescription = null)
            Text(
                text = "Coffee Spark",
                style = TextStyle(fontFamily = JetBrainsMono, fontSize = 20.sp),
            )
        }
    }
}

/** A field-like outline: the theme's field shape and border width in `outlineVariant`. */
@Composable
fun Modifier.themeFieldBorder(): Modifier {
    val style = LocalFullCustomizationStyle.current
    return border(
        width = style.fieldBorderWidth,
        color = MaterialTheme.colorScheme.outlineVariant,
        shape = style.fieldShape,
    )
}

/** Switches the demo between its themes live, on top of whatever screen is showing. */
@Composable
fun ThemeSwitcher(
    selected: FullCustomizationThemeId,
    onSelect: (FullCustomizationThemeId) -> Unit,
    modifier: Modifier = Modifier,
) {
    val pill = LocalFullCustomizationStyle.current.pill
    Surface(
        color = pill.container,
        contentColor = pill.content,
        shape = RoundedCornerShape(percent = 50),
        shadowElevation = 2.dp,
        modifier = modifier,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier
                .padding(4.dp)
                .selectableGroup()
                .semantics { contentDescription = "theme-switcher" },
        ) {
            Icon(
                imageVector = Icons.Outlined.Palette,
                contentDescription = null,
                modifier = Modifier
                    .padding(start = 8.dp, end = 4.dp)
                    .size(18.dp),
            )
            FullCustomizationThemeId.entries.forEach { theme ->
                val isSelected = theme == selected
                Text(
                    text = theme.label,
                    style = MaterialTheme.typography.labelLarge,
                    color = if (isSelected) pill.container else pill.content,
                    modifier = Modifier
                        .background(
                            color = if (isSelected) pill.content else Color.Transparent,
                            shape = RoundedCornerShape(percent = 50),
                        )
                        .selectable(
                            selected = isSelected,
                            role = Role.RadioButton,
                            onClick = { onSelect(theme) },
                        )
                        .padding(horizontal = 12.dp, vertical = 14.dp),
                )
            }
        }
    }
}
