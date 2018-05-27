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
                isOptimizeCode = true
            }
        }
    }

    lintOptions {
        disable("MissingTranslation")
    }
}

dependencies {
    implementation(Config.Libs.Firebase.core)
    implementation(Config.Libs.Support.design)
    implementation(Config.Libs.Support.multidex)

    implementation(project(":auth"))
    implementation(project(":firestore"))
    implementation(project(":database"))
    implementation(project(":storage"))

    implementation(Config.Libs.Provider.facebook)
    // Needed to override Facebook
    implementation(Config.Libs.Support.cardView)
    implementation(Config.Libs.Support.customTabs)
    implementation(Config.Libs.Provider.twitter) { isTransitive = true }

    implementation(Config.Libs.Miscellaneous.glide)
    annotationProcessor(Config.Libs.Miscellaneous.glideCompiler)

    // Used for FirestorePagingActivity
    implementation(Config.Libs.Arch.paging)

    // The following dependencies are not required to use the Firebase UI library.
    // They are used to make some aspects of the demo app implementation simpler for
    // demonstrative purposes, and you may find them useful in your own apps; YMMV.
    implementation(Config.Libs.Miscellaneous.permissions)
    implementation(Config.Libs.Miscellaneous.butterKnife)
    annotationProcessor(Config.Libs.Miscellaneous.butterKnifeCompiler)
    debugImplementation(Config.Libs.Miscellaneous.leakCanary)
    releaseImplementation(Config.Libs.Miscellaneous.leakCanaryNoop)
    testImplementation(Config.Libs.Miscellaneous.leakCanaryNoop)
}

apply(plugin = "com.google.gms.google-services")
