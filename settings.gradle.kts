pluginManagement {
    repositories {
        gradlePluginPortal()
        google()
        mavenCentral()
    }

    val kotlinVersion = "2.0.0-RC2"
    val composeVersion = "1.8.0-beta02"
    val agpVersion = "8.5.0-beta01"
    val dokkaVersion = "2.0.0"
    val develocityVersion = "4.0"

    plugins {
        id("com.android.application") version agpVersion
        id("org.jetbrains.kotlin.android") version kotlinVersion
        id("org.jetbrains.kotlin.plugin.compose") version kotlinVersion
        id("org.jetbrains.compose") version composeVersion
        id("org.jetbrains.dokka") version dokkaVersion
        id("com.gradle.develocity") version develocityVersion
    }
}

plugins {
    id("com.gradle.develocity") version "4.0"
    id("org.jetbrains.dokka") version "2.0.0" apply false
}

develocity {
    buildScan {
        termsOfUseUrl.set("https://gradle.com/help/legal-terms-of-use")
        termsOfUseAgree.set("yes")
    }
}

rootProject.buildFileName = "build.gradle.kts"

include(
    ":app",
    ":library",
    ":auth",
    ":common",
    ":database",
    ":firestore",
    ":storage",
    ":lint",
    ":proguard-tests",
    ":internal:lint",
    ":internal:lintchecks"
)