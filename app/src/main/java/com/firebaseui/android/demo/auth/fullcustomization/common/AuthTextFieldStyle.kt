package com.firebaseui.android.demo.auth.fullcustomization.common

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.firebaseui.android.demo.R
import com.firebaseui.android.demo.auth.fullcustomization.theme.FullCustomizationThemeId
import com.firebaseui.android.demo.auth.fullcustomization.theme.LocalFullCustomizationStyle

val AuthFieldShape = RoundedCornerShape(24.dp)

@Composable
fun authTextFieldColors(): TextFieldColors = OutlinedTextFieldDefaults.colors(
    unfocusedContainerColor = LocalFullCustomizationStyle.current.fieldContainer,
    focusedContainerColor = LocalFullCustomizationStyle.current.fieldContainer,
    disabledContainerColor = LocalFullCustomizationStyle.current.fieldContainer,
    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
    focusedBorderColor = MaterialTheme.colorScheme.secondary,
)

/**
 * An outlined text field in the theme's field shape. Built on [BasicTextField] rather than
 * `OutlinedTextField` because only the decoration box takes an unfocused border width, and Grid and
 * Coffee Spark draw theirs at 2dp.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FullCustomizationTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
    placeholder: String? = null,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    enabled: Boolean = true,
    isError: Boolean = false,
    supportingText: String? = null,
    singleLine: Boolean = true,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    shape: Shape = LocalFullCustomizationStyle.current.fieldShape,
    /** Professional only: an action at the end of the label row, such as "Forgot password?". */
    labelAction: (@Composable () -> Unit)? = null,
) {
    val colors = authTextFieldColors()
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val textColor = when {
        !enabled -> colors.disabledTextColor
        isError -> colors.errorTextColor
        isFocused -> colors.focusedTextColor
        else -> colors.unfocusedTextColor
    }
    // Professional sets the label above a 48dp field, as a plain form does; the others float it.
    val compact = LocalFullCustomizationStyle.current.id == FullCustomizationThemeId.Professional
    val labelAbove = compact && label != null
    val labelSlot: (@Composable () -> Unit)? = label?.takeUnless { labelAbove }?.let { { Text(it) } }
    val focusRequester = remember { FocusRequester() }
    val fieldModifier = when {
        labelAbove -> Modifier.fillMaxWidth().focusRequester(focusRequester)
        // OutlinedTextField's top inset, so a floating label isn't clipped by the field above.
        label != null -> modifier.semantics(mergeDescendants = true) {}.padding(top = 8.dp)
        else -> modifier
    }

    val field: @Composable () -> Unit = { BasicTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = fieldModifier.defaultMinSize(
            minWidth = OutlinedTextFieldDefaults.MinWidth,
            minHeight = if (compact) 48.dp else OutlinedTextFieldDefaults.MinHeight,
        ),
        enabled = enabled,
        textStyle = LocalTextStyle.current.merge(color = textColor),
        keyboardOptions = keyboardOptions,
        singleLine = singleLine,
        maxLines = if (singleLine) 1 else Int.MAX_VALUE,
        visualTransformation = visualTransformation,
        interactionSource = interactionSource,
        cursorBrush = SolidColor(if (isError) colors.errorCursorColor else colors.cursorColor),
        decorationBox = { innerTextField ->
            OutlinedTextFieldDefaults.DecorationBox(
                value = value,
                innerTextField = innerTextField,
                enabled = enabled,
                singleLine = singleLine,
                visualTransformation = visualTransformation,
                interactionSource = interactionSource,
                isError = isError,
                label = labelSlot,
                placeholder = placeholder?.let { { Text(it) } },
                leadingIcon = leadingIcon,
                trailingIcon = trailingIcon,
                supportingText = supportingText?.let { { Text(it) } },
                colors = colors,
                contentPadding = if (compact) {
                    OutlinedTextFieldDefaults.contentPadding(start = 14.dp, top = 12.dp, end = 14.dp, bottom = 12.dp)
                } else {
                    OutlinedTextFieldDefaults.contentPadding()
                },
                container = {
                    OutlinedTextFieldDefaults.Container(
                        enabled = enabled,
                        isError = isError,
                        interactionSource = interactionSource,
                        colors = colors,
                        shape = shape,
                        // 1dp heavier than unfocused, so focus still shows where both colours match.
                        focusedBorderThickness = LocalFullCustomizationStyle.current.fieldBorderWidth + 1.dp,
                        unfocusedBorderThickness = LocalFullCustomizationStyle.current.fieldBorderWidth,
                    )
                },
            )
        },
    ) }

    if (labelAbove) {
        // The whole block focuses the field, as a form label does, so a tap on the label lands.
        Column(
            verticalArrangement = Arrangement.spacedBy(6.dp),
            modifier = modifier
                .semantics(mergeDescendants = true) {}
                .clickable(
                    enabled = enabled,
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                ) { focusRequester.requestFocus() },
        ) {
            // On the text baseline, so a label action set at another size still lines up.
            Row {
                Text(
                    text = label.orEmpty(),
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier
                        .weight(1f)
                        .alignByBaseline(),
                )
                labelAction?.let { action -> Box(modifier = Modifier.alignByBaseline()) { action() } }
            }
            field()
        }
    } else {
        field()
    }
}

@Composable
fun EmailFieldIcon() {
    Image(
        painter = painterResource(R.drawable.email_at_sign),
        contentDescription = null,
        // The art is Doggo's; the other themes recolour it to their field text.
        colorFilter = if (LocalFullCustomizationStyle.current.showsMascot) {
            null
        } else {
            ColorFilter.tint(MaterialTheme.colorScheme.onSurfaceVariant)
        },
        modifier = Modifier.size(24.dp),
    )
}
