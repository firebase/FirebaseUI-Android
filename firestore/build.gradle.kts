tasks.named("check").configure { dependsOn("compileDebugAndroidTestJavaWithJavac") }

android {
    defaultConfig {
        multiDexEnabled = true
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        named("release").configure {
            isMinifyEnabled = false
            consumerProguardFiles("proguard-rules.pro")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
}

dependencies {
    implementation(platform(Config.Libs.Firebase.bom))
    api(project(":common"))
    api(Config.Libs.Firebase.firestore)

    api(Config.Libs.Androidx.legacySupportv4)
    api(Config.Libs.Androidx.recyclerView)

    compileOnly(Config.Libs.Androidx.paging)
    api(Config.Libs.Androidx.pagingRxJava3)
    annotationProcessor(Config.Libs.Androidx.lifecycleCompiler)

    lintChecks(project(":lint"))

    androidTestImplementation(Config.Libs.Test.archCoreTesting)
    androidTestImplementation(Config.Libs.Test.core)
    androidTestImplementation(Config.Libs.Test.junit)
    androidTestImplementation(Config.Libs.Test.junitExt)
    androidTestImplementation(Config.Libs.Test.runner)
    androidTestImplementation(Config.Libs.Test.rules)
    androidTestImplementation(Config.Libs.Test.mockito)
    androidTestImplementation(Config.Libs.Androidx.paging)
}
