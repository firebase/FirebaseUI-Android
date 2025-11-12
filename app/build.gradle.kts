plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.gms.google-services")
}

android {
    namespace = "com.firebase.uidemo"
    compileSdk = Config.SdkVersions.compile

    defaultConfig {
        applicationId = "com.firebase.uidemo"
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
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(project(":auth"))

    implementation(Config.Libs.Kotlin.jvm)
    implementation(Config.Libs.Androidx.lifecycleRuntime)
    implementation(Config.Libs.Androidx.Compose.activityCompose)
    implementation(platform(Config.Libs.Androidx.Compose.bom))
    implementation(Config.Libs.Androidx.Compose.ui)
    implementation(Config.Libs.Androidx.Compose.uiGraphics)
    implementation(Config.Libs.Androidx.Compose.toolingPreview)
    implementation(Config.Libs.Androidx.Compose.material3)


    // Facebook
    implementation(Config.Libs.Provider.facebook)

    testImplementation(Config.Libs.Test.junit)
    androidTestImplementation(Config.Libs.Test.junitExt)
    androidTestImplementation(platform(Config.Libs.Androidx.Compose.bom))
    androidTestImplementation(Config.Libs.Test.composeUiTestJunit4)

    debugImplementation(Config.Libs.Androidx.Compose.tooling)

    implementation(platform(Config.Libs.Firebase.bom))
}
