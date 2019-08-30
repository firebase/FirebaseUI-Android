tasks.named("check").configure { dependsOn("compileDebugAndroidTestJavaWithJavac") }

android {
    defaultConfig {
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
    api(Config.Libs.Firebase.database)

    api(Config.Libs.Support.v4)
    api(Config.Libs.Support.recyclerView)

    compileOnly(Config.Libs.Arch.paging)
    annotationProcessor(Config.Libs.Arch.compiler)

    androidTestImplementation(Config.Libs.Test.junit)
    androidTestImplementation(Config.Libs.Test.runner)
    androidTestImplementation(Config.Libs.Test.rules)
}
