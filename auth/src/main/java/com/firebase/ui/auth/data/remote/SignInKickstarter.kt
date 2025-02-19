package com.firebase.ui.auth.data.remote

import android.app.Activity
import android.app.Application
import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import com.firebase.ui.auth.AuthUI
import com.firebase.ui.auth.ErrorCodes
import com.firebase.ui.auth.IdpResponse
import com.firebase.ui.auth.data.model.IntentRequiredException
import com.firebase.ui.auth.data.model.Resource
import com.firebase.ui.auth.data.model.User
import com.firebase.ui.auth.data.model.UserCancellationException
import com.firebase.ui.auth.ui.email.EmailActivity
import com.firebase.ui.auth.ui.email.EmailLinkCatcherActivity
import com.firebase.ui.auth.ui.idp.AuthMethodPickerActivity
import com.firebase.ui.auth.ui.idp.SingleSignInActivity
import com.firebase.ui.auth.ui.phone.PhoneActivity
import com.firebase.ui.auth.util.ExtraConstants
import com.firebase.ui.auth.viewmodel.RequestCodes
import com.firebase.ui.auth.viewmodel.SignInViewModelBase
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.auth.api.identity.SignInCredential
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.PhoneAuthProvider
import kotlinx.coroutines.launch
import androidx.lifecycle.viewModelScope

import androidx.credentials.Credential
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetPasswordOption
import androidx.credentials.PasswordCredential
import androidx.credentials.PublicKeyCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException


private const val TAG = "SignInKickstarter"

class SignInKickstarter(application: Application?) : SignInViewModelBase(application) {

    private val app: Application = checkNotNull(application)

    /**
     * Entry point. If an email link is detected, immediately launch the email catcher.
     * Otherwise, launch startAuthMethodChoice.
     */
    fun start() {
        if (!TextUtils.isEmpty(arguments.emailLink)) {
            setResult(
                Resource.forFailure<IdpResponse>(
                    IntentRequiredException(
                        EmailLinkCatcherActivity.createIntent(app, arguments),
                        RequestCodes.EMAIL_FLOW
                    )
                )
            )
            return
        }
        startAuthMethodChoice()
    }


    /**
     * Fallback: if no credential was obtained (or after a failed Credential Manager attempt)
     * choose the proper sign‑in flow.
     */
    private fun startAuthMethodChoice() {
        if (!arguments.shouldShowProviderChoice()) {
            val firstIdpConfig = arguments.defaultOrFirstProvider
            val firstProvider = firstIdpConfig.providerId
            when (firstProvider) {
                AuthUI.EMAIL_LINK_PROVIDER, EmailAuthProvider.PROVIDER_ID ->
                    setResult(
                        Resource.forFailure<IdpResponse>(
                            IntentRequiredException(
                                EmailActivity.createIntent(app, arguments),
                                RequestCodes.EMAIL_FLOW
                            )
                        )
                    )
                PhoneAuthProvider.PROVIDER_ID ->
                    setResult(
                        Resource.forFailure<IdpResponse>(
                            IntentRequiredException(
                                PhoneActivity.createIntent(app, arguments, firstIdpConfig.params),
                                RequestCodes.PHONE_FLOW
                            )
                        )
                    )
                else -> redirectSignIn(firstProvider, null)
            }
        } else {
            setResult(
                Resource.forFailure<IdpResponse>(
                    IntentRequiredException(
                        AuthMethodPickerActivity.createIntent(app, arguments),
                        RequestCodes.AUTH_PICKER_FLOW
                    )
                )
            )
        }
    }

