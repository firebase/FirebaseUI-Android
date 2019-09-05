import com.android.build.gradle.internal.dsl.TestOptions

android {
    buildTypes {
        named("release").configure {
            postprocessing {
                consumerProguardFiles("auth-proguard.pro")
            }
        }
    }

    lintOptions {
        disable("UnusedQuantity")
        disable("UnknownNullness")  // TODO fix in future PR
        disable("TypographyQuotes") // Straight versus directional quotes
        disable("DuplicateStrings")
    }

    testOptions {
        unitTests(closureOf<TestOptions.UnitTestOptions> {
            isIncludeAndroidResources = true
        })
    }
}

dependencies {
    implementation(Config.Libs.Androidx.design)
    implementation(Config.Libs.Androidx.customTabs)
    implementation(Config.Libs.Androidx.constraint)
    implementation(Config.Libs.Misc.materialProgress)

    implementation(Config.Libs.Arch.extensions)
    annotationProcessor(Config.Libs.Arch.compiler)

    api(Config.Libs.Firebase.auth)
    api(Config.Libs.PlayServices.auth)

    compileOnly(Config.Libs.Provider.facebook)
    implementation(Config.Libs.Androidx.v4) // Needed to override deps
    implementation(Config.Libs.Androidx.cardView) // Needed to override Facebook
    compileOnly(Config.Libs.Provider.twitter) { isTransitive = true }

    testImplementation(Config.Libs.Test.junit)
    testImplementation(Config.Libs.Test.truth)
    testImplementation(Config.Libs.Test.mockito)
    testImplementation(Config.Libs.Test.core)
    testImplementation(Config.Libs.Test.robolectric)
    testImplementation(Config.Libs.Provider.facebook)
    testImplementation(Config.Libs.Provider.twitter) { isTransitive = true }

    debugImplementation(project(":internal:lintchecks"))
}
