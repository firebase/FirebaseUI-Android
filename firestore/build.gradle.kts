plugins {
  id("com.android.library")
  id("com.vanniktech.maven.publish")
}

android {
    compileSdk = Config.SdkVersions.compile
    namespace = "com.firebase.ui.firestore"

    defaultConfig {
        minSdk = Config.SdkVersions.min

        resourcePrefix("fui_")
        vectorDrawables.useSupportLibrary = true

        multiDexEnabled = true
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    testOptions {
        targetSdk = Config.SdkVersions.target
    }

    lint {
        // Common lint options across all modules
        disable += mutableSetOf(
            "IconExpectedSize",
            "InvalidPackage", // Firestore uses GRPC which makes lint mad
            "NewerVersionAvailable", "GradleDependency", // For reproducible builds
            "SelectableText", "SyntheticAccessor" // We almost never care about this
        )

        checkAllWarnings = true
        warningsAsErrors = true
        abortOnError = true

        baseline = file("$rootDir/library/quality/lint-baseline.xml")
    }

    buildTypes {
        named("release").configure {
            isMinifyEnabled = false
            consumerProguardFiles("proguard-rules.pro")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
}

dependencies {
    implementation(platform(libs.firebase.bom))
    api(project(":common"))
    api(libs.firebase.firestore)

    api(libs.androidx.legacy.support.v4)
    api(libs.androidx.recyclerview)

    compileOnly(libs.androidx.paging)
    api(libs.androidx.paging.rxjava3)
    annotationProcessor(libs.androidx.lifecycle.compiler)

    lintChecks(project(":lint"))

    androidTestImplementation(libs.arch.core.testing)
    androidTestImplementation(libs.test.core)
    androidTestImplementation(libs.junit)
    androidTestImplementation(libs.junit.ext)
    androidTestImplementation(libs.test.runner)
    androidTestImplementation(libs.test.rules)
    androidTestImplementation(libs.mockito.android)
    androidTestImplementation(libs.androidx.paging)
}