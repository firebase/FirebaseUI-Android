android {
    buildTypes {
        named("release").configure {
            isMinifyEnabled = false
            consumerProguardFiles("proguard-rules.pro")
        }
    }
}

dependencies {
    api(Config.Libs.Androidx.lifecycleRuntime)
    api(Config.Libs.Androidx.lifecycleViewModel)
    implementation(Config.Libs.Androidx.annotations)
    annotationProcessor(Config.Libs.Androidx.lifecycleCompiler)
}
