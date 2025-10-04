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

import android.content.Context
import java.util.Locale
import com.google.firebase.auth.ActionCodeSettings
import androidx.compose.ui.graphics.vector.ImageVector
import com.firebase.ui.auth.compose.configuration.auth_provider.AuthProvider
import com.firebase.ui.auth.compose.configuration.auth_provider.AuthProvidersBuilder
import com.firebase.ui.auth.compose.configuration.auth_provider.Provider
import com.firebase.ui.auth.compose.configuration.string_provider.AuthUIStringProvider
import com.firebase.ui.auth.compose.configuration.string_provider.DefaultAuthUIStringProvider
import com.firebase.ui.auth.compose.configuration.theme.AuthUITheme

fun authUIConfiguration(block: AuthUIConfigurationBuilder.() -> Unit) =
    AuthUIConfigurationBuilder().apply(block).build()

@DslMarker
annotation class AuthUIConfigurationDsl

@AuthUIConfigurationDsl
class AuthUIConfigurationBuilder {
    var context: Context? = null
    private val providers = mutableListOf<AuthProvider>()
    var theme: AuthUITheme = AuthUITheme.Default
    var locale: Locale? = null
    var stringProvider: AuthUIStringProvider? = null
    var isCredentialManagerEnabled: Boolean = true
    var isMfaEnabled: Boolean = true
    var isAnonymousUpgradeEnabled: Boolean = false
    var tosUrl: String? = null
    var privacyPolicyUrl: String? = null
    var logo: ImageVector? = null
    var actionCodeSettings: ActionCodeSettings? = null
    var isNewEmailAccountsAllowed: Boolean = true
    var isDisplayNameRequired: Boolean = true
    var isProviderChoiceAlwaysShown: Boolean = false

    fun providers(block: AuthProvidersBuilder.() -> Unit) =
        providers.addAll(AuthProvidersBuilder().apply(block).build())

    internal fun build(): AuthUIConfiguration {
        val context = requireNotNull(context) {
            "Application context is required"
        }

        require(providers.isNotEmpty()) {
            "At least one provider must be configured"
        }

        // No unsupported providers
        val supportedProviderIds = Provider.entries.map { it.id }.toSet()
        val unknownProviders = providers.filter { it.providerId !in supportedProviderIds }
        require(unknownProviders.isEmpty()) {
            "Unknown providers: ${unknownProviders.joinToString { it.providerId }}"
        }

        // Cannot have only anonymous provider
        AuthProvider.Anonymous.validate(providers)

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
                is AuthProvider.Email -> {
                    provider.validate()

                    if (isAnonymousUpgradeEnabled && provider.isEmailLinkSignInEnabled) {
                        check(provider.isEmailLinkForceSameDeviceEnabled) {
                            "You must force the same device flow when using email link sign in " +
                                    "with anonymous user upgrade"
                        }
                    }
                }

                is AuthProvider.Phone -> provider.validate()
                is AuthProvider.Google -> provider.validate(context)
                is AuthProvider.Facebook -> provider.validate(context)
                is AuthProvider.GenericOAuth -> provider.validate()
                else -> null
            }
        }

        return AuthUIConfiguration(
            context = context,
            providers = providers.toList(),
            theme = theme,
            locale = locale,
            stringProvider = stringProvider ?: DefaultAuthUIStringProvider(context, locale),
            isCredentialManagerEnabled = isCredentialManagerEnabled,
            isMfaEnabled = isMfaEnabled,
            isAnonymousUpgradeEnabled = isAnonymousUpgradeEnabled,
            tosUrl = tosUrl,
            privacyPolicyUrl = privacyPolicyUrl,
            logo = logo,
            actionCodeSettings = actionCodeSettings,
            isNewEmailAccountsAllowed = isNewEmailAccountsAllowed,
            isDisplayNameRequired = isDisplayNameRequired,
            isProviderChoiceAlwaysShown = isProviderChoiceAlwaysShown
        )
    }
}

/**
 * Configuration object for the authentication flow.
 */
class AuthUIConfiguration(
    /**
     * Application context
     */
    val context: Context,

    /**
     * The list of enabled authentication providers.
     */
    val providers: List<AuthProvider> = emptyList(),

    /**
     * The theming configuration for the UI. Default to [AuthUITheme.Default].
     */
    val theme: AuthUITheme = AuthUITheme.Default,

    /**
     * The locale for internationalization.
     */
    val locale: Locale? = null,

    /**
     * A custom provider for localized strings.
     */
    val stringProvider: AuthUIStringProvider = DefaultAuthUIStringProvider(context, locale),

    /**
     * Enables integration with Android's Credential Manager API. Defaults to true.
     */
    val isCredentialManagerEnabled: Boolean = true,

    /**
     * Enables Multi-Factor Authentication support. Defaults to true.
     */
    val isMfaEnabled: Boolean = true,

    /**
     * Allows upgrading an anonymous user to a new credential.
     */
    val isAnonymousUpgradeEnabled: Boolean = false,

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
    val isNewEmailAccountsAllowed: Boolean = true,

    /**
     * Requires the user to provide a display name on sign-up. Defaults to true.
     */
    val isDisplayNameRequired: Boolean = true,

    /**
     * Always shows the provider selection screen, even if only one is enabled.
     */
    val isProviderChoiceAlwaysShown: Boolean = false,
)
