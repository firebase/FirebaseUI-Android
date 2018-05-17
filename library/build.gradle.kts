check { dependsOn("testAll", "prepareArtifacts") }

android {
    lintOptions {
        isAbortOnError = false
    }
}

dependencies {
    api(project(":auth"))
    api(project(":database"))
    api(project(":firestore"))
    api(project(":storage"))
}

///**
// * Returns list of tasks names that prepareArtifacts should depend on.
// */
//def prepareArtifactsTasks() {
//    return submodules.collect { name ->
//        ":${name}:prepareArtifacts"
//    }.toArray()
//}
//
///**
// * Returns list of publish tasks that monolith publish tasks should depend on.
// * @param repo relevant repository, such as 'MavenLocal' or 'CustomLocalRepository'
// */
//def publishToRepoTasks(repo) {
//    return submodules.collect { name ->
//        // Ex: name = database, repo = MavenLocal
//        // Return: :database:publishDatabaseLibraryPublicationToMavenLocal
//        ":${name}:publish${name.capitalize()}LibraryPublicationTo${repo}"
//    }.toArray()
//}
//
///**
// * Returns a list of task names to upload modules to bintray.
// */
//def bintrayUploadTasks() {
//    return submodules.collect { name ->
//        ":${name}:bintrayUpload"
//    }.toArray()
//}
//
///**
// * Returns a list of tasks names to test submodules.
// */
//def testTasks() {
//    return submodules.collect { name ->
//        ":${name}:testDebugUnitTest"
//    }.toArray()
//}
//
///**
// * Publish all artifacts to the maven local repository.
// */
//task publishAllToMavenLocal(dependsOn: [
//        'publishMonolithLibraryPublicationToMavenLocal',
//        publishToRepoTasks('MavenLocal')]) {
//
//}
//
///**
// * Publish all artifacts to the customLocal repository.
// */
//task publishAllToCustomLocal(dependsOn: [
//        'publishMonolithLibraryPublicationToCustomLocalRepository',
//        publishToRepoTasks('CustomLocalRepository')]) {
//
//}
//
///**
// * Upload all artifacts to bintray.
// */
//task bintrayUploadAll(dependsOn: [
//        'bintrayUpload',
//        bintrayUploadTasks()]) {
//
//}
//
///**
// * Test all submodules.
// */
//task testAll(dependsOn: [testTasks()]) {
//}
//
//afterEvaluate {
//    /**
//     * Prepare artifacts for this an all sub-projects.
//     */
//    task prepareArtifacts(dependsOn: [
//            javadocJar,
//            sourcesJar,
//            assembleRelease,
//            prepareArtifactsTasks(),
//            "generatePomFileForMonolithLibraryPublication"]) {
//    }
//}