    /**
     * Helper to route to the proper sign‑in activity for a given provider.
     */
    private fun redirectSignIn(provider: String, id: String?) {
        when (provider) {
            EmailAuthProvider.PROVIDER_ID ->
                setResult(
                    Resource.forFailure<IdpResponse>(
                        IntentRequiredException(
                            EmailActivity.createIntent(app, arguments, id),
                            RequestCodes.EMAIL_FLOW
                        )
                    )
                )
            PhoneAuthProvider.PROVIDER_ID -> {
                val args = Bundle().apply { putString(ExtraConstants.PHONE, id) }
                setResult(
                    Resource.forFailure<IdpResponse>(
                        IntentRequiredException(
                            PhoneActivity.createIntent(app, arguments, args),
                            RequestCodes.PHONE_FLOW
                        )
                    )
                )
            }
            else ->
                setResult(
                    Resource.forFailure<IdpResponse>(
                        IntentRequiredException(
                            SingleSignInActivity.createIntent(
                                app, arguments, User.Builder(provider, id).build()
                            ),
                            RequestCodes.PROVIDER_FLOW
                        )
                    )
                )
        }
    }

    /**
     * Legacy onActivityResult handler for other flows.
     */
    fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        when (requestCode) {
            RequestCodes.EMAIL_FLOW,
            RequestCodes.AUTH_PICKER_FLOW,
            RequestCodes.PHONE_FLOW,
            RequestCodes.PROVIDER_FLOW -> {
                if (resultCode == RequestCodes.EMAIL_LINK_WRONG_DEVICE_FLOW ||
                    resultCode == RequestCodes.EMAIL_LINK_INVALID_LINK_FLOW
                ) {
                    startAuthMethodChoice()
                    return
                }
                val response = IdpResponse.fromResultIntent(data)
                if (response == null) {
                    setResult(Resource.forFailure(UserCancellationException()))
                } else if (response.isSuccessful) {
                    setResult(Resource.forSuccess(response))
                } else if (response.error!!.errorCode == ErrorCodes.ANONYMOUS_UPGRADE_MERGE_CONFLICT) {
                    handleMergeFailure(response)
                } else {
                    setResult(Resource.forFailure(response.error!!))
                }
            }
            else -> startAuthMethodChoice()
        }
    }

    /**
     * Handle a successfully returned Credential from the Credential Manager.
     */
    private fun handleCredentialManagerResult(credential: Credential) {
        when (credential) {
            is PasswordCredential -> {
                val username = credential.id
                val password = credential.password
                val response = IdpResponse.Builder(
                    User.Builder(EmailAuthProvider.PROVIDER_ID, username).build()
                ).build()
                setResult(Resource.forLoading())
                auth.signInWithEmailAndPassword(username, password)
                    .addOnSuccessListener { authResult: AuthResult ->
                        handleSuccess(response, authResult)
                        // (Optionally finish the hosting activity here.)
                    }
                    .addOnFailureListener { e ->
                        if (e is FirebaseAuthInvalidUserException ||
                            e is FirebaseAuthInvalidCredentialsException
                        ) {
                            // Sign out using the new API.
                            Identity.getSignInClient(app).signOut()
                        }
                        startAuthMethodChoice()
                    }
            }
            is CustomCredential -> {
                if (credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                    try {
                        val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                        auth.signInWithCredential(
                            GoogleAuthProvider.getCredential(googleIdTokenCredential.idToken, null)
                        )
                            .addOnSuccessListener { authResult: AuthResult ->
                                val response = IdpResponse.Builder(
                                    User.Builder(
                                        GoogleAuthProvider.PROVIDER_ID,
                                        // Assume the credential data contains the email.
                                        googleIdTokenCredential.data.getString("email")
                                    ).build()
                                )
                                    .setToken(googleIdTokenCredential.idToken)
                                    .build()
                                handleSuccess(response, authResult)
                            }
                            .addOnFailureListener { e ->
                                Log.e(TAG, "Failed to sign in with Google ID token", e)
                                startAuthMethodChoice()
                            }
                    } catch (e: GoogleIdTokenParsingException) {
                        Log.e(TAG, "Received an invalid google id token response", e)
                        startAuthMethodChoice()
                    }
                } else {
                    Log.e(TAG, "Unexpected type of credential")
                    startAuthMethodChoice()
                }
            }
            else -> {
                Log.e(TAG, "Unexpected type of credential")
                startAuthMethodChoice()
            }
        }
    }
}