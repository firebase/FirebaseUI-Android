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

    implementation(platform(Config.Libs.Firebase.bom))
    api(Config.Libs.Firebase.storage)
    // Override Play Services
    implementation(Config.Libs.Androidx.legacySupportv4)
}
