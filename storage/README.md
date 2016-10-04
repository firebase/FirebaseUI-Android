# FirebaseUI for Android — Storage

## Using FirebaseUI to download and display images

[Firebase Storage][firebase-storage] provides secure file uploads and downloads for your Firebase apps,
regardless of network quality. You can use it to store images, audio, video, or other
user-generated content. Firebase Storage is backed by Google Cloud Storage, a powerful, simple,
and cost-effective object storage service.

FirebaseUI provides bindings to download an image file stored in Firebase Storage
from a [`StorageReference`][storage-reference] and display it using the popular
[Glide][glide] library. This technique allows you to get all of Glide's performance
benefits while leveraging Firebase Storage's authenticated storage capabilities.

To load an image from a `StorageReference`, simply use the `FirebaseImageLoader` class:

```java
    // Reference to an image file in Firebase Storage
    StorageReference storageReference = ...;

    // ImageView in your Activity
    ImageView imageView = ...;

    // Load the image using Glide
    Glide.with(this /* context */)
            .using(new FirebaseImageLoader())
            .load(storageReference)
            .into(imageView);
```

Images displayed using `FirebaseImageLoader` are cached by their path in Firebase Storage, so
repeated loads will be fast and conserve bandwidth. For more information on caching in Glide,
see [this guide][glide-caching].

## Known Issues

There is a bug in `com.google.firebase:firebase-storage:9.6.0` and earlier that results in
excessive logging when downloading images. You may see messages in your logs like this:

```
W/StorageTask: unable to change internal state to: INTERNAL_STATE_IN_PROGRESS isUser: false from state:INTERNAL_STATE_SUCCESS
```

In production this could slow your app down if you are downloading many images. The suggested
workaround is to disable logging in production by adding the following lines to your
ProGuard configuration (`proguard-rules.pro`):

```
-assumenosideeffects class android.util.Log {
    public static int w(...);
    public static int d(...);
    public static int v(...);
}
```

This will disable calls to `Log.w()`, `Log.d()`, and `Log.v()` but preserve log calls at the
`INFO` and `ERROR` levels. Note that this will only be effective when using
`proguard-android-optimize.txt` as the default ProGuard file in `build.gradle`.

[firebase-storage]: https://firebase.google.com/docs/storage/
[glide]: https://github.com/bumptech/glide
[storage-reference]: https://firebase.google.com/docs/reference/android/com/google/firebase/storage/StorageReference
[glide-caching]: https://github.com/bumptech/glide/wiki/Caching-and-Cache-Invalidation