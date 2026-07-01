import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("com.android.library")
    id("com.vanniktech.maven.publish")
    alias(libs.plugins.kotlin.compose)
}

android {
    compileSdk = Config.SdkVersions.compile
    namespace = "com.firebase.ui.auth"

    defaultConfig {
        minSdk = Config.SdkVersions.min

        buildConfigField("String", "LIBRARY_NAME", "\"firebase-ui-android\"")
        buildConfigField("String", "VERSION_NAME", "\"${Config.version}\"")

        resourcePrefix = "fui_"
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
        targetSdk = Config.SdkVersions.target
        unitTests {
            isIncludeAndroidResources = true
        }
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.material3)
    implementation(libs.compose.foundation)
    implementation(libs.compose.ui.tooling)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.activity)
    implementation(libs.androidx.activity)
    implementation(libs.material)
    implementation(libs.compose.material.icons.extended)
    implementation(libs.androidx.datastore.preferences)
    // The new activity result APIs force us to include Fragment 1.3.0
    // See https://issuetracker.google.com/issues/152554847
    implementation(libs.androidx.fragment)
    implementation(libs.androidx.browser)
    implementation(libs.androidx.constraintlayout)

    // Google Authentication
    implementation(libs.androidx.credentials)
    implementation(libs.androidx.credentials.play.services)
    implementation(libs.googleid)
    implementation(libs.play.services.auth)

    implementation(libs.androidx.lifecycle.extensions)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.android)
    api(libs.compose.navigation)
    implementation(libs.zxing.core)
    annotationProcessor(libs.androidx.lifecycle.compiler)

    api(platform(libs.firebase.bom))
    api(libs.firebase.auth)

    // Phone number validation
    implementation(libs.libphonenumber)

    compileOnly(libs.facebook.login)
    implementation(libs.androidx.legacy.support.v4) // Needed to override deps
    implementation(libs.androidx.cardview) // Needed to override Facebook

    testImplementation(libs.junit)
    testImplementation(libs.truth)
    testImplementation(libs.test.core)
    testImplementation(libs.robolectric)
    testImplementation(libs.kotlin.reflect)
    testImplementation(libs.facebook.login)
    testImplementation(libs.mockito.core)
    testImplementation(libs.mockito.kotlin)
    testImplementation(libs.androidx.credentials)
    testImplementation(libs.compose.ui.test.junit4)

    debugImplementation(project(":internal:lintchecks"))
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_17
    }
}

val mockitoAgent by configurations.creating

dependencies {
    mockitoAgent(libs.mockito.core) {
        isTransitive = false
    }
}

tasks.withType<Test>().configureEach {
    jvmArgs("-javaagent:${mockitoAgent.asPath}")
}
