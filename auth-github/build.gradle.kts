dependencies {
    compileOnly(project(":auth"))

    implementation(Config.Libs.Support.appCompat)
    implementation(Config.Libs.Support.customTabs)

    implementation(Config.Libs.Misc.retrofit)
    implementation(Config.Libs.Misc.retrofitGson)
}
