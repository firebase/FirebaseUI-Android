@file:Suppress("UnstableApiUsage")

import com.android.build.api.dsl.ApplicationExtension
import com.android.build.api.dsl.LibraryExtension
import com.android.build.api.dsl.Lint

plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.google.services) apply false
    alias(libs.plugins.maven.publish) apply false
}

allprojects {
    repositories {
        google()
        mavenCentral()
        mavenLocal()
    }

    if (name != "lint" && name != "internal" && name != "lintchecks") {
        apply(plugin = "checkstyle")

        configure<CheckstyleExtension> { toolVersion = "8.10.1" }
        tasks.register<Checkstyle>("checkstyle") {
            configFile = file("$rootDir/library/quality/checkstyle.xml")
            source("src")
            include("**/*.java")
            exclude("**/gen/**")
            classpath = files()
        }
    }
}

// The shared Android Lint policy, alongside the checkstyle one above. Modules add only their
// own disables; the strictness flags live here so a module cannot quietly opt out of the gate
// the way :library did with abortOnError = false.
fun Lint.applyCommonPolicy() {
    disable += setOf(
        "IconExpectedSize",
        "InvalidPackage", // Firestore uses GRPC which makes lint mad
        "NewerVersionAvailable", "GradleDependency", // For reproducible builds
        "SelectableText", "SyntheticAccessor" // We almost never care about this
    )

    checkAllWarnings = true
    warningsAsErrors = true
    abortOnError = true
}

subprojects {
    plugins.withId("com.android.application") {
        extensions.configure<ApplicationExtension> { lint { applyCommonPolicy() } }
    }
    plugins.withId("com.android.library") {
        extensions.configure<LibraryExtension> { lint { applyCommonPolicy() } }
    }
}

// Android Lint has no repo-wide entry point by default. This task is that entry point, and
// the module list is the gate's definition:
//   - :proguard-tests disables its debug variant on CI, so it is gated on release instead.
tasks.register("lintAll") {
    group = "verification"
    description = "Runs Android Lint for every module gated on it."

    dependsOn(
        ":app:lintDebug",
        ":auth:lintDebug",
        ":common:lintDebug",
        ":database:lintDebug",
        ":e2eTest:lintDebug",
        ":firestore:lintDebug",
        ":library:lintDebug",
        ":storage:lintDebug",
        ":internal:lintchecks:lintDebug",
        ":proguard-tests:lintRelease"
    )
}
