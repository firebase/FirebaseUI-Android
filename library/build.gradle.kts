plugins {
  id("com.android.library")
  id("com.vanniktech.maven.publish")
}

android {
    compileSdk = Config.SdkVersions.compile
    namespace = "com.firebase.ui"

    defaultConfig {
        minSdk = Config.SdkVersions.min

        resourcePrefix("fui_")
        vectorDrawables.useSupportLibrary = true
    }

    testOptions {
        targetSdk = Config.SdkVersions.target
    }

    compileOptions {    
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

}

dependencies {
    api(project(":auth"))
    api(project(":database"))
    api(project(":firestore"))
    api(project(":storage"))
}

tasks.register("prepareArtifacts") {
    dependsOn("assembleRelease")
    dependsOn(*Config.submodules.map {
        ":$it:assembleRelease"
    }.toTypedArray())
}
