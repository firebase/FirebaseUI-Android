package com.firebaseui.lint.internal

import com.android.tools.lint.checks.infrastructure.TestLintTask
import java.io.File
import java.util.Properties

/**
 * Points [TestLintTask] at the local Android SDK.
 *
 * Lint's test harness refuses to run without one, and Gradle does not put `ANDROID_HOME` into
 * the test JVM's environment. The previous approach scanned `java.library.path` for a segment
 * containing "SDK", splitting on `;`, which only ever resolved on Windows: on macOS and Linux
 * it silently found nothing, the SDK went unconfigured, and every test in this module failed
 * with "This test requires an Android SDK".
 */
internal fun TestLintTask.withLocalSdk(): TestLintTask {
    val sdk = androidSdkHome() ?: error(
        "No Android SDK found. Set ANDROID_HOME, or add sdk.dir to local.properties at the " +
            "repository root."
    )
    return sdkHome(sdk)
}

private fun androidSdkHome(): File? {
    val fromEnv = sequenceOf("ANDROID_HOME", "ANDROID_SDK_ROOT")
        .mapNotNull { System.getenv(it) }
        .map(::File)
        .firstOrNull(File::isDirectory)
    if (fromEnv != null) return fromEnv

    // Tests run with the module directory as the working directory, so walk up to the root.
    var dir: File? = File(".").absoluteFile
    while (dir != null) {
        val properties = File(dir, "local.properties")
        if (properties.isFile) {
            val sdkDir = properties.inputStream().use { Properties().apply { load(it) } }
                .getProperty("sdk.dir")
                ?.let(::File)
            if (sdkDir?.isDirectory == true) return sdkDir
        }
        dir = dir.parentFile
    }
    return null
}
