package com.firebaseui.lint.internal

import com.android.tools.lint.checks.infrastructure.TestFiles.xml
import com.android.tools.lint.checks.infrastructure.TestLintTask
import com.firebaseui.lint.internal.UntranslatedResourceDetector.Companion.UNTRANSLATED_RESOURCE
import org.junit.Test

class UntranslatedResourceDetectorTest {

    private fun configuredLint(): TestLintTask = TestLintTask.lint().withLocalSdk()

    private fun base(vararg entries: String) = xml(
        "res/values/strings.xml",
        """
        |<resources>
        |${entries.joinToString("\n") { "    $it" }}
        |</resources>""".trimMargin()
    )

    private fun locale(folder: String, vararg entries: String) = xml(
        "res/values-$folder/strings.xml",
        """
        |<resources>
        |${entries.joinToString("\n") { "    $it" }}
        |</resources>""".trimMargin()
    )

    @Test
    fun `Passes on a translated string`() {
        configuredLint()
            .files(
                base("""<string name="fui_sign_in">Sign in</string>"""),
                locale("fr", """<string name="fui_sign_in">Se connecter</string>""")
            )
            .issues(UNTRANSLATED_RESOURCE)
            .run()
            .expectClean()
    }

    @Test
    fun `Fails on a string left as base English`() {
        configuredLint()
            .files(
                base("""<string name="fui_error_dialog_title">Authentication Error</string>"""),
                locale(
                    "zh-rTW",
                    """<string name="fui_error_dialog_title">Authentication Error</string>"""
                )
            )
            .issues(UNTRANSLATED_RESOURCE)
            .run()
            .expectErrorCount(1)
    }

    @Test
    fun `Passes on English regional folders`() {
        configuredLint()
            .files(
                base("""<string name="fui_sign_in">Sign in</string>"""),
                locale("en-rGB", """<string name="fui_sign_in">Sign in</string>""")
            )
            .issues(UNTRANSLATED_RESOURCE)
            .run()
            .expectClean()
    }

    @Test
    fun `Passes on allowlisted brand names`() {
        configuredLint()
            .files(
                base("""<string name="fui_idp_name_google">Google</string>"""),
                locale("fr", """<string name="fui_idp_name_google">Google</string>""")
            )
            .issues(UNTRANSLATED_RESOURCE)
            .run()
            .expectClean()
    }

    @Test
    fun `Passes on non-translatable strings`() {
        configuredLint()
            .files(
                base(
                    """<string name="fui_sign_in">Sign in</string>""",
                    """<string name="fui_internal" translatable="false">Internal</string>"""
                ),
                locale(
                    "fr",
                    """<string name="fui_sign_in">Se connecter</string>""",
                    """<string name="fui_internal" translatable="false">Internal</string>"""
                )
            )
            .issues(UNTRANSLATED_RESOURCE)
            .run()
            .expectClean()
    }

    @Test
    fun `Passes on a BCP47 locale folder with a real translation`() {
        configuredLint()
            .files(
                base("""<string name="fui_sign_in">Sign in</string>"""),
                locale("b+es+419", """<string name="fui_sign_in">Iniciar sesión</string>""")
            )
            .issues(UNTRANSLATED_RESOURCE)
            .run()
            .expectClean()
    }

    @Test
    fun `Fails on a BCP47 locale folder left as base English`() {
        configuredLint()
            .files(
                base("""<string name="fui_sign_in">Sign in</string>"""),
                locale("b+es+419", """<string name="fui_sign_in">Sign in</string>""")
            )
            .issues(UNTRANSLATED_RESOURCE)
            .run()
            .expectErrorCount(1)
    }

    @Test
    fun `Passes on a placeholder-only string`() {
        configuredLint()
            .files(
                base(
                    """<string name="fui_sign_in">Sign in</string>""",
                    """<string name="fui_tos_and_pp_footer">%1${'$'}s \u00A0 %2${'$'}s</string>"""
                ),
                locale(
                    "fr",
                    """<string name="fui_sign_in">Se connecter</string>""",
                    """<string name="fui_tos_and_pp_footer">%1${'$'}s \u00A0 %2${'$'}s</string>"""
                )
            )
            .issues(UNTRANSLATED_RESOURCE)
            .run()
            .expectClean()
    }

    @Test
    fun `Passes when the string carries a tools ignore`() {
        configuredLint()
            .files(
                base("""<string name="fui_password_hint">Password</string>"""),
                xml(
                    "res/values-it/strings.xml",
                    """
                    |<resources xmlns:tools="http://schemas.android.com/tools">
                    |    <string name="fui_password_hint" tools:ignore="UntranslatedResource">Password</string>
                    |</resources>""".trimMargin()
                )
            )
            .issues(UNTRANSLATED_RESOURCE)
            .run()
            .expectClean()
    }

    /**
     * Only `values/` seeds the comparison. A string defined solely in a configuration variant is
     * not a base string, so a locale copying it is not reported.
     *
     * The fixture deliberately keeps `fui_sign_in` out of `values/`: if the detector treated any
     * unqualified folder as the base, `values-v26` would seed it and `values-fr` would report.
     */
    @Test
    fun `Does not treat a non-locale configuration folder as the base`() {
        configuredLint()
            .files(
                base("""<string name="fui_unrelated">Unrelated</string>"""),
                xml(
                    "res/values-v26/strings.xml",
                    """
                    |<resources>
                    |    <string name="fui_sign_in">Sign in</string>
                    |</resources>""".trimMargin()
                ),
                locale("fr", """<string name="fui_sign_in">Sign in</string>""")
            )
            .issues(UNTRANSLATED_RESOURCE)
            .run()
            .expectClean()
    }
}
