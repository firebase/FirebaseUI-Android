package com.firebase.ui.auth.data.remote

import android.app.Activity
import android.app.Application
import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
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

class SignInKickstarter(application: Application?) : SignInViewModelBase(application) {
    private val app: Application = checkNotNull(application)

    fun start() {
        if (!TextUtils.isEmpty(arguments.emailLink)) {
            setResult(
                Resource.forFailure<IdpResponse>(
                    IntentRequiredException(
                        EmailLinkCatcherActivity.createIntent(
                            app,
                            arguments
                        ),
                        RequestCodes.EMAIL_FLOW
                    )
                )
            )
            return
        }

        startAuthMethodChoice()
    }

    private fun startAuthMethodChoice() {
        if (!arguments.shouldShowProviderChoice()) {
            val firstIdpConfig = arguments.defaultOrFirstProvider
            val firstProvider = firstIdpConfig.providerId
            when (firstProvider) {
                AuthUI.EMAIL_LINK_PROVIDER, EmailAuthProvider.PROVIDER_ID -> setResult(
                    Resource.forFailure<IdpResponse>(
                        IntentRequiredException(
                            EmailActivity.createIntent(app, arguments),
                            RequestCodes.EMAIL_FLOW
                        )
                    )
                )

                PhoneAuthProvider.PROVIDER_ID -> setResult(
                    Resource.forFailure<IdpResponse>(
                        IntentRequiredException(
                            PhoneActivity.createIntent(
                                app,
                                arguments, firstIdpConfig.params
                            ),
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
                        AuthMethodPickerActivity.createIntent(
                            app,
                            arguments
                        ),
                        RequestCodes.AUTH_PICKER_FLOW
                    )
                )
            )
        }
    }

    private fun redirectSignIn(provider: String, id: String?) {
        when (provider) {
            EmailAuthProvider.PROVIDER_ID -> setResult(
                Resource.forFailure<IdpResponse>(
                    IntentRequiredException(
                        EmailActivity.createIntent(app, arguments, id),
                        RequestCodes.EMAIL_FLOW
                    )
                )
            )

            PhoneAuthProvider.PROVIDER_ID -> {
                val args = Bundle()
                args.putString(ExtraConstants.PHONE, id)
                setResult(
                    Resource.forFailure<IdpResponse>(
                        IntentRequiredException(
                            PhoneActivity.createIntent(
                                app,
                                arguments, args
                            ),
                            RequestCodes.PHONE_FLOW
                        )
                    )
                )
            }

            else -> setResult(
                Resource.forFailure<IdpResponse>(
                    IntentRequiredException(
                        SingleSignInActivity.createIntent(
                            app, arguments,
                            User.Builder(provider, id).build()
                        ),
                        RequestCodes.PROVIDER_FLOW
                    )
                )
            )
        }
    }

    fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        when (requestCode) {
            RequestCodes.CRED_HINT -> if (resultCode == Activity.RESULT_OK && data != null) {
                try {
                    val signInClient = Identity.getSignInClient(app)
                    val credential = signInClient.getSignInCredentialFromIntent(data)
                    handleCredential(credential)
                } catch (e: ApiException) {
                    // Optionally log the error
                    startAuthMethodChoice()
                }
            } else {
                startAuthMethodChoice()
            }

            RequestCodes.EMAIL_FLOW, RequestCodes.AUTH_PICKER_FLOW, RequestCodes.PHONE_FLOW, RequestCodes.PROVIDER_FLOW -> {
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
                } else if (response.error!!.errorCode ==
                    ErrorCodes.ANONYMOUS_UPGRADE_MERGE_CONFLICT
                ) {
                    handleMergeFailure(response)
                } else {
                    setResult(
                        Resource.forFailure(
                            response.error!!
                        )
                    )
                }
            }
        }
    }

    /**
     * Minimal change: Adapted to work with the new SignInCredential.
     */
    private fun handleCredential(credential: SignInCredential) {
        val id = credential.id
        val password = credential.password
        if (TextUtils.isEmpty(password)) {
            // Instead of checking accountType, check for a Google ID token.
            val googleIdToken = credential.googleIdToken
            if (!TextUtils.isEmpty(googleIdToken)) {
                val response = IdpResponse.Builder(
                    User.Builder(GoogleAuthProvider.PROVIDER_ID, id).build()
                ).build()
                setResult(Resource.forLoading())
                auth.signInWithCredential(GoogleAuthProvider.getCredential(googleIdToken, null))
                    .addOnSuccessListener { authResult: AuthResult? ->
                        handleSuccess(
                            response,
                            authResult!!
                        )
                    }
                    .addOnFailureListener { e: Exception? -> startAuthMethodChoice() }
            } else {
                startAuthMethodChoice()
            }
        } else {
            val response = IdpResponse.Builder(
                User.Builder(EmailAuthProvider.PROVIDER_ID, id).build()
            ).build()
            setResult(Resource.forLoading())
            auth.signInWithEmailAndPassword(id, password!!)
                .addOnSuccessListener { authResult: AuthResult? ->
                    handleSuccess(
                        response,
                        authResult!!
                    )
                }
                .addOnFailureListener { e: Exception? ->
                    if (e is FirebaseAuthInvalidUserException ||
                        e is FirebaseAuthInvalidCredentialsException
                    ) {
                        // Minimal change: sign out using the new API (delete isn’t available).
                        Identity.getSignInClient(
                            app
                        )
                            .signOut()
                    }
                    startAuthMethodChoice()
                }
        }
    }

    companion object {
        private const val TAG = "SignInKickstarter"
    }
}
