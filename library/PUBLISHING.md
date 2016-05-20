# Publishing Firebase UI

# Publishing to Maven Local

In order to publish to your local maven repository, run the following command:

```
./gradlew: library:prepareArtifacts :library:publishAllToMavenLocal
```

If you would like to specify a custom local maven repo location, run the following command, changing
the value of the `custom_local` property to your desired location.  The default value for
`custom_local` is `/tmp/`.

```
./gradlew -Pcustom_local=<YOUR_MAVEN_REPO> :library:prepareArtifacts :library:publishAllToCustomLocal
```

# Publishing to jCenter

This will publish all of the artfiacts (as drafts) to Bintray. You will need
the environment variables `BINTRAY_USER` and `BINTRAY_KEY` defined appropriately.

```
./gradlew: clean library:prepareArtifacts :library:bintrayUploadAll
```
