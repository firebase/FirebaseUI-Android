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
import androidx.annotation.Nullable
import androidx.annotation.RestrictTo
import androidx.annotation.StringRes
import androidx.core.view.ViewCompat
import androidx.fragment.app.FragmentTransaction
import com.firebase.ui.auth.AuthUI
import com.firebase.ui.auth.ErrorCodes
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
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.auth.ActionCodeSettings
import com.google.firebase.auth.EmailAuthProvider

import com.firebase.ui.auth.ui.email.CheckEmailFragment
import com.firebase.ui.auth.ui.email.RegisterEmailFragment
import com.firebase.ui.auth.ui.email.EmailLinkFragment
import com.firebase.ui.auth.ui.email.TroubleSigningInFragment
import com.firebase.ui.auth.ui.email.WelcomeBackPasswordPrompt

/**
 * Activity to control the entire email sign up flow. Plays host to {@link CheckEmailFragment} and
 * {@link RegisterEmailFragment} and triggers {@link WelcomeBackPasswordPrompt} and {@link
 * WelcomeBackIdpPrompt}.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class EmailActivity : AppCompatBase(),
    CheckEmailFragment.CheckEmailListener,
    RegisterEmailFragment.AnonymousUpgradeListener,
    EmailLinkFragment.TroubleSigningInListener,
    TroubleSigningInFragment.ResendEmailListener {

    private var emailLayout: TextInputLayout? = null

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

        if (savedInstanceState != null) {
            return
        }

        // Get email from intent (can be null)
        var email: String? = intent.extras?.getString(ExtraConstants.EMAIL)
        val responseForLinking: IdpResponse? = intent.extras?.getParcelable(ExtraConstants.IDP_RESPONSE)
        val user: User? = intent.extras?.getParcelable(ExtraConstants.USER)
        if (email != null && responseForLinking != null) {
            // Got here from WelcomeBackEmailLinkPrompt.
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
        } else {
            var emailConfig: AuthUI.IdpConfig? = ProviderUtils.getConfigFromIdps(getFlowParams().providers, EmailAuthProvider.PROVIDER_ID)
            if (emailConfig == null) {
                emailConfig = ProviderUtils.getConfigFromIdps(getFlowParams().providers, AuthUI.EMAIL_LINK_PROVIDER)
            }
            if (emailConfig == null) {
                finishOnDeveloperError(IllegalStateException("No email provider configured."))
                return
            }
            if (emailConfig.getParams().getBoolean(ExtraConstants.ALLOW_NEW_EMAILS, true)) {
                val ft: FragmentTransaction = supportFragmentManager.beginTransaction()
                if (emailConfig.providerId == AuthUI.EMAIL_LINK_PROVIDER) {
                    if (email == null) {
                        finishOnDeveloperError(IllegalStateException("Email cannot be null for email link sign in."))
                        return
                    }
                    showRegisterEmailLinkFragment(emailConfig, email)
                } else {
                    if (user == null) {
                        finishOnDeveloperError(IllegalStateException("User cannot be null for email/password sign in."))
                        return
                    }
                    val fragment = RegisterEmailFragment.newInstance(user)
                    ft.replace(R.id.fragment_register_email, fragment, RegisterEmailFragment.TAG)
                    emailLayout?.let {
                        val emailFieldName = getString(R.string.fui_email_field_name)
                        ViewCompat.setTransitionName(it, emailFieldName)
                        ft.addSharedElement(it, emailFieldName)
                    }
                    ft.disallowAddToBackStack().commit()
                }
            } else {
                emailLayout?.error = getString(R.string.fui_error_email_does_not_exist)
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == RequestCodes.WELCOME_BACK_EMAIL_FLOW ||
            requestCode == RequestCodes.WELCOME_BACK_IDP_FLOW
        ) {
            finish(resultCode, data)
        }
    }

    override fun onExistingEmailUser(user: User) {
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
            startActivityForResult(
                WelcomeBackPasswordPrompt.createIntent(this, getFlowParams(), IdpResponse.Builder(user).build()),
                RequestCodes.WELCOME_BACK_EMAIL_FLOW
            )
            setSlideAnimation()
        }
    }

    override fun onExistingIdpUser(user: User) {
        // Existing social user: direct them to sign in using their chosen provider.
        startActivityForResult(
            WelcomeBackIdpPrompt.createIntent(this, getFlowParams(), user),
            RequestCodes.WELCOME_BACK_IDP_FLOW
        )
        setSlideAnimation()
    }

    override fun onNewUser(user: User) {
        // New user: direct them to create an account with email/password if account creation is enabled.
        var emailConfig: AuthUI.IdpConfig? = ProviderUtils.getConfigFromIdps(getFlowParams().providers, EmailAuthProvider.PROVIDER_ID)
        if (emailConfig == null) {
            emailConfig = ProviderUtils.getConfigFromIdps(getFlowParams().providers, AuthUI.EMAIL_LINK_PROVIDER)
        }
        if (emailConfig == null) {
            finishOnDeveloperError(IllegalStateException("No email provider configured."))
            return
        }
        if (emailConfig.getParams().getBoolean(ExtraConstants.ALLOW_NEW_EMAILS, true)) {
            val ft: FragmentTransaction = supportFragmentManager.beginTransaction()
            if (emailConfig.providerId == AuthUI.EMAIL_LINK_PROVIDER) {
                val email = user.email
                if (email == null) {
                    finishOnDeveloperError(IllegalStateException("Email cannot be null for email link sign in."))
                    return
                }
                showRegisterEmailLinkFragment(emailConfig, email)
            } else {
                val fragment = RegisterEmailFragment.newInstance(user)
                ft.replace(R.id.fragment_register_email, fragment, RegisterEmailFragment.TAG)
                emailLayout?.let {
                    val emailFieldName = getString(R.string.fui_email_field_name)
                    ViewCompat.setTransitionName(it, emailFieldName)
                    ft.addSharedElement(it, emailFieldName)
                }
                ft.disallowAddToBackStack().commit()
            }
        } else {
            emailLayout?.error = getString(R.string.fui_error_email_does_not_exist)
        }
    }

    override fun onTroubleSigningIn(email: String) {
        val troubleSigningInFragment = TroubleSigningInFragment.newInstance(email)
        switchFragment(troubleSigningInFragment, R.id.fragment_register_email, TroubleSigningInFragment.TAG, true, true)
    }

    override fun onClickResendEmail(email: String) {
        if (supportFragmentManager.backStackEntryCount > 0) {
            // We assume that to get to TroubleSigningInFragment we went through EmailLinkFragment,
            // which was added to the fragment back stack. To avoid needing to pop the back stack twice,
            // we preemptively pop off the last EmailLinkFragment.
            supportFragmentManager.popBackStack()
        }
        val emailConfig: AuthUI.IdpConfig = ProviderUtils.getConfigFromIdpsOrThrow(
            getFlowParams().providers,
            EmailAuthProvider.EMAIL_LINK_SIGN_IN_METHOD
        )
        showRegisterEmailLinkFragment(emailConfig, email)
    }

    override fun onSendEmailFailure(e: Exception) {
        finishOnDeveloperError(e)
    }

    override fun onDeveloperFailure(e: Exception) {
        finishOnDeveloperError(e)
    }

    private fun finishOnDeveloperError(e: Exception) {
        finish(
            RESULT_CANCELED,
            IdpResponse.getErrorIntent(FirebaseUiException(ErrorCodes.DEVELOPER_ERROR, e.message ?: "Unknown error"))
        )
    }

    private fun setSlideAnimation() {
        // Make the next activity slide in.
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

    override fun showProgress(@StringRes message: Int) {
        throw UnsupportedOperationException("Email fragments must handle progress updates.")
    }

    override fun hideProgress() {
        throw UnsupportedOperationException("Email fragments must handle progress updates.")
    }

    override fun onMergeFailure(response: IdpResponse) {
        finish(ErrorCodes.ANONYMOUS_UPGRADE_MERGE_CONFLICT, response.toIntent())
    }
}