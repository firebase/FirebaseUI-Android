plugins {
    id("org.jetbrains.kotlin.jvm")
}

dependencies {
    compileOnly(libs.lint.api)
    compileOnly(libs.kotlin.stdlib)

    testImplementation(libs.lint.api)
    testImplementation(libs.lint.tests)
}

tasks.withType<Jar>().configureEach {
    manifest {
        attributes(mapOf("Lint-Registry-v2" to "com.firebaseui.lint.internal.LintIssueRegistry"))
    }
}
