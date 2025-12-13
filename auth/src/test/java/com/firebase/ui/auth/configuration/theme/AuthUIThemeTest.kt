package com.firebase.ui.auth.configuration.theme

import android.content.Context
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.ShapeDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.test.core.app.ApplicationProvider
import com.firebase.ui.auth.configuration.AuthUIConfiguration
import com.firebase.ui.auth.configuration.authUIConfiguration
import com.firebase.ui.auth.configuration.auth_provider.AuthProvider
import com.firebase.ui.auth.ui.screens.FirebaseAuthScreen
import com.google.common.truth.Truth.assertThat
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@Config(sdk = [34])
@RunWith(RobolectricTestRunner::class)
class AuthUIThemeTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var applicationContext: Context

    @Before
    fun setup() {
        applicationContext = ApplicationProvider.getApplicationContext()

        // Clear any existing Firebase apps
        FirebaseApp.getApps(applicationContext).forEach { app ->
            app.delete()
        }

        // Initialize default FirebaseApp
        FirebaseApp.initializeApp(
            applicationContext,
            FirebaseOptions.Builder()
                .setApiKey("fake-api-key")
                .setApplicationId("fake-app-id")
                .setProjectId("fake-project-id")
                .build()
        )
    }

    private fun createTestConfiguration(theme: AuthUITheme? = null): AuthUIConfiguration {
        return authUIConfiguration {
            this.context = this@AuthUIThemeTest.applicationContext
            this.theme = theme
            providers {
                provider(
                    AuthProvider.Email(
                        emailLinkActionCodeSettings = null,
                        passwordValidationRules = emptyList()
                    )
                )
            }
        }
    }

    // ========================================================================
    // Basic Theme Tests
    // ========================================================================

    @Test
    fun `Default AuthUITheme applies to MaterialTheme`() {
        val theme = AuthUITheme.Default

        composeTestRule.setContent {
            AuthUITheme {
                assertThat(MaterialTheme.colorScheme).isEqualTo(theme.colorScheme)
                assertThat(MaterialTheme.typography).isEqualTo(theme.typography)
                assertThat(MaterialTheme.shapes).isEqualTo(theme.shapes)
            }
        }
    }

    @Test
    fun `AuthUITheme synchronizes with MaterialTheme`() {
        val theme = AuthUITheme.DefaultDark

        var authUIThemeColors: ColorScheme? = null
        var materialThemeColors: ColorScheme? = null

        composeTestRule.setContent {
            AuthUITheme(theme = theme) {
                authUIThemeColors = LocalAuthUITheme.current.colorScheme
                materialThemeColors = MaterialTheme.colorScheme
            }
        }

        composeTestRule.waitForIdle()

        assertThat(authUIThemeColors).isEqualTo(materialThemeColors)
    }

    @Test
    fun `AuthUITheme Default uses light color scheme`() {
        val expectedLightColors = lightColorScheme()

        composeTestRule.setContent {
            AuthUITheme(theme = AuthUITheme.Default) {
                val colors = LocalAuthUITheme.current.colorScheme
                assertThat(colors.primary).isEqualTo(expectedLightColors.primary)
                assertThat(colors.background).isEqualTo(expectedLightColors.background)
                assertThat(colors.surface).isEqualTo(expectedLightColors.surface)
            }
        }
    }

    @Test
    fun `AuthUITheme DefaultDark uses dark color scheme`() {
        val expectedDarkColors = darkColorScheme()

        composeTestRule.setContent {
            AuthUITheme(theme = AuthUITheme.DefaultDark) {
                val colors = LocalAuthUITheme.current.colorScheme
                assertThat(colors.primary).isEqualTo(expectedDarkColors.primary)
                assertThat(colors.background).isEqualTo(expectedDarkColors.background)
                assertThat(colors.surface).isEqualTo(expectedDarkColors.surface)
            }
        }
    }

    // ========================================================================
    // Theme Inheritance & Precedence Tests
    // ========================================================================

    @Test
    fun `Configuration theme takes precedence over wrapper theme`() {
        val wrapperTheme = AuthUITheme.DefaultDark
        val configurationTheme = AuthUITheme.Default

        var insideFirebaseAuthScreenTheme: AuthUITheme? = null

        composeTestRule.setContent {
            val configuration = createTestConfiguration(theme = configurationTheme)

            AuthUITheme(theme = wrapperTheme) {
                FirebaseAuthScreen(
                    configuration = configuration,
                    onSignInSuccess = {},
                    onSignInFailure = {},
                    onSignInCancelled = {},
                    authenticatedContent = { _, _ ->
                        insideFirebaseAuthScreenTheme = LocalAuthUITheme.current
                        Text("Test")
                    }
                )
            }
        }

        composeTestRule.waitForIdle()

        assertThat(insideFirebaseAuthScreenTheme?.colorScheme).isEqualTo(configurationTheme.colorScheme)
    }

    @Test
    fun `Wrapper theme applies when configuration theme is null`() {
        val wrapperTheme = AuthUITheme.DefaultDark

        var insideWrapperTheme: AuthUITheme? = null
        var insideFirebaseAuthScreenTheme: AuthUITheme? = null

        composeTestRule.setContent {
            val configuration = createTestConfiguration(theme = null)

            AuthUITheme(theme = wrapperTheme) {
                insideWrapperTheme = LocalAuthUITheme.current

                FirebaseAuthScreen(
                    configuration = configuration,
                    onSignInSuccess = {},
                    onSignInFailure = {},
                    onSignInCancelled = {},
                    authenticatedContent = { _, _ ->
                        insideFirebaseAuthScreenTheme = LocalAuthUITheme.current
                        Text("Test")
                    }
                )
            }
        }

        composeTestRule.waitForIdle()

        assertThat(insideFirebaseAuthScreenTheme?.colorScheme).isEqualTo(wrapperTheme.colorScheme)
        assertThat(insideWrapperTheme?.colorScheme).isEqualTo(insideFirebaseAuthScreenTheme?.colorScheme)
    }

    @Test
    fun `Default theme applies when no theme specified`() {
        var insideFirebaseAuthScreenTheme: AuthUITheme? = null

        composeTestRule.setContent {
            val configuration = createTestConfiguration(theme = null)

            FirebaseAuthScreen(
                configuration = configuration,
                onSignInSuccess = {},
                onSignInFailure = {},
                onSignInCancelled = {},
                authenticatedContent = { _, _ ->
                    insideFirebaseAuthScreenTheme = LocalAuthUITheme.current
                    Text("Test")
                }
            )
        }

        composeTestRule.waitForIdle()

        assertThat(insideFirebaseAuthScreenTheme).isEqualTo(AuthUITheme.Default)
    }

    // ========================================================================
    // Adaptive Theme Tests
    // ========================================================================

    @Test
    fun `Adaptive theme resolves to Default or DefaultDark`() {
        var adaptiveTheme: AuthUITheme? = null

        composeTestRule.setContent {
            adaptiveTheme = AuthUITheme.Adaptive
        }

        composeTestRule.waitForIdle()

        assertThat(adaptiveTheme).isIn(listOf(AuthUITheme.Default, AuthUITheme.DefaultDark))
    }

    @Test
    fun `Adaptive theme in configuration applies correctly`() {
        var insideFirebaseAuthScreenTheme: AuthUITheme? = null
        var adaptiveThemeResolved: AuthUITheme? = null

        composeTestRule.setContent {
            adaptiveThemeResolved = AuthUITheme.Adaptive
            val configuration = createTestConfiguration(theme = adaptiveThemeResolved)

            FirebaseAuthScreen(
                configuration = configuration,
                onSignInSuccess = {},
                onSignInFailure = {},
                onSignInCancelled = {},
                authenticatedContent = { _, _ ->
                    insideFirebaseAuthScreenTheme = LocalAuthUITheme.current
                    Text("Test")
                }
            )
        }

        composeTestRule.waitForIdle()

        assertThat(insideFirebaseAuthScreenTheme?.colorScheme).isEqualTo(adaptiveThemeResolved?.colorScheme)
    }

    // ========================================================================
    // Customization Tests
    // ========================================================================

    @Test
    fun `Copy with custom provider button shape applies correctly`() {
        val customShape = ShapeDefaults.ExtraLarge
        val customTheme = AuthUITheme.Default.copy(
            providerButtonShape = customShape
        )

        var observedProviderButtonShape: Shape? = null

        composeTestRule.setContent {
            val configuration = createTestConfiguration(theme = customTheme)

            FirebaseAuthScreen(
                configuration = configuration,
                onSignInSuccess = {},
                onSignInFailure = {},
                onSignInCancelled = {},
                authenticatedContent = { _, _ ->
                    observedProviderButtonShape = LocalAuthUITheme.current.providerButtonShape
                    Text("Test")
                }
            )
        }

        composeTestRule.waitForIdle()

        assertThat(observedProviderButtonShape).isEqualTo(customShape)
    }

    @Test
    fun `Copy preserves other properties`() {
        val customStyles = mapOf(
            "google.com" to AuthUITheme.ProviderStyle(
                icon = null,
                backgroundColor = Color.Red,
                contentColor = Color.White
            )
        )

        val original = AuthUITheme.Default.copy(
            providerButtonShape = RoundedCornerShape(12.dp),
            providerStyles = customStyles
        )

        val copied = original.copy(
            providerButtonShape = RoundedCornerShape(20.dp)
        )

        var observedProviderStyles: Map<String, AuthUITheme.ProviderStyle>? = null
        var observedProviderButtonShape: Shape? = null

        composeTestRule.setContent {
            val configuration = createTestConfiguration(theme = copied)

            FirebaseAuthScreen(
                configuration = configuration,
                onSignInSuccess = {},
                onSignInFailure = {},
                onSignInCancelled = {},
                authenticatedContent = { _, _ ->
                    observedProviderStyles = LocalAuthUITheme.current.providerStyles
                    observedProviderButtonShape = LocalAuthUITheme.current.providerButtonShape
                    Text("Test")
                }
            )
        }

        composeTestRule.waitForIdle()

        assertThat(observedProviderButtonShape).isEqualTo(RoundedCornerShape(20.dp))
        assertThat(observedProviderStyles).containsKey("google.com")
        assertThat(observedProviderStyles?.get("google.com")?.backgroundColor).isEqualTo(Color.Red)
    }

    // ========================================================================
    // fromMaterialTheme Tests
    // ========================================================================

    @Test
    fun `fromMaterialTheme inherits MaterialTheme values`() {
        val appLightColorScheme = lightColorScheme(
            primary = Color(0xFF6650a4),
            secondary = Color(0xFF625b71),
            tertiary = Color(0xFF7D5260)
        )

        val appTypography = Typography(
            bodyLarge = TextStyle(
                fontFamily = FontFamily.Default,
                fontWeight = FontWeight.Normal,
                fontSize = 16.sp,
                lineHeight = 24.sp,
                letterSpacing = 0.5.sp
            )
        )

        val appShapes = Shapes(extraSmall = RoundedCornerShape(13.dp))

        composeTestRule.setContent {
            MaterialTheme(
                colorScheme = appLightColorScheme,
                typography = appTypography,
                shapes = appShapes,
            ) {
                AuthUITheme(
                    theme = AuthUITheme.fromMaterialTheme()
                ) {
                    assertThat(MaterialTheme.colorScheme).isEqualTo(appLightColorScheme)
                    assertThat(MaterialTheme.typography).isEqualTo(appTypography)
                    assertThat(MaterialTheme.shapes).isEqualTo(appShapes)
                }
            }
        }
    }

    @Test
    fun `fromMaterialTheme inherits all properties completely`() {
        val customColorScheme = lightColorScheme(
            primary = Color(0xFFFF0000),
            background = Color(0xFFFFFFFF)
        )
        val customTypography = Typography(
            bodyLarge = TextStyle(fontSize = 18.sp)
        )
        val customShapes = Shapes(
            small = RoundedCornerShape(8.dp)
        )

        var observedColorScheme: ColorScheme? = null
        var observedTypography: Typography? = null
        var observedShapes: Shapes? = null

        composeTestRule.setContent {
            MaterialTheme(
                colorScheme = customColorScheme,
                typography = customTypography,
                shapes = customShapes
            ) {
                val theme = AuthUITheme.fromMaterialTheme(
                    providerButtonShape = RoundedCornerShape(16.dp)
                )
                val configuration = createTestConfiguration(theme = theme)

                FirebaseAuthScreen(
                    configuration = configuration,
                    onSignInSuccess = {},
                    onSignInFailure = {},
                    onSignInCancelled = {},
                    authenticatedContent = { _, _ ->
                        observedColorScheme = LocalAuthUITheme.current.colorScheme
                        observedTypography = LocalAuthUITheme.current.typography
                        observedShapes = LocalAuthUITheme.current.shapes
                        Text("Test")
                    }
                )
            }
        }

        composeTestRule.waitForIdle()

        assertThat(observedColorScheme?.primary).isEqualTo(Color(0xFFFF0000))
        assertThat(observedTypography).isEqualTo(customTypography)
        assertThat(observedShapes).isEqualTo(customShapes)
    }

    @Test
    fun `fromMaterialTheme with custom provider button shape`() {
        val customShape = RoundedCornerShape(16.dp)

        var observedProviderButtonShape: Shape? = null

        composeTestRule.setContent {
            MaterialTheme {
                val theme = AuthUITheme.fromMaterialTheme(
                    providerButtonShape = customShape
                )

                AuthUITheme(theme = theme) {
                    observedProviderButtonShape = LocalAuthUITheme.current.providerButtonShape
                }
            }
        }

        composeTestRule.waitForIdle()

        assertThat(observedProviderButtonShape).isEqualTo(customShape)
    }
}
