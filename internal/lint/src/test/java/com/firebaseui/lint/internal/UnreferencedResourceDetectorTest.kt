package com.firebaseui.lint.internal

import com.android.tools.lint.checks.infrastructure.TestFiles.java
import com.android.tools.lint.checks.infrastructure.TestFiles.kotlin
import com.android.tools.lint.checks.infrastructure.TestFiles.manifest
import com.android.tools.lint.checks.infrastructure.TestFiles.xml
import com.android.tools.lint.checks.infrastructure.TestLintTask
import com.firebaseui.lint.internal.UnreferencedResourceDetector.Companion.UNREFERENCED_RESOURCE
import org.junit.Test

class UnreferencedResourceDetectorTest {

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
    fun `Passes on a string referenced from Kotlin`() {
        configuredLint()
            .files(
                base("""<string name="fui_sign_in">Sign in</string>"""),
                kotlin(
                    """
                    |package com.firebase.ui.auth
                    |
                    |fun label(): Int = R.string.fui_sign_in
                    """.trimMargin()
                )
            )
            .issues(UNREFERENCED_RESOURCE)
            .run()
            .expectClean()
    }

    @Test
    fun `Passes on a string referenced from Java`() {
        configuredLint()
            .files(
                base("""<string name="fui_sign_in">Sign in</string>"""),
                java(
                    """
                    |package com.firebase.ui.auth;
                    |
                    |class Labels {
                    |    int label() { return R.string.fui_sign_in; }
                    |}
                    """.trimMargin()
                )
            )
            .issues(UNREFERENCED_RESOURCE)
            .run()
            .expectClean()
    }

    @Test
    fun `Fails on a string nothing references`() {
        configuredLint()
            .files(
                base("""<string name="fui_abandoned">Abandoned</string>"""),
                kotlin(
                    """
                    |package com.firebase.ui.auth
                    |
                    |fun nothing() = Unit
                    """.trimMargin()
                )
            )
            .issues(UNREFERENCED_RESOURCE)
            .run()
            .expectErrorCount(1)
    }

    @Test
    fun `Counts R plurals as a reference`() {
        configuredLint()
            .files(
                base(
                    """<plurals name="fui_error_weak_password">""" +
                        """<item quantity="other">Too weak</item></plurals>"""
                ),
                kotlin(
                    """
                    |package com.firebase.ui.auth
                    |
                    |fun message(): Int = R.plurals.fui_error_weak_password
                    """.trimMargin()
                )
            )
            .issues(UNREFERENCED_RESOURCE)
            .run()
            .expectClean()
    }

    @Test
    fun `Reports a plurals that nothing references`() {
        configuredLint()
            .files(
                base(
                    """<plurals name="fui_dead_plurals">""" +
                        """<item quantity="other">Nothing reads this</item></plurals>"""
                ),
                kotlin("package com.firebase.ui.auth\n\nfun nothing() = Unit")
            )
            .issues(UNREFERENCED_RESOURCE)
            .run()
            .expectErrorCount(1)
    }

    @Test
    fun `Counts an XML string reference`() {
        configuredLint()
            .files(
                base("""<string name="fui_sign_in">Sign in</string>"""),
                xml(
                    "res/layout/activity.xml",
                    """
                    |<TextView xmlns:android="http://schemas.android.com/apk/res/android"
                    |    android:text="@string/fui_sign_in" />""".trimMargin()
                )
            )
            .issues(UNREFERENCED_RESOURCE)
            .run()
            .expectClean()
    }

    @Test
    fun `Counts an alias in the values folder as a reference`() {
        configuredLint()
            .files(
                base(
                    """<string name="fui_sign_in">Sign in</string>""",
                    """<string name="fui_alias">@string/fui_sign_in</string>"""
                ),
                kotlin(
                    """
                    |package com.firebase.ui.auth
                    |
                    |fun label(): Int = R.string.fui_alias
                    """.trimMargin()
                )
            )
            .issues(UNREFERENCED_RESOURCE)
            .run()
            .expectClean()
    }

    @Test
    fun `Ignores resources outside the fui prefix`() {
        configuredLint()
            .files(
                base("""<string name="app_name">ui_flow</string>"""),
                kotlin("package com.firebase.ui.auth\n\nfun nothing() = Unit")
            )
            .issues(UNREFERENCED_RESOURCE)
            .run()
            .expectClean()
    }

    @Test
    fun `Does not treat a locale declaration as the declaration site`() {
        // The base folder is what declares a resource; a locale file only translates it. If
        // locale files seeded the map, a string deleted from values/ but left behind in one
        // translation would be reported against the translation rather than the base.
        configuredLint()
            .files(
                base("""<string name="fui_sign_in">Sign in</string>"""),
                locale("fr", """<string name="fui_sign_in">Se connecter</string>"""),
                locale("de", """<string name="fui_orphan">Verwaist</string>"""),
                kotlin(
                    """
                    |package com.firebase.ui.auth
                    |
                    |fun label(): Int = R.string.fui_sign_in
                    """.trimMargin()
                )
            )
            .issues(UNREFERENCED_RESOURCE)
            .run()
            .expectClean()
    }

