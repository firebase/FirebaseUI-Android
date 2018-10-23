android {
    lintOptions {
        disable("UnknownNullness") // TODO fix in future PR
    }
}

dependencies {
    compileOnly(project(":auth")) { isTransitive = false }
    compileOnly(Config.Libs.Firebase.auth) { isTransitive = false }

    implementation(Config.Libs.Support.appCompat)
    implementation(Config.Libs.Support.customTabs)

    implementation(Config.Libs.Misc.retrofit)
    implementation(Config.Libs.Misc.retrofitGson)
}
