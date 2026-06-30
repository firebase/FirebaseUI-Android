@file:Suppress("UnstableApiUsage")

plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.google.services) apply false
    alias(libs.plugins.maven.publish) apply false
    alias(libs.plugins.versions)
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
