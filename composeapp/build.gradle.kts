plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.gms.google-services") apply false
}

android {
    namespace = "com.firebase.composeapp"
    compileSdk = Config.SdkVersions.compile

    defaultConfig {
        applicationId = "com.firebase.composeapp"
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
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
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

// Only apply google-services plugin if the google-services.json file exists
if (rootProject.file("composeapp/google-services.json").exists()) {
    apply(plugin = "com.google.gms.google-services")
}
