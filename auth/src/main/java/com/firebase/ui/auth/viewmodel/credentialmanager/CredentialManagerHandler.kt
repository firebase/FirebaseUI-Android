package com.firebase.ui.auth.viewmodel.credentialmanager

import android.app.Application
import android.content.Context
import androidx.lifecycle.viewModelScope
import androidx.credentials.CreatePasswordRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CreateCredentialResponse
import androidx.credentials.exceptions.CreateCredentialException
import com.firebase.ui.auth.ErrorCodes
import com.firebase.ui.auth.FirebaseUiException
import com.firebase.ui.auth.IdpResponse
import com.firebase.ui.auth.data.model.Resource
import com.firebase.ui.auth.viewmodel.AuthViewModelBase
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.launch

class CredentialManagerHandler(application: Application) :
    AuthViewModelBase<IdpResponse>(application) {

    private val credentialManager = CredentialManager.create(application)
    private var response: IdpResponse? = null

    fun setResponse(newResponse: IdpResponse) {
        response = newResponse
    }

    /**
     * Saves credentials via Credential Manager if enabled in [getArguments().enableCredentials].
     * Uses a password-based credential for demonstration; adapt to passkeys or other flows as needed.
     */
    fun saveCredentials(
        context: Context,
        firebaseUser: FirebaseUser?,
        password: String?
    ) {
        if (!arguments.enableCredentials) {
            setResult(Resource.forSuccess(response!!))
            return
        }
        setResult(Resource.forLoading())

        if (firebaseUser == null || firebaseUser.email.isNullOrEmpty() || password.isNullOrEmpty()) {
            setResult(
                Resource.forFailure(
                    FirebaseUiException(
                        ErrorCodes.UNKNOWN_ERROR,
                        "Invalid FirebaseUser or missing password."
                    )
                )
            )
            return
        }

        // Example: Password credential with the user's email as the identifier
        val request = CreatePasswordRequest(
            id = firebaseUser.email!!,
            password = password
        )

        viewModelScope.launch {
            try {
                // Use the createCredential function and store the response
                val createResponse: CreateCredentialResponse =
                    credentialManager.createCredential(context, request)

                // If the response is successful, set the success result
                if (createResponse != null) {
                    setResult(Resource.forSuccess(response!!))
                } else {
                    setResult(
                        Resource.forFailure(
                            FirebaseUiException(
                                ErrorCodes.UNKNOWN_ERROR,
                                "Received null response from Credential Manager."
                            )
                        )
                    )
                }
            } catch (e: CreateCredentialException) {
                setResult(
                    Resource.forFailure(
                        FirebaseUiException(
                            ErrorCodes.UNKNOWN_ERROR,
                            "Error saving credential with Credential Manager.",
                            e
                        )
                    )
                )
            } catch (e: Exception) {
                setResult(
                    Resource.forFailure(
                        FirebaseUiException(
                            ErrorCodes.UNKNOWN_ERROR,
                            "Unexpected error saving credential.",
                            e
                        )
                    )
                )
            }
        }
    }
}