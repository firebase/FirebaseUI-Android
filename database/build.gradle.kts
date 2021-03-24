plugins {
  id("com.android.library")
  id("com.vanniktech.maven.publish")
}

android {
    compileSdkVersion(Config.SdkVersions.compile)

    defaultConfig {
        minSdkVersion(Config.SdkVersions.min)
        targetSdkVersion(Config.SdkVersions.target)

        versionName = Config.version
        versionCode = 1

        resourcePrefix("fui_")
        vectorDrawables.useSupportLibrary = true
    }

    defaultConfig {
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    lintOptions {
        // Common lint options across all modules
        disable(
            "ObsoleteLintCustomCheck", // ButterKnife will fix this in v9.0
            "IconExpectedSize",
            "InvalidPackage", // Firestore uses GRPC which makes lint mad
            "NewerVersionAvailable", "GradleDependency", // For reproducible builds
            "SelectableText", "SyntheticAccessor" // We almost never care about this
        )

        isCheckAllWarnings = true
        isWarningsAsErrors = true
        isAbortOnError = true

        baselineFile = file("$rootDir/library/quality/lint-baseline.xml")
    }

    buildTypes {
        named("release").configure {
            isMinifyEnabled = false
            consumerProguardFiles("proguard-rules.pro")
        }
    }
}

dependencies {
    implementation(platform(Config.Libs.Firebase.bom))
    api(project(":common"))
    api(Config.Libs.Firebase.database)

    api(Config.Libs.Androidx.legacySupportv4)
    api(Config.Libs.Androidx.recyclerView)

    compileOnly(Config.Libs.Androidx.paging)
    annotationProcessor(Config.Libs.Androidx.lifecycleCompiler)

    androidTestImplementation(Config.Libs.Test.junit)
    androidTestImplementation(Config.Libs.Test.junitExt)
    androidTestImplementation(Config.Libs.Test.runner)
    androidTestImplementation(Config.Libs.Test.rules)
}
