# FirebaseUI for Android — Storage

## Using FirebaseUI to download and display images

FirebaseUI provides bindings to download an image file from a `StorageReference` and display it
using the popular `Glide` library. This technique allows you to get all of Glide's performance
benefits while leveraging Firebase Storage's authenticated hosting capabilities.

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
repeated loads will be fast and conserve bandwidth.

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