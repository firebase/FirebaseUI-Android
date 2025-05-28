/*
 * Copyright 2025 Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.firebase.ui.auth.ui.credentials

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.lifecycle.ViewModelProvider
import com.firebase.ui.auth.IdpResponse
import com.firebase.ui.auth.data.model.FlowParameters
import com.firebase.ui.auth.data.model.Resource
import com.firebase.ui.auth.ui.InvisibleActivityBase
import com.firebase.ui.auth.util.ExtraConstants
import com.firebase.ui.auth.viewmodel.ResourceObserver
import com.firebase.ui.auth.viewmodel.credentialmanager.CredentialManagerHandler
import com.google.firebase.auth.FirebaseAuth

class CredentialSaveActivity : InvisibleActivityBase() {

    private lateinit var credentialManagerHandler: CredentialManagerHandler

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val response: IdpResponse? = intent.getParcelableExtra(ExtraConstants.IDP_RESPONSE)
        val emailExtra: String? = intent.getStringExtra(ExtraConstants.EMAIL)
        val password: String? = intent.getStringExtra(ExtraConstants.PASSWORD)

        credentialManagerHandler = ViewModelProvider(this)
            .get(CredentialManagerHandler::class.java)
            .apply {
                // Initialize with flow parameters.
                init(flowParams)
                // Pass the IdP response if present.
                response?.let { setResponse(it) }

                // Observe the operation's result.
                operation.observe(
                    this@CredentialSaveActivity,
                    object : ResourceObserver<IdpResponse>(this@CredentialSaveActivity) {
                        override fun onSuccess(response: IdpResponse) {
                            finish(RESULT_OK, response.toIntent())
                        }

                        override fun onFailure(e: Exception) {
                            // Even if saving fails, do not block the sign-in flow.
                            response?.let {
                                finish(RESULT_OK, it.toIntent())
                            } ?: finish(RESULT_OK, null)
                        }
                    }
                )
            }

        val currentOp: Resource<IdpResponse>? = credentialManagerHandler.operation.value

        if (currentOp == null) {
            Log.d(TAG, "Launching save operation.")
            // With the new CredentialManager, pass the email and password directly.
            val firebaseUser = FirebaseAuth.getInstance().currentUser
            val email = firebaseUser?.email ?: emailExtra

            credentialManagerHandler.saveCredentials(this, firebaseUser, email, password)
        } else {
            Log.d(TAG, "Save operation in progress, doing nothing.")
        }
    }

    companion object {
        private const val TAG = "CredentialSaveActivity"

        @JvmStatic
        fun createIntent(
            context: Context,
            flowParams: FlowParameters,
            email: String,
            password: String?,
            response: IdpResponse
        ): Intent {
            return createBaseIntent(context, CredentialSaveActivity::class.java, flowParams).apply {
                putExtra(ExtraConstants.EMAIL, email)
                putExtra(ExtraConstants.PASSWORD, password)
                putExtra(ExtraConstants.IDP_RESPONSE, response)
            }
        }
    }
}