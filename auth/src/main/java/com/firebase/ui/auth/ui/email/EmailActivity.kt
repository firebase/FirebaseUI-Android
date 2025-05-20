/*
 * Copyright 2016 Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.firebase.ui.auth.ui.email

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.Nullable
import androidx.annotation.RestrictTo
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.core.view.ViewCompat
import androidx.fragment.app.FragmentTransaction
import androidx.compose.ui.platform.LocalContext
import com.firebase.ui.auth.AuthUI
import com.firebase.ui.auth.ErrorCodes
import com.firebase.ui.auth.FirebaseAuthAnonymousUpgradeException
import com.firebase.ui.auth.FirebaseUiException
import com.firebase.ui.auth.IdpResponse
import com.firebase.ui.auth.R
import com.firebase.ui.auth.data.model.FlowParameters
import com.firebase.ui.auth.data.model.User
import com.firebase.ui.auth.ui.AppCompatBase
import com.firebase.ui.auth.ui.idp.WelcomeBackIdpPrompt
import com.firebase.ui.auth.util.ExtraConstants
import com.firebase.ui.auth.util.data.EmailLinkPersistenceManager
import com.firebase.ui.auth.util.data.ProviderUtils
import com.firebase.ui.auth.viewmodel.RequestCodes
import com.firebase.ui.auth.viewmodel.email.EmailProviderResponseHandler
import com.firebase.ui.auth.viewmodel.email.WelcomeBackPasswordViewModel
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.auth.ActionCodeSettings
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import androidx.lifecycle.ViewModelProvider

import com.firebase.ui.auth.ui.email.CheckEmailFragment
import com.firebase.ui.auth.ui.email.RegisterEmailFragment
import com.firebase.ui.auth.ui.email.EmailLinkFragment
import com.firebase.ui.auth.ui.email.TroubleSigningInFragment
import com.firebase.ui.auth.ui.email.WelcomeBackPasswordPrompt
import com.firebase.ui.auth.ui.email.CheckEmailScreen
import com.firebase.ui.auth.ui.email.RegisterEmailScreen

/**
 * Activity to control the entire email sign up flow. Plays host to {@link CheckEmailFragment} and
 * {@link RegisterEmailFragment} and triggers {@link WelcomeBackPasswordPrompt} and {@link
 * WelcomeBackIdpPrompt}.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class EmailActivity : AppCompatBase(), RegisterEmailFragment.AnonymousUpgradeListener {

    private var emailLayout: TextInputLayout? = null
    private lateinit var mHandler: EmailProviderResponseHandler
    private lateinit var mPasswordHandler: WelcomeBackPasswordViewModel

    companion object {
        @JvmStatic
        fun createIntent(context: Context, flowParams: FlowParameters): Intent {
            return createBaseIntent(context, EmailActivity::class.java, flowParams)
        }

        @JvmStatic
        fun createIntent(context: Context, flowParams: FlowParameters, email: String?): Intent {
            return createBaseIntent(context, EmailActivity::class.java, flowParams)
                .putExtra(ExtraConstants.EMAIL, email)
        }

        @JvmStatic
        fun createIntentForLinking(
            context: Context,
            flowParams: FlowParameters,
            responseForLinking: IdpResponse
        ): Intent {
            return createIntent(context, flowParams, responseForLinking.email)
                .putExtra(ExtraConstants.IDP_RESPONSE, responseForLinking)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.fui_activity_register_email)

        emailLayout = findViewById(R.id.email_layout)
        mHandler = ViewModelProvider(this).get(EmailProviderResponseHandler::class.java)
        mHandler.init(getFlowParams())
        mPasswordHandler = ViewModelProvider(this).get(WelcomeBackPasswordViewModel::class.java)
        mPasswordHandler.init(getFlowParams())

        if (savedInstanceState != null) {
            return
        }

        // Get email from intent (can be null)
        var email: String? = intent.extras?.getString(ExtraConstants.EMAIL)
        val responseForLinking: IdpResponse? = intent.extras?.getParcelable(ExtraConstants.IDP_RESPONSE)
        val user: User? = intent.extras?.getParcelable(ExtraConstants.USER)

        if (email != null && responseForLinking != null) {
            handleEmailLinkLinking(email, responseForLinking)
        } else {
            handleNormalEmailFlow(email, user)
        }
    }

    private fun handleEmailLinkLinking(email: String, responseForLinking: IdpResponse) {
        val emailConfig: AuthUI.IdpConfig = ProviderUtils.getConfigFromIdpsOrThrow(
            getFlowParams().providers,
            AuthUI.EMAIL_LINK_PROVIDER
        )
        val actionCodeSettings: ActionCodeSettings? =
            emailConfig.getParams().getParcelable(ExtraConstants.ACTION_CODE_SETTINGS)
        if (actionCodeSettings == null) {
            finishOnDeveloperError(IllegalStateException("ActionCodeSettings cannot be null for email link sign in."))
            return
        }
        EmailLinkPersistenceManager.getInstance().saveIdpResponseForLinking(application, responseForLinking)
        val forceSameDevice: Boolean = emailConfig.getParams().getBoolean(ExtraConstants.FORCE_SAME_DEVICE)
        val fragment = EmailLinkFragment.newInstance(email, actionCodeSettings, responseForLinking, forceSameDevice)
        switchFragment(fragment, R.id.fragment_register_email, EmailLinkFragment.TAG)
    }

    private fun handleNormalEmailFlow(email: String?, user: User?) {
        var emailConfig: AuthUI.IdpConfig? = ProviderUtils.getConfigFromIdps(getFlowParams().providers, EmailAuthProvider.PROVIDER_ID)
        if (emailConfig == null) {
            emailConfig = ProviderUtils.getConfigFromIdps(getFlowParams().providers, AuthUI.EMAIL_LINK_PROVIDER)
        }
        if (emailConfig == null) {
            finishOnDeveloperError(IllegalStateException("No email provider configured."))
            return
        }

        if (emailConfig.getParams().getBoolean(ExtraConstants.ALLOW_NEW_EMAILS, true)) {
            if (emailConfig.providerId == AuthUI.EMAIL_LINK_PROVIDER) {
                if (email == null) {
                    finishOnDeveloperError(IllegalStateException("Email cannot be null for email link sign in."))
                    return
                }
                showRegisterEmailLinkFragment(emailConfig, email)
            } else {
                if (user == null) {
                    // Use default email from configuration if none was provided via the intent.
                    val finalEmail = email ?: emailConfig.getParams().getString(ExtraConstants.DEFAULT_EMAIL)
                    setContent {
                        CheckEmailScreenContent(
                            flowParameters = getFlowParams(),
                            initialEmail = finalEmail,
                            onExistingEmailUser = { user -> handleExistingEmailUser(user) },
                            onExistingIdpUser = { user -> handleExistingIdpUser(user) },
                            onNewUser = { user -> handleNewUser(user) },
                            onDeveloperFailure = { e -> handleDeveloperFailure(e) }
                        )
                    }
                } else {
                    setContent {
                        RegisterEmailScreen(
                            flowParameters = getFlowParams(),
                            user = user,
                            onRegisterSuccess = { newUser, password ->
                                mHandler.startSignIn(
                                    IdpResponse.Builder(newUser).build(),
                                    password
                                )
                                finish()
                            },
                            onRegisterError = { e ->
                            }
                        )
                    }
                }
            }
        } else {
            emailLayout?.error = getString(R.string.fui_error_email_does_not_exist)
        }
    }

    @Composable
    private fun CheckEmailScreenContent(
        flowParameters: FlowParameters,
        initialEmail: String?,
        onExistingEmailUser: (User) -> Unit,
        onExistingIdpUser: (User) -> Unit,
        onNewUser: (User) -> Unit,
        onDeveloperFailure: (Exception) -> Unit
    ) {
        CheckEmailScreen(
            flowParameters = flowParameters,
            initialEmail = initialEmail,
            onExistingEmailUser = onExistingEmailUser,
            onExistingIdpUser = onExistingIdpUser,
            onNewUser = onNewUser,
            onDeveloperFailure = onDeveloperFailure
        )
    }

    private fun handleExistingEmailUser(user: User) {
        if (user.providerId == AuthUI.EMAIL_LINK_PROVIDER) {
            val emailConfig: AuthUI.IdpConfig = ProviderUtils.getConfigFromIdpsOrThrow(
                getFlowParams().providers,
                AuthUI.EMAIL_LINK_PROVIDER
            )
            val email = user.email
            if (email == null) {
                finishOnDeveloperError(IllegalStateException("Email cannot be null for email link sign in."))
                return
            }
            showRegisterEmailLinkFragment(emailConfig, email)
        } else {
            setContent {
                WelcomeBackPasswordPrompt(
                    flowParameters = getFlowParams(),
                    email = user.email ?: "",
                    idpResponse = IdpResponse.Builder(user).build(),
                    onSignInSuccess = {
                        finish(RESULT_OK, IdpResponse.Builder(user).build().toIntent())
                    },
                    onSignInError = { exception ->
                        when (exception) {
                            is FirebaseAuthAnonymousUpgradeException -> {
                                finish(ErrorCodes.ANONYMOUS_UPGRADE_MERGE_CONFLICT, exception.response.toIntent())
                            }
                            is FirebaseAuthInvalidCredentialsException -> {
                                // Error is already handled in the UI
                            }
                            else -> {
                                finish(RESULT_CANCELED, IdpResponse.getErrorIntent(exception))
                            }
                        }
                    },
                    onForgotPassword = {
                        startActivityForResult(
                            RecoverPasswordActivity.createIntent(this, getFlowParams(), user.email),
                            RequestCodes.RECOVER_PASSWORD
                        )
                        setSlideAnimation()
                    },
                    viewModel = mPasswordHandler
                )
            }
        }
    }

    private fun handleExistingIdpUser(user: User) {
        startActivityForResult(
            WelcomeBackIdpPrompt.createIntent(this, getFlowParams(), user),
            RequestCodes.WELCOME_BACK_IDP_FLOW
        )
        setSlideAnimation()
    }

    private fun handleNewUser(user: User) {
        var emailConfig: AuthUI.IdpConfig? = ProviderUtils.getConfigFromIdps(getFlowParams().providers, EmailAuthProvider.PROVIDER_ID)
        if (emailConfig == null) {
            emailConfig = ProviderUtils.getConfigFromIdps(getFlowParams().providers, AuthUI.EMAIL_LINK_PROVIDER)
        }
        if (emailConfig == null) {
            finishOnDeveloperError(IllegalStateException("No email provider configured."))
            return
        }
        if (emailConfig.getParams().getBoolean(ExtraConstants.ALLOW_NEW_EMAILS, true)) {
            if (emailConfig.providerId == AuthUI.EMAIL_LINK_PROVIDER) {
                val email = user.email
                if (email == null) {
                    finishOnDeveloperError(IllegalStateException("Email cannot be null for email link sign in."))
                    return
                }
                showRegisterEmailLinkFragment(emailConfig, email)
            } else {
                setContent {
                    RegisterEmailScreen(
                        flowParameters = getFlowParams(),
                        user = user,
                        onRegisterSuccess = { newUser, password ->
                            mHandler.startSignIn(
                                IdpResponse.Builder(newUser).build(),
                                password
                            )
                            finish()
                        },
                        onRegisterError = { e ->

                        }
                    )
                }
            }
        } else {
            emailLayout?.error = getString(R.string.fui_error_email_does_not_exist)
        }
    }

    private fun handleDeveloperFailure(e: Exception) {
        finishOnDeveloperError(e)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == RequestCodes.WELCOME_BACK_EMAIL_FLOW ||
            requestCode == RequestCodes.WELCOME_BACK_IDP_FLOW
        ) {
            finish(resultCode, data)
        }
    }

    private fun finishOnDeveloperError(e: Exception) {
        finish(
            RESULT_CANCELED,
            IdpResponse.getErrorIntent(FirebaseUiException(ErrorCodes.DEVELOPER_ERROR, e.message ?: "Unknown error"))
        )
    }

    private fun setSlideAnimation() {
        overridePendingTransition(R.anim.fui_slide_in_right, R.anim.fui_slide_out_left)
    }

    private fun showRegisterEmailLinkFragment(emailConfig: AuthUI.IdpConfig, email: String) {
        val actionCodeSettings: ActionCodeSettings? = emailConfig.getParams().getParcelable(ExtraConstants.ACTION_CODE_SETTINGS)
        if (actionCodeSettings == null) {
            finishOnDeveloperError(IllegalStateException("ActionCodeSettings cannot be null for email link sign in."))
            return
        }
        val fragment = EmailLinkFragment.newInstance(email, actionCodeSettings)
        switchFragment(fragment, R.id.fragment_register_email, EmailLinkFragment.TAG)
    }

    override fun showProgress(message: Int) {
        throw UnsupportedOperationException("Email fragments must handle progress updates.")
    }

    override fun hideProgress() {
        throw UnsupportedOperationException("Email fragments must handle progress updates.")
    }

    override fun onMergeFailure(response: IdpResponse) {
        finish(ErrorCodes.ANONYMOUS_UPGRADE_MERGE_CONFLICT, response.toIntent())
    }
}