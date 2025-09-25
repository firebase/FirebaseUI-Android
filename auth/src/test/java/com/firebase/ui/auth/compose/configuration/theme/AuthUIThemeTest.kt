package com.firebase.ui.auth.compose.configuration.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.common.truth.Truth.assertThat
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
    fun `fromMaterialTheme inherits client MaterialTheme values`() {
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
                    assertThat(MaterialTheme.colorScheme)
                        .isEqualTo(appLightColorScheme)
                    assertThat(MaterialTheme.typography)
                        .isEqualTo(appTypography)
                    assertThat(MaterialTheme.shapes)
                        .isEqualTo(appShapes)
                }
            }
        }
    }
}
