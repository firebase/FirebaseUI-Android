package com.firebase.ui.auth.viewmodel.credentialmanager

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.credentials.CreatePasswordRequest
import androidx.credentials.CredentialManager
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
        activity: androidx.activity.ComponentActivity,
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
                credentialManager.createCredential(activity, request)
                setResult(Resource.forSuccess(response!!))
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