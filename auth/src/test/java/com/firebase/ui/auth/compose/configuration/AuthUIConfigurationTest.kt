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
import android.content.res.Configuration
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.test.core.app.ApplicationProvider
import com.firebase.ui.auth.R
import com.firebase.ui.auth.compose.configuration.auth_provider.AuthProvider
import com.firebase.ui.auth.compose.configuration.string_provider.AuthUIStringProvider
import com.firebase.ui.auth.compose.configuration.string_provider.DefaultAuthUIStringProvider
import com.firebase.ui.auth.compose.configuration.theme.AuthUITheme
import com.google.common.truth.Truth.assertThat
import com.google.firebase.auth.actionCodeSettings
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mock
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.Locale
import kotlin.reflect.KMutableProperty
import kotlin.reflect.full.memberProperties

/**
 * Unit tests for [AuthUIConfiguration] covering configuration builder behavior,
 * validation rules, provider setup, and immutability guarantees.
 *
 * @suppress Internal test class
 */
@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class AuthUIConfigurationTest {

    private lateinit var applicationContext: Context

    @Before
    fun setUp() {
        applicationContext = ApplicationProvider.getApplicationContext()
    }

    // =============================================================================================
    // Basic Configuration Tests
    // =============================================================================================

    @Test
    fun `authUIConfiguration with minimal setup uses correct defaults`() {
        val config = authUIConfiguration {
            context = applicationContext
            providers {
                provider(
                    AuthProvider.Google(
                        scopes = listOf(),
                        serverClientId = "test_client_id"
                    )
                )
            }
        }

        assertThat(config.context).isEqualTo(applicationContext)
        assertThat(config.providers).hasSize(1)
        assertThat(config.theme).isEqualTo(AuthUITheme.Default)
        assertThat(config.stringProvider).isInstanceOf(DefaultAuthUIStringProvider::class.java)
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
        val customStringProvider = mock(AuthUIStringProvider::class.java)
        val customLocale = Locale.US
        val customActionCodeSettings = actionCodeSettings {
            url = "https://example.com/verify"
            handleCodeInApp = true
        }

        val config = authUIConfiguration {
            context = applicationContext
            providers {
                provider(
                    AuthProvider.Google(
                        scopes = listOf(),
                        serverClientId = "test_client_id"
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

        assertThat(config.context).isEqualTo(applicationContext)
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

    @Test
    fun `providers block can be called multiple times and accumulates providers`() {
        val config = authUIConfiguration {
            context = applicationContext
            providers {
                provider(
                    AuthProvider.Google(
                        scopes = listOf(),
                        serverClientId = "test_client_id"
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

    @Test
    fun `authUIConfiguration uses custom string provider`() {
        val spanishAuthUIStringProvider =
            object : AuthUIStringProvider by DefaultAuthUIStringProvider(applicationContext) {
                // Email Validation
                override val missingEmailAddress: String =
                    "Ingrese su dirección de correo para continuar"
                override val invalidEmailAddress: String = "Esa dirección de correo no es correcta"

                // Password Validation
                override val invalidPassword: String = "Contraseña incorrecta"
                override val passwordsDoNotMatch: String = "Las contraseñas no coinciden"
            }

        val config = authUIConfiguration {
            context = applicationContext
            providers {
                provider(
                    AuthProvider.Google(
                        scopes = listOf(),
                        serverClientId = "test_client_id"
                    )
                )
            }
            stringProvider = spanishAuthUIStringProvider
        }

        assertThat(config.stringProvider.missingEmailAddress)
            .isEqualTo(spanishAuthUIStringProvider.missingEmailAddress)
    }

    @Test
    fun `locale set to FR in authUIConfiguration reflects in DefaultAuthUIStringProvider`() {
        val localizedContext = applicationContext.createConfigurationContext(
            Configuration(applicationContext.resources.configuration).apply {
                setLocale(Locale.FRANCE)
            }
        )

        val config = authUIConfiguration {
            context = applicationContext
            providers {
                provider(
                    AuthProvider.Google(
                        scopes = listOf(),
                        serverClientId = "test_client_id"
                    )
                )
            }
            locale = Locale.FRANCE
        }

        assertThat(config.stringProvider.continueText)
            .isEqualTo(localizedContext.getString(R.string.fui_continue))
    }

    @Test
    fun `unsupported locale set in authUIConfiguration uses default localized strings`() {
        val unsupportedLocale = Locale("zz", "ZZ")

        val config = authUIConfiguration {
            context = applicationContext
            providers {
                provider(
                    AuthProvider.Google(
                        scopes = listOf(),
                        serverClientId = "test_client_id"
                    )
                )
            }
            locale = unsupportedLocale
        }

        assertThat(config.stringProvider.signInWithGoogle).isNotEmpty()
        assertThat(config.stringProvider.continueText).isNotEmpty()
        assertThat(config.stringProvider.signInWithGoogle)
            .isEqualTo(applicationContext.getString(R.string.fui_sign_in_with_google))
        assertThat(config.stringProvider.continueText)
            .isEqualTo(applicationContext.getString(R.string.fui_continue))
    }

    // =============================================================================================
    // Validation Tests
    // =============================================================================================

    @Test
    fun `authUIConfiguration throws when no context configured`() {
        try {
            authUIConfiguration {
                context = applicationContext
                providers {
                    provider(
                        AuthProvider.Google(
                            scopes = listOf(),
                            serverClientId = "test_client_id"
                        )
                    )
                }
            }
        } catch (e: Exception) {
            assertThat(e.message).isEqualTo("Application context is required")
        }
    }

    @Test
    fun `authUIConfiguration throws when no providers configured`() {
        try {
            authUIConfiguration {
                context = applicationContext
            }
        } catch (e: Exception) {
            assertThat(e.message).isEqualTo("At least one provider must be configured")
        }
    }

    @Test
    fun `validation accepts all supported providers`() {
        val config = authUIConfiguration {
            context = applicationContext
            providers {
                provider(AuthProvider.Google(scopes = listOf(), serverClientId = "test_client_id"))
                provider(AuthProvider.Facebook(applicationId = "test_app_id"))
                provider(AuthProvider.Twitter(customParameters = mapOf()))
                provider(AuthProvider.Github(customParameters = mapOf()))
                provider(AuthProvider.Microsoft(customParameters = mapOf(), tenant = null))
                provider(AuthProvider.Yahoo(customParameters = mapOf()))
                provider(AuthProvider.Apple(customParameters = mapOf(), locale = null))
                provider(
                    AuthProvider.Phone(
                        defaultNumber = null,
                        defaultCountryCode = null,
                        allowedCountries = null
                    )
                )
                provider(
                    AuthProvider.Email(
                        actionCodeSettings = null,
                        passwordValidationRules = listOf()
                    )
                )
            }
        }
        assertThat(config.providers).hasSize(9)
    }

    @Test
    fun `validation throws for unsupported provider`() {
        val mockProvider = AuthProvider.GenericOAuth(
            providerId = "unsupported.provider",
            scopes = listOf(),
            customParameters = mapOf(),
            buttonLabel = "Test",
            buttonIcon = null,
            buttonColor = null,
            contentColor = null,
        )

        try {
            authUIConfiguration {
                context = applicationContext
                providers {
                    provider(mockProvider)
                }
            }
        } catch (e: Exception) {
            assertThat(e).isInstanceOf(IllegalArgumentException::class.java)
            assertThat(e.message).isEqualTo("Unknown providers: unsupported.provider")
        }
    }

    @Test
    fun `validate throws for duplicate providers`() {
        try {
            authUIConfiguration {
                context = applicationContext
                providers {
                    provider(AuthProvider.Google(scopes = listOf(), serverClientId = ""))
                    provider(
                        AuthProvider.Google(
                            scopes = listOf("email"),
                            serverClientId = "different"
                        )
                    )
                }
            }
        } catch (e: Exception) {
            assertThat(e).isInstanceOf(IllegalArgumentException::class.java)
            assertThat(e.message).isEqualTo(
                "Each provider can only be set once. Duplicates: google.com"
            )
        }
    }

    // =============================================================================================
    // Builder Immutability Tests
    // =============================================================================================

    @Test
    fun `authUIConfiguration providers list is immutable`() {
        val config = authUIConfiguration {
            context = applicationContext
            providers {
                provider(
                    AuthProvider.Google(
                        scopes = listOf(),
                        serverClientId = "test_client_id"
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
            "context",
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
