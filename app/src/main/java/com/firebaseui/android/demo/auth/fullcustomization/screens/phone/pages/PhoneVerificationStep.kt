package com.firebaseui.android.demo.auth.fullcustomization.screens.phone.pages

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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.firebase.ui.auth.ui.components.VerificationCodeInputField
import com.firebase.ui.auth.ui.screens.phone.PhoneAuthContentState
import com.firebaseui.android.demo.R
import com.firebaseui.android.demo.auth.fullcustomization.common.AuthFieldShape
import com.firebaseui.android.demo.auth.fullcustomization.common.CtaButton
import com.firebaseui.android.demo.auth.fullcustomization.common.HardOffsetShadow

@Composable
fun PhoneVerificationStep(state: PhoneAuthContentState) {
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
                    painter = painterResource(id = R.drawable.full_customization_phone_mascot),
                    contentDescription = "doggo - cute phone sign-in mascot",
                    modifier = Modifier.size(72.dp),
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Enter your code",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.primary,
                )
                Spacer(modifier = Modifier.height(24.dp))

                HardOffsetShadow(shape = AuthFieldShape, modifier = Modifier.fillMaxWidth()) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .semantics { contentDescription = "phone - verification card" },
                        color = MaterialTheme.colorScheme.surface,
                        shape = AuthFieldShape,
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                        ) {
                            Text(
                                text = "We sent a code to ${state.fullPhoneNumber}.",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )

                            VerificationCodeInputField(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .semantics { contentDescription = "text-field - verification code input" },
                                isError = state.error != null,
                                errorMessage = state.error,
                                onCodeChange = state.onVerificationCodeChange,
                            )

                            Text(
                                text = if (state.resendTimer > 0) {
                                    "Resend code in ${state.resendTimer}s"
                                } else {
                                    "Resend code"
                                },
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.primary,
                                textAlign = TextAlign.End,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable(enabled = state.resendTimer == 0) {
                                        state.onResendCodeClick()
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
                    text = "Verify",
                    onClick = state.onVerifyCodeClick,
                    enabled = state.verificationCode.isNotBlank() && !state.isLoading,
                    isLoading = state.isLoading,
                    modifier = Modifier.semantics { contentDescription = "button - verify code" },
                )

                Spacer(modifier = Modifier.height(8.dp))

                TextButton(
                    onClick = state.onChangeNumberClick,
                    enabled = !state.isLoading,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Use a different number")
                }
            }
        }
    }
}
