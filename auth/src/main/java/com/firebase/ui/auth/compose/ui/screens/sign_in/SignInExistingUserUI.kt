//package com.firebase.ui.auth.compose.ui.screens.sign_in
//
//import android.content.Intent
//import androidx.compose.foundation.layout.Column
//import androidx.compose.foundation.layout.PaddingValues
//import androidx.compose.foundation.layout.Row
//import androidx.compose.foundation.layout.Spacer
//import androidx.compose.foundation.layout.height
//import androidx.compose.foundation.layout.padding
//import androidx.compose.foundation.layout.safeDrawingPadding
//import androidx.compose.foundation.layout.size
//import androidx.compose.foundation.layout.width
//import androidx.compose.material.icons.Icons
//import androidx.compose.material.icons.filled.Email
//import androidx.compose.material.icons.filled.Lock
//import androidx.compose.material3.Button
//import androidx.compose.material3.CircularProgressIndicator
//import androidx.compose.material3.ExperimentalMaterial3Api
//import androidx.compose.material3.Icon
//import androidx.compose.material3.MaterialTheme
//import androidx.compose.material3.Scaffold
//import androidx.compose.material3.Text
//import androidx.compose.material3.TextButton
//import androidx.compose.material3.TopAppBar
//import androidx.compose.runtime.Composable
//import androidx.compose.runtime.remember
//import androidx.compose.ui.Alignment
//import androidx.compose.ui.Modifier
//import androidx.compose.ui.graphics.Color
//import androidx.compose.ui.platform.LocalContext
//import androidx.compose.ui.text.style.TextAlign
//import androidx.compose.ui.text.style.TextDecoration
//import androidx.compose.ui.tooling.preview.Preview
//import androidx.compose.ui.unit.dp
//import androidx.compose.ui.unit.sp
//import androidx.core.net.toUri
//import com.firebase.ui.auth.compose.configuration.AuthUIConfiguration
//import com.firebase.ui.auth.compose.configuration.authUIConfiguration
//import com.firebase.ui.auth.compose.configuration.auth_provider.AuthProvider
//import com.firebase.ui.auth.compose.configuration.string_provider.DefaultAuthUIStringProvider
//import com.firebase.ui.auth.compose.configuration.theme.AuthUITheme
//import com.firebase.ui.auth.compose.configuration.validators.EmailValidator
//import com.firebase.ui.auth.compose.configuration.validators.PasswordValidator
//import com.firebase.ui.auth.compose.ui.components.AuthTextField
//import com.firebase.ui.auth.compose.ui.screens.SignInUI
//
//@OptIn(ExperimentalMaterial3Api::class)
//@Composable
//fun SignInExistingUserUI(
//    modifier: Modifier = Modifier,
//    configuration: AuthUIConfiguration,
//    provider: AuthProvider.Email,
//    email: String,
//    password: String,
//    isLoading: Boolean,
//    onEmailChange: (String) -> Unit,
//    onPasswordChange: (String) -> Unit,
//    onSignInClick: () -> Unit,
//    onGoToSignUp: () -> Unit,
//    onGoToResetPassword: () -> Unit,
//) {
//    val context = LocalContext.current
//    val passwordValidator = remember {
//        PasswordValidator(
//            stringProvider = DefaultAuthUIStringProvider(context),
//            rules = emptyList()
//        )
//    }
//
//    Scaffold(
//        modifier = modifier,
//        topBar = {
//            TopAppBar(
//                title = {
//                    Text("Sign In")
//                },
//            )
//        },
//    ) { innerPadding ->
//        Column(
//            modifier = Modifier
//                .padding(innerPadding)
//                .safeDrawingPadding()
//                .padding(horizontal = 16.dp),
//        ) {
//            Text(
//                "Welcome Back",
//                fontSize = 20.sp,
//            )
//            Text(
//                "You've already used $email to sign in. " +
//                        "Enter the password for that account",
//                color = Color.Gray,
//                fontSize = 14.sp,
//            )
//            Spacer(modifier = Modifier.height(24.dp))
//            AuthTextField(
//                value = password,
//                validator = passwordValidator,
//                enabled = !isLoading,
//                label = {
//                    Text("Password")
//                },
//                onValueChange = { text ->
//                    onPasswordChange(password)
//                },
//                leadingIcon = {
//                    Icon(
//                        imageVector = Icons.Default.Lock,
//                        contentDescription = ""
//                    )
//                }
//            )
//            Spacer(modifier = Modifier.height(8.dp))
//            TextButton(
//                modifier = Modifier
//                    .align(Alignment.Start),
//                onClick = {
//                    onGoToResetPassword()
//                },
//                enabled = !isLoading,
//                contentPadding = PaddingValues.Zero
//            ) {
//                Text(
//                    modifier = modifier,
//                    text = "Trouble signing in?",
//                    style = MaterialTheme.typography.bodyMedium,
//                    textAlign = TextAlign.Center,
//                    textDecoration = TextDecoration.Underline
//                )
//            }
//            Spacer(modifier = Modifier.height(8.dp))
//            Row(
//                modifier = Modifier
//                    .align(Alignment.End),
//            ) {
//                Button(
//                    onClick = {
//                        onSignInClick()
//                    },
//                    enabled = !isLoading,
//                ) {
//                    if (isLoading) {
//                        CircularProgressIndicator(
//                            modifier = Modifier
//                                .size(16.dp)
//                        )
//                    } else {
//                        Text("Sign In")
//                    }
//                }
//            }
//            Spacer(modifier = Modifier.height(16.dp))
//            Row(
//                modifier = Modifier
//                    .align(Alignment.End),
//            ) {
//                TextButton(
//                    onClick = {
//                        val intent = Intent(
//                            Intent.ACTION_VIEW,
//                            configuration.tosUrl?.toUri()
//                        )
//                        context.startActivity(intent)
//                    },
//                    contentPadding = PaddingValues.Zero,
//                    enabled = !isLoading,
//                ) {
//                    Text(
//                        modifier = modifier,
//                        text = "Terms of Service",
//                        style = MaterialTheme.typography.bodyMedium,
//                        textAlign = TextAlign.Center,
//                        textDecoration = TextDecoration.Underline
//                    )
//                }
//                Spacer(modifier = Modifier.width(24.dp))
//                TextButton(
//                    onClick = {
//                        val intent = Intent(
//                            Intent.ACTION_VIEW,
//                            configuration.privacyPolicyUrl?.toUri()
//                        )
//                        context.startActivity(intent)
//                    },
//                    contentPadding = PaddingValues.Zero,
//                    enabled = !isLoading,
//                ) {
//                    Text(
//                        modifier = modifier,
//                        text = "Privacy Policy",
//                        style = MaterialTheme.typography.bodyMedium,
//                        textAlign = TextAlign.Center,
//                        textDecoration = TextDecoration.Underline
//                    )
//                }
//            }
//        }
//    }
//}
//
//@Preview
//@Composable
//fun PreviewSignInExistingUserUI() {
//    val applicationContext = LocalContext.current
//    val provider = AuthProvider.Email(
//        isDisplayNameRequired = true,
//        isEmailLinkSignInEnabled = false,
//        isEmailLinkForceSameDeviceEnabled = true,
//        actionCodeSettings = null,
//        isNewAccountsAllowed = true,
//        minimumPasswordLength = 8,
//        passwordValidationRules = listOf()
//    )
//
//    AuthUITheme {
//        SignInExistingUserUI(
//            configuration = authUIConfiguration {
//                context = applicationContext
//                providers { provider(provider) }
//                tosUrl = ""
//                privacyPolicyUrl = ""
//            },
//            provider = provider,
//            email = "test@example.com",
//            password = "",
//            isLoading = false,
//            onEmailChange = { email -> },
//            onPasswordChange = { password -> },
//            onSignInClick = {},
//            onGoToSignUp = {},
//            onGoToResetPassword = {},
//        )
//    }
//}