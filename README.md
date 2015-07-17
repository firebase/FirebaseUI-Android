# FirebaseUI-Android

This library provides the simplest way to bind the Firebase Android SDK to your native Android app.
Get started now, be running in minutes.

## Using the library in your Android app

To use the FirebaseUI library in our project, we need to do a few things:

1. Add the library to the list of dependencies of our project
2. Create a class to represent the properties of our objects, as they are stored into the database
3. Create a custom list adapter to map from Firebase to Android

The FirebaseUI library is most prominent in step 3. But first we have to add it to our project.

### Adding the library to your project (gradle.build or module dependencies dialog)

If your Android app already uses Firebase, you have added a dependency to the Firebase SDK to your dependencies.
In this step we'll add the FirebaseUI library as another dependency.

![Open module settings](doc-images/1-module-settings-menu.png "Open the module settings")
![Current dependencies](doc-images/2-module-settings-add-library.png "Current dependencies")
![Find library](doc-images/3-module-settings-find-library.png "Find the firebase-ui library")
![Updated dependencies](doc-images/4-module-settings-library-added.png "Updated dependencies")

You can also add the library dependency directly to your app's gradle.build file:

![Added to gradle.build](doc-images/5-gradle-dependency-added.png "Added to gradle.build")

After the project is synchronized, we're ready to start using Firebase functionality in our app.

### Creating a model class



### Subclassing the FirebaseListAdapter


## Contributing to the library