    @Test
    fun `Reports a dead string that is fully translated`() {
        // The real-world shape: every one of the 34 resources CPRN-445 deleted was present in
        // all 84 locale folders. A translation is not a reference, so the presence of one must
        // not keep the base declaration alive.
        configuredLint()
            .files(
                base("""<string name="fui_dead">Dead</string>"""),
                locale("fr", """<string name="fui_dead">Mort</string>"""),
                locale("de", """<string name="fui_dead">Tot</string>"""),
                kotlin("package com.firebase.ui.auth\n\nfun nothing() = Unit")
            )
            .issues(UNREFERENCED_RESOURCE)
            .run()
            .expectErrorCount(1)
    }

    @Test
    fun `Reports every unreferenced resource, not just the first`() {
        configuredLint()
            .files(
                base(
                    """<string name="fui_dead_one">One</string>""",
                    """<string name="fui_dead_two">Two</string>""",
                    """<string name="fui_live">Live</string>"""
                ),
                kotlin(
                    """
                    |package com.firebase.ui.auth
                    |
                    |fun label(): Int = R.string.fui_live
                    """.trimMargin()
                )
            )
            .issues(UNREFERENCED_RESOURCE)
            .run()
            // Named rather than counted: a detector that reported fui_live and one dead string
            // would also produce two errors.
            .expectContains("\"fui_dead_one\" is not referenced")
            .expectContains("\"fui_dead_two\" is not referenced")
            .expectErrorCount(2)
    }

    @Test
    fun `Honours tools ignore at the declaration`() {
        configuredLint()
            .files(
                xml(
                    "res/values/strings.xml",
                    """
                    |<resources xmlns:tools="http://schemas.android.com/tools">
                    |    <string name="fui_reflective" tools:ignore="UnreferencedResource">Hi</string>
                    |</resources>""".trimMargin()
                ),
                kotlin("package com.firebase.ui.auth\n\nfun nothing() = Unit")
            )
            .issues(UNREFERENCED_RESOURCE)
            .run()
            .expectClean()
    }

    @Test
    fun `Does not match a different resource type with the same name`() {
        // @color/fui_x must not keep <string name="fui_x"> alive.
        configuredLint()
            .files(
                base("""<string name="fui_brand">Brand</string>"""),
                xml(
                    "res/layout/activity.xml",
                    """
                    |<TextView xmlns:android="http://schemas.android.com/apk/res/android"
                    |    android:textColor="@color/fui_brand" />""".trimMargin()
                )
            )
            .issues(UNREFERENCED_RESOURCE)
            .run()
            .expectContains("\"fui_brand\" is not referenced")
            .expectErrorCount(1)
    }

    @Test
    fun `Counts a manifest reference`() {
        // The manifest is not under res/, so it is only seen because the implementation declares
        // Scope.MANIFEST and beforeCheckFile reads it. Without that, android:label="@string/x"
        // fails the build telling you to delete a string that is in use.
        configuredLint()
            .files(
                base("""<string name="fui_app_label">Sign in</string>"""),
                manifest(
                    """
                    |<manifest xmlns:android="http://schemas.android.com/apk/res/android"
                    |    package="com.firebase.ui.auth">
                    |    <application android:label="@string/fui_app_label" />
                    |</manifest>""".trimMargin()
                )
            )
            .issues(UNREFERENCED_RESOURCE)
            .run()
            .expectClean()
    }

    @Test
    fun `Checks a resource declared only in a configuration-qualified folder`() {
        // values-v26 and values-sw360dp already exist in :auth. They are configuration variants,
        // not translations, so a resource declared only there can still be dead.
        configuredLint()
            .files(
                base("""<string name="fui_live">Live</string>"""),
                locale("v26", """<string name="fui_v26_only">Only here</string>"""),
                kotlin(
                    """
                    |package com.firebase.ui.auth
                    |
                    |fun label(): Int = R.string.fui_live
                    """.trimMargin()
                )
            )
            .issues(UNREFERENCED_RESOURCE)
            .run()
            .expectContains("\"fui_v26_only\" is not referenced")
            .expectErrorCount(1)
    }

    @Test
    fun `Does not count a design-time tools attribute as a reference`() {
        configuredLint()
            .files(
                base("""<string name="fui_preview_only">Preview</string>"""),
                xml(
                    "res/layout/activity.xml",
                    """
                    |<TextView xmlns:tools="http://schemas.android.com/tools"
                    |    tools:text="@string/fui_preview_only" />""".trimMargin()
                )
            )
            .issues(UNREFERENCED_RESOURCE)
            .run()
            .expectContains("\"fui_preview_only\" is not referenced")
            .expectErrorCount(1)
    }

    @Test
    fun `Counts a name mentioned only in a comment, an admitted limit`() {
        // Pinned rather than merely documented: this is the price of matching source textually,
        // and it errs towards not reporting, which is the safe direction for a build gate.
        configuredLint()
            .files(
                base("""<string name="fui_commented_out">Gone</string>"""),
                kotlin(
                    """
                    |package com.firebase.ui.auth
                    |
                    |// was R.string.fui_commented_out before the rewrite
                    |fun nothing() = Unit
                    """.trimMargin()
                )
            )
            .issues(UNREFERENCED_RESOURCE)
            .run()
            .expectClean()
    }
}
