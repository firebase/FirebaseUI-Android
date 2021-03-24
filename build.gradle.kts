@file:Suppress("UnstableApiUsage")

import com.android.build.gradle.BaseExtension
import com.android.build.gradle.LibraryExtension

buildscript {
    repositories {
        google()
        mavenCentral()
        jcenter()
        mavenLocal()
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
}

// See https://github.com/gradle/kotlin-dsl/issues/607#issuecomment-375687119
subprojects { parent!!.path.takeIf { it != rootProject.path }?.let { evaluationDependsOn(it) } }

allprojects {
    repositories {
        google()
        jcenter()
        mavenLocal()
    }

    if ((group as String).isNotEmpty() && name != "lint" && name != "internal") {
        configureAndroid()

        if (name != "lintchecks") {
            configureQuality()
        }

        if (Config.submodules.contains(name) || isLibrary) {
            setupPublishing()
        }
    }
}

tasks.withType<Wrapper>().configureEach {
    distributionType = Wrapper.DistributionType.ALL
}

val Project.configDir get() = "$rootDir/library/quality"
val Project.reportsDir get() = "$buildDir/reports"

/**
 * Determines if a Project is the 'library' module
 */
val Project.isLibrary get() = name == "library"

fun Project.configureAndroid() {
    if (name == "app" || name == "proguard-tests") {
        apply(plugin = "com.android.application")
    } else {
        apply(plugin = "com.android.library")
        apply(plugin = "com.vanniktech.maven.publish")
    }

    configure<BaseExtension> {
        compileSdkVersion(Config.SdkVersions.compile)

        defaultConfig {
            minSdkVersion(Config.SdkVersions.min)
            targetSdkVersion(Config.SdkVersions.target)

            versionName = Config.version
            versionCode = 1

            resourcePrefix("fui_")
            vectorDrawables.useSupportLibrary = true
        }

        lintOptions {
            disable(
                    "ObsoleteLintCustomCheck", // ButterKnife will fix this in v9.0
                    "IconExpectedSize",
                    "InvalidPackage", // Firestore uses GRPC which makes lint mad
                    "NewerVersionAvailable", "GradleDependency", // For reproducible builds
                    "SelectableText", "SyntheticAccessor" // We almost never care about this
            )

            isCheckAllWarnings = true
            isWarningsAsErrors = true
            isAbortOnError = true

            baselineFile = file("$configDir/lint-baseline.xml")
            // htmlOutput = file("$reportsDir/lint-results.html")
            // xmlOutput = file("$reportsDir/lint-results.xml")
        }
    }
}

fun Project.configureQuality() {
    apply(plugin = "checkstyle")

    configure<CheckstyleExtension> { toolVersion = "8.10.1" }
    tasks.named("check").configure { dependsOn("checkstyle") }

    tasks.register<Checkstyle>("checkstyle") {
        configFile = file("${project.configDir}/checkstyle.xml")
        source("src")
        include("**/*.java")
        exclude("**/gen/**")
        classpath = files()
    }
}

fun Project.setupPublishing() {
    val sourcesJar = tasks.register<Jar>("sourcesJar") {
        classifier = "sources"
        from(project.the<BaseExtension>().sourceSets["main"].java.srcDirs)
    }

    val javadoc = tasks.register<Javadoc>("javadoc") {
        setSource(project.the<BaseExtension>().sourceSets["main"].java.srcDirs)
        classpath += files(project.the<BaseExtension>().bootClasspath)

        project.the<LibraryExtension>().libraryVariants.configureEach {
            dependsOn(assemble)
            classpath += files((javaCompiler as AbstractCompile).classpath)
        }

        // Ignore warnings about incomplete documentation
        (options as StandardJavadocDocletOptions).addStringOption("Xdoclint:none", "-quiet")
    }

    val javadocJar = tasks.register<Jar>("javadocJar") {
        dependsOn(javadoc)
        classifier = "javadoc"
        from(javadoc.get().destinationDir)
    }

    artifacts.add("archives", javadocJar)
    artifacts.add("archives", sourcesJar)

    afterEvaluate {
        if (isLibrary) {
            tasks.register("testAll") {
                dependsOn(*Config.submodules.map {
                    ":$it:testDebugUnitTest"
                }.toTypedArray())
            }

            tasks.register("prepareArtifacts") {
                dependsOn(javadocJar, sourcesJar, "assembleRelease")
                dependsOn(*Config.submodules.map {
                    ":$it:prepareArtifacts"
                }.toTypedArray())
            }
        } else {
            tasks.register("prepareArtifacts") {
                dependsOn("assembleRelease")
            }
        }
    }
}
