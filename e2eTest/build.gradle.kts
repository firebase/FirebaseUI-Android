import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    alias(libs.plugins.kotlin.compose)
}

android {
    compileSdk = Config.SdkVersions.compile
    namespace = "com.firebase.ui.auth.e2etest"

    defaultConfig {
        minSdk = Config.SdkVersions.min
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
    }

    testOptions {
        targetSdk = Config.SdkVersions.target
        unitTests {
            isIncludeAndroidResources = true
        }
    }
}

dependencies {
    // Depend on the auth module to test it
    implementation(project(":auth"))

    // Compose dependencies
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.material3)

    // Firebase dependencies
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth)

    // Test dependencies
    testImplementation(libs.junit)
    testImplementation(libs.truth)
    testImplementation(libs.test.core)
    testImplementation(libs.robolectric)
    testImplementation(libs.kotlin.reflect)
    testImplementation(libs.mockito.core)
    testImplementation(libs.mockito.kotlin)
    testImplementation(libs.androidx.credentials)
    testImplementation(libs.googleid)
    testImplementation(libs.compose.ui.test.junit4)
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_17
    }
}

val mockitoAgent by configurations.creating

dependencies {
    mockitoAgent(libs.mockito.core) {
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