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

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import com.google.common.truth.Truth.assertThat
import com.google.firebase.auth.actionCodeSettings
import org.junit.Assert.assertThrows
import org.junit.Test
import java.util.Locale
import kotlin.reflect.KMutableProperty
import kotlin.reflect.full.memberProperties

class AuthUIConfigurationTest {

    @Test
    fun `authUIConfiguration with minimal setup uses correct defaults`() {
        val config = authUIConfiguration {
            providers {
                provider(
                    AuthProvider.Google(
                        scopes = listOf(),
                        serverClientId = ""
                    )
                )
            }
        }

        assertThat(config.providers).hasSize(1)
        assertThat(config.theme).isEqualTo(AuthUITheme.Default)
        assertThat(config.stringProvider).isNull()
        assertThat(config.locale).isNull()
        assertThat(config.isCredentialManagerEnabled).isTrue()
        assertThat(config.isMfaEnabled).isTrue()
        assertThat(config.isAnonymousUpgradeEnabled).isFalse()
        assertThat(config.tosUrl).isNull()
        assertThat(config.privacyPolicyUrl).isNull()
        assertThat(config.logo).isNull()
        assertThat(config.actionCodeSettings).isNull()
        assertThat(config.isNewEmailAccountsAllowed).isTrue()
        assertThat(config.isDisplayNameRequired).isTrue()
        assertThat(config.isProviderChoiceAlwaysShown).isFalse()
    }

    @Test
    fun `authUIConfiguration with all fields overridden uses custom values`() {
        val customTheme = AuthUITheme.Default
        val customStringProvider = object : AuthUIStringProvider {
            override fun initializing(): String = ""
            override fun signInWithGoogle(): String = ""
            override fun invalidEmail(): String = ""
            override fun passwordsDoNotMatch(): String = ""
        }
        val customLocale = Locale.US
        val customActionCodeSettings = actionCodeSettings {
            url = "https://example.com/verify"
            handleCodeInApp = true
        }

        val config = authUIConfiguration {
            providers {
                provider(
                    AuthProvider.Google(
                        scopes = listOf(),
                        serverClientId = ""
                    )
                )
                provider(
                    AuthProvider.Github(
                        customParameters = mapOf()
                    )
                )
            }
            theme = customTheme
            stringProvider = customStringProvider
            locale = customLocale
            isCredentialManagerEnabled = false
            isMfaEnabled = false
            isAnonymousUpgradeEnabled = true
            tosUrl = "https://example.com/tos"
            privacyPolicyUrl = "https://example.com/privacy"
            logo = Icons.Default.AccountCircle
            actionCodeSettings = customActionCodeSettings
            isNewEmailAccountsAllowed = false
            isDisplayNameRequired = false
            isProviderChoiceAlwaysShown = true
        }

        assertThat(config.providers).hasSize(2)
        assertThat(config.theme).isEqualTo(customTheme)
        assertThat(config.stringProvider).isEqualTo(customStringProvider)
        assertThat(config.locale).isEqualTo(customLocale)
        assertThat(config.isCredentialManagerEnabled).isFalse()
        assertThat(config.isMfaEnabled).isFalse()
        assertThat(config.isAnonymousUpgradeEnabled).isTrue()
        assertThat(config.tosUrl).isEqualTo("https://example.com/tos")
        assertThat(config.privacyPolicyUrl).isEqualTo("https://example.com/privacy")
        assertThat(config.logo).isEqualTo(Icons.Default.AccountCircle)
        assertThat(config.actionCodeSettings).isEqualTo(customActionCodeSettings)
        assertThat(config.isNewEmailAccountsAllowed).isFalse()
        assertThat(config.isDisplayNameRequired).isFalse()
        assertThat(config.isProviderChoiceAlwaysShown).isTrue()
    }

    // ===========================================================================================
    // Validation Tests
    // ===========================================================================================

    @Test(expected = IllegalArgumentException::class)
    fun `authUIConfiguration throws when no providers configured`() {
        authUIConfiguration { }
    }

