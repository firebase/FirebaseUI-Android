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

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.annotation.NonNull
import androidx.annotation.Nullable
import androidx.annotation.RestrictTo
import androidx.lifecycle.ViewModelProvider
import com.firebase.ui.auth.ErrorCodes
import com.firebase.ui.auth.FirebaseAuthAnonymousUpgradeException
import com.firebase.ui.auth.FirebaseUiException
import com.firebase.ui.auth.IdpResponse
import com.firebase.ui.auth.R
import com.firebase.ui.auth.data.model.FlowParameters
import com.firebase.ui.auth.data.model.UserCancellationException
import com.firebase.ui.auth.ui.InvisibleActivityBase
import com.firebase.ui.auth.util.ExtraConstants
import com.firebase.ui.auth.viewmodel.RequestCodes
import com.firebase.ui.auth.viewmodel.ResourceObserver
import com.firebase.ui.auth.viewmodel.email.EmailLinkSignInHandler
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException

// Assuming EmailLinkErrorRecoveryActivity exists in your project.
import com.firebase.ui.auth.ui.email.EmailLinkErrorRecoveryActivity

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class EmailLinkCatcherActivity : InvisibleActivityBase() {

    private lateinit var mHandler: EmailLinkSignInHandler

    companion object {
        @JvmStatic
        fun createIntent(context: Context, flowParams: FlowParameters): Intent {
            return createBaseIntent(context, EmailLinkCatcherActivity::class.java, flowParams)
        }
    }

    override fun onCreate(@Nullable savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        initHandler()

        if (getFlowParams().emailLink != null) {
            mHandler.startSignIn()
        }
    }

    private fun initHandler() {
        mHandler = ViewModelProvider(this).get(EmailLinkSignInHandler::class.java)
        mHandler.init(getFlowParams())
        mHandler.operation.observe(this, object : ResourceObserver<IdpResponse>(this) {
            override fun onSuccess(@NonNull response: IdpResponse) {
                finish(RESULT_OK, response.toIntent())
            }

            override fun onFailure(@NonNull e: Exception) {
                when {
                    e is UserCancellationException -> finish(RESULT_CANCELED, null)
                    e is FirebaseAuthAnonymousUpgradeException -> {
                        val res = e.response
                        finish(RESULT_CANCELED, Intent().putExtra(ExtraConstants.IDP_RESPONSE, res))
                    }
                    e is FirebaseUiException -> {
                        val errorCode = e.errorCode
                        when (errorCode) {
                            ErrorCodes.EMAIL_LINK_WRONG_DEVICE_ERROR,
                            ErrorCodes.INVALID_EMAIL_LINK_ERROR,
                            ErrorCodes.EMAIL_LINK_DIFFERENT_ANONYMOUS_USER_ERROR ->
                                buildAlertDialog(errorCode).show()
                            ErrorCodes.EMAIL_LINK_PROMPT_FOR_EMAIL_ERROR,
                            ErrorCodes.EMAIL_MISMATCH_ERROR ->
                                startErrorRecoveryFlow(RequestCodes.EMAIL_LINK_PROMPT_FOR_EMAIL_FLOW)
                            ErrorCodes.EMAIL_LINK_CROSS_DEVICE_LINKING_ERROR ->
                                startErrorRecoveryFlow(RequestCodes.EMAIL_LINK_CROSS_DEVICE_LINKING_FLOW)
                            else -> finish(RESULT_CANCELED, IdpResponse.getErrorIntent(e))
                        }
                    }
                    e is FirebaseAuthInvalidCredentialsException ->
                        startErrorRecoveryFlow(RequestCodes.EMAIL_LINK_PROMPT_FOR_EMAIL_FLOW)
                    else -> finish(RESULT_CANCELED, IdpResponse.getErrorIntent(e))
                }
            }
        })
    }

    /**
     * @param flow must be one of RequestCodes.EMAIL_LINK_PROMPT_FOR_EMAIL_FLOW or
     *             RequestCodes.EMAIL_LINK_CROSS_DEVICE_LINKING_FLOW
     */
    private fun startErrorRecoveryFlow(flow: Int) {
        if (flow != RequestCodes.EMAIL_LINK_CROSS_DEVICE_LINKING_FLOW &&
            flow != RequestCodes.EMAIL_LINK_PROMPT_FOR_EMAIL_FLOW
        ) {
            throw IllegalStateException(
                "Invalid flow param. It must be either " +
                        "RequestCodes.EMAIL_LINK_CROSS_DEVICE_LINKING_FLOW or " +
                        "RequestCodes.EMAIL_LINK_PROMPT_FOR_EMAIL_FLOW"
            )
        }
        val intent = EmailLinkErrorRecoveryActivity.createIntent(applicationContext, getFlowParams(), flow)
        startActivityForResult(intent, flow)
    }

    private fun buildAlertDialog(errorCode: Int): AlertDialog {
        val builder = AlertDialog.Builder(this)
        val (titleText, messageText) = when (errorCode) {
            ErrorCodes.EMAIL_LINK_DIFFERENT_ANONYMOUS_USER_ERROR -> Pair(
                getString(R.string.fui_email_link_different_anonymous_user_header),
                getString(R.string.fui_email_link_different_anonymous_user_message)
            )
            ErrorCodes.INVALID_EMAIL_LINK_ERROR -> Pair(
                getString(R.string.fui_email_link_invalid_link_header),
                getString(R.string.fui_email_link_invalid_link_message)
            )
            else -> Pair(
                getString(R.string.fui_email_link_wrong_device_header),
                getString(R.string.fui_email_link_wrong_device_message)
            )
        }
        return builder.setTitle(titleText)
            .setMessage(messageText)
            .setPositiveButton(R.string.fui_email_link_dismiss_button) { _, _ ->
                finish(errorCode, null)
            }
            .create()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, @Nullable data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == RequestCodes.EMAIL_LINK_PROMPT_FOR_EMAIL_FLOW ||
            requestCode == RequestCodes.EMAIL_LINK_CROSS_DEVICE_LINKING_FLOW
        ) {
            val response = IdpResponse.fromResultIntent(data)
            // CheckActionCode is called before starting this flow, so we only get here
            // if the sign in link is valid – it can only fail by being cancelled.
            if (resultCode == RESULT_OK) {
                finish(RESULT_OK, response?.toIntent())
            } else {
                finish(RESULT_CANCELED, null)
            }
        }
    }
}