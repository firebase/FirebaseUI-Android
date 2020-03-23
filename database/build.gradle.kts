tasks.named("check").configure { dependsOn("compileDebugAndroidTestJavaWithJavac") }

android {
    defaultConfig {
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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
