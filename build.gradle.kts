import com.android.build.gradle.BaseExtension
import com.android.build.gradle.LibraryExtension
import com.jfrog.bintray.gradle.BintrayExtension
import com.jfrog.bintray.gradle.tasks.RecordingCopyTask
import org.jfrog.gradle.plugin.artifactory.dsl.ArtifactoryPluginConvention
import org.jfrog.gradle.plugin.artifactory.dsl.DoubleDelegateWrapper
import org.jfrog.gradle.plugin.artifactory.dsl.PublisherConfig
import org.jfrog.gradle.plugin.artifactory.task.ArtifactoryTask

buildscript {
    repositories {
        google()
        jcenter()
        mavenLocal()
    }

    dependencies {
        classpath(Config.Plugins.android)
        classpath(Config.Plugins.kotlin)
        classpath(Config.Plugins.google)
        classpath(Config.Plugins.bintray)
        classpath(Config.Plugins.buildInfo)
    }
}

plugins {
    `build-scan` version "1.16"
    id("com.github.ben-manes.versions") version "0.20.0"
}

buildScan {
    setTermsOfServiceUrl("https://gradle.com/terms-of-service")
    setTermsOfServiceAgree("yes")
}

// See https://github.com/gradle/kotlin-dsl/issues/607#issuecomment-375687119
subprojects { parent!!.path.takeIf { it != rootProject.path }?.let { evaluationDependsOn(it) } }

allprojects {
    repositories {
        google()
        jcenter()
        mavenLocal()
    }

    if ((group as String).isNotEmpty() && name != "lint" && name != "internal") {
        configureAndroid()
        configureQuality()

        if (Config.submodules.contains(name) || isLibrary) {
            setupPublishing()
        }
    }
}

tasks.withType<Wrapper>().configureEach {
    distributionType = Wrapper.DistributionType.ALL
}

val Project.configDir get() = "$rootDir/library/quality"
val Project.reportsDir get() = "$buildDir/reports"

/**
 * Determines if a Project is the 'library' module
 */
val Project.isLibrary get() = name == "library"

/**
 * Returns the maven artifact name for a Project.
 */
val Project.artifactName get() = if (isLibrary) "firebase-ui" else "firebase-ui-$name"

/**
 * Returns the name for a Project's maven publication.
 */
val Project.publicationName get() = if (isLibrary) "monolithLibrary" else "${name}Library"

fun Project.configureAndroid() {
    if (name == "app" || name == "proguard-tests") {
        apply(plugin = "com.android.application")
    } else {
        apply(plugin = "com.android.library")
    }

    configure<BaseExtension> {
        compileSdkVersion(Config.SdkVersions.compile)

        defaultConfig {
            minSdkVersion(Config.SdkVersions.min)
            targetSdkVersion(Config.SdkVersions.target)

            versionName = Config.version
            versionCode = 1

            resourcePrefix("fui_")
            vectorDrawables.useSupportLibrary = true
        }

        lintOptions {
            disable(
                    "ObsoleteLintCustomCheck", // ButterKnife will fix this in v9.0
                    "IconExpectedSize",
                    "InvalidPackage", // Firestore uses GRPC which makes lint mad
                    "NewerVersionAvailable", "GradleDependency", // For reproducible builds
                    "SelectableText", "SyntheticAccessor" // We almost never care about this
            )

            isCheckAllWarnings = true
            isWarningsAsErrors = true
            isAbortOnError = true

            baselineFile = file("$configDir/lint-baseline.xml")
            htmlOutput = file("$reportsDir/lint-results.html")
            xmlOutput = file("$reportsDir/lint-results.xml")
        }
    }
}

fun Project.configureQuality() {
    apply(plugin = "checkstyle")

    configure<CheckstyleExtension> { toolVersion = "8.10.1" }
    tasks.named("check").configure { dependsOn("checkstyle") }

    tasks.register<Checkstyle>("checkstyle") {
        configFile = file("${project.configDir}/checkstyle.xml")
        source("src")
        include("**/*.java")
        exclude("**/gen/**")
        classpath = files()
    }
}

