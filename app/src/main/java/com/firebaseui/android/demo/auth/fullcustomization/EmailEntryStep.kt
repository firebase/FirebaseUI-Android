package com.firebaseui.android.demo.auth.fullcustomization

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.firebaseui.android.demo.R

@Composable
fun EmailEntryStep(
    email: String,
    onEmailChange: (String) -> Unit,
    isLoading: Boolean,
    onContinue: () -> Unit,
    onShowOtherMethods: () -> Unit,
) {
    val isEmailValid = remember(email) {
        android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }
    val showEmailError = email.isNotBlank() && !isEmailValid

    // verticalScroll measures content with infinite max height, and Column distributes weights
    // against the MIN height when max is infinite (RowColumnMeasurePolicy.kt) — so
    // heightIn(min = viewport) makes the weighted spacers expand (centering content, anchoring
    // the link to the bottom) when everything fits, and collapse to zero (plain scrolling) when
    // it doesn't.
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
                .padding(horizontal = 48.dp, vertical = 24.dp),
        ) {
            Spacer(modifier = Modifier.weight(1f))

            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Image(
                    painter = painterResource(id = R.drawable.full_customization_mascot),
                    contentDescription = "doggo - cute welcome mascot",
                    modifier = Modifier
                        .size(96.dp)
                        .offset(y = 12.dp)
                        .zIndex(1f),
                )

                Surface(
                    color = MaterialTheme.colorScheme.secondary,
                    shape = IntroShape,
                    modifier = Modifier
                        .fillMaxWidth()
                        .semantics { contentDescription = "intro - welcome headline bubble" },
                ) {
                    Text(
                        text = "Hey there,\nWelcome",
                        style = MaterialTheme.typography.headlineMedium.copy(
                            textAlign = TextAlign.Center,
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    Color(0xFFFFF8F8),
                                    Color(0xFFFFDDB4),
                                    Color(0xFFFFD8EB),
                                ),
                            ),
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 16.dp),
                    )
                }

                HardOffsetShadow(
                    shape = AuthFieldShape,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .semantics { contentDescription = "email - sign in card" },
                        color = MaterialTheme.colorScheme.surface,
                        shape = AuthFieldShape,
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                        ) {
                            Text(
                                text = "Enter your email address to continue.",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth(),
                            )

                            FullCustomizationTextField(
                                value = email,
                                onValueChange = onEmailChange,
                                label = "Email address",
                                leadingIcon = { EmailFieldIcon() },
                                enabled = !isLoading,
                                isError = showEmailError,
                                supportingText = if (showEmailError) "Enter a valid email address" else null,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .semantics { contentDescription = "text-field - email address input" },
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                CtaButton(
                    text = "Continue",
                    onClick = onContinue,
                    enabled = isEmailValid && !isLoading,
                    isLoading = isLoading,
                    modifier = Modifier.semantics { contentDescription = "button - continue to password" },
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            TextButton(
                onClick = onShowOtherMethods,
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .semantics { contentDescription = "Other sign-in methods button" },
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Login,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Use other sign-in methods")
            }
        }
    }
}
