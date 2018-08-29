package com.firebaseui.lint.internal

import com.android.tools.lint.checks.infrastructure.TestFiles.xml
import com.android.tools.lint.checks.infrastructure.TestLintTask
import com.firebaseui.lint.internal.NonGlobalIdDetector.Companion.NON_GLOBAL_ID
import org.junit.Test
import java.io.File

class NonGlobalIdDetectorTest {

    // Nasty hack to make lint tests pass on Windows. For some reason, lint doesn't
    // automatically find the Android SDK in its standard path on Windows. This hack looks
    // through the system properties to find the path defined in `local.properties` and then
    // sets lint's SDK home to that path if it's found.
    private val sdkPath = System.getProperty("java.library.path").split(';').find {
        it.contains("SDK", true)
    }

    fun configuredLint(): TestLintTask = TestLintTask.lint().apply {
        sdkHome(File(sdkPath ?: return@apply))
    }

    @Test
    fun `Passes on valid view id`() {
        configuredLint()
                .files(xml("res/layout/layout.xml", """
                        |<RelativeLayout
                        |    xmlns:android="http://schemas.android.com/apk/res/android"
                        |    android:id="@+id/valid"/>""".trimMargin()))
                .issues(NON_GLOBAL_ID)
                .run()
                .expectClean()
    }

    @Test
    fun `Fails on invalid view id`() {
        configuredLint()
                .files(xml("res/layout/layout.xml", """
                        |<ScrollView
                        |    xmlns:android="http://schemas.android.com/apk/res/android"
                        |    android:id="@id/invalid"/>""".trimMargin()))
                .issues(NON_GLOBAL_ID)
                .run()
                .expect("""
                        |res/layout/layout.xml:3: Error: Use of non-global @id in layout file, consider using @+id instead for compatibility with aapt1. [NonGlobalIdInLayout]
                        |    android:id="@id/invalid"/>
                        |    ~~~~~~~~~~~~~~~~~~~~~~~~
                        |1 errors, 0 warnings""".trimMargin())
                .expectFixDiffs("""
                        |Fix for res/layout/layout.xml line 2: Fix id:
                        |@@ -3 +3
                        |-     android:id="@id/invalid"/>
                        |@@ -4 +3
                        |+     android:id="@+id/invalid"/>
                        |""".trimMargin())
    }
}
