import com.android.build.gradle.BaseExtension
import com.jfrog.bintray.gradle.BintrayExtension
import com.jfrog.bintray.gradle.RecordingCopyTask
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

// See https://github.com/gradle/kotlin-dsl/issues/607#issuecomment-375687119
subprojects { parent!!.path.takeIf { it != rootProject.path }?.let { evaluationDependsOn(it) } }

allprojects {
    repositories {
        google()
        jcenter()
        mavenLocal()
    }

    // Skip Javadoc generation for Java 1.8 as it breaks build
    if (JavaVersion.current().isJava8Compatible) {
        tasks.withType<Javadoc> {
            options {
                this as StandardJavadocDocletOptions
                addStringOption("Xdoclint:none", "-quiet")
            }
        }
    }

    if ((group as String).isNotEmpty() && name != "lint" && name != "internal") {
        configureAndroid()
        configureQuality()

        if (Config.submodules.contains(name) || isLibrary) {
            // TODO: Re-enable this in the future
            // setupPublishing()
            setupTasks()
        }
    }
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
val Project.artifactName get() = if (isLibrary) "firebase-ui" else "firebase-ui-${this.name}"

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
            disable("UnknownNullness") // TODO fix in future PR

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
    check { dependsOn("checkstyle") }

    task<Checkstyle>("checkstyle") {
        configFile = file("$configDir/checkstyle.xml")
        source("src")
        include("**/*.java")
        exclude("**/gen/**")
        classpath = files()
    }
}

fun Project.setupTasks() {
    afterEvaluate {
        if (isLibrary) {
            task("testAll") {
                dependsOn(*Config.submodules.map {
                    ":$it:testDebugUnitTest"
                }.toTypedArray())
            }

            task("prepareArtifacts") {
                dependsOn("javadocJar", "sourcesJar", "assembleRelease")
                dependsOn("generatePomFileForMonolithLibraryPublication")
                dependsOn(*Config.submodules.map {
                    ":$it:prepareArtifacts"
                }.toTypedArray())
            }

            task("publishAllToMavenLocal") {
                dependsOn("publishMonolithLibraryPublicationToMavenLocal")
                dependsOn(*Config.submodules.map {
                    ":$it:publish${it.capitalize()}LibraryPublicationToMavenLocal"
                }.toTypedArray())
            }

            task("publishAllToCustomLocal") {
                dependsOn("publishMonolithLibraryPublicationToCustomLocalRepository")
                dependsOn(*Config.submodules.map {
                    ":$it:publish${it.capitalize()}LibraryPublicationToCustomLocalRepository"
                }.toTypedArray())
            }

            task("bintrayUploadAll") {
                dependsOn("bintrayUpload")
                dependsOn(*Config.submodules.map {
                    ":$it:bintrayUpload"
                }.toTypedArray())
            }
        } else {
            val pomTask = "generatePomFileFor${project.name.capitalize()}LibraryPublication"
            task("prepareArtifacts") {
                dependsOn("javadocJar", "sourcesJar", "assembleRelease", pomTask)
            }
        }
    }
}

