import com.android.build.gradle.internal.dsl.TestOptions

plugins {
    id("com.android.library")
    id("com.vanniktech.maven.publish")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose") version "2.0.0"
}

android {
    compileSdk = Config.SdkVersions.compile
    namespace = "com.firebase.ui.auth"

    defaultConfig {
        minSdk = Config.SdkVersions.min
        targetSdk =Config.SdkVersions.target

        buildConfigField("String", "VERSION_NAME", "\"${Config.version}\"")

        resourcePrefix("fui_")
        vectorDrawables.useSupportLibrary = true
    }

    buildTypes {
        named("release").configure {
            isMinifyEnabled = false
            consumerProguardFiles("auth-proguard.pro")
        }
    }
        
    compileOptions {    
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    lint {
        // Common lint options across all modules
        disable += mutableSetOf(
            "IconExpectedSize",
            "InvalidPackage", // Firestore uses GRPC which makes lint mad
            "NewerVersionAvailable", "GradleDependency", // For reproducible builds
            "SelectableText", "SyntheticAccessor" // We almost never care about this
        )

        // Module specific
        disable += mutableSetOf(
            "UnusedQuantity",
            "UnknownNullness",  // TODO fix in future PR
            "TypographyQuotes", // Straight versus directional quotes
            "DuplicateStrings",
            "LocaleFolder",
            "IconLocation",
            "VectorPath"
        )

        checkAllWarnings = true
        warningsAsErrors = true
        abortOnError = true

        baseline = file("$rootDir/library/quality/lint-baseline.xml")
    }

    testOptions {
        unitTests {
            isIncludeAndroidResources = true
        }
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(platform(Config.Libs.Androidx.Compose.bom))
    implementation(Config.Libs.Androidx.Compose.ui)
    implementation(Config.Libs.Androidx.Compose.uiGraphics)
    implementation(Config.Libs.Androidx.Compose.material3)
    implementation(Config.Libs.Androidx.Compose.foundation)
    implementation(Config.Libs.Androidx.Compose.tooling)
    implementation(Config.Libs.Androidx.Compose.toolingPreview)
    implementation(Config.Libs.Androidx.Compose.activityCompose)
    implementation(Config.Libs.Androidx.materialDesign)
    implementation(Config.Libs.Androidx.activity)
    // The new activity result APIs force us to include Fragment 1.3.0
    // See https://issuetracker.google.com/issues/152554847
    implementation(Config.Libs.Androidx.fragment)
    implementation(Config.Libs.Androidx.customTabs)
    implementation(Config.Libs.Androidx.constraint)
    implementation("androidx.credentials:credentials:1.3.0")
    implementation("androidx.credentials:credentials-play-services-auth:1.3.0")

    implementation(Config.Libs.Androidx.lifecycleExtensions)
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.2.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
    implementation("com.google.android.libraries.identity.googleid:googleid:1.1.1")
    annotationProcessor(Config.Libs.Androidx.lifecycleCompiler)

    implementation(platform(Config.Libs.Firebase.bom))
    api(Config.Libs.Firebase.auth)
    api(Config.Libs.PlayServices.auth)

    compileOnly(Config.Libs.Provider.facebook)
    implementation(Config.Libs.Androidx.legacySupportv4) // Needed to override deps
    implementation(Config.Libs.Androidx.cardView) // Needed to override Facebook

    testImplementation(Config.Libs.Test.junit)
    testImplementation(Config.Libs.Test.truth)
    testImplementation(Config.Libs.Test.mockito)
    testImplementation(Config.Libs.Test.core)
    testImplementation(Config.Libs.Test.robolectric)
    testImplementation(Config.Libs.Test.kotlinReflect)
    testImplementation(Config.Libs.Provider.facebook)

    debugImplementation(project(":internal:lintchecks"))
}
