tasks.named("check").configure { dependsOn("compileDebugAndroidTestJavaWithJavac") }

android {
    defaultConfig {
        testInstrumentationRunner = "android.support.test.runner.AndroidJUnitRunner"
    }
}

dependencies {
    api(project(":common"))
    api(Config.Libs.Firebase.database)

    api(Config.Libs.Support.v4)
    api(Config.Libs.Support.recyclerView)
    annotationProcessor(Config.Libs.Arch.compiler)

    androidTestImplementation(Config.Libs.Test.junit)
    androidTestImplementation(Config.Libs.Test.runner)
    androidTestImplementation(Config.Libs.Test.rules)
}