    @Test
    fun `validation accepts all supported providers`() {
        val config = authUIConfiguration {
            providers {
                provider(AuthProvider.Google(scopes = listOf(), serverClientId = ""))
                provider(AuthProvider.Facebook())
                provider(AuthProvider.Twitter(customParameters = mapOf()))
                provider(AuthProvider.Github(customParameters = mapOf()))
                provider(AuthProvider.Microsoft(customParameters = mapOf(), tenant = null))
                provider(AuthProvider.Yahoo(customParameters = mapOf()))
                provider(AuthProvider.Apple(customParameters = mapOf(), locale = null))
                provider(AuthProvider.Phone(defaultCountryCode = null, allowedCountries = null))
                provider(AuthProvider.Email(actionCodeSettings = null, passwordValidationRules = listOf()))
            }
        }
        assertThat(config.providers).hasSize(9)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `validation throws for unsupported provider`() {
        val mockProvider = AuthProvider.GenericOAuth(
            providerId = "unsupported.provider",
            scopes = listOf(),
            customParameters = mapOf(),
            buttonLabel = "Test",
            buttonIcon = null,
            buttonColor = null
        )

        authUIConfiguration {
            providers {
                provider(mockProvider)
            }
        }
    }

    @Test(expected = IllegalStateException::class)
    fun `validate throws when only anonymous provider is configured`() {
        authUIConfiguration {
            providers {
                provider(AuthProvider.Anonymous)
            }
        }
    }

    @Test(expected = IllegalArgumentException::class)
    fun `validate throws for duplicate providers`() {
        authUIConfiguration {
            providers {
                provider(AuthProvider.Google(scopes = listOf(), serverClientId = ""))
                provider(AuthProvider.Google(scopes = listOf("email"), serverClientId = "different"))
            }
        }
    }

    @Test(expected = IllegalArgumentException::class)
    fun `validate throws for enableEmailLinkSignIn true when actionCodeSettings is null`() {
        authUIConfiguration {
            providers {
                provider(AuthProvider.Email(
                    isEmailLinkSignInEnabled = true,
                    actionCodeSettings = null,
                    passwordValidationRules = listOf()
                ))
            }
        }
    }

    @Test(expected = IllegalStateException::class)
    fun `validate throws for enableEmailLinkSignIn true when actionCodeSettings canHandleCodeInApp false`() {
        val customActionCodeSettings = actionCodeSettings {
            url = "https://example.com"
            handleCodeInApp = false
        }
        authUIConfiguration {
            providers {
                provider(AuthProvider.Email(
                    isEmailLinkSignInEnabled = true,
                    actionCodeSettings = customActionCodeSettings,
                    passwordValidationRules = listOf()
                ))
            }
        }
    }

    // ===========================================================================================
    // Provider Configuration Tests
    // ===========================================================================================

    @Test
    fun `providers block can be called multiple times and accumulates providers`() {
        val config = authUIConfiguration {
            providers {
                provider(
                    AuthProvider.Google(
                        scopes = listOf(),
                        serverClientId = ""
                    )
                )
            }

            providers {
                provider(
                    AuthProvider.Github(
                        customParameters = mapOf()
                    )
                )
            }
            isCredentialManagerEnabled = true
        }

        assertThat(config.providers).hasSize(2)
    }

    // ===========================================================================================
    // Builder Immutability Tests
    // ===========================================================================================

    @Test
    fun `authUIConfiguration providers list is immutable`() {
        val config = authUIConfiguration {
            providers {
                provider(
                    AuthProvider.Google(
                        scopes = listOf(),
                        serverClientId = ""
                    )
                )
            }
        }

        val originalSize = config.providers.size

        assertThrows(UnsupportedOperationException::class.java) {
            (config.providers as MutableList).add(
                AuthProvider.Twitter(customParameters = mapOf())
            )
        }

        assertThat(config.providers.size).isEqualTo(originalSize)
    }

    @Test
    fun `authUIConfiguration creates immutable configuration`() {
        val kClass = AuthUIConfiguration::class

        val allProperties = kClass.memberProperties

        allProperties.forEach {
            assertThat(it).isNotInstanceOf(KMutableProperty::class.java)
        }

        val expectedProperties = setOf(
            "providers",
            "theme",
            "stringProvider",
            "locale",
            "isCredentialManagerEnabled",
            "isMfaEnabled",
            "isAnonymousUpgradeEnabled",
            "tosUrl",
            "privacyPolicyUrl",
            "logo",
            "actionCodeSettings",
            "isNewEmailAccountsAllowed",
            "isDisplayNameRequired",
            "isProviderChoiceAlwaysShown"
        )

        val actualProperties = allProperties.map { it.name }.toSet()

        assertThat(actualProperties).containsExactlyElementsIn(expectedProperties)
    }
}
