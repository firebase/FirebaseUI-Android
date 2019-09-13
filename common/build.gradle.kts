android {
    buildTypes {
        named("release").configure {
            isMinifyEnabled = false
            consumerProguardFiles("proguard-rules.pro")
        }
    }
}

dependencies {
    api(Config.Libs.Arch.runtime)
    api(Config.Libs.Arch.viewModel)
    implementation(Config.Libs.Androidx.annotations)
    annotationProcessor(Config.Libs.Arch.compiler)
}
