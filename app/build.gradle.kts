import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.gms.google-services")
    id("kotlin-kapt")
}

android {
    namespace = "com.firebaseui.android.demo"
    compileSdk = Config.SdkVersions.compile

    defaultConfig {
        applicationId = "com.firebaseui.android.demo"
        minSdk = Config.SdkVersions.min
        targetSdk = Config.SdkVersions.target
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            // Only sign with debug keystore if it exists (for local testing)
            val debugKeystoreFile = file("${System.getProperty("user.home")}/.android/debug.keystore")
            if (debugKeystoreFile.exists()) {
                signingConfig = signingConfigs.getByName("debug")
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_17
    }
}

dependencies {
    implementation(project(":auth"))
    implementation(project(":database"))
    implementation(project(":firestore"))
    implementation(project(":storage"))
    implementation(libs.androidx.paging)
    kapt(libs.glide.compiler)

    implementation(libs.kotlin.stdlib)
    implementation(libs.androidx.lifecycle.runtime)
    implementation(libs.compose.activity)
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)

    // Facebook
    implementation(libs.facebook.login)

    testImplementation(libs.junit)
    androidTestImplementation(libs.junit.ext)
    androidTestImplementation(platform(libs.compose.bom))
    androidTestImplementation(libs.compose.ui.test.junit4)

    debugImplementation(libs.compose.ui.tooling)

    implementation(platform(libs.firebase.bom))
}
