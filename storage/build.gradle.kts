android {
    buildTypes {
        named("release").configure {
            isMinifyEnabled = false
            consumerProguardFiles("proguard-rules.pro")
        }
    }
}

dependencies {
    api(Config.Libs.Misc.glide)

    api(Config.Libs.Firebase.storage)
    // Override Play Services
    implementation(Config.Libs.Androidx.legacySupportv4)
}
