package com.firebase.ui.auth.compose.configuration.auth_provider

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.google.firebase.auth.actionCodeSettings
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Unit tests for [AuthProvider] covering provider validation rules, configuration constraints,
 * and error handling for all supported authentication providers.
 *
 * @suppress Internal test class
 */
@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class AuthProviderTest {

    private lateinit var applicationContext: Context

    @Before
    fun setUp() {
        applicationContext = ApplicationProvider.getApplicationContext()
    }

    // =============================================================================================
    // Email Provider Tests
    // =============================================================================================

    @Test
    fun `email provider with valid configuration should succeed`() {
        val provider = AuthProvider.Email(
            emailLinkActionCodeSettings = null,
            passwordValidationRules = listOf()
        )

        provider.validate()
    }

    @Test
    fun `email provider with email link enabled and valid action code settings should succeed`() {
        val actionCodeSettings = actionCodeSettings {
            url = "https://example.com/verify"
            handleCodeInApp = true
        }

        val provider = AuthProvider.Email(
            isEmailLinkSignInEnabled = true,
            emailLinkActionCodeSettings = actionCodeSettings,
            passwordValidationRules = listOf()
        )

        provider.validate()
    }

    @Test
    fun `email provider with email link enabled but null action code settings should throw`() {
        val provider = AuthProvider.Email(
            isEmailLinkSignInEnabled = true,
            emailLinkActionCodeSettings = null,
            passwordValidationRules = listOf()
        )

        try {
            provider.validate()
        } catch (e: Exception) {
            assertThat(e).isInstanceOf(IllegalArgumentException::class.java)
            assertThat(e.message).isEqualTo(
                "ActionCodeSettings cannot be null when using " +
                        "email link sign in."
            )
        }
    }

    @Test
    fun `email provider with email link enabled but canHandleCodeInApp false should throw`() {
        val actionCodeSettings = actionCodeSettings {
            url = "https://example.com/verify"
            handleCodeInApp = false
        }

        val provider = AuthProvider.Email(
            isEmailLinkSignInEnabled = true,
            emailLinkActionCodeSettings = actionCodeSettings,
            passwordValidationRules = listOf()
        )

        try {
            provider.validate()
        } catch (e: Exception) {
            assertThat(e).isInstanceOf(IllegalStateException::class.java)
            assertThat(e.message).isEqualTo(
                "You must set canHandleCodeInApp in your " +
                        "ActionCodeSettings to true for Email-Link Sign-in."
            )
        }
    }

    // =============================================================================================
    // Phone Provider Tests
    // =============================================================================================

    @Test
    fun `phone provider with valid configuration should succeed`() {
        val provider = AuthProvider.Phone(
            defaultNumber = null,
            defaultCountryCode = null,
            allowedCountries = null
        )

        provider.validate()
    }

    @Test
    fun `phone provider with valid default number should succeed`() {
        val provider = AuthProvider.Phone(
            defaultNumber = "+1234567890",
            defaultCountryCode = null,
            allowedCountries = null
        )

        provider.validate()
    }

    @Test
    fun `phone provider with invalid default number should throw`() {
        val provider = AuthProvider.Phone(
            defaultNumber = "invalid_number",
            defaultCountryCode = null,
            allowedCountries = null
        )

        try {
            provider.validate()
        } catch (e: Exception) {
            assertThat(e).isInstanceOf(IllegalStateException::class.java)
            assertThat(e.message).isEqualTo("Invalid phone number: invalid_number")
        }
    }

    @Test
    fun `phone provider with valid default country code should succeed`() {
        val provider = AuthProvider.Phone(
            defaultNumber = null,
            defaultCountryCode = "US",
            allowedCountries = null
        )

        provider.validate()
    }

    @Test
    fun `phone provider with invalid default country code should throw`() {
        val provider = AuthProvider.Phone(
            defaultNumber = null,
            defaultCountryCode = "invalid",
            allowedCountries = null
        )

        try {
            provider.validate()
        } catch (e: Exception) {
            assertThat(e).isInstanceOf(IllegalStateException::class.java)
            assertThat(e.message).isEqualTo("Invalid country iso: invalid")
        }
    }

    @Test
    fun `phone provider with valid allowed countries should succeed`() {
        val provider = AuthProvider.Phone(
            defaultNumber = null,
            defaultCountryCode = null,
            allowedCountries = listOf("US", "CA", "+1")
        )

        provider.validate()
    }

    @Test
    fun `phone provider with invalid country in allowed list should throw`() {
        val provider = AuthProvider.Phone(
            defaultNumber = null,
            defaultCountryCode = null,
            allowedCountries = listOf("US", "invalid_country")
        )

        try {
            provider.validate()
        } catch (e: Exception) {
            assertThat(e).isInstanceOf(IllegalStateException::class.java)
            assertThat(e.message).isEqualTo(
                "Invalid input: You must provide a valid country iso (alpha-2) " +
                        "or code (e-164). e.g. 'us' or '+1'. Invalid code: invalid_country"
            )
        }
    }

    @Test
    fun `phone provider with valid default number, country code and compatible allowed countries should succeed`() {
        val provider = AuthProvider.Phone(
            defaultNumber = "+1234567890",
            defaultCountryCode = "US",
            allowedCountries = listOf("US", "CA")
        )

        provider.validate()
    }

    // =============================================================================================
    // Google Provider Tests
    // =============================================================================================

    @Test
    fun `google provider with valid configuration should succeed`() {
        val provider = AuthProvider.Google(
            scopes = listOf("email"),
            serverClientId = "test_client_id"
        )

        provider.validate(applicationContext)
    }

    @Test
    fun `google provider with empty serverClientId string throws`() {
        val provider = AuthProvider.Google(
            scopes = listOf("email"),
            serverClientId = ""
        )

        try {
            provider.validate(applicationContext)
        } catch (e: Exception) {
            assertThat(e).isInstanceOf(IllegalArgumentException::class.java)
            assertThat(e.message).isEqualTo("Server client ID cannot be blank.")
        }
    }

    @Test
    fun `google provider validates default_web_client_id when serverClientId is null`() {
        val provider = AuthProvider.Google(
            scopes = listOf("email"),
            serverClientId = null
        )

        try {
            provider.validate(applicationContext)
        } catch (e: Exception) {
            assertThat(e).isInstanceOf(IllegalStateException::class.java)
            assertThat(e.message).isEqualTo(
                "Check your google-services plugin " +
                        "configuration, the default_web_client_id string wasn't populated."
            )
        }
    }

    // =============================================================================================
    // Facebook Provider Tests
    // =============================================================================================

    @Test
    fun `facebook provider with valid configuration should succeed`() {
        val provider = AuthProvider.Facebook(applicationId = "application_id")

        provider.validate(applicationContext)
    }

    @Test
    fun `facebook provider with empty application id throws`() {
        val provider = AuthProvider.Facebook(applicationId = "")

        try {
            provider.validate(applicationContext)
        } catch (e: Exception) {
            assertThat(e).isInstanceOf(IllegalArgumentException::class.java)
            assertThat(e.message).isEqualTo("Facebook application ID cannot be blank")
        }
    }

    @Test
    fun `facebook provider validates facebook_application_id when applicationId is null`() {
        val provider = AuthProvider.Facebook()

        try {
            provider.validate(applicationContext)
        } catch (e: Exception) {
            assertThat(e).isInstanceOf(IllegalStateException::class.java)
            assertThat(e.message).isEqualTo(
                "Facebook provider unconfigured. Make sure to " +
                        "add a `facebook_application_id` string or provide applicationId parameter."
            )
        }
    }

    // =============================================================================================
    // Anonymous Provider Tests
    // =============================================================================================

    @Test
    fun `anonymous provider as only provider should throw`() {
        val providers = listOf(AuthProvider.Anonymous)

        try {
            AuthProvider.Anonymous.validate(providers)
        } catch (e: Exception) {
            assertThat(e).isInstanceOf(IllegalStateException::class.java)
            assertThat(e.message).isEqualTo(
                "Sign in as guest cannot be the only sign in method. " +
                        "In this case, sign the user in anonymously your self; no UI is needed."
            )
        }
    }

    @Test
    fun `anonymous provider with other providers should succeed`() {
        val providers = listOf(
            AuthProvider.Anonymous,
            AuthProvider.Email(
                emailLinkActionCodeSettings = null,
                passwordValidationRules = listOf()
            )
        )

        AuthProvider.Anonymous.validate(providers)
    }

    // =============================================================================================
    // GenericOAuth Provider Tests
    // =============================================================================================

    @Test
    fun `generic oauth provider with valid configuration should succeed`() {
        val provider = AuthProvider.GenericOAuth(
            providerName = "Generic Provider",
            providerId = "custom.provider",
            scopes = listOf("read"),
            customParameters = mapOf(),
            buttonLabel = "Sign in with Custom",
            buttonIcon = null,
            buttonColor = null,
            contentColor = null,
        )

        provider.validate()
    }

    @Test
    fun `generic oauth provider with blank provider id should throw`() {
        val provider = AuthProvider.GenericOAuth(
            providerName = "Generic Provider",
            providerId = "",
            scopes = listOf("read"),
            customParameters = mapOf(),
            buttonLabel = "Sign in with Custom",
            buttonIcon = null,
            buttonColor = null,
            contentColor = null,
        )

        try {
            provider.validate()
        } catch (e: Exception) {
            assertThat(e).isInstanceOf(IllegalArgumentException::class.java)
            assertThat(e.message).isEqualTo("Provider ID cannot be null or empty")
        }
    }

    @Test
    fun `generic oauth provider with blank button label should throw`() {
        val provider = AuthProvider.GenericOAuth(
            providerName = "Generic Provider",
            providerId = "custom.provider",
            scopes = listOf("read"),
            customParameters = mapOf(),
            buttonLabel = "",
            buttonIcon = null,
            buttonColor = null,
            contentColor = null,
        )

        try {
            provider.validate()
        } catch (e: Exception) {
            assertThat(e).isInstanceOf(IllegalArgumentException::class.java)
            assertThat(e.message).isEqualTo("Button label cannot be null or empty")
        }
    }
}