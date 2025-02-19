/*
 * Copyright 2016 Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.firebase.ui.auth.ui.idp

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.view.isVisible
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.firebase.ui.auth.AuthMethodPickerLayout
import com.firebase.ui.auth.AuthUI
import com.firebase.ui.auth.AuthUI.IdpConfig
import com.firebase.ui.auth.ErrorCodes
import com.firebase.ui.auth.FirebaseAuthAnonymousUpgradeException
import com.firebase.ui.auth.FirebaseUiException
import com.firebase.ui.auth.IdpResponse
import com.firebase.ui.auth.KickoffActivity
import com.firebase.ui.auth.R
import com.firebase.ui.auth.data.model.FlowParameters
import com.firebase.ui.auth.data.model.Resource
import com.firebase.ui.auth.data.model.User
import com.firebase.ui.auth.data.model.UserCancellationException
import com.firebase.ui.auth.data.remote.AnonymousSignInHandler
import com.firebase.ui.auth.data.remote.EmailSignInHandler
import com.firebase.ui.auth.data.remote.FacebookSignInHandler
import com.firebase.ui.auth.data.remote.GenericIdpSignInHandler
import com.firebase.ui.auth.data.remote.GoogleSignInHandler
import com.firebase.ui.auth.data.remote.PhoneSignInHandler
import com.firebase.ui.auth.ui.AppCompatBase
import com.firebase.ui.auth.util.ExtraConstants
import com.firebase.ui.auth.util.data.PrivacyDisclosureUtils
import com.firebase.ui.auth.util.data.ProviderUtils
import com.firebase.ui.auth.viewmodel.ProviderSignInBase
import com.firebase.ui.auth.viewmodel.ResourceObserver
import com.firebase.ui.auth.viewmodel.idp.SocialProviderResponseHandler
import com.google.android.gms.auth.api.identity.BeginSignInRequest
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.auth.api.identity.SignInCredential
import com.google.android.gms.common.api.ApiException
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FacebookAuthProvider
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.PhoneAuthProvider
import kotlinx.coroutines.launch

// Imports for the new Credential Manager types (adjust these to match your library)
import androidx.credentials.Credential
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetPasswordOption
import androidx.credentials.PasswordCredential
import androidx.credentials.PublicKeyCredential
import androidx.credentials.exceptions.GetCredentialException

import com.firebase.ui.auth.AuthUI.EMAIL_LINK_PROVIDER
import com.firebase.ui.auth.util.ExtraConstants.GENERIC_OAUTH_BUTTON_ID
import com.firebase.ui.auth.util.ExtraConstants.GENERIC_OAUTH_PROVIDER_ID
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import com.google.firebase.auth.GoogleAuthCredential

@androidx.annotation.RestrictTo(androidx.annotation.RestrictTo.Scope.LIBRARY_GROUP)
class AuthMethodPickerActivity : AppCompatBase() {

    private lateinit var mHandler: SocialProviderResponseHandler
    private val mProviders: MutableList<ProviderSignInBase<*>> = mutableListOf()

    private var mProgressBar: ProgressBar? = null
    private var mProviderHolder: ViewGroup? = null

    private var customLayout: AuthMethodPickerLayout? = null

    // For demonstration, assume that CredentialManager provides a create() method.
    private val credentialManager by lazy {
        // Replace with your actual CredentialManager instance creation.
        CredentialManager.create(this)
    }
    
    companion object {
        private const val TAG = "AuthMethodPickerActivity"

        fun createIntent(context: Context, flowParams: FlowParameters): Intent {
            return createBaseIntent(context, AuthMethodPickerActivity::class.java, flowParams)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val params = flowParams
        customLayout = params.authMethodPickerLayout

        mHandler = ViewModelProvider(this).get(SocialProviderResponseHandler::class.java)
        mHandler.init(params)

        if (customLayout != null) {
            setContentView(customLayout!!.mainLayout)
            populateIdpListCustomLayout(params.providers)
        } else {
            setContentView(R.layout.fui_auth_method_picker_layout)
            mProgressBar = findViewById(R.id.top_progress_bar)
            mProviderHolder = findViewById(R.id.btn_holder)
            populateIdpList(params.providers)

            val logoId = params.logoId
            if (logoId == AuthUI.NO_LOGO) {
                findViewById<View>(R.id.logo).visibility = View.GONE

                val layout = findViewById<ConstraintLayout>(R.id.root)
                val constraints = ConstraintSet()
                constraints.clone(layout)
                constraints.setHorizontalBias(R.id.container, 0.5f)
                constraints.setVerticalBias(R.id.container, 0.5f)
                constraints.applyTo(layout)
            } else {
                val logo = findViewById<ImageView>(R.id.logo)
                logo.setImageResource(logoId)
            }
        }

        val tosAndPpConfigured = flowParams.isPrivacyPolicyUrlProvided() &&
                flowParams.isTermsOfServiceUrlProvided()

        val termsTextId = if (customLayout == null) {
            R.id.main_tos_and_pp
        } else {
            customLayout!!.tosPpView
        }

        if (termsTextId >= 0) {
            val termsText = findViewById<TextView>(termsTextId)
            if (!tosAndPpConfigured) {
                termsText.visibility = View.GONE
            } else {
                PrivacyDisclosureUtils.setupTermsOfServiceAndPrivacyPolicyText(this, flowParams, termsText)
            }
        }

        // Observe the social provider response handler.
        mHandler.operation.observe(this, object : ResourceObserver<IdpResponse>(this, R.string.fui_progress_dialog_signing_in) {
            override fun onSuccess(response: IdpResponse) {
                startSaveCredentials(mHandler.currentUser, response, null)
            }

            override fun onFailure(e: Exception) {
                when (e) {
                    is UserCancellationException -> {
                        // User pressed back – no error.
                    }
                    is FirebaseAuthAnonymousUpgradeException -> {
                        finish(ErrorCodes.ANONYMOUS_UPGRADE_MERGE_CONFLICT, e.response.toIntent())
                    }
                    is FirebaseUiException -> {
                        finish(RESULT_CANCELED, IdpResponse.from(e).toIntent())
                    }
                    else -> {
                        val text = getString(R.string.fui_error_unknown)
                        Toast.makeText(this@AuthMethodPickerActivity, text, Toast.LENGTH_SHORT).show()
                    }
                }
            }
        })

        // Attempt sign in using the new Credential Manager API.
        attemptCredentialSignIn()
    }

    /**
     * Attempts to sign in automatically using the Credential Manager API.
     */
    private fun attemptCredentialSignIn() {
        val args = flowParams
        val supportPasswords = ProviderUtils.getConfigFromIdps(args.providers, EmailAuthProvider.PROVIDER_ID) != null
        val accountTypes = getCredentialAccountTypes()
        val willRequestCredentials = supportPasswords || accountTypes.isNotEmpty()

        if (args.enableCredentials && willRequestCredentials) {
            // Build the new Credential Manager request.
            val getPasswordOption = GetPasswordOption()
            val googleIdOption = GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(true)
                .setServerClientId(getString(R.string.default_web_client_id))
                .build()
            val request = GetCredentialRequest(listOf(getPasswordOption, googleIdOption))

            lifecycleScope.launch {
                try {
                    val result = credentialManager.getCredential(
                        context = this@AuthMethodPickerActivity,
                        request = request
                    )
                    // Handle the returned credential.
                    handleCredentialManagerResult(result.credential)
                } catch (e: GetCredentialException) {
                    handleCredentialManagerFailure(e)
                    // Fallback: show the auth method picker.
                    showAuthMethodPicker()
                }
            }
        } else {
            showAuthMethodPicker()
        }
    }

    /**
     * Handles the credential returned from the Credential Manager.
     */
    private fun handleCredentialManagerResult(credential: Credential) {
        when (credential) {
            is PasswordCredential -> {
                val username = credential.id
                val password = credential.password
                val response = IdpResponse.Builder(
                    User.Builder(EmailAuthProvider.PROVIDER_ID, username).build()
                ).build()
                KickoffActivity.mKickstarter.setResult(Resource.forLoading())
                auth.signInWithEmailAndPassword(username, password)
                    .addOnSuccessListener { authResult ->
                        KickoffActivity.mKickstarter.handleSuccess(response, authResult)
                        finish()
                    }
                    .addOnFailureListener { e ->
                        if (e is FirebaseAuthInvalidUserException ||
                            e is FirebaseAuthInvalidCredentialsException) {
                            // Sign out via the new API.
                            Identity.getSignInClient(application).signOut()
                        }
                    }
            }
            is CustomCredential -> {
                if (credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                    try {
                        val googleIdTokenCredential = GoogleIdTokenCredential
                            .createFrom(credential.data)
                        auth.signInWithCredential(GoogleAuthProvider.getCredential(googleIdTokenCredential.idToken, null))
                            .addOnSuccessListener { authResult ->
                                val response = IdpResponse.Builder(
                                    User.Builder(GoogleAuthProvider.PROVIDER_ID, googleIdTokenCredential.id).build()
                                ).build()
                                KickoffActivity.mKickstarter.handleSuccess(response, authResult)
                                finish()
                            }
                            .addOnFailureListener { e ->
                                Log.e(TAG, "Failed to sign in with Google ID token", e)
                            }
                    } catch (e: GoogleIdTokenParsingException) {
                        Log.e(TAG, "Received an invalid google id token response", e)
                    }
                } else {
                    // Catch any unrecognized custom credential type here.
                    Log.e(TAG, "Unexpected type of credential")
                }
            }
            else -> {
                Log.e(TAG, "Unexpected type of credential")
            }
        }
    }

    /**
     * Example helper to extract a Google ID token from a PublicKeyCredential.
     * In your implementation you may need to parse the JSON response accordingly.
     */
    private fun extractGoogleIdToken(credential: PublicKeyCredential): String? {
        // TODO: Extract and return the Google ID token from credential.authenticationResponseJson.
        // For demonstration, we assume that authenticationResponseJson is the token.
        return credential.authenticationResponseJson
    }

    private fun handleCredentialManagerFailure(e: GetCredentialException) {
        Log.e(TAG, "Credential Manager sign in failed", e)
    }

    /**
     * Returns the account types to pass to the credential manager.
     */
    private fun getCredentialAccountTypes(): List<String> {
        val accounts = mutableListOf<String>()
        for (idpConfig in flowParams.providers) {
            if (idpConfig.providerId == GoogleAuthProvider.PROVIDER_ID) {
                accounts.add(ProviderUtils.providerIdToAccountType(idpConfig.providerId))
            }
        }
        return accounts
    }

    /**
     * Fallback – show the auth method picker UI.
     */
    private fun showAuthMethodPicker() {
        hideProgress()
    }

    private fun populateIdpList(providerConfigs: List<IdpConfig>) {
        // Clear any previous providers.
        mProviders.clear()
        for (idpConfig in providerConfigs) {
            val buttonLayout = when (idpConfig.providerId) {
                GoogleAuthProvider.PROVIDER_ID -> R.layout.fui_idp_button_google
                FacebookAuthProvider.PROVIDER_ID -> R.layout.fui_idp_button_facebook
                EMAIL_LINK_PROVIDER, EmailAuthProvider.PROVIDER_ID -> R.layout.fui_provider_button_email
                PhoneAuthProvider.PROVIDER_ID -> R.layout.fui_provider_button_phone
                AuthUI.ANONYMOUS_PROVIDER -> R.layout.fui_provider_button_anonymous
                else -> {
                    if (!TextUtils.isEmpty(idpConfig.params.getString(GENERIC_OAUTH_PROVIDER_ID))) {
                        idpConfig.params.getInt(GENERIC_OAUTH_BUTTON_ID)
                    } else {
                        throw IllegalStateException("Unknown provider: ${idpConfig.providerId}")
                    }
                }
            }
            val loginButton = layoutInflater.inflate(buttonLayout, mProviderHolder, false)
            handleSignInOperation(idpConfig, loginButton)
            mProviderHolder?.addView(loginButton)
        }
    }

    private fun populateIdpListCustomLayout(providerConfigs: List<IdpConfig>) {
        val providerButtonIds = customLayout?.providersButton ?: return
        for (idpConfig in providerConfigs) {
            val providerId = providerOrEmailLinkProvider(idpConfig.providerId)
            val buttonResId = providerButtonIds[providerId]
                ?: throw IllegalStateException("No button found for auth provider: ${idpConfig.providerId}")
            val loginButton = findViewById<View>(buttonResId)
            handleSignInOperation(idpConfig, loginButton)
        }
        // Hide custom layout buttons that don't have an associated provider.
        for ((providerBtnId, resId) in providerButtonIds) {
            if (providerBtnId == null) continue
            var hasProvider = false
            for (idpConfig in providerConfigs) {
                if (providerOrEmailLinkProvider(idpConfig.providerId) == providerBtnId) {
                    hasProvider = true
                    break
                }
            }
            if (!hasProvider) {
                findViewById<View>(resId)?.visibility = View.GONE
            }
        }
    }

    private fun providerOrEmailLinkProvider(providerId: String): String {
        return if (providerId == EmailAuthProvider.EMAIL_LINK_SIGN_IN_METHOD) {
            EmailAuthProvider.PROVIDER_ID
        } else providerId
    }

    private fun handleSignInOperation(idpConfig: IdpConfig, view: View) {
        val providerId = idpConfig.providerId
        val authUI = getAuthUI()
        val viewModelProvider = ViewModelProvider(this)
        val provider: ProviderSignInBase<*> = when (providerId) {
            EMAIL_LINK_PROVIDER, EmailAuthProvider.PROVIDER_ID ->
                viewModelProvider.get(EmailSignInHandler::class.java).initWith(null)
            PhoneAuthProvider.PROVIDER_ID ->
                viewModelProvider.get(PhoneSignInHandler::class.java).initWith(idpConfig)
            AuthUI.ANONYMOUS_PROVIDER ->
                viewModelProvider.get(AnonymousSignInHandler::class.java).initWith(flowParams)
            GoogleAuthProvider.PROVIDER_ID ->
                if (authUI.isUseEmulator) {
                    viewModelProvider.get(GenericIdpSignInHandler::class.java)
                        .initWith(GenericIdpSignInHandler.getGenericGoogleConfig())
                } else {
                    viewModelProvider.get(GoogleSignInHandler::class.java)
                        .initWith(GoogleSignInHandler.Params(idpConfig))
                }
            FacebookAuthProvider.PROVIDER_ID ->
                if (authUI.isUseEmulator) {
                    viewModelProvider.get(GenericIdpSignInHandler::class.java)
                        .initWith(GenericIdpSignInHandler.getGenericFacebookConfig())
                } else {
                    viewModelProvider.get(FacebookSignInHandler::class.java).initWith(idpConfig)
                }
            else -> {
                if (!TextUtils.isEmpty(idpConfig.params.getString(GENERIC_OAUTH_PROVIDER_ID))) {
                    viewModelProvider.get(GenericIdpSignInHandler::class.java).initWith(idpConfig)
                } else {
                    throw IllegalStateException("Unknown provider: $providerId")
                }
            }
        }

        mProviders.add(provider)

        provider.operation.observe(this, object : ResourceObserver<IdpResponse>(this) {
            override fun onSuccess(response: IdpResponse) {
                handleResponse(response)
            }

            override fun onFailure(e: Exception) {
                if (e is FirebaseAuthAnonymousUpgradeException) {
                    finish(
                        RESULT_CANCELED,
                        Intent().putExtra(ExtraConstants.IDP_RESPONSE, IdpResponse.from(e))
                    )
                    return
                }
                handleResponse(IdpResponse.from(e))
            }

            private fun handleResponse(response: IdpResponse) {
                // For social providers (unless using an emulator) use the social response handler.
                val isSocialResponse = AuthUI.SOCIAL_PROVIDERS.contains(providerId) && !authUI.isUseEmulator
                if (!response.isSuccessful) {
                    mHandler.startSignIn(response)
                } else if (isSocialResponse) {
                    mHandler.startSignIn(response)
                } else {
                    finish(if (response.isSuccessful) RESULT_OK else RESULT_CANCELED, response.toIntent())
                }
            }
        })

        view.setOnClickListener {
            if (isOffline()) {
                Snackbar.make(findViewById(android.R.id.content), getString(R.string.fui_no_internet), Snackbar.LENGTH_SHORT)
                    .show()
                return@setOnClickListener
            }
            provider.startSignIn(getAuth(), this@AuthMethodPickerActivity, idpConfig.providerId)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        mHandler.onActivityResult(requestCode, resultCode, data)
        for (provider in mProviders) {
            provider.onActivityResult(requestCode, resultCode, data)
        }
    }

    override fun showProgress(message: Int) {
        if (customLayout == null) {
            mProgressBar?.visibility = View.VISIBLE
            mProviderHolder?.let { holder ->
                for (i in 0 until holder.childCount) {
                    val child = holder.getChildAt(i)
                    child.isEnabled = false
                    child.alpha = 0.75f
                }
            }
        }
    }

    override fun hideProgress() {
        if (customLayout == null) {
            mProgressBar?.visibility = View.INVISIBLE
            mProviderHolder?.let { holder ->
                for (i in 0 until holder.childCount) {
                    val child = holder.getChildAt(i)
                    child.isEnabled = true
                    child.alpha = 1.0f
                }
            }
        }
    }
}