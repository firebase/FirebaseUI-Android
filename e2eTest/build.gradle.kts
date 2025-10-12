plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose") version Config.kotlinVersion
}

android {
    compileSdk = Config.SdkVersions.compile
    namespace = "com.firebase.ui.auth.e2etest"

    defaultConfig {
        minSdk = Config.SdkVersions.min
        targetSdk = Config.SdkVersions.target
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
    }

    testOptions {
        unitTests {
            isIncludeAndroidResources = true
        }
    }
}

dependencies {
    // Depend on the auth module to test it
    implementation(project(":auth"))

    // Compose dependencies
    implementation(platform(Config.Libs.Androidx.Compose.bom))
    implementation(Config.Libs.Androidx.Compose.ui)
    implementation(Config.Libs.Androidx.Compose.material3)

    // Firebase dependencies
    implementation(platform(Config.Libs.Firebase.bom))
    implementation(Config.Libs.Firebase.auth)

    // Test dependencies
    testImplementation(Config.Libs.Test.junit)
    testImplementation(Config.Libs.Test.truth)
    testImplementation(Config.Libs.Test.core)
    testImplementation(Config.Libs.Test.robolectric)
    testImplementation(Config.Libs.Test.kotlinReflect)
    testImplementation(Config.Libs.Test.mockitoCore)
    testImplementation(Config.Libs.Test.mockitoInline)
    testImplementation(Config.Libs.Test.mockitoKotlin)
    testImplementation(Config.Libs.Androidx.credentials)
    testImplementation(Config.Libs.Test.composeUiTestJunit4)
}

val mockitoAgent by configurations.creating

dependencies {
    mockitoAgent(Config.Libs.Test.mockitoCore) {
        isTransitive = false
    }
}

tasks.withType<Test>().configureEach {
    jvmArgs("-javaagent:${mockitoAgent.asPath}")
}

tasks.register<Test>("e2eTest") {
    description = "Runs e2e emulator tests"
    group = "verification"

    val debug = tasks.named<Test>("testDebugUnitTest").get()
    testClassesDirs = debug.testClassesDirs
    classpath = debug.classpath

    doNotTrackState("Always run e2e emulator tests to mirror Android Studio")
}
