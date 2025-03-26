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

    private val credentialManager = GoogleApiUtils.getCredentialManager(application)
    private var response: IdpResponse? = null

    fun setResponse(newResponse: IdpResponse) {
        response = newResponse
    }

    /**
     * Saves credentials via Credential Manager if enabled in [getArguments().enableCredentials].
     * Uses a password-based credential for demonstration; adapt to passkeys or other flows as needed.
     *
     * @param context the Context to use.
     * @param firebaseUser the current FirebaseUser.
     * @param email the email to use as the identifier.
     * @param password the password used for sign-in.
     */
    fun saveCredentials(
        context: Context,
        firebaseUser: FirebaseUser?,
        email: String?,
        password: String?
    ) {
        if (!arguments.enableCredentials) {
            setResult(Resource.forSuccess(response!!))
            return
        }
        setResult(Resource.forLoading())

        if (firebaseUser == null || email.isNullOrEmpty() || password.isNullOrEmpty()) {
            setResult(
                Resource.forFailure(
                    FirebaseUiException(
                        ErrorCodes.UNKNOWN_ERROR,
                        "Invalid FirebaseUser or missing email/password."
                    )
                )
            )
            return
        }

        // Create a password-based credential using the provided email as the identifier.
        val request = CreatePasswordRequest(
            id = email,
            password = password
        )

        viewModelScope.launch {
            try {
                // Use the CredentialManager to create (i.e., save) the credential.
                val createResponse: CreateCredentialResponse =
                    credentialManager.createCredential(context, request)

                // If successful, report success.
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