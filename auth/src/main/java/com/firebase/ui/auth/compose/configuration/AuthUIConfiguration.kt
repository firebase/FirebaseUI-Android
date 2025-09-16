/*
 * Copyright 2025 Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.firebase.ui.auth.compose.configuration

import java.util.Locale
import com.google.firebase.auth.ActionCodeSettings
import androidx.compose.ui.graphics.vector.ImageVector

fun actionCodeSettings(
    block: ActionCodeSettings.Builder.() -> Unit
) = ActionCodeSettings.newBuilder().apply(block).build()

fun authUIConfiguration(block: AuthUIConfigurationBuilder.() -> Unit): AuthUIConfiguration {
    val builder = AuthUIConfigurationBuilder()
    builder.block()
    return builder.build()
}

@DslMarker
annotation class AuthUIConfigurationDsl

@AuthUIConfigurationDsl
class AuthUIConfigurationBuilder {
    private val providers = mutableListOf<AuthProvider>()
    var theme: AuthUITheme = AuthUITheme.Default
    var stringProvider: AuthUIStringProvider? = null
    var locale: Locale? = null
    var enableCredentialManager: Boolean = true
    var enableMfa: Boolean = true
    var enableAnonymousUpgrade: Boolean = false
    var tosUrl: String? = null
    var privacyPolicyUrl: String? = null
    var logo: ImageVector? = null
    var actionCodeSettings: ActionCodeSettings? = null
    var allowNewEmailAccounts: Boolean = true
    var requireDisplayName: Boolean = true
    var alwaysShowProviderChoice: Boolean = false

    fun providers(block: AuthProvidersBuilder.() -> Unit) {
        val builder = AuthProvidersBuilder()
        builder.block()
        providers.addAll(builder.build())
    }

    internal fun build(): AuthUIConfiguration {
        validate()
        return AuthUIConfiguration(
            providers = providers.toList(),
            theme = theme,
            stringProvider = stringProvider,
            locale = locale,
            enableCredentialManager = enableCredentialManager,
            enableMfa = enableMfa,
            enableAnonymousUpgrade = enableAnonymousUpgrade,
            tosUrl = tosUrl,
            privacyPolicyUrl = privacyPolicyUrl,
            logo = logo,
            actionCodeSettings = actionCodeSettings,
            allowNewEmailAccounts = allowNewEmailAccounts,
            requireDisplayName = requireDisplayName,
            alwaysShowProviderChoice = alwaysShowProviderChoice
        )
    }

    private fun validate() {
        // At least one provider
        if (providers.isEmpty()) {
            throw IllegalArgumentException("At least one provider must be configured")
        }

        // No unsupported providers
        val supportedProviderIds = Provider.entries.map { it.id }.toSet()
        val unknownProviders = providers.filter { it.providerId !in supportedProviderIds }
        require(unknownProviders.isEmpty()) {
            "Unknown providers: ${unknownProviders.joinToString { it.providerId }}"
        }

        // Cannot have only anonymous provider
        if (providers.size == 1 && providers.first() is AuthProvider.Anonymous) {
            throw IllegalStateException(
                "Sign in as guest cannot be the only sign in method. " +
                        "In this case, sign the user in anonymously your self; no UI is needed."
            )
        }

        // Check for duplicate providers
        val providerIds = providers.map { it.providerId }
        val duplicates = providerIds.groupingBy { it }.eachCount().filter { it.value > 1 }

        require(duplicates.isEmpty()) {
            val message = duplicates.keys.joinToString(", ")
            throw IllegalArgumentException(
                "Each provider can only be set once. Duplicates: $message"
            )
        }

        // Provider specific validations
        providers.forEach { provider ->
            when (provider) {
                is AuthProvider.Email -> provider.validate()
                else -> null
            }
        }
    }
}

/**
 * Configuration object for the authentication flow.
 */
data class AuthUIConfiguration(
    /**
     * The list of enabled authentication providers.
     */
    val providers: List<AuthProvider> = emptyList(),

    /**
     * The theming configuration for the UI. Default to [AuthUITheme.Default].
     */
    val theme: AuthUITheme = AuthUITheme.Default,

    /**
     * A custom provider for localized strings.
     */
    val stringProvider: AuthUIStringProvider? = null,

    /**
     * The locale for internationalization.
     */
    val locale: Locale? = null,

    /**
     * Enables integration with Android's Credential Manager API. Defaults to true.
     */
    val enableCredentialManager: Boolean = true,

    /**
     * Enables Multi-Factor Authentication support. Defaults to true.
     */
    val enableMfa: Boolean = true,

    /**
     * Allows upgrading an anonymous user to a new credential.
     */
    val enableAnonymousUpgrade: Boolean = false,

    /**
     * The URL for the terms of service.
     */
    val tosUrl: String? = null,

    /**
     * The URL for the privacy policy.
     */
    val privacyPolicyUrl: String? = null,

    /**
     * The logo to display on the authentication screens.
     */
    val logo: ImageVector? = null,

    /**
     * Configuration for email link sign-in.
     */
    val actionCodeSettings: ActionCodeSettings? = null,

    /**
     * Allows new email accounts to be created. Defaults to true.
     */
    val allowNewEmailAccounts: Boolean = true,

    /**
     * Requires the user to provide a display name on sign-up. Defaults to true.
     */
    val requireDisplayName: Boolean = true,

    /**
     * Always shows the provider selection screen, even if only one is enabled.
     */
    val alwaysShowProviderChoice: Boolean = false,
)
