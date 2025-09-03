package com.firebase.core

import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.PhoneAuthProvider

data class IdpConfig(
    val providerId: String,
    val params: Map<String, Any> = emptyMap()
) {
    abstract class Builder(private val providerId: String) {
        protected val params = mutableMapOf<String, Any>()

        open fun build(): IdpConfig = IdpConfig(providerId, params.toMap())
    }

    class GoogleBuilder : Builder(GoogleAuthProvider.PROVIDER_ID) {
        fun setServerClientId(clientId: String?): GoogleBuilder {
            clientId?.let { params["serverClientId"] = it }
            return this
        }

        fun setScopes(scopes: List<String>): GoogleBuilder {
            params["scopes"] = scopes
            return this
        }
    }

    class EmailBuilder : Builder(EmailAuthProvider.PROVIDER_ID) {
        fun setRequireDisplayName(requireDisplayName: Boolean): EmailBuilder {
            params["requireDisplayName"] = requireDisplayName
            return this
        }

        fun setAllowNewAccounts(allowNewAccounts: Boolean): EmailBuilder {
            params["allowNewAccounts"] = allowNewAccounts
            return this
        }
    }

    class PhoneBuilder : Builder(PhoneAuthProvider.PROVIDER_ID) {
        fun setDefaultCountryIso(countryIso: String?): PhoneBuilder {
            countryIso?.let { params["defaultCountryIso"] = it }
            return this
        }

        fun setDefaultPhoneNumber(phoneNumber: String?): PhoneBuilder {
            phoneNumber?.let { params["defaultPhoneNumber"] = it }
            return this
        }
    }
}
