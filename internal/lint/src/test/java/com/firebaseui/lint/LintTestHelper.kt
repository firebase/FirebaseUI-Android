package com.firebaseui.lint

import com.android.tools.lint.checks.infrastructure.TestLintTask
import java.io.File

object LintTestHelper {
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
}
