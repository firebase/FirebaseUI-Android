package com.firebase.ui.auth.ui.method_picker

import android.content.Context
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.test.core.app.ApplicationProvider
import com.firebase.ui.auth.R
import com.firebase.ui.auth.configuration.auth_provider.AuthProvider
import com.firebase.ui.auth.ui.FirebaseAuthTestTags
import com.firebase.ui.auth.ui.method_picker.MethodPickerTermsConfiguration
import com.firebase.ui.auth.configuration.string_provider.DefaultAuthUIStringProvider
import com.firebase.ui.auth.configuration.string_provider.LocalAuthUIStringProvider
import com.firebase.ui.auth.configuration.theme.AuthUIAsset
import com.firebase.ui.auth.util.SignInPreferenceManager
import com.google.common.truth.Truth
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Unit tests for [AuthMethodPicker] covering UI interactions, provider selection,
 * scroll tests, logo display, and custom layouts.
 *
 * @suppress Internal test class
 */
@Config(sdk = [34])
@RunWith(RobolectricTestRunner::class)
class AuthMethodPickerTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var context: Context
    private var selectedProvider: AuthProvider? = null

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        selectedProvider = null
    }

    private fun setContentWithStringProvider(content: @Composable () -> Unit) {
        composeTestRule.setContent {
            CompositionLocalProvider(
                LocalAuthUIStringProvider provides DefaultAuthUIStringProvider(context)
            ) {
                content()
            }
        }
    }

    // =============================================================================================
    // Basic UI Tests
    // =============================================================================================

    @Test
    fun `AuthMethodPicker displays all providers`() {
        val providers = listOf(
            AuthProvider.Google(scopes = emptyList(), serverClientId = null),
            AuthProvider.Facebook(),
            AuthProvider.Email(
                emailLinkActionCodeSettings = null,
                passwordValidationRules = emptyList()
            )
        )

        setContentWithStringProvider {
            AuthMethodPicker(
                providers = providers,
                onProviderSelected = { selectedProvider = it }
            )
        }

        composeTestRule
            .onNodeWithText(context.getString(R.string.fui_sign_in_with_google))
            .assertIsDisplayed()
            .assertHasClickAction()

        composeTestRule
            .onNodeWithText(context.getString(R.string.fui_sign_in_with_facebook))
            .assertIsDisplayed()
            .assertHasClickAction()

        composeTestRule
            .onNodeWithText(context.getString(R.string.fui_sign_in_with_email))
            .assertIsDisplayed()
            .assertHasClickAction()
    }

    @Test
    fun `AuthMethodPicker displays terms of service text`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val links = arrayOf("Terms of Service" to "", "Privacy Policy" to "")
        val labels = links.map { it.first }.toTypedArray()
        val providers = listOf(
            AuthProvider.Google(scopes = emptyList(), serverClientId = null)
        )

        setContentWithStringProvider {
            AuthMethodPicker(
                providers = providers,
                onProviderSelected = { selectedProvider = it }
            )
        }

        composeTestRule
            .onNodeWithText(context.getString(R.string.fui_tos_and_pp, *labels))
            .assertIsDisplayed()
    }

    @Test
    fun `AuthMethodPicker displays logo when provided`() {
        val providers = listOf(
            AuthProvider.Google(scopes = emptyList(), serverClientId = null)
        )

        setContentWithStringProvider {
            AuthMethodPicker(
                providers = providers,
                logo = AuthUIAsset.Resource(R.drawable.fui_ic_check_circle_black_128dp),
                onProviderSelected = { selectedProvider = it }
            )
        }

        composeTestRule
            .onNodeWithContentDescription(context.getString(R.string.fui_auth_method_picker_logo))
            .assertIsDisplayed()
    }

    @Test
    fun `AuthMethodPicker does not display logo when null`() {
        val providers = listOf(
            AuthProvider.Google(scopes = emptyList(), serverClientId = null)
        )

        setContentWithStringProvider {
            AuthMethodPicker(
                providers = providers,
                logo = null,
                onProviderSelected = { selectedProvider = it }
            )
        }

        composeTestRule
            .onNodeWithContentDescription(context.getString(R.string.fui_auth_method_picker_logo))
            .assertIsNotDisplayed()
    }

    @Test
    fun `AuthMethodPicker displays logo and providers together`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val links = arrayOf("Terms of Service" to "", "Privacy Policy" to "")
        val labels = links.map { it.first }.toTypedArray()
        val providers = listOf(
            AuthProvider.Google(scopes = emptyList(), serverClientId = null)
        )

        setContentWithStringProvider {
            AuthMethodPicker(
                providers = providers,
                logo = AuthUIAsset.Resource(R.drawable.fui_ic_check_circle_black_128dp),
                onProviderSelected = { selectedProvider = it }
            )
        }

        composeTestRule
            .onNodeWithContentDescription(context.getString(R.string.fui_auth_method_picker_logo))
            .assertIsDisplayed()

        composeTestRule
            .onNodeWithText(context.getString(R.string.fui_sign_in_with_google))
            .assertIsDisplayed()

        composeTestRule
            .onNodeWithText(context.getString(R.string.fui_tos_and_pp, *labels))
            .assertIsDisplayed()
    }

    @Test
    fun `AuthMethodPicker calls onProviderSelected when Provider is clicked`() {
        val googleProvider = AuthProvider.Google(scopes = emptyList(), serverClientId = null)
        val providers = listOf(googleProvider)

        setContentWithStringProvider {
            AuthMethodPicker(
                providers = providers,
                onProviderSelected = { selectedProvider = it }
            )
        }

        composeTestRule
            .onNodeWithText(context.getString(R.string.fui_sign_in_with_google))
            .performClick()

        Truth.assertThat(selectedProvider).isEqualTo(googleProvider)
    }

    // =============================================================================================
    // Custom Layout Tests
    // =============================================================================================

    @Test
    fun `AuthMethodPicker uses custom layout when provided`() {
        val providers = listOf(
            AuthProvider.Google(scopes = emptyList(), serverClientId = null)
        )
        var customLayoutCalled = false

        setContentWithStringProvider {
            AuthMethodPicker(
                providers = providers,
                onProviderSelected = { selectedProvider = it },
                customLayout = { _, _ ->
                    customLayoutCalled = true
                    Text("Custom Layout")
                }
            )
        }

        Truth.assertThat(customLayoutCalled).isTrue()
        composeTestRule
            .onNodeWithText("Custom Layout")
            .assertIsDisplayed()
    }

    @Test
    fun `AuthMethodPicker custom layout receives providers list`() {
        val providers = listOf(
            AuthProvider.Google(scopes = emptyList(), serverClientId = null),
            AuthProvider.Facebook()
        )
        var receivedProviders: List<AuthProvider>? = null

        setContentWithStringProvider {
            AuthMethodPicker(
                providers = providers,
                onProviderSelected = { selectedProvider = it },
                customLayout = { providersList, _ ->
                    receivedProviders = providersList
                }
            )
        }

        Truth.assertThat(receivedProviders).isEqualTo(providers)
    }

    @Test
    fun `AuthMethodPicker custom layout can trigger provider selection`() {
        val googleProvider = AuthProvider.Google(scopes = emptyList(), serverClientId = null)
        val providers = listOf(googleProvider)

        setContentWithStringProvider {
            AuthMethodPicker(
                providers = providers,
                onProviderSelected = { selectedProvider = it },
                customLayout = { providersList, onSelected ->
                    Button(onClick = { onSelected(providersList[0]) }) {
                        Text("Custom Button")
                    }
                }
            )
        }

        composeTestRule
            .onNodeWithText("Custom Button")
            .performClick()

        Truth.assertThat(selectedProvider).isEqualTo(googleProvider)
    }

    @Test
    fun `AuthMethodPicker still renders default ToS text when customLayout is provided`() {
        val links = arrayOf("Terms of Service" to "", "Privacy Policy" to "")
        val labels = links.map { it.first }.toTypedArray()
        val providers = listOf(
            AuthProvider.Google(scopes = emptyList(), serverClientId = null)
        )

        setContentWithStringProvider {
            AuthMethodPicker(
                providers = providers,
                onProviderSelected = { selectedProvider = it },
                customLayout = { _, _ -> Text("Custom Layout") }
            )
        }

        composeTestRule
            .onNodeWithText(context.getString(R.string.fui_tos_and_pp, *labels))
            .assertIsDisplayed()
    }

    // =============================================================================================
    // Custom Terms Content Tests
    // =============================================================================================

    @Test
    fun `AuthMethodPicker renders termsConfiguration content instead of default ToS when provided`() {
        val links = arrayOf("Terms of Service" to "", "Privacy Policy" to "")
        val labels = links.map { it.first }.toTypedArray()
        val providers = listOf(
            AuthProvider.Google(scopes = emptyList(), serverClientId = null)
        )

        setContentWithStringProvider {
            AuthMethodPicker(
                providers = providers,
                onProviderSelected = { selectedProvider = it },
                termsConfiguration = MethodPickerTermsConfiguration(
                    content = { Text("Custom ToS checkbox") }
                )
            )
        }

        composeTestRule
            .onNodeWithText("Custom ToS checkbox")
            .assertIsDisplayed()

        composeTestRule
            .onNodeWithText(context.getString(R.string.fui_tos_and_pp, *labels))
            .assertDoesNotExist()
    }

    @Test
    fun `AuthMethodPicker still renders providers when termsConfiguration is provided`() {
        val providers = listOf(
            AuthProvider.Google(scopes = emptyList(), serverClientId = null)
        )

        setContentWithStringProvider {
            AuthMethodPicker(
                providers = providers,
                onProviderSelected = { selectedProvider = it },
                termsConfiguration = MethodPickerTermsConfiguration(
                    content = { Text("Custom ToS checkbox") }
                )
            )
        }

        composeTestRule
            .onNodeWithText(context.getString(R.string.fui_sign_in_with_google))
            .assertIsDisplayed()
    }

    // =============================================================================================
    // Terms Accepted / Gating Tests
    // =============================================================================================

    @Test
    fun `AuthMethodPicker disables provider buttons when disableProvidersUntilAccepted is true and accepted is false`() {
        val googleProvider = AuthProvider.Google(scopes = emptyList(), serverClientId = null)

        setContentWithStringProvider {
            AuthMethodPicker(
                providers = listOf(googleProvider),
                onProviderSelected = { selectedProvider = it },
                termsConfiguration = MethodPickerTermsConfiguration(
                    content = { Text("Checkbox") },
                    accepted = false,
                    disableProvidersUntilAccepted = true
                )
            )
        }

        composeTestRule
            .onNodeWithText(context.getString(R.string.fui_sign_in_with_google))
            .assertIsNotEnabled()
    }

    @Test
    fun `AuthMethodPicker enables provider buttons when disableProvidersUntilAccepted is true and accepted is true`() {
        val googleProvider = AuthProvider.Google(scopes = emptyList(), serverClientId = null)

        setContentWithStringProvider {
            AuthMethodPicker(
                providers = listOf(googleProvider),
                onProviderSelected = { selectedProvider = it },
                termsConfiguration = MethodPickerTermsConfiguration(
                    content = { Text("Checkbox") },
                    accepted = true,
                    disableProvidersUntilAccepted = true
                )
            )
        }

        composeTestRule
            .onNodeWithText(context.getString(R.string.fui_sign_in_with_google))
            .assertIsEnabled()
    }

    @Test
    fun `AuthMethodPicker ignores accepted when disableProvidersUntilAccepted is false`() {
        val googleProvider = AuthProvider.Google(scopes = emptyList(), serverClientId = null)

        setContentWithStringProvider {
            AuthMethodPicker(
                providers = listOf(googleProvider),
                onProviderSelected = { selectedProvider = it },
                termsConfiguration = MethodPickerTermsConfiguration(
                    content = { Text("Checkbox") },
                    accepted = false,
                    disableProvidersUntilAccepted = false
                )
            )
        }

        composeTestRule
            .onNodeWithText(context.getString(R.string.fui_sign_in_with_google))
            .assertIsEnabled()
    }

    // =============================================================================================
    // Scrolling Tests
    // =============================================================================================

    @Test
    fun `AuthMethodPicker allows scrolling through many providers`() {
        val providers = listOf(
            AuthProvider.Google(scopes = emptyList(), serverClientId = null),
            AuthProvider.Facebook(),
            AuthProvider.Twitter(customParameters = emptyMap()),
            AuthProvider.Github(customParameters = emptyMap()),
            AuthProvider.Microsoft(tenant = null, customParameters = emptyMap()),
            AuthProvider.Yahoo(customParameters = emptyMap()),
            AuthProvider.Apple(locale = null, customParameters = emptyMap()),
            AuthProvider.Email(
                emailLinkActionCodeSettings = null,
                passwordValidationRules = emptyList()
            ),
            AuthProvider.Phone(
                defaultNumber = null,
                defaultCountryCode = null,
                allowedCountries = null
            ),
            AuthProvider.Anonymous
        )

        setContentWithStringProvider {
            AuthMethodPicker(
                providers = providers,
                onProviderSelected = { selectedProvider = it }
            )
        }

        composeTestRule
            .onNodeWithText(context.getString(R.string.fui_sign_in_with_google))
            .assertIsDisplayed()

        composeTestRule
            .onNodeWithTag(FirebaseAuthTestTags.MethodPicker.PROVIDER_LIST)
            .performScrollToNode(hasText(context.getString(R.string.fui_sign_in_anonymously)))

        composeTestRule
            .onNodeWithText(context.getString(R.string.fui_sign_in_anonymously))
            .assertIsDisplayed()
    }

    // =============================================================================================
    // Continue As Tests
    // =============================================================================================

    @Test
    fun `AuthMethodPicker shows ContinueAsButton when lastSignInPreference matches a provider`() {
        val emailProvider = AuthProvider.Email(
            emailLinkActionCodeSettings = null,
            passwordValidationRules = emptyList()
        )
        val preference = SignInPreferenceManager.SignInPreference(
            providerId = emailProvider.providerId,
            identifier = "user@example.com",
            timestamp = 0L
        )

        setContentWithStringProvider {
            AuthMethodPicker(
                providers = listOf(emailProvider),
                onProviderSelected = { selectedProvider = it },
                lastSignInPreference = preference
            )
        }

        // Load-bearing: catches the tag drifting onto a non-clickable wrapper around the button.
        composeTestRule
            .onNodeWithTag(FirebaseAuthTestTags.MethodPicker.CONTINUE_AS_BUTTON)
            .assertIsDisplayed()
            .assertHasClickAction()
    }

    @Test
    fun `AuthMethodPicker hides ContinueAsButton when there is no lastSignInPreference`() {
        setContentWithStringProvider {
            AuthMethodPicker(
                providers = listOf(
                    AuthProvider.Google(scopes = emptyList(), serverClientId = null)
                ),
                onProviderSelected = { selectedProvider = it }
            )
        }

        composeTestRule
            .onNodeWithTag(FirebaseAuthTestTags.MethodPicker.CONTINUE_AS_BUTTON)
            .assertDoesNotExist()
    }

    @Test
    fun `AuthMethodPicker hides ContinueAsButton when lastSignInPreference has no matching provider`() {
        val preference = SignInPreferenceManager.SignInPreference(
            providerId = "some.unlisted.provider",
            identifier = "user@example.com",
            timestamp = 0L
        )

        setContentWithStringProvider {
            AuthMethodPicker(
                providers = listOf(
                    AuthProvider.Google(scopes = emptyList(), serverClientId = null)
                ),
                onProviderSelected = { selectedProvider = it },
                lastSignInPreference = preference
            )
        }

        composeTestRule
            .onNodeWithTag(FirebaseAuthTestTags.MethodPicker.CONTINUE_AS_BUTTON)
            .assertDoesNotExist()
    }

    @Test
    fun `AuthMethodPicker calls onContinueAsSelected with provider and identifier when ContinueAsButton is clicked`() {
        val emailProvider = AuthProvider.Email(
            emailLinkActionCodeSettings = null,
            passwordValidationRules = emptyList()
        )
        val preference = SignInPreferenceManager.SignInPreference(
            providerId = emailProvider.providerId,
            identifier = "user@example.com",
            timestamp = 0L
        )
        var continueAsProvider: AuthProvider? = null
        var continueAsIdentifier: String? = null

        setContentWithStringProvider {
            AuthMethodPicker(
                providers = listOf(emailProvider),
                onProviderSelected = { selectedProvider = it },
                lastSignInPreference = preference,
                onContinueAsSelected = { provider, identifier ->
                    continueAsProvider = provider
                    continueAsIdentifier = identifier
                }
            )
        }

        composeTestRule
            .onNodeWithTag(FirebaseAuthTestTags.MethodPicker.CONTINUE_AS_BUTTON)
            .performClick()

        Truth.assertThat(continueAsProvider).isEqualTo(emailProvider)
        Truth.assertThat(continueAsIdentifier).isEqualTo("user@example.com")
        Truth.assertThat(selectedProvider).isNull()
    }

    @Test
    fun `AuthMethodPicker falls back to onProviderSelected when onContinueAsSelected is not provided`() {
        val emailProvider = AuthProvider.Email(
            emailLinkActionCodeSettings = null,
            passwordValidationRules = emptyList()
        )
        val preference = SignInPreferenceManager.SignInPreference(
            providerId = emailProvider.providerId,
            identifier = "user@example.com",
            timestamp = 0L
        )

        setContentWithStringProvider {
            AuthMethodPicker(
                providers = listOf(emailProvider),
                onProviderSelected = { selectedProvider = it },
                lastSignInPreference = preference
            )
        }

        composeTestRule
            .onNodeWithTag(FirebaseAuthTestTags.MethodPicker.CONTINUE_AS_BUTTON)
            .performClick()

        Truth.assertThat(selectedProvider).isEqualTo(emailProvider)
    }
}