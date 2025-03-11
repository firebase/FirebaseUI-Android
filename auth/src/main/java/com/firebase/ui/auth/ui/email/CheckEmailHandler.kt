package com.firebase.ui.auth.ui.email

import android.app.Activity
import android.app.Application
import android.app.PendingIntent
import android.content.Intent
import android.util.Log
import com.firebase.ui.auth.data.model.PendingIntentRequiredException
import com.firebase.ui.auth.data.model.Resource
import com.firebase.ui.auth.data.model.User
import com.firebase.ui.auth.util.data.ProviderUtils
import com.firebase.ui.auth.viewmodel.AuthViewModelBase
import com.firebase.ui.auth.viewmodel.RequestCodes
import com.google.android.gms.auth.api.identity.BeginSignInRequest
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.auth.api.identity.SignInCredential
import com.google.android.gms.auth.api.identity.SignInClient
import com.google.android.gms.common.api.ApiException
import androidx.annotation.Nullable

class CheckEmailHandler(application: Application) : AuthViewModelBase<User>(application) {
    companion object {
        private const val TAG = "CheckEmailHandler"
    }

    /**
     * Initiates a hint picker flow using the new Identity API.
     * This replaces the deprecated Credentials API call.
     */
    fun fetchCredential() {
        val signInClient: SignInClient = Identity.getSignInClient(getApplication())
        val signInRequest = BeginSignInRequest.builder()
            .setPasswordRequestOptions(
                BeginSignInRequest.PasswordRequestOptions.builder()
                    .setSupported(true)
                    .build()
            )
            .build()

        signInClient.beginSignIn(signInRequest)
            .addOnSuccessListener { result ->
                // The new API returns a PendingIntent to launch the hint picker.
                val pendingIntent: PendingIntent = result.pendingIntent
                setResult(
                    Resource.forFailure(
                        PendingIntentRequiredException(pendingIntent, RequestCodes.CRED_HINT)
                    )
                )
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "beginSignIn failed", e)
                setResult(Resource.forFailure(e))
            }
    }

    /**
     * Fetches the top provider for the given email.
     */
    fun fetchProvider(email: String) {
        setResult(Resource.forLoading())
        ProviderUtils.fetchTopProvider(getAuth(), getArguments(), email)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    setResult(Resource.forSuccess(User.Builder(task.result, email).build()))
                } else {
                    setResult(Resource.forFailure(task.exception ?: Exception("Unknown error")))
                }
            }
    }

    /**
     * Handles the result from the hint picker launched via the new Identity API.
     */
    fun onActivityResult(requestCode: Int, resultCode: Int, @Nullable data: Intent?) {
        if (requestCode != RequestCodes.CRED_HINT || resultCode != Activity.RESULT_OK) {
            return
        }

        setResult(Resource.forLoading())
        val signInClient: SignInClient = Identity.getSignInClient(getApplication())
        try {
            // Retrieve the SignInCredential from the returned intent.
            val credential: SignInCredential = signInClient.getSignInCredentialFromIntent(data)
            val email: String = credential.id

            ProviderUtils.fetchTopProvider(getAuth(), getArguments(), email)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        setResult(
                            Resource.forSuccess(
                                User.Builder(task.result, email)
                                    .setName(credential.displayName)
                                    .setPhotoUri(credential.profilePictureUri)
                                    .build()
                            )
                        )
                    } else {
                        setResult(Resource.forFailure(task.exception ?: Exception("Unknown error")))
                    }
                }
        } catch (e: ApiException) {
            Log.e(TAG, "getSignInCredentialFromIntent failed", e)
            setResult(Resource.forFailure(e))
        }
    }
}