/*
 * Copyright 2016 Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.firebase.ui.auth

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Parcel
import android.os.Parcelable
import android.util.Log
import androidx.annotation.CallSuper
import androidx.annotation.DrawableRes
import androidx.annotation.NonNull
import androidx.annotation.Nullable
import androidx.annotation.RestrictTo
import androidx.annotation.StringDef
import androidx.annotation.StyleRes
import com.facebook.login.LoginManager
import com.firebase.ui.auth.data.model.FlowParameters
import com.firebase.ui.auth.ui.idp.AuthMethodPickerActivity
import com.firebase.ui.auth.util.CredentialUtils
import com.firebase.ui.auth.util.ExtraConstants
import com.firebase.ui.auth.util.GoogleApiUtils
import com.firebase.ui.auth.util.Preconditions
import com.firebase.ui.auth.util.data.PhoneNumberUtils
import com.firebase.ui.auth.util.data.ProviderAvailability
import com.firebase.ui.auth.util.data.ProviderUtils
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.Scope
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.ActionCodeSettings
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FacebookAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.GithubAuthProvider
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.PhoneAuthProvider
import com.google.firebase.auth.TwitterAuthProvider
import java.util.IdentityHashMap
import java.util.*
import com.google.android.gms.common.api.CommonStatusCodes

class AuthUI private constructor(private val mApp: FirebaseApp) {

    private val mAuth: FirebaseAuth = FirebaseAuth.getInstance(mApp)
    private var mEmulatorHost: String? = null
    private var mEmulatorPort = -1

    init {
        try {
            mAuth.setFirebaseUIVersion(BuildConfig.VERSION_NAME)
        } catch (e: Exception) {
            Log.e(TAG, "Couldn't set the FUI version.", e)
        }
        mAuth.useAppLanguage()
    }

    fun getApp(): FirebaseApp = mApp

    fun getAuth(): FirebaseAuth = mAuth

    fun signOut(context: Context): Task<Void> {
        val playServicesAvailable = GoogleApiUtils.isPlayServicesAvailable(context)
        if (!playServicesAvailable) {
            Log.w(TAG, "Google Play services not available during signOut")
        }
        return signOutIdps(context).continueWith { task ->
            task.result // propagate exceptions if any.
            mAuth.signOut()
            null
        }
    }

    fun delete(context: Context): Task<Void> {
        val currentUser = mAuth.currentUser
        if (currentUser == null) {
            return Tasks.forException(
                FirebaseAuthInvalidUserException(
                    CommonStatusCodes.SIGN_IN_REQUIRED.toString(),
                    "No currently signed in user."
                )
            )
        }
        return signOutIdps(context).continueWithTask { task ->
            task.result // propagate exception if any.
            currentUser.delete()
        }
    }

    fun useEmulator(host: String, port: Int) {
        Preconditions.checkArgument(port >= 0, "Port must be >= 0")
        Preconditions.checkArgument(port <= 65535, "Port must be <= 65535")
        mEmulatorHost = host
        mEmulatorPort = port

        mAuth.useEmulator(host, port)
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    fun isUseEmulator(): Boolean = mEmulatorHost != null && mEmulatorPort >= 0

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    fun getEmulatorHost(): String? = mEmulatorHost

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    fun getEmulatorPort(): Int = mEmulatorPort

    private fun signOutIdps(context: Context): Task<Void> {
        if (ProviderAvailability.IS_FACEBOOK_AVAILABLE) {
            LoginManager.getInstance().logOut()
        }
        return if (GoogleApiUtils.isPlayServicesAvailable(context)) {
            GoogleSignIn.getClient(context, GoogleSignInOptions.DEFAULT_SIGN_IN).signOut()
        } else {
            Tasks.forResult(null)
        }
    }

    fun createSignInIntentBuilder(): SignInIntentBuilder {
        return SignInIntentBuilder()
    }

    @StringDef(
        GoogleAuthProvider.PROVIDER_ID,
        FacebookAuthProvider.PROVIDER_ID,
        TwitterAuthProvider.PROVIDER_ID,
        GithubAuthProvider.PROVIDER_ID,
        EmailAuthProvider.PROVIDER_ID,
        PhoneAuthProvider.PROVIDER_ID,
        ANONYMOUS_PROVIDER,
        EmailAuthProvider.EMAIL_LINK_SIGN_IN_METHOD
    )
    @Retention(AnnotationRetention.SOURCE)
    annotation class SupportedProvider

    class IdpConfig private constructor(private val mProviderId: String, private val mParams: Bundle) :
        Parcelable {

        val providerId: String
            get() = mProviderId

        fun getParams(): Bundle = Bundle(mParams)

        override fun describeContents(): Int = 0

        override fun writeToParcel(parcel: Parcel, flags: Int) {
            parcel.writeString(mProviderId)
            parcel.writeBundle(mParams)
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other == null || javaClass != other.javaClass) return false
            val config = other as IdpConfig
            return mProviderId == config.mProviderId
        }

        override fun hashCode(): Int = mProviderId.hashCode()

        override fun toString(): String {
            return "IdpConfig{mProviderId='$mProviderId', mParams=$mParams}"
        }

        companion object {
            @JvmField
            val CREATOR: Parcelable.Creator<IdpConfig> = object : Parcelable.Creator<IdpConfig> {
                override fun createFromParcel(parcel: Parcel): IdpConfig {
                    return IdpConfig(parcel)
                }

                override fun newArray(size: Int): Array<IdpConfig?> = arrayOfNulls(size)
            }
        }

        private constructor(parcel: Parcel) : this(
            parcel.readString() ?: "",
            parcel.readBundle(IdpConfig::class.java.classLoader) ?: Bundle()
        )

        open class Builder(@NonNull @SupportedProvider providerId: String) {
            protected val mParams: Bundle = Bundle()
            protected var mProviderId: String = providerId

            @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
            protected fun getParams(): Bundle = mParams

            @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
            protected fun setProviderId(providerId: String) {
                mProviderId = providerId
            }

            @CallSuper
            open fun build(): IdpConfig = IdpConfig(mProviderId, mParams)
        }

        class EmailBuilder : Builder(EmailAuthProvider.PROVIDER_ID) {
            fun setAllowNewAccounts(allow: Boolean): EmailBuilder {
                getParams().putBoolean(ExtraConstants.ALLOW_NEW_EMAILS, allow)
                return this
            }

            fun setRequireName(requireName: Boolean): EmailBuilder {
                getParams().putBoolean(ExtraConstants.REQUIRE_NAME, requireName)
                return this
            }

            fun enableEmailLinkSignIn(): EmailBuilder {
                setProviderId(EMAIL_LINK_PROVIDER)
                return this
            }

            fun setActionCodeSettings(actionCodeSettings: ActionCodeSettings): EmailBuilder {
                getParams().putParcelable(ExtraConstants.ACTION_CODE_SETTINGS, actionCodeSettings)
                return this
            }

            fun setForceSameDevice(): EmailBuilder {
                getParams().putBoolean(ExtraConstants.FORCE_SAME_DEVICE, true)
                return this
            }

            fun setDefaultEmail(email: String): EmailBuilder {
                getParams().putString(ExtraConstants.DEFAULT_EMAIL, email)
                return this
            }

            override fun build(): IdpConfig {
                if (mProviderId == EMAIL_LINK_PROVIDER) {
                    val actionCodeSettings: ActionCodeSettings? =
                        getParams().getParcelable(ExtraConstants.ACTION_CODE_SETTINGS)
                    Preconditions.checkNotNull(
                        actionCodeSettings,
                        "ActionCodeSettings cannot be null when using email link sign in."
                    )
                    if (!actionCodeSettings!!.canHandleCodeInApp()) {
                        throw IllegalStateException(
                            "You must set canHandleCodeInApp in your ActionCodeSettings to true for Email-Link Sign-in."
                        )
                    }
                }
                return super.build()
            }
        }

        class PhoneBuilder : Builder(PhoneAuthProvider.PROVIDER_ID) {
            fun setDefaultNumber(number: String): PhoneBuilder {
                Preconditions.checkUnset(
                    getParams(),
                    "Cannot overwrite previously set phone number",
                    ExtraConstants.PHONE,
                    ExtraConstants.COUNTRY_ISO,
                    ExtraConstants.NATIONAL_NUMBER
                )
                if (!PhoneNumberUtils.isValid(number)) {
                    throw IllegalStateException("Invalid phone number: $number")
                }
                getParams().putString(ExtraConstants.PHONE, number)
                return this
            }

            fun setDefaultNumber(iso: String, number: String): PhoneBuilder {
                Preconditions.checkUnset(
                    getParams(),
                    "Cannot overwrite previously set phone number",
                    ExtraConstants.PHONE,
                    ExtraConstants.COUNTRY_ISO,
                    ExtraConstants.NATIONAL_NUMBER
                )
                if (!PhoneNumberUtils.isValidIso(iso)) {
                    throw IllegalStateException("Invalid country iso: $iso")
                }
                getParams().putString(ExtraConstants.COUNTRY_ISO, iso)
                getParams().putString(ExtraConstants.NATIONAL_NUMBER, number)
                return this
            }

            fun setDefaultCountryIso(iso: String): PhoneBuilder {
                Preconditions.checkUnset(
                    getParams(),
                    "Cannot overwrite previously set phone number",
                    ExtraConstants.PHONE,
                    ExtraConstants.COUNTRY_ISO,
                    ExtraConstants.NATIONAL_NUMBER
                )
                if (!PhoneNumberUtils.isValidIso(iso)) {
                    throw IllegalStateException("Invalid country iso: $iso")
                }
                getParams().putString(
                    ExtraConstants.COUNTRY_ISO,
                    iso.uppercase(Locale.getDefault())
                )
                return this
            }

            fun setAllowedCountries(countries: List<String>): PhoneBuilder {
                if (getParams().containsKey(ExtraConstants.BLOCKLISTED_COUNTRIES)) {
                    throw IllegalStateException(
                        "You can either allowlist or blocklist country codes for phone authentication."
                    )
                }
                val message =
                    "Invalid argument: Only non-%s allowlists are valid. To specify no allowlist, do not call this method."
                Preconditions.checkNotNull(countries, String.format(message, "null"))
                Preconditions.checkArgument(countries.isNotEmpty(), String.format(message, "empty"))
                addCountriesToBundle(countries, ExtraConstants.ALLOWLISTED_COUNTRIES)
                return this
            }

            fun setBlockedCountries(countries: List<String>): PhoneBuilder {
                if (getParams().containsKey(ExtraConstants.ALLOWLISTED_COUNTRIES)) {
                    throw IllegalStateException(
                        "You can either allowlist or blocklist country codes for phone authentication."
                    )
                }
                val message =
                    "Invalid argument: Only non-%s blocklists are valid. To specify no blocklist, do not call this method."
                Preconditions.checkNotNull(countries, String.format(message, "null"))
                Preconditions.checkArgument(countries.isNotEmpty(), String.format(message, "empty"))
                addCountriesToBundle(countries, ExtraConstants.BLOCKLISTED_COUNTRIES)
                return this
            }

            override fun build(): IdpConfig {
                validateInputs()
                return super.build()
            }

            private fun addCountriesToBundle(countryIsos: List<String>, countryIsoType: String) {
                val uppercaseCodes = ArrayList<String>()
                for (code in countryIsos) {
                    uppercaseCodes.add(code.uppercase(Locale.getDefault()))
                }
                getParams().putStringArrayList(countryIsoType, uppercaseCodes)
            }

            private fun validateInputs() {
                val allowedCountries = getParams().getStringArrayList(ExtraConstants.ALLOWLISTED_COUNTRIES)
                val blockedCountries = getParams().getStringArrayList(ExtraConstants.BLOCKLISTED_COUNTRIES)
                if (allowedCountries != null && blockedCountries != null) {
                    throw IllegalStateException(
                        "You can either allowlist or blocked country codes for phone authentication."
                    )
                } else if (allowedCountries != null) {
                    validateInputs(allowedCountries, true)
                } else if (blockedCountries != null) {
                    validateInputs(blockedCountries, false)
                }
            }

            private fun validateInputs(countries: List<String>, allowed: Boolean) {
                validateCountryInput(countries)
                validateDefaultCountryInput(countries, allowed)
            }

            private fun validateCountryInput(codes: List<String>) {
                for (code in codes) {
                    if (!PhoneNumberUtils.isValidIso(code) && !PhoneNumberUtils.isValid(code)) {
                        throw IllegalArgumentException(
                            "Invalid input: You must provide a valid country iso (alpha-2) or code (e-164). e.g. 'us' or '+1'."
                        )
                    }
                }
            }

            private fun validateDefaultCountryInput(codes: List<String>, allowed: Boolean) {
                if (getParams().containsKey(ExtraConstants.COUNTRY_ISO) ||
                    getParams().containsKey(ExtraConstants.PHONE)
                ) {
                    if (!validateDefaultCountryIso(codes, allowed) ||
                        !validateDefaultPhoneIsos(codes, allowed)
                    ) {
                        throw IllegalArgumentException(
                            "Invalid default country iso. Make sure it is either part of the allowed list or that you haven't blocked it."
                        )
                    }
                }
            }

            private fun validateDefaultCountryIso(codes: List<String>, allowed: Boolean): Boolean {
                val defaultIso = getDefaultIso()
                return isValidDefaultIso(codes, defaultIso, allowed)
            }

            private fun validateDefaultPhoneIsos(codes: List<String>, allowed: Boolean): Boolean {
                val phoneIsos = getPhoneIsosFromCode()
                for (iso in phoneIsos) {
                    if (isValidDefaultIso(codes, iso, allowed)) {
                        return true
                    }
                }
                return phoneIsos.isEmpty()
            }

            private fun isValidDefaultIso(codes: List<String>, iso: String?, allowed: Boolean): Boolean {
                if (iso == null) return true
                val containsIso = containsCountryIso(codes, iso)
                return (containsIso && allowed) || (!containsIso && !allowed)
            }

            private fun containsCountryIso(codes: List<String>, iso: String): Boolean {
                val isoUpper = iso.uppercase(Locale.getDefault())
                for (code in codes) {
                    if (PhoneNumberUtils.isValidIso(code)) {
                        if (code.equals(isoUpper, ignoreCase = true)) {
                            return true
                        }
                    } else {
                        val isos = PhoneNumberUtils.getCountryIsosFromCountryCode(code)
                        if (isos?.contains(isoUpper) == true) {
                            return true
                        }
                    }
                }
                return false
            }

            private fun getPhoneIsosFromCode(): List<String> {
                val isos = ArrayList<String>()
                val phone = getParams().getString(ExtraConstants.PHONE)
                if (phone != null && phone.startsWith("+")) {
                    val countryCode = "+" + PhoneNumberUtils.getPhoneNumber(phone).countryCode
                    val isosToAdd = PhoneNumberUtils.getCountryIsosFromCountryCode(countryCode)
                    if (isosToAdd != null) {
                        isos.addAll(isosToAdd)
                    }
                }
                return isos
            }

            private fun getDefaultIso(): String? {
                return if (getParams().containsKey(ExtraConstants.COUNTRY_ISO))
                    getParams().getString(ExtraConstants.COUNTRY_ISO)
                else null
            }
        }

        class GoogleBuilder : Builder(GoogleAuthProvider.PROVIDER_ID) {
            private fun validateWebClientId() {
                Preconditions.checkConfigured(
                    AuthUI.getApplicationContext(),
                    "Check your google-services plugin configuration, the default_web_client_id string wasn't populated.",
                    R.string.default_web_client_id
                )
            }

            fun setScopes(scopes: List<String>): GoogleBuilder {
                val builder = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                    .requestEmail()
                for (scope in scopes) {
                    builder.requestScopes(Scope(scope))
                }
                return setSignInOptions(builder.build())
            }

            fun setSignInOptions(options: GoogleSignInOptions): GoogleBuilder {
                Preconditions.checkUnset(
                    getParams(),
                    "Cannot overwrite previously set sign-in options.",
                    ExtraConstants.GOOGLE_SIGN_IN_OPTIONS
                )
                val builder = GoogleSignInOptions.Builder(options)
                var clientId = options.serverClientId
                if (clientId == null) {
                    validateWebClientId()
                    clientId = AuthUI.getApplicationContext().getString(R.string.default_web_client_id)
                }
                var hasEmailScope = false
                for (s in options.scopes) {
                    if ("email" == s.scopeUri) {
                        hasEmailScope = true
                        break
                    }
                }
                if (!hasEmailScope) {
                    Log.w(
                        TAG,
                        "The GoogleSignInOptions passed to setSignInOptions does not request the 'email' scope. In most cases this is a mistake! Call requestEmail() on the GoogleSignInOptions object."
                    )
                }
                builder.requestIdToken(clientId)
                getParams().putParcelable(ExtraConstants.GOOGLE_SIGN_IN_OPTIONS, builder.build())
                return this
            }

            override fun build(): IdpConfig {
                if (!getParams().containsKey(ExtraConstants.GOOGLE_SIGN_IN_OPTIONS)) {
                    validateWebClientId()
                    setScopes(Collections.emptyList())
                }
                return super.build()
            }
        }

        class FacebookBuilder : Builder(FacebookAuthProvider.PROVIDER_ID) {
            init {
                if (!ProviderAvailability.IS_FACEBOOK_AVAILABLE) {
                    throw RuntimeException(
                        "Facebook provider cannot be configured without dependency. Did you forget to add 'com.facebook.android:facebook-login:VERSION' dependency?"
                    )
                }
                Preconditions.checkConfigured(
                    AuthUI.getApplicationContext(),
                    "Facebook provider unconfigured. Make sure to add a `facebook_application_id` string. See the docs for more info: " +
                            "https://github.com/firebase/FirebaseUI-Android/blob/master/auth/README.md#facebook",
                    R.string.facebook_application_id
                )
                if (AuthUI.getApplicationContext().getString(R.string.facebook_login_protocol_scheme) == "fbYOUR_APP_ID") {
                    Log.w(TAG, "Facebook provider unconfigured for Chrome Custom Tabs.")
                }
            }

            fun setPermissions(permissions: List<String>): FacebookBuilder {
                getParams().putStringArrayList(
                    ExtraConstants.FACEBOOK_PERMISSIONS,
                    ArrayList(permissions)
                )
                return this
            }
        }

        class AnonymousBuilder : Builder(ANONYMOUS_PROVIDER)

        class TwitterBuilder : GenericOAuthProviderBuilder(
            TwitterAuthProvider.PROVIDER_ID,
            "Twitter",
            R.layout.fui_idp_button_twitter
        )

        class GitHubBuilder : GenericOAuthProviderBuilder(
            GithubAuthProvider.PROVIDER_ID,
            "Github",
            R.layout.fui_idp_button_github
        ) {
            @Deprecated("Please use setScopes(List) instead.")
            fun setPermissions(permissions: List<String>): GitHubBuilder {
                setScopes(permissions)
                return this
            }
        }

        class AppleBuilder : GenericOAuthProviderBuilder(
            APPLE_PROVIDER,
            "Apple",
            R.layout.fui_idp_button_apple
        )

        class MicrosoftBuilder : GenericOAuthProviderBuilder(
            MICROSOFT_PROVIDER,
            "Microsoft",
            R.layout.fui_idp_button_microsoft
        )

        class YahooBuilder : GenericOAuthProviderBuilder(
            YAHOO_PROVIDER,
            "Yahoo",
            R.layout.fui_idp_button_yahoo
        )

        open class GenericOAuthProviderBuilder(providerId: String, providerName: String, buttonId: Int) :
            Builder(providerId) {
            init {
                Preconditions.checkNotNull(providerId, "The provider ID cannot be null.")
                Preconditions.checkNotNull(providerName, "The provider name cannot be null.")
                getParams().putString(ExtraConstants.GENERIC_OAUTH_PROVIDER_ID, providerId)
                getParams().putString(ExtraConstants.GENERIC_OAUTH_PROVIDER_NAME, providerName)
                getParams().putInt(ExtraConstants.GENERIC_OAUTH_BUTTON_ID, buttonId)
            }

            fun setScopes(scopes: List<String>): GenericOAuthProviderBuilder {
                getParams().putStringArrayList(
                    ExtraConstants.GENERIC_OAUTH_SCOPES,
                    ArrayList(scopes)
                )
                return this
            }

            fun setCustomParameters(customParameters: Map<String, String>): GenericOAuthProviderBuilder {
                getParams().putSerializable(
                    ExtraConstants.GENERIC_OAUTH_CUSTOM_PARAMETERS,
                    HashMap(customParameters)
                )
                return this
            }
        }
    }

    @Suppress("UNCHECKED_CAST")
    public abstract inner class AuthIntentBuilder<T : AuthIntentBuilder<T>> {
        val mProviders: MutableList<IdpConfig> = ArrayList()
        var mDefaultProvider: IdpConfig? = null
        var mLogo: Int = NO_LOGO
        var mTheme: Int = getDefaultTheme()
        var mTosUrl: String? = null
        var mPrivacyPolicyUrl: String? = null
        var mAlwaysShowProviderChoice = false
        var mLockOrientation = false
        var mEnableCredentials = true
        var mAuthMethodPickerLayout: AuthMethodPickerLayout? = null
        var mPasswordSettings: ActionCodeSettings? = null

        fun setTheme(@StyleRes theme: Int): T {
            mTheme = Preconditions.checkValidStyle(
                mApp.applicationContext,
                theme,
                "theme identifier is unknown or not a style definition"
            )
            return this as T
        }

        fun setLogo(@DrawableRes logo: Int): T {
            mLogo = logo
            return this as T
        }

        @Deprecated("Please use setTosAndPrivacyPolicyUrls(String, String)")
        fun setTosUrl(tosUrl: String?): T {
            mTosUrl = tosUrl
            return this as T
        }

        @Deprecated("Please use setTosAndPrivacyPolicyUrls(String, String)")
        fun setPrivacyPolicyUrl(privacyPolicyUrl: String?): T {
            mPrivacyPolicyUrl = privacyPolicyUrl
            return this as T
        }

        fun setTosAndPrivacyPolicyUrls(tosUrl: String, privacyPolicyUrl: String): T {
            Preconditions.checkNotNull(tosUrl, "tosUrl cannot be null")
            Preconditions.checkNotNull(privacyPolicyUrl, "privacyPolicyUrl cannot be null")
            mTosUrl = tosUrl
            mPrivacyPolicyUrl = privacyPolicyUrl
            return this as T
        }

        fun setAvailableProviders(idpConfigs: List<IdpConfig>): T {
            Preconditions.checkNotNull(idpConfigs, "idpConfigs cannot be null")
            if (idpConfigs.size == 1 && idpConfigs[0].providerId == ANONYMOUS_PROVIDER) {
                throw IllegalStateException(
                    "Sign in as guest cannot be the only sign in method. In this case, sign the user in anonymously your self; no UI is needed."
                )
            }
            mProviders.clear()
            for (config in idpConfigs) {
                if (mProviders.contains(config)) {
                    throw IllegalArgumentException("Each provider can only be set once. " +
                            "${config.providerId} was set twice.")
                } else {
                    mProviders.add(config)
                }
            }
            return this as T
        }

        fun setDefaultProvider(config: IdpConfig?): T {
            if (config != null) {
                if (!mProviders.contains(config)) {
                    throw IllegalStateException("Default provider not in available providers list.")
                }
                if (mAlwaysShowProviderChoice) {
                    throw IllegalStateException("Can't set default provider and always show provider choice.")
                }
            }
            mDefaultProvider = config
            return this as T
        }

        fun setCredentialManagerEnabled(enableCredentials: Boolean): T {
            mEnableCredentials = enableCredentials
            return this as T
        }

        fun setAuthMethodPickerLayout(authMethodPickerLayout: AuthMethodPickerLayout): T {
            mAuthMethodPickerLayout = authMethodPickerLayout
            return this as T
        }

        fun setAlwaysShowSignInMethodScreen(alwaysShow: Boolean): T {
            if (alwaysShow && mDefaultProvider != null) {
                throw IllegalStateException("Can't show provider choice with a default provider.")
            }
            mAlwaysShowProviderChoice = alwaysShow
            return this as T
        }

        fun setLockOrientation(lockOrientation: Boolean): T {
            mLockOrientation = lockOrientation
            return this as T
        }

        fun setResetPasswordSettings(passwordSettings: ActionCodeSettings): T {
            mPasswordSettings = passwordSettings
            return this as T
        }

        @CallSuper
        open fun build(): Intent {
            if (mProviders.isEmpty()) {
                mProviders.add(IdpConfig.EmailBuilder().build())
            }
            return KickoffActivity.createIntent(mApp.applicationContext, getFlowParams())
        }

        protected abstract fun getFlowParams(): FlowParameters
    }

    inner class SignInIntentBuilder : AuthIntentBuilder<SignInIntentBuilder>() {
        private var mEmailLink: String? = null
        private var mEnableAnonymousUpgrade = false

        fun setEmailLink(emailLink: String): SignInIntentBuilder {
            mEmailLink = emailLink
            return this
        }

        fun enableAnonymousUsersAutoUpgrade(): SignInIntentBuilder {
            mEnableAnonymousUpgrade = true
            validateEmailBuilderConfig()
            return this
        }

        private fun validateEmailBuilderConfig() {
            for (config in mProviders) {
                if (config.providerId == EMAIL_LINK_PROVIDER) {
                    val emailLinkForceSameDevice =
                        config.getParams().getBoolean(ExtraConstants.FORCE_SAME_DEVICE, true)
                    if (!emailLinkForceSameDevice) {
                        throw IllegalStateException(
                            "You must force the same device flow when using email link sign in with anonymous user upgrade"
                        )
                    }
                }
            }
        }

        override fun getFlowParams(): FlowParameters {
            return FlowParameters(
                mApp.name,
                mProviders,
                mDefaultProvider,
                mTheme,
                mLogo,
                mTosUrl,
                mPrivacyPolicyUrl,
                mEnableCredentials,
                mEnableAnonymousUpgrade,
                mAlwaysShowProviderChoice,
                mLockOrientation,
                mEmailLink,
                mPasswordSettings,
                mAuthMethodPickerLayout
            )
        }
    }

    companion object {
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        const val TAG = "AuthUI"

        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        const val ANONYMOUS_PROVIDER = "anonymous"
        const val EMAIL_LINK_PROVIDER = EmailAuthProvider.EMAIL_LINK_SIGN_IN_METHOD

        const val MICROSOFT_PROVIDER = "microsoft.com"
        const val YAHOO_PROVIDER = "yahoo.com"
        const val APPLE_PROVIDER = "apple.com"

        const val NO_LOGO = -1

        val SUPPORTED_PROVIDERS: Set<String> = setOf(
            GoogleAuthProvider.PROVIDER_ID,
            FacebookAuthProvider.PROVIDER_ID,
            TwitterAuthProvider.PROVIDER_ID,
            GithubAuthProvider.PROVIDER_ID,
            EmailAuthProvider.PROVIDER_ID,
            PhoneAuthProvider.PROVIDER_ID,
            ANONYMOUS_PROVIDER,
            EMAIL_LINK_PROVIDER
        )

        val SUPPORTED_OAUTH_PROVIDERS: Set<String> = setOf(
            MICROSOFT_PROVIDER,
            YAHOO_PROVIDER,
            APPLE_PROVIDER,
            TwitterAuthProvider.PROVIDER_ID,
            GithubAuthProvider.PROVIDER_ID
        )

        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        val SOCIAL_PROVIDERS: Set<String> = setOf(
            GoogleAuthProvider.PROVIDER_ID,
            FacebookAuthProvider.PROVIDER_ID
        )

        @JvmStatic
        fun isSocialProvider(providerId: String): Boolean {
            return SOCIAL_PROVIDERS.contains(providerId)
        }

        @JvmStatic
        fun isSupportedProvider(providerId: String): Boolean {
            return SUPPORTED_PROVIDERS.contains(providerId)
        }

        @JvmStatic
        fun isSupportedOAuthProvider(providerId: String): Boolean {
            return SUPPORTED_OAUTH_PROVIDERS.contains(providerId)
        }

        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        const val UNCONFIGURED_CONFIG_VALUE = "CHANGE-ME"

        private val INSTANCES: IdentityHashMap<FirebaseApp, AuthUI> = IdentityHashMap()

        @JvmStatic
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        fun getApplicationContext(): Context {
            return sApplicationContext ?: throw IllegalStateException("Application context not set")
        }

        @JvmStatic
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        fun setApplicationContext(context: Context) {
            sApplicationContext = Preconditions.checkNotNull(context, "App context cannot be null.").applicationContext
        }

        @JvmStatic
        fun getInstance(): AuthUI {
            return getInstance(FirebaseApp.getInstance())
        }

        @JvmStatic
        fun getInstance(appName: String): AuthUI {
            return getInstance(FirebaseApp.getInstance(appName))
        }

        @JvmStatic
        fun getInstance(app: FirebaseApp): AuthUI {
            val releaseUrl = "https://github.com/firebase/FirebaseUI-Android/releases/tag/6.2.0"
            val devWarning = "Beginning with FirebaseUI 6.2.0 you no longer need to include %s to sign in with %s. Go to %s for more information"
            if (ProviderAvailability.IS_TWITTER_AVAILABLE) {
                Log.w(TAG, String.format(devWarning, "the TwitterKit SDK", "Twitter", releaseUrl))
            }
            if (ProviderAvailability.IS_GITHUB_AVAILABLE) {
                Log.w(TAG, String.format(devWarning, "com.firebaseui:firebase-ui-auth-github", "GitHub", releaseUrl))
            }
            synchronized(INSTANCES) {
                var authUi = INSTANCES[app]
                if (authUi == null) {
                    authUi = AuthUI(app)
                    INSTANCES[app] = authUi
                }
                return authUi
            }
        }

        @JvmStatic
        fun canHandleIntent(intent: Intent?): Boolean {
            if (intent == null || intent.data == null) {
                return false
            }
            val link = intent.data.toString()
            return FirebaseAuth.getInstance().isSignInWithEmailLink(link)
        }

        @StyleRes
        @JvmStatic
        fun getDefaultTheme(): Int = R.style.FirebaseUI_DefaultMaterialTheme

        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        private var sApplicationContext: Context? = null
    }
}