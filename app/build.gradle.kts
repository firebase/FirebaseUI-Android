// NOTE: this project uses Gradle Kotlin DSL. More common build.gradle instructions can be found in
// the main README.

android {
    defaultConfig {
        multiDexEnabled = true
    }

    buildTypes {
        named("release").configure {
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
        disable("ResourceName", "MissingTranslation")
    }
}

dependencies {
    implementation(Config.Libs.Firebase.core)
    implementation(Config.Libs.Support.design)
    implementation(Config.Libs.Support.multidex)

    implementation(project(":auth"))
    implementation(project(":auth-github"))
    implementation(project(":firestore"))
    implementation(project(":database"))
    implementation(project(":storage"))

    implementation(Config.Libs.Provider.facebook)
    // Needed to override Facebook
    implementation(Config.Libs.Support.cardView)
    implementation(Config.Libs.Support.customTabs)
    implementation(Config.Libs.Provider.twitter) { isTransitive = true }

    implementation(Config.Libs.Misc.glide)
    annotationProcessor(Config.Libs.Misc.glideCompiler)

    // Used for FirestorePagingActivity
    implementation(Config.Libs.Arch.paging)

    // The following dependencies are not required to use the Firebase UI library.
    // They are used to make some aspects of the demo app implementation simpler for
    // demonstrative purposes, and you may find them useful in your own apps; YMMV.
    implementation(Config.Libs.Misc.permissions)
    implementation(Config.Libs.Misc.butterKnife)
    annotationProcessor(Config.Libs.Misc.butterKnifeCompiler)
    debugImplementation(Config.Libs.Misc.leakCanary)
    debugImplementation(Config.Libs.Misc.leakCanaryFragments)
    releaseImplementation(Config.Libs.Misc.leakCanaryNoop)
    testImplementation(Config.Libs.Misc.leakCanaryNoop)
}

apply(plugin = "com.google.gms.google-services")
