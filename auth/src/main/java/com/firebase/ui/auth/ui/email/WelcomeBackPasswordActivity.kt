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