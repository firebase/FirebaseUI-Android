plugins {
  id("com.android.library")
  id("com.vanniktech.maven.publish")
}

android {
    compileSdk = Config.SdkVersions.compile
    namespace = "com.firebase.ui.storage.images"

    defaultConfig {
        minSdk = Config.SdkVersions.min

        resourcePrefix("fui_")
        vectorDrawables.useSupportLibrary = true
    }

    testOptions {
        targetSdk = Config.SdkVersions.target
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
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
}

dependencies {
    api(libs.glide)

    implementation(platform(libs.firebase.bom))
    api(libs.firebase.storage)
    // Override Play Services
    implementation(libs.androidx.legacy.support.v4)

    testImplementation(libs.junit)
    testImplementation(libs.mockito.core)
}
