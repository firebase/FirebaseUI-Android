package com.firebase.ui.auth.ui.idp

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.firebase.ui.auth.AuthUI
import com.firebase.ui.auth.AuthUI.IdpConfig
import com.firebase.ui.auth.ErrorCodes
import com.firebase.ui.auth.IdpResponse
import com.firebase.ui.auth.KickoffActivity
import com.firebase.ui.auth.R
import com.firebase.ui.auth.data.model.FlowParameters
import com.firebase.ui.auth.data.model.Resource
import com.firebase.ui.auth.data.model.User
import com.firebase.ui.auth.data.model.UserCancellationException
import com.firebase.ui.auth.data.remote.*
import com.firebase.ui.auth.ui.AppCompatBase
import com.firebase.ui.auth.util.ExtraConstants
import com.firebase.ui.auth.util.data.ProviderUtils
import com.firebase.ui.auth.viewmodel.ProviderSignInBase
import com.firebase.ui.auth.viewmodel.ResourceObserver
import com.firebase.ui.auth.viewmodel.idp.SocialProviderResponseHandler
import com.google.android.gms.auth.api.identity.Identity
import com.google.firebase.auth.*
import androidx.credentials.Credential
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetPasswordOption
import androidx.credentials.CustomCredential
import androidx.credentials.PasswordCredential
import androidx.credentials.exceptions.GetCredentialException
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import kotlinx.coroutines.launch
import com.firebase.ui.auth.FirebaseAuthAnonymousUpgradeException
import com.firebase.ui.auth.FirebaseUiException
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.style.TextAlign

@androidx.annotation.RestrictTo(androidx.annotation.RestrictTo.Scope.LIBRARY_GROUP)
class AuthMethodPickerActivity : AppCompatBase() {

    private lateinit var handler: SocialProviderResponseHandler
    private val providers = mutableListOf<ProviderSignInBase<*>>()
    private var showProgress by mutableStateOf(false)
    private val credentialManager by lazy { CredentialManager.create(this) }

    companion object {
        private const val TAG = "AuthMethodPickerActivity"
        @JvmStatic fun createIntent(ctx: Context, params: FlowParameters) =
            createBaseIntent(ctx, AuthMethodPickerActivity::class.java, params)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)

        val params = flowParams
        handler = ViewModelProvider(this)[SocialProviderResponseHandler::class.java].apply {
            init(params)
        }
        observeSocialHandler()

        setContent {
            Surface(Modifier.fillMaxSize()) {
                Box(Modifier.fillMaxSize()) {
                    Column(
                        Modifier
                            .fillMaxSize()
                            .systemBarsPadding()
                    ) {
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .height(120.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            if (flowParams.logoId != AuthUI.NO_LOGO) {
                                Image(
                                    painter = painterResource(flowParams.logoId),
                                    contentDescription = stringResource(R.string.fui_accessibility_logo),
                                    modifier = Modifier.size(100.dp)
                                )
                            }
                        }

                        Spacer(Modifier.weight(1f))

                        Column(
                            Modifier
                                .fillMaxWidth()
                                .verticalScroll(rememberScrollState())
                                .padding(horizontal = 24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            flowParams.providers.forEach { cfg ->
                                ProviderButton(cfg) { launchProviderFlow(cfg) }
                                Spacer(Modifier.height(12.dp))
                            }
                        }

                        if (flowParams.isPrivacyPolicyUrlProvided() &&
                            flowParams.isTermsOfServiceUrlProvided()
                        ) {
                            TermsAndPrivacyText(
                                tosUrl   = flowParams.termsOfServiceUrl!!,
                                ppUrl    = flowParams.privacyPolicyUrl!!,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp)
                            )
                        }
                    }

                    if (showProgress) {
                        LinearProgressIndicator(
                            Modifier
                                .fillMaxWidth()
                                .height(4.dp)
                                .align(Alignment.TopCenter)
                        )
                    }
                }
            }
        }

