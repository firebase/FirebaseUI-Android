package com.firebaseui.android.demo.auth.fullcustomization.professional

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.Icon
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withLink
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.firebaseui.android.demo.auth.fullcustomization.theme.FullCustomizationThemeId
import com.firebaseui.android.demo.auth.fullcustomization.theme.LocalFullCustomizationStyle
import com.firebaseui.android.demo.auth.fullcustomization.theme.ProColors
import com.firebaseui.android.demo.auth.fullcustomization.theme.ProDarkColors
import com.firebaseui.android.demo.auth.fullcustomization.theme.ProLightColors

/** The configuration's Terms and Privacy URLs, for [ProLegalText]. */
@Immutable
data class LegalLinks(val tosUrl: String?, val privacyPolicyUrl: String?)

val LocalLegalLinks = staticCompositionLocalOf { LegalLinks(null, null) }

/** Whether the screens should take the professional layout rather than the shared one. */
@Composable
fun isProfessional(): Boolean =
    LocalFullCustomizationStyle.current.id == FullCustomizationThemeId.Professional

/** Professional's palette for the current mode; the theme picks dark the same way. */
@Composable
fun proColors(): ProColors = if (isSystemInDarkTheme()) ProDarkColors else ProLightColors

/**
 * The scaffold every professional screen shares: a scrolling, left-aligned column with a header,
 * the content and an optional footer. The footer sits at the bottom when the content is short and
 * scrolls with it otherwise. Inset 32dp and capped at 480dp, as the iOS demo is.
 */
@Composable
fun ProPage(
    modifier: Modifier = Modifier,
    footer: @Composable () -> Unit = {},
    header: @Composable ColumnScope.() -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = proColors()
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
            .safeDrawingPadding(),
    ) {
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .widthIn(max = 480.dp)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                // heightIn(min = maxHeight) pins the footer to the bottom when everything fits.
                .heightIn(min = maxHeight)
                // The top inset clears the floating theme switcher.
                .padding(start = 32.dp, end = 32.dp, top = 88.dp, bottom = 16.dp),
        ) {
            Column(content = header)
            Column(
                modifier = modifier
                    .fillMaxWidth()
                    .padding(top = 28.dp),
                content = content,
            )
            Spacer(modifier = Modifier.weight(1f))
            Spacer(modifier = Modifier.height(32.dp))
            footer()
        }
    }
}

/** The product mark and name, above the landing's title. */
@Composable
fun ProBrand() {
    val colors = proColors()
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        ProIconTile(icon = Icons.Outlined.Shield, size = 32.dp, fill = colors.accentFill, tint = Color.White)
        Text(text = "FirebaseUI Demo", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = colors.text)
    }
}

/** A title and a one-line explanation, under an optional icon tile. */
@Composable
fun ProHeader(title: String, subtitle: String? = null, icon: ImageVector? = null) {
    val colors = proColors()
    Column {
        icon?.let {
            ProIconTile(icon = it, size = 44.dp)
            Spacer(modifier = Modifier.height(20.dp))
        }
        Text(
            text = title,
            fontSize = 26.sp,
            lineHeight = 32.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = (-0.3).sp,
            color = colors.text,
            modifier = Modifier.semantics { heading() },
        )
        subtitle?.let {
            Spacer(modifier = Modifier.height(6.dp))
            Text(text = it, fontSize = 15.sp, lineHeight = 21.sp, color = colors.muted)
        }
    }
}

/** An icon on a soft accent square. */
@Composable
fun ProIconTile(
    icon: ImageVector,
    size: Dp = 36.dp,
    fill: Color = proColors().accentSoft,
    tint: Color = proColors().accentSoftText,
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(size)
            .background(fill, RoundedCornerShape(size * 0.28f)),
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = tint, modifier = Modifier.size(size * 0.5f))
    }
}

/** "‹ Back", for screens pushed over another (iOS gets this from its navigation bar). */
@Composable
fun ProBackButton(onBack: () -> Unit, enabled: Boolean = true) {
    val colors = proColors()
    TextButton(
        onClick = onBack,
        enabled = enabled,
        contentPadding = PaddingValues(start = 0.dp, end = 12.dp),
        modifier = Modifier
            .padding(bottom = 12.dp)
            .semantics { contentDescription = "button - back" },
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
            contentDescription = null,
            tint = colors.accent,
            modifier = Modifier.size(22.dp),
        )
        Text(text = "Back", fontSize = 15.sp, fontWeight = FontWeight.Medium, color = colors.accent)
    }
}

