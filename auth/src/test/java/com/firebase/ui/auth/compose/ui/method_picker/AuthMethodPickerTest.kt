package com.firebase.ui.auth.compose.ui.method_picker

import android.content.Context
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.test.core.app.ApplicationProvider
import com.firebase.ui.auth.R
import com.firebase.ui.auth.compose.configuration.auth_provider.AuthProvider
import com.firebase.ui.auth.compose.configuration.theme.AuthUIAsset
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

    // =============================================================================================
    // Basic UI Tests
    // =============================================================================================

    @Test
    fun `AuthMethodPicker displays all providers`() {
        val providers = listOf(
            AuthProvider.Google(scopes = emptyList(), serverClientId = null),
            AuthProvider.Facebook(),
            AuthProvider.Email(actionCodeSettings = null, passwordValidationRules = emptyList())
        )

        composeTestRule.setContent {
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

        composeTestRule.setContent {
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

        composeTestRule.setContent {
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

        composeTestRule.setContent {
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

        composeTestRule.setContent {
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

        composeTestRule.setContent {
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

        composeTestRule.setContent {
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

        composeTestRule.setContent {
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

        composeTestRule.setContent {
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
            AuthProvider.Email(actionCodeSettings = null, passwordValidationRules = emptyList()),
            AuthProvider.Phone(
                defaultNumber = null,
                defaultCountryCode = null,
                allowedCountries = null
            ),
            AuthProvider.Anonymous
        )

        composeTestRule.setContent {
            AuthMethodPicker(
                providers = providers,
                onProviderSelected = { selectedProvider = it }
            )
        }

        composeTestRule
            .onNodeWithText(context.getString(R.string.fui_sign_in_with_google))
            .assertIsDisplayed()

        composeTestRule
            .onNodeWithTag("AuthMethodPicker LazyColumn")
            .performScrollToNode(hasText(context.getString(R.string.fui_sign_in_anonymously)))

        composeTestRule
            .onNodeWithText(context.getString(R.string.fui_sign_in_anonymously))
            .assertIsDisplayed()
    }
}