        attemptCredentialSignIn()
    }

    override fun showProgress(message: Int) { showProgress = true }
    override fun hideProgress()          { showProgress = false }

    private fun observeSocialHandler() {
        handler.operation.observe(
            this,
            object : ResourceObserver<IdpResponse>(this, R.string.fui_progress_dialog_signing_in) {
                override fun onSuccess(response: IdpResponse) =
                    startSaveCredentials(handler.currentUser, response, null)

                override fun onFailure(e: Exception) {
                    hideProgress()

                    when (e) {
                        is FirebaseAuthAnonymousUpgradeException ->
                            finish(ErrorCodes.ANONYMOUS_UPGRADE_MERGE_CONFLICT, e.response.toIntent())
                        is FirebaseUiException ->
                            finish(RESULT_CANCELED, IdpResponse.from(e).toIntent())
                        is UserCancellationException ->
                            Unit 
                        else ->
                            Toast.makeText(
                                this@AuthMethodPickerActivity,
                                getString(R.string.fui_error_unknown),
                                Toast.LENGTH_SHORT
                            ).show()
                    }
                }            }
        )
    }

    private fun launchProviderFlow(cfg: IdpConfig) {
        if (isOffline()) {
            Toast.makeText(this, getString(R.string.fui_no_internet), Toast.LENGTH_SHORT).show()
            return
        }
        getProviderForConfig(cfg).also { providers += it }
            .startSignIn(auth, this, cfg.providerId)
        showProgress = true
    }

    private fun getProviderForConfig(idp: IdpConfig): ProviderSignInBase<*> {
        val authUI = getAuthUI()
        val vm     = ViewModelProvider(this)
        val pid    = idp.providerId

        val provider = when (pid) {
            AuthUI.EMAIL_LINK_PROVIDER,
            EmailAuthProvider.PROVIDER_ID ->
                vm.get(EmailSignInHandler::class.java).initWith(null)
            PhoneAuthProvider.PROVIDER_ID ->
                vm.get(PhoneSignInHandler::class.java).initWith(idp)
            AuthUI.ANONYMOUS_PROVIDER ->
                vm.get(AnonymousSignInHandler::class.java).initWith(flowParams)
            GoogleAuthProvider.PROVIDER_ID ->
                if (authUI.isUseEmulator()) vm.get(GenericIdpSignInHandler::class.java)
                    .initWith(GenericIdpSignInHandler.getGenericGoogleConfig())
                else vm.get(GoogleSignInHandler::class.java).initWith(GoogleSignInHandler.Params(idp))
            FacebookAuthProvider.PROVIDER_ID ->
                if (authUI.isUseEmulator()) vm.get(GenericIdpSignInHandler::class.java)
                    .initWith(GenericIdpSignInHandler.getGenericFacebookConfig())
                else vm.get(FacebookSignInHandler::class.java).initWith(idp)
            else ->
                if (!TextUtils.isEmpty(idp.getParams().getString(ExtraConstants.GENERIC_OAUTH_PROVIDER_ID)))
                    vm.get(GenericIdpSignInHandler::class.java).initWith(idp)
                else throw IllegalStateException("Unknown provider $pid")
        }

        provider.operation.observe(this, object : ResourceObserver<IdpResponse>(this) {
            override fun onSuccess(r: IdpResponse) = handleResult(r, pid)
            override fun onFailure(e: Exception) {
                if (e is FirebaseAuthAnonymousUpgradeException) {
                    finish(RESULT_CANCELED,
                        Intent().putExtra(ExtraConstants.IDP_RESPONSE, IdpResponse.from(e)))
                } else handleResult(IdpResponse.from(e), pid)
            }
            private fun handleResult(r: IdpResponse, providerId: String) {
                showProgress = false
                val social = AuthUI.isSocialProvider(providerId) &&
                        !getAuthUI().isUseEmulator()
                if (!r.isSuccessful || social) handler.startSignIn(r)
                else finish(RESULT_OK, r.toIntent())
            }
        })
        return provider
    }

    private fun attemptCredentialSignIn() {
        val args       = flowParams
        val supportsPw = ProviderUtils
            .getConfigFromIdps(args.providers, EmailAuthProvider.PROVIDER_ID) != null

        if (!(args.enableCredentials && (supportsPw || args.providers.any {
                it.providerId == GoogleAuthProvider.PROVIDER_ID
            }))) return

        val request = GetCredentialRequest(
            listOf(
                GetPasswordOption(),
                com.google.android.libraries.identity.googleid
                    .GetGoogleIdOption.Builder()
                    .setFilterByAuthorizedAccounts(true)
                    .setServerClientId(getString(R.string.default_web_client_id))
                    .build()
            )
        )

        lifecycleScope.launch {
            try {
                val result = credentialManager.getCredential(this@AuthMethodPickerActivity, request)
                handleCredentialManagerResult(result.credential)
            } catch (e: GetCredentialException) {
                Log.w(TAG, "CredentialManager sign-in failed", e)
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        hideProgress()
    }

    private fun handleCredentialManagerResult(cred: Credential) {
        when (cred) {
            is PasswordCredential -> {
                val email = cred.id; val pw = cred.password
                KickoffActivity.mKickstarter.setResult(Resource.forLoading())
                auth.signInWithEmailAndPassword(email, pw)
                    .addOnSuccessListener { res ->
                        KickoffActivity.mKickstarter.handleSuccess(
                            IdpResponse.Builder(User.Builder(
                                EmailAuthProvider.PROVIDER_ID, email
                            ).build()).build(),
                            res
                        )
                        finish()
                    }
                    .addOnFailureListener {
                        if (it is FirebaseAuthInvalidUserException ||
                            it is FirebaseAuthInvalidCredentialsException
                        ) Identity.getSignInClient(application).signOut()
                    }
            }
            is CustomCredential -> {
                if (cred.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                    try {
                        val g = GoogleIdTokenCredential.createFrom(cred.data)
                        auth.signInWithCredential(
                            GoogleAuthProvider.getCredential(g.idToken, null)
                        ).addOnSuccessListener { res ->
                            KickoffActivity.mKickstarter.handleSuccess(
                                IdpResponse.Builder(User.Builder(
                                    GoogleAuthProvider.PROVIDER_ID,
                                    g.data.getString("email")
                                ).build()).setToken(g.idToken).build(),
                                res
                            )
                            finish()
                        }.addOnFailureListener {
                            Log.e(TAG, "Google token sign-in failed", it)
                        }
                    } catch (e: GoogleIdTokenParsingException) {
                        Log.e(TAG, "Bad GoogleIdTokenCredential", e)
                    }
                }
            }
            else -> Log.e(TAG, "Unhandled credential ${cred::class.java.simpleName}")
        }
    }

    @Composable
    private fun ProviderButton(cfg: IdpConfig, onClick: () -> Unit) {
        val (iconRes, bgColor, textColor) = when (cfg.providerId) {
            GoogleAuthProvider.PROVIDER_ID -> Triple(
                R.drawable.fui_ic_googleg_color_24dp,
                colorResource(R.color.fui_bgGoogle),
                Color(0xFF757575)
            )
            FacebookAuthProvider.PROVIDER_ID -> Triple(
                R.drawable.fui_ic_facebook_white_22dp,
                colorResource(R.color.fui_bgFacebook),
                Color.White
            )
            TwitterAuthProvider.PROVIDER_ID /* "twitter.com" */ -> Triple(
                R.drawable.fui_ic_twitter_bird_white_24dp,
                colorResource(R.color.fui_bgTwitter),
                Color.White
            )
            GithubAuthProvider.PROVIDER_ID /* "github.com" */ -> Triple(
                R.drawable.fui_ic_github_white_24dp,
                colorResource(R.color.fui_bgGitHub),
                Color.White
            )
            EmailAuthProvider.PROVIDER_ID,
            AuthUI.EMAIL_LINK_PROVIDER -> Triple(
                R.drawable.fui_ic_mail_white_24dp,
                colorResource(R.color.fui_bgEmail),
                Color.White
            )
            PhoneAuthProvider.PROVIDER_ID -> Triple(
                R.drawable.fui_ic_phone_white_24dp,
                colorResource(R.color.fui_bgPhone),
                Color.White
            )
            AuthUI.ANONYMOUS_PROVIDER -> Triple(
                R.drawable.fui_ic_anonymous_white_24dp,
                colorResource(R.color.fui_bgAnonymous),
                Color.White
            )
            AuthUI.MICROSOFT_PROVIDER /* "microsoft.com" */ -> Triple(
                R.drawable.fui_ic_microsoft_24dp,
                colorResource(R.color.fui_bgMicrosoft),
                Color.White
            )
            AuthUI.YAHOO_PROVIDER /* "yahoo.com" */ -> Triple(
                R.drawable.fui_ic_yahoo_24dp,
                colorResource(R.color.fui_bgYahoo),
                Color.White
            )
            AuthUI.APPLE_PROVIDER /* "apple.com" */ -> Triple(
                R.drawable.fui_ic_apple_white_24dp,
                colorResource(R.color.fui_bgApple),
                Color.White
            )
            else -> Triple(
                R.drawable.fui_ic_mail_white_24dp,
                colorResource(R.color.fui_bgEmail),
                Color.White
            )
        }

        val label = when (cfg.providerId) {
            GoogleAuthProvider.PROVIDER_ID ->
                stringResource(R.string.fui_sign_in_with_google)
            FacebookAuthProvider.PROVIDER_ID ->
                stringResource(R.string.fui_sign_in_with_facebook)
            TwitterAuthProvider.PROVIDER_ID ->
                stringResource(R.string.fui_sign_in_with_twitter)
            GithubAuthProvider.PROVIDER_ID ->
                stringResource(R.string.fui_sign_in_with_github)
            EmailAuthProvider.PROVIDER_ID,
            AuthUI.EMAIL_LINK_PROVIDER ->
                stringResource(R.string.fui_sign_in_with_email)
            PhoneAuthProvider.PROVIDER_ID ->
                stringResource(R.string.fui_sign_in_with_phone)
            AuthUI.ANONYMOUS_PROVIDER ->
                stringResource(R.string.fui_sign_in_anonymously)
            AuthUI.MICROSOFT_PROVIDER ->
                stringResource(R.string.fui_sign_in_with_microsoft)
            AuthUI.YAHOO_PROVIDER ->
                stringResource(R.string.fui_sign_in_with_yahoo)
            AuthUI.APPLE_PROVIDER ->
                stringResource(R.string.fui_sign_in_with_apple)
            else -> cfg.providerId
        }

        Button(
            onClick = onClick,
            colors = ButtonDefaults.buttonColors(
                containerColor = bgColor,
                contentColor   = textColor
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
        ) {
            Icon(
                painter           = painterResource(iconRes),
                contentDescription = null
            )
            Spacer(Modifier.width(24.dp))
            Text(text = label)
        }
    }

    @Composable
    private fun TermsAndPrivacyText(
        tosUrl: String,
        ppUrl: String,
        modifier: Modifier = Modifier
    ) {
        val tosLabel = stringResource(R.string.fui_terms_of_service)
        val ppLabel  = stringResource(R.string.fui_privacy_policy)

        val fullText = stringResource(
            R.string.fui_tos_and_pp,
            tosLabel,
            ppLabel
        )

        val tosStart = fullText.indexOf(tosLabel).coerceAtLeast(0)
        val tosEnd   = tosStart + tosLabel.length
        val ppStart  = fullText.indexOf(ppLabel).coerceAtLeast(0)
        val ppEnd    = ppStart + ppLabel.length

        val annotated = buildAnnotatedString {
            append(fullText)

            addStyle(
                style = SpanStyle(fontWeight = FontWeight.Bold),
                start = tosStart,
                end   = tosEnd
            )
            addStringAnnotation(
                tag    = "URL",
                annotation = tosUrl,
                start  = tosStart,
                end    = tosEnd
            )

            addStyle(
                style = SpanStyle(fontWeight = FontWeight.Bold),
                start = ppStart,
                end   = ppEnd
            )
            addStringAnnotation(
                tag    = "URL",
                annotation = ppUrl,
                start  = ppStart,
                end    = ppEnd
            )
        }

        val uriHandler = LocalUriHandler.current
        ClickableText(
            text = annotated,
            style = MaterialTheme.typography.bodySmall.copy(textAlign = TextAlign.Center),
            modifier = modifier,
            onClick = { offset ->
                annotated
                    .getStringAnnotations(tag = "URL", start = offset, end = offset)
                    .firstOrNull()
                    ?.let { uriHandler.openUri(it.item) }
            }
        )
    }
}