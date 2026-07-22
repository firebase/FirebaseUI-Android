package com.firebaseui.android.demo.auth.fullcustomization

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.firebase.ui.auth.ui.screens.email.EmailAuthContentState
import com.firebaseui.android.demo.R

@Composable
fun LoginStep(
    state: EmailAuthContentState,
    onUseDifferentEmail: () -> Unit,
) {
    var passwordVisible by remember { mutableStateOf(false) }

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
                    text = "Login",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.primary,
                )
                Spacer(modifier = Modifier.height(24.dp))

                HardOffsetShadow(shape = AuthFieldShape, modifier = Modifier.fillMaxWidth()) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .semantics { contentDescription = "email - login card" },
                        color = MaterialTheme.colorScheme.surface,
                        shape = AuthFieldShape,
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                        ) {
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
                                value = state.password,
                                onValueChange = state.onPasswordChange,
                                label = "Password",
                                enabled = !state.isLoading,
                                visualTransformation = if (passwordVisible) {
                                    VisualTransformation.None
                                } else {
                                    PasswordVisualTransformation()
                                },
                                trailingIcon = {
                                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                        Icon(
                                            imageVector = if (passwordVisible) {
                                                Icons.Default.VisibilityOff
                                            } else {
                                                Icons.Default.Visibility
                                            },
                                            contentDescription = null,
                                        )
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .semantics { contentDescription = "text-field - password secure input" },
                            )

                            Text(
                                text = if (state.resetLinkSent) "Reset link sent!" else "Forgot password?",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.primary,
                                textDecoration = TextDecoration.Underline,
                                textAlign = TextAlign.End,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable(enabled = !state.resetLinkSent) {
                                        state.onSendResetLinkClick()
                                    },
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))
            Spacer(modifier = Modifier.height(24.dp))

            Column(modifier = Modifier.fillMaxWidth()) {
                CtaButton(
                    text = "Login",
                    onClick = state.onSignInClick,
                    enabled = state.password.isNotBlank() && !state.isLoading,
                    isLoading = state.isLoading,
                    modifier = Modifier.semantics { contentDescription = "button - login" },
                )

                Spacer(modifier = Modifier.height(16.dp))

                CtaButton(
                    text = if (state.emailSignInLinkSent) "Login link sent!" else "Send login link",
                    onClick = state.onSignInEmailLinkClick,
                    enabled = !state.isLoading,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    ),
                    modifier = Modifier.semantics { contentDescription = "button - send login link" },
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
