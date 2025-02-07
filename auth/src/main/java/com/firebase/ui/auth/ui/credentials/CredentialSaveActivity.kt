package com.firebase.ui.auth.ui.credentials

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import com.firebase.ui.auth.IdpResponse
import com.firebase.ui.auth.data.model.FlowParameters
import com.firebase.ui.auth.data.model.Resource
import com.firebase.ui.auth.ui.InvisibleActivityBase
import com.firebase.ui.auth.util.ExtraConstants
import com.firebase.ui.auth.viewmodel.ResourceObserver
import com.firebase.ui.auth.viewmodel.credentialmanager.CredentialManagerHandler
import com.google.android.gms.auth.api.credentials.Credential
import androidx.lifecycle.ViewModelProvider
import com.google.firebase.auth.FirebaseAuth

class CredentialSaveActivity : InvisibleActivityBase() {

    

    private lateinit var credentialManagerHandler: CredentialManagerHandler

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val response: IdpResponse? = intent.getParcelableExtra(ExtraConstants.IDP_RESPONSE)
        val credential: Credential? = intent.getParcelableExtra(ExtraConstants.CREDENTIAL)

        credentialManagerHandler = ViewModelProvider(this)
            .get(CredentialManagerHandler::class.java)
            .apply {
                // Initialize with flow parameters
                init(flowParams)
                // If we have an IdpResponse, set it so subsequent operations can report results
                response?.let { setResponse(it) }

                // Observe the operation resource
                operation.observe(
                    this@CredentialSaveActivity,
                    object : ResourceObserver<IdpResponse>(this@CredentialSaveActivity) {
                        override fun onSuccess(response: IdpResponse) {
                            // Done saving – success
                            finish(RESULT_OK, response.toIntent())
                        }

                        override fun onFailure(e: Exception) {
                            // We don’t want to block the sign-in flow just because saving failed,
                            // so return RESULT_OK
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
            // In the old SmartLock flow, you saved a `Credential`;
            // with CredentialManager, we typically need email & password for the new request.
            // Example usage: pass the current user & the password.
            // Adjust as needed for passkeys or other flows.
            val firebaseUser = FirebaseAuth.getInstance().currentUser
            val password = credential?.password

            credentialManagerHandler.saveCredentials(this, firebaseUser, password)
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
            credential: Credential,
            response: IdpResponse
        ): Intent {
            return createBaseIntent(context, CredentialSaveActivity::class.java, flowParams).apply {
                putExtra(ExtraConstants.CREDENTIAL, credential)
                putExtra(ExtraConstants.IDP_RESPONSE, response)
            }
        }
    }
}