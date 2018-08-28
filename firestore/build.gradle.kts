check { dependsOn("compileDebugAndroidTestJavaWithJavac") }

android {
    defaultConfig {
        testInstrumentationRunner = "android.support.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        getByName("release") {
            postprocessing {
                consumerProguardFiles("proguard-rules.pro")
            }
        }
    }
}

dependencies {
    api(project(":common"))
    api(Config.Libs.Firebase.firestore)

    api(Config.Libs.Support.v4)
    api(Config.Libs.Support.recyclerView)
    annotationProcessor(Config.Libs.Arch.compiler)

    compileOnly(Config.Libs.Arch.paging)

    androidTestImplementation(Config.Libs.Test.junit)
    androidTestImplementation(Config.Libs.Test.runner)
    androidTestImplementation(Config.Libs.Test.rules)
    androidTestImplementation(Config.Libs.Test.mockito)
    androidTestImplementation(Config.Libs.Arch.paging)
}