/** A full-width outlined button, for the sign-in providers. */
@Composable
fun ProOutlineButton(
    title: String,
    onClick: () -> Unit,
    contentDescription: String,
    enabled: Boolean = true,
    /** The provider mark or icon, drawn in a 20dp box before the title. */
    content: @Composable () -> Unit,
) {
    val colors = proColors()
    Surface(
        onClick = onClick,
        enabled = enabled,
        color = colors.surface,
        contentColor = colors.text,
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, colors.border),
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .semantics { this.contentDescription = contentDescription },
    ) {
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.heightIn(min = 48.dp),
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(20.dp)) { content() }
            Spacer(modifier = Modifier.width(10.dp))
            Text(text = title, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

/** A provider mark sized for [ProOutlineButton]; Apple's white mark is inked for the outlined button. */
@Composable
fun ProProviderMark(painter: Painter, tintTo: Color? = null) {
    Image(
        painter = painter,
        contentDescription = null,
        colorFilter = tintTo?.let { androidx.compose.ui.graphics.ColorFilter.tint(it) },
        modifier = Modifier.size(20.dp),
    )
}

/** A text-only action in the accent colour, 44dp tall. */
@Composable
fun ProTextButton(
    title: String,
    onClick: () -> Unit,
    contentDescription: String,
    enabled: Boolean = true,
    alignment: Alignment = Alignment.Center,
) {
    val colors = proColors()
    Box(
        contentAlignment = alignment,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 44.dp)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .semantics { this.contentDescription = contentDescription },
    ) {
        Text(
            text = title,
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
            color = if (enabled) colors.accent else colors.accent.copy(alpha = 0.5f),
        )
    }
}

/**
 * "Prompt? Action", centred, with only the action tappable. One row of two texts at the same size,
 * so both sit on one baseline (a TextButton next to a Text would not).
 */
@Composable
fun ProPromptLink(prompt: String, action: String, onClick: () -> Unit, contentDescription: String) {
    val colors = proColors()
    Row(
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(text = prompt, fontSize = 15.sp, color = colors.muted)
        Spacer(modifier = Modifier.width(4.dp))
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .heightIn(min = 44.dp)
                .clickable(role = Role.Button, onClick = onClick)
                .semantics { this.contentDescription = contentDescription },
        ) {
            Text(text = action, fontSize = 15.sp, fontWeight = FontWeight.Medium, color = colors.accent)
        }
    }
}

/** A rule with a short label in the middle. */
@Composable
fun ProDivider(label: String) {
    val colors = proColors()
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Box(modifier = Modifier.weight(1f).height(1.dp).background(colors.hairline))
        Text(text = label, fontSize = 13.sp, color = colors.muted)
        Box(modifier = Modifier.weight(1f).height(1.dp).background(colors.hairline))
    }
}

/** A content slot's error message, if it has one. */
@Composable
fun ProErrorText(message: String?) {
    message ?: return
    val colors = proColors()
    Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.Top,
        modifier = Modifier.semantics(mergeDescendants = true) { contentDescription = "error-message" },
    ) {
        Icon(
            imageVector = Icons.Outlined.ErrorOutline,
            contentDescription = null,
            tint = colors.danger,
            modifier = Modifier.size(18.dp),
        )
        Text(text = message, fontSize = 14.sp, color = colors.danger)
    }
}

/** A small uppercase label above a [ProGroup]. */
@Composable
fun ProSectionLabel(text: String) {
    Text(
        text = text.uppercase(),
        fontSize = 13.sp,
        fontWeight = FontWeight.Medium,
        letterSpacing = 0.3.sp,
        color = proColors().muted,
        modifier = Modifier.padding(start = 4.dp, end = 4.dp, bottom = 8.dp),
    )
}

/** Grouped rows on a bordered card, like a settings section. Separate rows with [ProRowSeparator]. */
@Composable
fun ProGroup(content: @Composable ColumnScope.() -> Unit) {
    val colors = proColors()
    val shape = RoundedCornerShape(12.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(colors.surface)
            .border(1.dp, colors.hairline, shape),
        content = content,
    )
}

