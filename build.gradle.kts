@file:Suppress("UnstableApiUsage")

buildscript {
    repositories {
        google()
        mavenCentral()
        mavenLocal()
        maven("https://oss.jfrog.org/artifactory/oss-release-local")
    }

    dependencies {
        classpath(Config.Plugins.android)
        classpath(Config.Plugins.kotlin)
        classpath(Config.Plugins.google)
        classpath(Config.Plugins.mavenPublish)
        classpath(Config.Plugins.buildInfo)
    }
}

plugins {
    id("com.github.ben-manes.versions") version "0.20.0"
    id("org.jetbrains.kotlin.plugin.compose") version Config.kotlinVersion apply false
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
