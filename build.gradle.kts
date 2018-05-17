import com.android.build.gradle.BaseExtension

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
        mavenCentral()
    }

    if ((group as String).isNotEmpty() && name != "lint" && name != "internal") {
        configureAndroid()
        configureQuality()
        setupPublishing(name == "library")
    }
}

val Project.configDir get() = "$rootDir/library/quality"
val Project.reportsDir get() = "$buildDir/reports"

fun Project.configureAndroid() {
    apply(plugin = "com.android.${if (name == "app" || name == "proguard-tests") {
        "application"
    } else {
        "library"
    }}")

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
                    "ObsoleteLintCustomCheck", // TODO ButterKnife will fix this in v9.0
                    "IconExpectedSize",
                    "InvalidPackage", // Firestore uses GRPC which makes lint mad
                    "NewerVersionAvailable", "GradleDependency" // For reproducible builds
            )

            baselineFile = file("$configDir/lint-baseline.xml")
            isCheckAllWarnings = true
            isWarningsAsErrors = true
            isAbortOnError = true
            htmlOutput = file("$reportsDir/lint-results.html")
            xmlOutput = file("$reportsDir/lint-results.xml")
        }
    }
}

fun Project.configureQuality() {
    apply(plugin = "checkstyle")

    configure<CheckstyleExtension> { toolVersion = "8.3" }
    check { dependsOn("checkstyle") }

    task("checkstyle", Checkstyle::class) {
        configFile = file("$configDir/checkstyle.xml")
        source("src")
        include("**/*.java")
        exclude("**/gen/**")
        classpath = files()
    }
}

fun Project.setupPublishing(isLibrary: Boolean) {
    tasks.whenTaskAdded {
        if (name.contains("publish") && name.contains("publication", true)) {
            dependsOn("assemble")
        }
    }

    task("sourcesJar", Jar::class) {
        classifier = "sources"
        from(project.the<BaseExtension>().sourceSets.getByName("main").java.srcDirs)
    }

    task("javadoc", Javadoc::class) {
        setSource(project.the<BaseExtension>().sourceSets.getByName("main").java.srcDirs)
        classpath += configurations.getByName("compile")
        classpath += project.files(project.the<BaseExtension>().bootClasspath)
    }

    task("javadocJar", Jar::class) {
        dependsOn("javadoc")
        classifier = "javadoc"
        from(tasks.withType(Javadoc::class.java)["javadoc"].destinationDir)
    }

    artifacts.add("archives", tasks["javadocJar"])
    artifacts.add("archives", tasks["sourcesJar"])

    apply(plugin = "com.jfrog.artifactory")
    apply(plugin = "maven-publish")

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

        // POM to meet maven specs
        val publicationName = if (isLibrary) "monolithLibrary" else "${name}Library"
        val archivesBaseName = if (isLibrary) "firebase-ui" else "firebase-ui-$name"
        val groupName = "com.firebaseui"
        val versionName = Config.version

        // We need to override the variables 'group' and 'version' on the 'Project' object in order
        // to prevent the bintray plugin from creating 'unspecified' artifacts.
        group = groupName
        version = versionName

        publications {

        }
    }
}

// publishing {
//     publications {
//         "${publicationName}"(MavenPublication) {

//             groupId groupName
//             artifactId archivesBaseName
//             version versionName

//             artifact "$buildDir/outputs/aar/${archivesBaseName}-release.aar"
//             artifact javadocJar
//             artifact sourcesJar

//             pom.withXml {
//                 // Dependencies
//                 def dependenciesNode = asNode().getAt("dependencies")[0]
//                 if (dependenciesNode == null) {
//                     dependenciesNode = asNode().appendNode("dependencies")
//                 }

//                 configurations.api.dependencies.each {
//                     def dependencyNode = dependenciesNode.appendNode('dependency')

//                     if (submodules.contains(it.name)) {
//                         dependencyNode.appendNode('groupId', groupName)
//                         dependencyNode.appendNode('artifactId', "firebase-ui-${it.name}")
//                         dependencyNode.appendNode('version', versionName)
//                     } else {
//                         dependencyNode.appendNode('groupId', it.group)
//                         dependencyNode.appendNode('artifactId', it.name)
//                         dependencyNode.appendNode('version', it.version)
//                     }

//                     dependencyNode.appendNode('scope', 'compile')
//                 }
//                 configurations.implementation.dependencies.each {
//                     def dependencyNode = dependenciesNode.appendNode('dependency')

