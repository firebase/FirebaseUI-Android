package com.firebaseui.lint

import com.android.tools.lint.checks.infrastructure.TestFiles.xml
import com.android.tools.lint.checks.infrastructure.TestLintTask
import com.android.tools.lint.checks.infrastructure.TestLintTask.lint
import com.firebaseui.lint.NonGlobalIdDetector.Companion.NON_GLOBAL_ID
import org.junit.Test
import java.io.File

class NonGlobalIdDetectorTest {
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

    companion object {
        // Nasty hack to make lint tests pass on Windows
        private val sdkPath = System.getProperty("java.library.path").split(';').find {
            it.contains("SDK", true)
        }

        fun configuredLint(): TestLintTask = lint().apply {
            sdkHome(File(sdkPath ?: return@apply))
        }
    }
}
