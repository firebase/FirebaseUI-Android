# FirebaseUI for Storage

## Table of contents

1. [Intro](#intro)
1. [Displaying images](#using-firebaseui-to-download-and-display-images)
   1. [Setup](#setup)
   1. [Usage](#usage)
   1. [Troubleshooting](#troubleshooting)

## Intro

[Cloud Storage for Firebase][firebase-storage] provides secure file uploads and downloads for your Firebase apps,
regardless of network quality. You can use it to store images, audio, video, or other
user-generated content. Cloud Storage is a powerful, simple,
and cost-effective object storage service.

## Using FirebaseUI to download and display images

FirebaseUI provides bindings to download an image file stored in Cloud Storage
from a [`StorageReference`][storage-reference] and display it using the popular
[Glide][glide] library. This technique allows you to get all of Glide's performance
benefits while leveraging Cloud Storage's authenticated storage capabilities.

### Setup

If you're not already using Glide in your application, add the following dependencies
to your `app/build.gradle` file:

```groovy
// Find the latest Glide releases here: https://goo.gl/LpksbR
implementation 'com.github.bumptech.glide:glide:4.x'
// If you're using Kotlin (and therefore, kapt), use kapt instead of annotationProcessor
annotationProcessor 'com.github.bumptech.glide:compiler:4.x'
```

To load an image from a `StorageReference`, first register an `AppGlideModule`:

```java
@GlideModule
public class MyAppGlideModule extends AppGlideModule {

    @Override
    public void registerComponents(Context context, Glide glide, Registry registry) {
        // Register FirebaseImageLoader to handle StorageReference
        registry.append(StorageReference.class, InputStream.class,
                new FirebaseImageLoader.Factory());
    }
}
```

The class `MyAppGlideModule` can live anywhere in your source directory and is
processed by the Glide annotation processor at compile time in order to create
the `GlideApp` class.

### Usage

Once you have created an `AppGlideModule` class and done a clean build,
you can use `GlideApp` to load a `StorageReference` into an `ImageView`:

```java
// Reference to an image file in Cloud Storage
StorageReference storageReference = ...;

// ImageView in your Activity
ImageView imageView = ...;

// Download directly from StorageReference using Glide
// (See MyAppGlideModule for Loader registration)
GlideApp.with(this /* context */)
        .load(storageReference)
        .into(imageView);
```

### Troubleshooting

If GlideApp is not an importable class, build your application first before trying to use.
For more information, see Glide v4 [Generated API][generated-api] documentation.

Images displayed using `FirebaseImageLoader` are cached by their path in Cloud Storage, so
repeated loads will be fast and conserve bandwidth. For more information on caching in Glide,
see [this guide][glide-caching].

[firebase-storage]: https://firebase.google.com/docs/storage/
[glide]: https://github.com/bumptech/glide
[storage-reference]: https://firebase.google.com/docs/reference/android/com/google/firebase/storage/StorageReference
[glide-caching]: http://bumptech.github.io/glide/doc/caching.html
[generated-api]: https://bumptech.github.io/glide/doc/generatedapi.html