fun Project.setupPublishing() {
    val sourcesJar = tasks.register<Jar>("sourcesJar") {
        classifier = "sources"
        from(project.the<BaseExtension>().sourceSets["main"].java.srcDirs)
    }

    val javadoc = tasks.register<Javadoc>("javadoc") {
        setSource(project.the<BaseExtension>().sourceSets["main"].java.srcDirs)
        classpath += files(project.the<BaseExtension>().bootClasspath)

        project.the<LibraryExtension>().libraryVariants.configureEach {
            dependsOn(assemble)
            classpath += files((javaCompiler as AbstractCompile).classpath)
        }

        // Ignore warnings about incomplete documentation
        (options as StandardJavadocDocletOptions).addStringOption("Xdoclint:none", "-quiet")
    }

    val javadocJar = tasks.register<Jar>("javadocJar") {
        dependsOn(javadoc)
        classifier = "javadoc"
        from(javadoc.get().destinationDir)
    }

    artifacts.add("archives", javadocJar)
    artifacts.add("archives", sourcesJar)

    afterEvaluate {
        if (isLibrary) {
            tasks.register("testAll") {
                dependsOn(*Config.submodules.map {
                    ":$it:testDebugUnitTest"
                }.toTypedArray())
            }

            tasks.register("prepareArtifacts") {
                dependsOn(javadocJar, sourcesJar, "assembleRelease")
                dependsOn("generatePomFileForMonolithLibraryPublication")
                dependsOn(*Config.submodules.map {
                    ":$it:prepareArtifacts"
                }.toTypedArray())
            }

            tasks.register("publishAllToMavenLocal") {
                dependsOn("publishMonolithLibraryPublicationToMavenLocal")
                dependsOn(*Config.submodules.map {
                    ":$it:publish${it.capitalize()}LibraryPublicationToMavenLocal"
                }.toTypedArray())
            }

            tasks.register("publishAllToCustomLocal") {
                dependsOn("publishMonolithLibraryPublicationToCustomLocalRepository")
                dependsOn(*Config.submodules.map {
                    ":$it:publish${it.capitalize()}LibraryPublicationToCustomLocalRepository"
                }.toTypedArray())
            }

            tasks.register("bintrayUploadAll") {
                dependsOn("bintrayUpload")
                dependsOn(*Config.submodules.map {
                    ":$it:bintrayUpload"
                }.toTypedArray())
            }
        } else {
            val pomTask = "generatePomFileFor${name.capitalize()}LibraryPublication"
            tasks.register("prepareArtifacts") {
                dependsOn(javadocJar, sourcesJar, "assembleRelease", pomTask)
            }
        }

        tasks.named("bintrayUpload").configure { dependsOn("prepareArtifacts") }
    }

    apply(plugin = "maven-publish")
    apply(plugin = "com.jfrog.artifactory")
    apply(plugin = "com.jfrog.bintray")

    configure<PublishingExtension> {
        repositories {
            maven {
                name = "CustomLocal"
                // By passing -Pcustom_local=/some/path and running the
                // publishLibraryPublicationToCustomLocalRepository task you can publish this library to a
                // custom maven repository location on your machine.
                url = uri(properties["custom_local"] ?: "/tmp/")
            }

            maven {
                name = "BuildLocal"
                url = uri("$buildDir/repo")
            }
        }

        // We need to override the variables 'group' and 'version' on the 'Project' object in order
        // to prevent the bintray plugin from creating 'unspecified' artifacts.
        val groupName = "com.firebaseui"
        group = groupName
        version = Config.version

        publications {
            create<MavenPublication>(publicationName) {
                groupId = groupName
                artifactId = artifactName
                version = Config.version

                val releaseAar = "$buildDir/outputs/aar/${project.name}-release.aar"

                logger.info("""
                    |Creating maven publication '$publicationName'
                    |    Group: $groupName
                    |    Artifact: $artifactName
                    |    Version: $version
                    |    Aar: $releaseAar
                """.trimMargin())

                artifact(releaseAar)
                artifact(javadocJar.get())
                artifact(sourcesJar.get())

                pom {
                    name.set("FirebaseUI ${project.name.capitalize()}")
                    description.set("Firebase UI for Android")
                    url.set("https://github.com/firebase/FirebaseUI-Android")

                    organization {
                        name.set("Firebase")
                        url.set("https://github.com/firebase")
                    }

                    scm {
                        val scmUrl = "scm:git:git@github.com/firebase/firebaseui-android.git"
                        connection.set(scmUrl)
                        developerConnection.set(scmUrl)
                        url.set(this@pom.url)
                        tag.set("HEAD")
                    }

                    developers {
                        developer {
                            id.set("samtstern")
                            name.set("Sam Stern")
                            email.set("samstern@google.com")
                            organization.set("Firebase")
                            organizationUrl.set("https://firebase.google.com")
                            roles.set(listOf("Project-Administrator", "Developer"))
                            timezone.set("-8")
                        }

                        developer {
                            id.set("SUPERCILEX")
                            name.set("Alex Saveau")
                            email.set("saveau.alexandre@gmail.com")
                            roles.set(listOf("Developer"))
                            timezone.set("-8")
                        }
                    }

                    licenses {
                        license {
                            name.set("The Apache License, Version 2.0")
                            url.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
                        }
                    }

                    withXml {
                        asNode().appendNode("dependencies").apply {
                            fun Dependency.write(scope: String) = appendNode("dependency").apply {
                                appendNode("groupId", group)
                                appendNode("artifactId", if (group == groupName) {
                                    "firebase-ui-$name"
                                } else {
                                    name
                                })
                                appendNode("version", version)
                                appendNode("scope", scope)
                            }

                            for (dependency in configurations["api"].dependencies) {
                                dependency.write("compile")
                            }
                            for (dependency in configurations["implementation"].dependencies) {
                                dependency.write("runtime")
                            }
                        }
                    }
                }
            }
        }
    }

    tasks.matching {
        it.name.contains("publish") && it.name.contains("publication", true)
    }.configureEach {
        dependsOn("assembleRelease")
    }

    val bintrayUsername = properties["bintrayUser"] as String?
            ?: System.getProperty("BINTRAY_USER") ?: System.getenv("BINTRAY_USER")
    val bintrayKey = properties["bintrayKey"] as String?
            ?: System.getProperty("BINTRAY_KEY") ?: System.getenv("BINTRAY_KEY")

    configure<ArtifactoryPluginConvention> {
        setContextUrl("https://oss.jfrog.org")
        publish(closureOf<PublisherConfig> {
            repository(closureOf<DoubleDelegateWrapper> {
                invokeMethod("setRepoKey", "oss-snapshot-local")
                invokeMethod("setUsername", bintrayUsername)
                invokeMethod("setPassword", bintrayKey)
            })
        })
    }

    tasks.withType<ArtifactoryTask>().configureEach { publications(publicationName) }

    configure<BintrayExtension> {
        user = bintrayUsername
        key = bintrayKey
        setPublications(publicationName)

        // When uploading, move and rename the generated POM
        val pomSrc = "$buildDir/publications/$publicationName/pom-default.xml"
        val pomDest = "com/firebaseui/$artifactName/${Config.version}/"
        val pomName = "$artifactName-${Config.version}.pom"

        val pubLog: (String) -> String = { name ->
            val publishing = project.extensions
                    .getByType(PublishingExtension::class.java)
                    .publications[name] as MavenPublication
            "'$name': ${publishing.artifacts}"
        }
        logger.info("""
            |Bintray configuration for '$publicationName'
            |    Artifact name: $artifactName
            |    Artifacts: ${publications.joinToString(transform = pubLog)}
        """.trimMargin())
        logger.info("""
            |POM transformation
            |    Src: $pomSrc
            |    Dest: $pomDest
            |    Name: $pomName
        """.trimMargin())

        filesSpec(closureOf<RecordingCopyTask> {
            from(pomSrc)
            into(pomDest)
            rename(KotlinClosure1<String, String>({ pomName }))
        })

        pkg(closureOf<BintrayExtension.PackageConfig> {
            repo = "firebase-ui"
            name = artifactName
            userOrg = "firebaseui"
            setLicenses("Apache-2.0")
            vcsUrl = "https://github.com/firebase/FirebaseUI-Android.git"

            version(closureOf<BintrayExtension.VersionConfig> {
                name = Config.version
            })
        })
    }
}
