object Config {
    const val version = "10.0.0-SNAPSHOT"
    val submodules = listOf("auth", "common", "firestore", "database", "storage")

    const val kotlinVersion = "2.2.0"
    const val kotlinSerializationVersion = "1.9.0"

    object SdkVersions {
        const val compile = 36
        const val target = 36
        const val min = 23
    }

    object Plugins {
        const val android = "com.android.tools.build:gradle:8.10.0"
        const val kotlin = "org.jetbrains.kotlin:kotlin-gradle-plugin:$kotlinVersion"
        const val google = "com.google.gms:google-services:4.3.8"

        const val mavenPublish = "com.vanniktech:gradle-maven-publish-plugin:0.30.0"
        const val buildInfo = "org.jfrog.buildinfo:build-info-extractor-gradle:4.15.2"
    }

    object Libs {
        object Kotlin {
            const val jvm = "org.jetbrains.kotlin:kotlin-stdlib-jdk8:$kotlinVersion"
        }

        object Androidx {
            const val annotations = "androidx.annotation:annotation:1.2.0"
            const val activity = "androidx.activity:activity:1.2.3"
            const val customTabs = "androidx.browser:browser:1.3.0"
            const val cardView = "androidx.cardview:cardview:1.0.0"
            const val constraint = "androidx.constraintlayout:constraintlayout:2.0.4"
            const val fragment = "androidx.fragment:fragment:1.3.5"
            const val lifecycleCompiler = "androidx.lifecycle:lifecycle-compiler:2.3.1"
            const val lifecycleExtensions = "androidx.lifecycle:lifecycle-extensions:2.2.0"
            const val lifecycleRuntime = "androidx.lifecycle:lifecycle-runtime:2.3.1"
            const val lifecycleViewModel = "androidx.lifecycle:lifecycle-viewmodel:2.3.1"
            const val legacySupportv4 = "androidx.legacy:legacy-support-v4:1.0.0"
            const val multidex = "androidx.multidex:multidex:2.0.1"
            const val paging = "androidx.paging:paging-runtime:3.0.0"
            const val pagingRxJava = "androidx.paging:paging-rxjava3:3.0.0"
            const val recyclerView = "androidx.recyclerview:recyclerview:1.2.1"
            const val materialDesign = "com.google.android.material:material:1.4.0"

            const val datastorePreferences = "androidx.datastore:datastore-preferences:1.1.1"
            const val credentials = "androidx.credentials:credentials:1.3.0"
            object Compose {
                const val bom = "androidx.compose:compose-bom:2025.10.00"
                const val ui = "androidx.compose.ui:ui"
                const val uiGraphics = "androidx.compose.ui:ui-graphics"
                const val toolingPreview = "androidx.compose.ui:ui-tooling-preview"
                const val tooling = "androidx.compose.ui:ui-tooling"
                const val foundation = "androidx.compose.foundation:foundation"
                const val material3 = "androidx.compose.material3:material3"
                const val materialIconsExtended = "androidx.compose.material:material-icons-extended"
                const val activityCompose = "androidx.activity:activity-compose:1.11.0"
            }

            object Navigation {
                const val nav3Runtime = "androidx.navigation3:navigation3-runtime:1.0.0-alpha08"
                const val nav3UI = "androidx.navigation3:navigation3-ui:1.0.0-alpha08"
                const val lifecycleViewmodelNav3 = "androidx.lifecycle:lifecycle-viewmodel-compose:1.0.0-alpha04"
            }

            const val kotlinxSerialization = "org.jetbrains.kotlinx:kotlinx-serialization-core:$kotlinSerializationVersion"
        }

        object Firebase {
            const val bom = "com.google.firebase:firebase-bom:33.9.0"
            const val auth = "com.google.firebase:firebase-auth"
            const val database = "com.google.firebase:firebase-database"
            const val firestore = "com.google.firebase:firebase-firestore"
            const val storage = "com.google.firebase:firebase-storage"
        }

        object PlayServices {
            const val auth = "com.google.android.gms:play-services-auth:21.3.0"
        }

        object Provider {
            const val facebook = "com.facebook.android:facebook-login:8.1.0"
        }

        object Misc {
            private const val leakCanaryVersion = "2.14"
            private const val glideVersion = "4.11.0"

            const val leakCanary = "com.squareup.leakcanary:leakcanary-android:$leakCanaryVersion"

            const val glide = "com.github.bumptech.glide:glide:$glideVersion"
            const val glideCompiler = "com.github.bumptech.glide:compiler:$glideVersion"

            const val permissions = "pub.devrel:easypermissions:3.0.0"
            const val libphonenumber = "com.googlecode.libphonenumber:libphonenumber:9.0.16"
        }

        object Test {
            const val junit = "junit:junit:4.13.2"
            const val junitExt = "androidx.test.ext:junit:1.1.5"
            const val truth = "com.google.truth:truth:0.42"
            const val mockito = "org.mockito:mockito-android:2.21.0"
            const val mockitoCore = "org.mockito:mockito-core:5.19.0"
            const val mockitoInline = "org.mockito:mockito-inline:5.2.0"
            const val mockitoKotlin = "org.mockito.kotlin:mockito-kotlin:6.0.0"
            const val robolectric = "org.robolectric:robolectric:4.14"

            const val core = "androidx.test:core:1.5.0"
            const val archCoreTesting = "androidx.arch.core:core-testing:2.1.0"
            const val runner = "androidx.test:runner:1.5.0"
            const val rules = "androidx.test:rules:1.5.0"

            const val kotlinReflect = "org.jetbrains.kotlin:kotlin-reflect"
            const val composeUiTestJunit4 = "androidx.compose.ui:ui-test-junit4"
        }

        object Lint {
            private const val version = "30.0.0"

            const val api = "com.android.tools.lint:lint-api:$version"
            const val tests = "com.android.tools.lint:lint-tests:$version"
        }
    }
}