/** The hairline between rows, inset past a row's icon. */
@Composable
fun ProRowSeparator(inset: Dp = 48.dp) {
    Box(
        modifier = Modifier
            .padding(start = inset)
            .fillMaxWidth()
            .height(1.dp)
            .background(proColors().hairline),
    )
}

/** One settings row: an optional icon, a title, an optional value and a chevron. */
@Composable
fun ProRow(
    title: String,
    contentDescription: String,
    onClick: (() -> Unit)?,
    icon: ImageVector? = null,
    value: String? = null,
    valueTint: Color = proColors().muted,
    tint: Color = proColors().text,
    showsChevron: Boolean = true,
    enabled: Boolean = true,
) {
    val colors = proColors()
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (onClick != null) {
                    Modifier.clickable(enabled = enabled, role = Role.Button, onClick = onClick)
                } else {
                    Modifier
                },
            )
            .semantics(mergeDescendants = true) { this.contentDescription = contentDescription }
            .heightIn(min = 52.dp)
            .padding(horizontal = 16.dp),
    ) {
        icon?.let { Icon(imageVector = it, contentDescription = null, tint = tint, modifier = Modifier.size(20.dp)) }
        Text(
            text = title,
            fontSize = 16.sp,
            fontWeight = if (icon == null) FontWeight.Medium else FontWeight.Normal,
            color = tint,
            modifier = Modifier.weight(1f),
        )
        value?.let { Text(text = it, fontSize = 15.sp, color = valueTint) }
        if (showsChevron) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = colors.chevron,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

/** A selectable row in a [ProGroup]: icon tile, title, description and a radio mark. */
@Composable
fun ProChoiceRow(
    icon: ImageVector,
    title: String,
    detail: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    contentDescription: String,
) {
    val colors = proColors()
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(role = Role.RadioButton, onClick = onClick)
            .semantics(mergeDescendants = true) { this.contentDescription = contentDescription }
            .padding(horizontal = 16.dp, vertical = 14.dp),
    ) {
        ProIconTile(
            icon = icon,
            fill = if (isSelected) colors.accentSoft else colors.tile,
            tint = if (isSelected) colors.accentSoftText else colors.text,
        )
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(text = title, fontSize = 16.sp, fontWeight = FontWeight.Medium, color = colors.text)
            Text(text = detail, fontSize = 14.sp, color = colors.muted)
        }
        RadioButton(
            selected = isSelected,
            onClick = null,
            colors = RadioButtonDefaults.colors(selectedColor = colors.accent, unselectedColor = colors.border),
        )
    }
}

/** The terms line under the screens that sign in or create an account, from the configured URLs. */
@Composable
fun ProLegalText(prefix: String = "By continuing, you agree to the") {
    val links = LocalLegalLinks.current
    val tos = links.tosUrl ?: return
    val privacy = links.privacyPolicyUrl ?: return
    val colors = proColors()
    val linkStyle = TextLinkStyles(SpanStyle(color = colors.accent))
    Text(
        text = buildAnnotatedString {
            append("$prefix ")
            withLink(LinkAnnotation.Url(tos, linkStyle)) { append("Terms of Service") }
            append(" and ")
            withLink(LinkAnnotation.Url(privacy, linkStyle)) { append("Privacy Policy") }
            append(".")
        },
        fontSize = 13.sp,
        lineHeight = 18.sp,
        color = colors.muted,
        textAlign = TextAlign.Center,
        modifier = Modifier
            .fillMaxWidth()
            .semantics { contentDescription = "terms" },
    )
}

/** The identity's first letter on a soft accent circle, or the person glyph when it has none. */
@Composable
fun ProAvatar(identity: String) {
    val letter = identity.firstOrNull { it.isLetter() }?.uppercase()
    val colors = proColors()
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(56.dp)
            .background(colors.accentSoft, CircleShape),
    ) {
        if (letter != null) {
            Text(text = letter, fontSize = 22.sp, fontWeight = FontWeight.SemiBold, color = colors.accentSoftText)
        } else {
            Icon(imageVector = Icons.Outlined.Person, contentDescription = null, tint = colors.accentSoftText)
        }
    }
}
