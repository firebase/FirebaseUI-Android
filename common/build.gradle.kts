plugins {
  id("com.android.library")
  id("com.vanniktech.maven.publish")
}

android {
    compileSdk = Config.SdkVersions.compile
    namespace = "com.firebase.ui.common"

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
    api(libs.androidx.lifecycle.runtime)
    api(libs.androidx.lifecycle.viewmodel)
    implementation(libs.androidx.annotation)
    annotationProcessor(libs.androidx.lifecycle.compiler)
}
