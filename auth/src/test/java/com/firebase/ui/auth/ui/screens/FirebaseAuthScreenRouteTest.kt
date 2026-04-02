package com.firebase.ui.auth.ui.screens

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.firebase.ui.auth.configuration.authUIConfiguration
import com.firebase.ui.auth.configuration.auth_provider.AuthProvider
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class FirebaseAuthScreenRouteTest {

    private lateinit var applicationContext: Context

    @Before
    fun setUp() {
        applicationContext = ApplicationProvider.getApplicationContext()
    }

    @Test
    fun `single email provider starts at email route`() {
        val configuration = authUIConfiguration {
            context = applicationContext
            providers {
                provider(
                    AuthProvider.Email(
                        emailLinkActionCodeSettings = null,
                        passwordValidationRules = emptyList()
                    )
                )
            }
        }

        assertThat(getStartRoute(configuration)).isEqualTo(AuthRoute.Email)
    }

    @Test
    fun `single phone provider starts at phone route`() {
        val configuration = authUIConfiguration {
            context = applicationContext
            providers {
                provider(
                    AuthProvider.Phone(
                        defaultNumber = null,
                        defaultCountryCode = null,
                        allowedCountries = null
                    )
                )
            }
        }

        assertThat(getStartRoute(configuration)).isEqualTo(AuthRoute.Phone)
    }

    @Test
    fun `single google provider starts at method picker`() {
        val configuration = authUIConfiguration {
            context = applicationContext
            providers {
                provider(
                    AuthProvider.Google(
                        scopes = emptyList(),
                        serverClientId = "test-client-id"
                    )
                )
            }
        }

        assertThat(getStartRoute(configuration)).isEqualTo(AuthRoute.MethodPicker)
    }

    @Test
    fun `single email provider shows picker when always shown is enabled`() {
        val configuration = authUIConfiguration {
            context = applicationContext
            providers {
                provider(
                    AuthProvider.Email(
                        emailLinkActionCodeSettings = null,
                        passwordValidationRules = emptyList()
                    )
                )
            }
            isProviderChoiceAlwaysShown = true
        }

        assertThat(getStartRoute(configuration)).isEqualTo(AuthRoute.MethodPicker)
    }

    @Test
    fun `multiple providers start at method picker`() {
        val configuration = authUIConfiguration {
            context = applicationContext
            providers {
                provider(
                    AuthProvider.Email(
                        emailLinkActionCodeSettings = null,
                        passwordValidationRules = emptyList()
                    )
                )
                provider(
                    AuthProvider.Phone(
                        defaultNumber = null,
                        defaultCountryCode = null,
                        allowedCountries = null
                    )
                )
            }
        }

        assertThat(getStartRoute(configuration)).isEqualTo(AuthRoute.MethodPicker)
    }
}
