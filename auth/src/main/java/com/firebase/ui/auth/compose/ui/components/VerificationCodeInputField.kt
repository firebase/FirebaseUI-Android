package com.firebase.ui.auth.compose.ui.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.draw.clip
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.text.isDigitsOnly
import com.firebase.ui.auth.compose.configuration.theme.AuthUITheme
import com.firebase.ui.auth.compose.configuration.validators.FieldValidator

@Composable
fun VerificationCodeInputField(
    modifier: Modifier = Modifier,
    codeLength: Int = 6,
    validator: FieldValidator? = null,
    isError: Boolean = false,
    errorMessage: String? = null,
    onCodeComplete: (String) -> Unit = {},
    onCodeChange: (String) -> Unit = {},
) {
    val code = remember { mutableStateOf(List<Int?>(codeLength) { null }) }
    val focusedIndex = remember { mutableStateOf<Int?>(null) }
    val focusRequesters = remember { (1..codeLength).map { FocusRequester() } }
    val keyboardManager = LocalSoftwareKeyboardController.current

    // Derive validation state
    val currentCodeString = remember { mutableStateOf("") }
    val validationError = remember { mutableStateOf<String?>(null) }

    // Auto-focus first field on initial composition
    LaunchedEffect(Unit) {
        focusRequesters.firstOrNull()?.requestFocus()
    }

    // Handle focus changes
    LaunchedEffect(focusedIndex.value) {
        focusedIndex.value?.let { index ->
            focusRequesters.getOrNull(index)?.requestFocus()
        }
    }

    // Handle code completion and validation
    LaunchedEffect(code.value) {
        val codeString = code.value.mapNotNull { it }.joinToString("")
        currentCodeString.value = codeString
        onCodeChange(codeString)

        // Run validation if validator is provided
        validator?.let {
            val isValid = it.validate(codeString)
            validationError.value = if (!isValid && codeString.length == codeLength) {
                it.errorMessage
            } else {
                null
            }
        }

        val allNumbersEntered = code.value.none { it == null }
        if (allNumbersEntered) {
            keyboardManager?.hide()
            onCodeComplete(codeString)
        }
    }

    // Determine error state: use validator if provided, otherwise use explicit isError
    val showError = if (validator != null) {
        validationError.value != null
    } else {
        isError
    }

    val displayErrorMessage = if (validator != null) {
        validationError.value
    } else {
        errorMessage
    }

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)
        ) {
            code.value.forEachIndexed { index, number ->
                SingleDigitField(
                    modifier = Modifier
                        .weight(1f)
                        .aspectRatio(1f),
                    number = number,
                    isError = showError,
                    focusRequester = focusRequesters[index],
                    onFocusChanged = { isFocused ->
                        if (isFocused) {
                            focusedIndex.value = index
                        }
                    },
                    onNumberChanged = { value ->
                        val oldValue = code.value[index]
                        val newCode = code.value.toMutableList()
                        newCode[index] = value
                        code.value = newCode

                        // Move focus to next field if number was entered (and field was previously empty)
                        if (value != null && oldValue == null) {
                            focusedIndex.value = getNextFocusedIndex(newCode, index)
                        }
                    },
                    onKeyboardBack = {
                        val previousIndex = getPreviousFocusedIndex(index)
                        if (previousIndex != null) {
                            val newCode = code.value.toMutableList()
                            newCode[previousIndex] = null
                            code.value = newCode
                            focusedIndex.value = previousIndex
                        }
                    },
                    onNumberEntered = {
                        focusRequesters[index].freeFocus()
                    }
                )
            }
        }

        if (showError && displayErrorMessage != null) {
            Text(
                modifier = Modifier.padding(top = 8.dp),
                text = displayErrorMessage,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

@Composable
private fun SingleDigitField(
    modifier: Modifier = Modifier,
    number: Int?,
    isError: Boolean = false,
    focusRequester: FocusRequester,
    onFocusChanged: (Boolean) -> Unit,
    onNumberChanged: (Int?) -> Unit,
    onKeyboardBack: () -> Unit,
    onNumberEntered: () -> Unit,
) {
    val text = remember { mutableStateOf(TextFieldValue()) }
    val isFocused = remember { mutableStateOf(false) }

    // Update text field value when number changes externally
    LaunchedEffect(number) {
        text.value = TextFieldValue(
            text = number?.toString().orEmpty(),
            selection = TextRange(
                index = if (number != null) 1 else 0
            )
        )
    }

    val borderColor = if (isError) {
        MaterialTheme.colorScheme.error
    } else {
        MaterialTheme.colorScheme.primary
    }

//    val backgroundColor = if (isError) {
//        MaterialTheme.colorScheme.errorContainer
//    } else {
//        MaterialTheme.colorScheme.primaryContainer
//    }

    val textColor = if (isError) {
        MaterialTheme.colorScheme.onErrorContainer
    } else {
        MaterialTheme.colorScheme.primary
    }

    val targetBorderWidth = if (isError || isFocused.value || number != null) 2.dp else 1.dp
    val animatedBorderWidth by animateDpAsState(
        targetValue = targetBorderWidth,
        animationSpec = tween(durationMillis = 150),
        label = "borderWidth"
    )

    val shape = RoundedCornerShape(8.dp)

    Box(
        modifier = modifier
            .clip(shape)
            .border(
                width = animatedBorderWidth,
                shape = shape,
                color = borderColor,
            ),
            //.background(backgroundColor),
        contentAlignment = Alignment.Center
    ) {
        BasicTextField(
            modifier = Modifier
                .fillMaxSize()
                .wrapContentSize()
                .focusRequester(focusRequester)
                .onFocusChanged {
                    isFocused.value = it.isFocused
                    onFocusChanged(it.isFocused)
                }
                .onPreviewKeyEvent { event ->
                    val isDelete = event.key == Key.Backspace || event.key == Key.Delete
                    val isInitialDown = event.type == KeyEventType.KeyDown &&
                            event.nativeKeyEvent.repeatCount == 0

                    if (isDelete && isInitialDown && number == null) {
                        onKeyboardBack()
                        return@onPreviewKeyEvent true
                    }
                    false
                },
            value = text.value,
            onValueChange = { value ->
                val newNumber = value.text
                if (newNumber.length <= 1 && newNumber.isDigitsOnly()) {
                    val digit = newNumber.toIntOrNull()
                    onNumberChanged(digit)
                    if (digit != null) {
                        onNumberEntered()
                    }
                }
            },
            cursorBrush = SolidColor(textColor),
            singleLine = true,
            textStyle = MaterialTheme.typography.bodyMedium.copy(
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Normal,
                fontSize = 24.sp,
                color = textColor,
                lineHeight = 24.sp,
            ),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.NumberPassword
            ),
            decorationBox = { innerTextField ->
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    innerTextField()
                }
            }
        )
    }
}

