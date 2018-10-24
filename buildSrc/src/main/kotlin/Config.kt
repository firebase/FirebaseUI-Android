object Config {
    const val version = "4.2.1"
    val submodules = listOf("auth", "auth-github", "common", "firestore", "database", "storage")

    private const val kotlinVersion = "1.2.61"

    object SdkVersions {
        const val compile = 27
        const val target = 27
        const val min = 16
    }

    object Plugins {
        const val android = "com.android.tools.build:gradle:3.2.1"
        const val kotlin = "org.jetbrains.kotlin:kotlin-gradle-plugin:$kotlinVersion"
        const val google = "com.google.gms:google-services:4.0.2"

        const val bintray = "com.jfrog.bintray.gradle:gradle-bintray-plugin:1.8.4"
        const val buildInfo = "org.jfrog.buildinfo:build-info-extractor-gradle:4.7.5"
    }

    object Libs {
        object Kotlin {
            const val jvm = "org.jetbrains.kotlin:kotlin-stdlib-jdk8:$kotlinVersion"
        }

        object Support {
            private const val version = "27.1.1"

            const val multidex = "com.android.support:multidex:1.0.3"
            const val annotations = "com.android.support:support-annotations:$version"
            const val v4 = "com.android.support:support-v4:$version"
            const val appCompat = "com.android.support:appcompat-v7:$version"
            const val design = "com.android.support:design:$version"
            const val recyclerView = "com.android.support:recyclerview-v7:$version"
            const val cardView = "com.android.support:cardview-v7:$version"
            const val customTabs = "com.android.support:customtabs:$version"

            const val constraint = "com.android.support.constraint:constraint-layout:1.1.3"
        }

        object Arch {
            private const val version = "1.1.1"

            const val runtime = "android.arch.lifecycle:runtime:$version"
            const val viewModel = "android.arch.lifecycle:viewmodel:$version"
            const val extensions = "android.arch.lifecycle:extensions:$version"
            const val compiler = "android.arch.lifecycle:compiler:$version"

            const val paging = "android.arch.paging:runtime:1.0.1"
        }

        object Firebase {
            const val core = "com.google.firebase:firebase-core:16.0.4"
            const val auth = "com.google.firebase:firebase-auth:16.0.5"
            const val firestore = "com.google.firebase:firebase-firestore:17.1.1"
            const val database = "com.google.firebase:firebase-database:16.0.3"
            const val storage = "com.google.firebase:firebase-storage:16.0.3"
        }

        object PlayServices {
            const val auth = "com.google.android.gms:play-services-auth:16.0.1"
        }


        object Provider {
            const val facebook = "com.facebook.android:facebook-login:4.35.0"
            // WARNING: the Twitter requires Java 8 support. Therefore, the dep cannot be upgraded
            // futher until we decide to force clients to enable Java 8 support.
            const val twitter = "com.twitter.sdk.android:twitter-core:3.1.1@aar"
        }

        object Misc {
            private const val retrofitVersion = "2.4.0"
            private const val leakCanaryVersion = "1.6.1"
            private const val glideVersion = "4.8.0"
            private const val butterVersion = "8.8.1"

            const val retrofit = "com.squareup.retrofit2:retrofit:$retrofitVersion"
            const val retrofitGson = "com.squareup.retrofit2:converter-gson:$retrofitVersion"

            const val leakCanary = "com.squareup.leakcanary:leakcanary-android:$leakCanaryVersion"
            const val leakCanaryFragments =
                    "com.squareup.leakcanary:leakcanary-support-fragment:$leakCanaryVersion"
            const val leakCanaryNoop =
                    "com.squareup.leakcanary:leakcanary-android-no-op:$leakCanaryVersion"

            const val glide = "com.github.bumptech.glide:glide:$glideVersion"
            const val glideCompiler = "com.github.bumptech.glide:compiler:$glideVersion"

            const val butterKnife = "com.jakewharton:butterknife:$butterVersion"
            const val butterKnifeCompiler = "com.jakewharton:butterknife-compiler:$butterVersion"

            const val permissions = "pub.devrel:easypermissions:1.3.0"
            const val materialProgress = "me.zhanghai.android.materialprogressbar:library:1.4.2"
        }

        object Test {
            const val junit = "junit:junit:4.12"
            const val truth = "com.google.truth:truth:0.42"
            const val mockito = "org.mockito:mockito-android:2.21.0"
            const val robolectric = "org.robolectric:robolectric:3.8"

            const val runner = "com.android.support.test:runner:1.0.2"
            const val rules = "com.android.support.test:rules:1.0.2"
        }

        object Lint {
            private const val version = "26.2.1"

            const val api = "com.android.tools.lint:lint-api:$version"
            const val tests = "com.android.tools.lint:lint-tests:$version"
        }
    }
}
