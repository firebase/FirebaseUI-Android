package com.firebaseui.android.demo.auth.fullcustomization

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.firebase.ui.auth.ui.screens.email.EmailAuthContentState
import com.firebaseui.android.demo.R

private val NameFieldStartShape = RoundedCornerShape(
    topStart = 16.dp,
    bottomStart = 16.dp,
    topEnd = 0.dp,
    bottomEnd = 0.dp,
)
private val NameFieldEndShape = RoundedCornerShape(
    topStart = 0.dp,
    bottomStart = 0.dp,
    topEnd = 16.dp,
    bottomEnd = 16.dp,
)

@Composable
fun SignUpStep(
    state: EmailAuthContentState,
    onUseDifferentEmail: () -> Unit,
) {
    var firstName by remember { mutableStateOf("") }
    var lastName by remember { mutableStateOf("") }
    var confirmEmail by remember { mutableStateOf("") }

    val emailsMatch = confirmEmail.isNotBlank() && confirmEmail == state.email
    val passwordsMatch = state.confirmPassword.isNotBlank() && state.confirmPassword == state.password
    val canSignUp = firstName.isNotBlank() &&
        lastName.isNotBlank() &&
        emailsMatch &&
        state.password.isNotBlank() &&
        passwordsMatch &&
        !state.isLoading

    // verticalScroll measures content with infinite max height, and Column distributes weights
    // against the MIN height when max is infinite (RowColumnMeasurePolicy.kt) — so
    // heightIn(min = viewport) makes the weighted spacers expand (centering content, anchoring
    // CTAs to the bottom) when everything fits, and collapse to zero (plain scrolling) when it
    // doesn't.
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .heightIn(min = maxHeight)
                .padding(horizontal = 40.dp, vertical = 24.dp),
        ) {
            Spacer(modifier = Modifier.weight(1f))

            Column(modifier = Modifier.fillMaxWidth()) {
                Image(
                    painter = painterResource(id = R.drawable.full_customization_mascot),
                    contentDescription = "doggo - cute welcome mascot",
                    modifier = Modifier.size(72.dp),
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Sign up",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.primary,
                )
                Spacer(modifier = Modifier.height(24.dp))

                HardOffsetShadow(shape = AuthFieldShape, modifier = Modifier.fillMaxWidth()) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .semantics { contentDescription = "sign up card" },
                        color = MaterialTheme.colorScheme.surface,
                        shape = AuthFieldShape,
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(24.dp),
                        ) {
                            Row(modifier = Modifier.fillMaxWidth()) {
                                FullCustomizationTextField(
                                    value = firstName,
                                    onValueChange = { firstName = it },
                                    label = "First name",
                                    enabled = !state.isLoading,
                                    shape = NameFieldStartShape,
                                    modifier = Modifier
                                        .weight(1f)
                                        .semantics { contentDescription = "text-field - first name" },
                                )
                                FullCustomizationTextField(
                                    value = lastName,
                                    onValueChange = { lastName = it },
                                    label = "Last name",
                                    enabled = !state.isLoading,
                                    shape = NameFieldEndShape,
                                    modifier = Modifier
                                        .weight(1f)
                                        .semantics { contentDescription = "text-field - last name" },
                                )
                            }

                            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                                FullCustomizationTextField(
                                    value = state.email,
                                    onValueChange = {},
                                    enabled = false,
                                    leadingIcon = { EmailFieldIcon() },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .semantics { contentDescription = "text-field - email address display" },
                                )
                                FullCustomizationTextField(
                                    value = confirmEmail,
                                    onValueChange = { confirmEmail = it },
                                    label = "Confirm Email",
                                    enabled = !state.isLoading,
                                    isError = confirmEmail.isNotBlank() && !emailsMatch,
                                    supportingText = if (confirmEmail.isNotBlank() && !emailsMatch) {
                                        "Emails don't match"
                                    } else {
                                        null
                                    },
                                    leadingIcon = { EmailFieldIcon() },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .semantics { contentDescription = "text-field - confirm email" },
                                )
                            }

                            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                                FullCustomizationTextField(
                                    value = state.password,
                                    onValueChange = state.onPasswordChange,
                                    label = "Password",
                                    enabled = !state.isLoading,
                                    visualTransformation = PasswordVisualTransformation(),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .semantics { contentDescription = "text-field - password" },
                                )
                                FullCustomizationTextField(
                                    value = state.confirmPassword,
                                    onValueChange = state.onConfirmPasswordChange,
                                    label = "Confirm Password",
                                    enabled = !state.isLoading,
                                    visualTransformation = PasswordVisualTransformation(),
                                    isError = state.confirmPassword.isNotBlank() && !passwordsMatch,
                                    supportingText = if (state.confirmPassword.isNotBlank() && !passwordsMatch) {
                                        "Passwords don't match"
                                    } else {
                                        null
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .semantics { contentDescription = "text-field - confirm password" },
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))
            Spacer(modifier = Modifier.height(24.dp))

            Column(modifier = Modifier.fillMaxWidth()) {
                CtaButton(
                    text = "Sign up",
                    onClick = {
                        state.onDisplayNameChange("$firstName $lastName".trim())
                        state.onSignUpClick()
                    },
                    enabled = canSignUp,
                    isLoading = state.isLoading,
                    modifier = Modifier.semantics { contentDescription = "button - sign up" },
                )

                Spacer(modifier = Modifier.height(8.dp))

                TextButton(
                    onClick = onUseDifferentEmail,
                    enabled = !state.isLoading,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Use a different email")
                }
            }
        }
    }
}
