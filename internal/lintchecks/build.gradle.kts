plugins {
  id("com.android.library")
}

android {
    compileSdk = Config.SdkVersions.compile
    namespace = "com.firebaseui.lint"

    defaultConfig {
        minSdk = Config.SdkVersions.min

        resourcePrefix("fui_")
        vectorDrawables.useSupportLibrary = true

        multiDexEnabled = true
    }

    testOptions {
        targetSdk = Config.SdkVersions.target
    }

    compileOptions {    
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
}

dependencies {
    lintChecks(project(":internal:lint"))
}