//                     if (submodules.contains(it.name)) {
//                         dependencyNode.appendNode('groupId', groupName)
//                         dependencyNode.appendNode('artifactId', "firebase-ui-${it.name}")
//                         dependencyNode.appendNode('version', versionName)
//                     } else {
//                         dependencyNode.appendNode('groupId', it.group)
//                         dependencyNode.appendNode('artifactId', it.name)
//                         dependencyNode.appendNode('version', it.version)
//                     }

//                     dependencyNode.appendNode('scope', 'runtime')
//                 }

//                 // Common values
//                 def repoUrl = 'https://github.com/firebase/FirebaseUI-Android'
//                 def scmUrl = 'scm:git:git@github.com/firebase/firebaseui-android.git'

//                 // Name
//                 asNode().appendNode('name', artifactId)

//                 // Description
//                 asNode().appendNode('description', 'Firebase UI for Android')

//                 // Organization
//                 def organization = asNode().appendNode('organization')
//                 organization.appendNode('name', 'FirebaseUI')
//                 organization.appendNode('url', repoUrl)

//                 // URL
//                 asNode().appendNode('url', repoUrl)

//                 // SCM
//                 def scm = asNode().appendNode('scm')
//                 scm.appendNode('connection', scmUrl)
//                 scm.appendNode('developerConnection', scmUrl)
//                 scm.appendNode('url', repoUrl)
//                 scm.appendNode('tag', 'HEAD')

//                 // Developers
//                 def developer = asNode().appendNode('developers').appendNode('developer')
//                 developer.appendNode('id', 'samtstern')
//                 developer.appendNode('email', 'samstern@google.com')
//                 developer.appendNode('organization', 'Firebase')
//                 developer.appendNode('organizationUrl', 'https://firebase.google.com')
//                 def roles = developer.appendNode('roles')
//                 roles.appendNode('role', 'Project-Administrator')
//                 roles.appendNode('role', 'Developer')
//                 developer.appendNode('timezone', '-8')

//                 // Licenses
//                 def license = asNode().appendNode('licenses').appendNode('license')
//                 license.appendNode('name', 'The Apache License, Version 2.0')
//                 license.appendNode('url', 'http://www.apache.org/licenses/LICENSE-2.0.txt')
//             }
//         }
//     }
// }

// def bintrayUsername = hasProperty('BINTRAY_USER') ? getProperty('BINTRAY_USER') : System.getenv('BINTRAY_USER')
// def bintrayKey = hasProperty('BINTRAY_KEY') ? getProperty('BINTRAY_KEY') : System.getenv('BINTRAY_KEY')

// artifactory {
//     contextUrl = 'https://oss.jfrog.org'
//     publish {
//         repository {
//             repoKey = 'oss-snapshot-local'

//             username = bintrayUsername
//             password = bintrayKey
//         }
//     }
// }

// artifactoryPublish {
//     publications(publishing.publications."$publicationName")
// }

// // Bintray Configuration (applies to submodule and the monolith)
// project.apply plugin: 'com.jfrog.bintray'

// def pomLoc = isLibrary ? "$buildDir/publications/monolithLibrary/pom-default.xml" : "$buildDir/publications/${project.name}Library/pom-default.xml"

// bintray {
//     user = bintrayUsername
//     key = bintrayKey
//     publications = [publicationName]

//     filesSpec {
//         from pomLoc
//         into "com/firebaseui/$archivesBaseName/$versionName/"
//         rename { String fileName ->
//             "${archivesBaseName}-${versionName}.pom"
//         }
//     }

//     configurations = ['archives']

//     pkg {
//         repo = 'firebase-ui'
//         name = archivesBaseName
//         userOrg = 'firebaseui'
//         licenses = ['Apache-2.0']
//         vcsUrl = 'https://github.com/firebase/FirebaseUI-Android.git'

//         version {
//             name = versionName
//         }
//     }
// }
//     }
// }

// allprojects {
//     afterEvaluate { project ->
//         def isSubmodule = submodules.contains(project.name)

//         if (isSubmodule) {
//             // Only applies to submodules, not the library module
//             def pomTask = "generatePomFileFor${project.name.capitalize()}LibraryPublication"

//             // Convenience task to prepare everything we need for releases
//             task prepareArtifacts(dependsOn: [javadocJar, sourcesJar, assembleRelease, pomTask]) {}
//         }

//     }
// }
