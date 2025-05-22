// NOTE: this project uses Gradle Kotlin DSL. More common build.gradle instructions can be found in
// the main README.
plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.compose")
    id("com.google.gms.google-services")
    id("kotlin-kapt")
}

android {
    namespace = "com.firebase.uidemo"
    compileSdk = Config.SdkVersions.compile

    defaultConfig {
        minSdk = Config.SdkVersions.min
        targetSdk = Config.SdkVersions.target
        versionName = Config.version
        versionCode = 1
        multiDexEnabled = true
        resourcePrefix("fui_")
        vectorDrawables.useSupportLibrary = true
    }

    buildTypes {
        release {
            // For the purposes of the sample, allow testing of a proguarded release build
            // using the debug key
            signingConfig = signingConfigs["debug"]
            postprocessing {
                isRemoveUnusedCode = true
                isRemoveUnusedResources = true
                isObfuscate = true
                isOptimizeCode = true
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        viewBinding = true
        compose = true
    }

    lint {
        // Common lint options across all modules

        disable += mutableSetOf(
            "IconExpectedSize",
            "InvalidPackage", // Firestore uses GRPC which makes lint mad
            "NewerVersionAvailable", "GradleDependency", // For reproducible builds
            "SelectableText", "SyntheticAccessor", // We almost never care about this
            "UnusedIds", "MediaCapabilities" // TODO(rosariopfernandes): remove this once we confirm
            // it builds successfully
        )

        // Module-specific
        disable += mutableSetOf("ResourceName", "MissingTranslation", "DuplicateStrings")

        checkAllWarnings = true
        warningsAsErrors = true
        abortOnError = true

        baseline = file("$rootDir/library/quality/lint-baseline.xml")
    }
}

dependencies {
    implementation(Config.Libs.Androidx.materialDesign)
    implementation(Config.Libs.Androidx.multidex)

    implementation(project(":auth"))
    implementation(project(":firestore"))
    implementation(project(":database"))
    implementation(project(":storage"))

    implementation(Config.Libs.Misc.glide)
    kapt(Config.Libs.Misc.glideCompiler)

    implementation(Config.Libs.Provider.facebook)
    // Needed to override Facebook
    implementation(Config.Libs.Androidx.cardView)
    implementation(Config.Libs.Androidx.customTabs)
    // Used for FirestorePagingActivity
    implementation(Config.Libs.Androidx.paging)

    // The following dependencies are not required to use the Firebase UI library.
    // They are used to make some aspects of the demo app implementation simpler for
    // demonstrative purposes, and you may find them useful in your own apps; YMMV.

    implementation(Config.Libs.Misc.permissions)
    implementation(Config.Libs.Androidx.constraint)
    debugImplementation(Config.Libs.Misc.leakCanary)

    val composeBom = platform("androidx.compose:compose-bom:2025.02.00")
    implementation(composeBom)
    androidTestImplementation(composeBom)

    implementation("androidx.compose.material3:material3")
    implementation("androidx.activity:activity-compose:1.10.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.5")
    implementation("com.github.bumptech.glide:compose:1.0.0-beta01")
    debugImplementation("androidx.compose.ui:ui-tooling")
    releaseImplementation("androidx.compose.ui:ui-tooling-preview")
}

kapt { correctErrorTypes = true } // optional but avoids some kapt warnings