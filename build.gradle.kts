@file:Suppress("UnstableApiUsage")

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

// Android Lint is configured per module, so there is no repo-wide entry point by default.
// This task is that entry point, and the module list is the gate's definition:
//   - :app and :e2eTest declare no lint { } block yet, so they are deliberately absent (CPRN-433).
//   - :proguard-tests disables its debug variant on CI, so it is gated on release instead.
tasks.register("lintAll") {
    group = "verification"
    description = "Runs Android Lint for every module that configures a lint { } block."

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
