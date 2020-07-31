// This is always set to 'true' on Travis CI
val inCiBuild = System.getenv("CI") == "true"

android {
    defaultConfig {
        multiDexEnabled = true
    }

    buildTypes {
        named("debug").configure {
            // This empty config is only here to make Android Studio happy.
            // This build type is later ignored in the variantFilter section
        }

        named("release").configure {
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
        if (inCiBuild && name == "debug") {
            ignore = true
        }
    }
}

dependencies {
    implementation(project(":auth"))
    implementation(project(":firestore"))
    implementation(project(":database"))
    implementation(project(":storage"))

    implementation(Config.Libs.Androidx.lifecycleExtensions)
}

apply(plugin = "com.google.gms.google-services")
