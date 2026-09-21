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

    buildTypes {
        named("release").configure {
            isMinifyEnabled = false
        }
    }
}

dependencies {
    api(libs.glide)

    api(platform(libs.firebase.bom))
    api(libs.firebase.storage)
    // Override Play Services
    implementation(libs.androidx.legacy.support.v4)

    testImplementation(libs.junit)
    testImplementation(libs.mockito.core)
}