private fun getPreviousFocusedIndex(currentIndex: Int): Int? {
    return currentIndex.minus(1).takeIf { it >= 0 }
}

private fun getNextFocusedIndex(code: List<Int?>, currentIndex: Int): Int? {
    if (currentIndex >= code.size - 1) return currentIndex

    for (i in (currentIndex + 1) until code.size) {
        if (code[i] == null) {
            return i
        }
    }
    return currentIndex
}

@Preview
@Composable
private fun PreviewVerificationCodeInputFieldExample() {
    val completedCode = remember { mutableStateOf<String?>(null) }
    val currentCode = remember { mutableStateOf("") }
    val isError = remember { mutableStateOf(false) }

    AuthUITheme {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .padding(innerPadding)
                    .consumeWindowInsets(innerPadding)
                    .fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                VerificationCodeInputField(
                    modifier = Modifier.padding(16.dp),
                    isError = isError.value,
                    errorMessage = if (isError.value) "Invalid verification code" else null,
                    onCodeComplete = { code ->
                        completedCode.value = code
                        // Simulate validation - in real app this would be async
                        isError.value = code != "123456"
                    },
                    onCodeChange = { code ->
                        currentCode.value = code
                        // Clear error on change
                        if (isError.value) {
                            isError.value = false
                        }
                    }
                )

                if (!isError.value) {
                    completedCode.value?.let { code ->
                        Text(
                            modifier = Modifier.padding(top = 16.dp),
                            text = "Code entered: $code",
                            color = MaterialTheme.colorScheme.primary,
                            fontSize = 16.sp,
                        )
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun PreviewVerificationCodeInputFieldError() {
    AuthUITheme {
        VerificationCodeInputField(
            modifier = Modifier.padding(16.dp),
            isError = true,
            errorMessage = "Invalid verification code"
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun PreviewVerificationCodeInputField() {
    AuthUITheme {
        VerificationCodeInputField(
            modifier = Modifier.padding(16.dp)
        )
    }
}