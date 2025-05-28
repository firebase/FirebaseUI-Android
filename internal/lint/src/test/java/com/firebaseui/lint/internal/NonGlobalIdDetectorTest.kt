/*
 * Copyright 2025 Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
