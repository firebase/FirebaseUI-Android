object Config {
    const val version = "8.0.0"
    val submodules = listOf("auth", "common", "firestore", "database", "storage")

    private const val kotlinVersion = "1.3.72"

    object SdkVersions {
        const val compile = 29
        const val target = 29
        const val min = 16
    }

    object Plugins {
        const val android = "com.android.tools.build:gradle:7.0.0"
        const val kotlin = "org.jetbrains.kotlin:kotlin-gradle-plugin:$kotlinVersion"
        const val google = "com.google.gms:google-services:4.3.8"

        const val mavenPublish = "com.vanniktech:gradle-maven-publish-plugin:0.14.2"
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
        }

        object Firebase {
            const val bom = "com.google.firebase:firebase-bom:28.2.0"
            const val auth = "com.google.firebase:firebase-auth"
            const val database = "com.google.firebase:firebase-database"
            const val firestore = "com.google.firebase:firebase-firestore"
            const val storage = "com.google.firebase:firebase-storage"
        }

        object PlayServices {
            const val auth = "com.google.android.gms:play-services-auth:19.0.0"
        }

        object Provider {
            const val facebook = "com.facebook.android:facebook-login:8.1.0"
        }

        object Misc {
            private const val leakCanaryVersion = "1.6.1"
            private const val glideVersion = "4.11.0"

            const val leakCanary = "com.squareup.leakcanary:leakcanary-android:$leakCanaryVersion"
            const val leakCanaryFragments =
                    "com.squareup.leakcanary:leakcanary-support-fragment:$leakCanaryVersion"
            const val leakCanaryNoop =
                    "com.squareup.leakcanary:leakcanary-android-no-op:$leakCanaryVersion"

            const val glide = "com.github.bumptech.glide:glide:$glideVersion"
            const val glideCompiler = "com.github.bumptech.glide:compiler:$glideVersion"

            const val permissions = "pub.devrel:easypermissions:3.0.0"
        }

        object Test {
            const val junit = "junit:junit:4.12"
            const val junitExt = "androidx.test.ext:junit:1.1.2"
            const val truth = "com.google.truth:truth:0.42"
            const val mockito = "org.mockito:mockito-android:2.21.0"
            const val robolectric = "org.robolectric:robolectric:4.3.1"

            const val core = "androidx.test:core:1.3.0"
            const val archCoreTesting = "androidx.arch.core:core-testing:2.1.0"
            const val runner = "androidx.test:runner:1.3.0"
            const val rules = "androidx.test:rules:1.3.0"
        }

        object Lint {
            private const val version = "26.5.0"

            const val api = "com.android.tools.lint:lint-api:$version"
            const val tests = "com.android.tools.lint:lint-tests:$version"
        }
    }
}
