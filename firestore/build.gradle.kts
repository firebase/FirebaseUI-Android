tasks.named("check").configure { dependsOn("compileDebugAndroidTestJavaWithJavac") }

android {
    defaultConfig {
        multiDexEnabled = true
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        named("release").configure {
            postprocessing {
                consumerProguardFiles("proguard-rules.pro")
            }
        }
    }
}

dependencies {
    api(project(":common"))
    api(Config.Libs.Firebase.firestore)

    api(Config.Libs.Androidx.v4)
    api(Config.Libs.Androidx.recyclerView)

    compileOnly(Config.Libs.Arch.paging)
    annotationProcessor(Config.Libs.Arch.compiler)

    lintChecks(project(":lint"))

    androidTestImplementation(Config.Libs.Arch.coreTesting)
    androidTestImplementation(Config.Libs.Test.junit)
    androidTestImplementation(Config.Libs.Test.junitExt)
    androidTestImplementation(Config.Libs.Test.runner)
    androidTestImplementation(Config.Libs.Test.rules)
    androidTestImplementation(Config.Libs.Test.mockito)
    androidTestImplementation(Config.Libs.Arch.paging)
}
