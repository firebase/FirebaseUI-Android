android {
    defaultConfig {
        multiDexEnabled = true
    }

    buildTypes {
        getByName("release") {
            // For the purposes of the sample, allow testing of a proguarded release build
            // using the debug key
            signingConfig = signingConfigs["debug"]

            postprocessing {
                isRemoveUnusedCode = true
                isRemoveUnusedResources = true
                isObfuscate = true
            }
        }
    }

    variantFilter {
        if (name == "debug") setIgnore(true)
    }
}

dependencies {
    implementation(project(":auth"))
    implementation(project(":firestore"))
    implementation(project(":database"))
    implementation(project(":storage"))

    implementation(Config.Libs.Firebase.core)
    implementation(Config.Libs.Arch.extensions)
}

apply(plugin = "com.google.gms.google-services")