fun Project.setupPublishing() {
    println("Configuring publishing for ${this}")

    val sourcesJar = task<Jar>("sourcesJar") {
        classifier = "sources"
        from(project.the<BaseExtension>().sourceSets["main"].java.srcDirs)
    }

    val javadoc = task<Javadoc>("javadoc") {
        setSource(project.the<BaseExtension>().sourceSets["main"].java.srcDirs)
        classpath += configurations["compile"]
        classpath += project.files(project.the<BaseExtension>().bootClasspath)
    }

    val javadocJar = task<Jar>("javadocJar") {
        dependsOn(javadoc)
        classifier = "javadoc"
        from(javadoc.destinationDir)
    }

    artifacts.add("archives", javadocJar)
    artifacts.add("archives", sourcesJar)

    tasks.whenTaskAdded {
        if (name.toLowerCase().contains("publish") && name.contains("publication", true)) {
            dependsOn("assembleRelease")
        }
    }

    apply(plugin = "maven-publish")
    apply(plugin = "com.jfrog.artifactory")

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
        val projectName = name
        group = groupName
        version = Config.version

        publications {
            create<MavenPublication>(publicationName) {
                groupId = groupName
                artifactId = artifactName
                version = Config.version

                val releaseAar = "$buildDir/outputs/aar/${projectName}-release.aar"

                artifact(releaseAar)
                artifact(javadocJar)
                artifact(sourcesJar)

                println("Creating maven publication $publicationName")
                println("\tgroup: $groupName")
                println("\tartifact: $artifactName")
                println("\tversion: $version")
                println("\taar: $releaseAar")

                pom {
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

                        // Common values
                        val repoUrl = "https://github.com/firebase/FirebaseUI-Android"
                        val scmUrl = "scm:git:git@github.com/firebase/firebaseui-android.git"

                        // Name
                        asNode().appendNode("name", artifactId)

                        // Description
                        asNode().appendNode("description", "Firebase UI for Android")

                        // Organization
                        asNode().appendNode("organization").apply {
                            appendNode("name", "FirebaseUI")
                            appendNode("url", repoUrl)
                        }

                        // URL
                        asNode().appendNode("url", repoUrl)

                        // SCM
                        asNode().appendNode("scm").apply {
                            appendNode("connection", scmUrl)
                            appendNode("developerConnection", scmUrl)
                            appendNode("url", repoUrl)
                            appendNode("tag", "HEAD")
                        }

                        // Developers
                        asNode().appendNode("developers").appendNode("developer").apply {
                            appendNode("id", "samtstern")
                            appendNode("email", "samstern@google.com")
                            appendNode("organization", "Firebase")
                            appendNode("organizationUrl", "https://firebase.google.com")

                            appendNode("roles").apply {
                                appendNode("role", "Project-Administrator")
                                appendNode("role", "Developer")
                            }

                            appendNode("timezone", "-8")
                        }

                        // Licenses
                        asNode().appendNode("licenses").appendNode("license").apply {
                            appendNode("name", "The Apache License, Version 2.0")
                            appendNode("url", "http://www.apache.org/licenses/LICENSE-2.0.txt")
                        }
                    }
                }
            }
        }
    }

    val bintrayUsername = System.getProperty("BINTRAY_USER") ?: System.getenv("BINTRAY_USER")
    val bintrayKey = System.getProperty("BINTRAY_KEY") ?: System.getenv("BINTRAY_KEY")

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

    tasks.withType<ArtifactoryTask> { publications(publicationName) }

    apply(plugin = "com.jfrog.bintray")

    configure<BintrayExtension> {

        user = bintrayUsername
        key = bintrayKey
        setPublications(publicationName)
        setConfigurations("archives")

        println("Bintray configuration for ${publicationName}")
        println("\tartifact: ${artifactName}")
        publications.forEach { pubName ->
            println("\tpub: $pubName")

            val publ = project.extensions
                    .getByType(PublishingExtension::class.java)
                    .publications.findByName(pubName) as MavenPublication

            publ.artifacts.forEach { art ->
                println("\t\tpub_artifact: $art")
            }
        }
        configurations.forEach { config ->
            println("\tconfig: $config")

            project.configurations.findByName(config)?.allArtifacts?.forEach { art ->
                println("\t\tconfig_artifact: $art")
            }
        }

        // When uploading, move and rename the generated POM
        val pomSrc = "$buildDir/publications/$publicationName/pom-default.xml"
        val pomDst = "com/firebaseui/$artifactName/${Config.version}/"
        val pomName = "$artifactName-${Config.version}.pom"

        println("POM Transformation")
        println("\tsrc: ${pomSrc}")
        println("\tdst: ${pomDst}")
        println("\tname: ${pomName}")

        filesSpec(closureOf<RecordingCopyTask> {
            from(pomSrc)
            into(pomDst)
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

// TODO: Remove this
apply(from = "publishing.gradle")
