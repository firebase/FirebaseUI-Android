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

// src/main/java/com/firebase/ui/auth/ui/email/WelcomeBackPasswordActivity.kt
package com.firebase.ui.auth.ui.email

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.ui.Modifier
import com.firebase.ui.auth.IdpResponse
import com.firebase.ui.auth.data.model.FlowParameters
import com.firebase.ui.auth.viewmodel.email.WelcomeBackPasswordViewModel
import com.firebase.ui.auth.viewmodel.email.WelcomeBackPasswordViewModelFactory
import com.firebase.ui.auth.ui.email.WelcomeBackPasswordPrompt

class WelcomeBackPasswordActivity : ComponentActivity() {
    companion object {
        private const val EXTRA_FLOW_PARAMS = "extra_flow_params"
        private const val EXTRA_IDP_RESPONSE = "extra_idp_response"

        @JvmStatic
        fun createIntent(
            context: Context,
            flowParams: FlowParameters,
            response: IdpResponse
        ): Intent = Intent(context, WelcomeBackPasswordActivity::class.java).apply {
            putExtra(EXTRA_FLOW_PARAMS, flowParams)
            putExtra(EXTRA_IDP_RESPONSE, response)
        }
    }

    private lateinit var viewModel: WelcomeBackPasswordViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val flowParams = intent
            .getParcelableExtra<FlowParameters>(EXTRA_FLOW_PARAMS)
            ?: error("Missing flow parameters")
        val response = intent
            .getParcelableExtra<IdpResponse>(EXTRA_IDP_RESPONSE)
            ?: error("Missing IdpResponse")

        viewModel = WelcomeBackPasswordViewModelFactory(application, flowParams)
            .create(WelcomeBackPasswordViewModel::class.java)

        // This is the ComposeActivity.setContent extension:
        setContent {
            WelcomeBackPasswordPrompt(
                flowParameters = flowParams,
                email = response.email!!,
                idpResponse = response,
                onSignInSuccess = {
                    setResult(RESULT_OK, response.toIntent())
                    finish()
                },
                onSignInError = { e ->
                    setResult(RESULT_CANCELED, IdpResponse.getErrorIntent(e))
                    finish()
                },
                onForgotPassword = {
                },
                viewModel = viewModel
            )
        }
    }
}